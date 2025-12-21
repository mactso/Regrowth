package com.mactso.regrowth.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.WallBiomeDataManager;
import com.mactso.regrowth.config.WallFoundationManager;
import com.mactso.regrowth.utility.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.FarmlandWaterManager;

//-----------------------
//Action Tests answer true/false questions
//-----------------------
public class ActionTests {

	public static boolean isGrassOrDirtBlock(BlockState tempBlockState) {

		if (ActionTests.isKindOfGrassBlock(tempBlockState) || (ActionTests.isKindOfDirtBlock(tempBlockState))) {
			return true;
		}
		return false;
	}

	public static boolean isGrassOrFlower(BlockState footBlockState) {
		Block footBlock = footBlockState.getBlock();

		if (footBlock instanceof TallGrassBlock) {
			return true;
		}
		if (footBlock instanceof FlowerBlock) {
			return true;
		}
		if (footBlock instanceof DoublePlantBlock) {
			return true;
		}
		if (footBlock == Blocks.FERN) {
			return true;
		}
		if (footBlock == Blocks.LARGE_FERN) {
			return true;
		}
		// compatibility with other biome mods.
		try {
			if (footBlockState.is(BlockTags.FLOWERS)) {
				return true;
			}
			if (footBlockState.is(BlockTags.TALL_FLOWERS)) {
				return true;
			}
		} catch (Exception e) {
			if (MyConfig.getDebugLevel() > 0) {
				System.out.println("Tag Exception 1009-1014:" + footBlock.getDescriptionId() + ".");
			}
		}
		// biomes you'll go grass compatibility
		if (footBlock.getDescriptionId().equals("block.byg.short_grass")) {
			return true;
		}
		if (MyConfig.getDebugLevel() > 0) {
			System.out.println("Not grass or Flower:" + footBlock.getDescriptionId() + ".");
		}
		return false;
	}

	public static boolean isKindOfGrassBlock(BlockState groundBlockState) {
		if (groundBlockState.getBlock() instanceof GrassBlock)
			return true;
		if (groundBlockState.getBlock().getDescriptionId().equals("block.byg.meadow_grass_block"))
			return true;
		return false;
	}

	public static boolean isKindOfDirtBlock(BlockState groundState) {

		Block block = groundState.getBlock();
		if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.PODZOL) {
			return true;
		}

		// Then check if the block is tagged as dirt (modded dirt support)
		if (groundState.is(BlockTags.DIRT)) {
			return true;
		}

