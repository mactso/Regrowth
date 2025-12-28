package com.mactso.regrowth.actions;

import org.jetbrains.annotations.Nullable;

import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

//-----------------------
//  Action Utilities calculate and count things but don't modify the world.
//-----------------------
public class ActionUtilities {


	/**
	 * Returns the actual block position.
	 * If the entity's feet are slightly inside the lower full block (> ~0.2) 
	 * such as partial blocks like dirtpath, farmland, soulsand
	 * then return an adjusted block position 1 higher.
	 */
	public static BlockPos getAdjustedPos(LivingEntity le) {
	    BlockPos basePos = le.blockPosition();

	    double fracY = le.getY() - Math.floor(le.getY());
		if (fracY > 0.001) {
	        return basePos.above();
	    }

	    return basePos;
	}

	public static BlockState getAdjustedFootBlockState(LivingEntity le) {
		BlockPos pos = getAdjustedPos(le);
		return le.level().getBlockState(pos);
	}

	public static BlockState getAdjustedGroundBlockState(Entity e) {
		return e.level().getBlockState(e.blockPosition().below(getAdjustedY(e)));
	}

	public static int getAdjustedY(Entity e) {
		if (e.getY() == e.blockPosition().getY())
			return 1;
		return 0;
	}
	
	public static int getAbsVX(Entity e, BlockPos gVMPPos) {
		int absvx = (int) Math.abs(getVX(e, gVMPPos));
		return absvx;
	}

	public static int getAbsVX(BlockPos pos, BlockPos gVMPPos) {
		int absvx = (int) Math.abs(getVX(pos, gVMPPos));
		return absvx;
	}

	public static int getAbsVZ(Entity e, BlockPos gVMPPos) {
		return (int) Math.abs(getVZ(e, gVMPPos));
	}

	public static int getAbsVZ(BlockPos pos, BlockPos gVMPPos) {
		return (int) Math.abs(getVZ(pos, gVMPPos));
	}

	public static int getVX(Entity e, BlockPos gVMPPos) {
		return (int) (e.getX() - gVMPPos.getX());
	}

	public static int getVX(BlockPos pos, BlockPos gVMPPos) {
		return (int) (pos.getX() - gVMPPos.getX());
	}

	public static int getVZ(Entity e, BlockPos gVMPPos) {
		return (int) (e.getZ() - gVMPPos.getZ());
	}

	public static int getVZ(BlockPos pos, BlockPos gVMPPos) {
		return (int) (pos.getZ() - gVMPPos.getZ());
	}

	/**
	 * Returns a predictable random source based on the entity's block position.
	 * Any entity at the same (x, y, z) will get the same sequence of numbers.
	 * First value is discarded for better distribution. Uses fixed primes to mix coords.
	 *
	 * @param le the entity whose position seeds the random
	 * @return RandomSource seeded by block coordinates
	 */
	private static long PRIME_X = 32413L;
	private static long PRIME_Y = 54317L;
	private static long PRIME_Z = 65521L;

	static RandomSource createPredictableRandom(LivingEntity le) {
	    BlockPos pos = le.blockPosition();
	    long x = pos.getX();
	    long y = pos.getY();
	    long z = pos.getZ();
	    

	    // Mix coordinates into a single long
	    long seed = x * PRIME_X ^ y * PRIME_Y ^ z * PRIME_Z;

	    RandomSource rand = RandomSource.create();
	    rand.setSeed(seed);
	    rand.nextDouble(); // discard first value for predictability
	    return rand;
	}

	static BlockState getBiomeRoadBlockType(String localBiome) {
		return Blocks.DIRT_PATH.defaultBlockState();
	}

	// this routine returns a count of the searchBlock immediately orthogonal to
	// BlockPos, exiting if a max count is exceeded.
	public static int countBlocksOrthogonalBB(Block searchBlock, int maxCount, Level level, BlockPos bPos, int boundY) {
		return ActionUtilities.countBlocksOrthogonalBB(searchBlock, maxCount, level, bPos, 0 - boundY, 0 + boundY);
	}

	public static int countBlocksOrthogonalBB(Block searchBlock, int maxCount, Level w, BlockPos bPos, int lowerBoundY,
			int upperBoundY) {
		int count = 0;
		MutableBlockPos mPos = new MutableBlockPos();

		for (int j = lowerBoundY; j <= upperBoundY; j++) {

			mPos.set(bPos.getX() + 1, bPos.getY() + j, bPos.getZ()); // East
			if (w.getBlockState(mPos).getBlock() == searchBlock && ++count >= maxCount)
				return count;

			mPos.set(bPos.getX() - 1, bPos.getY() + j, bPos.getZ()); // West
			if (w.getBlockState(mPos).getBlock() == searchBlock && ++count >= maxCount)
				return count;

			mPos.set(bPos.getX(), bPos.getY() + j, bPos.getZ() - 1); // North

			if (w.getBlockState(mPos).getBlock() == searchBlock && ++count >= maxCount)
				return count;

			mPos.set(bPos.getX(), bPos.getY() + j, bPos.getZ() + 1); // South
			if (w.getBlockState(mPos).getBlock() == searchBlock && ++count >= maxCount)
				return count;
		}

		return count;
	}

