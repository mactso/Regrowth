// 15.2 - 1.0.0.0 regrowth
package com.mactso.regrowth.config;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class RegrowthEntitiesManager {
	public static Hashtable<String, RegrowthMobItem> regrowthMobHashtable = new Hashtable<>();
	private static String defaultRegrowthMobString = "hbm:default";
	private static String defaultRegrowthMobKey = defaultRegrowthMobString;

	public static RegrowthMobItem getRegrowthMobInfo(String key) {
		String iKey = key;

		if (regrowthMobHashtable.isEmpty()) {
			regrowthMobInit();
		}

		RegrowthMobItem r = regrowthMobHashtable.get(iKey);

		return r;
	}

	public static String getRegrowthHashAsString() {
		String returnString="";
		String regrowthType;
		double percentage;
		for (String key:regrowthMobHashtable.keySet()) {
			regrowthType = regrowthMobHashtable.get(key).regrowthAction;
			percentage = regrowthMobHashtable.get(key).regrowthEventSeconds;
			String tempString = key+","+regrowthType+","+percentage+";";
			returnString += tempString;
		}
		return returnString;
	
	}

	public static void regrowthMobInit() {
		
		List <String> dTL6464 = new ArrayList<>();
		
		int i = 0;
		String regrowthMobLine6464 = "";
		// Forge Issue 6464 patch.
		StringTokenizer st6464 = new StringTokenizer(MyConfig.defaultRegrowthMobs6464, ";");
		while (st6464.hasMoreElements()) {
			regrowthMobLine6464 = st6464.nextToken().trim();
			if (regrowthMobLine6464.isEmpty()) continue;
			dTL6464.add(regrowthMobLine6464);  
			i++;
		}

		MyConfig.defaultRegrowthMobs = dTL6464.toArray(new String[i]);

		i = 0;
		regrowthMobHashtable.clear();
		while (i < MyConfig.defaultRegrowthMobs.length) {
			try {
				StringTokenizer st = new StringTokenizer(MyConfig.defaultRegrowthMobs[i], ",");
				String modAndEntity = st.nextToken();
				String key = modAndEntity;
				String regrowthType = st.nextToken();
				String secondsString = st.nextToken();
				double seconds = Double.parseDouble(secondsString.trim());
				if (seconds <= 1.0) {
					seconds = 1.0;
				}

				regrowthMobHashtable.put(key, new RegrowthMobItem(regrowthType, seconds));
				if (!modAndEntity.contentEquals("hbm:default") &&
				    !ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(modAndEntity))
				   )  {
					System.out.println("Regrowth Debug: Mob: " + modAndEntity + " not in Forge Entity Type Registry.  Mispelled?");
				}
			} catch (Exception e) {
				System.out.println("Regrowth Debug:  Bad Mob Config : " + MyConfig.defaultRegrowthMobs[i]);
			}
			i++;
		}

	}

	public static class RegrowthMobItem {
		double regrowthEventSeconds;
		String regrowthAction;
		
		public RegrowthMobItem(String regrowthType, double regrowthEventSeconds) {
			this.regrowthAction =  regrowthType;
			this.regrowthEventSeconds = regrowthEventSeconds;
		}

		public String getRegrowthActions() {
			return regrowthAction.toLowerCase();
		}
		public double getRegrowthEventSeconds() {
			return regrowthEventSeconds;
		}

	}


}
