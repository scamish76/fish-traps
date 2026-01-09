package com.tech_monkey.fishtraps.registry;

import com.tech_monkey.fishtraps.FishTraps;
import com.tech_monkey.fishtraps.block.FishTrapBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    private ModBlocks() {}

    public static final Identifier FISH_TRAP_ID = Identifier.of(FishTraps.MOD_ID, "fish_trap");
    public static final RegistryKey<Block> FISH_TRAP_KEY = RegistryKey.of(RegistryKeys.BLOCK, FISH_TRAP_ID);

    // Registers the block and ensures its Settings include the registry key (prevents "Block id not set")
    public static final Block FISH_TRAP = Blocks.register(
            FISH_TRAP_KEY,
            FishTrapBlock::new,
            AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque()
    );

    // Blocks do NOT automatically get an inventory item. Register a BlockItem explicitly.
    public static final BlockItem FISH_TRAP_ITEM = Registry.register(
            Registries.ITEM,
            FISH_TRAP_ID,
            new BlockItem(FISH_TRAP, new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, FISH_TRAP_ID)))
    );

    /** Called from mod init to ensure this class is loaded. */
    public static void register() {
        // no-op: static initializers do the registration
    }
}
