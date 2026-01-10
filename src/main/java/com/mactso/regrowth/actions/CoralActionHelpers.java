package com.mactso.regrowth.actions;

import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;


/**
 * Provides helper methods for actions that modify the world or entities.
 * Includes coral-specific operations as well as a few coral-related tests.
 */

public class CoralActionHelpers {

	static int MAXIMUM_CORALFAN_DENSITY = 3;

	static int MAX_SEAGRASS_DENSITY = 3;

	// currently a dead method because it's preconditions were not met where it was.
	// if in a good biome and then 
	// if a search for a coralblock pos instead finds ocean floor or wood planks, then use this to 
	// grow seagrass on it if the current seagrass isn't too dense.
	
	public static void tryPlaceSeaGrassOnOceanFloor(LivingEntity le, ServerLevel serverLevel, BlockPos theCoralBlockPos) {
	
		BlockState bs = serverLevel.getBlockState(theCoralBlockPos);
	
		if (!ActionTests.isOceanFloorBlock(bs)) {
			return;
		}
	
		BlockState tBs = serverLevel.getBlockState(theCoralBlockPos.above());
		if (!(tBs.is(Blocks.WATER)))
			return;
	
		int count = 0;
		for (BlockPos oPos : BlockPos.betweenClosed(theCoralBlockPos.offset(-2, 0, -2),
				theCoralBlockPos.offset(2, 0, 2))) {
			if (serverLevel.getBlockState(oPos).is(Blocks.SEAGRASS)) {
				if (++count >= MAX_SEAGRASS_DENSITY)
					return;
			}
		}
	
		serverLevel.setBlockAndUpdate(theCoralBlockPos.above(), Blocks.SEAGRASS.defaultBlockState());
	
	}

	public static void tryPlaceSeaPickleOnCoral(LivingEntity le, ServerLevel serverLevel, BlockPos theCoralBlockPos, RandomSource pRand) {
	
		// Don't place sea pickle if too bright.
		int brightness = serverLevel.getBrightness(LightLayer.BLOCK, theCoralBlockPos);
		if (brightness > 3) { // note, water reduces light by 2 per manhatten block
			return;
		}
		// Check that the coral block is valid
		BlockState coralBlock = serverLevel.getBlockState(theCoralBlockPos);
		if (!(coralBlock.is(BlockTags.CORAL_BLOCKS))) {
			return; // Only place on coral blocks
		}
	
		// Random number of pickles: 1-4
		int pickleCount = 1 + pRand.nextInt(2) + pRand.nextInt(2);
	
		// Place the sea pickle block
		BlockState seaPickleState = Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, pickleCount);
		serverLevel.setBlock(theCoralBlockPos.above(), seaPickleState, 3);
	
