package com.mactso.regrowth.utility;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.regrowth.config.ModConfigs;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Category;

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

	public static Category getBiomeCategory(Biome b) {
		Category bc = Category.PLAINS;
		try {
			bc = (Category) fieldBiomeCategory.get(b);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return bc;
	}

	// support for any color chattext
	public static void sendChat(PlayerEntity p, String chatMessage, TextColor color) {
		Text component = new LiteralText(chatMessage);
		component.getStyle().withColor(color);
		p.sendSystemMessage(component, p.getUuid());
	}

	// support for any color, optionally bold text.
	public static void sendBoldChat(PlayerEntity p, String chatMessage, TextColor color) {
		Text component = new LiteralText(chatMessage);

		component.getStyle().withBold(true);
		component.getStyle().withColor(color);

		p.sendSystemMessage(component, p.getUuid());
	}

	public static void debugMsg (int level, BlockPos pos, String dMsg) {
	debugMsg(level, " ("+pos.getX()+","+pos.getY()+","+pos.getZ()+"): " + dMsg);
	}
	
	public static void debugMsg(int level, String dMsg) {
		if (ModConfigs.getDebugLevel() > level - 1) {
			LOGGER.warn("L" + level + ":" + dMsg);
		}
	}
	

}
