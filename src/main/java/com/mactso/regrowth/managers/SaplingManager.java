package com.mactso.regrowth.managers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SaplingManager {
	
	private static final Logger LOGGER = LogManager.getLogger();

	private static final Set<String> END_BIOMES = Set.of("minecraft:end_barrens", "minecraft:end_highlands",
			"minecraft:end_midlands", "minecraft:small_end_islands", "minecraft:the_end");
	private static final Set<String> NETHER_BIOMES = Set.of("minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:warped_forest",
			"minecraft:basalt_deltas", "minecraft:soul_sand_valley");
	public static final ResourceLocation NO_SAPLING = ResourceLocation.parse("regrowth:no_sapling");
	private static final Map<Biome, BlockState> BIOME_TO_SAPLING_STATE = new HashMap<>();
	private static final Map<ResourceLocation, ResourceLocation> BIOME_TO_SAPLING = new HashMap<>();
	private static final List<ResourceLocation> ALL_SAPLINGS = new ArrayList<>();

	/** Initialize both sapling and biome reports */
	public static void init(MinecraftServer server, Path configDir) {
		try {

			generateAllSaplingsReport(server, configDir);
			generateBiomeSaplingsConfig(server, configDir);
			readBiomeSaplingsConfig(server, configDir); // populate map after CSV is created

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// -------------------------------
	// Generate RegrowthAllSaplings.txt (vanilla + modded) if it doesn't exist
	// -------------------------------
	public static void generateAllSaplingsReport(MinecraftServer server, Path configDir) {
		try {
			Path out = configDir.resolve("RegrowthAllSaplings.txt");
			if (Files.exists(out))
				return;

			ALL_SAPLINGS.clear();

			// Use registryAccess to avoid relying on BuiltInRegistries directly
			Registry<Block> blockRegistry = server.registryAccess().registryOrThrow(Registries.BLOCK);
			Iterator<Map.Entry<ResourceKey<Block>, Block>> iter = blockRegistry.entrySet().iterator();

			while (iter.hasNext()) {
				Map.Entry<ResourceKey<Block>, Block> entry = iter.next();
				Block block = entry.getValue();
				if (block instanceof SaplingBlock) {
					ResourceLocation key = blockRegistry.getKey(block);
					ALL_SAPLINGS.add(key);
				}
			}

			// Sort alphabetically
			ALL_SAPLINGS.sort(Comparator.comparing(ResourceLocation::toString));

			// Make list unmodifiable to prevent accidental changes
			List<ResourceLocation> unmodifiableSaplings = Collections.unmodifiableList(new ArrayList<>(ALL_SAPLINGS));
			ALL_SAPLINGS.clear();
			ALL_SAPLINGS.addAll(unmodifiableSaplings);

			// Write to file
			List<String> lines = new ArrayList<>();
			for (ResourceLocation key : ALL_SAPLINGS) {
				lines.add(key.toString());
			}

			Files.write(out, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	// -------------------------------
	// Generate RegrowthBiomeSaplings.csv (default + modded detection) if it doesn't exist
	// -------------------------------
	public static void generateBiomeSaplingsConfig(MinecraftServer server, Path configDir) {
		try {

			// Access the registry
			RegistryAccess registryAccess = server.registryAccess();
			Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);

			Path out = configDir.resolve("RegrowthBiomeSaplings.csv");
			if (Files.exists(out))
				return;

			List<String> lines = new ArrayList<>();

			Iterator<Map.Entry<ResourceKey<Biome>, Biome>> iter = biomeRegistry.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<ResourceKey<Biome>, Biome> entry = iter.next();
				ResourceLocation biomeId = biomeRegistry.getKey(entry.getValue());
				String biomeName = biomeId.toString().toLowerCase();

				String sapling = determineDefaultSapling(biomeName);

				// Detect modded saplings heuristically
				// Check if biome name contains the base name of any registered sapling
				// Allows automatic assignment for most modded saplings
				if (!sapling.isEmpty()) {
					for (ResourceLocation saplingKey : ALL_SAPLINGS) {
						String path = saplingKey.getPath().toLowerCase();
						if (path.endsWith("_sapling"))
							path = path.substring(0, path.length() - "_sapling".length());
						if (biomeName.contains(path)) {
							sapling = saplingKey.toString();
							break;
						}
					}
				}

				lines.add(biomeId + "," + sapling);
			}

			Files.write(out, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	// -------------------------------
	// Read RegrowthBiomeSaplings.csv into map
	// -------------------------------
	public static void readBiomeSaplingsConfig(MinecraftServer server, Path configDir) {
	    Path csv = configDir.resolve("RegrowthBiomeSaplings.csv");
	    if (!Files.exists(csv))
	        return;

	    try {
	        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
	        BIOME_TO_SAPLING.clear();
	        BIOME_TO_SAPLING_STATE.clear();

	        Registry<Block> blockRegistry = server.registryAccess().registryOrThrow(Registries.BLOCK);
	        Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);

	        // NEW: Include line index
	        for (int i = 0; i < lines.size(); i++) {
	            String line = lines.get(i);

	            if (line.isBlank() || line.startsWith("#") || !line.contains(","))
	                continue;

	            String[] parts = line.split(",", 2);
	            if (parts.length != 2)
	                continue;

	            // NEW: safe parse of biome ResourceLocation with line number
	            ResourceLocation biomeRL;
	            try {
	                biomeRL = ResourceLocation.parse(parts[0].trim());
	            } catch (IllegalArgumentException ex) {
	                LOGGER.error(
	                        "Line {}: Invalid biome ResourceLocation '{}'. Skipping. Error: {}",
	                        i + 1, parts[0].trim(), ex.getMessage());
	                continue;
	            }

	            String saplingStr = parts[1].trim();

	            BlockState state;
	            ResourceLocation saplingRL;

	            if (saplingStr.isEmpty()) {
	                saplingRL = NO_SAPLING;
	                state = Blocks.AIR.defaultBlockState();
	            } else {

	                // NEW: safe parse of sapling ResourceLocation with line number
	                try {
	                    saplingRL = ResourceLocation.parse(saplingStr);
	                } catch (IllegalArgumentException ex) {
	                    LOGGER.error(
	                            "Line {}: Invalid sapling ResourceLocation '{}' for biome '{}'. Using AIR. Error: {}",
	                            i + 1, saplingStr, biomeRL, ex.getMessage());
	                    saplingRL = NO_SAPLING;
	                    state = Blocks.AIR.defaultBlockState();

	                    // still record biome → NO_SAPLING mapping
	                    BIOME_TO_SAPLING.put(biomeRL, saplingRL);
	                    continue;
	                }

	                state = blockRegistry.getOptional(saplingRL)
	                        .map(Block::defaultBlockState)
	                        .orElse(Blocks.AIR.defaultBlockState());
	            }

	            // Save mappings
	            BIOME_TO_SAPLING.put(biomeRL, saplingRL);

	            // Map all biome instances matching this ResourceLocation to the BlockState
	            biomeRegistry.entrySet().forEach(entry -> {
	                if (biomeRegistry.getKey(entry.getValue()).equals(biomeRL)) {
	                    BIOME_TO_SAPLING_STATE.put(entry.getValue(), state);
	                }
	            });
	        }

	    } catch (IOException ex) {
	        ex.printStackTrace();
	    }
	}

	// -------------------------------
	// Default vanilla biome → sapling mapping
	// -------------------------------
	private static String determineDefaultSapling(String biomeIdString) {
		String biomeName = biomeIdString.toLowerCase();

		// Only exact matches for known End biomes
		// end biomes default to no saplings.
		if (END_BIOMES.contains(biomeIdString)) {
			return SaplingManager.NO_SAPLING.toString();
		}
		// Only exact matches for known End biomes
		// end biomes default to no saplings.
		if (NETHER_BIOMES.contains(biomeIdString)) {
			return SaplingManager.NO_SAPLING.toString();
		}

		// Ocean substring check remains for modded compatibility
		if (biomeName.contains("mushroom_fields") || biomeName.contains("deep_dark") || biomeName.contains("ocean")
				|| biomeName.contains("void")) {

			return SaplingManager.NO_SAPLING.toString();
		}

		if (biomeName.contains("birch"))
			return "minecraft:birch_sapling";
		if (biomeName.contains("taiga") || biomeName.contains("snow"))
			return "minecraft:spruce_sapling";
		if (biomeName.contains("jungle"))
			return "minecraft:jungle_sapling";
		if (biomeName.contains("savanna"))
			return "minecraft:acacia_sapling";
		if (biomeName.contains("ice_spikes"))
			return "minecraft:spruce_sapling";

		return "minecraft:oak_sapling";
	}

	// -------------------------------
	// Public API
	// -------------------------------

	public static BlockState getBiomeSaplingBlockState(MinecraftServer server, Biome biome) {
		if (biome == null)
			return Blocks.AIR.defaultBlockState();

		// Return cached BlockState if available
		if (BIOME_TO_SAPLING_STATE.containsKey(biome)) {
			return BIOME_TO_SAPLING_STATE.get(biome);
		}

		return Blocks.AIR.defaultBlockState();

	}

}