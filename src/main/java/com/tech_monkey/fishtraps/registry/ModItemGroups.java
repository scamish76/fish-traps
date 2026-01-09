package com.tech_monkey.fishtraps.registry;

import com.tech_monkey.fishtraps.FishTraps;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroups {
    private ModItemGroups() {}

    public static final Identifier FISH_TRAPS_TAB_ID = Identifier.of(FishTraps.MOD_ID, "fish_traps");

    public static final ItemGroup FISH_TRAPS_TAB = Registry.register(
            Registries.ITEM_GROUP,
            FISH_TRAPS_TAB_ID,
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.fishtraps.fish_traps"))
					// Use the registered BlockItem (avoids creating an empty stack if the BlockItem isn't present).
					.icon(() -> new ItemStack(ModBlocks.FISH_TRAP.asItem()))
					.entries((displayContext, entries) -> entries.add(new ItemStack(ModBlocks.FISH_TRAP.asItem(), 1)))
                    .build()
    );

    public static void register() {}
}