		return false;
	}

	public static boolean isHorseTypeEatingNow(Entity entity) {
		if (entity instanceof AbstractHorse) {
			AbstractHorse h = (AbstractHorse) entity;
			if (h.isEating()) {
				return true;
			}
		}
		return false;
	}


	public static boolean isNearWater(LevelReader level, BlockPos pos) {
		// TODO This also gets lava, so change later to
		// getFluidState(p_46802_).is(FluidTags.WATER);
		AABB box = AABB.encapsulatingFullBlocks(pos.east(4).north(4), pos.west(4).south(4).below(1));
		// new AABB(pos.east(4).north(4), pos.west(4).south(4).below(1));
		if (level.containsAnyLiquid(box)) {
			return true;
		}

		// Ask Forge Farmland Manager if some other mod is hydrating the block.
		return FarmlandWaterManager.hasBlockWaterTicket(level, pos);
	}

	public static boolean isOnGround(Entity e) {
		return e.onGround();
	}

	static int SHORT_RANGE = 2;

	public static boolean isWallBuildAllowedByPOIs(ActionContext rgCtx, int wallRadius) {

		ServerLevel serverLevel = rgCtx.serverLevel();
		BlockPos vePos = rgCtx.ve().blockPosition();

		// 1. Short-range check: any very close POI blocks wall building
		// 2. This is slower but this code doesn't work in 1.21.5
		//		boolean hasClosePOI =
		//		        serverLevel.getPoiManager()
		//		                .getInSquare(t -> true, vePos, SHORT_RANGE, Occupancy.ANY)
		//		                .findFirst()
		//		                .isPresent();

		List<PoiRecord> closePois = serverLevel.getPoiManager()
				.getInSquare(t -> true, vePos, SHORT_RANGE, Occupancy.ANY).toList();

		if (closePois.size() > 0)
			return false;

		// 2. Wall-radius check: must have exactly one meeting point at wall radius
		// distance

		Collection<PoiRecord> result = serverLevel.getPoiManager().getInSquare(t -> true, vePos, wallRadius, Occupancy.ANY)
				.collect(Collectors.toCollection(ArrayList::new));

		int count = 0;
		for (PoiRecord rec : result) {
			if (rec.getPoiType().is(PoiTypes.MEETING)) {
				if (++count > 1)
					return false;
			}
		}

		if (count != 1)
			return false;

		return true;
	}

	public static boolean isValidTorchLocation(int wallRadius, int wallTorchSpacing, int absvx, int absvz,
			Block wallFenceBlock) {

		boolean hasAWallUnderIt = false;
		if (wallFenceBlock instanceof WallBlock) {
			hasAWallUnderIt = true;
		}
		if (wallFenceBlock instanceof FenceBlock) {
			hasAWallUnderIt = true;
		}
		if (!(hasAWallUnderIt)) {
			return false;
		}
		if ((absvx == wallRadius) && ((absvz % wallTorchSpacing) == 1)) {
			return true;
		}
		if (((absvx % wallTorchSpacing) == 1) && (absvz == wallRadius)) {
			return true;
		}
		if ((absvx == wallRadius) && (absvz == wallRadius)) {
			return true;
		}

		return false;
	}

	public static boolean isFoundationValid(ActionContext rgCtx) {

		Villager ve = rgCtx.ve();
		Block groundBlock = rgCtx.groundBlock();
		Block footBlock = rgCtx.footBlock();

		Utility.debugMsg(1, ve, "isFoundationValid? : gb" + groundBlock.toString() + ", fb:" + footBlock.toString());

		if (WallFoundationManager.isValid(groundBlock))
			return true;

		if (ve.level().getBrightness(LightLayer.SKY, ve.blockPosition()) < 13)
			return false;

		if (groundBlock instanceof WallBlock)
			return false;

		if (groundBlock instanceof TorchBlock)
			return false;

		if (groundBlock instanceof SnowLayerBlock)
			return false;

		if (groundBlock instanceof TorchBlock)
			return false; // includes WallTorchBlock

		return true;

	}

	private static final int POI_RANGE = 3;

	public static boolean isNearbyPoi(ActionContext rgCtx) {

		Collection<PoiRecord> result = (rgCtx.serverLevel()).getPoiManager()
				.getInSquare(t -> true, rgCtx.ve().blockPosition(), POI_RANGE, Occupancy.ANY)
				.collect(Collectors.toCollection(ArrayList::new));

		// 08/30/20 Collection pre 16.2 bug returns poi that are out of range too.
		// So retain the old looping code for 1.16.2 and older versions.

		if (!result.isEmpty())
			return true;

		return false;

	}

	static boolean isFootBlockOkayToBuildIn(ActionContext rgCtx) {

		BlockState footBlockState = rgCtx.footBlockState();
		if ((footBlockState.isAir()) || (isGrassOrFlower(footBlockState))) {
			return true;
		}
		if (footBlockState.getBlock() instanceof SnowLayerBlock) {
			return true;
		}
		if (ActionTests.isNatProgPebbleOrStick(rgCtx))
			return true;

		return false;
	}

	// handle Natural Progression pebbles and sticks and Minecraft Buttons placed to
	// imitate "stones".
	static boolean isNatProgPebbleOrStick(ActionContext rgCtx) {

		String rl = Utility.getResourceLocationString(rgCtx.serverLevel(), rgCtx.footBlock());

		if ((rl.contains("natprog")) && (rl.contains("pebble")))
			return true;

		if ((rl.contains("natprog")) && (rl.contains("twigs")))
			return true;

		if ((rl.contains("minecraft")) && (rl.contains("button"))) {
			return true;
		}
		return false;
	}

	// Sapling spacing checks
	public static final int SAPLING_NEARBY_RADIUS = 5; // horizontal radius to search for existing saplings
	public static final int SAPLING_NEARBY_YRANGE = 0; // no vertical scan needed

	// Leaf-overhead checks (default values)
	public static final int LEAF_CHECK_RADIUS_DEFAULT = 4;
	public static final int LEAF_CHECK_Y_OFFSET_DEFAULT = 4;

	// Acacia adjustments
	public static final int LEAF_CHECK_RADIUS_ACACIA = 7;
	public static final int LEAF_CHECK_Y_OFFSET_ACACIA = 5;

	static boolean isTooCloseToSapling(ServerLevel serverLevel, BlockPos pos) {
		return ActionUtilities.countBlocksInBox(SaplingBlock.class, 1, serverLevel, pos,
				ActionTests.SAPLING_NEARBY_RADIUS, SAPLING_NEARBY_YRANGE) > 0;
	}

	static boolean isTreeAbove(Level level, BlockState sapling, BlockPos pos) {
		int leafYOffset = sapling.is(Blocks.ACACIA_SAPLING) ? LEAF_CHECK_Y_OFFSET_ACACIA : LEAF_CHECK_Y_OFFSET_DEFAULT;

		int leafRadius = sapling.is(Blocks.ACACIA_SAPLING) ? LEAF_CHECK_RADIUS_ACACIA : LEAF_CHECK_RADIUS_DEFAULT;

		return ActionUtilities.countBlocksInBox(LeavesBlock.class, 1, level, pos.above(leafYOffset), leafRadius, 0) > 0;
	}

	static boolean isImpossibleRegrowthEvent(String regrowthType, BlockState testFootBlockState, Block testFootBlock) {
		if ((regrowthType.equals("eat")) && (testFootBlockState.isAir())) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (testFootBlock instanceof TallGrassBlock)) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (testFootBlock instanceof FlowerBlock)) {
			return true;
		}
		if ((regrowthType.equals("tall")) && (!(testFootBlock instanceof TallGrassBlock))) {
			return true;
		}
		return false;
	}

	static boolean isOutsideMeetingPlaceWall(Villager ve, Optional<GlobalPos> vMeetingPlace, BlockPos meetingPlacePos,
			Biome localBiome) {

		BlockPos vePos = ActionUtilities.getAdjustedPos(ve);
		String key = "minecraft:" + localBiome.toString(); // TODO probably broken.

		int wallDiameter = 64;
		key = key.toLowerCase();
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		if (!(currentWallBiomeDataItem == null)) {
			wallDiameter = currentWallBiomeDataItem.getWallLength();
		}
		wallDiameter = (wallDiameter / 2) - 1;
		int absVMpX = (int) Math.abs(vePos.getX() - meetingPlacePos.getX());
		int absVMpZ = (int) Math.abs(vePos.getZ() - meetingPlacePos.getZ());
		if ((absVMpX > wallDiameter + 1))
			return true;
		if ((absVMpZ > wallDiameter + 1))
			return true;
		return false;

	}

	static boolean isFootblockValid(BlockState state) {
		Block block = state.getBlock();

		// skip carpets
		if (block instanceof WoolCarpetBlock)
			return false;

		// skip bottom slabs only
		if (block instanceof SlabBlock slab) {
			SlabType type = state.getValue(SlabBlock.TYPE);
			if (type == SlabType.BOTTOM)
				return false;
		}

		// skip stairs entirely
		if (block instanceof StairBlock)
			return false;

		// skip trapdoors entirely
		if (block instanceof TrapDoorBlock)
			return false;

		// skip string
		if (block instanceof TripWireBlock)
			return false;

		// skip redstone (wire and blocks)
		if (block instanceof RedStoneWireBlock)
			return false;

		// falls through → valid
		return true;
	}

	private static final int VILLAGER_MEETING_START = 9000;
	private static final int VILLAGER_MEETING_END = 11000;

	static boolean isVillagerMeetingTime(ServerLevel serverLevel) {
		long time = serverLevel.getDayTime() % 24000;

		if (time >= VILLAGER_MEETING_START && time <= VILLAGER_MEETING_END) {
			return true;
		}

		return false;
	}

	static boolean isValidRegenerationTarget(LivingEntity le, Villager ve) {
		if (le.getHealth() >= le.getMaxHealth())
			return false;
		if (le.hasEffect(MobEffects.REGENERATION))
			return false;
		if (le instanceof Player pe && ve.getPlayerReputation(pe) < 0)
			return false;
		return true;
	}

	// ---------------------
	// Check if block is too bright to place torch
	// ---------------------
	static boolean isDark(ServerLevel serverLevel, BlockPos pos) {
		int lightLevel = serverLevel.getBrightness(LightLayer.BLOCK, pos);
		// Higher level / coal villager gives higher threshold
		int threshold = MyConfig.getTorchLightLevel();
		if (lightLevel >= threshold)
			return true;
		return false;
	}

	static boolean isOkayToBuildWallHere(ActionContext rgCtx) {

		// allows replacing grass, flowers, etc. with a wall block.
		if (!(isFootBlockOkayToBuildIn(rgCtx))) {
			return false;
		}
		if (!(isFoundationValid(rgCtx))) {
			return false;
		}
		return true;
	}

	static boolean isVillageMeetingTime(ServerLevel serverLevel) {

		long daytime = serverLevel.getDayTime() % 24000;

		if ((daytime > 9000 && daytime < 11000)) {
			return true;
		}

		return false;

	}

	static boolean isNewChunk(ServerLevel serverLevel, BlockPos villageMeetingPointPos) {
		return serverLevel.getChunkAt(villageMeetingPointPos).getInhabitedTime() < 200;
	}

	static boolean isWallorLantern(ServerLevel serverLevel, BlockPos mPos, BlockState wallBs) {

		BlockState bs = null;
		BlockState ws = null;

		bs = serverLevel.getBlockState(mPos.below(1));
		if (bs.getBlock() instanceof LanternBlock) {
			return true;
		}

		ws = serverLevel.getBlockState(mPos.below(2));
		if (ws.getBlock() instanceof WallBlock) {
			return true;
		}

		if (serverLevel.getBlockState(mPos.below(1)).getBlock() == wallBs.getBlock()) {
			return true;
		}

		if (serverLevel.getBlockState(mPos.below(2)).getBlock() == wallBs.getBlock()) {
			return true;
		}

		return false;
	}

	/**
	 * Returns true if the given block is a valid ocean floor block
	 * suitable for placement checks (e.g., sand, gravel, clay, or magma).
	 *
	 * @param bs the block state to check
	 * @return true if the block is sand, gravel, clay, or magma; false otherwise
	 */
	static boolean isOceanFloorBlock(BlockState bs) {
	    return bs.is(BlockTags.SAND) || bs.is(Blocks.GRAVEL) || bs.is(Blocks.CLAY) || bs.is(Blocks.MAGMA_BLOCK);
	}

	 static boolean hasAdjacentDoor(ServerLevel serverLevel, BlockPos pos) {
		if (serverLevel.getBlockState(pos.offset(-1, 0, 0)).getBlock() instanceof DoorBlock)
			return true;
		if (serverLevel.getBlockState(pos.offset(1, 0, 0)).getBlock() instanceof DoorBlock)
			return true;
		if (serverLevel.getBlockState(pos.offset(0, 0, -1)).getBlock() instanceof DoorBlock)
			return true;
		if (serverLevel.getBlockState(pos.offset(0, 0, 1)).getBlock() instanceof DoorBlock)
			return true;

		return false;
	}

	public static boolean isCactusOrRemovableLeaves(BlockState bs) {

		Block block = bs.getBlock();

		if (block instanceof CactusBlock)
			return true;

		if ((block instanceof LeavesBlock) && bs.hasProperty(LeavesBlock.PERSISTENT)
				&& (!bs.getValue(LeavesBlock.PERSISTENT))) // maybe is(BlockTag.Leves) in future
			return true;

		return false;

	}

}
