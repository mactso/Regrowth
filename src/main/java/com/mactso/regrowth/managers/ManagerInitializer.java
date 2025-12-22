package com.mactso.regrowth.managers;

import java.nio.file.Path;

import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.server.MinecraftServer;

public class ManagerInitializer {

	
    /** Initialize all Regrowth managers with server-derived config path */
    public static void initializeManagers(MinecraftServer server) {
        MyUtilities.debugMsg(0, "Regrowth: Initializing Managers");

        // Get config directory in a modloader-agnostic way
        Path configDir = server.getServerDirectory().resolve("config");

        RegrowthEntitiesManager.regrowthMobInit();
        WallFoundationManager.wallFoundationsInit(server);
        WallBiomeDataManager.wallBiomeDataInit(server);
        SaplingManager.saplingManagerInit(server, configDir);

        MyUtilities.debugMsg(0, "Managers initialized with config dir: " + configDir);
    }
	
	
}
