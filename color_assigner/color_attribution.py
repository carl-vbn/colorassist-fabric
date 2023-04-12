# This script is used to generate the color_dictionary.txt file

from PIL import Image, ImageDraw
from math import floor, sqrt
import os, random, json

def get_average_color(imgs):
    rgb_sum = [0,0,0]
    pixel_count = 0
    for img in imgs:
        pixels = img.getdata()
        for pixel in pixels:
            if pixel[3] < 50: # Ignore transparent pixels
                continue

            for i in range(3):
                rgb_sum[i] += pixel[i]
            pixel_count+=1

    if pixel_count > 0:
        return (floor(rgb_sum[0]/pixel_count), floor(rgb_sum[1]/pixel_count), floor(rgb_sum[2]/pixel_count))
    else:
        return (0,0,0)

def get_distance(color_a, color_b):
    if len(color_a) != len(color_b): raise Exception("Both colors don't have the same dimension")

    squared_distance = 0
    for i in range(len(color_a)):
        squared_distance += (color_a[i]-color_b[i])**2
    
    return sqrt(squared_distance)

def random_sample_unique(list, num):
    list = list.copy()
    random.shuffle(list)

    sample = []
    index = 0
    while len(sample) < num:
        if index >= len(list):
            return None

        element = list[index]
        if element not in sample:
            sample.append(element)
        index+=1

    return sample


class NotEnoughColorsException(Exception):
    pass

def get_dominant_colors(imgs, num_clusters):
    colors = []
    for img in imgs:
        pixels = img.getdata()
        for pixel in pixels:
            if pixel[3] < 50: # Ignore transparent pixels
                continue

            colors.append((pixel[0], pixel[1], pixel[2]))

    if num_clusters > len(set(colors)):
        raise NotEnoughColorsException()

    cluster_centers = random_sample_unique(colors, num_clusters)

    clusters = None

    for _ in range(1):
        clusters = [[] for __ in range(num_clusters)]

        for color in colors:
            closest_cluster_index = None
            closest_cluster_distance = None
            for i in range(num_clusters):
                cluster_center = cluster_centers[i]
                cluster_center_distance = get_distance(cluster_center, color)
                if closest_cluster_index is None or cluster_center_distance < closest_cluster_distance:
                    closest_cluster_index = i
                    closest_cluster_distance = cluster_center_distance

            clusters[closest_cluster_index].append(color)


        for i in range(num_clusters):
            rSum = 0
            gSum = 0
            bSum = 0
            color_count = 0

            for color in clusters[i]:
                rSum += color[0]
                gSum += color[1]
                bSum += color[2]
                color_count += 1

            cluster_centers[i] = (rSum/color_count, gSum/color_count, bSum/color_count)

    return [(floor(x[0]), floor(x[1]), floor(x[2])) for _, x in sorted(zip(map(lambda e: -len(e), clusters), cluster_centers))]

def extract_colors(images):
    average_color = get_average_color(images)
    dominant_halves = get_dominant_colors(images, 2)
    dominant_thirds = get_dominant_colors(images, 3)
    dominant_quarters = get_dominant_colors(images, 4)

    return average_color, dominant_halves[0], dominant_thirds[0], dominant_quarters[0]

def resolve_texture(texture_name, input_dir):
    return os.path.join(input_dir, 'textures', texture_name.split(':')[-1]+'.png')

def extract_block_colors(model_data, input_dir):
    if 'textures' not in model_data:
        return None

    imgs = []
    for texture in model_data['textures'].values():
        texture_path = resolve_texture(texture, input_dir)
        if os.path.exists(texture_path):
            imgs.append(Image.open(texture_path, 'r').convert('RGBA'))

    if len(imgs) == 0:
        return None

    colors = extract_colors(imgs)
    for img in imgs:
        img.close()

    return colors

def get_model_data(model_name, input_dir):
    if model_name.startswith('minecraft:'):
        model_name = model_name[10:]

    if not model_name.endswith('.json'):
        model_name = model_name + '.json'

    models_dir = os.path.join(input_dir, 'models') if model_name.startswith('block/') else os.path.join(input_dir, 'models', 'block')
    model_data = None
    with open(os.path.join(models_dir, model_name.replace('/', '\\')), 'r') as f:
        model_data = json.load(f)
    
    return model_data

def is_full_cube(model_data, input_dir):
    if 'parent' in model_data:
        if model_data['parent'].startswith('minecraft:block/cube') or model_data['parent'].startswith('minecraft:block/orientable') or model_data['parent'].startswith('minecraft:block/leaves') or model_data['parent'] == 'minecraft:block/template_glazed_terracotta':
            return True
        else:
            return is_full_cube(get_model_data(model_data['parent'], input_dir), input_dir)

    return False

def process_directory(input_dir, output_dir, color_dict_file):
    models_dir = os.path.join(input_dir, 'models', 'block')
    for model_file_name in os.listdir(models_dir):
        block_name = model_file_name.split('.json')[0]

        model_data = get_model_data(block_name, input_dir)
        if model_data is None:
            print("ERROR: Can't read model data for "+block_name)
            exit(-1)
        
        if not is_full_cube(model_data, input_dir):
            print("* Skipping",model_file_name)
            continue

        print(model_file_name)


        colors = None
        try:
            colors = extract_block_colors(model_data, input_dir)
        except NotEnoughColorsException:
            pass

        if colors is None:
            continue

        output_image = Image.new('RGBA', (16*len(colors), 16), 'BLACK')
        img_draw = ImageDraw.Draw(output_image)
        for i in range(len(colors)):
            img_draw.rectangle(((i*16, 0),(16+i*16,16)), fill=colors[i])

        output_image.save(os.path.join(output_dir, 'palettes', block_name+'.png'))
        output_image.close()

        dict_entry_string = block_name+':'+'-'.join(map(lambda col: f'{col[0]},{col[1]},{col[2]}', colors))+'\n'
        color_dict_file.write(dict_entry_string)



color_dict_file = open(os.path.join('output', 'color_dictionary.txt'), 'w')
process_directory('input', 'output', color_dict_file)
color_dict_file.close()