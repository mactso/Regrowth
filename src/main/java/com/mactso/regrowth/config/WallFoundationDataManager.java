package com.mactso.regrowth.config;

import java.util.Hashtable;
import java.util.StringTokenizer;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;


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
		

		
		wallFoundationsHashtable.clear();

		String oneLine = "";
		StringTokenizer tokenizedWallFoundationString = new StringTokenizer(MyConfig.getWallFoundationsList(), ";");

		while (tokenizedWallFoundationString.hasMoreElements()) {
			oneLine = tokenizedWallFoundationString.nextToken().trim();
			if (oneLine.isEmpty()) continue;
			try {
				String key = oneLine;
		        if (Registry.BLOCK.containsId(new Identifier(key))) {
					wallFoundationsHashtable.put(key, new wallFoundationItem(key));		
				} else {
					System.out.println("Regrowth Debug: Wall Foundation Block: " + key + " not in Block Registry.  Mispelled?  Missing semicolon? ");
				}
				
			} catch (Exception e) {
				System.out.println("Regrowth Debug:  Bad Wall Foundation Config (illegal characters, Upper Case or not [a-z0-9_.-] ?): " + oneLine);
			}
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
