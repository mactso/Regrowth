package com.mactso.regrowth.config;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import com.mactso.regrowth.utility.Utility;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

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
		String returnString = "";
		String regrowthType;
		double percentage;
		for (String key : regrowthMobHashtable.keySet()) {
			regrowthType = regrowthMobHashtable.get(key).regrowthAction;
			percentage = regrowthMobHashtable.get(key).regrowthEventSeconds;
			String tempString = key + "," + regrowthType + "," + percentage + ";";
			returnString += tempString;
		}
		return returnString;

	}

	public static void regrowthMobInit() {

		// Parse the default Regrowth Mobs string
		List<String> mobLines = new ArrayList<>();
		for (String line : MyConfig.defaultRegrowthMobs6464.split(";")) {
		    line = line.trim();
		    if (!line.isEmpty()) {
		        mobLines.add(line);
		    }
		}
		MyConfig.defaultRegrowthMobs = mobLines.toArray(new String[0]);

		// Clear previous data
		regrowthMobHashtable.clear();

		// Process each mob
		for (String mobConfig : MyConfig.defaultRegrowthMobs) {
			try {
				String[] parts = mobConfig.split(",");
				if (parts.length < 3) {
					Utility.debugMsg(0, "Regrowth Debug: Bad Mob Config: " + mobConfig);
					continue;
				}

				String modAndEntity = parts[0];
				String key = modAndEntity;
				String regrowthType = parts[1];
				double seconds = Double.parseDouble(parts[2].trim());
				if (seconds <= 1.0)
					seconds = 1.0;

				regrowthMobHashtable.put(key, new RegrowthMobItem(regrowthType, seconds));

				// Skip check for default
				if (!modAndEntity.equals("hbm:default")) {
					Identifier id = null;
					try {
						id = Identifier.parse(modAndEntity);
					} catch (Exception e) {
						Utility.debugMsg(0, "Regrowth Debug: Invalid Identifier: " + modAndEntity);
						continue;
					}

					Optional<EntityType<?>> optEntityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
					if (optEntityType.isEmpty()) {
					    Utility.debugMsg(0, "Regrowth Debug: Mob not found in registry: " + modAndEntity);
					};
					if (optEntityType.isEmpty()) {
						Utility.debugMsg(0,"Regrowth Debug: Mob not found in registry: " + modAndEntity);
					}

				}

			} catch (Exception e) {
				Utility.debugMsg(0,"Regrowth Debug: Bad Mob Config Line: " + mobConfig);
			}
		}
	}

	public static class RegrowthMobItem {
		double regrowthEventSeconds;
		String regrowthAction;

		public RegrowthMobItem(String regrowthType, double regrowthEventSeconds) {
			this.regrowthAction = regrowthType;
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
