package com.mactso.regrowth.config;

import java.util.Hashtable;
import java.util.StringTokenizer;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

public class WallFoundationDataManager {

	public static Hashtable<String, WallFoundationItem> wallFoundationsHashtable = new Hashtable<>();
	private static String defaultWallFoundationString = "hbm:default";
	private static String defaultWallFoundationKey = defaultWallFoundationString;

	public static WallFoundationItem getWallFoundationInfo(String key) {
		String iKey = key+">0";

		if (wallFoundationsHashtable.isEmpty()) {
			wallFoundationsInit();
		}

		WallFoundationItem r = wallFoundationsHashtable.get(iKey);
		if (r == null) {
			iKey = key+">1";
			r = wallFoundationsHashtable.get(iKey);
		}
		if (r == null) {
			iKey = key+">2";
			r = wallFoundationsHashtable.get(iKey);
		}

		return r;
	}

	public static String getWallFoundationHashAsString() {
		String returnString="";
		String wallFoundationItem;
		double percentage;
		for (String key:wallFoundationsHashtable.keySet()) {
			wallFoundationItem = wallFoundationsHashtable.get(key).wallFoundation;
			String tempString = key+"," + wallFoundationItem + ";";
			returnString += tempString;
		}
		return returnString;
	
	}

	public static void wallFoundationsInit() {
		
		int i = 0;
		String result = "";
		wallFoundationsHashtable.clear();
		
		while (i < MyConfig.defaultWallFoundations.length) {
			try {
				StringTokenizer st = new StringTokenizer(MyConfig.defaultWallFoundations[i], ",");
				String wallFoundationBlockKey = st.nextToken();
				String key = wallFoundationBlockKey;	
				StringTokenizer keySt = new StringTokenizer(wallFoundationBlockKey,">");
				String keyRegistryDomain = keySt.nextToken();
				if (!Block.REGISTRY.containsKey(new ResourceLocation(keyRegistryDomain)))  {
					result = "Regrowth: Block: " + keyRegistryDomain + " not in Forge Registry.  Mispelled?";
				} else {
					WallFoundationItem w = new WallFoundationItem(wallFoundationBlockKey);
					wallFoundationsHashtable.put (wallFoundationBlockKey, w);
					result = "Regrowth: Block: " + keyRegistryDomain + " wall foundation block accepted.";
				}
				
				if (MyConfig.aDebugLevel>0) {
					System.out.println(result);
				}					

				
			} catch (Exception e) {
				System.out.println("Regrowth Debug:  Bad Wall Foundation Config : " + MyConfig.defaultWallFoundations[i]);
			}
			i++;
		}

	}
	

	public static class WallFoundationItem {
		String wallFoundation;
		public WallFoundationItem(String wallFoundation) {
			this.wallFoundation = wallFoundation;
		}
	}
	
}
