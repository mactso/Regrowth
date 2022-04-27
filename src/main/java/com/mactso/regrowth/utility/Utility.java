package com.mactso.regrowth.utility;


import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.regrowth.config.ModConfigs;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Category;

public class Utility {
	private static Field fieldBiomeCategory = null;
	private static final Logger LOGGER = LogManager.getLogger();
	static {
		try {
			String name = "field_9329";  // see mappings.jar
			fieldBiomeCategory = Biome.class.getDeclaredField(name);
			fieldBiomeCategory.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("Unexpected Reflection Failure set Biome.category accessible");
		}
		if (fieldBiomeCategory == null) {
			try {
				String name = "category";  // see mappings.jar
				fieldBiomeCategory = Biome.class.getDeclaredField(name);
				fieldBiomeCategory.setAccessible(true);
			} catch (Exception e) {
				LOGGER.error("Development Biome field 'category' not found.");
			}
			
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
	
	public static String getResourceLocationString(BlockState blockState) {
		return getResourceLocationString(blockState.getBlock());
	}
	
	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(Block block) {
		return block.getRegistryEntry().registryKey().getValue().toString();
	}

	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(Item item) {
		return item.getRegistryEntry().registryKey().getValue().toString();
	}

	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(Entity entity) {
		return entity.getType().getRegistryEntry().registryKey().getValue().toString();
	}

	public static String getResourceLocationString(World world) {
		return world.getRegistryKey().getValue().toString();
	}

}
