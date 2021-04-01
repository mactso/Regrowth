// 15.2 - 1.0.0.0 regrowth
package com.mactso.regrowth.config;

import java.util.Hashtable;
import java.util.StringTokenizer;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class RegrowthEntitiesManager {
	public static Hashtable<String, RegrowthMobItem> regrowthMobHashtable = new Hashtable<>();
	
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

		int i = 0;
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
				if (
				    !ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(modAndEntity))
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
