package com.mactso.regrowth.actions;

import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

//-----------------------
//Action Helpers help do detailed fiddly work and they all modify the world in some way.   
//-----------------------

public class ActionHelpers {
	




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

	static void applyRegeneration(LivingEntity le, int regenDuration, Villager ve, ServerLevel level, BlockPos pos) {
		le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenDuration, 0), ve);
		level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.2f, 1.52f);
	}





	// ---------------------
	// Try to place a torch on the side of a village building wall, cliff wall, or tree.
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
		
		if (!MyUtilities.isStringValid(MyConfig.getTorchBlock()))
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
