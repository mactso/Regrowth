package com.mactso.regrowth.managers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Manages wall and fence block configurations for biomes. Loads data from
 * config, validates against registries, and provides access to
 * WallBiomeDataItem for each biome.
 */

public class WallBiomeDataManager {

	private static final Map<String, WallBiomeDataItem> wallBiomeDataMap = new HashMap<>();

	private static BlockState DEFAULT_WALL_BLOCKSTATE = Blocks.COBBLESTONE_WALL.defaultBlockState();
	private static BlockState DEFAULT_FENCE_BLOCKSTATE = Blocks.OAK_FENCE.defaultBlockState();
	private static WallBiomeDataItem DEFAULT_BIOME_WALL_ITEM = new WallBiomeDataItem(36, DEFAULT_WALL_BLOCKSTATE,
			DEFAULT_FENCE_BLOCKSTATE);

	public static WallBiomeDataItem getWallBiomeDataItem(MinecraftServer server, String key) {

		String iKey = key.toLowerCase(Locale.ROOT);

		if (wallBiomeDataMap.isEmpty()) {
			wallBiomeDataInit(server);
			if (wallBiomeDataMap.isEmpty()) {
				return DEFAULT_BIOME_WALL_ITEM;
			}
		}
		
		WallBiomeDataItem wbdi = wallBiomeDataMap.getOrDefault(iKey, DEFAULT_BIOME_WALL_ITEM);

		if (MyConfig.getDebugLevel() > 1) {
    String msg = "222 WallBiomeDataItem: " + iKey 
            + " wall=" + wbdi.getWallBlockState().getBlock().toString()
            + " fence=" + wbdi.getFenceBlockState().getBlock().toString() + ".";
    MyUtilities.debugMsg(0, msg);
		}
		return wbdi;
	}

	public static String getWallBiomeDataHashAsString() {
		String returnString = "";
		int wallLength;
		BlockState wallTypeBlockState;
		for (String key : wallBiomeDataMap.keySet()) {
			wallLength = wallBiomeDataMap.get(key).wallLength;
			if (wallLength < 12)
				wallLength = 12;
			wallTypeBlockState = wallBiomeDataMap.get(key).getWallBlockState();
			String tempString = key + "," + wallLength + "," + wallTypeBlockState.toString() + ";";
			returnString += tempString;
		}
		return returnString;

	}

	public static void wallBiomeDataInit(MinecraftServer server) {
		wallBiomeDataMap.clear();

		RegistryAccess registryAccess = server.registryAccess();
		Registry<Block> blockRegistry = MyUtilities.getRegistrySafe(registryAccess, Registries.BLOCK);

		String biomeWallblockList = MyConfig.getWallBiomeBlocks();
		if (biomeWallblockList == null || biomeWallblockList.isEmpty()) {
			return;
		}

		String[] entries = biomeWallblockList.split(";");
		for (String entry : entries) {
			entry = entry.trim();
			if (entry.isEmpty())
				continue;

			String[] parts = entry.split(",");
			if (parts.length != 4) {
				MyUtilities.debugMsg(0, "Invalid entry format (Not four parts): " + entry);
				continue;
			}

			String biomeName = parts[0].trim();
			int wallLength = parseWallLength(parts[1].trim(), entry);
			if (wallLength < 0)
				continue;

			
			if (!validateBiomeCategory(biomeName))
				continue;

			Block wallBlock = parseWallBlock(parts[2].trim(), biomeName, blockRegistry);
			if (wallBlock == null)
				continue;

			Block fenceBlock = parseFenceBlock(parts[3].trim(), biomeName, blockRegistry);
			if (fenceBlock == null)
				continue;

			WallBiomeDataItem item = new WallBiomeDataItem(wallLength, wallBlock.defaultBlockState(),
					fenceBlock.defaultBlockState());
			wallBiomeDataMap.put(biomeName.toLowerCase(Locale.ROOT), item);
		}
	}

	// ------------------------ Helper Methods ------------------------

	private static boolean validateBiomeCategory(String rawCategory) {
	    String category = normalizeBiomeCategory(rawCategory);

	    if (!MyUtilities.VALID_BIOME_CATEGORIES.contains(category)) {
	        MyUtilities.debugMsg(0,
	            "Invalid biome category in wall biome config: '" + rawCategory +
	            "'. Valid values are: " + MyUtilities.VALID_BIOME_CATEGORIES);
	        return false;
	    }

	    return true;
	}
	
	private static String normalizeBiomeCategory(String raw) {
	    if (raw == null || raw.isBlank())
	        return MyUtilities.NONE;

	    String s = raw.trim().toLowerCase(Locale.ROOT);

	    // Strip legacy namespace (minecraft:, regrowth:, etc.)
	    int colon = s.indexOf(':');
	    if (colon >= 0) {
	        s = s.substring(colon + 1);
	    }

	    return s;
	}

	private static int parseWallLength(String diameterStr, String entry) {
		try {
			int diameter = Integer.parseInt(diameterStr);
			return Math.max(24, Math.min(diameter, 80));
		} catch (NumberFormatException e) {
			MyUtilities.debugMsg(0, "Invalid diameter for entry: " + entry);
			return -1;
		}
	}


	private static Block parseWallBlock(String blockName, String biomeName, Registry<Block> blockRegistry) {
		Block block = blockRegistry.getOptional(ResourceLocation.parse(blockName)).orElse(null);
		if (block == null) {
			MyUtilities.debugMsg(0, "Invalid WallBlock for biome " + biomeName + ": " + blockName);
			return null;
		}
		if (!(block instanceof WallBlock)) {
			MyUtilities.debugMsg(0, "Block is not a WallBlock for biome " + biomeName + ": " + blockName);
			return null;
		}
		return block;
	}

	private static Block parseFenceBlock(String blockName, String biomeName, Registry<Block> blockRegistry) {
		Block block = blockRegistry.getOptional(ResourceLocation.parse(blockName)).orElse(null);
		if (block == null) {
			MyUtilities.debugMsg(0, "Invalid FenceBlock for biome " + biomeName + ": " + blockName);
			return null;
		}
		if (!(block instanceof FenceBlock)) {
			MyUtilities.debugMsg(0, "Block is not a FenceBlock for biome " + biomeName + ": " + blockName);
			return null;
		}
		return block;
	}

	public static class WallBiomeDataItem {
		int wallLength;
		BlockState wallBlockState;
		BlockState fenceBlockState;

		public WallBiomeDataItem(int wallLength, BlockState wallBlockState, BlockState fenceBlockState) {
			this.wallLength = wallLength;
			this.wallBlockState = wallBlockState;
			this.fenceBlockState = fenceBlockState;
		}

		public int getWallLength() {
			return wallLength;
		}

		public BlockState getFenceBlockState() {
			return fenceBlockState;
		}

		public int getWallRadius() {
			return (wallLength / 2) + 1;
		}
		
		public int getTorchSpacing () {
			return getWallRadius() / 4;
		}

		public BlockState getWallBlockState() {
			return wallBlockState;
		}

	}

}