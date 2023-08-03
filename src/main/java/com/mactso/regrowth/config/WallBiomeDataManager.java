package com.mactso.regrowth.config;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.mactso.regrowth.utility.Utility;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class WallBiomeDataManager {
	public static class WallBiomeDataItem {
		int wallDiameter;
		BlockState wallBlockState;
		BlockState fenceBlockState;

		public WallBiomeDataItem(int wallRadius, BlockState wallBlockState, BlockState fenceBlockState) {
			this.wallDiameter = wallRadius;
			this.wallBlockState = wallBlockState;
			this.fenceBlockState = fenceBlockState;
		}

		public BlockState getFenceBlockState() {
			return fenceBlockState;
		}

		public BlockState getWallBlockState() {
			return wallBlockState;
		}
		
		public int getWallDiameter() {
			return wallDiameter;
		}

		public int getWallRadius() {
			return (wallDiameter/2)+1;
		}
	}
	private static Hashtable<String, WallBiomeDataItem> wallBiomeDataHashtable = new Hashtable<>();
	private static BlockState DEFAULT_WALL_BLOCKSTATE = Blocks.COBBLESTONE_WALL.defaultBlockState();

	private static BlockState DEFAULT_FENCE_BLOCKSTATE = Blocks.OAK_FENCE.defaultBlockState();

	private static WallBiomeDataItem DEFAULT_WALL_ITEM= 
			new WallBiomeDataItem(36, DEFAULT_WALL_BLOCKSTATE, DEFAULT_FENCE_BLOCKSTATE);

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

	public static WallBiomeDataItem getWallBiomeDataItem(String key) {
		String iKey = key;

		if (wallBiomeDataHashtable.isEmpty()) {
			wallBiomeDataInit();
		}
		WallBiomeDataItem r = wallBiomeDataHashtable.get(iKey);

		if (r == null) {
			if (MyConfig.getDebugLevel() > 0) {
				System.out.println("Error! Requested wall has unknown Biome:" + key + ".");
			}
			r = DEFAULT_WALL_ITEM;
		}
		
		if (MyConfig.getDebugLevel() > 1) {
			System.out.println("222 WallBiomeDataItem: "+ iKey +" wall=" + r.getWallBlockState().getBlock().toString() + "fence=" + r.getFenceBlockState().getBlock().toString() + ".");
		}
		return r;
	}

	private static int validatedWallDiameter(int wallDiameter) {
		if (wallDiameter <= 24)
			wallDiameter = 24;
		if (wallDiameter > 80)
			wallDiameter = 80;
		return wallDiameter;
	}

	public static void wallBiomeDataInit() {

		
        // TODO: line 64 might be wrong method "Named" is different.
    	Named<Block> walls = BuiltInRegistries.BLOCK.getOrCreateTag(BlockTags.WALLS);
    	if (!walls.iterator().hasNext()) {  // TODO change to size in fabric and forge
        	System.out.println("failed to get walls all tags ");
        	return;
    	}
    	System.out.println("succeeded in loading walls all tags");
    	
        Named<Block> fences = BuiltInRegistries.BLOCK.getOrCreateTag(BlockTags.FENCES);
    	if (!fences.iterator().hasNext()) {
        	System.out.println("failed to get fences all tags ");
        	return;
    	}        
    	System.out.println("succeeded in loading fences all tags");
    	
		wallBiomeDataHashtable.clear();

		String oneLine = "";
		StringTokenizer tokenizedMobString = new StringTokenizer(MyConfig.getWallblockList(), ";");
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

				Iterator<Holder<Block>> wallIter = walls.iterator();
				while ( wallIter.hasNext()) {
					Holder<Block> w = wallIter.next();
				    String wbs = Utility.getResourceLocationString(w.value());
					if (wbs.equals(wallBlockString)) {
						wallBlockState = w.value().defaultBlockState();
						break;
					}
				}
				
				BlockState fenceBlockState = DEFAULT_FENCE_BLOCKSTATE;
				Iterator<Holder<Block>> fenceIter = fences.iterator();
				while ( fenceIter.hasNext()) {
					Holder<Block> f = fenceIter.next();
				    String fbs = Utility.getResourceLocationString(f.value());
					if (fbs.equals(fenceBlockString)) {
						fenceBlockState = f.value().defaultBlockState();
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

}
