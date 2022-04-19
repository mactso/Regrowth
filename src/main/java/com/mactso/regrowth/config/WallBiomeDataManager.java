package com.mactso.regrowth.config;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList.Named;

public class WallBiomeDataManager {
	private static Hashtable<String, WallBiomeDataItem> wallBiomeDataHashtable = new Hashtable<>();
	private static String defaultRegrowthMobString = "regrowth:default";
	private static String defaultRegrowthMobKey = defaultRegrowthMobString;
	private static BlockState DEFAULT_WALL_BLOCKSTATE = Blocks.COBBLESTONE.getDefaultState();
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
				System.out.println("Error!  Villager in unknown Biome:" + key + ".");
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

		List<String> dTL6464 = new ArrayList<>();

//		List<Block> walls = new ArrayList<>();
//        List<Block> fences = new ArrayList<>();
        
//        Iterable<Holder<Block>> walls;
        // TODO: line 69 might be wrong method
    	Named<Block> walls = Registry.BLOCK.getOrCreateEntryList(BlockTags.WALLS);
    	if (!walls.iterator().hasNext()) {
        	System.out.println("failed to get walls all tags ");
        	return;
    	}
    	System.out.println("succeeded in loading walls all tags");

//		Iterable<Holder<Block>> fences;
        // TODO: line 69 might be wrong method
        Named<Block> fences = Registry.BLOCK.getOrCreateEntryList(BlockTags.FENCES);
    	if (!fences.iterator().hasNext()) {
        	System.out.println("failed to get fences all tags ");
        	return;
    	}        
    	System.out.println("succeeded in loading fences all tags");

    	int i = 0;
		String wallBiomeDataLine6464 = "";
		// Forge Issue 6464 patch.
		StringTokenizer st6464 = new StringTokenizer(ModConfigs.defaultWallBiomeData6464, ";");
		while (st6464.hasMoreElements()) {
			wallBiomeDataLine6464 = st6464.nextToken().trim();
			if (wallBiomeDataLine6464.isEmpty())
				continue;
			dTL6464.add(wallBiomeDataLine6464);
			i++;
		}

		ModConfigs.defaultWallBiomeData = dTL6464.toArray(new String[i]);

		i = 0;
		wallBiomeDataHashtable.clear();
		while (i < ModConfigs.defaultWallBiomeData.length) {
			try {
				StringTokenizer st = new StringTokenizer(ModConfigs.defaultWallBiomeData[i], ",");
				String modAndBiome = st.nextToken();
				String key = modAndBiome;
				String wallDiameterString = st.nextToken();
				String wallBlockString = st.nextToken();
				String fenceBlockString = st.nextToken();
				int wallDiameter = validatedWallDiameter(Integer.parseInt(wallDiameterString.trim()));
				BlockState wallBlockState = DEFAULT_WALL_BLOCKSTATE;
				for (RegistryEntry<Block> w : walls) {
				    String wbs = w.value().getRegistryEntry().toString();
					if (wbs.equals(wallBlockString)) {
						wallBlockState = w.value().getDefaultState();
						break; // TODO debug this.
					}
				}
				
				BlockState fenceBlockState = DEFAULT_FENCE_BLOCKSTATE;
				for (RegistryEntry<Block> f : fences) {
				    String fbs = f.value().getRegistryEntry().toString();
					if (fbs.equals(fenceBlockString)) {
						fenceBlockState = f.value().getDefaultState();
						break; // TODO debug this.
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
						
						// TODO:						&& !ForgeRegistries.BIOMES.containsKey(new ResourceLocation(modAndBiome))) {
					System.out.println("Regrowth Debug: Wall Biome Data: " + key
							+ " not in Forge Entity Type Registry.  Mispelled?");
				}
			} catch (Exception e) {
				System.out.println("Regrowth Debug:  Bad Wall Biome Data Config : " + ModConfigs.defaultWallBiomeData[i]);
			}
			i++;
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
