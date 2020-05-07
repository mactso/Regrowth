package com.mactso.regrowth.config;

import java.util.Arrays;
import java.util.List;
import com.mactso.regrowth.Main;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import org.apache.commons.lang3.tuple.Pair;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfig.Server;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod.EventBusSubscriber(modid = Main.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
public class MyConfig {
	
	public static final Server SERVER;
	public static final ForgeConfigSpec SERVER_SPEC;
	
	static
	{
		final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
		SERVER_SPEC = specPair.getRight();
		SERVER = specPair.getLeft();
	}
	
	public static int       aDebugLevel;
	public static String[]  defaultRegrowthMobs;
	public static String    defaultRegrowthMobs6464;

	@SubscribeEvent
	public static void onModConfigEvent(final ModConfig.ModConfigEvent configEvent)
	{
		if (configEvent.getConfig().getSpec() == MyConfig.SERVER_SPEC)
		{
			bakeConfig();
			RegrowthEntitiesManager.regrowthMobInit();
		}
	}	

	public static void pushValues() {
		SERVER.defaultRegrowthMobsActual.set(RegrowthEntitiesManager.getRegrowthHashAsString());
	}	
	public static void bakeConfig()
	{
		aDebugLevel = SERVER.debugLevel.get();
		defaultRegrowthMobs6464 = SERVER.defaultRegrowthMobsActual.get() ;
		if (aDebugLevel > 0) {
			System.out.println("Regrowth Debug Level: " + aDebugLevel );
		}
	}
	
	public static class Server {


		public final IntValue     debugLevel;
		public final ConfigValue<String> defaultRegrowthMobsActual;
		// mod:mob,type(eat,grow,both,tall),%;
		public final String defaultRegrowthMobs6464 = 
				  "minecraft:cow,both,1.9;"
				+ "minecraft:creeper,tall,1.1;"
				+ "minecraft:sheep,eat,2.1;"
				;
		
		public Server(ForgeConfigSpec.Builder builder) {
			builder.push("Regrowth Control Values");
			
			debugLevel = builder
					.comment("Debug Level: 0 = Off, 1 = Log, 2 = Chat+Log")
					.translation(Main.MODID + ".config." + "debugLevel")
					.defineInRange("debugLevel", () -> 0, 0, 2);
		
			builder.pop();

			builder.push ("Regrowth Mobs 6464");
			
			defaultRegrowthMobsActual = builder
					.comment("RegrowthMobs String 6464")
					.translation(Main.MODID + ".config" + "defaultRegrowthMobsActual")
					.define("defaultRegrowthMobsActual", defaultRegrowthMobs6464);
			builder.pop();			
		}
	}
}
