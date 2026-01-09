package com.tech_monkey.fishtraps.blockentity;

import com.tech_monkey.fishtraps.registry.ModBlockEntities;
import com.tech_monkey.fishtraps.screen.FishTrapScreenHandler;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.fluid.Fluids;
import com.mojang.serialization.Codec;

import java.util.List;

public class FishTrapBlockEntity extends BlockEntity implements SidedInventory, NamedScreenHandlerFactory {

    // Inventory layout: [0]=rod, [1..9]=outputs (3x3)
    public static final int SLOT_ROD = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_SLOTS = 9;
    public static final int OUTPUT_END = OUTPUT_START + OUTPUT_SLOTS; // exclusive
    public static final int INV_SIZE = OUTPUT_END;

    private static final String NBT_WAIT = "WaitTicks";
    private static final String NBT_NEXT = "NextCatchTicks";
    private static final String NBT_OPEN_WATER = "OpenWater";

    private static final Identifier LT_FISH = Identifier.of("minecraft", "gameplay/fishing/fish");
    private static final Identifier LT_JUNK = Identifier.of("minecraft", "gameplay/fishing/junk");
    private static final Identifier LT_TREASURE = Identifier.of("minecraft", "gameplay/fishing/treasure");

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);

    /**
     * Server-side timer.
     * nextCatchTicks counts down to 0, then we roll loot and schedule a new cycle.
     */
    private int nextCatchTicks = 0;
    private int nextCatchTotalTicks = 0;

    // Particle pacing (server)
    private int bubbleCooldown = 0;

    // Synced-ish state (server computes, handler reads)
    private boolean openWater = false;
    private int openWaterRecheckCooldown = 0;

    // Screen sync:
    // 0 = openWater (0/1)
    // 1 = nextCatchTicks (remaining)
    // 2 = nextCatchTotalTicks (total)
    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> openWater ? 1 : 0;
                case 1 -> Math.max(0, nextCatchTicks);
                case 2 -> Math.max(0, nextCatchTotalTicks);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Client-side values sync from server -> client via ScreenHandler; we don't accept client edits.
            if (index == 0) openWater = (value != 0);
            if (index == 1) nextCatchTicks = value;
            if (index == 2) nextCatchTotalTicks = value;
        }

        @Override
        public int size() {
            return 3;
        }
    };

    public FishTrapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FISH_TRAP, pos, state);
    }

    // ---- TICK ----
    public static void tick(World world, BlockPos pos, BlockState state, FishTrapBlockEntity be) {
        if (world.isClient()) return;

        if (!(world instanceof ServerWorld serverWorld)) return;

        // Recheck open-water once per second
        if (be.openWaterRecheckCooldown <= 0) {
            boolean newValue = be.computeOpenWater(serverWorld);
            if (newValue != be.openWater) {
                be.openWater = newValue;
                be.markDirty();
            }
            be.openWaterRecheckCooldown = 20;
        } else {
            be.openWaterRecheckCooldown--;
        }

        boolean running = be.canRun(serverWorld, state);

        // If we're not running, don't burn timers.
        if (!running) {
            be.nextCatchTicks = 0;
            be.nextCatchTotalTicks = 0;
            be.bubbleCooldown = 0;
            return;
        }

        // Schedule a cycle if none is active.
        if (be.nextCatchTicks <= 0 || be.nextCatchTotalTicks <= 0) {
            int total = be.rollNextCatchTime(serverWorld);
            be.nextCatchTotalTicks = total;
            be.nextCatchTicks = total;
            be.markDirty();
        }

        // Particle pacing (visual feedback while running)
        if (be.bubbleCooldown > 0) {
            be.bubbleCooldown--;
        } else {
            be.spawnBubbles(serverWorld);
            // more frequent than before so the "working" state is obvious
            be.bubbleCooldown = 40;
        }

        // Progress timer (open water = faster)
        int step = be.openWater ? 2 : 1;
        be.nextCatchTicks -= step;

        if (be.nextCatchTicks <= 0) {
            be.performCatch(serverWorld);
            // Next cycle will be scheduled on the next tick.
            be.nextCatchTicks = 0;
            be.nextCatchTotalTicks = 0;
            be.markDirty();
        }
    }

    private boolean computeOpenWater(ServerWorld world) {
        BlockState state = world.getBlockState(this.pos);

        // Must be waterlogged (your rule)
        if (!state.contains(Properties.WATERLOGGED) || !state.get(Properties.WATERLOGGED)) {
            return false;
        }

        // Your option A: 5x5 footprint, must have water at y+1 and y+2
        return OpenWaterUtil.isTrapOpenWater(world, this.pos);
    }

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    public boolean isOpenWater() {
        return openWater;
    }

    public boolean isOutputFull() {
        for (int i = OUTPUT_START; i < OUTPUT_END; i++) {
            if (items.get(i).isEmpty()) return false;
        }
        return true;
    }

    // ---- Phase A: fishing loop ----
    private boolean canRun(ServerWorld world, BlockState state) {
        // Must be waterlogged
        if (!state.contains(Properties.WATERLOGGED) || !state.get(Properties.WATERLOGGED)) return false;

        // Must have a fishing rod
        ItemStack rod = items.get(SLOT_ROD);
        if (rod.isEmpty() || !rod.isOf(Items.FISHING_ROD)) return false;

        // Must have output room
        return !isOutputFull();
    }

    private int rollNextCatchTime(ServerWorld world) {
        // Feels vanilla-ish: 60-120 seconds baseline.
        // Open-water speed-up is handled in tick() by stepping faster.
        int min = 20 * 60;
        int max = 20 * 120;

        ItemStack rod = items.get(SLOT_ROD);
        int lure = getEnchantmentLevel(rod, Enchantments.LURE);
        // Each Lure level reduces time by ~10% (clamped)
        float mult = Math.max(0.4f, 1.0f - (0.10f * lure));

        int base = min + world.getRandom().nextInt(max - min + 1);
        return Math.max(20, Math.round(base * mult));
    }

    private void performCatch(ServerWorld world) {
        if (isOutputFull()) return;

        ItemStack rod = items.get(SLOT_ROD);
        if (rod.isEmpty() || !rod.isOf(Items.FISHING_ROD)) return;

        // Choose which vanilla subtable to roll (fish/junk/treasure) using vanilla-ish probabilities.
        Identifier tableId = chooseFishingSubtable(world, rod);

        LootTable table = getLootTableSafe(world, tableId);
        if (table == null) return;

        float luck = getEnchantmentLevel(rod, Enchantments.LUCK_OF_THE_SEA);

        ContextParameterMap params = new ContextParameterMap.Builder()
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(this.pos))
                .add(LootContextParameters.TOOL, rod)
                .build(LootContextTypes.FISHING);

        LootWorldContext ctx = new LootWorldContext(world, params, java.util.Map.of(), luck);

        List<ItemStack> loot = table.generateLoot(ctx);
        if (loot.isEmpty()) return;

        boolean insertedAny = false;

        // Insert items until we run out of room.
        for (ItemStack drop : loot) {
            if (drop == null || drop.isEmpty()) continue;
            ItemStack copy = drop.copy();
            if (insertIntoOutputs(copy)) {
                insertedAny = true;
            } else {
                // No more room.
                break;
            }
        }

        if (insertedAny) {
            // Vanilla fishing always consumes durability *sometimes* (Unbreaking affects it).
            // We also generate "catch XP" and apply it to Mending only (no XP farm).
            int xp = (tableId.equals(LT_JUNK) ? 0 : (1 + world.getRandom().nextInt(6))); // vanilla: XP only on fish/treasure
            damageRodAndApplyMending(world, xp);
        }
    }


    /**
     * Get loot table using standard 1.21.10 API
     */
    private LootTable getLootTableSafe(ServerWorld world, Identifier id) {
        try {
            var server = world.getServer();
            if (server == null) return null;
            
            RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, id);
            
            // 1.21.10 direct method
            var reloadableRegistries = server.getReloadableRegistries();
            LootTable table = reloadableRegistries.getLootTable(key);
            
            if (table == LootTable.EMPTY) {
                com.tech_monkey.fishtraps.FishTraps.LOGGER.warn("Loot table is EMPTY for key: {}", key);
                return null;
            }
            
            return table;
        } catch (Exception e) {
            com.tech_monkey.fishtraps.FishTraps.LOGGER.error("Exception getting loot table for {}: {}", id, e.getMessage());
            return null;
        }
    }

    private boolean insertIntoOutputs(ItemStack stack) {
        // First try merge
        for (int i = OUTPUT_START; i < OUTPUT_END; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty()) continue;
            if (ItemStack.areItemsAndComponentsEqual(existing, stack)) {
                int space = existing.getMaxCount() - existing.getCount();
                if (space <= 0) continue;

                int move = Math.min(space, stack.getCount());
                existing.increment(move);
                stack.decrement(move);
                if (stack.isEmpty()) {
                    markDirty();
                    return true;
                }
            }
        }

        // Then empty slot
        for (int i = OUTPUT_START; i < OUTPUT_END; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack);
                markDirty();
                return true;
            }
        }
        return false;
    }

    private Identifier chooseFishingSubtable(ServerWorld world, ItemStack rod) {
        // Vanilla baseline (unenchanted): Fish 85%, Junk 10%, Treasure 5%.
        // Luck of the Sea shifts probability from Fish/Junk into Treasure (about +2% treasure per level).
        // Treasure is only possible in Open Water.
        int luckLvl = getEnchantmentLevel(rod, Enchantments.LUCK_OF_THE_SEA);

        float treasureChance = this.openWater ? (0.05f + (0.02f * luckLvl)) : 0.0f;
        float junkChance = 0.10f - (0.02f * luckLvl);

        // When treasure is disabled (not open water), vanilla still gives fish/junk only.
        // Keep junk in a sensible vanilla-ish range.
        junkChance = Math.max(0.0f, junkChance);

        // Clamp so we never go negative / exceed 100%
        treasureChance = Math.min(treasureChance, 0.60f);
        junkChance = Math.min(junkChance, 0.60f);

        float roll = world.getRandom().nextFloat();
        if (roll < treasureChance) return LT_TREASURE;
        if (roll < treasureChance + junkChance) return LT_JUNK;
        return LT_FISH;
    }

    private void damageRodAndApplyMending(ServerWorld world, int catchXp) {
        ItemStack rod = items.get(SLOT_ROD);
        if (rod.isEmpty()) return;
        if (!rod.isDamageable()) return;

        // ---- Durability consumption (Unbreaking) ----
        int unbreaking = getEnchantmentLevel(rod, Enchantments.UNBREAKING);
        boolean consume = unbreaking <= 0 || world.getRandom().nextInt(unbreaking + 1) == 0;

        if (consume) {
            int newDamage = rod.getDamage() + 1;
            rod.setDamage(newDamage);

            // If it breaks, remove it and stop.
            if (rod.getDamage() >= rod.getMaxDamage()) {
                items.set(SLOT_ROD, ItemStack.EMPTY);
                markDirty();
                return;
            }
        }

        // ---- Mending behavior (NO XP farm) ----
        // Any XP from a catch is applied ONLY to the rod if it has Mending.
        // If the rod is fully repaired (or doesn't need repair), the XP is discarded.
        int mending = getEnchantmentLevel(rod, Enchantments.MENDING);
        if (mending > 0 && catchXp > 0 && rod.getDamage() > 0) {
            // In vanilla: 2 durability per 1 xp
            int repair = Math.min(rod.getDamage(), catchXp * 2);
            rod.setDamage(rod.getDamage() - repair);
            // leftover XP intentionally destroyed
        }

        markDirty();
    }


