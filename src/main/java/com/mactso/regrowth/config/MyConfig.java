// 1.12.2
package com.mactso.regrowth.config;

import com.mactso.regrowth.util.Reference;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Ignore;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.RangeDouble;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid=Reference.MOD_ID)
@Mod.EventBusSubscriber
public class MyConfig {

	
	@Comment ( { "Debug Level" } )
	@Name ("Debug Level   0 to 2 : 'Off', 'Log', 'Chat & Log'")
	@RangeInt  (min=0, max=2)
	public static int aDebugLevel = 0;

	@Comment ( { "Mushroom Density Along X Coordinate." } )
	@Name ("mushroomDensityX")
	@RangeInt (min=3, max=11)
	private static int mushroomXDensity = 6;

	@Comment ( { "Mushroom Density Along Z Coordinate." } )
	@Name ("mushroomDensityZ")
	@RangeInt (min=3, max=11)
	private static int mushroomZDensity = 6;

	@Comment ( { "Mushroom Minimum Biome Temperature" } )
	@Name ("MushroomMinTemp")
	@RangeDouble (min=-2.0, max=2.0)
	private static double mushroomMinTemp = 0.2;

	@Comment ( { "Mushroom Maximum Biome Temperature" } )
	@Name ("MushroomMaxTemp")
	@RangeDouble (min=-2.0, max=2.0)
	private static double mushroomMaxTemp = 1.2;

	@Comment ( { "Eating Heals: 0-No, 1-yes" } )
	@Name ("eatingHeals")
	@RangeDouble (min=1.0, max=11.0)
	public static double aEatingHeals = 0.99;

	@Comment ( { "Regrowth Mobs and Actions" } )
	@Name ( "Regrowth Mobs and Actions:" )
	public static String[] defaultRegrowthMobs = {
			"minecraft:cow,both,480.0"
			,"minecraft:horse,eat,360.0"
			, "minecraft:donkey,eat,360.0" 
			, "minecraft:sheep,eat,240.0" 
			, "minecraft:pig,reforest,900.0"
			, "minecraft:bee,grow,1000.0" 
			, "minecraft:chicken,grow,640.0" 
			, "minecraft:villager,crwplvt,5.0"
			, "minecraft:creeper,tall,120.0" 
			, "minecraft:zombie,stumble, 60.0" 
			, "minecraft:bat,stumble, 60.0"
			, "minecraft:skeleton,mushroom, 60.0"
	};

	@Comment ( { "List of Wall Foundation Blocks" } )
	@Name ( "Wall Foundation Block List:" )
	public static String[] defaultWallFoundations = {
			"minecraft:grass>0" // grass block 
			, "minecraft:sand>0" // sand
			, "minecraft:sand>1"  // red sand
			, "minecraft:netherrack>0" 
			, "minecraft:sandstone>0" 
			, "minecraft:podzol>2" // podzol
			, "minecraft:dirt>0" 
			, "minecraft:stone>0" 
			, "minecraft:dirt>1"
	};

	@Comment ( { "List of Wall Block Definitions By Biome" } )
	@Name ( "Biome Wall Block List:" )
	public static String[] defaultWallBiomeData = {
			"Regrowth:default,48,minecraft:cobblestone_wall"
			, "minecraft:plains,48,minecraft:cobblestone_wall"
			, "minecraft:desert,48,minecraft:fence"
			, "minecraft:extreme_hills,48,minecraft:cobblestone_wall"
			, "minecraft:taiga,48,minecraft:cobblestone_wall"
			, "minecraft:savanna,48,minecraft:fence"
			, "minecraft:icy,40,minecraft:cobblestone_wall"
			, "minecraft:the_end,40,minecraft:cobblestone_wall"
			, "minecraft:beach,48,minecraft:fence"
			, "minecraft:forest,48,minecraft:cobblestone_wall"
			, "minecraft:mesa,48,minecraft:cobblestone_wall"
			, "minecraft:jungle,48,minecraft:cobblestone_wall"
			, "minecraft:river,48,minecraft:cobblestone_wall"
			, "minecraft:nether,40,minecraft:cobblestone_wall"
	};

	@Ignore
	public static boolean serverSide = false;
	
	public static int getaDebugLevel() {
		return aDebugLevel;
	}

	public static void setaDebugLevel(int aDebugLevel) {
		MyConfig.aDebugLevel = aDebugLevel;
	}

	public static double getaEatingHeals() {
		return aEatingHeals;
	}
//
	public static void setaEatingHeals(double aEatingHeals) {
		MyConfig.aEatingHeals = aEatingHeals;
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
	
	@SubscribeEvent
	public static void onModConfigEvent(OnConfigChangedEvent event)
	{
		if(event.getModID().equals(Reference.MOD_ID))
		{
			RegrowthEntitiesManager.regrowthMobInit();	
			WallFoundationDataManager.wallFoundationsInit();	
			WallBiomeDataManager.wallBiomeDataInit();	
			if (serverSide) {
				if (aDebugLevel>0) {
					System.out.println("Server: Regrowth ConfigurationChangedEvent.");
				}				
			}
			if (!serverSide) {
				if (aDebugLevel>0) {
					System.out.println("Client: Regrowth ConfigurationChangedEvent.");
				}				
			}
		}
	}

	public static void debugMsg (int level, String dMsg) {
		if (aDebugLevel > level-1) {
			System.out.println("L"+level + ":" + dMsg);
		}
	}

	public static void debugMsg (int level, BlockPos pos, String dMsg) {
		if (aDebugLevel > level-1) {
			System.out.println("L"+level+" ("+pos.getX()+","+pos.getY()+","+pos.getZ()+"): " + dMsg);
		}
	}
	
	public static void sendChat(EntityPlayer p, String chatMessage, TextFormatting textColor) {
	ITextComponent component = 
			new TextComponentString (
		  		  chatMessage
				);		        		
		component.getStyle().setColor(textColor);
		p.sendMessage(component);
	}

	
	public static void sendBoldChat(EntityPlayer p, String chatMessage, TextFormatting textColor) {
	ITextComponent component = 
			new TextComponentString (
		  		  chatMessage
				);		
		component.getStyle().setBold(true);
		component.getStyle().setColor(textColor);
		p.sendMessage(component);
	}

}



