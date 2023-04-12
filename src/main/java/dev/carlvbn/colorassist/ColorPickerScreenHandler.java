package dev.carlvbn.colorassist;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public class ColorPickerScreenHandler extends ScreenHandler {
    protected final Inventory input = new SimpleInventory(27);

    public ColorPickerScreenHandler(PlayerInventory playerInventory) {
        super(ScreenHandlerType.GENERIC_9X3, 0);

        for(int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                Slot slot = new Slot(input, j + i * 9, 8 + j * 18, 84 + i * 18);
                this.addSlot(slot);
            }
        }


    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return getSlot(slot).getStack();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
