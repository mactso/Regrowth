package com.mactso.regrowth.config;

import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import com.mactso.regrowth.utility.Utility;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.registry.Registry;


public class WallBiomeDataManager {
	private static Hashtable<String, WallBiomeDataItem> wallBiomeDataHashtable = new Hashtable<>();
	private static BlockState DEFAULT_WALL_BLOCKSTATE = Blocks.COBBLESTONE_WALL.getDefaultState();
	private static BlockState DEFAULT_FENCE_BLOCKSTATE = Blocks.OAK_FENCE.getDefaultState();

	private static WallBiomeDataItem DEFAULT_WALL_ITEM= 
			new WallBiomeDataItem(36, DEFAULT_WALL_BLOCKSTATE, DEFAULT_FENCE_BLOCKSTATE);

	public static WallBiomeDataItem getWallBiomeDataItem(String key) {
		String iKey = key;

		if (wallBiomeDataHashtable.isEmpty()) {
			wallBiomeDataInit();
		}
		WallBiomeDataItem r = wallBiomeDataHashtable.get(iKey);

		if (r == null) {
			if (ModConfigs.getDebugLevel() > 0) {
				System.out.println("Error! Requested wall has unknown Biome:" + key + ".");
			}
			r = DEFAULT_WALL_ITEM;
		}
		
		if (ModConfigs.getDebugLevel() > 1) {
			System.out.println("222 WallBiomeDataItem: "+ iKey +" wall=" + r.getWallBlockState().getBlock().toString() + "fence=" + r.getFenceBlockState().getBlock().toString() + ".");
		}
		return r;
	}

	public static String getWallBiomeDataHashAsString() {
		String returnString = "";
		int wallDiameter;
		BlockState wallTypeBlockState;
		for (String key : wallBiomeDataHashtable.keySet()) {
			wallDiameter = wallBiomeDataHashtable.get(key).wallDiameter;
			wallTypeBlockState = wallBiomeDataHashtable.get(key).getWallBlockState();
			String tempString = key + "," + wallDiameter + "," + wallTypeBlockState.toString() + ";";
			returnString += tempString;
		}
		return returnString;

	}

	public static void wallBiomeDataInit() {

		
        // TODO: line 64 might be wrong method "Named" is different.
		List<Block> walls = null;
		List<Block> fences = null;

		try {
			walls = BlockTags.WALLS.values();
	    	if (walls.isEmpty()) {
	        	System.out.println("failed to get walls all tags ");
	        	return;
	    	}
	    	System.out.println("succeeded in loading walls all tags");
	    	
	        fences = BlockTags.FENCES.values();
	    	if (fences.isEmpty()) {
	        	System.out.println("failed to get fences all tags ");
	        	return;
	    	}        
	    	System.out.println("succeeded in loading fences all tags");
		}  catch (Exception e) {
			Utility.debugMsg(1, "BlockTags for WALLS or FENCES not ready yet.");
			return;
		}
    	
		wallBiomeDataHashtable.clear();

		String oneLine = "";
		StringTokenizer tokenizedMobString = new StringTokenizer(ModConfigs.getWallblockList(), ";");
		while (tokenizedMobString.hasMoreElements()) {
			oneLine = tokenizedMobString.nextToken().trim();
			if (oneLine.isEmpty()) continue;
			try {
				StringTokenizer st = new StringTokenizer(oneLine, ",");
				String modAndBiome = st.nextToken();
				String key = modAndBiome;
				String wallDiameterString = st.nextToken();
				String wallBlockString = st.nextToken();
				String fenceBlockString = st.nextToken();
				int wallDiameter = validatedWallDiameter(Integer.parseInt(wallDiameterString.trim()));
				
				BlockState wallBlockState = DEFAULT_WALL_BLOCKSTATE;
				for (Block w : walls) {
				    String wbs = Utility.getResourceLocationString(w);
					if (wbs.equals(wallBlockString)) {
						wallBlockState = w.getDefaultState();
						break;
					}
				}
				
				BlockState fenceBlockState = DEFAULT_FENCE_BLOCKSTATE;
				for (Block f : fences) {
				    String fbs = Utility.getResourceLocationString(f);
					if (fbs.equals(fenceBlockString)) {
						fenceBlockState = f.getDefaultState();
						break; 
					}
				}

				wallBiomeDataHashtable.put(key, new WallBiomeDataItem(wallDiameter, wallBlockState, fenceBlockState));

				// odd bug: can't see extreme hills, mesa, or nether here but can elsewhere.
				if (!modAndBiome.contentEquals("Regrowth:default") 
						&& !modAndBiome.contentEquals("Regrowth:minimum")
						&& !modAndBiome.contentEquals("minecraft:icy") // TODO: Hack...
						&& !modAndBiome.contentEquals("minecraft:extreme_hills") // TODO: Hack...
						&& !modAndBiome.contentEquals("minecraft:mesa") // TODO: Hack... aka badlands
						&& !modAndBiome.contentEquals("minecraft:nether")) { // TODO: Hack... aks the_nether
						// TODO: Check Dynamic Registries.
				}
			} catch (Exception e) {
				System.out.println("Regrowth Debug:  Bad Wall Biome Data Config : " + oneLine);
			}

		}

	}

	private static int validatedWallDiameter(int wallDiameter) {
		if (wallDiameter <= 24)
			wallDiameter = 24;
		if (wallDiameter > 80)
			wallDiameter = 80;
		return wallDiameter;
	}

	public static class WallBiomeDataItem {
		int wallDiameter;
		BlockState wallBlockState;
		BlockState fenceBlockState;

		public WallBiomeDataItem(int wallRadius, BlockState wallBlockState, BlockState fenceBlockState) {
			this.wallDiameter = wallRadius;
			this.wallBlockState = wallBlockState;
			this.fenceBlockState = fenceBlockState;
		}

		public int getWallDiameter() {
			return wallDiameter;
		}

		public int getWallRadius() {
			return (wallDiameter/2)+1;
		}
		
		public BlockState getWallBlockState() {
			return wallBlockState;
		}

		public BlockState getFenceBlockState() {
			return fenceBlockState;
		}
	}

}
