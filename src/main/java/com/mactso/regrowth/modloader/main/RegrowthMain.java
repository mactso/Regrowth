package com.mactso.regrowth.modloader.main;

import com.mactso.regrowth.commands.RegrowthCommands;
import com.mactso.regrowth.managers.ManagerInitializer;
import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.utilities.MyUtilities;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class RegrowthMain implements ModInitializer {

    public static final String MODID = "regrowth"; 

    @Override
    public void onInitialize() {
        registerEvents();
        MyConfig.registerConfigs();
    }

    /** Register Fabric event callbacks */
    private void registerEvents() {

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RegrowthCommands.register(dispatcher);
        });

        // Server startup event (simpler lambda style)
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            onServerStarting(server);
        });
    }

    private void onServerStarting(MinecraftServer server) {
        MyUtilities.debugMsg(0, "Regrowth: Initializing Managers of Configured data.");
        // Initialize all managers in a modloader-agnostic way
        ManagerInitializer.initializeManagers(server);
    }
}


