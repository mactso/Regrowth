package com.mactso.regrowth.actions;

import java.util.Optional;

import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
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
		
		Optional<ResourceKey<VillagerProfession>> optKey = ve.getVillagerData().profession().unwrapKey();

		if (optKey.isEmpty())
			return;

		Identifier id = optKey.get().registry();

		if (!id.getPath().contains("beekeeper"))
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
			MyUtilities.debugMsg(1, ve, "Beekeeper planted azure bluet flowers here.");
		}
	}

	static void vClericalHealing(ActionContext rgCtx) {
		Villager ve = rgCtx.ve();
		ServerLevel serverLevel = (ServerLevel) ve.level();

		if (!isVillagerProfession(ve, VillagerProfession.CLERIC))
			return;
		if (!ActionTests.isVillagerMeetingTime(serverLevel))
			return;

		int clericalLevel = getVillagerLevel(ve);
		int duration = clericalLevel * 51;
		int healingRange = 3 + clericalLevel;

		// Define area around the cleric
		BlockPos pos = BlockPos.containing(ve.getX(), ve.getY(), ve.getZ());
		AABB box = new AABB(pos).inflate(healingRange, 3, healingRange); // 9x5x9 area centered on cleric

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
		if (!isVillagerProfession(ve, VillagerProfession.FARMER))
			return false;

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
			
			// Weak anti-exploit: prevents villagers from quickly replacing torches at the exact same position.
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


	static boolean vImproveGrass(ActionContext rgCtx) {

		Villager ve = rgCtx.ve();
		Block fb = rgCtx.footBlock();
		if (isCuttable(fb)) {

		MutableBlockPos tmpBP = new MutableBlockPos();
		tmpBP.set(rgCtx.footBlockPos());
		rgCtx.serverLevel().destroyBlock(tmpBP, false);

		MyUtilities.debugMsg(1, ve, rgCtx.key() + " grass cut.");

		return true;
		
		}
		
		return false;
	}
	
	private static boolean isCuttable(Block b) {

		// this lets the same version of the code run over 1.21.6, 1.21.7, 1.21.8
		String className = b.getClass().getSimpleName();
		boolean isCuttablePlant = className.equals("LeafLitterBlock") // 1.21.8
				|| className.equals("ShortDryGrassBlock") // 1.21.8
				|| className.equals("TallDryGrassBlock") // 1.21.8
				|| (b instanceof TallGrassBlock) || (b instanceof DoublePlantBlock)
				|| b.getDescriptionId().equals("block.byg.short_grass");

		if (isCuttablePlant) {
			return true;
		}

//falls through if not a cuttable plant
		return false;
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
				MyUtilities.debugMsg(2, ve, rgCtx.key() + " cleared " + tempBlock.getDescriptionId());
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
				MyUtilities.debugMsg(1, ve, debugkey + " clear snow on road.");
		}

		if (RoadActionHelpers.roadFixPatch(rgCtx)) {
			if (doDebug)
				MyUtilities.debugMsg(1, ve, debugkey + " fix patches on road.");
			return; // early exit.
		}

		if (RoadActionHelpers.roadFixBump(rgCtx)) {
			if (doDebug)
				MyUtilities.debugMsg(1, ve, debugkey + "  fixed road bump.");
			return; // early exit.
		}

		if (RoadActionHelpers.roadFixPotholes(rgCtx)) {
			if (doDebug)
				MyUtilities.debugMsg(1, ve, debugkey + " fix potholes in road.");
			return; // early exit.
		}

		// smooth roads broken by cliffs and overhangs upwards or downwards.
		if (RoadActionHelpers.roadSmoothSlope(rgCtx)) {
			if (doDebug)
				MyUtilities.debugMsg(1, ve, debugkey + " Smooth road slope.");
		}
	}

	static void vImproveVillageWalls(ActionContext rgCtx) {

    
	    if (!WallActionHelpers.canBuildVillageWall(rgCtx)) return;

	    WallActionHelpers.improveVillageWall(rgCtx);

	    if (rgCtx.doDebug())
	        MyUtilities.debugMsg(1, rgCtx.ve(), "try improve Town Wall");
	}

	// Toolmasters heal Iron Golems. When they heal an Iron Golem, they heal
	// themselves too.
	static boolean vToolMasterHealing(ActionContext rgCtx) {

		Villager ve = rgCtx.ve();
		ServerLevel level = rgCtx.serverLevel();
		if (!ActionTests.isVillageMeetingTime(level))
			return false;

		// Only Toolsmithss heal
		if (!isVillagerProfession(ve, VillagerProfession.TOOLSMITH)) {
		    return false;
		}

		int villagerLevel = getVillagerLevel(ve);
		BlockPos vePos = ve.blockPosition(); // at villager's feet

		// Centered 13x13 horizontal, 7-block vertical region (±6 X/Z, ±3 Y)
		AABB box = new AABB(vePos).inflate(6, 3, 6);

		// Heal first Iron Golem that needs it then exit
		for (IronGolem golem : level.getEntities(EntityType.IRON_GOLEM, box, golem -> golem.isAlive())) {
			if (golem.getHealth() < golem.getMaxHealth() && !golem.hasEffect(MobEffects.REGENERATION)) {

				golem.addEffect(new MobEffectInstance(MobEffects.REGENERATION, villagerLevel * 41, 0), ve);
				ve.addEffect(new MobEffectInstance(MobEffects.REGENERATION, villagerLevel * 11, 0), ve);

				level.playSound(ve, vePos, SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.NEUTRAL, 0.5f, 0.5f);

				return true;
			}
		}

		return false;
	}
	
	// Multi Minecraft Version Helper methods
	public static boolean isVillagerProfession(Villager villager, ResourceKey<VillagerProfession> professionKey) {

		if (villager.getVillagerData().profession().is(professionKey))
			return true;
		return false;
		
	}
	
	public static int getVillagerLevel(Villager villager) {
		
		return villager.getVillagerData().level()+1;
		
	}
	

}
