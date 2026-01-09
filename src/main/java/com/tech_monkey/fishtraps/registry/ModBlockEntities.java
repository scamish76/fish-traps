package com.tech_monkey.fishtraps.registry;

import com.tech_monkey.fishtraps.FishTraps;
import com.tech_monkey.fishtraps.blockentity.FishTrapBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final Identifier FISH_TRAP_ID = Identifier.of(FishTraps.MOD_ID, "fish_trap");

    public static final BlockEntityType<FishTrapBlockEntity> FISH_TRAP =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    FISH_TRAP_ID,
                    FabricBlockEntityTypeBuilder.create(FishTrapBlockEntity::new, ModBlocks.FISH_TRAP).build()
            );

    public static void register() {
        // class init triggers registration
    }
}
