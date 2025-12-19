package com.mactso.regrowth.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.WallBiomeDataManager;
import com.mactso.regrowth.utility.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class VillagerActions {
	
	static int lastTorchX = 0;
	static int lastTorchY = 0;
	static int lastTorchZ = 0;
	
	static final char ACTION_HEAL = 'h'; // heal villagers and players
	static final char ACTION_IMPROVE_LEAVES = 'v'; // cut/remove leaves
	static final char ACTION_CUT_GRASS = 'c'; // cut grass
	static final char ACTION_IMPROVE_ROADS = 'r'; // improve roads
	static final char ACTION_BUILD_WALLS = 'w'; // town wall build
	static final char ACTION_BUILD_FENCES = 'p'; // personal fence build
	static final char ACTION_IMPROVE_LIGHTING = 't'; // improve lighting (set Torch or lantern)

	static void vBeeKeeperPlantFlowers(ActionContext rgCtx) {
	
		Villager ve = rgCtx.ve();
		if (!ve.getVillagerData().getProfession().name().toLowerCase().contains("beekeeper"))
			return;
	
		// Only process certain X/Z positions
		if ((ve.getX() % 6 != 0) || (ve.getZ() % 7 != 0))
			return;
	
		BlockState groundBS = rgCtx.groundBlockState();
		BlockPos adjustedPos = rgCtx.adjustedPos();
	
		// Check if ground is grass or dirt
		if (!ActionTests.isGrassOrDirtBlock(groundBS))
			return;
	
		// Check orthogonal DIRT_PATH blocks below
		if (ActionUtilities.countBlocksOrthogonalBB(Blocks.DIRT_PATH, 1, rgCtx.serverLevel(),
				ve.blockPosition().below(), 0) == 1) {
			BlockState flowerBlockState = Blocks.AZURE_BLUET.defaultBlockState();
			rgCtx.serverLevel().setBlockAndUpdate(adjustedPos, flowerBlockState);
			Utility.debugMsg(1, ve, "Beekeeper planted azure bluet flowers here.");
		}
	}

	static void vClericalHealing(ActionContext rgCtx) {
		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = (ServerLevel) ve.level();
	
		// Only clerics heal
		if (ve.getVillagerData().getProfession() != VillagerProfession.CLERIC)
			return;
		if (!ActionTests.isVillagerMeetingTime(serverLevel))
			return;
	
		int clericalLevel = ve.getVillagerData().getLevel();
		int duration = clericalLevel * 51;
		int healingRange = 3 + clericalLevel;
	
		// Define area around the cleric
		BlockPos pos = BlockPos.containing(ve.getX(), ve.getY() + 0.99d, ve.getZ());
		AABB box = new AABB(pos).inflate(healingRange, 2, healingRange); // 9x5x9 area centered on cleric
	
		// Loop over nearby villagers and players, stop after healing one
		for (LivingEntity le : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
				e -> e instanceof Villager || e instanceof Player)) {
			if (ActionTests.isValidRegenerationTarget(le, ve)) {
	
				ActionHelpers.applyRegeneration(le, duration, ve, serverLevel, pos);
				return; // stop immediately after healing one entity
			}
		}
	}

	// if a grassblock in village has farmland next to it on the same level- retill
	// it.
	// todo add hydration check before tilling land.
	static boolean vImproveFarm(ActionContext rgCtx) {
	
		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = (ServerLevel) ve.level();
		if (ve.getVillagerData().getProfession() != VillagerProfession.FARMER) {
			return false;
		}
	
		BlockPos vePos = rgCtx.adjustedPos();
		Block groundBlock = rgCtx.groundBlock();
		Block footBlock = rgCtx.footBlock();
	
		if (ActionUtilities.countBlocksOrthogonalBB(Blocks.FARMLAND, 1, serverLevel, vePos.below(1), 0) > 0) {
			if (ActionTests.isNearWater(ve.level(), vePos.below(1))) {
				if (groundBlock instanceof GrassBlock) {
					serverLevel.setBlockAndUpdate(vePos.below(), Blocks.FARMLAND.defaultBlockState());
					return true;
				}
			}
	
			if (!(rgCtx.hasVillagerAction(ACTION_IMPROVE_LIGHTING)) || (footBlock != Blocks.AIR)) {
				return false;
			}
	
			// Special farm lighting torches.
			if (serverLevel.getBrightness(LightLayer.BLOCK, vePos) > 12) {
				return false; // block already bright enough
			}
	
			int veX = vePos.getX();
			int veY = vePos.getY();
			int veZ = vePos.getZ();
	
			if ((lastTorchX == veX) && (lastTorchY == veY) && (lastTorchZ == veZ)) {
				return false; // weak Anti torch-exploit
			}
	
			int waterValue = ActionUtilities.countBlocksOrthogonalBB(Blocks.WATER, 1, ve.level(), vePos.below(), 0);
			if ((waterValue > 0) && (rgCtx.groundBlockState().is(BlockTags.LOGS))
					|| (groundBlock == Blocks.SMOOTH_SANDSTONE)) {
				serverLevel.setBlockAndUpdate(vePos, Blocks.TORCH.defaultBlockState());
				lastTorchX = veX;
				lastTorchY = veY;
				lastTorchZ = veZ;
				return true;
			}
		}
		return false;
	}

	static void vImprovePowderedSnow(ActionContext rgCtx) {
		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = rgCtx.serverLevel();
	
		if (!ve.isInPowderSnow)
			return;
	
		int snowBonus = 0;
		MutableBlockPos tmpPos = new MutableBlockPos();
	
		int veX = ve.getBlockX();
		int veZ = ve.getBlockZ();
		for (int offset = 2; offset >= 0; offset--) {
			tmpPos.set(veX, ve.getBlockY() + offset, veZ);
			if (serverLevel.getBlockState(tmpPos).getBlock() == Blocks.POWDER_SNOW) {
				serverLevel.destroyBlock(tmpPos, false);
				snowBonus += 2;
			}
		}
		if (snowBonus == 0)
			return;
	
		tmpPos.set(veX, ve.getBlockY(), veZ);
		BlockState snowLayer = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, snowBonus);
		serverLevel.setBlockAndUpdate(tmpPos, snowLayer);
	}

	static void vImproveFences(ActionContext rgCtx) {
	
		Villager ve = rgCtx.ve();
		Brain<Villager> vb = ve.getBrain();
		ServerLevel serverLevel = (ServerLevel) ve.level();
	
		Optional<GlobalPos> vMeetingPlace = vb.getMemory(MemoryModuleType.MEETING_POINT);
		if (!(vMeetingPlace.isPresent())) {
			return;
		}
	
		if (ActionTests.isOkayToBuildWallHere(rgCtx)) {
			GlobalPos gVMP = vMeetingPlace.get();
			BlockPos villageMeetingPlaceBlockPos = gVMP.pos();
	
			if (!(serverLevel.getBlockState(villageMeetingPlaceBlockPos.above(1)).getBlock() instanceof WallBlock)) {
				return;
			}
	
			// build a wall on perimeter of villager's home
			if (rgCtx.hasVillagerAction(ACTION_BUILD_FENCES)) {
				Optional<GlobalPos> villagerHome = vb.getMemory(MemoryModuleType.HOME);
				if (!(villagerHome.isPresent())) {
					return;
				}
				GlobalPos gVHP = villagerHome.get();
				BlockPos villagerHomePos = gVHP.pos();
				// don't build personal walls inside the village wall perimeter.
				// don't build personal walls until the village has a meeting place.
				if (ActionTests.isOutsideMeetingPlaceWall(ve, vMeetingPlace, vMeetingPlace.get().pos(),
						rgCtx.localBiome())) {
					if (vImproveHomeFence(rgCtx, villagerHomePos)) {
					}
				}
			}
		}
	}

	// villagers build protective walls around their homes. currently 32 out.
	// to do- reduce distance of wall from home.
	static boolean vImproveHomeFence(ActionContext rgCtx, BlockPos vHomePos) {
	
		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = (ServerLevel) ve.level();
		BlockPos vePos = ActionUtilities.getAdjustedPos(ve);
		String biomeKey = "minecraft:" + rgCtx.biomeCategory();
		biomeKey = biomeKey.toLowerCase();
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(biomeKey);
		if (currentWallBiomeDataItem == null) {
	
			biomeKey = "minecraft:" + rgCtx.biomeCategory().toLowerCase();
			currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem(biomeKey);
			if (currentWallBiomeDataItem == null) {
				currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem("minecraft:plains");
			}
		}
	
		int homeFenceDiameter = currentWallBiomeDataItem.getWallLength();
		homeFenceDiameter = homeFenceDiameter / 4; // resize for personal home fence.
	
		int wallTorchSpacing = homeFenceDiameter / 4;
		homeFenceDiameter = (homeFenceDiameter / 2) - 1;
	
		Collection<PoiRecord> result = ((ServerLevel) ve.level()).getPoiManager()
				.getInSquare(t -> t == PoiTypes.HOME, vePos, 17, Occupancy.ANY)
				.collect(Collectors.toCollection(ArrayList::new));
	
		// 08/30/20 Collection had bug with range that I couldn't resolve.
		boolean buildFence = true;
		if (!(result.isEmpty())) {
			Iterator<PoiRecord> i = result.iterator();
			while (i.hasNext()) { // in 16.1, finds the point of interest.
				PoiRecord P = i.next();
				if ((vHomePos.getX() == P.getPos().getX()) && (vHomePos.getY() == P.getPos().getY())
						&& (vHomePos.getZ() == P.getPos().getZ())) {
					continue; // ignore meeting place that owns this wall segment.
				} else {
					int disX = Math.abs(vePos.getX() - P.getPos().getX());
					int disZ = Math.abs(vePos.getZ() - P.getPos().getZ());
					Utility.debugMsg(1, P.getPos(), "extra Point of Interest Found.");
					if ((disX < homeFenceDiameter) && (disZ < homeFenceDiameter)) {
						buildFence = false; // another meeting place too close. cancel wall.
						break;
					}
				}
			}
		} else if ((result.isEmpty())) {
			buildFence = true;
		}
	
		if (buildFence) {
	
			BlockState fenceBlockState = currentWallBiomeDataItem.getFenceBlockState();
	
			if (ActionHelpers.tryPlaceOneWallPiece(rgCtx, homeFenceDiameter, wallTorchSpacing, fenceBlockState,
					vHomePos)) {
	
				if (rgCtx.hasVillagerAction(ACTION_IMPROVE_LIGHTING)) {
					if (ActionTests.isValidTorchLocation(homeFenceDiameter, wallTorchSpacing,
							ActionUtilities.getAbsVX(ve, vHomePos), ActionUtilities.getAbsVZ(ve, vHomePos),
							serverLevel.getBlockState(vePos).getBlock())) {
						serverLevel.setBlockAndUpdate(vePos.above(), Blocks.TORCH.defaultBlockState());
					}
				}
				ActionHelpers.jumpUp(ve);
				return true;
			}
		}
	
		return false;
	}

	static boolean vImproveGrass(ActionContext rgCtx) {
	
		Villager ve = rgCtx.ve();
		Block fb = rgCtx.footBlock();
	
		if (!(fb instanceof TallGrassBlock || fb instanceof DoublePlantBlock
				|| fb.getDescriptionId().equals("block.byg.short_grass"))) {
			return false;
		}
	
		MutableBlockPos tmpBP = new MutableBlockPos();
		tmpBP.set(rgCtx.footBlockPos());
		rgCtx.serverLevel().destroyBlock(tmpBP, false);
	
		Utility.debugMsg(1, ve, rgCtx.key() + " grass cut.");
	
		return true;
	}

	// ---------------------
	// remove non persistant leaves or cactus
	// ---------------------
	static boolean vImproveLeavesOrCactus(ActionContext rgCtx) {
	
		Villager ve = rgCtx.ve();
		BlockPos vePos = rgCtx.adjustedPos();
		ServerLevel serverLevel = rgCtx.serverLevel();
	
		// Compute facing direction
		float veYaw = ve.getViewYRot(1.0f) / 45f;
		int facingNdx = ((Math.round(veYaw) % 8) + 8) % 8; // ensure always in range 0-7
		int dx = ActionConstants.facingArray[facingNdx][0];
		int dz = ActionConstants.facingArray[facingNdx][1];
	
		int veX = vePos.getX();
		int veY = vePos.getY();
		int veZ = vePos.getZ();
	
		boolean anyDestroyed = false;
		MutableBlockPos tmpPos = new MutableBlockPos();
	
		for (int iY = 0; iY < 2; iY++) { // checks 2 blocks in front of villagers facing
	
			tmpPos.set(veX + dx, veY + iY, veZ + dz);
	
			BlockState tempBS = serverLevel.getBlockState(tmpPos);
			Block tempBlock = tempBS.getBlock();
	
			if (!ActionTests.isCactusOrRemovableLeaves(tempBS))
				continue;
	
			serverLevel.destroyBlock(tmpPos, false);
			serverLevel.playSound(ve, ve.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.BLOCKS,
					/* Volume */ 0.75f, /* Pitch */ 0.75f);
			anyDestroyed = true;
			if (rgCtx.doDebug())
				Utility.debugMsg(2, ve, rgCtx.key() + " cleared " + tempBlock.getDescriptionId());
		}
		return anyDestroyed;
	}

	// ---------------------
	// Place torches if a block lacks enough lighting.
	// ---------------------
	static boolean vImproveLighting(ActionContext rgCtx) {
		Villager ve = rgCtx.ve();
	
		if (ve.isBaby())
			return false;
	
		if (!ActionTests.isDark(rgCtx.serverLevel(), rgCtx.adjustedPos()))
			return ActionHelpers.tryPlaceTorch(rgCtx);
	
		return false;
	
	}

	static void vImproveRoads(ActionContext rgCtx, String debugkey) {
		Villager ve = rgCtx.ve();
		Boolean doDebug = rgCtx.doDebug();
	
		if (RoadActionHelpers.roadFixSnow(rgCtx)) {
			if (doDebug)
				Utility.debugMsg(1, ve, debugkey + " clear snow on road.");
		}
	
		if (RoadActionHelpers.roadFixPatch(rgCtx)) {
			if (doDebug)
				Utility.debugMsg(1, ve, debugkey + " fix patches on road.");
			return; // early exit.
		}
	
		if (RoadActionHelpers.roadFixBump(rgCtx)) {
			if (doDebug)
				Utility.debugMsg(1, ve, debugkey + "  fixed road bump.");
			return; // early exit.
		}
	
		if (RoadActionHelpers.roadFixPotholes(rgCtx)) {
			if (doDebug)
				Utility.debugMsg(1, ve, debugkey + " fix potholes in road.");
			return; // early exit.
		}
	
		// smooth roads broken by cliffs and overhangs upwards or downwards.
		if (RoadActionHelpers.roadSmoothSlope(rgCtx)) {
			if (doDebug)
				Utility.debugMsg(1, ve, debugkey + " Smooth road slope.");
		}
	}

	static void vImproveVillageWalls(ActionContext rgCtx) {
	
		if (MyConfig.isWallBuildingOff())
			return;
	
		// okay for wall block to replace air, grass, flowers, etc.
		if (!(ActionTests.isFootBlockOkayToBuildIn(rgCtx))) {
			return;
		}
	
		// don't build a wall on top of a wall
		if (rgCtx.groundBlock() instanceof WallBlock) {
			return;
		}
	
		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = rgCtx.serverLevel(); // cast for server methods
	
		Optional<GlobalPos> optVmp = ve.getBrain().getMemory(MemoryModuleType.MEETING_POINT);
		if (optVmp.isEmpty())
			return;
	
		GlobalPos villageMeetingPoint = optVmp.get();
		BlockPos villageMeetingPointPos = villageMeetingPoint.pos();
	
		if (serverLevel.getBlockState(villageMeetingPointPos).getBlock() != Blocks.BELL)
			return;
	
		// if in a new chunk set the wall control block over the village bell to test
		// later
		BlockPos controlWallPos = villageMeetingPointPos.above();
		if (ActionTests.isNewChunk(serverLevel, villageMeetingPointPos)) {
			serverLevel.setBlockAndUpdate(controlWallPos, MyConfig.getPlayerWallControlBlock().defaultBlockState());
		}
	
		Block controlWallBlock = serverLevel.getBlockState(controlWallPos).getBlock();
		if (controlWallBlock != MyConfig.playerWallControlBlock) {
			return;
		}
	
		if (rgCtx.groundBlockState().isAir()) // hanging Chad exception.
			return; // ignore edge case where villager is literally hanging on the edge of a block.
	
		if (!ActionTests.isOkayToBuildWallHere(rgCtx))
			return;
	
		BlockPos adjustedPos = rgCtx.adjustedPos();
		if (serverLevel.getBlockState(adjustedPos).getBlock() == Blocks.DIRT_PATH) {
			return;
		}
	
		if (serverLevel.getBlockState(adjustedPos.below()).getBlock() == Blocks.DIRT_PATH) {
			return;
		}
	
		// don't block a door with a wall block.
		if (ActionTests.hasAdjacentDoor(serverLevel, adjustedPos))
			return;
	
		// looks good! Let's improve the wall block relative to the meeting point.
		vImproveVillageWall(rgCtx, villageMeetingPointPos);
		if (rgCtx.doDebug())
			Utility.debugMsg(1, rgCtx.ve(), "try improve Town Wall");
	}

	static boolean vImproveVillageWall(ActionContext rgCtx, BlockPos villageMeetingPointPos) {
	
		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = rgCtx.serverLevel(); // cast for server methods
		BlockPos vePos = rgCtx.adjustedPos();
		String wallKey = ("minecraft:" + rgCtx.biomeCategory()).toLowerCase();
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(wallKey);
	
		int wallRadius = (currentWallBiomeDataItem.getWallLength() / 2) - 1;
	
		// Masons bonus building ability can build walls from anywhere
		if (ve.getVillagerData().getProfession() == VillagerProfession.MASON) {
			ActionHelpers.tryBuildMasonWalls(rgCtx, wallRadius, villageMeetingPointPos, currentWallBiomeDataItem);
		}
	
		// No workstations within 2 blocks, only 1 village meeting point in wall radius
		if (!ActionTests.isWallBuildAllowedByPOIs(rgCtx, wallRadius))
			return false;
	
		BlockState wallBlock = currentWallBiomeDataItem.getWallBlockState();
		int wallTorchSpacing = (wallRadius + 1) / 4;
	
		if (ActionHelpers.tryPlaceOneWallPiece(rgCtx, wallRadius, wallTorchSpacing, wallBlock,
				villageMeetingPointPos)) {
			if (rgCtx.hasVillagerAction(ACTION_IMPROVE_LIGHTING)) {
				if (ActionTests.isValidTorchLocation(wallRadius, wallTorchSpacing,
						ActionUtilities.getAbsVX(ve, villageMeetingPointPos),
						ActionUtilities.getAbsVZ(ve, villageMeetingPointPos),
						serverLevel.getBlockState(vePos).getBlock())) {
					serverLevel.setBlockAndUpdate(vePos.above(), Blocks.TORCH.defaultBlockState());
				}
			}
	
			ActionHelpers.jumpUp(ve);
			if (rgCtx.doDebug())
				Utility.debugMsg(2, ve, "Villager built one block of wall.");
			return true;
		}
	
		return false;
	}

	// Toolmasters heal Iron Golems. When they heal an Iron Golem, they heal
	// themselves too.
	static boolean vToolMasterHealing(ActionContext rgCtx) {
	
		Villager ve = rgCtx.ve();
		ServerLevel level = rgCtx.serverLevel();
		if (!ActionTests.isVillageMeetingTime(level))
			return false;
	
		// Only Toolsmithss heal
		if (ve.getVillagerData().getProfession() != VillagerProfession.TOOLSMITH)
			return false;
	
		int villagerLevel = ve.getVillagerData().getLevel() + 1;
		BlockPos vePos = ve.blockPosition(); // at villager's feet
	
		// Centered 13x13 horizontal, 7-block vertical region (±6 X/Z, ±3 Y)
		AABB box = new AABB(vePos).inflate(6, 3, 6);
	
		// Heal first Iron Golem that needs it then exit
		for (IronGolem golem : level.getEntities(EntityType.IRON_GOLEM, box, e -> true)) {
			if (golem.getHealth() < golem.getMaxHealth() && !golem.hasEffect(MobEffects.REGENERATION)) {
	
				golem.addEffect(new MobEffectInstance(MobEffects.REGENERATION, villagerLevel * 41, 0), ve);
				ve.addEffect(new MobEffectInstance(MobEffects.REGENERATION, villagerLevel * 11, 0), ve);
	
				level.playSound(ve, vePos, SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.NEUTRAL, 0.5f, 0.5f);
	
				return true;
			}
		}
	
		return false;
	}

}