		// Optional: play placement sound
		serverLevel.playSound(
			    null, // null = send only to nearby players
			    theCoralBlockPos,
			    SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
			    SoundSource.BLOCKS,
			    0.95f,
			    1.0f
			);
	
	}

	static void tryGrowCoralBlock(ActionContext rgCtx, BlockPos theCoralBlockPos, RandomSource pRand) {
	
	
		LivingEntity le = rgCtx.livingEntity();
		ServerLevel serverLevel = rgCtx.serverLevel();
	
		// Early exit if coral growth preconditions fail
		if (!CoralActionHelpers.isCoralCanGrowHere(le, serverLevel, pRand, theCoralBlockPos)) {
			return;
		}
	
		int maxCoral = 3;
	
		// Example: count up to maxCoral blocks in a 3x3 horizontal area, 1 block above
		// the coral root block.
		int coralCount = ActionUtilities.countBlocksAbove(CoralBlock.class, // block type
				serverLevel, // world
				theCoralBlockPos, // reference position
				maxCoral, // max count
				1, // box size (horizontal radius)
				1 // yBelow (how far below to check)
		);
	
		if (coralCount >= maxCoral)
			return;
	
		int ew = pRand.nextInt(3) - 1; // -1, 0, 1
		int ns = pRand.nextInt(3) - 1; // -1, 0, 1
		BlockPos newPos = theCoralBlockPos.above().offset(ew, 0, ns);
	
		if (serverLevel.getBlockState(newPos).getBlock() != Blocks.WATER)
			return;
	
		// Get the coral block here
		BlockState theCoralBlock = serverLevel.getBlockState(theCoralBlockPos);
	
		serverLevel.setBlockAndUpdate(newPos, theCoralBlock);
		serverLevel.playSound(le, newPos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.AMBIENT, 1.9f, 1.0f);
	
		if (rgCtx.doDebug())
			MyUtilities.debugMsg(2, newPos, "CORAL: " + MyUtilities.getIdentifierString(le) + " new block at " + newPos);
	}

	/**
	 * Attempts to place a coral fan adjacent to or on top of a coral block.
	 * Only places fans if water is present and density limits are respected.
	 *
	 * @param serverLevel the server level
	 * @param coralBlockPos the base coral block position
	 * @param pRand predictable random source
	 */
	static void tryPlaceCoralFanOnCoral(ServerLevel serverLevel, BlockPos coralBlockPos, RandomSource pRand) {
		
	    CoralActionHelpers.tryPlaceHorizontalFan(serverLevel, coralBlockPos, pRand);
	
	    int oceanFloorY = serverLevel.getHeight(Types.OCEAN_FLOOR, coralBlockPos.getX(), coralBlockPos.getZ());
	    if (coralBlockPos.getY() - oceanFloorY == 5) {
	        CoralActionHelpers.tryPlaceTopFan(serverLevel, coralBlockPos, pRand);
	    }
	}

	/** Place a coral fan on a horizontal adjacent block if water is present and density allows. */
	static void tryPlaceHorizontalFan(ServerLevel serverLevel, BlockPos coralBlockPos, RandomSource rand) {
		
	    if (!CoralActionHelpers.canPlaceFan(serverLevel, coralBlockPos)) return;
	
	    Direction dir = Direction.from2DDataValue(rand.nextInt(4));
	    BlockPos fanPos = coralBlockPos.relative(dir);
	    if (serverLevel.getBlockState(fanPos).getBlock() != Blocks.WATER) return;
	
	    BlockState fanState = MobActions.coralfans[rand.nextInt(MobActions.coralfans.length)]
	            .defaultBlockState()
	            .setValue(CoralWallFanBlock.FACING, dir);
	
	    serverLevel.setBlockAndUpdate(fanPos, fanState);
	    serverLevel.playSound(null, coralBlockPos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
	            SoundSource.AMBIENT, 1.9f, 1.0f);
	}

	/** Place a coral fan directly on top of the coral block if water is present and density allows. */
	static void tryPlaceTopFan(ServerLevel serverLevel, BlockPos coralBlockPos, RandomSource pRand) {
		
	    if (!CoralActionHelpers.canPlaceFan(serverLevel, coralBlockPos)) return;
	
	    BlockPos topPos = coralBlockPos.above();
	    if (serverLevel.getBlockState(topPos).getBlock() != Blocks.WATER) return;
	
	    BlockState topFan = MobActions.coralfans[pRand.nextInt(MobActions.coralfans.length)]
	            .defaultBlockState()
	            .setValue(CoralWallFanBlock.FACING, Direction.UP);
	
	    serverLevel.setBlockAndUpdate(topPos, topFan);
	}

	static void tryDecorateCoralArea(LivingEntity le, ServerLevel serverLevel, BlockPos theCoralBlockPos,
			RandomSource pRand) {
	

		if (pRand.nextDouble() < 0.03)
			tryPlaceSeaPickleOnCoral(le, serverLevel, theCoralBlockPos,pRand);
	
		// Attempt to place a coral fan adjacent to the existing coral block
		if (pRand.nextDouble() < 0.5)
			tryPlaceCoralFanOnCoral(serverLevel, theCoralBlockPos, pRand);
	}

	/** Returns true if the number of coral fans around the given block is below the maximum density. */
	static boolean canPlaceFan(ServerLevel serverLevel, BlockPos coralBlockPos) {
		
	    int count = 0;
	    for (BlockPos oPos : BlockPos.betweenClosed(coralBlockPos.offset(-2, 0, -2),
	            coralBlockPos.offset(2, 0, 2))) {
	        if (serverLevel.getBlockState(oPos).getBlock() instanceof CoralFanBlock) {
	            if (++count >= MAXIMUM_CORALFAN_DENSITY) return false;
	        }
	    }
	    return true;
	}

	/**
	 * Returns true if the given biome is suitable for coral growth.
	 * Excludes cold and frozen oceans, allows all other biomes.
	 * Any other biome allowed so players can build aquariums.
	 */
	static boolean isGoodCoralBiome(ServerLevel serverLevel, BlockPos aPos) {
	    Holder<Biome> biome = serverLevel.getBiome(aPos);
	    return !(biome.is(Biomes.COLD_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN)
	             || biome.is(Biomes.FROZEN_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN));
	}

	/**
	 * Determines if coral can grow from a given position.
	 * Returns false if the target is not a coral block or too far above solid ground.
	 * Only allows growth if underlying blocks are solid and non-protected.
	 *
	 * @return true if coral can safely grow here, false otherwise.
	 */

	static boolean isCoralCanGrowHere(LivingEntity le, ServerLevel serverLevel, RandomSource pRand, BlockPos theCoralBlockPos) {
	
		// don't grow unless the target block is a CoralBlock
		BlockState bs = serverLevel.getBlockState(theCoralBlockPos);
		if (!(bs.getBlock() instanceof CoralBlock))
			return false;
		
		// don't grow if too high off sea floor.
		bs = serverLevel.getBlockState(theCoralBlockPos.below(4+pRand.nextInt(3)));
		if (!ActionTests.isOceanFloorBlock(bs)) { 
			return false;
		}
	
		return true;
	}

}
