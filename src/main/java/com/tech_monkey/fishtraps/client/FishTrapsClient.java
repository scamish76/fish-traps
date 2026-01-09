package com.tech_monkey.fishtraps.client;

import com.tech_monkey.fishtraps.registry.ModScreenHandlers;
import com.tech_monkey.fishtraps.screen.FishTrapScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class FishTrapsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.FISH_TRAP, FishTrapScreen::new);
    }
}
