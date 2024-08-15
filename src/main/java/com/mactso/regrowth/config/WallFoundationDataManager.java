package com.mactso.regrowth.config;

import java.util.Hashtable;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class WallFoundationDataManager {

	public static Hashtable<String, wallFoundationItem> wallFoundationsHashtable = new Hashtable<>();


	public static wallFoundationItem getWallFoundationInfo(String key) {
		String iKey = key;

		if (wallFoundationsHashtable.isEmpty()) {
			wallFoundationsInit();
		}

		wallFoundationItem r = wallFoundationsHashtable.get(iKey);

		return r;
	}

	public static String getWallFoundationHashAsString() {
		String returnString="";
		String wallFoundationType;
		for (String key:wallFoundationsHashtable.keySet()) {
			wallFoundationType = wallFoundationsHashtable.get(key).wallFoundationType;
			String tempString = key+"," + wallFoundationType + ";";
			returnString += tempString;
		}
		return returnString;
	
	}

	public static void wallFoundationsInit() {
		
		int i = 0;


		i = 0;
		wallFoundationsHashtable.clear();
		while (i < MyConfig.defaultWallFoundationsArray.length) {
			try {
				String wallFoundationBlockKey = MyConfig.defaultWallFoundationsArray[i];
				String key = wallFoundationBlockKey;				
				if (ForgeRegistries.BLOCKS.containsKey(ResourceLocation.parse(wallFoundationBlockKey))) {
					wallFoundationsHashtable.put(key, new wallFoundationItem(wallFoundationBlockKey));
				} else {
					System.out.println("Regrowth Debug: Wall Foundation Block: " + wallFoundationBlockKey + " not in Forge Entity Type Registry.  Mispelled?  Missing semicolon? ");
				}
			} catch (Exception e) {
				System.out.println("Regrowth Debug:  Bad Wall Foundation Config (illegal characters, Upper Case or not [a-z0-9_.-] ?): " + MyConfig.defaultWallFoundationsArray[i]);
			}
			i++;
		}

	}

	public static class wallFoundationItem {
		String wallFoundationType;  // a block like "minecraft:block" or "oby:dirt"
		
		public wallFoundationItem(String wallFoundationType) {
			this.wallFoundationType =  wallFoundationType;
		}

		public String getwallFoundationType() {
			return wallFoundationType.toLowerCase();
		}

	}
	
}
