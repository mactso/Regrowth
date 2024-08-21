package com.mactso.regrowth.config;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class WallBiomeDataManager {
	private static Hashtable<String, WallBiomeDataItem> wallBiomeDataHashtable = new Hashtable<>();
	private static String defaultRegrowthMobString = "regrowth:default";
	private static String defaultRegrowthMobKey = defaultRegrowthMobString;
	private static BlockState DEFAULT_WALL_BLOCKSTATE = Blocks.COBBLESTONE_WALL.defaultBlockState();
	private static BlockState DEFAULT_FENCE_BLOCKSTATE = Blocks.OAK_FENCE.defaultBlockState();

	private static WallBiomeDataItem DEFAULT_WALL_ITEM = new WallBiomeDataItem(36, DEFAULT_WALL_BLOCKSTATE,
			DEFAULT_FENCE_BLOCKSTATE);

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
			r = DEFAULT_WALL_ITEM;
		}

		if (MyConfig.aDebugLevel > 1) {
			System.out.println("222 WallBiomeDataItem: " + iKey + " wall=" + r.getWallBlockState().getBlock().toString()
					+ "fence=" + r.getFenceBlockState().getBlock().toString() + ".");
		}
		return r;
	}

	public static String getWallBiomeDataHashAsString() {
		String returnString = "";
		int wallDiameter;
		BlockState wallTypeBlockState;
		for (String key : wallBiomeDataHashtable.keySet()) {
			wallDiameter = wallBiomeDataHashtable.get(key).wallDiameter;
			if (wallDiameter < 12) wallDiameter= 12;
			wallTypeBlockState = wallBiomeDataHashtable.get(key).getWallBlockState();
			String tempString = key + "," + wallDiameter + "," + wallTypeBlockState.toString() + ";";
			returnString += tempString;
		}
		return returnString;

	}

	public static void wallBiomeDataInit() {

		List<Block> wallBlocks = new ArrayList<>();
		for (Block b : ForgeRegistries.BLOCKS.tags().getTag(BlockTags.WALLS)) {
			wallBlocks.add(b);
		}

		if (!wallBlocks.iterator().hasNext()) {
			System.out.println("failed to get wallblocks this time- too early");
			return;
		}
		System.out.println("succeeded in loading wallblocks");

		List<Block> fenceBlocks = new ArrayList<>();
		for (Block b : ForgeRegistries.BLOCKS.tags().getTag(BlockTags.FENCES)) {
			fenceBlocks.add(b);
		}

		if (!fenceBlocks.iterator().hasNext()) {
			System.out.println("failed to get fence blocks");
			return;
		}
		System.out.println("succeeded in loading fence blocks");

		List<String> dTL6464 = new ArrayList<>();

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
				String wallDiameterString = st.nextToken();
				String wallBlockString = st.nextToken();
				String fenceBlockString = st.nextToken();
				int wallDiameter = validatedWallDiameter(Integer.parseInt(wallDiameterString.trim()));

				BlockState wallBlockState = DEFAULT_WALL_BLOCKSTATE;
				if (ForgeRegistries.BLOCKS.containsKey( ResourceLocation.parse(wallBlockString))) {
					@NotNull
					Optional<Holder<Block>> opt = ForgeRegistries.BLOCKS
							.getHolder(ResourceLocation.parse(wallBlockString));
					if (opt.isPresent()) {
						wallBlockState = opt.get().get().defaultBlockState();
					}
				}

				BlockState fenceBlockState = DEFAULT_FENCE_BLOCKSTATE;
				if (ForgeRegistries.BLOCKS.containsKey(ResourceLocation.parse(wallBlockString))) {
					@NotNull
					Optional<Holder<Block>> opt = ForgeRegistries.BLOCKS
							.getHolder(ResourceLocation.parse(fenceBlockString));
					if (opt.isPresent()) {
						fenceBlockState = opt.get().get().defaultBlockState();
					}
				}

				wallBiomeDataHashtable.put(key, new WallBiomeDataItem(wallDiameter, wallBlockState, fenceBlockState));
				
				if (!ForgeRegistries.BIOMES.isEmpty()  ) {
					int break3 = 4;
				}
				// odd bug: can't see extreme hills, mesa, or nether here but can elsewhere.
				if (    !ForgeRegistries.BIOMES.isEmpty()    
						&& !modAndBiome.contentEquals("Regrowth:default") && !modAndBiome.contentEquals("Regrowth:minimum")
						&& !modAndBiome.contentEquals("minecraft:icy") // TODO: Hack...
						&& !modAndBiome.contentEquals("minecraft:extreme_hills") // TODO: Hack...
						&& !modAndBiome.contentEquals("minecraft:mesa") // TODO: Hack... aka badlands
						&& !modAndBiome.contentEquals("minecraft:nether") // TODO: Hack... aks the_nether
						&& !ForgeRegistries.BIOMES.containsKey(ResourceLocation.parse(modAndBiome))) {
					System.out.println("Regrowth Debug: Wall Biome Data: " + key
							+ " not in Forge Biome Type Registry.  Mispelled?");
				}
			} catch (Exception e) {
				System.out.println("Regrowth Debug:  Bad Wall Biome Data Config : " + MyConfig.defaultWallBiomeData[i]);
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
			return (wallDiameter / 2) + 1;
		}

		public BlockState getWallBlockState() {
			return wallBlockState;
		}

		public BlockState getFenceBlockState() {
			return fenceBlockState;
		}
	}

}
