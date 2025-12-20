package com.mactso.regrowth.actions;

import com.mactso.regrowth.utility.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

/**
 * Provides helper methods for actions that manipulate road blocks and terrain.
 * Includes logic for detecting, smoothing, and repairing roads, potholes,
 * bumps, and snow coverage. Designed for villager-driven road maintenance
 * and biome-specific road blocks.
 */
public class RoadActionHelpers {

	static boolean hasAdjacentLowerRoadBlock(ServerLevel serverLevel, int veX, int veZ, int startY, Block biomeRoadBlock) {
	
		MutableBlockPos tempPos = new MutableBlockPos();
		for (int i = 0; i < 4; i++) {
			int neighborX = veX + ActionConstants.dx[i];
			int neighborZ = veZ + ActionConstants.dz[i];
			int neighborY = startY - 1;
			tempPos.set(neighborX, neighborY, neighborZ);
			Block neighborBlock = serverLevel.getBlockState(tempPos).getBlock();
	
			if (neighborBlock == biomeRoadBlock) {
				return true;
			}
	
		}
		return false;
	}

	static boolean hasAdjacentMuchHigherRoadBlock(ServerLevel serverLevel, BlockPos pos, Block biomeRoadBlock) {
	
		int posX = pos.getX();
		int posY = pos.getY();
		int posZ = pos.getZ();
	
		MutableBlockPos tempPos = new MutableBlockPos();
	
		for (int i = 0; i < 4; i++) { // loop east,west,south,north
			int testX = posX + ActionConstants.dx[i];
			int testZ = posZ + ActionConstants.dz[i];
			int testY = serverLevel.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, testX, testZ);
	
			if (testY < 1) // won't smooth roads deep underground.
				continue;
			int deltaY = testY - posY;
			if (deltaY < 1) // lower neighbor, ignore in lower test
				continue;
			if (deltaY == 1) // one block higher neighbor- stop below it
				continue;
			if (deltaY > 11) // too far above
				continue;
			tempPos.set(testX, testY, testZ);
			Block testBlock = serverLevel.getBlockState(tempPos.below()).getBlock();
	
			if (testBlock == biomeRoadBlock) {
				return true;
			}
	
		}
	
