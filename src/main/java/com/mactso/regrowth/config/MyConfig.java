// 15.2 - 1.0.0.0 regrowth
package com.mactso.regrowth.config;

import java.util.Arrays;
import java.util.List;
import com.mactso.regrowth.Main;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import org.apache.commons.lang3.tuple.Pair;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
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
	public static double    aEatingHeals;	
	public static String[]  defaultRegrowthMobs;
	public static String    defaultRegrowthMobs6464;
	public static String[]  defaultWallFoundations;
	public static String    defaultWallFoundations6464;
	@SubscribeEvent
	public static void onModConfigEvent(final ModConfig.ModConfigEvent configEvent)
	{
		if (configEvent.getConfig().getSpec() == MyConfig.SERVER_SPEC)
		{
			bakeConfig();
			RegrowthEntitiesManager.regrowthMobInit();
		}
	}	

	public static void pushDebugLevel() {
		SERVER.debugLevel.set(aDebugLevel);
	}
	
	public static void pushValues() {
		SERVER.defaultRegrowthMobsActual.set(RegrowthEntitiesManager.getRegrowthHashAsString());
		SERVER.defaultRegrowthMobsActual.set(RegrowthEntitiesManager.getRegrowthHashAsString());
	}
	
	// remember need to push each of these values separately once we have commands.
	public static void bakeConfig()
	{
		aDebugLevel = SERVER.debugLevel.get();
		aEatingHeals = SERVER.eatingHeals.get();		
		defaultRegrowthMobs6464 = SERVER.defaultRegrowthMobsActual.get() ;
		defaultWallFoundations6464 = SERVER.defaultWallFoundationsActual.get() ;
		if (aDebugLevel > 0) {
			System.out.println("Regrowth Debug Level: " + aDebugLevel );
		}
	}
	
	public static class Server {

		public final IntValue     debugLevel;
		public final DoubleValue  eatingHeals;
		public final ConfigValue<String> defaultRegrowthMobsActual;
		// mod:mob,type(eat,cut,grow,both,tall,villagerflags),Seconds;
		public final String defaultRegrowthMobs6464 = 
				  "minecraft:cow,both,240.0;"
  			    + "minecraft:horse,eat,360.0;"
				+ "minecraft:donkey,eat,360.0;"
				+ "minecraft:sheep,eat,240.0;"
				+ "minecraft:pig,grow,800.0;"
				+ "minecraft:bee,grow,1000.0;"
				+ "minecraft:chicken,grow,600.0;"
				+ "minecraft:villager,crwlvt,5.0;"
				+ "minecraft:creeper,tall,60.0"
				;

		public final ConfigValue<String> defaultWallFoundationsActual;
		public final String defaultWallFoundations6464 = 
				  "minecraft:grass_block;"
				+ "minecraft:sand;"
				+ "minecraft:podzal;"
				+ "minecraft:dirt;"
				+ "minecraft:coarse_dirt"
				;
			
		
		public Server(ForgeConfigSpec.Builder builder) {
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

			builder.push ("Regrowth Mobs 6464");
			
			defaultWallFoundationsActual = builder
					.comment("WallFoundations String 6464")
					.translation(Main.MODID + ".config" + "defaultWallFoundationsActual")
					.define("defaultWallFoundationsActual", defaultWallFoundations6464);
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
