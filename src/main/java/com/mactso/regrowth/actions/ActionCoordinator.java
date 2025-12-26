package com.mactso.regrowth.actions;

import java.util.Map;
import java.util.WeakHashMap;

import com.mactso.regrowth.managers.RegrowthEntitiesManager;
import com.mactso.regrowth.managers.RegrowthEntitiesManager.RegrowthMobItem;
import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ActionCoordinator {

	static final int CHECKS_PER_SECOND = 10;
	static final int TICKS_PER_SECOND = 20;

	// Track starting time per ServerLevel to support multiple worlds/dimensions
	private static final Map<ServerLevel, Long> startingTimes = new WeakHashMap<>();

	// A world becomes stable for Regrowth when it has ticked for 1 second.
	public static boolean isWorldStable(ServerLevel serverLevel) {

		long gametime = serverLevel.getGameTime();
		// Initialize starting time for this world if needed
		startingTimes.putIfAbsent(serverLevel, gametime);
		long startingTime = startingTimes.get(serverLevel);

		if ((MyConfig.getDebugLevel() >= 2 && (gametime % 20 == 0))) 
			MyUtilities.debugMsg(2, String.format("World: %s, gametime: %d, startingTime: %d",
					serverLevel.dimension().location(), gametime, startingTime));

		// Return true only when the world has advanced at least one second
		return gametime >= startingTime + TICKS_PER_SECOND;

	}
	
	@org.jetbrains.annotations.Nullable
	private static ServerLevel validateAndGetServerLevel(LivingEntity le) {

	    Level level = le.level(); // redundant protection
	    if (!(level instanceof ServerLevel serverLevel))
	        return null;

	    if (!isWorldStable(serverLevel))
	        return null;

	    if (!le.isAlive())
	        return null;

	    // tick throttling: even id goes on even, odd id goes on odd.  Half the load.
	    if (le.getId() % 2 == serverLevel.getGameTime() % 2)
	        return null;

	    if (le.blockPosition() == null)
	        return null;

	    // Must be grounded unless bat or swimming
	    if (!le.onGround() && !(le instanceof Bat)) {
	        if (!le.isInWater())
	            return null;
	    }

	    return serverLevel;
	}

	// this determines the world and entity state are valid.
	// then it calculates the odds based on the config setting of seconds.
	// Eg. 300 seconds would result 1/300 odds per second of mob action.
	
	public static void coordinateLivingEntityActions (LivingEntity le) {

		if (MyConfig.getDebugLevel() > 0)
			MyUtilities.debugMsg(1, "Enter DoRegrowthSwitchboard");

		ServerLevel serverLevel = validateAndGetServerLevel(le);
		if (serverLevel == null)
		    return;

		ActionContext rgCtx = ActionContext.buildRgCtx(le);
		if (rgCtx == null)
			return;

		String rlString = MyUtilities.getResourceLocationString(le).toString();
		RegrowthMobItem currentRegrowthMobItem = RegrowthEntitiesManager.getRegrowthMobInfo(rlString);
		if (currentRegrowthMobItem == null)
			return;
		String regrowthActions = currentRegrowthMobItem.getRegrowthActions();

		BlockState footBlockState = rgCtx.footBlockState();
		Block footBlock = footBlockState.getBlock();

		if (!ActionTests.isFootblockValid(footBlockState))
			return;
		if (ActionTests.isImpossibleRegrowthEvent(regrowthActions, footBlockState, footBlock))
			return;

		// improve village roads and walls faster for the first 200 minutes;
	    if (le instanceof Villager ve) {
			doYoungChunkVillagerBonusActions(rgCtx); // bonus actions
	    }

		double regrowthEventOdds = 1 / (currentRegrowthMobItem.getRegrowthEventSeconds() * CHECKS_PER_SECOND);

		if (ActionTests.isHorseTypeEatingNow(le)) {
			regrowthEventOdds *= 20; // 20x odds when eating animation
		}
		double randomD100Roll = serverLevel.random.nextDouble();
		randomD100Roll = regrowthEventOdds; // TODO randomD100Roll Testing Override
		if (randomD100Roll <= regrowthEventOdds) {
			nudgeEntityTowardsCenter(rgCtx);
			if (le instanceof Villager ve) {
				ActionCoordinator.doVillagerActions(rgCtx);
			} else {
				ActionCoordinator.doMobActions(rgCtx);
			}
		}
		if (rgCtx.doDebug())
			MyUtilities.debugMsg(1, "fall out of doRegrowthSwitchBoard");
	}
	
	/**
	 * Gently nudges a LivingEntity toward the horizontal center of a block
	 * to reduce edge-case positioning near block edges.
	 *
	 * @param entity The living entity to move
	 * @param pos The target block position
	 */
	public static void nudgeEntityTowardsCenter(ActionContext rgCtx) {
	    final double CURRENT_WEIGHT = 5.0; // weight for entity's current position
	    final double CENTER_WEIGHT = 1.0;  // weight for block center
	    final double TOTAL_WEIGHT = CURRENT_WEIGHT + CENTER_WEIGHT;

	    LivingEntity le = rgCtx.livingEntity();
	    BlockPos pos = le.blockPosition();
	    double blockCenterX = pos.getX() + 0.5;
	    double blockCenterZ = pos.getZ() + 0.5;

	    double newX = (le.getX() * CURRENT_WEIGHT + blockCenterX * CENTER_WEIGHT) / TOTAL_WEIGHT;
	    double newZ = (le.getZ() * CURRENT_WEIGHT + blockCenterZ * CENTER_WEIGHT) / TOTAL_WEIGHT;
	    double newY = le.getY(); // keep vertical position

	    le.teleportTo(newX, newY, newZ);
	}

	// -------------------------
	// In this method, every villager will attempt every action. Some will qualify
	// based on the blocks around.
	// Based on profession, certain villagers will try special actions.
	// Triggered every ~5 seconds, not tick-by-tick, so performance is trivial.
	// -------------------------
	static void doVillagerActions(ActionContext rgCtx) {
	
		// Skip if villager is still inside a road it fixed.
		if (rgCtx.isInsideRoadBlock())
			return;
	
		// Skip if villager is inside a road it built or pressed against a wall.
		if (rgCtx.isInsideWallBlock())
			return;
	
		// There are no actions for Villagers on beds
		if (rgCtx.isInBed())
			return;
	
		Villager ve = rgCtx.ve();
	
		// There are no actions for sleeping Villagers (probably redundant)
		if (ve.isSleeping())
			return;
	
		VillagerActions.vImprovePowderedSnow(rgCtx);
	
		// Prevent Villagers who are hopping, falling, etc. from trying actions.
		if (!(ve.onGround()))
			return;
	
		String key = rgCtx.key();
	
		// If debugging is on, give special debugging names to unnamed villagers.
		ActionHelpers.updateDebugName(ve);
	
		// note all villagers may not have a home. poor homeless villagers.
		// Universal villager action: farmland maintenance
		if (VillagerActions.vImproveFarm(rgCtx)) {
			MyUtilities.debugMsg(1, ve, rgCtx.key() + " farm improved.");
		}
	
		// 'h'eal villagers and players
		if (rgCtx.hasVillagerAction(VillagerActions.ACTION_HEAL)) {
	
			VillagerActions.vClericalHealing(rgCtx);
	
			if (VillagerActions.vToolMasterHealing(rgCtx)) {
				MyUtilities.debugMsg(1, ve, rgCtx.key() + " Iron Golem healed");
			}
	
		}
	
		// after this methods may modify the footblock. so skip if standing in a torch
		// block.
	
		if (rgCtx.isInTorchBlock())
			return;
	
		VillagerActions.vBeeKeeperPlantFlowers(rgCtx);

		if (rgCtx.hasVillagerAction(VillagerActions.ACTION_IMPROVE_LEAVES)) {
			if (VillagerActions.vImproveLeavesOrCactus(rgCtx))
				MyUtilities.debugMsg(1, ve, key + " leaves cut.");
	
		}
	
		if (rgCtx.hasVillagerAction(VillagerActions.ACTION_CUT_GRASS)) {
			if (VillagerActions.vImproveGrass(rgCtx)) {
				MyUtilities.debugMsg(1, ve, key + " grass cut.");
			}
		}
	
		if (rgCtx.hasVillagerAction(VillagerActions.ACTION_IMPROVE_ROADS)) {
			VillagerActions.vImproveRoads(rgCtx, key);
			MyUtilities.debugMsg(1, ve, key + " tried to road improve.");
			if (rgCtx.footBlock() == rgCtx.biomeRoadBlock()) {
				return; // villager now inside road jumping out.. skip further actions.
			}
		}
	
		if (rgCtx.hasVillagerAction(VillagerActions.ACTION_BUILD_WALLS)) {
			VillagerActions.vImproveVillageWalls(rgCtx);
			MyUtilities.debugMsg(1, ve, " tried to build town wall.");
		}
	
	
		if (rgCtx.hasVillagerAction(VillagerActions.ACTION_IMPROVE_LIGHTING)) {
			if (VillagerActions.vImproveLighting(rgCtx)) {
				MyUtilities.debugMsg(1, ve, key + "-" + rgCtx.footBlock() + ", " + rgCtx.groundBlock() + " pitch: "
						+ ve.getXRot() + " lighting improved.");
			}
		}
	
		MyUtilities.debugMsg(1, "exit Village Events");
	}
	
	private static void doYoungChunkVillagerBonusActions(ActionContext rgCtx) {
		
	    BlockPos aPos = rgCtx.adjustedPos();
	    if (aPos == null) return;
	    ServerLevel serverLevel = rgCtx.serverLevel();
	    long chunkAge = serverLevel.getChunkAt(aPos).getInhabitedTime();

	    if (chunkAge >= 480_000) return; // only first 200 minutes

	    if (serverLevel.getGameTime() % 12 != 0) return; // every 12th tick so not too fast.

	    if (rgCtx.hasVillagerAction(VillagerActions.ACTION_IMPROVE_ROADS)) {
	        VillagerActions.vImproveRoads(rgCtx, "preRoad");
	        if (rgCtx.footBlock() == rgCtx.biomeRoadBlock()) {
	            return; // villager now inside road, skip further actions
	        }
	    }

	    if (rgCtx.hasVillagerAction(VillagerActions.ACTION_BUILD_WALLS)) {
	        VillagerActions.vImproveVillageWalls(rgCtx);
	    }
	}

	static void doMobActions(ActionContext rgCtx) {
	
		LivingEntity le = rgCtx.livingEntity();
	
		MyUtilities.debugMsg(1, le, "enter doMobRegrowthActions");
		// this may hurt or kill entities.
		MobActions.mobHandleOverCrowding(rgCtx);
		if (!rgCtx.livingEntity().isAlive())
			return;
	
		// Actions that do not require grass underfoot
		if (rgCtx.hasMobAction(MobActions.ACTION_STUMBLE)) {
			MobActions.mobStumbleAction(rgCtx);
			return;
		}
	
		if (rgCtx.hasMobAction(MobActions.ACTION_REFOREST)) {
			MobActions.mobReforestAction(rgCtx);
			return;
		}
	
		if (rgCtx.hasMobAction(MobActions.ACTION_MUSHROOM)) {
			MobActions.mobHugeMushroomAction(rgCtx);
			return;
		}
	
		if (rgCtx.hasMobAction(MobActions.ACTION_CORAL)) {
			MobActions.mobCoralAction(rgCtx);
			return;
		}
	
		// Remaining actions require a grass block underfoot for performance
		if (!ActionTests.isKindOfGrassBlock(rgCtx.groundBlockState())) {
			return;
		}
	
	
		if (rgCtx.hasMobAction(MobActions.ACTION_TALL)) {
			MobActions.mobGrowTallAction(rgCtx);
			return;
		}
		
		if (rgCtx.hasMobAction(MobActions.ACTION_BOTH)) {
			if (rgCtx.getRand().nextDouble() < 0.85) {
				MobActions.mobEatPlantsAction(rgCtx);
				return;
			} else {
				MobActions.mobGrowPlantsAction(rgCtx);
				return;
			}
		}
		
	
		if (rgCtx.hasMobAction(MobActions.ACTION_EAT)) {
			MobActions.mobEatPlantsAction(rgCtx);
			return;
		}
		
		if (rgCtx.hasMobAction(MobActions.ACTION_GROW)) {
			MobActions.mobGrowPlantsAction(rgCtx);
		}
	
		MyUtilities.debugMsg(1, le, "fall out of doMobRegrowthActions");
	}

}
