package com.mactso.regrowth.modloader.config;

import org.apache.commons.lang3.tuple.Pair;

import com.mactso.regrowth.modloader.main.RegrowthMain;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = RegrowthMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MyConfig {

	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;

	static {
		final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
	}

	public static int getaDebugLevel() {
		return debugLevel;
	}

	public static int getDebugLevel() {
		return debugLevel;
	}

	public static void setDebugLevel(int newDebugLevel) {
		MyConfig.debugLevel = newDebugLevel;
	}

	public static double getEatingHealsOdds() {
		return eatingHealsOdds;
	}

	public static String getTorchBlock() {
		return torchBlock;
	}

	public static String getPlayerWallControlBlock() {
		return playerWallControlBlock;
	}

	public static String getRegrowthMobsAndActions() {
		return regrowthMobsAndActions;
	}

	public static String getWallFoundations() {
		return wallFoundations;
	}

	public static String getWallBiomeBlocks() {
		return wallBiomeBlocks;
	}

	public static int getMushroomDensity() {
		return MyConfig.mushroomDensity;
	}

	public static int getMushroomXDensity() {
		return MyConfig.mushroomXDensity;
	}

	public static int getMushroomZDensity() {
		return MyConfig.mushroomZDensity;
	}

	public static double getMushroomMinTemp() {
		return MyConfig.mushroomMinTemp;
	}

	public static double getMushroomMaxTemp() {
		return MyConfig.mushroomMaxTemp;
	}

	public static int getTorchLightLevel() {
		return torchLightLevel;
	}

	public static int debugLevel;
	public static double eatingHealsOdds;

	public static String torchBlock;
	public static String playerWallControlBlock;
	public static String regrowthMobsAndActions;
	public static String wallFoundations;
	public static String wallBiomeBlocks;

	private static int torchLightLevel;

	private static int mushroomDensity;
	private static int mushroomXDensity;
	private static int mushroomZDensity;
	private static double mushroomMinTemp;
	private static double mushroomMaxTemp;

	@SubscribeEvent
	public static void onModConfigEvent(final ModConfigEvent configEvent) {
		if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC) {
			bakeConfig();

		}
	}

	public static void pushDebugLevel() {
		COMMON.debugLevel.set(debugLevel);
	}

	// Populate static variables from config
	public static void bakeConfig() {
		debugLevel = COMMON.debugLevel.get();
		eatingHealsOdds = COMMON.eatingHeals.get();
		torchLightLevel = COMMON.torchLightLevel.get();
		mushroomDensity = COMMON.mushroomDensity.get();
		mushroomXDensity = COMMON.mushroomXDensity.get();
		mushroomZDensity = COMMON.mushroomZDensity.get();
		mushroomMinTemp = COMMON.mushroomMinTemp.get();
		mushroomMaxTemp = COMMON.mushroomMaxTemp.get();

		torchBlock = COMMON.torchBlockString.get();
		playerWallControlBlock = COMMON.playerWallControlBlockString.get();
		regrowthMobsAndActions = COMMON.actualRegrowthMobsAndActionsString.get();
		wallFoundations = COMMON.actualWallFoundationsString.get();
		wallBiomeBlocks = COMMON.actualWallBiomeBlocksString.get();

		if (debugLevel > 0) {
			System.out.println("Regrowth Debug Level: " + debugLevel);
		}
	}

	public static class Common {

		public final IntValue debugLevel;
		public final DoubleValue eatingHeals;
		public final IntValue torchLightLevel;
		public final ForgeConfigSpec.IntValue mushroomDensity;
		public final ForgeConfigSpec.IntValue mushroomXDensity;
		public final ForgeConfigSpec.IntValue mushroomZDensity;
		public final ForgeConfigSpec.DoubleValue mushroomMinTemp;
		public final ForgeConfigSpec.DoubleValue mushroomMaxTemp;
		public final ConfigValue<String> playerWallControlBlockString;
		public final ConfigValue<String> torchBlockString;

		// mod:mob,type(eat,cut,grow,both,tall,villagerflags),Seconds;
		public final ConfigValue<String> actualRegrowthMobsAndActionsString;
		public final String defaultRegrowthMobsAndActionsString = "minecraft:cow,both,300.0;"
				+ "minecraft:horse,eat,180.0;" + "minecraft:donkey,eat,180.0;" + "minecraft:sheep,eat,120.0;"
				+ "minecraft:pig,reforest,450.0;" + "minecraft:bee,grow,500.0;" + "minecraft:chicken,grow,320.0;"
				+ "minecraft:villager,chrwvt,2.0;" + "minecraft:creeper,tall,90.0;" + "minecraft:zombie,stumble, 30.0;"
				+ "minecraft:bat,stumble, 30.0;" + "minecraft:skeleton,mushroom, 40.0;"
				+ "minecraft:tropical_fish,coral, 15.0;" + "minecraft:squid,coral, 15.0;";

		// blocks walls can be built on
		public final ConfigValue<String> actualWallFoundationsString;
		public final String defaultWallFoundationsString = "minecraft:grass_block;" + "minecraft:sand;"
				+ "minecraft:red_sand;" + "minecraft:netherrack;" + "minecraft:sandstone;" + "minecraft:podzol;"
				+ "minecraft:dirt;" + "minecraft:stone;" + "minecraft:coarse_dirt;";

		// biome to get biome category, wall size, wall block type
		public final ConfigValue<String> actualWallBiomeBlocksString;
		public final String defaultWallBiomeBlocksString = "Regrowth:default,40,minecraft:cobblestone_wall,minecraft:oak_fence;"
				+ "minecraft:plains,40,minecraft:cobblestone_wall,minecraft:oak_fence;"
				+ "minecraft:desert,40,minecraft:sandstone_wall,minecraft:birch_fence;"
				+ "minecraft:extreme_hills,40,minecraft:cobblestone_wall,minecraft:spruce_fence;"
				+ "minecraft:taiga,40,minecraft:mossy_cobblestone_wall,minecraft:spruce_fence;"
				+ "minecraft:savanna,40,minecraft:stone_brick_wall,minecraft:acacia_fence;"
				+ "minecraft:icy,40,minecraft:diorite_wall,minecraft:spruce_fence;"
				+ "minecraft:the_end,40,minecraft:end_stone_brick_wall,minecraft:birch_fence;"
				+ "minecraft:beach,40,minecraft:sandstone_wall,minecraft:oak_fence;"
				+ "minecraft:forest,40,minecraft:mossy_stone_brick_wall,minecraft:oak_fence;"
				+ "minecraft:mesa,40,minecraft:red_sandstone_wall,minecraft:oak_fence;"
				+ "minecraft:jungle,40,minecraft:granite_wall,minecraft:jungle_fence;"
				+ "minecraft:river,40,minecraft:mossy_cobblestone_wall,minecraft:oak_fence;"
				+ "minecraft:nether,40,minecraft:blackstone_wall,minecraft:nether_brick_fence;"
				+ "Regrowth:minimum,32,regrowth:minimum_wall_size,regrowth:fence_placeholder";

		public Common(ForgeConfigSpec.Builder builder) {
			builder.push("Regrowth Control Values");

			debugLevel = builder.comment("Debug Level: 0 = Off, 1 = Log, 2 = Chat+Log")
					.translation(RegrowthMain.MODID + ".config." + "debugLevel").defineInRange("debugLevel", () -> 0, 0, 2);

			eatingHeals = builder.comment("Eating Heals: 0-No, 1-yes")
					.translation(RegrowthMain.MODID + ".config." + "eatingHeals")
					.defineInRange("eatingHeals", () -> .99, 0.0, 1.0);

			this.torchLightLevel = builder
					.comment("Torch Light Level - Villagers will only place torches on blocks this dark or darker.")
					.translation("regrowth.config.torchLightLevel ").defineInRange("torchLightLevel ", () -> 3, 0, 10);

			this.mushroomDensity = builder.comment("Mushroom density - 3 dense to 11 sparse to 21 very sparse")
					.translation("regrowth.config.mushroomXDensity ")
					.defineInRange("mushroomXDensity ", () -> 7, 3, 21);

			this.mushroomXDensity = builder.comment("Mushroom X axis density - 3 dense to 11 sparse")
					.translation("regrowth.config.mushroomXDensity ")
					.defineInRange("mushroomXDensity ", () -> 6, 3, 11);
			this.mushroomZDensity = builder.comment("Mushroom Z axis density - 3 dense to 11 sparse")
					.translation("regrowth.config.mushroomZDensity ")
					.defineInRange("mushroomZDensity ", () -> 6, 3, 11);
			this.mushroomMinTemp = builder.comment("Mushroom Minimum Biome Temperature")
					.translation("regrowth.config.mushroomMinTemp")
					.defineInRange("mushroomMinTemp", () -> 0.2, -2.0, 2.0);
			this.mushroomMaxTemp = builder.comment("Mushroom Maximum Biome Temperature")
					.translation("regrowth.config.mushroomMaxTemp")
					.defineInRange("mushroomMaxTemp", () -> 1.2, -2.0, 2.0);

			this.playerWallControlBlockString = builder.comment(
					"When block is over bell, villagers build walls. This block is created over bell when village is new.  If block is 'Air' players can't turn off wall building.")
					.translation("regrowth.config.playerWallControlBlockString")
					.define("playerWallControlBlockString", "minecraft:cobblestone_wall");

			this.torchBlockString = builder.comment("This is the torch the villagers place.  It can be a modded torch.")
					.translation("regrowth.config.torchBlockString").define("torchBlockString", "minecraft:torch");

			builder.pop();

			builder.push("Regrowth Mobs And Actions");

			actualRegrowthMobsAndActionsString = builder.comment("Regrowth Mobs And Actions")
					.translation(RegrowthMain.MODID + ".config.regrowthMobsAndActions")
					.define("regrowthMobsAndActions", defaultRegrowthMobsAndActionsString);

			builder.pop();

			builder.push("Valid Regrowth Wall Foundation Blocks");

			actualWallFoundationsString = builder.comment("Blocks villagers can build walls on.")
					.translation(RegrowthMain.MODID + ".config.wallFoundationsString")
					.define("wallFoundationsString", defaultWallFoundationsString);

			builder.pop();

			builder.push("Regrowth Blocks used to build Walls by Biome ");

			actualWallBiomeBlocksString = builder.comment("Wall Blocks by Biome Category")
					.translation(RegrowthMain.MODID + ".config.wallBiomeBlocksString")
					.define("wallBiomeBlocksString", defaultWallBiomeBlocksString);

			builder.pop();

		}

		public static boolean isString(Object o) {
			return (o instanceof String);
		}
	}

}