	/** Count blocks below the reference position */
	public static int countBlocksBelow(Class<? extends Block> blockClass, Level world, BlockPos pos, int maxCount,
			int boxSize, int heightBelow) {
		if (heightBelow < 0) {
			throw new IllegalArgumentException("heightBelow must be >= 0: " + heightBelow);
		}

		int yBoxBottom = -heightBelow;
		int yBoxTop = 0;
		return countBlocksInVerticalBox(blockClass, world, pos, maxCount, boxSize, yBoxBottom, yBoxTop);
	}

	/** Count blocks above the reference position */
	public static int countBlocksAbove(Class<? extends Block> blockClass, Level world, BlockPos pos, int maxCount,
			int boxSize, int heightAbove) {
		if (heightAbove < 0) {
			throw new IllegalArgumentException("heightAbove must be >= 0: " + heightAbove);
		}

		int yBoxBottom = 1;
		int yBoxTop = heightAbove;
		return countBlocksInVerticalBox(blockClass, world, pos, maxCount, boxSize, yBoxBottom, yBoxTop);
	}

	public static int countBlocksInVerticalBox(Class<? extends Block> blockClass, Level world, BlockPos pos,
			int maxCount, int boxSize, int yStart, int yEnd) {

		int count = 0;
		MutableBlockPos mPos = new MutableBlockPos();

		for (int yOff = yStart; yOff <= yEnd; yOff++) {
			for (int xOff = -boxSize; xOff <= boxSize; xOff++) {
				for (int zOff = -boxSize; zOff <= boxSize; zOff++) {
					mPos.set(pos.getX() + xOff, pos.getY() + yOff, pos.getZ() + zOff);
					if (blockClass.isInstance(world.getBlockState(mPos).getBlock())) {
						if (++count >= maxCount)
							return count;
					}
				}
			}
		}

		return count;
	}

	public static int countBlocksinBox(Block searchBlock, int maxCount, ServerLevel serverLevel, BlockPos bPos,
			int boxSize) {
		return ActionUtilities.countBlocksInBox(searchBlock, maxCount, serverLevel, bPos, boxSize, boxSize); // "square"
																												// box
																												// subcase
	}

