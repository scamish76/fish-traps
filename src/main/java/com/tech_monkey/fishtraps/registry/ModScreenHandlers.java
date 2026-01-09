package com.tech_monkey.fishtraps.registry;

import com.tech_monkey.fishtraps.FishTraps;
import com.tech_monkey.fishtraps.screen.FishTrapScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    private ModScreenHandlers() {}

    public static final ScreenHandlerType<FishTrapScreenHandler> FISH_TRAP =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of(FishTraps.MOD_ID, "fish_trap"),
                    new ScreenHandlerType<>(FishTrapScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
            );

    public static void register() {}
}
