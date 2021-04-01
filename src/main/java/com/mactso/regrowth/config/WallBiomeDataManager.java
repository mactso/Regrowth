package com.mactso.regrowth.config;

import java.util.Hashtable;
import java.util.StringTokenizer;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;


public class WallBiomeDataManager {
	private static Hashtable<String, WallBiomeDataItem> wallBiomeDataHashtable = new Hashtable<>();


	public static WallBiomeDataItem getWallBiomeDataItem(String key) {
		String iKey = key;

		if (wallBiomeDataHashtable.isEmpty()) {
			wallBiomeDataInit();
		}
		WallBiomeDataItem r = wallBiomeDataHashtable.get(iKey);
		if (MyConfig.aDebugLevel > 1) {
			System.out.println("222 WallBiomeDataItem: "+ iKey +" wall=" + r.getWallBlockState().getBlock().toString()  + ".");
		}
		if (r == null) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println("Error!  Unknown Biome in Wall Biome Data:" + key + ".");
			}
			r = wallBiomeDataHashtable.get("minecraft:plains");
		}
		return r;
	}

	public static String getWallBiomeDataHashAsString() {
		String returnString = "";
		int wallSize;
		IBlockState wallTypeBlockState;
		for (String key : wallBiomeDataHashtable.keySet()) {
			wallSize = wallBiomeDataHashtable.get(key).wallSize;
			wallTypeBlockState = wallBiomeDataHashtable.get(key).getWallBlockState();
			String tempString = key + "," + wallSize + "," + wallTypeBlockState.toString() + ";";
			returnString += tempString;
		}
		return returnString;

	}

	public static void wallBiomeDataInit() {

		int i = 0;
		wallBiomeDataHashtable.clear();
		while (i < MyConfig.defaultWallBiomeData.length) {
			try {
				StringTokenizer st = new StringTokenizer(MyConfig.defaultWallBiomeData[i], ",");
				String modAndBiome = st.nextToken();
				String key = modAndBiome;
				String wallSizeString = st.nextToken();
				int wallSize = Integer.parseInt(wallSizeString.trim());
				if (wallSize <= 32)
					wallSize = 32;
				if (wallSize > 80)
					wallSize = 80;

				String wallBlockKey = st.nextToken();

				IBlockState wallBlockState = null;
				if (!ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(wallBlockKey)))  {
					System.out.println("Regrowth Debug: Wall Foundation Block: " + wallBlockKey + " not in Forge Entity Type Registry.  Mispelled?  Missing semicolon? ");
					wallBlockState = Blocks.COBBLESTONE_WALL.getDefaultState();
				} else {
					wallBlockState = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(wallBlockKey)).getDefaultState();
				}
				
				wallBiomeDataHashtable.put(key, new WallBiomeDataItem(wallSize, wallBlockState ));


			} catch (Exception e) {
				System.out.println("Regrowth Debug:  Bad Wall Biome Data Config : " + MyConfig.defaultWallBiomeData[i]);
			}
			i++;
		}

	}

	public static class WallBiomeDataItem {
		int wallSize;
		IBlockState wallBlockState;
		IBlockState fenceBlockState;

		public WallBiomeDataItem(int wallSize, IBlockState wallBlockState) {
			this.wallSize = wallSize;
			this.wallBlockState = wallBlockState;
			this.fenceBlockState = fenceBlockState;
		}

		public int getWallDiameter() {
			return wallSize;
		}

		public IBlockState getWallBlockState() {
			return wallBlockState;
		}

	}

}