		return false;
	}

	static boolean hasAdjacentMuchLowerRoadBlock(ServerLevel serverLevel, BlockPos pos, Block biomeRoadBlock) {
	
		int posX = pos.getX();
		int posY = pos.getY();
		int posZ = pos.getZ();
	
		MutableBlockPos tempPos = new MutableBlockPos();
	
		for (int i = 0; i < 4; i++) { // loop east,west,south,north
			int testX = posX + ActionConstants.dx[i];
			int testZ = posZ + ActionConstants.dz[i];
			int testY = serverLevel.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, testX, testZ);
	
			if (testY < 1) // won't smooth roads deep underground.
				continue;
			int deltaY = posY - testY;
			if (deltaY < 1) // higher neighbor, ignore in lower test
				continue;
			if (deltaY == 1) // one block lower neighbor- stop above it
				continue;
			if (deltaY > 11) // too far below
				continue;
			tempPos.set(testX, testY, testZ);
			Block testBlock = serverLevel.getBlockState(tempPos.below()).getBlock();
			if (testBlock == biomeRoadBlock) {
				return true;
			}
	
		}
	
		return false;
	}

	static boolean hasAdjacentRoadBlock(ServerLevel serverLevel, BlockPos actualPos, Block biomeRoadBlock) {
	
		MutableBlockPos tempPos = new MutableBlockPos();
		for (int i = 0; i < 4; i++) {
			int neighborX = actualPos.getX() + ActionConstants.dx[i];
			int neighborZ = actualPos.getZ() + ActionConstants.dz[i];
			tempPos.set(neighborX, actualPos.getY(), neighborZ);
			Block neighborBlock = serverLevel.getBlockState(tempPos).getBlock();
	
			if (neighborBlock == biomeRoadBlock) {
				return true;
			}
	
		}
		return false;
	}

	// Checks if 3 or 4 road blocks are orthagonally adjacent with some Y flexibility for
	// slopes.
	// Note: Exits once it finds 3 adjacent roadblocks
	// Note: Exits early once it finds 3+ road blocks can't happen
	static boolean isPatchAdjacentRoadBlocks(ActionContext rgCtx) {

	    ServerLevel level = (ServerLevel) rgCtx.ve().level();
	    BlockPos base = rgCtx.adjustedPos();
	    Block biomeRoadBlock = rgCtx.biomeRoadBlock();

	    MutableBlockPos pos = new MutableBlockPos();
	    int count = 0;

	    // EAST
	    pos.set(base.getX() + 1, base.getY(), base.getZ());
	    if (RoadActionHelpers.isRoadInYColumn(level, biomeRoadBlock, pos)) count++;

	    // WEST
	    pos.set(base.getX() - 1, base.getY(), base.getZ());
	    if (RoadActionHelpers.isRoadInYColumn(level, biomeRoadBlock, pos)) count++;

	    // NORTH
	    pos.set(base.getX(), base.getY(), base.getZ() - 1);
	    if (RoadActionHelpers.isRoadInYColumn(level, biomeRoadBlock, pos)) count++;
	    if (count == 3) return true;
	    
	    // SOUTH
	    pos.set(base.getX(), base.getY(), base.getZ() + 1);
	    if (RoadActionHelpers.isRoadInYColumn(level, biomeRoadBlock, pos)) count++;
	    if (count >=3) return true;

	    return false;
	}

	static boolean isRoadBump(ActionContext rgCtx) {
	
		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos adjustedPos = rgCtx.adjustedPos();
		Block biomeRoadBlock = rgCtx.biomeRoadBlock();
	
		BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
		BlockState bs = null;
	
		// EAST
		mPos.set(adjustedPos.getX() + 1, adjustedPos.getY() - 2, adjustedPos.getZ() - 0);
		bs = serverLevel.getBlockState(mPos);
		if (!(bs.getBlock() == biomeRoadBlock)) {
			return false;
		}
	
		// WEST
		mPos.set(adjustedPos.getX() - 1, adjustedPos.getY() - 2, adjustedPos.getZ() - 0);
		bs = serverLevel.getBlockState(mPos);
		if (!(bs.getBlock() == biomeRoadBlock)) {
			return false;
		}
	
		// NORTH
		mPos.set(adjustedPos.getX() - 0, adjustedPos.getY() - 2, adjustedPos.getZ() - 1);
		bs = serverLevel.getBlockState(mPos);
		if (!(bs.getBlock() == biomeRoadBlock)) {
			return false;
		}
	
		// SOUTH
		mPos.set(adjustedPos.getX() - 0, adjustedPos.getY() - 2, adjustedPos.getZ() + 1);
		bs = serverLevel.getBlockState(mPos);
		if (!(bs.getBlock() == biomeRoadBlock)) {
			return false;
		}
	
		return true;
	}

	public static boolean isRoadInYColumn(ServerLevel level, Block roadBlock, BlockPos pos) {
	    // adjust down by 1 since road blocks are partial blocks
		// this will be an issue if the road blocks are ever not dirt_path partial blocks.
	    int topY = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
	    BlockPos topPos = new BlockPos(pos.getX(), topY-1, pos.getZ());
	    
	    BlockState state = level.getBlockState(topPos);
	    return state.is(roadBlock);
	}

	// a pothole is any air block with four orthagonal adjacent road blocks.
	static boolean isRoadPotHole(ServerLevel serverLevel, BlockPos pos, Block biomeRoadBlock) {
		BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
	
		if (!(serverLevel.getBlockState(mPos).is(BlockTags.AIR)))
			return false;
	
		mPos.set(pos).move(1, 0, 0); // east
		if (serverLevel.getBlockState(mPos).getBlock() != biomeRoadBlock)
			return false;
	
		mPos.set(pos).move(-1, 0, 0); // west
		if (serverLevel.getBlockState(mPos).getBlock() != biomeRoadBlock)
			return false;
	
		mPos.set(pos).move(0, 0, -1); // north
		if (serverLevel.getBlockState(mPos).getBlock() != biomeRoadBlock)
			return false;
	
		mPos.set(pos).move(0, 0, 1); // south
		if (serverLevel.getBlockState(mPos).getBlock() != biomeRoadBlock)
			return false;
	
		return true;
	}

	// flatten 1 block road bumps in the middle of roads.
	static boolean roadFixBump(ActionContext rgCtx) {
	
		if (!rgCtx.isStandingOnRoad())
			return false;
	
		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = (ServerLevel) ve.level();
		Boolean doDebug = rgCtx.doDebug();
		BlockPos adjustedPos = rgCtx.adjustedPos();
		Block biomeRoadBlock = rgCtx.biomeRoadBlock();
	
		if (RoadActionHelpers.isRoadBump(rgCtx)) {
			if (doDebug)
				Utility.debugMsg(1, ve, " actual fix bumps in road.");
			serverLevel.setBlockAndUpdate(adjustedPos.below(1), Blocks.AIR.defaultBlockState());
			serverLevel.setBlockAndUpdate(adjustedPos.below(2), biomeRoadBlock.defaultBlockState());
			serverLevel.playSound(ve, ve.blockPosition(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS,
					/* Volume */ 0.75f, /* Pitch */ 0.75f);
	
			rgCtx.setGroundBlockState(Blocks.AIR.defaultBlockState());
			rgCtx.setGroundBlock(Blocks.AIR);
			rgCtx.setFootBlockState(biomeRoadBlock.defaultBlockState());
			rgCtx.setFootBlock(biomeRoadBlock);
			rgCtx.setAdjustedPos(adjustedPos.below());
	
			return true;
		}
		return false;
	}

	// ---------------------
	// Change dirt or grass patches with 3 or 4 adjacent road blocks to road blocks
	// ---------------------
	// @SuppressWarnings("deprecation")
	static boolean roadFixPatch(ActionContext rgCtx) {
	
		if (!rgCtx.isStandingOnDirtOrGrass())
			return false;
	
		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = (ServerLevel) ve.level();
		BlockPos adjustedPos = rgCtx.adjustedPos();
		Block biomeRoadBlock = rgCtx.biomeRoadBlock();
	
		if (!(RoadActionHelpers.isPatchAdjacentRoadBlocks(rgCtx)))
			return false;
	
		BlockState biomeRoadBlockState = biomeRoadBlock.defaultBlockState();
		serverLevel.setBlockAndUpdate(adjustedPos.below(), biomeRoadBlockState); // TODO: Fix in 1.21.1-1.21.4
		serverLevel.playSound(ve, ve.blockPosition(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, /* Volume */ 0.75f,
				/* Pitch */ 0.75f);
		roadFixSnow(rgCtx); // now we are on a road.
	
		rgCtx.setFootBlockState(Blocks.AIR.defaultBlockState());
		rgCtx.setFootBlock(Blocks.AIR);
		rgCtx.setGroundBlockState(biomeRoadBlockState);
		rgCtx.setGroundBlock(biomeRoadBlock);
	
		return true;
	
	}

	// fix 1 block pot holes in roads.
	static boolean roadFixPotholes(ActionContext rgCtx) {
	
		if (!rgCtx.isStandingOnRoad())
			return false;
	
		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos adjustedPos = rgCtx.adjustedPos();
		Block biomeRoadBlock = rgCtx.biomeRoadBlock();
	
		// Check if villager in a pothole
		if (!RoadActionHelpers.isRoadPotHole(serverLevel, adjustedPos, biomeRoadBlock)) {
			return false;
		}
	
		if (rgCtx.doDebug())
			Utility.debugMsg(2, ve, " fixed a pothole in the road.");
	
		// Fix the pothole:
		// 1. Replace the foot block with biomeRoadBlock
	
		serverLevel.playSound(ve, ve.blockPosition(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, /* volume */ 0.75f,
				/* pitch */ 0.75f);
		serverLevel.setBlockAndUpdate(adjustedPos, biomeRoadBlock.defaultBlockState());
	
		// 2. Replace the old ground block with dirt
		BlockState dirtState = Blocks.DIRT.defaultBlockState();
		serverLevel.setBlockAndUpdate(adjustedPos.below(), dirtState);
		// silent no noise.
	
		// Make the villager jump up onto the new road block (won't finish this tick)
		ActionHelpers.jumpUp(ve);
	
		// update context to reflect the new roadblock (not strictly accurate)
		rgCtx.setFootBlockState(biomeRoadBlock.defaultBlockState());
		rgCtx.setFootBlock(biomeRoadBlock);
		rgCtx.setGroundBlockState(Blocks.DIRT.defaultBlockState());
		rgCtx.setGroundBlock(Blocks.DIRT);
	
		return true;
	}

	// ---------------------
	// Clear generated snow off of roads. Falling snow doesn't stick on Roads.
	// ---------------------
	static boolean roadFixSnow(ActionContext rgCtx) {
	
		if (!rgCtx.isStandingOnRoad())
			return false;
	
		ServerLevel serverLevel = rgCtx.serverLevel();
	
		// 2. Villager's feet must be in a layer of snow.
		if (rgCtx.footBlock() instanceof SnowLayerBlock) {
	
			// Remove the snow
			serverLevel.destroyBlock(rgCtx.adjustedPos(), false); // plays sound
	
			// Update context block fields
			rgCtx.setFootBlockState(Blocks.AIR.defaultBlockState());
			rgCtx.setFootBlock(Blocks.AIR);
	
			return true;
		}
	
		return false;
	}

	static boolean roadSmoothDownwards(ActionContext rgCtx) {
	
		if (!rgCtx.isStandingOnRoad())
			return false;
	
		ServerLevel serverLevel = rgCtx.serverLevel();
		Villager ve = rgCtx.ve();
		Block biomeRoadBlock = rgCtx.biomeRoadBlock();
	
		BlockPos actualPos = ActionUtilities.getAdjustedPos(ve);
	
		// stop if there is an adjacent road block one block higher next to villager's
		// feet
		if (RoadActionHelpers.hasAdjacentRoadBlock(serverLevel, actualPos, biomeRoadBlock)) {
			return false;
		}
	
		// stop if there is an adjacent road block one block lower next to villager's
		// feet
		if (RoadActionHelpers.hasAdjacentRoadBlock(serverLevel, actualPos.below(2), biomeRoadBlock)) {
			return false;
		}
	
		// Search for a much lower road block in 4 adjacent squares
		if (RoadActionHelpers.hasAdjacentMuchLowerRoadBlock(serverLevel, actualPos, biomeRoadBlock)) {
			BlockPos lowerPos = actualPos.below();
			serverLevel.setBlockAndUpdate(lowerPos, Blocks.AIR.defaultBlockState());
			serverLevel.playSound(ve, ve.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.BLOCKS,
					/* volume */ 0.75f, /* pitch */ 0.75f);
	
			serverLevel.setBlockAndUpdate(lowerPos.below(), biomeRoadBlock.defaultBlockState());
	
			// The villager is now in the air above the new lower road block
			rgCtx.setFootBlockState(Blocks.AIR.defaultBlockState());
			rgCtx.setFootBlock(Blocks.AIR);
			rgCtx.setGroundBlockState(Blocks.AIR.defaultBlockState());
			rgCtx.setGroundBlock(Blocks.AIR);
			BlockPos pos = ve.blockPosition();
			ve.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
			return true;
		}
	
		return false;
	}

	// Smooth roads up towards neighbors that are 2 or more blocks higher but less
	// than 10 blocks higher
	static boolean roadSmoothSlope(ActionContext rgCtx) {
	
		if (!rgCtx.isStandingOnRoad())
			return false;
	
		Villager ve = rgCtx.ve();
	
		if (ve.isBaby()) // baby's can't do heavy labor
			return false;
	
		ServerLevel sLevel = (ServerLevel) ve.level();
	
		if (!(sLevel.canSeeSky(ve.blockPosition())))
			return false;
	
		BlockState groundBlockState = rgCtx.groundBlockState();
		Block biomeRoadBlock = rgCtx.biomeRoadBlock();
	
		if (groundBlockState.getBlock() != biomeRoadBlock)
			return false;
	
		if (ActionTests.isNearbyPoi(rgCtx)) {
			return false;
		}
	
		if (RoadActionHelpers.roadSmoothUpwards(rgCtx)) {
			return true;
		}
	
		if (RoadActionHelpers.roadSmoothDownwards(rgCtx)) {
			return true;
		}
	
		return false;
	}

	public static boolean roadSmoothUpwards(ActionContext rgCtx) {
	
		ServerLevel serverLevel = rgCtx.serverLevel();
		Villager ve = rgCtx.ve();
		Block biomeRoadBlock = rgCtx.biomeRoadBlock();
	
		BlockPos actualPos = ActionUtilities.getAdjustedPos(ve);
	
		// stop if there is an adjacent road block one block higher next to villager's
		// feet
		if (hasAdjacentRoadBlock(serverLevel, actualPos, biomeRoadBlock)) {
			return false;
		}
	
		// don't check for road block one lower Y... we want villagers to tower up.
		// Search for a much lower road block in 4 adjacent squares
	
		if (hasAdjacentMuchHigherRoadBlock(serverLevel, actualPos, biomeRoadBlock)) {
			ve.teleportTo(actualPos.getX() + 0.5, actualPos.getY() + 0.1, actualPos.getZ() + 0.5);
			serverLevel.setBlockAndUpdate(actualPos, biomeRoadBlock.defaultBlockState());
			serverLevel.playSound(ve, ve.blockPosition(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS,
					/* volume */ 0.75f, /* pitch */ 0.75f);
			serverLevel.setBlockAndUpdate(actualPos.below(), Blocks.DIRT.defaultBlockState());
	
			// The villager is now standing in the the new road block above the new lower
			// road block
			rgCtx.setFootBlockState(biomeRoadBlock.defaultBlockState());
			rgCtx.setFootBlock(biomeRoadBlock);
			rgCtx.setGroundBlockState(Blocks.DIRT.defaultBlockState());
			rgCtx.setGroundBlock(Blocks.DIRT);
	
			ActionHelpers.jumpUp(ve);
			return true;
		}
	
		return false;
	}

}
