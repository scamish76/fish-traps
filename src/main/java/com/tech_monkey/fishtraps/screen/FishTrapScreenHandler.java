package com.tech_monkey.fishtraps.screen;

import com.tech_monkey.fishtraps.blockentity.FishTrapBlockEntity;
import com.tech_monkey.fishtraps.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class FishTrapScreenHandler extends ScreenHandler {

    private final Inventory inventory;
    private final PropertyDelegate props;

    // Output grid (3x3) position
    private static final int OUT_X = 26;
    private static final int OUT_Y = 17;

    // Rod slot position (yours)
    private static final int ROD_X = 130 + 4;
    private static final int ROD_Y = 31 + 4;

    public FishTrapScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(FishTrapBlockEntity.INV_SIZE), new ArrayPropertyDelegate(3));
    }

    public FishTrapScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate props) {
        super(ModScreenHandlers.FISH_TRAP, syncId);
        this.inventory = inventory;

        checkSize(inventory, FishTrapBlockEntity.INV_SIZE);
        inventory.onOpen(playerInventory.player);

        // 0 = openWater
        this.props = props;
        this.addProperties(this.props);

// Rod slot
        this.addSlot(new RodOnlySlot(inventory, FishTrapBlockEntity.SLOT_ROD, ROD_X, ROD_Y));

        // Outputs (no insert)
        int idx = FishTrapBlockEntity.OUTPUT_START;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(inventory, idx++, OUT_X + col * 18, OUT_Y + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        // Player inventory
        int invY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }

        // Hotbar
        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    public boolean isOpenWater() {
        return this.props.get(0) == 1;
    }

    public int getCatchTicksRemaining() {
        return Math.max(0, this.props.get(1));
    }

    public int getCatchTicksTotal() {
        return Math.max(0, this.props.get(2));
    }

    /** Returns 0..100 (percent). */
    public int getCatchPercent() {
        int total = getCatchTicksTotal();
        if (total <= 0) return 0;
        int remaining = getCatchTicksRemaining();
        int done = Math.max(0, total - remaining);
        return Math.min(100, Math.round((done / (float) total) * 100f));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return empty;

        ItemStack stackInSlot = slot.getStack();
        ItemStack copy = stackInSlot.copy();

        int containerSlots = FishTrapBlockEntity.INV_SIZE;
        int playerInvStart = containerSlots;
        int playerInvEnd = playerInvStart + 36;

        if (index < containerSlots) {
            if (!this.insertItem(stackInSlot, playerInvStart, playerInvEnd, true)) return empty;
        } else {
            if (stackInSlot.isOf(Items.FISHING_ROD)) {
                if (!this.insertItem(stackInSlot, FishTrapBlockEntity.SLOT_ROD, FishTrapBlockEntity.SLOT_ROD + 1, false)) {
                    return empty;
                }
            } else {
                return empty;
            }
        }

        if (stackInSlot.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        return copy;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}
