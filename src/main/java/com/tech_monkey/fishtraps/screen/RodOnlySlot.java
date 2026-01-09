package com.tech_monkey.fishtraps.screen;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

public class RodOnlySlot extends Slot {
    public RodOnlySlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return stack.isOf(Items.FISHING_ROD);
    }

    @Override
    public int getMaxItemCount() {
        return 1;
    }
}
