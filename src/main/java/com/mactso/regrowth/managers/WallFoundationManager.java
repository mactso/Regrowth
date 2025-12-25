package com.mactso.regrowth.managers;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Manages valid wall foundation blocks.
 * Loads block list from config, validates against registries,
 * and provides lookup for foundation validity.
 */
public class WallFoundationManager {

    private static final Set<Block> validFoundations = new HashSet<>();

    /** Initialize the set of valid foundation blocks from the config */
    public static void wallFoundationsInit(MinecraftServer server) {
        validFoundations.clear();

        Registry<Block> blockRegistry = getRegistrySafe(server.registryAccess(), Registries.BLOCK);

        String wallFoundationsList = MyConfig.getWallFoundationsList();
        if (wallFoundationsList == null || wallFoundationsList.isEmpty()) {
            MyUtilities.debugMsg(0, "Wall Foundations list is empty, using defaults");
            return;
        }

        String[] entries = wallFoundationsList.split(";");
        for (String entry : entries) {
            entry = entry.trim();
            if (entry.isEmpty()) continue;

            Block block = parseFoundationBlock(entry, blockRegistry);
            if (block != null) {
                validFoundations.add(block);
            }
        }

        MyUtilities.debugMsg(0, "Wall Foundations initialized. Valid blocks count: " + validFoundations.size());
    }

    // ------------------------ Helper Methods ------------------------

    private static Block parseFoundationBlock(String entry, Registry<Block> blockRegistry) {
            try {
                ResourceLocation id = ResourceLocation.parse(entry);
            Optional<Block> optBlock = blockRegistry.getOptional(id);
            if (optBlock.isEmpty()) return null;

            Block block = optBlock.get();
            if (block == null || block == Blocks.AIR) {
                    MyUtilities.debugMsg(0, "Wall Foundation Block not found or invalid: " + entry);
                return null;
                }
            return block;
            } catch (Exception e) {
                MyUtilities.debugMsg(0, "Bad Wall Foundation Config: " + entry);
            return null;
            }
        }

	public static <T> Registry<T> getRegistrySafe(RegistryAccess access, ResourceKey<Registry<T>> key) {
	    try {
	        return access.registryOrThrow(key);
	    } catch (IllegalStateException e) {
	        MyUtilities.debugMsg(0, "Registry not found: " + key.location());
	        return null;
	    }
	}

    /** Check if a block is a valid foundation */
    public static boolean isValid(Block block) {
        return validFoundations.contains(block);
    }

    /** Return a copy of all valid foundations */
    public static Set<Block> getAllValidFoundations() {
        return new HashSet<>(validFoundations);
    }
}