	public static int countBlocksInBox(Block searchBlock, int maxCount, ServerLevel serverLevel, BlockPos bPos,
			int boxSize, int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;

		MutableBlockPos mPos = new MutableBlockPos();
		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
				for (int dy = minY; dy <= maxY; dy++) {
					mPos.set(dx, dy, dz);
					if (serverLevel.getBlockState(mPos).getBlock() == searchBlock) {
						if (++count >= maxCount)
							return count;
					}
				}
			}
		}

		MyUtilities.debugMsg(2, bPos, MyUtilities.getResourceLocationString(serverLevel, searchBlock) + " Sparse count:" + count
				+ " countBlockBB ");

		return count;
	}

	public static int countBlocksInBox(Class<? extends Block> searchBlock, int maxCount, Level w, BlockPos bPos,
			int boxSize, int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;

		MutableBlockPos mPos = new MutableBlockPos();

		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
				for (int dy = minY; dy <= maxY; dy++) {
					mPos.set(dx, dy, dz);
					if (searchBlock.isInstance(w.getBlockState(mPos).getBlock())) {
						if (++count >= maxCount) {
							return count;
						}
					}
				}
			}
		}

		MyUtilities.debugMsg(2, bPos, searchBlock.getSimpleName() + " Sparse count:" + count + " countBlockBB ");

		return count;
	}

	// this picks a random spot along the length of a wall.
	static int pickRandomWallSpot(ServerLevel serverLevel, int wallRadius) {
		int rndRadius = serverLevel.getRandom().nextInt(wallRadius - 2) + 2 + 1;
		if (serverLevel.getRandom().nextBoolean()) {
			rndRadius = -wallRadius;
		}
		return rndRadius;
	}

	static int getVillagerTorchPlaceOdds(Villager ve) {
		
		int placeTorchOdds = ve.getVillagerData().getLevel() * 10;

		if (VillagerActions.isVillagerProfession(ve, VillagerProfession.FISHERMAN) ||
			    VillagerActions.isVillagerProfession(ve, VillagerProfession.BUTCHER)) {
			    return placeTorchOdds + 4;
			}

			if (VillagerActions.isVillagerProfession(ve, VillagerProfession.TOOLSMITH) ||
			    VillagerActions.isVillagerProfession(ve, VillagerProfession.WEAPONSMITH)) {
			    return placeTorchOdds + 8;
			}

			if (VillagerActions.isVillagerProfession(ve, VillagerProfession.ARMORER)) {
			    return placeTorchOdds + 16;
			}
			
		return placeTorchOdds;
	}

	/**
	 * Find Ocean Floor.  If it is a Coral Block, return true;
	 * Otherwise, returns null.
	 */
	static @Nullable BlockPos findTheCoralBlockBelow(ServerLevel serverLevel, BlockPos pos) {
		
	    int x = pos.getX();
	    int z = pos.getZ();
	    int testY = serverLevel.getHeight(Types.OCEAN_FLOOR, x, z);  // returns the water block above the bottom.

	    BlockPos theCoralBlockPos = BlockPos.containing(x, testY-1, z); // get block below the water block
        if (serverLevel.getBlockState(theCoralBlockPos).getBlock() instanceof CoralBlock) 
	        return theCoralBlockPos;
        
	    return null;
	}

	static String debugSaplingInfo(ServerLevel serverLevel, Biome biome, BlockState sapling) {
	    // Get registries using your safe helpers
	    Registry<Biome> biomeRegistry = getBiomeRegistrySafe(serverLevel.getServer(), Registries.BIOME);
	    Registry<Block> blockRegistry = getBlockRegistrySafe(serverLevel.getServer(), Registries.BLOCK);

	    ResourceLocation biomeKey = null;
	    ResourceLocation saplingKey = null;

	    if (biomeRegistry != null) {
	        biomeKey = biomeRegistry.getKey(biome);
	    }

	    if (blockRegistry != null && sapling != null) {
	        saplingKey = blockRegistry.getKey(sapling.getBlock());
	    }
	
	
		return String.format("Biome=%s, Sapling=%s", biomeKey != null ? biomeKey : "unknown_biome",
				saplingKey != null ? saplingKey : "unknown_sapling");
	}
	
	public static Registry<Block> getBlockRegistrySafe(MinecraftServer server, ResourceKey<Registry<Block>> key) {
	    try {
	        // registryOrThrow returns Registry<T> directly in 1.21.1+
	        return server.registryAccess().registryOrThrow(key);
	    } catch (IllegalStateException e) {
	        // Registry not found
	        MyUtilities.debugMsg(0, "Block registry not found: " + key.location());
	        return null;
	    }
	}
	
	public static Registry<Biome> getBiomeRegistrySafe(MinecraftServer server, ResourceKey<Registry<Biome>> key) {
	    try {
	        // registryOrThrow returns Registry<T> directly in 1.21.1+
	        return server.registryAccess().registryOrThrow(key);
	    } catch (IllegalStateException e) {
	        // Registry not found
	        MyUtilities.debugMsg(0, "Biome registry not found: " + key.location());
	        return null;
	    }
	}
	


    /**
     * Safely gets the torch block from MyConfig.
     * 
     * Returns null if any step fails or if the config string is empty.
     * Logs a debug message on the first failure only.
     */

	private static boolean torchBlockFailureLogged = false;
    
    public static Block getTorchBlockFromConfig() {
        String torchBlockString = MyConfig.getTorchBlock();
        
		if (!MyUtilities.isStringValid(torchBlockString))
			return null;

        ResourceLocation id = ResourceLocation.tryParse(torchBlockString);
        if (id == null) {
            logTorchFailureOnce("Failed to parse torch block string: " + torchBlockString);
            return null;
        }

        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == null || block.defaultBlockState().is(BlockTags.AIR)) {
            logTorchFailureOnce("Torch block '" + torchBlockString + "' is not a Block or resolves to AIR.");
            return null;
        }

        return block;
    }

    private static void logTorchFailureOnce(String message) {
        if (!torchBlockFailureLogged) {
            torchBlockFailureLogged = true;
            MyUtilities.debugMsg(0, message);
        }
    }



    /**
     * Safely gets the player wall control block from MyConfig.
     * 
     * Returns null if any step fails or if the config string is empty.
     * Logs a debug message on the first failure only.
     */
    private static boolean playerWallBlockFailureLogged = false;
    public static Block getPlayerWallControlBlockFromConfig() {
        String wallBlockString = MyConfig.getPlayerWallControlBlock();

        if (!MyUtilities.isStringValid(wallBlockString))
            return null;

        ResourceLocation id = ResourceLocation.tryParse(wallBlockString);
        if (id == null) {
            logPlayerWallFailureOnce("Failed to parse player wall block string: " + wallBlockString);
            return null;
        }

        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == null || block.defaultBlockState().is(BlockTags.AIR)) {
            logPlayerWallFailureOnce("Player wall block '" + wallBlockString + "' is not a Block or resolves to AIR.");
            return null;
        }

        return block;
    }

    private static void logPlayerWallFailureOnce(String message) {
        if (!playerWallBlockFailureLogged) {
            playerWallBlockFailureLogged = true;
            MyUtilities.debugMsg(0, message);
        }
    }
    
}
