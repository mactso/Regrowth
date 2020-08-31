package com.mactso.regrowth.config;


	import java.util.ArrayList;
	import java.util.Hashtable;
	import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

	public class WallBiomeDataManager {
		public static Hashtable<String, WallBiomeDataItem> wallBiomeDataHashtable = new Hashtable<>();
		private static String defaultRegrowthMobString = "regrowth:default";
		private static String defaultRegrowthMobKey = defaultRegrowthMobString;

		public static WallBiomeDataItem getWallBiomeDataItem (String key) {
			String iKey = key;
int dbg = 3;
			if (wallBiomeDataHashtable.isEmpty()) {
				wallBiomeDataInit();
			}

			WallBiomeDataItem r = wallBiomeDataHashtable.get(iKey);

			return r;
		}

		public static String getWallBiomeDataHashAsString() {
			String returnString="";
			int wallSize;
			BlockState wallTypeBlockState;
			for (String key:wallBiomeDataHashtable.keySet()) {
				wallSize = wallBiomeDataHashtable.get(key).wallSize;
				wallTypeBlockState = wallBiomeDataHashtable.get(key).getWallBlockState();
				String tempString = key+","+ wallSize +","+ wallTypeBlockState.toString()+";";
				returnString += tempString;
			}
			return returnString;
		
		}

		public static void wallBiomeDataInit() {
			
			List <String> dTL6464 = new ArrayList<>();
			List<Block> walls = BlockTags.WALLS.getAllElements();
			List<Block> fences = BlockTags.FENCES.getAllElements();
			
			int i = 0;
			String wallBiomeDataLine6464 = "";
			// Forge Issue 6464 patch.
			StringTokenizer st6464 = new StringTokenizer(MyConfig.defaultWallBiomeData6464, ";");
			while (st6464.hasMoreElements()) {
				wallBiomeDataLine6464 = st6464.nextToken().trim();
				if (wallBiomeDataLine6464.isEmpty()) continue;
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
					if (wallSize <= 32) wallSize = 32;
					if (wallSize > 80) wallSize = 80;
					BlockState wallBlockState = null;
					for (int v = 0; v< walls.size(); v++) {
						String wbs = walls.get(v).getBlock().getRegistryName().toString();
						if (wbs.equals(wallBlockString)) {
							wallBlockState = walls.get(v).getBlock().getDefaultState();
							break;
						}
					}
					if (wallBlockState == null) wallBlockState = Blocks.COBBLESTONE_WALL.getDefaultState();

					BlockState fenceBlockState = null;
					for (int v = 0; v< fences.size(); v++) {
						String fbs = fences.get(v).getBlock().getRegistryName().toString();
						if (fbs.equals(fenceBlockString)) {
							fenceBlockState = walls.get(v).getBlock().getDefaultState();
							break;
						}
					}
					if (fenceBlockState == null) fenceBlockState = Blocks.OAK_FENCE.getDefaultState();

					wallBiomeDataHashtable.put(key, new WallBiomeDataItem(wallSize, wallBlockState, fenceBlockState));

					//					int debug = 5;
//					Set<ResourceLocation> s = ForgeRegistries.BIOMES.getKeys();
					if (!modAndBiome.contentEquals("Regrowth:default") &&
						!modAndBiome.contentEquals("Regrowth:minimum") &&	
						!modAndBiome.contentEquals("minecraft:icy") &&	// TODO: Hack...
						!ForgeRegistries.BIOMES.containsKey(new ResourceLocation(modAndBiome))
					   )  {
						System.out.println("Regrowth Debug: Wall Biome Data: " + wallBlockString + " not in Forge Entity Type Registry.  Mispelled?");
					}
				} catch (Exception e) {
					System.out.println("Regrowth Debug:  Bad Wall Biome Data Config : " + MyConfig.defaultWallBiomeData[i]);
				}
				i++;
			}
			int breakpoint2 = 3;
		}

		public static class WallBiomeDataItem {
			int wallSize;
			BlockState wallBlockState;
			BlockState fenceBlockState;
			
			public WallBiomeDataItem(int wallSize, BlockState wallBlockState, BlockState fenceBlockState) {
				this.wallSize= wallSize;
				this.wallBlockState = wallBlockState;
				this.fenceBlockState = fenceBlockState;
			}

			public int getWallSize() {
				return wallSize;
			}
			public BlockState getWallBlockState() {
				return wallBlockState;
			}
			public BlockState getFenceBlockState() {
				return fenceBlockState;
			}
		}


}
