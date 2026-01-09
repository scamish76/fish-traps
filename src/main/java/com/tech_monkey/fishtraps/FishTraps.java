package com.tech_monkey.fishtraps;

import com.tech_monkey.fishtraps.registry.ModBlockEntities;
import com.tech_monkey.fishtraps.registry.ModBlocks;
import com.tech_monkey.fishtraps.registry.ModItemGroups;
import com.tech_monkey.fishtraps.registry.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FishTraps implements ModInitializer {
    public static final String MOD_ID = "fishtraps";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[fishtraps] Fish Traps is loading!");

        ModBlocks.register();
        ModBlockEntities.register();
        ModScreenHandlers.register();
        ModItemGroups.register();
    }
}
