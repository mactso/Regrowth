// 15.2 - 1.0.0.0 regrowth
package com.mactso.regrowth.config;

import org.apache.commons.lang3.tuple.Pair;

import com.mactso.regrowth.Main;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod.EventBusSubscriber(modid = Main.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
public class MyConfig {
	
	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;
	
	static
	{
		final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
	}
	
	public static int getaDebugLevel() {
		return aDebugLevel;
	}

	public static void setaDebugLevel(int aDebugLevel) {
		MyConfig.aDebugLevel = aDebugLevel;
	}

	public static double getaEatingHeals() {
		return aEatingHeals;
	}

	public static void setaEatingHeals(double aEatingHeals) {
		MyConfig.aEatingHeals = aEatingHeals;
	}

	public static int       aDebugLevel;
	public static double    aEatingHeals;	
	public static String[]  defaultRegrowthMobs;
	public static String    defaultRegrowthMobs6464;
	public static String[]  defaultWallFoundations;
	public static String    defaultWallFoundations6464;
	public static String[]  defaultWallBiomeData;
	public static String    defaultWallBiomeData6464;
	@SubscribeEvent
	public static void onModConfigEvent(final ModConfig.ModConfigEvent configEvent)
	{
		if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC)
		{
			bakeConfig();
			RegrowthEntitiesManager.regrowthMobInit();
			WallFoundationDataManager.wallFoundationsInit();
			WallBiomeDataManager.wallBiomeDataInit();
		}
	}	

	public static void pushDebugLevel() {
		COMMON.debugLevel.set(aDebugLevel);
	}
	
	public static void pushValues() {
		COMMON.defaultRegrowthMobsActual.set(RegrowthEntitiesManager.getRegrowthHashAsString());
		COMMON.defaultWallFoundationsActual.set(WallFoundationDataManager.getWallFoundationHashAsString());
		COMMON.defaultBiomeWallDataActual.set(WallBiomeDataManager.getWallBiomeDataHashAsString());
	}
	
	// remember need to push each of these values separately once we have commands.
	public static void bakeConfig()
	{
		aDebugLevel = COMMON.debugLevel.get();
		aEatingHeals = COMMON.eatingHeals.get();		
		defaultRegrowthMobs6464 = COMMON.defaultRegrowthMobsActual.get() ;
		defaultWallFoundations6464 = COMMON.defaultWallFoundationsActual.get() ;
		defaultWallBiomeData6464 = COMMON.defaultBiomeWallDataActual.get() ;
		if (aDebugLevel > 0) {
			System.out.println("Regrowth Debug Level: " + aDebugLevel );
		}
	}
	
	public static class Common {

		public final IntValue     debugLevel;
		public final DoubleValue  eatingHeals;
		// mod:mob,type(eat,cut,grow,both,tall,villager flags),Seconds;
		public final ConfigValue<String> defaultRegrowthMobsActual;
		public final String defaultRegrowthMobs6464 = 
				  "minecraft:cow,both,240.0;"
  			    + "minecraft:horse,eat,360.0;"
				+ "minecraft:donkey,eat,360.0;"
				+ "minecraft:sheep,eat,240.0;"
				+ "minecraft:pig,grow,800.0;"
				+ "minecraft:bee,grow,1000.0;"
				+ "minecraft:chicken,grow,600.0;"
				+ "minecraft:villager,crwplvt,5.0;"
				+ "minecraft:creeper,tall,60.0;"
				+ "minecraft:zombie,stumble,60.0;"
				+ "minecraft:skeleton,mushroom,60.0;"
				;

		// blocks walls can be built on
		public final ConfigValue<String> defaultWallFoundationsActual;
		public final String defaultWallFoundations6464 = 
				  "minecraft:grass_block;"
				+ "minecraft:sand;"
				+ "minecraft:red_sand;"
				+ "minecraft:netherrack;"
				+ "minecraft:sandstone;"
				+ "minecraft:podzol;"
				+ "minecraft:dirt;"
				+ "minecraft:stone;"
				+ "minecraft:coarse_dirt"
				;

		// biome to get biome category, wall size, wall block type
		public final ConfigValue<String> defaultBiomeWallDataActual;
		public final String defaultBiomeWallData6464 = 
				  "Regrowth:default,48,minecraft:cobblestone_wall,minecraft:oak_fence;"
				+ "minecraft:plains,48,minecraft:cobblestone_wall,minecraft:oak_fence;"
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
				+ "minecraft:nether,40,minecraft:blackstone_wall,minecraft:nether_brick_fence;"
				+ "Regrowth:minimum,32,regrowth:minimum_wall_size,regrowth:fence_placeholder"
				;
		
		
		public Common(ForgeConfigSpec.Builder builder) {
			builder.push("Regrowth Control Values");
			
			debugLevel = builder
					.comment("Debug Level: 0 = Off, 1 = Log, 2 = Chat+Log")
					.translation(Main.MODID + ".config." + "debugLevel")
					.defineInRange("debugLevel", () -> 0, 0, 2);
		
			eatingHeals = builder
					.comment("Eating Heals: 0-No, 1-yes")
					.translation(Main.MODID + ".config." + "eatingHeals")
					.defineInRange("eatingHeals", () -> .99,0.0,1.0);
			
			builder.pop();

			builder.push ("Regrowth Mobs 6464");
			
			defaultRegrowthMobsActual = builder
					.comment("RegrowthMobs String 6464")
					.translation(Main.MODID + ".config" + "defaultRegrowthMobsActual")
					.define("defaultRegrowthMobsActual", defaultRegrowthMobs6464);
			builder.pop();			

			builder.push ("Regrowth Wall Foundations 6464");
			
			defaultWallFoundationsActual = builder
					.comment("WallFoundations String 6464")
					.translation(Main.MODID + ".config" + "defaultWallFoundationsActual")
					.define("defaultWallFoundationsActual", defaultWallFoundations6464);
			builder.pop();			

			builder.push ("Regrowth Biome Wall Data 6464");
			
			defaultBiomeWallDataActual = builder
					.comment("Biome Meeting Wall Data String 6464")
					.translation(Main.MODID + ".config" + "defaultBiomeWallDataActual")
					.define("defaultBiomeWallDataActual", defaultBiomeWallData6464);
			builder.pop();	
		
		}
	}
	
	// support for any color chattext
	public static void sendChat(PlayerEntity p, String chatMessage, Color color) {
		StringTextComponent component = new StringTextComponent (chatMessage);
		component.getStyle().setColor(color);
		p.sendMessage(component, p.getUniqueID());
	}
	
	// support for any color, optionally bold text.
	public static void sendBoldChat(PlayerEntity p, String chatMessage, Color color) {
		StringTextComponent component = new StringTextComponent (chatMessage);

		component.getStyle().setBold(true);
		component.getStyle().setColor(color);
		
		p.sendMessage(component, p.getUniqueID());
	}
	
}
