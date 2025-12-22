package com.mactso.regrowth.actions;

import com.mactso.regrowth.managers.WallBiomeDataManager;
import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.Heightmap.Types;

//-----------------------
//Action Helpers help do detailed fiddly work and they all modify the world in some way.   
//-----------------------

public class ActionHelpers {
	
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty OPEN = FenceGateBlock.OPEN;
	static final int WALL_CENTER = 0;
	static final int FENCE_CENTER = 0;
	static final int WALL_TYPE_WALL = -1;
	static final int WALL_TYPE_FENCE = -2;

	public static void jumpUp(Entity e) {
		e.setDeltaMovement(0, 0.62, 0);
	}

	public static void ageIfBaby(Entity ent) {
		if (ent instanceof AgeableMob) {
			AgeableMob aEnt = (AgeableMob) ent;
			if (aEnt.isBaby()) {
				aEnt.setAge(aEnt.getAge() + 30);
			}
		}
	}

	// Give special debugging names to villagers, but only if they do not already
	// have a non-Regrowth custom name.
	public static void updateDebugName(Villager ve) {

		if (MyConfig.getDebugLevel() > 0) {

			Component currentName = ve.getCustomName();
			boolean hasCustomNonRegName = currentName != null && !currentName.getString().startsWith("Reg-");

			// Do NOT override player-assigned or mod-assigned names.
			if (!hasCustomNonRegName) {
				float veYaw = ve.getViewYRot(1.0f);
				Component debugName = Component
						.literal("Reg-" + (int) ve.getX() + "," + (int) ve.getZ() + ": " + (int) (veYaw));
				ve.setCustomName(debugName);
			}

		} else {
			// Debugging OFF → remove only Regrowth-created names.
			Component currentName = ve.getCustomName();
			if (currentName != null && currentName.getString().startsWith("Reg-")) {
				ve.setCustomName(null);
			}
		}
	}

	// only happens when a mason build a wall section so performance hit low.
	// TODO :enhance later to fill in a hole on the wall on that side.
	public static boolean tryBuildMasonWalls(ActionContext rgCtx, int wallRadius, BlockPos meetingPlacePos,
			WallBiomeDataManager.WallBiomeDataItem wi) {

		ServerLevel serverLevel = rgCtx.serverLevel();
		Boolean doDebug = rgCtx.doDebug();
		if (doDebug)
			MyUtilities.debugMsg(1, "tryBuildMasonWalls from Meeting Place Pos: " + meetingPlacePos);

		BlockState wallBs = wi.getWallBlockState();
		BlockPos mPos = null;

		mPos = calcSurfaceBlockPos(serverLevel, meetingPlacePos.east(wallRadius).north(wallRadius + 1));
		if (tryBuildWallCorner(serverLevel, mPos, wallBs)) {
			if (doDebug)
				MyUtilities.debugMsg(1, "built north eastern corner at :" + mPos);
			return true;
		}

		mPos = calcSurfaceBlockPos(serverLevel, meetingPlacePos.east(wallRadius).south(wallRadius));
		if (tryBuildWallCorner(serverLevel, mPos, wallBs)) {
			if (doDebug)
				MyUtilities.debugMsg(1, "built south eastern corner at :" + mPos);
			return true;
		}

		mPos = calcSurfaceBlockPos(serverLevel, meetingPlacePos.west(wallRadius + 1).north(wallRadius + 1));
		if (tryBuildWallCorner(serverLevel, mPos, wallBs)) {
			if (doDebug)
				MyUtilities.debugMsg(1, "built north western corner at :" + mPos);
			return true;
		}

		mPos = calcSurfaceBlockPos(serverLevel, meetingPlacePos.west(wallRadius + 1).south(wallRadius));

		if (tryBuildWallCorner(serverLevel, mPos, wallBs)) {
			if (doDebug)
				MyUtilities.debugMsg(1, "built south western corner at :" + mPos);
			return true;
		}

		return false;

	}

	public static BlockPos calcSurfaceBlockPos(ServerLevel sLevel, BlockPos mPos) {
		int x = mPos.getX();
		int z = mPos.getZ();
		int y = sLevel.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos m1Pos = new BlockPos(x, y, z);
		return m1Pos;
	}

	static boolean tryBuildWallCorner(ServerLevel serverLevel, BlockPos mPos, BlockState wallBs) {

		if (!(ActionTests.isWallorLantern(serverLevel, mPos, wallBs))) {
			serverLevel.setBlockAndUpdate(mPos, wallBs);
			serverLevel.setBlockAndUpdate(mPos.above(), Blocks.LANTERN.defaultBlockState());
			return true;
		}

		return false;
	}

