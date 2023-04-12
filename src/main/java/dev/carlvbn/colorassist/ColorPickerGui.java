package dev.carlvbn.colorassist;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class ColorPickerGui extends HandledScreen<ColorPickerScreenHandler> {
    public static final Identifier BACKGROUND_TEXTURE = new Identifier("colorassist", "textures/gui/color_picker_gui.png");
    private Color selectedColor;
    private Vec2f pickerCenter = new Vec2f(166f, 78.5f);
    private final int pickerRadius = 32;
    private SimpleOption<Double> hueOption;
    private SimpleOption<Double> saturationOption;
    private SimpleOption<Double> brightnessOption;
    private boolean skipUpdate; // If this is true, the displayed blocks will not be updated when an HSB value changes
    private boolean sliderUpdateNeeded; // If this is true, the sliders will be updated when the mouse is released
    private boolean picking; // If this is true, the mouse will only be able to interact with the picker, nothing else
    private SliderWidget[] sliders;
    private int focusedSlider;
    private CyclingButtonWidget<ColorAttributionMode> colorAttributionModeButton;

    @SuppressWarnings("unchecked")
    public ColorPickerGui(ColorPickerScreenHandler handler, PlayerEntity player) {
        super(handler, player.getInventory(), Text.of("Color picker"));
        this.passEvents = true;
        this.titleX = 77;
        this.selectedColor = Color.RED;
        this.skipUpdate = false;
        this.sliderUpdateNeeded = false;
        this.picking = false;
        this.focusedSlider = -1;

        this.hueOption = new SimpleOption<Double>("colorassist.picker.hue", SimpleOption.emptyTooltip(), (prefix, value) -> Text.of("Hue: " + (int) (value * 100.0) + "%"), SimpleOption.DoubleSliderCallbacks.INSTANCE, 0.0D, (value) -> {
            if (!this.skipUpdate) updateColor();
        });

        saturationOption = new SimpleOption<Double>("colorassist.picker.saturation", SimpleOption.emptyTooltip(), (prefix, value) -> Text.of("Saturation: " + (int) (value * 100.0) + "%"), SimpleOption.DoubleSliderCallbacks.INSTANCE, 1.0D, (value) -> {
            if (!this.skipUpdate) updateColor();
        });

        brightnessOption = new SimpleOption<Double>("colorassist.picker.brightness", SimpleOption.emptyTooltip(), (prefix, value) -> Text.of("Brightness: " + (int) (value * 100.0) + "%"), SimpleOption.DoubleSliderCallbacks.INSTANCE, 1.0D, (value) -> {
            if (!this.skipUpdate) updateColor();
        });

        GameOptions options = MinecraftClient.getInstance().options;

        sliders = new SliderWidget[] {
                (SliderWidget) hueOption.createButton(options, 210, 50, 75),
                (SliderWidget) saturationOption.createButton(options, 210, 72, 75),
                (SliderWidget) brightnessOption.createButton(options, 210, 94, 75)
        };

        SimpleOption<ColorAttributionMode> colorAttributionModeOption = new SimpleOption<ColorAttributionMode>("colorassist.colorAttributionMode", SimpleOption.emptyTooltip(), SimpleOption.enumValueText(), new SimpleOption.PotentialValuesBasedCallbacks(Arrays.asList(ColorAttributionMode.values()), Codec.INT.xmap(ColorAttributionMode::byId, ColorAttributionMode::getId)), ColoredBlock.colorAttributionMode, (value) -> {
            ColoredBlock.colorAttributionMode = colorAttributionModeButton.getValue();
            if (!this.skipUpdate) updateColor();
        });

        this.colorAttributionModeButton = (CyclingButtonWidget<ColorAttributionMode>) colorAttributionModeOption.createButton(options, width-205, 5, 200);

        updateColor();
    }

    private void handleResize(int width, int height) {
        for (int i = 0; i<3; i++) sliders[i].setPos(x + 85, y+9+i*22);
        this.colorAttributionModeButton.setPos(width-205, colorAttributionModeButton.getY());

        this.pickerCenter = new Vec2f(x + 42, y + 42);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        handleResize(width, height);

        super.resize(client, width, height);
    }

    protected void init() {
        super.init();

        handleResize(width, height);
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);

        for (ClickableWidget cw : this.sliders) {
            cw.render(matrices, mouseX, mouseY, delta);
        }

        this.colorAttributionModeButton.render(matrices, mouseX, mouseY, delta);

        Vec2f cursorPos = getCursorPosition();
        this.drawCursor(matrices, (int)cursorPos.x, (int)cursorPos.y, 3, Color.black.getRGB());

    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        // Don't draw title
    }

    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
        int i = this.x;
        int j = this.y;
        drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    private void drawCursor(MatrixStack matrices, int x, int y, int radius, int color) {
        fill(matrices, x-radius, y-radius, x-radius+1, y+radius, color);
        fill(matrices, x+radius, y-radius, x+radius+1, y+radius, color);
        fill(matrices, x-radius, y-radius, x+radius, y-radius+1, color);
        fill(matrices, x-radius, y+radius, x+radius+1, y+radius+1, color);

    }

    private Vec2f getCursorPosition() {
        float dx = (float)(pickerRadius * Math.cos(hueOption.getValue() * Math.PI * 2) * Math.sqrt(saturationOption.getValue()));
        float dy = (float)(pickerRadius * Math.sin(hueOption.getValue() * Math.PI * 2) * Math.sqrt(saturationOption.getValue()));

        return pickerCenter.add(new Vec2f(dx, -dy));
    }

    protected boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY) {
        return super.isPointWithinBounds(x, y, width, height, pointX, pointY);
    }

    private boolean handleMouseDown(double mouseX, double mouseY) {
        double dx = mouseX - pickerCenter.x;
        double dy = mouseY - pickerCenter.y;
        double r2 = dx*dx+dy*dy;

        if (r2 < pickerRadius*pickerRadius || picking) {
            picking = true;
            sliderUpdateNeeded = true;
            skipUpdate = true;
            hueOption.setValue((Math.atan2(dy, -dx) / Math.PI + 1.0) / 2.0);
            saturationOption.setValue(Math.min(1, r2/(pickerRadius*pickerRadius)));
            skipUpdate = false;

            updateColor();


            return true;
        }
        return false;
    }

    private void updateColor() {
        selectedColor = new Color(Color.HSBtoRGB(hueOption.getValue().floatValue(), saturationOption.getValue().floatValue(), brightnessOption.getValue().floatValue()));

        ArrayList<ColoredBlock> selectedBlocks = new ArrayList<>();
        for (ColoredBlock coloredBlock : ColorAssist.getColoredBlocks()) {
            int colorError = ColoredBlock.getColorError(coloredBlock.getColor(), selectedColor);

            for (int i = 0; i<27; i++) {
                if (i<selectedBlocks.size()) {
                    ColoredBlock selectedBlock = selectedBlocks.get(i);
                    int error = ColoredBlock.getColorError(selectedBlock.getColor(), selectedColor);
                    if (colorError < error) {
                        selectedBlocks.add(i, coloredBlock);
                        break;
                    }
                } else {
                    selectedBlocks.add(coloredBlock);
                    break;
                }
            }
        }

        for (int i = 0; i<27; i++) {
            handler.getSlot(9+i).setStack(new ItemStack(Item.BLOCK_ITEMS.get(selectedBlocks.get(i).getBlock())));
        }
    }

    private void pickStackColor(ItemStack stack) {
        for (ColoredBlock coloredBlock : ColorAssist.getColoredBlocks()) {
            if (Item.BLOCK_ITEMS.get(coloredBlock.getBlock()) == stack.getItem()) {
                float[] hsb = Color.RGBtoHSB(coloredBlock.getColor().getRed(), coloredBlock.getColor().getGreen(), coloredBlock.getColor().getBlue(), null);

                skipUpdate = true;
                hueOption.setValue((double) hsb[0]);
                saturationOption.setValue((double) hsb[1]);
                brightnessOption.setValue((double)hsb[2]);
                skipUpdate = false;

                updateColor();
                updateSliders();

                return;
            }
        }
    }

    private void updateSliders() {
        GameOptions options = MinecraftClient.getInstance().options;
        sliders = new SliderWidget[] {
                (SliderWidget) hueOption.createButton(options, sliders[0].getX(), sliders[0].getY(), sliders[0].getWidth()),
                (SliderWidget) saturationOption.createButton(options, sliders[1].getX(), sliders[1].getY(), sliders[1].getWidth()),
                (SliderWidget) brightnessOption.createButton(options, sliders[2].getX(), sliders[2].getY(), sliders[2].getWidth()),
        };

        sliderUpdateNeeded = false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (focusedSlider == -1) {
            if (handleMouseDown(mouseX, mouseY)) return true;


            for (int i = 0; i<3; i++) {
                if (sliders[i].isMouseOver(mouseX, mouseY) && sliders[i].mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    focusedSlider = i;
                    return true;
                }
            }
        } else {
            sliders[focusedSlider].mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
        return mouseX < (double)left || mouseY < (double)top || mouseX >= (double)(left + this.backgroundWidth) || mouseY >= (double)(top + this.backgroundHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (focusedSlider == -1) {
            if (handleMouseDown(mouseX, mouseY)) return true;

            for (int i = 0; i < 3; i++) {
                if (sliders[i].isMouseOver(mouseX, mouseY) && sliders[i].mouseClicked(mouseX, mouseY, button)) {
                    focusedSlider = i;
                    return true;
                }
            }
        } else {
            sliders[focusedSlider].mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (colorAttributionModeButton.isMouseOver(mouseX, mouseY) && colorAttributionModeButton.mouseClicked(mouseX, mouseY, button)) return true;

        for(int i = 9; i < this.handler.slots.size(); i++) {
            Slot slot = this.handler.slots.get(i);
            if (this.isPointWithinBounds(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.isEnabled()) {
                if (slot.getStack() == null) continue; // Shouldn't happen, but just in case

                if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                    giveItemStack(slot.getStack());
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
                    giveItemStack(new ItemStack(slot.getStack().getItem(), 64));
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_3) {
                    ItemStack stack = slot.getStack();
                    if (stack != null) {
                        pickStackColor(stack);
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (sliderUpdateNeeded) updateSliders();
        picking = false;
        focusedSlider = -1;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void giveItemStack(ItemStack stack)
    {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (player.isCreative()) {
            for(int i = 0; i < 9; i++)
            {
                if(!player.getInventory().getStack(i).isEmpty())
                    continue;

                player.networkHandler.sendPacket(
                        new CreativeInventoryActionC2SPacket(36 + i, stack));

                return;
            }
        } else {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("give @s "+ id.getNamespace()+":"+id.getPath()+" "+stack.getCount());
        }

    }
}
