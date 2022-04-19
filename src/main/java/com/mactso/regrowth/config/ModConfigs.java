package com.mactso.regrowth.config;
import java.util.HashSet;

import com.mactso.regrowth.Main;
import com.mojang.datafixers.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModConfigs {
	
	private static final Logger LOGGER = LogManager.getLogger();
	
    public static SimpleConfig CONFIG;
    private static ModConfigProvider configs;


	public static int getDebugLevel() {
		return debugLevel;
	}

	public static void setDebugLevel(int debugLevel) {
		ModConfigs.debugLevel = debugLevel;
	}

	public static boolean getEatingHeals() {
	if (eatingHeals.equals("true")) {
		return true;
	}
	return false;
}

	public static int getTorchLightLevel() {
		return torchLightLevel;
	}

	public static int getMushroomDensity() {
		return mushroomDensity;
	}

	public static int getMushroomXDensity() {
		return mushroomXDensity;
	}

	public static int getMushroomZDensity() {
		return mushroomZDensity;
	}

	public static double getMushroomMinTemp() {
		return mushroomMinTemp;
	}

	public static double getMushroomMaxTemp() {
		return mushroomMaxTemp;
	}
    
    public static int debugLevel;
	public static String eatingHeals;
	private static int torchLightLevel;
	private static int mushroomDensity;
	private static int mushroomXDensity;
	private static int mushroomZDensity;
	private static double mushroomMinTemp;
	private static double mushroomMaxTemp;
	
	//	public static Block playerWallControlBlock;
//	
//	public static String[] defaultRegrowthMobs;
//	public static String defaultRegrowthMobs6464;
//	public static String[]  defaultWallFoundationsArray;
////	public static String[] defaultWallFoundations;
////	public static String defaultWallFoundations6464;
//	public static String[] defaultWallBiomeData;
//	public static String defaultWallBiomeData6464;
//

//
	

	public static HashSet<String> getModStringSet (String[] values) {
		HashSet<String> returnset = new HashSet<>();
		// Collection<ModContainer> loadedMods= FabricLoader.getAllMods();  error static calling non-static.
		HashSet<String> loadedset = new HashSet<>();
		loadedset.add("respawnvillager");
		loadedset.add("test");

		for (String s : loadedset) {
			String s2 = s.trim().toLowerCase();
			if (!s2.isEmpty()) {
				if (!returnset.contains(s2)) {
					returnset.add(s2);
				} else {
					LOGGER.warn("spawnbalanceutility includedReportModsSet entry : " +s2 + " is not a valid current loaded forge mod.");
				} 
			}
		}
		return returnset;
	}
	

	public static void registerConfigs() {
        configs = new ModConfigProvider();
        createConfigs();

        CONFIG = SimpleConfig.of(Main.MOD_ID + "config").provider(configs).request();

        assignConfigs();
    }

    private static void createConfigs() {
        configs.addKeyValuePair(new Pair<>("key.debugLevel", 0), "int");	
        configs.addKeyValuePair(new Pair<>("key.torchLightLevel", 7), "int");	
        configs.addKeyValuePair(new Pair<>("key.eatingHeals", "true"), "String");
        configs.addKeyValuePair(new Pair<>("key.eatingHeals", "true"), "String");
        configs.addKeyValuePair(new Pair<>("key.mushroomDensity", 7), "int");
        configs.addKeyValuePair(new Pair<>("key.mushroomXDensity", 6), "int");
        configs.addKeyValuePair(new Pair<>("key.mushroomZDensity", 6), "int");
        configs.addKeyValuePair(new Pair<> ("key.mushroomMinTemp", 0.2), "double");
        configs.addKeyValuePair(new Pair<> ("key.mushroomMaxTemp", 1.2), "double");
    }

    private static void assignConfigs() {
    	debugLevel = CONFIG.getOrDefault("key.debugLevel", 0);
    	torchLightLevel = CONFIG.getOrDefault("key.torchLightLevel", 7);
    	eatingHeals = CONFIG.getOrDefault("key.eatingHeals", "true");	
    	mushroomDensity = CONFIG.getOrDefault("key.mushroomDensity", 7);
    	mushroomXDensity = CONFIG.getOrDefault("key.mushroomXDensity", 6);
    	mushroomZDensity = CONFIG.getOrDefault("key.mushroomZDensity", 6);
    	mushroomMinTemp = CONFIG.getOrDefault("key.mushroomMinTemp", 0.2);
    	mushroomMaxTemp = CONFIG.getOrDefault("key.mushroomMaxTemp", 1.2);
       LOGGER.info("All " + configs.getConfigsList().size() + " have been set properly");
    }
}
