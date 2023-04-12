package dev.carlvbn.colorassist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

public class ColorAssist implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("colorassist");
	private KeyBinding colorPickerKey;
	private ColorPickerScreenHandler colorPickerScreenHandler;

	private static ArrayList<ColoredBlock> coloredBlocks;

	@Override
	public void onInitializeClient() {
		ColorCreativeTab.register();

		colorPickerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.colorassist.opencolorpicker", // The translation key of the keybinding's name
				InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
				GLFW.GLFW_KEY_V, // The keycode of the key
				"category.colorassist" // The translation key of the keybinding's category.
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (colorPickerKey.wasPressed()) {
				if (colorPickerScreenHandler == null) {
					colorPickerScreenHandler = new ColorPickerScreenHandler(client.player.getInventory());
				}

				client.setScreenAndRender(new ColorPickerGui(colorPickerScreenHandler, client.player));
			}
		});

		coloredBlocks = new ArrayList<ColoredBlock>();
		try {
			loadBlockColors();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void loadBlockColors() throws IOException {
		LOGGER.info("Loading block colors");
		coloredBlocks.clear();

		BufferedReader reader = new BufferedReader(new InputStreamReader(ColorAssist.class.getResourceAsStream("/assets/colorassist/color_dictionary.txt")));
		String line;
		while ((line = reader.readLine()) != null) {
			String name = line.split(":")[0];
			String[] colorStrings = line.split(":")[1].split("-");
			Color[] colors = new Color[4];

			for (int i = 0; i<colorStrings.length; i++) {
				String[] colorElements = colorStrings[i].split(",");
				colors[i] = new Color(Integer.parseInt(colorElements[0]), Integer.parseInt(colorElements[1]), Integer.parseInt(colorElements[2]));
			}

			Block block = Registries.BLOCK.get(new Identifier("minecraft", name));
			if (block != Blocks.AIR) {
				coloredBlocks.add(new ColoredBlock(block, colors[0], colors[1], colors[2], colors[3]));
			}
		}

		Collections.sort(coloredBlocks);
	}

	public static ArrayList<ColoredBlock> getColoredBlocks() {
		return coloredBlocks;
	}
}
