package com.mactso.regrowth.managers;

import java.util.Hashtable;
import java.util.StringTokenizer;

import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.utilities.MyUtilities;

/**
 * Manages Regrowth mod entities and their associated actions.
 * Provides initialization from config, lookup of mob info, and
 * serialization of mob data as a string.
 */

public class RegrowthEntitiesManager {
	public static Hashtable<String, RegrowthMobItem> regrowthMobHashtable = new Hashtable<>();
	public static String[] defaultRegrowthMobs;

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
		

		regrowthMobHashtable.clear();

		String oneMob = "";
		StringTokenizer tokenizedMobString = new StringTokenizer(MyConfig.getRegrowthMobsAndActions(), ";");
		while (tokenizedMobString.hasMoreElements()) {
			oneMob = tokenizedMobString.nextToken().trim();
			if (oneMob.isEmpty())
				continue;
			try {
				StringTokenizer st = new StringTokenizer(oneMob, ",");
				String modAndEntity = st.nextToken();
				String key = modAndEntity;
				String regrowthType = st.nextToken();
				String secondsString = st.nextToken();
				double seconds = Double.parseDouble(secondsString.trim());
				if (seconds <= 1.0) {
					seconds = 1.0;
				}
				regrowthMobHashtable.put(key, new RegrowthMobItem(regrowthType, seconds));
			} catch (Exception e) {
				MyUtilities.debugMsg(0, "Regrowth Debug:  Bad Mob Config : " + oneMob);
			}
		}
		
        MyUtilities.debugMsg(0, "Configured Mobs initialized. Valid mob count: " + regrowthMobHashtable.size());

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
