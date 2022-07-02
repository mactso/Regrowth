package com.mactso.regrowth.utility;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeCategory;
import net.minecraftforge.coremod.api.ASMAPI;

public class Utility {
	private static Field fieldBiomeCategory = null;
	private static final Logger LOGGER = LogManager.getLogger();
	static {
		try {
			String name = ASMAPI.mapField("f_47442_");
			fieldBiomeCategory = Biome.class.getDeclaredField(name);
			fieldBiomeCategory.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("Unexpected Reflection Failure set Biome.biomeCategory accessible");
		}
	}
	public static BiomeCategory getBiomeCategory(Biome b) {
		BiomeCategory bc = BiomeCategory.PLAINS;
		try {
			bc = (BiomeCategory) fieldBiomeCategory.get(b);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return bc;
	}
}