	static void applyRegeneration(LivingEntity le, int regenDuration, Villager ve, ServerLevel level, BlockPos pos) {
		le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenDuration, 0), ve);
		level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.2f, 1.52f);
	}

	static boolean tryPlaceOneWallPiece(ActionContext rgCtx, int wallRadius, int wallTorchSpacing,
			BlockState wallBlockState, BlockPos gVMPPos) {

		Villager ve = rgCtx.ve();

		// Build East and West Walls (and corners)
		BlockState gateBlockState = Blocks.OAK_FENCE_GATE.defaultBlockState()
				.setValue(FACING, Direction.EAST).setValue(OPEN, true);
		Direction d = null;
		if (ActionUtilities.getAbsVX(ve, gVMPPos) == wallRadius) {
			if (ActionUtilities.getAbsVZ(ve, gVMPPos) <= wallRadius) {
				d = (ActionUtilities.getVX(ve, gVMPPos) > 0) ? Direction.EAST : Direction.WEST;
				return placeTheWallPiece(rgCtx, wallBlockState, gateBlockState.setValue(FACING, d),
						ActionUtilities.getVZ(ve, gVMPPos));
			}
		}
		// Build North and South Walls (and corners)
		if (ActionUtilities.getAbsVZ(ve, gVMPPos) == wallRadius) {
			if (ActionUtilities.getAbsVX(ve, gVMPPos) <= wallRadius) {
				d = (ActionUtilities.getVZ(ve, gVMPPos) > 0) ? Direction.NORTH : Direction.SOUTH;
				return placeTheWallPiece(rgCtx, wallBlockState, gateBlockState.setValue(FACING, d),
						ActionUtilities.getVX(ve, gVMPPos));
			}
		}

		return false;
	}

	static boolean placeTheWallPiece(ActionContext rgCtx, BlockState wallBlockState, BlockState gateBlockState,
			int va) {

		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos vePos = ActionUtilities.getAdjustedPos(ve);
		Block footBlock = rgCtx.footBlock();

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

		int absva = Math.abs(va);
		if (absva == WALL_CENTER) {
			serverLevel.setBlockAndUpdate(vePos, gateBlockState);
			serverLevel.updateNeighborsAt(vePos, gateBlockState.getBlock());
			return true;
		}

		serverLevel.setBlockAndUpdate(vePos, wallBlockState);
		serverLevel.updateNeighborsAt(vePos, gateBlockState.getBlock());

		if (rgCtx.doDebug()) {
			if (serverLevel.getBlockState(vePos).is(BlockTags.AIR)) {
				return false;
			}
		}

		return true;

	}

	// ---------------------
	// Try to place a wall torch
	// ---------------------
	@SuppressWarnings("deprecation")
	static boolean tryPlaceWallTorch(ActionContext rgCtx) {

		Villager ve = rgCtx.ve();
		ServerLevel sLevel = rgCtx.serverLevel();
		BlockPos pos = rgCtx.adjustedPos();
		BlockState footBlockState = rgCtx.footBlockState();

		BlockPos veHeadPos = pos.above();

		if (!footBlockState.isAir())
			return false;

		BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

		Direction facing = Direction.fromYRot(ve.yHeadRot); // direction villager is looking
		mPos.set(veHeadPos).move(facing); // block in front of villager's head

		BlockState frontBS = sLevel.getBlockState(mPos);
		if (!frontBS.isSolid())
			return false;

		// Wall torch faces the opposite of the block it attaches to
		BlockState wallTorchState = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,
				facing.getOpposite());

		sLevel.setBlockAndUpdate(veHeadPos, wallTorchState);
		sLevel.playSound(null, veHeadPos.getX(), veHeadPos.getY(), veHeadPos.getZ(), SoundEvents.WOOD_PLACE,
				SoundSource.BLOCKS, 0.75f, 0.75f);

		return true;
	}

	// ---------------------
	// Try to place a torch on the ground
	// ---------------------
	static boolean tryPlaceGroundTorch(ActionContext rgCtx) {
		
		if (!MyUtilities.isStringValid(MyConfig.getTorchBlockString()))
			return false;

		ServerLevel sLevel = rgCtx.serverLevel();
		BlockPos pos = rgCtx.adjustedPos();
		BlockState footBlockState = rgCtx.footBlockState();

		if (!footBlockState.isAir() && !ActionTests.isNatProgPebbleOrStick(rgCtx))
			return false;

		if (!TorchBlock.canSupportCenter(sLevel, pos.below(), Direction.UP))
			return false;

		if (footBlockState.getBlock() instanceof BedBlock)
			return false;
		
		Block torchBlock = ActionUtilities.getTorchBlockFromConfig();
		if (torchBlock == null)
			return false;

		sLevel.setBlockAndUpdate(pos, torchBlock.defaultBlockState());
		sLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.75f,
				0.75f);
		return true;
	}

	// ---------------------
	// Try placing torch (wall or ground)
	// ---------------------
	static boolean tryPlaceTorch(ActionContext rgCtx) {

		Villager ve = rgCtx.ve();
		if (ActionUtilities.getVillagerTorchPlaceOdds(ve) < rgCtx.serverLevel().getRandom().nextInt(100))
			return false;

		if (tryPlaceWallTorch(rgCtx)) {
			if (rgCtx.doDebug())
				MyUtilities.debugMsg(2, ve, rgCtx.key() + " placed a wall torch.");
			return true;
		}

		if (tryPlaceGroundTorch(rgCtx)) {
			if (rgCtx.doDebug())
				MyUtilities.debugMsg(2, ve, rgCtx.key() + " placed a ground torch.");
			return true;
		}

		return false;
	}

	/**
	 * Handles eating, optional healing, and extra baby aging from eating. If they
	 * eat, the grass or flower is destroyed with no drops.
	 *
	 * @return true if entity consumed grass or flowers, false otherwise.
	 */
	static boolean tryEatGrassOrFlower(ActionContext rgCtx) {
		
		LivingEntity le = rgCtx.livingEntity();
	
		if (!(ActionTests.isGrassOrFlower(rgCtx.footBlockState()))) {
			return false;
		}
		rgCtx.serverLevel().destroyBlock(rgCtx.adjustedPos(), false);
		double roll = rgCtx.getRand().nextDouble();
		ageIfBaby(le);
		if (le.getMaxHealth() > le.getHealth() && (MyConfig.getEatingHealsOdds() > roll)) {
			MobEffectInstance ei = new MobEffectInstance(MobEffects.REGENERATION, 25, 0, false, true);
			le.addEffect(ei);
		}
		return true;
	}


}
