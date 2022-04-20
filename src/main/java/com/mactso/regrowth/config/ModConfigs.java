package com.mactso.regrowth.config;

import java.util.HashSet;
import java.util.StringTokenizer;

import com.mactso.regrowth.Main;
import com.mojang.datafixers.util.Pair;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.BlockTags;

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

	public static Block getPlayerWallControlBlock() {
		return playerWallControlBlock;
	}

	public static String getDefaultRegrowthMobs() {
		return defaultRegrowthMobs;
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
	private static String playerWallControlBlockString;
	public static Block playerWallControlBlock;

	private static String defaultRegrowthMobs;

	private static String defaultWallBiomeList;
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

	public static HashSet<String> getModStringSet(String[] values) {
		HashSet<String> returnset = new HashSet<>();
		// Collection<ModContainer> loadedMods= FabricLoader.getAllMods(); error static
		// calling non-static.
		HashSet<String> loadedset = new HashSet<>();
		loadedset.add("regrowth");
		loadedset.add("test");

		for (String s : loadedset) {
			String s2 = s.trim().toLowerCase();
			if (!s2.isEmpty()) {
				if (!returnset.contains(s2)) {
					returnset.add(s2);
				} else {
					LOGGER.warn("regrowth includedReportModsSet entry : " + s2
							+ " is not a valid current loaded forge mod.");
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
		configs.addKeyValuePair(new Pair<>("key.mushroomDensity", 7), "int");
		configs.addKeyValuePair(new Pair<>("key.mushroomXDensity", 6), "int");
		configs.addKeyValuePair(new Pair<>("key.mushroomZDensity", 6), "int");
		configs.addKeyValuePair(new Pair<>("key.mushroomMinTemp", 0.2), "double");
		configs.addKeyValuePair(new Pair<>("key.mushroomMaxTemp", 1.2), "double");
		configs.addKeyValuePair(new Pair<>("key.playerWallControlBlockString", "minecraft:cobblestone_wall"), "String");
		configs.addKeyValuePair(new Pair<>("key.defaultRegrowthMobs",
				"minecraft:cow,both,300.0;" + "minecraft:horse,eat,180.0;" + "minecraft:donkey,eat,180.0;"
						+ "minecraft:sheep,eat,120.0;" + "minecraft:pig,reforest,450.0;" + "minecraft:bee,grow,500.0;"
						+ "minecraft:chicken,grow,320.0;" + "minecraft:villager,chrwvt,2.0;"
						+ "minecraft:creeper,tall,90.0;" + "minecraft:zombie,stumble, 30.0;"
						+ "minecraft:bat,stumble, 30.0;" + "minecraft:skeleton,mushroom, 30.0"),
				"String");
		configs.addKeyValuePair(new Pair<>("key.defaultWallBiomeList",
				"minecraft:plains,48,minecraft:cobblestone_wall,minecraft:oak_fence;"
				+ "minecraft:desert,48,minecraft:sandstone_wall,minecraft:birch_fence;"
				+ "minecraft:extreme_hills,48,minecraft:cobblestone_wall,minecraft:spruce_fence;"
				+ "minecraft:taiga,48,minecraft:mossy_cobblestone_wall,minecraft:spruce_fence;"
				+ "minecraft:savanna,48,minecraft:stone_brick_wall,minecraft:acacia_fence;"
				+ "minecraft:icy,40,minecraft:diorite_wall,minecraft:spruce_fence;"
				+ "minecraft:the_end,40,minecraft:end_stone_brick_wall,minecraft:birch_fence;"
				+ "minecraft:beach,48,minecraft:sandstone_wall,minecraft:oak_fence;"
				+ "minecraft:forest,48,minecraft:mossy_stone_brick_wall,minecraft:oak_fence;"
				+ "minecraft:mesa,48,minecraft:red_sandstone_wall,minecraft:oak_fence;"
				+ "minecraft:jungle,48,minecraft:granite_wall,minecraft:jungle_fence;"
				+ "minecraft:river,48,minecraft:mossy_cobblestone_wall,minecraft:oak_fence;"
				+ "minecraft:nether,40,minecraft:blackstone_wall,minecraft:nether_brick_fence;"),
				"String");

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
		playerWallControlBlockString = CONFIG.getOrDefault("key.playerWallControlBlockString",
				"minecraft:cobblestone_wall");
		// TODO how do I get this from the string?
		playerWallControlBlock = Blocks.COBBLESTONE_WALL.getDefaultState().getBlock();
		
		// TODO This is duplicated from above.  Is that unavoidable?
		defaultRegrowthMobs = CONFIG.getOrDefault("key.defaultRegrowthMobs",
				"minecraft:cow,both,300.0;" + "minecraft:horse,eat,180.0;" + "minecraft:donkey,eat,180.0;"
						+ "minecraft:sheep,eat,120.0;" + "minecraft:pig,reforest,450.0;" + "minecraft:bee,grow,500.0;"
						+ "minecraft:chicken,grow,320.0;" + "minecraft:villager,chrwvt,2.0;"
						+ "minecraft:creeper,tall,90.0;" + "minecraft:zombie,stumble, 30.0;"
						+ "minecraft:bat,stumble, 30.0;" + "minecraft:skeleton,mushroom, 30.0");
		RegrowthEntitiesManager.regrowthMobInit();
		defaultWallBiomeList = CONFIG.getOrDefault("key.defaultWallBiomeList",
				"minecraft:plains,48,minecraft:cobblestone_wall,minecraft:oak_fence;"
				+ "minecraft:desert,48,minecraft:sandstone_wall,minecraft:birch_fence;"
				+ "minecraft:extreme_hills,48,minecraft:cobblestone_wall,minecraft:spruce_fence;"
				+ "minecraft:taiga,48,minecraft:mossy_cobblestone_wall,minecraft:spruce_fence;"
				+ "minecraft:savanna,48,minecraft:stone_brick_wall,minecraft:acacia_fence;"
				+ "minecraft:icy,40,minecraft:diorite_wall,minecraft:spruce_fence;"
				+ "minecraft:the_end,40,minecraft:end_stone_brick_wall,minecraft:birch_fence;"
				+ "minecraft:beach,48,minecraft:sandstone_wall,minecraft:oak_fence;"
				+ "minecraft:forest,48,minecraft:mossy_stone_brick_wall,minecraft:oak_fence;"
				+ "minecraft:mesa,48,minecraft:red_sandstone_wall,minecraft:oak_fence;"
				+ "minecraft:jungle,48,minecraft:granite_wall,minecraft:jungle_fence;"
				+ "minecraft:river,48,minecraft:mossy_cobblestone_wall,minecraft:oak_fence;"
				+ "minecraft:nether,40,minecraft:blackstone_wall,minecraft:nether_brick_fence;");

		WallBiomeDataManager.wallBiomeDataInit();
		
		LOGGER.info("All " + configs.getConfigsList().size() + " have been set properly");
	}

}
