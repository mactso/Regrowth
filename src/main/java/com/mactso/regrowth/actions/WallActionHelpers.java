package com.mactso.regrowth.actions;

import com.mactso.regrowth.managers.WallBiomeDataManager.WallBiomeDataItem;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class WallActionHelpers {

	static final int WALL_CENTER = 0;
	static final int FENCE_CENTER = 0;
	static final int WALL_TYPE_WALL = -1;
	static final int WALL_TYPE_FENCE = -2;

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty OPEN = FenceGateBlock.OPEN;

	/**
	 * Checks whether a villager can place a wall block at their current location.
	 * Validates village state, control block, foot block, ground block, adjacent
	 * doors, partial blocks, and dirt paths.
	 *
	 * @param rgCtx the action context for the villager
	 * @return true if the villager may build a wall here
	 */
	public static boolean canBuildVillageWall(ActionContext rgCtx) {

		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos adjustedPos = rgCtx.adjustedPos();
		GlobalPos gPos = rgCtx.villageMeetingPointPos();
		if (gPos == null)
			return false;

		if (!ActionTests.isWallBuildingOn())
			return false;

		if (!rgCtx.hasMeetingPoint())
			return false;
		if (!rgCtx.isAtAValidWallSpot() && (!rgCtx.isAtAValidGateSpot()))
			return false;

		if (!ActionTests.isFootBlockOkayToBuildIn(rgCtx))
			return false;
		if (rgCtx.groundBlock() instanceof WallBlock)
			return false;
		if (rgCtx.groundBlockState().isAir())
			return false;
		if (!ActionTests.isOkayToBuildWallHere(rgCtx))
			return false;
		if (serverLevel.getBlockState(adjustedPos).getBlock() == Blocks.DIRT_PATH)
			return false;
		if (serverLevel.getBlockState(adjustedPos.below()).getBlock() == Blocks.DIRT_PATH)
			return false;
		if (ActionTests.hasAdjacentDoor(serverLevel, adjustedPos))
			return false;
		// No workstations within 2 blocks, only 1 village meeting point in wall radius
		if (!ActionTests.isWallBuildAllowedByPOIs(rgCtx))
			return false;

		return true;
	}

	/**
	 * Executes wall-building logic for a villager. - Handles mason bonus building -
	 * Places one wall piece at the villager's location - Optionally places torches
	 * based on villager actions
	 *
	 * @param rgCtx the action context
	 * @return true if a wall block or torch was placed
	 */
	static boolean improveVillageWall(ActionContext rgCtx) {

		Villager ve = rgCtx.ve();

		// Masons bonus building ability can build walls from anywhere
		if (tryBuildMasonWalls(rgCtx)) {
			if (rgCtx.doDebug())
				MyUtilities.debugMsg(2, ve, "Mason built a corner of wall.");
			return true;
		}

		if (tryPlaceOneWallPiece(rgCtx)) {
			tryPlaceTorch(rgCtx);
			ActionHelpers.jumpUp(ve);
			if (rgCtx.doDebug())
				MyUtilities.debugMsg(2, ve, "Villager built one block of wall.");
			return true;
		}

		return false;
	}

	/**
	 * Places a torch above the villager if they have the ACTION_IMPROVE_LIGHTING
	 * action and the location is valid for torch placement.
	 */
	public static void tryPlaceTorch(ActionContext rgCtx) {
		if (!rgCtx.hasVillagerAction(VillagerActions.ACTION_IMPROVE_LIGHTING))
			return;

		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos vePos = rgCtx.adjustedPos();
		BlockPos vmpPos = rgCtx.villageMeetingPointPos().pos();
		WallBiomeDataItem wbdi = rgCtx.wallBiomeDataItem();
		int wallRadius = wbdi.getWallRadius();
		int wallTorchSpacing = wbdi.getTorchSpacing();

		int dx = vePos.getX() - vmpPos.getX();
		int dz = vePos.getZ() - vmpPos.getZ();

		if (ActionTests.isValidTorchLocation(wallRadius, wallTorchSpacing, Math.abs(dx), Math.abs(dz),
				serverLevel.getBlockState(vePos).getBlock())) {

			serverLevel.setBlockAndUpdate(vePos.above(), Blocks.TORCH.defaultBlockState());
			serverLevel.playSound(null, // player: null for all nearby
					vePos.above(), // position
					SoundEvents.WOOD_PLACE, // sound event
					SoundSource.BLOCKS, // category
					1.0f, 1.0f // volume, pitch
			);
		}
	}

	/**
	 * Determines whether the villager is at a location to place a wall or gate
	 * piece. Delegates actual block placement to placeTheWallPiece().
	 */
	static boolean tryPlaceOneWallPiece(ActionContext rgCtx) {

		Villager ve = rgCtx.ve();
		BlockPos pos = ve.blockPosition();
		BlockPos gVMPPos = rgCtx.villageMeetingPointPos().pos();
		WallBiomeDataItem currentWallBiomeDataItem = rgCtx.wallBiomeDataItem();
		int wallRadius = currentWallBiomeDataItem.getWallRadius();

		String direction = "EAST";
		Direction d = Direction.EAST;

		if (isOnValidEastWall(pos, gVMPPos, wallRadius)) {
			d = Direction.EAST;
			direction = " East ";
		} else if (isOnValidWestWall(pos, gVMPPos, wallRadius)) {
			d = Direction.WEST;
			direction = " West ";
		} else if (isOnValidNorthWall(pos, gVMPPos, wallRadius)) {
			d = Direction.NORTH;
			direction = " North ";
		} else if (isOnValidSouthWall(pos, gVMPPos, wallRadius)) {
			d = Direction.SOUTH;
			direction = " South ";
		}

		if (placeTheWallOrGatePiece(rgCtx, d)) {
			if (rgCtx.doDebug())
				MyUtilities.debugMsg(1, ve, "Built " + direction + " wall piece.");
			return true;
		}

		return false;

	}

	/**
	 * Places a wall or gate block at the villager's current position. Handles
	 * clearing partial blocks (snow, plants, etc.) and natural items
	 * (pebbles/sticks). Optionally logs debug info.
	 *
	 * @param rgCtx     the villager action context
	 * @param Direction the compass direction of the wall from the villagers meeting
	 *                  place.
	 */
	static boolean placeTheWallOrGatePiece(ActionContext rgCtx, Direction d) {

		Villager ve = rgCtx.ve();
		BlockPos vmpPos = rgCtx.villageMeetingPointPos().pos();
		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos vePos = ActionUtilities.getAdjustedPos(ve);
		Block footBlock = rgCtx.footBlock();
		WallBiomeDataItem tmpWbdi = rgCtx.wallBiomeDataItem();
		BlockState wallBlockState = tmpWbdi.getWallBlockState();
		BlockState gateBlockState = Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(OPEN, true).setValue(FACING, d);

		if (footBlock instanceof SnowLayerBlock) {
			serverLevel.destroyBlock(vePos, false);
		}

		if (ActionTests.isNatProgPebbleOrStick(rgCtx)) {
			serverLevel.destroyBlock(vePos, true);
		}

		if ((footBlock instanceof SaplingBlock) || (footBlock instanceof TallGrassBlock)
				|| (footBlock instanceof FlowerBlock) || (footBlock instanceof DoublePlantBlock)) {
			serverLevel.destroyBlock(vePos, true);
		}

		if (isAtValidGateSpot(vePos, vmpPos, tmpWbdi.getWallRadius())) {
			serverLevel.setBlockAndUpdate(vePos, gateBlockState);
			serverLevel.neighborChanged(vePos, gateBlockState.getBlock(), vePos);
			serverLevel.playSound(null, // player: null for all nearby
					vePos.above(), // position
					SoundEvents.WOOD_PLACE, // sound event
					SoundSource.BLOCKS, // category
					1.0f, 1.0f); // volume, pitch
			return true;
		}

		serverLevel.setBlockAndUpdate(vePos, wallBlockState);
		serverLevel.neighborChanged(vePos, wallBlockState.getBlock(), vePos);
		serverLevel.playSound(null, // player: null for all nearby
				vePos.above(), // position
				SoundEvents.STONE_PLACE, // sound event
				SoundSource.BLOCKS, // category
				1.0f, 1.0f); // volume, pitch
		if (rgCtx.doDebug()) {
			if (serverLevel.getBlockState(vePos).is(BlockTags.AIR)) {
				return false;
			}
		}

		return true;

	}

	// only happens when a mason build a wall section so performance hit low.
	// TODO :enhance later to fill in a hole on the wall on that side.
	public static boolean tryBuildMasonWalls(ActionContext rgCtx) {

		if (!(VillagerActions.isVillagerProfession(rgCtx.ve(), VillagerProfession.MASON)))
			return false;

		ServerLevel serverLevel = rgCtx.serverLevel();
		WallBiomeDataItem wi = rgCtx.wallBiomeDataItem();
		int wallRadius = wi.getWallRadius();

		GlobalPos gPos = rgCtx.villageMeetingPointPos();
		if (gPos == null)
			return false; // should always be defined tho.

		BlockPos meetingPlacePos = gPos.pos();
		Boolean doDebug = rgCtx.doDebug();
		if (doDebug)
			MyUtilities.debugMsg(1, "tryBuildMasonWalls from Meeting Place Pos: " + meetingPlacePos);

		BlockState wallBs = wi.getWallBlockState();
		MutableBlockPos mPos = new BlockPos.MutableBlockPos();

		// North-East corner
		calcSurfaceBlockPos(serverLevel, meetingPlacePos.east(wallRadius).north(wallRadius), mPos);
		if (WallActionHelpers.tryBuildWallCorner(serverLevel, mPos, wallBs)) {
			if (doDebug)
				MyUtilities.debugMsg(1, "built north eastern corner at :" + mPos);
			return true;
		}

		// South-East corner
		calcSurfaceBlockPos(serverLevel, meetingPlacePos.east(wallRadius).south(wallRadius), mPos);
		if (WallActionHelpers.tryBuildWallCorner(serverLevel, mPos, wallBs)) {
			if (doDebug)
				MyUtilities.debugMsg(1, "built south eastern corner at :" + mPos);
			return true;
		}

		// North-West corner
		calcSurfaceBlockPos(serverLevel, meetingPlacePos.west(wallRadius).north(wallRadius), mPos);
		if (WallActionHelpers.tryBuildWallCorner(serverLevel, mPos, wallBs)) {
			if (doDebug)
				MyUtilities.debugMsg(1, "built north western corner at :" + mPos);
			return true;
		}

		calcSurfaceBlockPos(serverLevel, meetingPlacePos.west(wallRadius).south(wallRadius), mPos);
		if (WallActionHelpers.tryBuildWallCorner(serverLevel, mPos, wallBs)) {
			if (doDebug)
				MyUtilities.debugMsg(1, "built south western corner at :" + mPos);
			return true;
		}

		return false;

	}

	public static void calcSurfaceBlockPos(ServerLevel sLevel, BlockPos input, BlockPos.MutableBlockPos result) {
		int x = input.getX();
		int z = input.getZ();
		int y = sLevel.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		result.set(x, y, z); // modifies the same object
	}

	static boolean tryBuildWallCorner(ServerLevel serverLevel, BlockPos mPos, BlockState wallBs) {

		if (!(ActionTests.isWallorLantern(serverLevel, mPos, wallBs))) {
			serverLevel.setBlockAndUpdate(mPos, wallBs);
			serverLevel.setBlockAndUpdate(mPos.above(), Blocks.LANTERN.defaultBlockState());
			serverLevel.playSound(null, // player: null for all nearby
					mPos, // position
					SoundEvents.LANTERN_PLACE, // sound event
					SoundSource.BLOCKS, // category
					1.0f, 1.0f // volume, pitch
			);
			return true;
		}

		return false;
	}

	// Helper methods
	public static boolean isOnValidEastWall(BlockPos pos, BlockPos meetingPos, int wallRadius) {
		int dx = pos.getX() - meetingPos.getX();
		int dz = Math.abs(pos.getZ() - meetingPos.getZ());
		return dx == wallRadius && dz <= wallRadius;
	}

	public static boolean isOnValidWestWall(BlockPos pos, BlockPos meetingPos, int wallRadius) {
		int dx = meetingPos.getX() - pos.getX();
		int dz = Math.abs(pos.getZ() - meetingPos.getZ());
		return dx == wallRadius && dz <= wallRadius;
	}

	public static boolean isOnValidNorthWall(BlockPos pos, BlockPos meetingPos, int wallRadius) {
		int dz = meetingPos.getZ() - pos.getZ();
		int dx = Math.abs(pos.getX() - meetingPos.getX());
		return dz == wallRadius && dx <= wallRadius;
	}

	public static boolean isOnValidSouthWall(BlockPos pos, BlockPos meetingPos, int wallRadius) {
		int dz = pos.getZ() - meetingPos.getZ();
		int dx = Math.abs(pos.getX() - meetingPos.getX());
		return dz == wallRadius && dx <= wallRadius;
	}

	// Overall wall validity
	public static boolean isAtValidWallSpot(BlockPos pos, BlockPos meetingPos, int wallRadius) {
		return isOnValidEastWall(pos, meetingPos, wallRadius) || isOnValidWestWall(pos, meetingPos, wallRadius)
				|| isOnValidNorthWall(pos, meetingPos, wallRadius) || isOnValidSouthWall(pos, meetingPos, wallRadius);
	}

	// called during context creation so cannot take context as a parameter
	public static boolean isAtValidGateSpot(BlockPos pos, BlockPos vmpPos, int wallRadius) {

		int dx = pos.getX() - vmpPos.getX();
		int dz = pos.getZ() - vmpPos.getZ();

		return (Math.abs(dx) == wallRadius && dz == 0) // East-West gate
				|| (Math.abs(dz) == wallRadius && dx == 0); // North-South gate
	}

}
