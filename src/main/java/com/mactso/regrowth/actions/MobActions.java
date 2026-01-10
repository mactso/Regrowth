package com.mactso.regrowth.actions;

import com.mactso.regrowth.managers.SaplingManager;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class MobActions {

	static final Block[] coralfans = { Blocks.BRAIN_CORAL_WALL_FAN, Blocks.BUBBLE_CORAL_WALL_FAN,
			Blocks.FIRE_CORAL_WALL_FAN, Blocks.HORN_CORAL_WALL_FAN, Blocks.TUBE_CORAL_WALL_FAN };

	static final String ACTION_GROW = "grow";
	static final String ACTION_EAT = "eat";
	static final String ACTION_BOTH = "both";
	static final String ACTION_TALL = "tall";
	static final String ACTION_MUSHROOM = "mushroom";
	static final String ACTION_STUMBLE = "stumble";
	static final String ACTION_REFOREST = "reforest";
	static final String ACTION_CORAL = "coral";

	/**
	 * Handles the "stumble" action for an entity when walking over torches.
	 *
	 * <p>
	 * If the block directly under the entity's foot is a {@link TorchBlock} or
	 * {@link WallTorchBlock}, the block is destroyed with drops. Optionally, a
	 * debug message is printed describing the entity and block position.
	 * </p>
	 *
	 * <p>
	 * This simulates entities accidentally knocking over torches in the world,
	 * adding dynamic environmental interaction.
	 * </p>
	 *
	 * @param rgCtx the context containing the entity, foot block, world, and
	 *              adjusted position for block interactions
	 */
	static void mobStumbleAction(ActionContext rgCtx) {

		if ((rgCtx.footBlock() instanceof TorchBlock) || (rgCtx.footBlock() instanceof WallTorchBlock)) {
			rgCtx.serverLevel().destroyBlock(rgCtx.adjustedPos(), true);
			if (rgCtx.doDebug())
				MyUtilities.debugMsg(2, rgCtx.livingEntity() + " " + rgCtx.adjustedPos() + " stumbled over torch.");
		}

	}

	/**
	 * Handles animal overcrowding with reduced health and packed dirt underfoot.
	 * This is a tight inner method that runs for every animal so optimizing is more
	 * important than ease of maintenance.
	 *
	 * <p>
	 * Method counts nearby same-type entities within a 7×5×7 (X×Y×Z) block volume
	 * centered on the entity. Overcrowding is managed with two thresholds:
	 * </p>
	 * 
	 * <ul>
	 * <li>When 31+ animals are present, the entity is discarded with no drops.</li>
	 * <li>When 11+ entities are present, the entity is slightly damaged.</li>
	 * <li>When 7+ entities are present, compactable ground becomes dirt path.</li>
	 * </ul>
	 *
	 * <p>
	 * Resolves inhumane conditions and prevents server lag from packed farms.
	 * </p>
	 *
	 * @param rgCtx the context containing the entity and server level
	 * @return true if the entity was removed or killed as a result of overcrowding;
	 *         false if the entity survived or overcrowding rules did not apply
	 */
	static boolean mobHandleOverCrowding(ActionContext rgCtx) {

		LivingEntity le = rgCtx.livingEntity();
		if (!(le instanceof Animal))
			return false;

		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos aPos = rgCtx.adjustedPos();
		BlockPos aPosBelow = aPos.below();

		// this creates a lot of BlockPos. Don't do it.
		// AABB box = AABB.encapsulatingFullBlocks(adjustedPos.offset(3, 2, 3),
		// adjustedPos.offset(-3, -2, -3));
		// instead do this

		int x = aPos.getX();
		int y = aPos.getY();
		int z = aPos.getZ();

		AABB box = new AABB(x - 3, y - 2, z - 3, x + 4, y + 3, z + 4);

		int count = 0;
		for (LivingEntity e : serverLevel.getEntitiesOfClass(le.getClass(), box)) {
			if (!e.isAlive())
				continue;
			if (++count > 31) {
				serverLevel.playSound(le, aPos, SoundEvents.COW_DEATH, SoundSource.NEUTRAL, 1.1f, 0.54f);
				le.remove(RemovalReason.DISCARDED); // Instant kill with no drops
				return true;
			}
		}

		if (count >= 7) {
			double fracY = le.getY() - Math.floor(le.getY());
			if (fracY < 0.001) {
				BlockState bs = serverLevel.getBlockState(aPosBelow);
				boolean onCompactable = ActionTests.isKindOfDirtBlock(bs) || ActionTests.isKindOfGrassBlock(bs);
				if (onCompactable)
					serverLevel.setBlockAndUpdate(aPosBelow, Blocks.DIRT_PATH.defaultBlockState());
			}
		}

		if (count >= 11) {
			float hurt = (count - 11) + (serverLevel.getRandom().nextFloat() / 6);
			le.hurtServer(serverLevel,serverLevel.damageSources().inWall(), hurt);
			if (!le.isAlive())
				return true;
		}

		return false;
	}

	/**
	 * Handles natural reforestation by planting saplings under animal (pig)
	 * movement.
	 *
	 * <p>
	 * A pseudo-random filter based on the Y-coordinate reduces the planting
	 * frequency (~25% chance per eligible block). Plants a biome-appropriate
	 * sapling if spacing and overhead tree rules pass.
	 *
	 * TODO: Add Dynamic Tree Support
	 *
	 * Note: This method runs infrequently per animal.
	 *
	 * </p>
	 *
	 * @param rgCtx the action context containing entity, world, position, and biome
	 *              info
	 * @return true if a sapling was planted, false otherwise
	 */
	static boolean mobReforestAction(ActionContext rgCtx) {


		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos aPos = rgCtx.adjustedPos();
		Block footBlock = rgCtx.footBlockState().getBlock();

		// Only plant if foot is air and ground is grass or dirt
		if (footBlock != Blocks.AIR || !ActionTests.isGrassOrDirtBlock(rgCtx.groundBlockState()))
			return false;

		// ~25% chance plant attempt
		double sinY = Math.sin(((aPos.getY() + 64) % 256) / 256.0);
		if (serverLevel.random.nextDouble() > Math.abs(sinY))
			return false;

		// Get biome-appropriate sapling
		BlockState sapling = SaplingManager.getBiomeSaplingBlockState(serverLevel.getServer(), rgCtx.localBiome());
		if (sapling.isAir())
			return false;

		// Abort if spacing rules fail
		if (ActionTests.isTooCloseToSapling(serverLevel, aPos))
			return false;
		if (ActionTests.isTreeAbove(serverLevel, sapling, aPos))
			return false;

		// Plant Sapling
		// TODO: Dynamic Trees Integration
		serverLevel.setBlockAndUpdate(aPos, sapling);

		if (rgCtx.doDebug()) {
			MyUtilities.debugMsg(1, aPos, String.format("%s planted sapling. %s", rgCtx.key(),
					ActionUtilities.debugSaplingInfo(serverLevel, rgCtx.localBiome(), sapling)));
		}

		return true;
	}

	// @SuppressWarnings("resource")
	static boolean mobGrowPlantsAction(ActionContext rgCtx) {

		LivingEntity le = rgCtx.livingEntity();
		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos aPos = rgCtx.adjustedPos();
		String key = rgCtx.key();
		BlockState footBlockState = rgCtx.footBlockState();
		Block groundBlock = rgCtx.groundBlockState().getBlock();

		if (footBlockState.isAir()) {
			if (!(groundBlock instanceof BonemealableBlock)) {
				return false;
			}
			if (aPos == null) { // "impossible" but it happened once in production.
				MyUtilities.debugMsg(0, "ERROR:" + key + "grow plant null position.");
				return false;
			}
			BonemealableBlock ib = (BonemealableBlock) groundBlock;
			MyUtilities.debugMsg(1, le, key + " growable plant found.");
			try {
				ib.performBonemeal(serverLevel, serverLevel.getRandom(), aPos, footBlockState);
			} catch (Exception e) {
				MyUtilities.debugMsg(1, le, key + " caught grow plant attempt exception.");
			}
		}
		return true;
	}

	/**
	 * Handles mushroom growth for an entity's location. Checks temperature,
	 * density, and environment for valid placement. Dusts giant mushroom tops with
	 * smaller mushrooms when applicable. Attempts to grow huge mushrooms on the
	 * ground or on top of existing huge mushrooms.
	 */
	static void mobHugeMushroomAction(ActionContext rgCtx) {
		LivingEntity le = rgCtx.livingEntity();
		String key = rgCtx.key();
		Block groundBlock = rgCtx.groundBlockState().getBlock();

		RandomSource predictableRand = ActionUtilities.createPredictableRandom(le);
		double growthFilterValue = predictableRand.nextDouble();

		if (!MushroomActionHelpers.isValidMushroomPosition(rgCtx, growthFilterValue))
			return;

		if (groundBlock instanceof HugeMushroomBlock) {
		// Dust the top of existing huge mushrooms with little mushrooms
		    if (predictableRand.nextDouble() < 0.8) {
			MushroomActionHelpers.tryDustMushroomTop(rgCtx);
		        return;
		    }
		}

		// Otherwise try to grow a huge mushroom here and cleanup if it doesn't grow.
		MushroomActionHelpers.tryGrowHugeMushroom(rgCtx, growthFilterValue);

		if (rgCtx.doDebug())
			MyUtilities.debugMsg(1, le, key + " exit grow mushroom.");
	}

	// If there is a coral block 2 or 3 blocks below the aquatic entity
	// try to grow a coral fan on the block 30% of the time.
	// if there are under 5 coral blocks above the coral block, try to grow a new
	// coral block there.
	static void mobCoralAction(ActionContext rgCtx) {

		LivingEntity le = rgCtx.livingEntity();
		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos aPos = rgCtx.adjustedPos();

		// TODO reenable after testing.
		if (!CoralActionHelpers.isGoodCoralBiome(serverLevel, aPos)) {
		    return;
		}
		
		// Create predictable random source for placement decisions
		RandomSource pRand = ActionUtilities.createPredictableRandom(le);
		
		BlockPos theCoralBlockPos = ActionUtilities.findTheCoralBlockBelow(serverLevel, aPos);
		if (theCoralBlockPos == null) return;
		
		
		// Decorate Coral Blocks with Fans and Sea Pickles.
		CoralActionHelpers.tryDecorateCoralArea(le, serverLevel, theCoralBlockPos, pRand);

		// Attempt to grow a new coral block above and slightly offset
		CoralActionHelpers.tryGrowCoralBlock(rgCtx, theCoralBlockPos, pRand);

	}
	
	static boolean mobEatPlantsAction(ActionContext rgCtx) {

		boolean eaten = ActionHelpers.tryEatGrassOrFlower(rgCtx);
		if ((eaten) && (rgCtx.doDebug()))
			MyUtilities.debugMsg(2, ActionUtilities.getAdjustedPos(rgCtx.livingEntity()), rgCtx.key() + " ate plants.");
		return eaten;
	}

	// @SuppressWarnings("resource")
	static boolean mobGrowTallAction(ActionContext rgCtx) {

		LivingEntity le = rgCtx.livingEntity();
		ServerLevel serverLevel = rgCtx.serverLevel();
		String key = rgCtx.key();
		Block footBlock = rgCtx.footBlockState().getBlock();

		if ((footBlock instanceof TallGrassBlock) && (footBlock instanceof BonemealableBlock)) {
			BlockPos ePos = ActionUtilities.getAdjustedPos(le);
			if (!MyUtilities.getIdentifierString(serverLevel, footBlock).contains("byg")) {
				try {
					BonemealableBlock ib = (BonemealableBlock) footBlock;
					ib.performBonemeal(serverLevel, serverLevel.random, ePos, le.level().getBlockState(ePos));
					MyUtilities.debugMsg(2, ePos, key + " grew and hid in tall plant.");
					return false;

				} catch (Exception e) {
					MyUtilities.debugMsg(1, ePos, key + " caught grow tall attempt exception.");
					return false;
				}
			}
		}
		return false;
	}

}