private void spawnBubbles(ServerWorld world) {
    double x = this.pos.getX() + 0.5;
    double z = this.pos.getZ() + 0.5;
    double y = this.pos.getY() + 0.9;
    
    // Use BUBBLE_COLUMN_UP for a natural rising column (like soul sand)
    // Spawn fewer particles than magma/soul sand for subtlety
    world.spawnParticles(
        ParticleTypes.BUBBLE_COLUMN_UP,
        x, y, z,
        3,                    // Just 2 particles (magma uses way more)
        0.2, 0.0, 0.2,       // Slight horizontal scatter
        0.0
    );
}

    private static int getEnchantmentLevel(ItemStack stack, net.minecraft.registry.RegistryKey<Enchantment> key) {
        // 1.21+ stores enchantments as RegistryEntry<Enchantment> -> level inside the stack components.
        // We avoid registry lookups entirely by matching the RegistryKey on each entry.
        var ench = stack.getEnchantments();
        for (var e : ench.getEnchantmentEntries()) {
            var entry = e.getKey();
            var kOpt = entry.getKey();
            if (kOpt.isPresent() && kOpt.get().equals(key)) {
                return e.getIntValue();
            }
        }
        return 0;
    }

    // ---- GUI ----
    @Override
    public Text getDisplayName() {
        return Text.translatable("container.fishtraps.fish_trap");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new FishTrapScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    // ---- Inventory ----
    @Override public int size() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > stack.getMaxCount()) {
            stack.setCount(stack.getMaxCount());
        }
        markDirty();
    }

    @Override public void clear() { items.clear(); markDirty(); }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return this.world != null
                && this.world.getBlockEntity(this.pos) == this
                && player.squaredDistanceTo(
                this.pos.getX() + 0.5D,
                this.pos.getY() + 0.5D,
                this.pos.getZ() + 0.5D
        ) <= 64.0D;
    }

    // ---- Hopper behavior (NO BAIT) ----
    // Top + sides can insert rod. Bottom extracts outputs.
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) {
            int[] out = new int[OUTPUT_SLOTS];
            for (int i = 0; i < OUTPUT_SLOTS; i++) out[i] = OUTPUT_START + i;
            return out;
        }

        // UP or SIDES: rod slot only
        return new int[]{SLOT_ROD};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        // Phase B change: hoppers should NOT feed the trap (rod insertion is GUI/player-only).
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        if (dir != Direction.DOWN) return false;
        return slot >= OUTPUT_START;
    }

    // ---- Persistence (1.21+ ReadView/WriteView) ----
    @Override
    protected void readData(ReadView view) {
        this.openWater = view.getBoolean(NBT_OPEN_WATER, false);
        this.nextCatchTicks = view.getInt(NBT_NEXT, 0);
        this.nextCatchTotalTicks = view.getInt(NBT_WAIT, 0); // reuse old key to avoid breaking older worlds

        // Clear inventory
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }

        // Items are stored as two parallel lists: Slots (int) and Items (ItemStack).
        // This avoids writing minecraft:air stacks, which 1.21+ rejects.
        var slotsView = view.getOptionalTypedListView("Slots", Codec.INT).orElse(null);
        var itemsView = view.getOptionalTypedListView("Items", ItemStack.CODEC).orElse(null);

        if (slotsView != null && itemsView != null) {
            var slotIt = slotsView.iterator();
            var itemIt = itemsView.iterator();
            while (slotIt.hasNext() && itemIt.hasNext()) {
                Integer slot = slotIt.next();
                ItemStack stack = itemIt.next();
                if (slot == null) continue;
                if (slot < 0 || slot >= this.items.size()) continue;
                this.items.set(slot, (stack == null) ? ItemStack.EMPTY : stack);
            }
        }
    }

    @Override
    protected void writeData(WriteView view) {
        view.putBoolean(NBT_OPEN_WATER, this.openWater);
        view.putInt(NBT_NEXT, this.nextCatchTicks);
        view.putInt(NBT_WAIT, this.nextCatchTotalTicks);

        var slots = view.getListAppender("Slots", Codec.INT);
        var stacks = view.getListAppender("Items", ItemStack.CODEC);

        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            if (stack == null || stack.isEmpty()) continue; // DO NOT write air/empty stacks
            slots.add(i);
            stacks.add(stack);
        }
    }
}