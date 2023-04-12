package dev.carlvbn.colorassist;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class ColorCreativeTab {

    public static void register() {
        ItemGroup ITEM_GROUP = FabricItemGroup.builder(new Identifier("colorassist", "color_sorted_group"))
                .icon(() -> new ItemStack(Items.PURPLE_CONCRETE_POWDER))
                .build();

        ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP).register(content -> {
            for (ColoredBlock block : ColorAssist.getColoredBlocks()) {
                ItemStack stack = new ItemStack(block.getBlock());
                if (stack.isEmpty()) continue;
                content.add(stack);
            }
        });
    }

}
