package com.mactso.regrowth.config;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.mactso.regrowth.utility.Utility;

import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
	            Identifier id = Identifier.parse(entry);
	            Optional<Reference<Block>> optBlock = BuiltInRegistries.BLOCK.get(id);

	            if (optBlock.isPresent() && optBlock.get().value() != Blocks.AIR) {
	                validFoundations.add(optBlock.get().value());
	            } else {
	                Utility.debugMsg(0, "Regrowth Debug: Wall Foundation Block not found or invalid: " + entry);
	            }
	        } catch (Exception e) {
	            Utility.debugMsg(0, "Regrowth Debug: Bad Wall Foundation Config: " + entry);
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
