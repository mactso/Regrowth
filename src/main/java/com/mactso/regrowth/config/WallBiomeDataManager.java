package com.mactso.regrowth.config;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class WallBiomeDataManager {
	private static Hashtable<String, WallBiomeDataItem> wallBiomeDataHashtable = new Hashtable<>();



	public static WallBiomeDataItem getWallBiomeDataItem(String key) {
		String iKey = key;

		if (wallBiomeDataHashtable.isEmpty()) {
			wallBiomeDataInit();
		}
		WallBiomeDataItem r = wallBiomeDataHashtable.get(iKey);

		if (r == null) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println("Error!  Villager in unknown Biome:" + key + ".");
			}
			r = wallBiomeDataHashtable.get("minecraft:plains");
		}
		
		if (MyConfig.aDebugLevel > 1) {
			System.out.println("222 WallBiomeDataItem: "+ iKey +" wall=" + r.getWallBlockState().getBlock().toString() + "fence=" + r.getFenceBlockState().getBlock().toString() + ".");
		}
		return r;
	}

	public static String getWallBiomeDataHashAsString() {
		String returnString = "";
		int wallSize;
		BlockState wallTypeBlockState;
		for (String key : wallBiomeDataHashtable.keySet()) {
			wallSize = wallBiomeDataHashtable.get(key).wallSize;
			wallTypeBlockState = wallBiomeDataHashtable.get(key).getWallBlockState();
			String tempString = key + "," + wallSize + "," + wallTypeBlockState.toString() + ";";
			returnString += tempString;
		}
		return returnString;

	}

	public static void wallBiomeDataInit() {

		List<String> dTL6464 = new ArrayList<>();

		List<Block> walls = new ArrayList<>();
        List<Block> fences = new ArrayList<>();
        try {
            walls = BlockTags.WALLS.getValues();
        	System.out.println("succeeded in loading walls all tags");
        }
        catch (Exception e) {
        	System.out.println("failed to get walls all tags ");
        	return;
        }
        try {
            fences = BlockTags.FENCES.getValues();
        	System.out.println("succeeded in loading fences all tags");
        }
        catch (Exception e) {
        	System.out.println("failed to load fences all tags");
        	return;
        }
        
		int i = 0;
		String wallBiomeDataLine6464 = "";
		// Forge Issue 6464 patch.
		StringTokenizer st6464 = new StringTokenizer(MyConfig.defaultWallBiomeData6464, ";");
		while (st6464.hasMoreElements()) {
			wallBiomeDataLine6464 = st6464.nextToken().trim();
			if (wallBiomeDataLine6464.isEmpty())
				continue;
			dTL6464.add(wallBiomeDataLine6464);
			i++;
		}

		MyConfig.defaultWallBiomeData = dTL6464.toArray(new String[i]);

		i = 0;
		wallBiomeDataHashtable.clear();
		while (i < MyConfig.defaultWallBiomeData.length) {
			try {
				StringTokenizer st = new StringTokenizer(MyConfig.defaultWallBiomeData[i], ",");
				String modAndBiome = st.nextToken();
				String key = modAndBiome;
				String wallSizeString = st.nextToken();
				String wallBlockString = st.nextToken();
				String fenceBlockString = st.nextToken();
				int wallSize = Integer.parseInt(wallSizeString.trim());
				if (wallSize <= 32)
					wallSize = 32;
				if (wallSize > 80)
					wallSize = 80;
				BlockState wallBlockState = null;
				for (int v = 0; v < walls.size(); v++) {
					String wbs = walls.get(v).getRegistryName().toString();
					if (wbs.equals(wallBlockString)) {
						wallBlockState = walls.get(v).defaultBlockState();
						break;
					}
				}
				if (wallBlockState == null) {
					wallBlockState = Blocks.COBBLESTONE_WALL.defaultBlockState();
				}
				
				BlockState fenceBlockState = null;
				for (int v = 0; v < fences.size(); v++) {
					String fbs = fences.get(v).getRegistryName().toString();
					if (fbs.equals(fenceBlockString)) {
						fenceBlockState = fences.get(v).defaultBlockState();
						break;
					}
				}
				if (fenceBlockState == null)
					fenceBlockState = Blocks.OAK_FENCE.defaultBlockState();

				wallBiomeDataHashtable.put(key, new WallBiomeDataItem(wallSize, wallBlockState, fenceBlockState));

				// odd bug: can't see extreme hills, mesa, or nether here but can elsewhere.
				if (!modAndBiome.contentEquals("Regrowth:default") 
						&& !modAndBiome.contentEquals("Regrowth:minimum")
						&& !modAndBiome.contentEquals("minecraft:icy") // TODO: Hack...
						&& !modAndBiome.contentEquals("minecraft:extreme_hills") // TODO: Hack...
						&& !modAndBiome.contentEquals("minecraft:mesa") // TODO: Hack...
						&& !modAndBiome.contentEquals("minecraft:nether")  // TODO: Hack...
						&& !ForgeRegistries.BIOMES.containsKey(new ResourceLocation(modAndBiome))) {
					System.out.println("Regrowth Debug: Wall Biome Data: " + key
							+ " not in Forge Entity Type Registry.  Mispelled?");
				}
			} catch (Exception e) {
				System.out.println("Regrowth Debug:  Bad Wall Biome Data Config : " + MyConfig.defaultWallBiomeData[i]);
			}
			i++;
		}

	}

	public static class WallBiomeDataItem {
		int wallSize;
		BlockState wallBlockState;
		BlockState fenceBlockState;

		public WallBiomeDataItem(int wallSize, BlockState wallBlockState, BlockState fenceBlockState) {
			this.wallSize = wallSize;
			this.wallBlockState = wallBlockState;
			this.fenceBlockState = fenceBlockState;
		}

		public int getWallDiameter() {
			return wallSize;
		}

		public BlockState getWallBlockState() {
			return wallBlockState;
		}

		public BlockState getFenceBlockState() {
//			Block b= fenceBlockState.getBlock();
			return fenceBlockState;
		}
	}

}
