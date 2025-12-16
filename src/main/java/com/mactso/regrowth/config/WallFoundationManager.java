package com.mactso.regrowth.config;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class WallFoundationManager {

	private static final Set<Block> validFoundations = new HashSet<>();
	private static volatile boolean initialized = false;

	/** Initialize the set of valid foundation blocks from the config */
	public static void init() {
		validFoundations.clear();

		for (String entry : MyConfig.defaultWallFoundationsArray) {
			try {
				ResourceLocation id = ResourceLocation.parse(entry);
				Block block = BuiltInRegistries.BLOCK.get(id);
				if (block != null && block != Blocks.AIR) {
					validFoundations.add(block);
				} else {
					System.out.println("Regrowth Debug: Wall Foundation Block not found or invalid: " + entry);
				}
			} catch (Exception e) {
				System.out.println("Regrowth Debug: Bad Wall Foundation Config: " + entry);
			}
		}

		initialized = true;
	}

	/** Check if a block state is a valid foundation */
	public static boolean isValid(Block block) {
		if (!initialized) {
			init();
		}
		return validFoundations.contains(block);
	}
}
