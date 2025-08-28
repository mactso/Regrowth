package com.mactso.regrowth.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import com.mactso.regrowth.config.RegrowthEntitiesManager.RegrowthMobItem;
import com.mactso.regrowth.config.WallBiomeDataManager;
import com.mactso.regrowth.config.WallFoundationDataManager;
import com.mactso.regrowth.utility.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.ShortDryGrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TallDryGrassBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber()
public class MoveEntityEvent {

	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

// Start of Fabric/Forge Custom Section.    
	// it will become "ConventionalTags." in a later minecraft version.
	private static final TagKey<Block> MY_ALL_FLOWERS = BlockTags.FLOWERS;
	private static final TagKey<Block> MY_SMALL_FLOWERS = BlockTags.SMALL_FLOWERS;
// End of Fabric/Forge Custom Section;

	public static final BooleanProperty OPEN = FenceGateBlock.OPEN;
	static final int WALL_CENTER = 0;
	static final int FENCE_CENTER = 0;
	static final int WALL_TYPE_WALL = -1;
	static final int WALL_TYPE_FENCE = -2;

	static final int FAIL = -Integer.MIN_VALUE;
	static final int UPDATE = 3;
	
	static final String ACTION_GROW = "grow";
	static final String ACTION_EAT = "eat";
	static final String ACTION_BOTH = "both";
	static final String ACTION_TALL = "tall";
	static final String ACTION_MUSHROOM = "mushroom";
	static final String ACTION_STUMBLE = "stumble";
	static final String ACTION_REFOREST = "reforest";
	static final String ACTION_CORAL = "coral";
	private static Random moveRand = new Random();
	private static final Block[] coralPlants = { Blocks.TALL_SEAGRASS, Blocks.SEAGRASS, Blocks.SEA_PICKLE,
			Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN,
			Blocks.TUBE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN,
			Blocks.HORN_CORAL_FAN, Blocks.TUBE_CORAL_FAN };
	private static final Block[] coralfans = { Blocks.BRAIN_CORAL_WALL_FAN, Blocks.BUBBLE_CORAL_WALL_FAN,
			Blocks.FIRE_CORAL_WALL_FAN, Blocks.HORN_CORAL_WALL_FAN, Blocks.TUBE_CORAL_WALL_FAN };

	private final Rotation[] coralfanrotations = { Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90,
			Rotation.CLOCKWISE_90, Rotation.NONE };

	private static int[] dx = { 1, 0, -1, 0 };
	private static int[] dz = { 0, 1, 0, -1 };
	private static int CHECKS_PER_SECOND = 10;
	private static int[][] facingArray = { { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 }, { 0, -1 }, { 1, -1 }, { 1, 0 },
			{ 1, 1 } };
	private static int lastTorchX = 0;
	private static int lastTorchY = 0;
	private static int lastTorchZ = 0;

	private static final ResourceLocation BEEKEEPER = ResourceLocation.parse("bk:beekeeper");

	private static boolean doDebug = false;
	private static BlockState footBlockState;
	private static BlockState groundBlockState;
	private static Block footBlock;
	private static Block groundBlock;
	private static BlockPos footBlockPos;
	private static BlockPos groundBlockPos;

	private static Biome localBiome;
	private static boolean isRoadPiece = false;
	private static String biomeCategory;
	static BlockPos adjustedPos;

	@SubscribeEvent // is in Forge 
	public static void handleEntityMoveEvents(LivingTickEvent event) {
		Utility.debugMsg(1, "enter LivingEntity movement event handler");
		if (event.getEntity() instanceof LivingEntity le) {
			
		Level level = le.level();
		if (level.isClientSide()) {
			return; // MyConfig.CONTINUE_EVENT;
		}
		ServerLevel serverLevel = (ServerLevel) level;
		doMobsAndVillagers(le, serverLevel);
		}
		return; 
	}
	
	private static boolean  doMobsAndVillagers(LivingEntity le, ServerLevel serverLevel) {

		if (le instanceof Player)
			return MyConfig.CONTINUE_EVENT;
		if (le.blockPosition() == null)
			return MyConfig.CONTINUE_EVENT;
		
		if (MyConfig.getaDebugLevel() > 0)
			doDebug = true;

		if (doDebug)
			Utility.debugMsg(1, "enter doMobsAndVillagers");

		if (le.getId() % 2 == serverLevel.getGameTime() % 2)
			return MyConfig.CONTINUE_EVENT;

		String rlString = Utility.getResourceLocationString(le).toString();

		RegrowthMobItem currentRegrowthMobItem = RegrowthEntitiesManager.getRegrowthMobInfo(rlString);
		if (currentRegrowthMobItem == null)
			return MyConfig.CONTINUE_EVENT;

		adjustedPos = getAdjustedBlockPos(le);
		footBlockState = getAdjustedFootBlockState(le);
		footBlock = footBlockState.getBlock();
		footBlockPos = getAdjustedBlockPos(le);

		if (footBlock instanceof WoolCarpetBlock)
			return MyConfig.CONTINUE_EVENT;

		groundBlockState = getAdjustedGroundBlockState(le);
		groundBlock = groundBlockState.getBlock();
		groundBlockPos = getAdjustedBlockPos(le); // TODO is this used?

		if ((groundBlockState.isAir()) && !(le instanceof Bat))
			return MyConfig.CONTINUE_EVENT;

		biomeCategory = Utility.getMyBC(serverLevel.getBiome(le.blockPosition()));
		localBiome = serverLevel.getBiome(le.blockPosition()).value();

		String regrowthActions = currentRegrowthMobItem.getRegrowthActions();
		if (isImpossibleRegrowthEvent(regrowthActions))
			return MyConfig.CONTINUE_EVENT;

		double regrowthEventOdds = 1 / (currentRegrowthMobItem.getRegrowthEventSeconds() * CHECKS_PER_SECOND);
		if (isHorseTypeEatingNow(le)) {
			regrowthEventOdds *= 20;
		}
		double randomD100Roll = serverLevel.random.nextDouble();
		int debugvalue = 0; // TODO make sure value 0 after debugging.

		long chunkAge = serverLevel.getChunkAt(le.blockPosition()).getInhabitedTime();

		// improve village roads and walls faster for the first 200 minutes;
		if (chunkAge < 480000) {
			if (le instanceof Villager ve) {
				if (serverLevel.getGameTime() % 12 == 0) {
					if (regrowthActions.contains("r")) {
						vRoadImprove(ve, serverLevel, "preRoad");
					}
					if (regrowthActions.contains("w")) {
						vImproveVillageWall(ve, serverLevel, regrowthActions);
					}

				}
			}
		}

		if (randomD100Roll <= regrowthEventOdds + debugvalue) {
			if (le instanceof Villager ve) {
					doVillagerRegrowthEvents(ve, serverLevel, rlString, regrowthActions);
			} else {
				doMobRegrowthEvents(le, serverLevel, rlString, regrowthActions);
			}
		}
		if (doDebug)
			Utility.debugMsg(1, "fall out of Handle Entity tick Events");
		return MyConfig.CONTINUE_EVENT;
	}
	
	private static void doMobRegrowthEvents(LivingEntity le, ServerLevel serverLevel, String key, String regrowthType) {

		Utility.debugMsg(1, "enter Mob Events action:" + regrowthType);
		BlockPos lePos = le.blockPosition();
		
		if (regrowthType.equals(ACTION_STUMBLE)) {
			if (mobStumbleAction(le, serverLevel, key)) {
				if (doDebug)
				Utility.debugMsg(1, lePos, key + " stumbled over torch.");
			}
			return;
		}

		if (regrowthType.equals(ACTION_REFOREST)) {
			if (mobReforestAction(le, serverLevel, key)) {
				if (doDebug)
				Utility.debugMsg(1, lePos, key + " planted sapling.");
			}
			return;
		}

		if (regrowthType.equals(ACTION_MUSHROOM)) {
			if (mobGrowMushroomAction(le, serverLevel, key)) {
				if (doDebug)
				Utility.debugMsg(1, lePos, key + " grow giant mushroom.");
			}
			return;
		}

		if (regrowthType.equals(ACTION_CORAL)) {
			if (mobGrowCoralAction(le, serverLevel, key)) {
				if (doDebug)
				Utility.debugMsg(1, lePos, key + " grow coral from below.");
			}
			return;
		}

		if (regrowthType.equals(ACTION_TALL)) {
			mobGrowTallAction(le, serverLevel, key);
			return;
		}

		mobCheckOverCrowding(le, serverLevel, key);

		// all remaining actions currently require a grass block underfoot so if not a
		// grass block- can exit now.
		// this is for performance savings only.
		// looks like meadow_grass_block is not a grassBlock

		if (!isKindOfGrassBlock(groundBlockState)) {
			return;
		}

		if (regrowthType.equals(ACTION_BOTH)) {
			if (serverLevel.random.nextDouble() * 100 > 85.0) {
				regrowthType = ACTION_GROW;
			} else {
				regrowthType = ACTION_EAT;
			}
		}

		if (regrowthType.contentEquals(ACTION_EAT)) {
			mobEatPlantsAction(le, serverLevel, key, regrowthType);
			return;
		}

		if ((regrowthType.equals(ACTION_GROW))) {
			if (mobGrowPlantsAction(le, serverLevel, key)) {
				Utility.debugMsg(2, le.getType().toShortString() + " grew plants.");
			}
			return;
		}

	}

	private static void doVillagerRegrowthEvents(Villager ve, ServerLevel serverLevel, String debugKey,
			String regrowthActions) {
		boolean debugOn = false;
		
		vPowderedSnowImprove(ve, serverLevel);
		if (!(ve.onGround()))
			return;
		if (footBlock instanceof BedBlock) {
			return;
		}
		// Villagers hopping, falling, etc. are doing improvements.
		if (groundBlockState.getBlock() instanceof TorchBlock)
			return;
		if (groundBlockState.getBlock() instanceof WallTorchBlock)
			return;

		// Give custom debugging names to nameless villagers.
		// MyConfig.setDebugLevel(0); // TODO comment this out before release
		if (MyConfig.getDebugLevel() > 0) {
			debugOn = true;
			float veYaw = ve.getViewYRot(1.0f);
			MutableComponent tName = Component.literal("Reg-" + ve.getX() + "," + ve.getZ() + ": " + veYaw);
			ve.setCustomName(tName);
		} else { // remove custom debugging names added by Regrowth
			if (ve.getCustomName() != null) {
				if (ve.getCustomName().toString().contains("Reg-")) {
					ve.setCustomName(null);
				}
			}
		}

		if (debugOn)
			Utility.debugMsg(1, "start checking Villager Actions");

		// note all villagers may not have a home. poor homeless villagers.
		// default = repair farms
		if (vImproveFarm(ve, serverLevel, regrowthActions)) {
			if (debugOn)
			Utility.debugMsg(1, ve, debugKey + " farm improved.");
		}

		// 'h'eal villagers and players and repair Iron Golems.
		if (regrowthActions.contains("h")) {
			if (debugOn)
				Utility.debugMsg(1, footBlockPos, debugKey + "Cleric healing, Toolmaster Repair ");
			vClericalHealing(ve, serverLevel);
			vToolMasterHealing(ve, serverLevel);
		}

		vBeeKeeperFlowers(ve, serverLevel);

		// cut lea'v'es.
		// remove leaves if facing head height leaves

		if (regrowthActions.contains("v")) {
			if (debugOn)
				Utility.debugMsg(1, footBlockPos, debugKey + " Trim Lea v es.");
			vImproveLeaves(ve, serverLevel, debugKey);
		}

		// c = cut down grass (but not flowers for now)
		// to do - maybe remove flowers unless by a road or elevated (air next to them
		// as in the flower beds)
		// to do - replace "c" with a meaningful constant.

		if (regrowthActions.contains("c")) {
			if (debugOn)
				Utility.debugMsg(1, ve, debugKey + " try road improve.");
			vCutGrass(serverLevel);
		}
		// improve roads
		// to do - replace "r" with a meaningful constant.f
		if (regrowthActions.contains("r")) {
			if (debugOn)
				Utility.debugMsg(1, footBlockPos, debugKey + " try road improve.");
			vRoadImprove(ve, serverLevel, debugKey);
		}

		// note villages may not have a meeting place. Sometimes they change. Sometimes
		// they take a few minutes to form.
		if ((regrowthActions.contains("w"))) {
			if (debugOn)
				Utility.debugMsg(1, footBlockPos, " try town wall build.");
			vImproveWalls(ve, serverLevel, debugKey, regrowthActions);

		}

		if ((regrowthActions.contains("p"))) {
			if (debugOn)
				Utility.debugMsg(1, footBlockPos, " try personal fence build.");
			vImproveFences(ve, serverLevel, debugKey, regrowthActions);

		}

		if ((regrowthActions.contains("t") && (footBlock != Blocks.TORCH))) {
			if (vImproveLighting(ve, serverLevel)) {
				if (debugOn)
				Utility.debugMsg(1, ve, debugKey + "-" + footBlock + ", " + groundBlock + " pitch: " + ve.getXRot()
						+ " lighting improved.");
			}
		}
		if (debugOn)
			Utility.debugMsg(1, "finish checking Villager Actions");
	}

	private static boolean vCutGrass(ServerLevel serverLevel) {
		if (isCuttable(footBlock)) {
			serverLevel.destroyBlock(footBlockPos, false);
			Utility.debugMsg(1, footBlockPos, " grass cut.");
			return true;
		}
		return false;
	}

	private static boolean isCuttable(Block b) {

		return ((b instanceof TallGrassBlock) || (b instanceof DoublePlantBlock) || (b instanceof ShortDryGrassBlock)
				|| (b instanceof LeafLitterBlock)
				|| (b instanceof TallDryGrassBlock) || (b.getDescriptionId().equals("block.byg.short_grass")));
	}

	private static int getAbsVX(Entity e, BlockPos gVMPPos) {
		int absvx = (int) Math.abs(getVX(e, gVMPPos));
		return absvx;
	}

	private static int getAbsVX(BlockPos pos, BlockPos gVMPPos) {
		int absvx = (int) Math.abs(getVX(pos, gVMPPos));
		return absvx;
	}

	private static int getAbsVZ(Entity e, BlockPos gVMPPos) {
		return (int) Math.abs(getVZ(e, gVMPPos));
	}

	private static int getAbsVZ(BlockPos pos, BlockPos gVMPPos) {
		return (int) Math.abs(getVZ(pos, gVMPPos));
	}

	// adjust position for partial blocks like slabs, farmland, soul sand, and mud.
	private static BlockPos getAdjustedBlockPos(Entity e) {
		if (e.getY() == e.blockPosition().getY()) {
			return e.blockPosition();
		}
		return e.blockPosition().above();
	}

	private static BlockState getAdjustedFootBlockState(Entity e) {
		BlockPos pos = getAdjustedBlockPos(e);
		return e.level().getBlockState(pos);
	}

	private static BlockState getAdjustedGroundBlockState(Entity e) {
		return e.level().getBlockState(e.blockPosition().below(getAdjustedY(e)));
	}

	private static int getAdjustedY(Entity e) {
		if (e.getY() == e.blockPosition().getY())
			return 1;
		return 0;
	}

	private static int getVX(Entity e, BlockPos gVMPPos) {
		return (int) (e.getX() - gVMPPos.getX());
	}

	private static int getVX(BlockPos pos, BlockPos gVMPPos) {
		return (int) (pos.getX() - gVMPPos.getX());
	}

	private static int getVZ(Entity e, BlockPos gVMPPos) {
		return (int) (e.getZ() - gVMPPos.getZ());
	}

	private static int getVZ(BlockPos pos, BlockPos gVMPPos) {
		return (int) (pos.getZ() - gVMPPos.getZ());
	}

	private static BlockState helpBiomeRoadBlockType(String localBiome) {
		BlockState gateBlockType = Blocks.DIRT_PATH.defaultBlockState();

		if (biomeCategory.equals(Utility.DESERT)) {
			gateBlockType = Blocks.ACACIA_PLANKS.defaultBlockState(); // 16.1 mojang change
		}
		return gateBlockType;
	}

	private static void helpAgeChildEntity(LivingEntity le) {
		if (le instanceof AgeableMob) {
			AgeableMob ageableLe = (AgeableMob) le;
			if (ageableLe.isBaby()) {
				ageableLe.setAge(ageableLe.getAge() + 30);
			}
		}
	}

	public static int helpCountBlocksInBox(Block searchBlock, int maxCount, ServerLevel serverLevel, BlockPos bPos,
			int boxSize) {
		return helpCountBlocksInBox(searchBlock, maxCount, serverLevel, bPos, boxSize, boxSize); // "square" box subcase
	}

	public static int helpCountBlocksInBox(Block searchBlock, int maxCount, ServerLevel serverLevel, BlockPos bPos,
			int boxSize, int ySize) {

		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;

		MutableBlockPos mPos = new MutableBlockPos();
		for (int dy = minY; dy <= maxY; dy++) {
		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
					mPos.set(dx, dy, dz);
					if (serverLevel.hasChunkAt(mPos)) {
					if (serverLevel.getBlockState(mPos).getBlock() == searchBlock) {
						if (++count >= maxCount)
							return count;
					}
				}
			}
		}
		}

		if (doDebug)
			Utility.debugMsg(2, bPos,
				Utility.getResourceLocationString(searchBlock) + " Sparse count:" + count + " countBlockBB ");

		return count;
		
	}

	public static int helpCountBlocksInBox(Class<? extends Block> searchBlock, int maxCount, ServerLevel serverLevel,
			BlockPos bPos, int boxSize) {
		return helpCountBlocksInBox(searchBlock, maxCount, serverLevel, bPos, boxSize, 0);
	}

	// used when counting blocks extended from a shared class
	// SaplingBlock for OakSaplingBlock, SpruceSaplingBlock, etc.)
	
	public static int helpCountBlocksInBox(Class<? extends Block> searchBlock, int maxCount, Level serverLevel,
			BlockPos bPos, int boxSize, int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;

		MutableBlockPos mPos = new MutableBlockPos();

		for (int dy = minY; dy <= maxY; dy++) {
		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
					mPos.set(dx, dy, dz);
					if (serverLevel.hasChunkAt(mPos)) {
						if (searchBlock.isInstance(serverLevel.getBlockState(mPos).getBlock())) {
						if (++count >= maxCount) {
							return count;
						}
					}
				}
			}
		}
		}

		if (doDebug) {
			Utility.debugMsg(2, bPos, searchBlock.getSimpleName() + " Sparse count:" + count + " countBlockBB ");
		}

		return count;
	}

	// this routine returns a count of the searchBlock immediately orthogonal to
	// BlockPos, exiting if a max count is exceeded.
	public static int helpCountBlocksOrthogonalInBox(Block searchBlock, int maxCount, Level w, BlockPos bPos,
			int boundY) {
		return helpCountBlocksOrthogonalInBox(searchBlock, maxCount, w, bPos, 0 - boundY, 0 + boundY);
	}

	public static int helpCountBlocksOrthogonalInBox(Block searchBlock, int maxCount, Level w, BlockPos bPos,
			int lowerBoundY, int upperBoundY) {
		int count = 0;
	    BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

		for (int j = lowerBoundY; j <= upperBoundY; j++) {
	    	
	        int baseX = bPos.getX();
	        int baseY = bPos.getY() + j;
	        int baseZ = bPos.getZ();
	        
	        mPos.set(baseX + 1, baseY, baseZ); // east
	        if (w.getBlockState(mPos).getBlock() == searchBlock)
				count++;
	        
	        mPos.set(baseX - 1, baseY, baseZ); // west
	        if (w.getBlockState(mPos).getBlock() == searchBlock)
				count++;
	        
	        mPos.set(baseX, baseY, baseZ - 1); // north
	        if (w.getBlockState(mPos).getBlock() == searchBlock)
				count++;
	        
	        mPos.set(baseX, baseY, baseZ + 1); // south
	        if (w.getBlockState(mPos).getBlock() == searchBlock)
				count++;

			if (count >= maxCount)
				return count;
		}

		return count;
	}

	private static void helpJumpAway(Entity e) {
		// "jump" villagers away if they are inside a wall, fence, or dirtPath block.
		Block postActionFootBlock = getAdjustedFootBlockState(e).getBlock();
		if (postActionFootBlock == Blocks.DIRT_PATH) {
			e.setDeltaMovement(0, 0.43, 0);
			return;
		}
		if ((postActionFootBlock instanceof WallBlock) || (postActionFootBlock instanceof FenceBlock)) {

			float veYaw = e.getViewYRot(1.0f) / 45;
			int facingNdx = Math.round(veYaw);
			if (facingNdx < 0) {
				facingNdx = Math.abs(facingNdx);
			}
			facingNdx %= 8;
			double dx = (facingArray[facingNdx][0]) / 2.0;
			double dz = (facingArray[facingNdx][1]) / 2.0;
			e.setDeltaMovement(dx, 0.55, dz);
		}
	}

	private static long helpLongRandomSeed(BlockPos ePos) {
		return (long) Math.abs(ePos.getX() * 31) + Math.abs(ePos.getZ() * 11) + Math.abs(ePos.getY() * 7);
	}

	private static boolean helpPlaceOneWallPiece(LivingEntity le, ServerLevel serverLevel, int wallRadius,
			BlockState wallType, BlockPos gVMPPos) {

		// Build East and West Walls (and corners)
		BlockState gateBlockState = Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FACING, Direction.EAST)
				.setValue(OPEN, true);
		Direction d = null;
		if (getAbsVX(le, gVMPPos) == wallRadius) {
			if (getAbsVZ(le, gVMPPos) <= wallRadius) {
				d = (getVX(le, gVMPPos) > 0) ? Direction.EAST : Direction.WEST;
				return helpPlaceWallPiece(le, serverLevel, wallType, gateBlockState.setValue(FACING, d),
						getVZ(le, gVMPPos));
			}
		}
		// Build North and South Walls (and corners)
		if (getAbsVZ(le, gVMPPos) == wallRadius) {
			if (getAbsVX(le, gVMPPos) <= wallRadius) {
				d = (getVZ(le, gVMPPos) > 0) ? Direction.NORTH : Direction.SOUTH;
				return helpPlaceWallPiece(le, serverLevel, wallType, gateBlockState.setValue(FACING, d),
						getVX(le, gVMPPos));
			}
		}

		return false;
	}

	private static boolean helpPlaceWallPiece(LivingEntity le, ServerLevel serverLevel, BlockState wallType,
			BlockState gateBlockType, int va) {

		BlockPos vePos = getAdjustedBlockPos(le);

		if (footBlock instanceof SnowLayerBlock) {
			serverLevel.destroyBlock(vePos, false);
		}

		if (isNatProgPebbleOrStick()) {
			serverLevel.destroyBlock(vePos, true);
		}

		if ((footBlock instanceof SaplingBlock) || (footBlock instanceof TallGrassBlock)
				|| (footBlock instanceof FlowerBlock) || (footBlock instanceof DoublePlantBlock)) {
			serverLevel.destroyBlock(vePos, true);
		}

		int absva = Math.abs(va);
		if (absva == WALL_CENTER) {
			serverLevel.setBlockAndUpdate(vePos, gateBlockType);
			return true;
		}

		if (serverLevel.setBlockAndUpdate(vePos, wallType)) {
			return true;
		} else {
			if (doDebug)
				Utility.debugMsg(1, le,
						"Building Wall Fail: SetBlockAndUpdate Time End = " + serverLevel.getGameTime());
			return false;
		}

	}

	private static BlockState helpGetSaplingState(Level serverLevel, BlockPos pos, Biome localBiome,
			BlockState sapling) {

		// TODO use new BlockTag.SAPLING, get list of valid trees, return default state
		// of one of those trees.
		// the BlockTag.SAPLING doesn't work that way. It says if something *is* a
		// sapling.
		// TagKey<Block> vlr = BlockTags.SAPLINGS;
		// would need a new manager for saplings by biome.

		sapling = Blocks.OAK_SAPLING.defaultBlockState();

		ResourceKey<Biome> k = serverLevel.getBiomeManager().getBiome(pos).unwrapKey().get();
		String biomeName = k.location().getPath();

		if (biomeName.contains("birch")) {
			sapling = Blocks.BIRCH_SAPLING.defaultBlockState();
		}
		if (biomeName.contains("taiga")) {
			sapling = Blocks.SPRUCE_SAPLING.defaultBlockState();
		}
		if (biomeName.contains("jungle")) {
			sapling = Blocks.JUNGLE_SAPLING.defaultBlockState();
		}
		if (biomeName.contains("savanna")) {
			sapling = Blocks.ACACIA_SAPLING.defaultBlockState();
		}
		if (biomeName.contains("desert")) {
			sapling = Blocks.ACACIA_SAPLING.defaultBlockState();
		}
//		System.out.print("local Sapling Type :" + sapling);
		return sapling;
	}

	private static void vPowderedSnowImprove(LivingEntity le, ServerLevel serverLevel) {

		if (le.isInPowderSnow) {
			int hp = 0;
			if (serverLevel.getBlockState(le.blockPosition().above(2)).getBlock() == Blocks.POWDER_SNOW) {
				serverLevel.destroyBlock(le.blockPosition().above(2), false);
				hp = 2;
			}
			if (serverLevel.getBlockState(le.blockPosition().above()).getBlock() == Blocks.POWDER_SNOW) {
				serverLevel.destroyBlock(le.blockPosition().above(), false);
				hp += 2;
			}
			if (serverLevel.getBlockState(le.blockPosition()).getBlock() == Blocks.POWDER_SNOW) {
				BlockState SnowLayer = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2 + hp);
				serverLevel.setBlockAndUpdate(le.blockPosition(), SnowLayer);
			}
		}

	}

	private static boolean isBlockGrassOrDirt(BlockState tempBlockState) {

		if (isKindOfGrassBlock(tempBlockState) || (tempBlockState.getBlock() == Blocks.DIRT)) {
			return true;
		}
		return false;
	}

	private static boolean isBlockDirtPathOrDirt(Block tempBlock) {

		if ((tempBlock == Blocks.DIRT_PATH) || (tempBlock == Blocks.DIRT)) {
			return true;
		}
		return false;
	}

	private static boolean isFootBlockOkayToBuildIn(ServerLevel serverLevel) {
		if ((footBlockState.isAir()) || (isGrassOrFlower(footBlockState))) {
			return true;
		}
		if (footBlockState.getBlock() instanceof SnowLayerBlock) {
			return true;
		}
		if (isNatProgPebbleOrStick())
			return true;

		return false;
	}

	private static boolean isGoodMushroomTemperature(Entity entity) {
		BlockPos ePos = getAdjustedBlockPos(entity);
		float biomeTemp = entity.level().getBiome(ePos).value().getBaseTemperature();
		if (doDebug)
			Utility.debugMsg(1, ePos, "Mushroom Biome temp: " + biomeTemp + ".");
		if (biomeTemp < MyConfig.getMushroomMinTemp())
			return false;
		if (biomeTemp > MyConfig.getMushroomMaxTemp())
			return false;
		return true;
	}

	private static boolean isGrassOrFlower(BlockState footBlockState) {
		Block footBlock = footBlockState.getBlock();

		if (footBlock instanceof TallGrassBlock) 
			return true;

		// DoublePlantBlock includes tall grass and ferns now in later releases	
		if (footBlock instanceof DoublePlantBlock) 
			return true;

		if (footBlock instanceof ShortDryGrassBlock)
			return true;
		
		if (footBlock instanceof TallDryGrassBlock)

		if (footBlock instanceof FlowerBlock) 
			return true;

		if (footBlock instanceof TallFlowerBlock) 
			return true;

		if (footBlock == Blocks.FERN) 
			return true;

		if (footBlock == Blocks.LARGE_FERN) 
			return true;

		// "biomes you'll go" grass compatibility
		if (footBlock.getDescriptionId().equals("block.byg.short_grass")) {
			return true;
		}

		// check for vanilla and most modded flowers
		try {
			if (footBlockState.is(MY_ALL_FLOWERS)) {
				return true;
			}
		} catch (Exception e) {
			if (doDebug)
				Utility.debugMsg(0, "ERROR: Tag Exception 1009-1014:" + footBlock.getDescriptionId() + ".");
		}

		if (doDebug)
			Utility.debugMsg(2, "Not grass or Flower:" + footBlock.getDescriptionId() + ".");

		return false;
	}

	private static boolean isHorseTypeEatingNow(Entity entity) {
		if (entity instanceof AbstractHorse) {
			AbstractHorse h = (AbstractHorse) entity;
			if (h.isEating()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isImpossibleRegrowthEvent(String regrowthType) {
		if ((regrowthType.equals(ACTION_EAT)) && (footBlockState.isAir())) {
			return true;
		}
		if ((regrowthType.equals(ACTION_GROW)) && (footBlockState.getBlock() instanceof TallGrassBlock)) {
			return true;
		}
		if ((regrowthType.equals(ACTION_GROW)) && (footBlockState.getBlock() instanceof FlowerBlock)) {
			return true;
		}
		if ((regrowthType.equals(ACTION_TALL)) && (!(footBlockState.getBlock() instanceof TallGrassBlock))) {
			return true;
		}
		return false;
	}

	private static boolean isKindOfGrassBlock(BlockState groundBlockState) {
		if (groundBlockState.getBlock() instanceof GrassBlock)
			return true;
		if (groundBlockState.getBlock().getDescriptionId().equals("block.byg.meadow_grass_block"))
			return true;
		return false;
	}

	// handle "Natural Progression" pebbles and sticks
	private static boolean isNatProgPebbleOrStick() {
		
		String rl = Utility.getResourceLocationString(footBlock);
		if ((rl.contains("natprog")) && (rl.contains("pebble")))
			return true;

		if ((rl.contains("natprog")) && (rl.contains("twigs")))
			return true;

		if ((rl.contains("minecraft")) && (rl.contains("button"))) {
			return true;
		}

		return false;

	}

	private static boolean isNearbyPoi(Villager ve, ServerLevel serverLevel, Biome localBiome, BlockPos vePos,
			int poiDistance) {

		// 08/30/20 Collection pre 16.2 bug returns non empty collections.
		// the collection is not empty when it should be.
		// are returned in the collection so have to loop thru it manually.

		Collection<PoiRecord> result = serverLevel.getPoiManager()
				.getInSquare(t -> true, ve.blockPosition(), poiDistance, Occupancy.ANY)
				.collect(Collectors.toCollection(ArrayList::new));

		if (!result.isEmpty()) {
			Iterator<PoiRecord> i = result.iterator();

		    MutableBlockPos mutablePos = new MutableBlockPos();
		    int veX = ve.blockPosition().getX();
		    int veZ = ve.blockPosition().getZ();

		    while (i.hasNext()) { // in 16.1 + , finds the point of interest.
		        PoiRecord poi = i.next();
		        mutablePos.set(poi.getPos());

		        int dX = Math.abs(veX - mutablePos.getX());
		        int dZ = Math.abs(veZ - mutablePos.getZ());

		        if (dX < poiDistance && dZ < poiDistance) {
		            if (doDebug) {
		                Utility.debugMsg(1, ve, "Point of Interest too Close: " + poi.getPoiType().toString() + ".");
		            }
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isOkayToBuildWallHere(Villager ve, ServerLevel serverLevel) {

		boolean okayToBuildWalls = true;

		if (!(isOnGround(ve))) {
			okayToBuildWalls = false;
		}
		if (!(isFootBlockOkayToBuildIn(serverLevel))) {
			okayToBuildWalls = false;
		}
		if (!(isValidGroundBlockToBuildWallOn(ve.blockPosition(), serverLevel))) {
			okayToBuildWalls = false;
		}
		return okayToBuildWalls;
	}

	private static boolean isOnGround(Entity e) {
		return e.onGround();
	}

	private static boolean isOnWallRadius(Entity e, int wallRadius, BlockPos gVMPPos) {
		if ((getAbsVX(e, gVMPPos) == wallRadius) && (getAbsVZ(e, gVMPPos) <= wallRadius))
			return true;
		if ((getAbsVZ(e, gVMPPos) == wallRadius) && (getAbsVX(e, gVMPPos) <= wallRadius))
			return true;
		return false;
	}

	private static boolean isOutsideMeetingPlaceWall(Villager ve, Optional<GlobalPos> vMeetingPlace,
			BlockPos meetingPlacePos, Biome localBiome) {

		BlockPos vePos = getAdjustedBlockPos(ve);
		String key = "minecraft:" + localBiome.toString(); // TODO probably broken.
//		ResourceLocation biomeName = ForgeRegistries.BIOMES.getKey(localBiome);

		int wallDiameter = 64;
		key = key.toLowerCase();
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		if (!(currentWallBiomeDataItem == null)) {
			wallDiameter = currentWallBiomeDataItem.getWallDiameter();
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

	private static boolean isValidGroundBlockToBuildWallOn(BlockPos lePos, ServerLevel serverLevel) {

		if (serverLevel.getBrightness(LightLayer.SKY, lePos) < 13)
			return false;

		if (groundBlock instanceof SnowLayerBlock)
			return false;
		if (groundBlock instanceof TorchBlock)
			return false; // includes WallTorchBlock

		if (serverLevel.getBlockState(lePos.above()).getBlock() instanceof WallBlock) {
			return false;
		}
		if (serverLevel.getBlockState(lePos.below()).getBlock() instanceof WallBlock) {
			return false;
		}
		if (serverLevel.getBlockState(lePos.below(1)).getBlock() instanceof WallBlock) {
			return false;
		}
		if (serverLevel.getBlockState(lePos.below(2)).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (serverLevel.getBlockState(lePos.above()).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (serverLevel.getBlockState(lePos.below()).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (serverLevel.getBlockState(lePos.below(1)).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (serverLevel.getBlockState(lePos.below(2)).getBlock() instanceof TorchBlock) {
			return false;
		}

		groundBlock = groundBlockState.getBlock();
		if (doDebug)
			Utility.debugMsg(1, lePos, "Build Wall : gb" + groundBlock.toString() + ", fb:" + footBlock.toString());
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(Utility.getResourceLocationString(groundBlock));

		if (currentWallFoundationItem == null)
			return false;

		return true;

	}

//	private static boolean isValidGroundBlockToPlaceTorchOn(Villager ve) {
//
//		String key = Utility.getResourceLocationString(groundBlock);
//		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager
//				.getWallFoundationInfo(key);
//		if (currentWallFoundationItem == null)
//			return false;
//
//		return true;
//
//	}

	private static boolean isValidTorchLocation(int wallRadius, int wallTorchSpacing, int absvx, int absvz,
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

	private static boolean mobEatGrassOrFlower(LivingEntity le, ServerLevel serverLevel, String regrowthType) {
		BlockPos ePos = getAdjustedBlockPos(le);
		if (!(isGrassOrFlower(footBlockState))) {
			return false;
		}
		if (isKindOfGrassBlock(groundBlockState)) {
			mobTrodGrassBlock(le, serverLevel);
		}
		serverLevel.destroyBlock(ePos, false);
		double roll = le.getRandom().nextDouble();
		if (roll >= MyConfig.getEatingHealsOdds())
			return false;

		helpAgeChildEntity(le);   // TODO refactor to AgeChildEntity

		if (le.getMaxHealth() > le.getHealth()) {
			MobEffectInstance ei = new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 0, false, true);
			le.addEffect(ei);
		}
		return true;
	}

	private static boolean mobEatPlantsAction(LivingEntity entity, ServerLevel serverLevel, String key,
			String regrowthType) {
		if (mobEatGrassOrFlower(entity, serverLevel, regrowthType)) {
			if (doDebug)
				Utility.debugMsg(2, getAdjustedBlockPos(entity), key + " ate plants.");
			return true;
		}
		return false;
	}

	private static int coralBelowFindY(BlockPos pos, ServerLevel serverLevel) {
	    
	    BlockPos base = pos;
	    BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

	    for (int ey = 1; ey <= 3; ey++) {
	            mPos.set(base.getX(), base.getY() - ey, base.getZ());
	            if (serverLevel.getBlockState(mPos).getBlock() instanceof CoralBlock) {
	                return mPos.getY();
	            }
	    }
	    return FAIL;
	}
	
	private static int coralCount(BlockPos base, ServerLevel serverLevel) {
		int c = 0;
	    BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
	    for (int ew = -1; ew <= 1; ew++) {
	        for (int ns = -1; ns <= 1; ns++) {
	            mPos.set(base.getX() + ew, base.getY(), base.getZ() + ns);
	            if (serverLevel.getBlockState(mPos).getBlock() instanceof CoralBlock) {
	                c++;
	            }
	        }
	    }
	    return c;
	}

	private static boolean coralBlockGrowNew(BlockPos pos, int coralFoundY, ServerLevel serverLevel) {

		RandomSource rand = serverLevel.getRandom();

		BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
		mPos.set(pos.getX(), coralFoundY + 1, pos.getZ());

		if (coralCount(mPos, serverLevel) > 5) {
        	return false;
        }

        int dxt = rand.nextInt(2) - 1;
        int dzt = rand.nextInt(2) - 1;

		mPos.set(pos.getX() + dxt, coralFoundY + 1, pos.getZ() + dzt);
		if (serverLevel.getBlockState(mPos).getBlock() instanceof CoralBlock) {
			return false;
		}

		BlockPos cPos = new BlockPos(pos.getX(), coralFoundY, pos.getZ());
		BlockState theCoralBlockState = serverLevel.getBlockState(cPos);
		if (doDebug)
			Utility.debugMsg(2, pos, "Grow Coral at " + mPos);
		serverLevel.setBlockAndUpdate(cPos, theCoralBlockState);
		serverLevel.playSound(null, pos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.AMBIENT, 0.9f, 1.4f);

		coralFanTry(serverLevel, rand, mPos);

		return true;
	}
	
	private static void coralFanTry(ServerLevel serverLevel, RandomSource rand, BlockPos.MutableBlockPos mPos) {
		
		if (rand.nextInt(100) > 30) {
			return;
				}

		int d = rand.nextInt(4);
		BlockPos fPos = new BlockPos(mPos.getX() + dx[d], mPos.getY(), mPos.getZ() + dz[d]);
		if (serverLevel.getBlockState(fPos).getBlock() != Blocks.WATER) {
			return;
			}

		BlockState theCoralFanBlockState = coralfans[rand.nextInt(coralfans.length)].defaultBlockState();
		;
		serverLevel.setBlockAndUpdate(fPos, theCoralFanBlockState);
		serverLevel.playSound(null, fPos, SoundEvents.BOAT_PADDLE_WATER, SoundSource.AMBIENT, 0.9f, 1.4f);
		
		return;

			}

	private static boolean mobGrowCoralAction(LivingEntity le, ServerLevel serverLevel, String key) {

		BlockPos lePos = le.blockPosition();

		int coralFoundY = coralBelowFindY(lePos, serverLevel);
		if (coralFoundY == FAIL) {
			return false;
		}
		if (coralBlockGrowNew(lePos, coralFoundY, serverLevel)) {
			return true;
		}

		return true;
	}

	private static boolean isMushroomValid(LivingEntity le, ServerLevel serverLevel,  BlockPos lePos) {
	    if (serverLevel.getBlockState(lePos).getBlock() instanceof MushroomBlock) {
			return false;
		}

	    if (serverLevel.canSeeSky(lePos)) {
			return false;
		}
	    
	    if (!isGoodMushroomTemperature(le)) {
			return false;
		}

	    Random mushRand = new Random(helpLongRandomSeed(lePos));
	    double mushFertility = mushRand.nextDouble();
		mushFertility = mushRand.nextDouble();
		if (mushFertility < .75) {
			return false;
		}

		int smallMushroomCount = helpCountBlocksInBox(MushroomBlock.class, 4, serverLevel, lePos, 4, 1);

		if (smallMushroomCount > 3)
			return false;

	    return true;
	}
	
	private static boolean mushroomDecorateGiant(ServerLevel serverLevel, BlockPos lePos) {

		if (groundBlock == Blocks.RED_MUSHROOM_BLOCK) {
			serverLevel.setBlockAndUpdate(lePos, Blocks.RED_MUSHROOM.defaultBlockState());
			return true;
		}

		if (groundBlock == Blocks.BROWN_MUSHROOM_BLOCK) {
			serverLevel.setBlockAndUpdate(lePos, Blocks.BROWN_MUSHROOM.defaultBlockState());
			return true;
		}
		
		return false;
		
	}
	
	private static boolean mobGrowMushroomAction(LivingEntity le, ServerLevel serverLevel, String key) {
		BlockPos lePos = le.blockPosition();
		
		if (!isMushroomValid(le, serverLevel, lePos)) {
			if (doDebug)
				Utility.debugMsg(2, lePos, key + " Mushroom invalid at this location. ");
			return false;
		}

		if (mushroomDecorateGiant(serverLevel, lePos)) {
			return true;
		}

		// HugeMushroomBlock in later versions.
		int hugeMushroomCount = helpCountBlocksInBox(MushroomBlock.class, 1, serverLevel, lePos, 1, 1);
		if (hugeMushroomCount > 0) {
			// if right next to a huge mushroom let it grow if it got past above density
			// check.
		} else {
			int huge = helpCountBlocksInBox(HugeMushroomBlock.class, 1, serverLevel, lePos,
					MyConfig.getMushroomDensity(), 1);
			if (huge > 0) {
				if (doDebug)
					Utility.debugMsg(1, le, key + " huge (" + huge + ") mushroom too crowded.");
				return false;
			}
		}

		boolean growMushroom = false;
		if (BlockTags.BASE_STONE_OVERWORLD == null) {
			if (doDebug)
				Utility.debugMsg(1, "ERROR BlockTags.BASE_STONE_OVERWORLD missing.");
			if (groundBlock == Blocks.STONE || groundBlock == Blocks.DIORITE || groundBlock == Blocks.ANDESITE
					|| groundBlock == Blocks.GRANITE || groundBlock == Blocks.DIRT) {
				growMushroom = true;
			}
		} else {
			if (!groundBlockState.is(BlockTags.BASE_STONE_OVERWORLD) && !groundBlockState.is(BlockTags.DIRT)) {
				return false;
			}
			growMushroom = true;
		}

		if (serverLevel.hasNearbyAlivePlayer((double) lePos.getX(), (double) lePos.getY(), (double) lePos.getZ(),
				12.0)) {
			growMushroom = false;
		}

		if (growMushroom) {

			double vx = le.position().x() - (lePos.getX() + 0.5d);
			double vz = le.position().z() - (lePos.getZ() + 0.5d);

			Vec3 vM = new Vec3(vx, 0.0d, vz).normalize().scale(1.0d).add(0, 0.5, 0);
			le.setDeltaMovement(le.getDeltaMovement().add(vM));

			Block theBlock = null;
			Block theCapBlock = null;

			if (serverLevel.random.nextDouble() * 100.0 > 75.0) {
				theBlock = Blocks.RED_MUSHROOM;
				theCapBlock = Blocks.RED_MUSHROOM_BLOCK;
			} else {
				theBlock = Blocks.BROWN_MUSHROOM;
				theCapBlock = Blocks.BROWN_MUSHROOM_BLOCK;
			}
			serverLevel.setBlockAndUpdate(lePos, theBlock.defaultBlockState());
			MushroomBlock mb = (MushroomBlock) theBlock;
			BlockState saveGroundBlockState = serverLevel.getBlockState(lePos.below());

			if (serverLevel.getRandom().nextDouble() < 0.03) {
				mobGrowSmallMushroom(serverLevel, lePos, theCapBlock);
				if (doDebug)
					Utility.debugMsg(1, le, key + " grow 4 block high mushroom.");
				return true;
			} else {
			try {
					serverLevel.setBlockAndUpdate(lePos.below(),Blocks.DIRT.defaultBlockState());
					mb.growMushroom(serverLevel, lePos, theBlock.defaultBlockState(), serverLevel.random);
					if (serverLevel.getBlockState(lePos).getBlock() == theBlock) {
						mb.growMushroom(serverLevel, lePos, theBlock.defaultBlockState(), serverLevel.random);
					}
					if (serverLevel.getBlockState(lePos).getBlock() == theBlock) {
						serverLevel.setBlockAndUpdate(lePos, Blocks.AIR.defaultBlockState());
						serverLevel.setBlockAndUpdate(lePos.below(),saveGroundBlockState);
					} else {
						Utility.debugMsg(1, le, key + " grew a huge mushroom."); // TODO remove after testing.
					}
			} catch (Exception e) {
				// technically an "impossible" error but it's happened so this should
				// bulletproof it.
			}


				// light the top stem inside the cap with shroomlight.
			if (theBlock == Blocks.RED_MUSHROOM) {
				for (int y = 9; y > 3; y--) {
						Block b = serverLevel.getBlockState(lePos.above(y)).getBlock();
					if (b == Blocks.MUSHROOM_STEM) {
							serverLevel.setBlockAndUpdate(lePos.above(y), Blocks.SHROOMLIGHT.defaultBlockState());
						break;
					}
				}
			}
				if (doDebug)
					Utility.debugMsg(2, le, key + " grow huge mushroom.");
				return true;
		}
		}

		return false;

	}

	private static boolean mobGrowPlantsAction(LivingEntity le, ServerLevel serverLevel, String key) {

		if (footBlockState.isAir()) {
			if (!(groundBlock instanceof BonemealableBlock)) {
				return false;
			}
			BlockPos bpos = le.blockPosition();
			if (bpos == null) {
				if (doDebug)
					Utility.debugMsg(1, "ERROR:" + key + "grow plant null position.");
				return false;
			}
			BonemealableBlock ib = (BonemealableBlock) groundBlock;
			try {
				BlockState bs = serverLevel.getBlockState(bpos);
				ib.performBonemeal(serverLevel, serverLevel.getRandom(), bpos, bs);
			} catch (Exception e) {
				if (doDebug)
					Utility.debugMsg(1, le, key + " caught grow attempt exception.");
			}
		}
		return true;
	}

	private static void mobGrowSmallMushroom(ServerLevel serverLevel, BlockPos ePos, Block theCapBlock) {
		// TODO: Set this back to 3 afterwards.
		if (serverLevel.getRandom().nextInt(6) < 3) {
			serverLevel.setBlockAndUpdate(ePos, Blocks.MUSHROOM_STEM.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(1), Blocks.MUSHROOM_STEM.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2), Blocks.MUSHROOM_STEM.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).east(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).west(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).north(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).south(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(3), theCapBlock.defaultBlockState());
		} else {
			serverLevel.setBlockAndUpdate(ePos, Blocks.MUSHROOM_STEM.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(1), Blocks.MUSHROOM_STEM.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2), Blocks.MUSHROOM_STEM.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).east(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).west(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).north(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).south(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).north().east(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).north().west(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).south().east(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(2).south().west(), theCapBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(ePos.above(3), theCapBlock.defaultBlockState());

		}

	}

	private static boolean mobGrowTallAction(LivingEntity le, ServerLevel serverLevel, String key) {
		if ((footBlock instanceof TallGrassBlock) && (footBlock instanceof BonemealableBlock)) {
			BlockPos ePos = getAdjustedBlockPos(le);
			if (!Utility.getResourceLocationString(footBlock).contains("byg")) { // byg grass crashes when bonemealed.
				try {
					BonemealableBlock ib = (BonemealableBlock) footBlock;
					ib.performBonemeal(serverLevel, serverLevel.random, ePos, serverLevel.getBlockState(ePos));
					if (doDebug)
						Utility.debugMsg(2, ePos, key + " grew and hid in tall plant.");
					return false;

				} catch (Exception e) {
					if (doDebug)
						Utility.debugMsg(1, ePos, key + " caught grow attempt exception.");
					return false;
				}
			}
		}
		return false;
	}

	private static void mobCheckOverCrowding(LivingEntity le, ServerLevel serverLevel, String key) {

		BlockPos pos = BlockPos.containing(le.getX(), le.getY(), le.getZ());

		if (le instanceof Animal a) {

				AABB box = AABB.encapsulatingFullBlocks(pos.east(3).above(2).north(3), pos.west(3).below(2).south(3));
			int excess = serverLevel.getEntities(le.getType(), box, (entity) -> true).size() - 16;

					if (excess > 16) {
				serverLevel.playLocalSound(le.getX(), le.getY(), le.getZ(), SoundEvents.COW_DEATH, SoundSource.NEUTRAL,
						1.1f, 0.54f, true);
						le.setPos(le.getX(), -66, le.getZ());
					} else {
					float hurt = excess + (serverLevel.getRandom().nextFloat() / 6);
					le.hurtServer(serverLevel, serverLevel.damageSources().inWall(), hurt);
				}

			}
		}

	private static boolean mobReforestAction(LivingEntity le, ServerLevel serverLevel, String key) {

		if (footBlock != Blocks.AIR)
			return false;

		if (!(isBlockGrassOrDirt(groundBlockState)))
			return false;

		BlockPos ePos = getAdjustedBlockPos(le);
		// only try to plant saplings in about 1/4th of blocks.
		double sinY = Math.sin((double) ((ePos.getY() + 64) % 256) / 256);

		if (serverLevel.random.nextDouble() > Math.abs(sinY))
			return false;

		BlockState sapling = null;
		// are we in a biome that has saplings in a spot a sapling can be planted?
		sapling = helpGetSaplingState(serverLevel, ePos, localBiome, sapling);

		// check if there is room for a new tree. Original trees.
		// don't plant a sapling near another sapling
		// TEST: The SaplingBlock
		int hval = 5;
		int yval = 0;
		int yrange = 0;

		if (helpCountBlocksInBox(SaplingBlock.class, 1, serverLevel, ePos, hval, yrange) > 0)
			return false;

		// check if there is room for a new tree.

		int leafCount = 0;
		yval = 4;
		yrange = 0;
		hval = 4;

		if (sapling == Blocks.ACACIA_SAPLING.defaultBlockState()) {
			yval = 5;
			hval = 7;
		}
		// TEST: The LeavesBlock
		leafCount = helpCountBlocksInBox(LeavesBlock.class, 1, serverLevel, ePos.above(yval), hval, yrange);
		if (leafCount > 0)
			return false;

		serverLevel.setBlockAndUpdate(ePos, sapling);
		
		return true;
	}

	private static boolean mobStumbleAction(Entity entity, ServerLevel serverLevel, String key) {
		if ((footBlock instanceof TorchBlock) || (footBlock instanceof WallTorchBlock)) {
			serverLevel.destroyBlock(getAdjustedBlockPos(entity), true);
			return true;
		}
		return false;
	}

	private static void mobTrodGrassBlock(LivingEntity le, ServerLevel serverLevel) {

		BlockPos pos = BlockPos.containing(le.getX(), le.getY(), le.getZ());

			AABB box = AABB.encapsulatingFullBlocks(pos.east(2).above(2).north(2), pos.west(2).below(2).south(2));
// TODO old way
		List<?> entityList = serverLevel.getEntities(le.getType(), box, (entity) -> true);

// TODO new way start when they make serverLevel.getEntities().get visible in a later version.
//		List<Entity> l = new ArrayList<>();
//		EntityType<?> test = e.getType();
//		serverLevel.getEntities().get(box, (entity) -> {
//			if (test.tryCast(entity) != null) {
//				l.add(entity);
//			}
//		});			
//
//		if (l.size() >= 9) {			
// TODO new way end

		if (entityList.size() >= 9) {
			serverLevel.setBlockAndUpdate(pos.below(), Blocks.DIRT_PATH.defaultBlockState());
			le.hurtServer(serverLevel, serverLevel.damageSources().inWall(), 0.25F);
			}
		}

	private static void vBeeKeeperFlowers(Villager ve, ServerLevel serverLevel) {

		Optional<ResourceKey<VillagerProfession>> optRk = ve.getVillagerData().profession().unwrapKey();
		if (optRk.isEmpty()) {
			return;
		}
	    String job = optRk.get().location().getPath().toString();
		if (job != "beekeeper") {
			return;
		}

		if (doDebug)
			Utility.debugMsg(1, ve.blockPosition(), "Beekeeper checking on flowers here.");
		if ((ve.getX() % 6 == 0) && (ve.getZ() % 7 == 0)) {
			if (isBlockGrassOrDirt(groundBlockState)) {
				if (helpCountBlocksOrthogonalInBox(Blocks.DIRT_PATH, 1, serverLevel, ve.blockPosition().below(), 0) == 1) {
					BlockState flowerBlockState = Blocks.AZURE_BLUET.defaultBlockState();
					serverLevel.setBlockAndUpdate(adjustedPos, flowerBlockState);
				}
			}
		}
	}

	private static boolean isVillagerProfession(Villager ve, ResourceKey<VillagerProfession> vc) {
		ResourceKey<VillagerProfession> vp = ve.getVillagerData().profession().unwrapKey().get();

		if (vp == vc) {
			return true;
		}
		return false;
	}

	private static boolean isClericalHealingValid(Villager ve, ServerLevel serverLevel) {

		if (!isVillagerProfession(ve, VillagerProfession.CLERIC)) {
			return false;
		}

		long daytime = serverLevel.getDayTime() % 24000;
		if (daytime < 9000 || daytime > 11000) {
			return false;
		}

		return true;

	}

	private static List<Entity> getEntitiesNearCleric(ServerLevel serverLevel, BlockPos pos) {
			AABB box = AABB.encapsulatingFullBlocks(pos.east(4).above(2).north(4), pos.west(4).below(2).south(4));
		List<Entity> entityList = serverLevel.getEntities((Entity) null, box, (entity) -> {
				if (entity instanceof Villager || entity instanceof Player) {
					return true;
				}
				return false;
			});
		return entityList;
	}

	private static boolean vClericalHealing(Villager ve, ServerLevel serverLevel) {

		if (!isClericalHealingValid(ve, serverLevel)) {
			return false;
				}

		int clericalLevel = ve.getVillagerData().level();

		BlockPos pos = BlockPos.containing(ve.getX(), (ve.getY() + 0.99d), (ve.getZ()));
		List<Entity> listVillagersPlayers = getEntitiesNearCleric(serverLevel, pos);

		for (Entity e : listVillagersPlayers) {
			if (!(e instanceof LivingEntity le)) 
				continue; // should be impossible
			
			if (le.getHealth() >= le.getMaxHealth())
				continue; // already at max HP

			if (le.hasEffect(MobEffects.REGENERATION))
				continue; // already has regen

			if (le instanceof Player pe && ve.getPlayerReputation(pe) < 0)
				continue; // been a bad bad player

					le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, clericalLevel * 51, 0), ve);
					serverLevel.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.2f, 1.52f);
			return true;
				}

		return false;
			}

	private static boolean vImproveFarm(Villager ve, ServerLevel serverLevel, String regrowthType) {

		if (!isVillagerProfession(ve, VillagerProfession.FARMER)) {
			return false;
		}
		if (vTryTillFarmland(ve, serverLevel)) {
					return true;
				}

				return false;
			}

	private static boolean vTryTillFarmland(Villager ve, ServerLevel serverLevel) {
		BlockPos pos = ve.blockPosition().below();
		BlockState theBlockState = serverLevel.getBlockState(pos);
		if (!(isTillable(pos, theBlockState)))
			return false;
		if (!(isAdjacentWetFarmland(serverLevel, pos)))
			return false;
		
		serverLevel.setBlockAndUpdate(pos, Blocks.FARMLAND.defaultBlockState());
		serverLevel.playSound(/* Player */ null, /* Pos */ pos.getX(), pos.getY(), pos.getZ(),
				/* SoundEvent */ SoundEvents.GRASS_PLACE, /* SoundSource */ SoundSource.BLOCKS, /* Volume */ 0.75f,
				/* Pitch */ 0.75f);
		
		return true;
			}

	private static boolean isTillable(BlockPos pos, BlockState theBlockState) {

		if (theBlockState.is(BlockTags.DIRT))
			return true;
		
		if (theBlockState.getBlock().getDescriptionId().equals("block.byg.meadow_grass_block"))
			return true;
		
		return false;

			}

	private static boolean isAdjacentWetFarmland(ServerLevel level, BlockPos pos) {
		
	    BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
	    Block B = null;
	    for (int i = 0; i < 4; i++) {
	        mpos.set(pos.getX() + dx[i], pos.getY(), pos.getZ() + dz[i]);
	        Block theBlock = level.getBlockState(mpos).getBlock();
	        if (theBlock instanceof FarmBlock) {
		        if (level.getBlockState(mpos).getValue(FarmBlock.MOISTURE) > 1) {
				return true;
			}
	        	
	        }
		}
		return false;
	}

	private static void vImproveFences(Villager ve, ServerLevel serverLevel, String key, String regrowthType) {

		Brain<Villager> vb = ve.getBrain();
		Optional<GlobalPos> vMeetingPlace = vb.getMemory(MemoryModuleType.MEETING_POINT);
		if (!(vMeetingPlace.isPresent())) {
			return;
		}

		if (isOkayToBuildWallHere(ve, serverLevel)) {
			GlobalPos gVMP = vMeetingPlace.get();
			BlockPos villageMeetingPlaceBlockPos = gVMP.pos();

			if (!(serverLevel.getBlockState(villageMeetingPlaceBlockPos.above(1)).getBlock() instanceof WallBlock)) {
				return;
			}

			// build a wall on perimeter of villager's home
			if (regrowthType.contains("p")) {
				Optional<GlobalPos> villagerHome = vb.getMemory(MemoryModuleType.HOME);
				if (!(villagerHome.isPresent())) {
					return;
				}
				GlobalPos gVHP = villagerHome.get();
				BlockPos villagerHomePos = gVHP.pos();
				// don't build personal walls inside the village wall perimeter.
				// don't build personal walls until the village has a meeting place.
				if (isOutsideMeetingPlaceWall(ve, vMeetingPlace, vMeetingPlace.get().pos(), localBiome)) {
					if (vImproveHomeFence(ve, serverLevel, villagerHomePos, regrowthType)) {
					}
				}
			}
		}
	}

//	ItemStack iStk = new ItemStack(Items.BONE_MEAL,1);
//	BoneMealItem.applyBonemeal(iStk, e.world,e.getPosition());
// (likely 12.2 and 14.4 call?)	ib.performBonemeal((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));\

	// villagers build protective walls around their homes. currently 32 out.
	// to do- reduce distance of wall from home.
	private static boolean vImproveHomeFence(Villager ve, ServerLevel serverLevel, BlockPos vHomePos,
			String regrowthActions) {

		BlockPos vePos = getAdjustedBlockPos(ve);
		String key = "minecraft:" + biomeCategory;
//		ResourceLocation biomeName = ForgeRegistries.BIOMES.getKey(localBiome);
		key = key.toLowerCase();
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		if (currentWallBiomeDataItem == null) {

			key = "minecraft:" + biomeCategory.toLowerCase();
			currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem(key);
			if (currentWallBiomeDataItem == null) {
				currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem("minecraft:plains");
			}
		}

		int homeFenceDiameter = currentWallBiomeDataItem.getWallDiameter();
		homeFenceDiameter = homeFenceDiameter / 4; // resize for personal home fence.
		homeFenceDiameter = (homeFenceDiameter / 2) - 1;

		Collection<PoiRecord> result = serverLevel.getPoiManager()
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
					if (doDebug)
						Utility.debugMsg(2, P.getPos(), "extra Point of Interest Found.");
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

			if (helpPlaceOneWallPiece(ve, serverLevel, homeFenceDiameter, fenceBlockState, vHomePos)) {

				if (regrowthActions.contains("t")) {
					int wallTorchSpacing = homeFenceDiameter / 4;
					if (isValidTorchLocation(homeFenceDiameter, wallTorchSpacing, getAbsVX(ve, vHomePos),
							getAbsVZ(ve, vHomePos), serverLevel.getBlockState(vePos).getBlock())) {
						serverLevel.setBlockAndUpdate(vePos.above(), Blocks.TORCH.defaultBlockState());
					}
				}
				helpJumpAway(ve);
				return true;
			}
		}

		return false;
	}

	private static void vImproveLeaves(Villager ve, ServerLevel serverLevel, String key) {

		float veYaw = ve.getViewYRot(1.0f) / 45;

		// when standing on a grass path, mud, soulsand - game reports you 1 block
		// lower. Adjust.
		BlockPos vePos = getAdjustedBlockPos(ve);

		int facingNdx = Math.round(veYaw);
		if (facingNdx < 0) {
			facingNdx = Math.abs(facingNdx);
		}
		facingNdx %= 8;

		int dx = facingArray[facingNdx][0];
		int dz = facingArray[facingNdx][1];
		BlockPos.MutableBlockPos tempPos = new BlockPos.MutableBlockPos();

		tempPos.set(vePos.getX() + dx, vePos.getY(), vePos.getZ() + dz);
		cutLeavesOrCactus(tempPos, serverLevel);
		tempPos.setY(tempPos.getY() + 1);
		cutLeavesOrCactus(tempPos, serverLevel);

		if (ve.tickCount < 199) {
			tempPos.set(vePos.getX(), vePos.getY(), vePos.getZ());
			cutLeavesOrCactus(tempPos, serverLevel);
			tempPos.setY(tempPos.getY() + 1);
			cutLeavesOrCactus(tempPos, serverLevel);
		}
	
	}

	private static boolean cutLeavesOrCactus(BlockPos tempPos, ServerLevel serverLevel) {
		Block theBlock = null;

		theBlock = serverLevel.getBlockState(tempPos).getBlock();
		if (theBlock instanceof LeavesBlock) {
			boolean persistantLeaves = serverLevel.getBlockState(tempPos).getValue(LeavesBlock.PERSISTENT);
				if (!(persistantLeaves)) {
				serverLevel.destroyBlock(tempPos, false);
				if (doDebug)
					Utility.debugMsg(1, tempPos, " removed Low Leaves. ");
				return true;
				}
			}

		if ((theBlock instanceof CactusBlock)) {
				serverLevel.destroyBlock(tempPos, false);

				if (doDebug)
				Utility.debugMsg(1, tempPos, " removed Cactus. ");
			return true;
		}
		return false;

	}

	private static int getCoalVillagerBonus(Villager ve) {
		
		if (ve.getVillagerData().level() < 3)
			return 0;

		ResourceKey<VillagerProfession> vp = ve.getVillagerData().profession().unwrapKey().get();
		
		if (vp == VillagerProfession.TOOLSMITH) {
			return 1;
		}
		if (vp == VillagerProfession.WEAPONSMITH) {
			return 1;
		}
		if (vp == VillagerProfession.ARMORER) {
			return 1;
		}
		if (vp == VillagerProfession.FISHERMAN) {
			return 1;
		}
		if (vp  == VillagerProfession.BUTCHER) {
			return 1;
		}

		return 0;
	}

	// blocklight is torches and other light sources.
	// skylight is ability to see the sky (drops by 1 per block away from where sky
	// could be seen)
	// net skylight is the sky value lowered by obstructions and
	private static boolean vImproveLighting(Villager ve, ServerLevel serverLevel) {

		if (ve.isSleeping() || ve.isBaby()) {
			return false;
		}

		return tryPlaceTorch(ve, getAdjustedBlockPos(ve), serverLevel);

	}

	private static boolean isTooBrightForTorch(Villager ve, BlockPos pos, ServerLevel serverLevel) {
		// is it too bright to place a torch?
		int lightLevel = serverLevel.getBrightness(LightLayer.BLOCK, pos);
		if (lightLevel >= MyConfig.getTorchLightLevel() + getCoalVillagerBonus(ve)) { 
			return true;
		}
		return false;
	}
	
    public static boolean tryPlaceTorch(Villager ve, BlockPos pos, ServerLevel serverLevel) {

		if (tryPlaceWallTorch(ve, pos, serverLevel)) {
			if (doDebug)
				 Utility.debugMsg(2, pos, "villager placed a wall torch.");
			return true;
		}
        
		if (tryPlaceGroundTorch(ve, pos, serverLevel)) {
			if (doDebug)
				 Utility.debugMsg(2, pos, "villager placed a ground torch.");
    		return true;
    	}

		return false;

	}
    	
	public static boolean tryPlaceGroundTorch(Villager ve, BlockPos pos, ServerLevel serverLevel) {
		
		if (!footBlockState.isAir() && !isNatProgPebbleOrStick())
			return false;
		if (isTooBrightForTorch(ve, pos, serverLevel)) {
            return false;
        }
		if (!TorchBlock.canSupportCenter(serverLevel, pos.below(), Direction.UP)) {
			return false;
		}
		if (footBlockState.getBlock() instanceof BedBlock) {
			return false;
		}
		serverLevel.setBlockAndUpdate(pos, MyConfig.getTorchBlock().defaultBlockState());
		serverLevel.playSound(/* Player */ null, /* Pos */ pos.getX(), pos.getY(), pos.getZ(),
				/* SoundEvent */ SoundEvents.WOOD_PLACE, /* SoundSource */ SoundSource.BLOCKS, /* Volume */ 0.75f,
				/* Pitch */ 0.75f);
		return true;
    	
    }

	public static boolean tryPlaceWallTorch(Villager ve, BlockPos pos, ServerLevel serverLevel) {
	
        BlockPos veHeadPos = pos.above();
		if (!footBlockState.isAir())  {
			return false;
        }

		if (isTooBrightForTorch(ve, veHeadPos, serverLevel)) {
			return false;
		}
		
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        
        Direction facing = Direction.fromYRot(ve.yHeadRot); // The direction the villager is facing
        mPos.set(veHeadPos).move(facing); // Position to wall in front of villager head)
        BlockState bs = serverLevel.getBlockState(veHeadPos);
        bs = serverLevel.getBlockState(mPos);
        if (!(bs.isSolid())) {
            return false; 
        }

        // Prepare the wall torch block state with correct facing
		BlockState wallTorchState = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,
				facing.getOpposite());

		serverLevel.setBlockAndUpdate(veHeadPos, wallTorchState);
    		serverLevel.playSound(/* Player */ null, /* Pos */ pos.getX(), pos.getY(), pos.getZ(),
				/* SoundEvent */ SoundEvents.WOOD_PLACE, /* SoundSource */ SoundSource.BLOCKS, /* Volume */ 0.75f,
				/* Pitch */ 0.75f);
			return true;
	}

	private static void vRoadImprove(Villager ve, ServerLevel serverLevel, String debugkey) {

		if (vRoadFixSnow(ve, serverLevel)) {
			if (doDebug)
				Utility.debugMsg(1, ve, debugkey + "  fixed road snow.");
		}

		if (vRoadFixGrassPatch(ve, serverLevel)) {
			if (doDebug)
				Utility.debugMsg(1, ve, debugkey + "  fixed road grass patch.");
		}

		if (vRoadFixBump(ve, serverLevel)) {
			if (doDebug)
				Utility.debugMsg(1, ve, debugkey + "  fixed road bump.");
		}
		if (vRoadFixPotholes(ve,  serverLevel)) {
			if (doDebug)
				Utility.debugMsg(1, ve, debugkey + "  fixed roadpothole.");
		}
		if (vRoadSmoothHeight(ve, serverLevel)) {
			if (doDebug)
				Utility.debugMsg(1, ve, debugkey + "  fixed road smoothness.");
		}
		
	}

	// Clear generated snow layers off of roads. Naturally falling snow doesn't
	// stick on roads.
	private static boolean vRoadFixSnow(Villager ve, ServerLevel serverLevel) {
		Block biomeRoadBlock = helpBiomeRoadBlockType(Utility.GetBiomeName(localBiome)).getBlock();
		if (groundBlock != biomeRoadBlock) {
			return false;
		}
		if (footBlock instanceof SnowLayerBlock) {
			serverLevel.destroyBlock(adjustedPos, false);
			footBlockState = Blocks.AIR.defaultBlockState();
			footBlock = footBlockState.getBlock();
			return true;
		}
		return false;
	}

	private static boolean vRoadFixGrassPatch(Villager ve, ServerLevel serverLevel) {

		if (!serverLevel.canSeeSky(ve.blockPosition())) {
			return false;
		}

		if (!(isBlockGrassOrDirt(groundBlockState))) {
			return false;
		}

		if ((!groundBlockState.canOcclude()) || (!groundBlockState.isSolid())) {
			return false;
		}

		BlockPos vePos = getAdjustedBlockPos(ve);

		if (footBlock instanceof SnowLayerBlock) {
			serverLevel.destroyBlock(adjustedPos, false);
			footBlockState = Blocks.AIR.defaultBlockState();
			footBlock = footBlockState.getBlock();
		}

		if (countAdjacentRoadBlocks(ve, serverLevel, getAdjustedBlockPos(ve)) >= 3) {
			if (doDebug)
				Utility.debugMsg(1, ve.blockPosition().below(), " actual fix grass in road.");
			serverLevel.setBlockAndUpdate(ve.blockPosition().below(),
					helpBiomeRoadBlockType(Utility.GetBiomeName(localBiome)));
			serverLevel.playSound(/* Player */ null, /* Pos */ vePos.getX(), vePos.getY(), vePos.getZ(),
					/* SoundEvent */ SoundEvents.GRASS_PLACE, /* SoundSource */ SoundSource.BLOCKS, /* Volume */ 0.75f,
					/* Pitch */ 0.75f);
			return true;
		}

			return false;
		}

	private static int countAdjacentRoadBlocks(Entity e, ServerLevel serverLevel, BlockPos pos) {
		int adjacentRoadBlockCount = 0;

		Block biomeRoadBlock = helpBiomeRoadBlockType(Utility.GetBiomeName(localBiome)).getBlock();

		if (doDebug)
			Utility.debugMsg(1, e.blockPosition().below(), " actual fix grass in road.");

		BlockPos eastPos = pos.east();
		BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos(eastPos.getX(), eastPos.getY(), eastPos.getZ());
		adjacentRoadBlockCount = countRoadInColumn(e, adjacentRoadBlockCount, biomeRoadBlock, serverLevel, mPos);

		BlockPos westPos = pos.west();
		mPos.set(westPos.getX(), westPos.getY(), westPos.getZ());
		adjacentRoadBlockCount = countRoadInColumn(e, adjacentRoadBlockCount, biomeRoadBlock, serverLevel, mPos);
		if (adjacentRoadBlockCount < 1)
			return adjacentRoadBlockCount; // mini-optimization early return

		BlockPos northPos = pos.north();
		mPos.set(northPos.getX(), northPos.getY(), northPos.getZ());
		adjacentRoadBlockCount = countRoadInColumn(e, adjacentRoadBlockCount, biomeRoadBlock, serverLevel, mPos);
		if (adjacentRoadBlockCount < 2)
			return adjacentRoadBlockCount; // mini-optimization early return

		BlockPos southPos = pos.south();
		mPos.set(southPos.getX(), southPos.getY(), southPos.getZ());
		adjacentRoadBlockCount = countRoadInColumn(e, adjacentRoadBlockCount, biomeRoadBlock, serverLevel, mPos);

		return adjacentRoadBlockCount;
	}

	private static int countRoadInColumn(Entity e, int adjacentRoadBlockCount, Block biomeRoadBlock, ServerLevel sLevel,
			MutableBlockPos mPos) {

		if (sLevel.getBlockState(mPos.above()).getBlock() == biomeRoadBlock) {
			adjacentRoadBlockCount++;
		}
		if (sLevel.getBlockState(mPos).getBlock() == biomeRoadBlock) { // above grass block
			adjacentRoadBlockCount++;
		}
		if (sLevel.getBlockState(mPos.below()).getBlock() == biomeRoadBlock) {
			adjacentRoadBlockCount++;
		}
		if (sLevel.getBlockState(mPos.below(2)).getBlock() == biomeRoadBlock) {
			adjacentRoadBlockCount++;
		}
		if (sLevel.getBlockState(mPos.below(3)).getBlock() == biomeRoadBlock) {
			adjacentRoadBlockCount++;
		}
		return adjacentRoadBlockCount;
	}

	private static boolean vRoadFixBump(Villager ve, ServerLevel serverLevel) {

		if (!serverLevel.canSeeSky(ve.blockPosition())) {
			return false;
		}

		Block biomeRoadBlock = helpBiomeRoadBlockType(Utility.GetBiomeName(localBiome)).getBlock();
		if (groundBlock != biomeRoadBlock) {
			return false;
		}
		if ((!groundBlockState.canOcclude()) || (!groundBlockState.isSolid())) {
			return false;
		}

		if (footBlock instanceof SnowLayerBlock) {
			serverLevel.destroyBlock(adjustedPos, false);
			footBlockState = Blocks.AIR.defaultBlockState();
			footBlock = footBlockState.getBlock();
		}

		BlockPos vePos = getAdjustedBlockPos(ve);
		if (isRoadBump(serverLevel, vePos)) {
			if (doDebug)
				Utility.debugMsg(1, vePos, " actual fix bumps in road.");
			serverLevel.setBlockAndUpdate(vePos.below(1), Blocks.AIR.defaultBlockState());
			serverLevel.setBlockAndUpdate(vePos.below(2), groundBlock.defaultBlockState());
			serverLevel.playSound(/* Player */ null, /* Pos */ vePos.getX(), vePos.getY(), vePos.getZ(),
					/* SoundEvent */ SoundEvents.GRASS_PLACE, /* SoundSource */ SoundSource.BLOCKS, /* Volume */ 0.75f,
					/* Pitch */ 0.75f);
					return true;
				}
		return false;
	}

	private static boolean isRoadBump(ServerLevel sLevel, BlockPos pos) {
		int adjacentBlocks = 0;
		// int debug = 1;
		// Block b = sLevel.getBlockState(pos.below(2).east()).getBlock();
		if (sLevel.getBlockState(pos.below(1).east()).getBlockHolder().is(BlockTags.AIR)
				&& !sLevel.getBlockState(pos.below(2).east()).getBlockHolder().is(BlockTags.AIR)) {
			adjacentBlocks++;
			}

		// b = sLevel.getBlockState(pos.below(2).west()).getBlock();
		if (sLevel.getBlockState(pos.below(1).west()).getBlockHolder().is(BlockTags.AIR)
				&& !sLevel.getBlockState(pos.below(2).west()).getBlockHolder().is(BlockTags.AIR)) {
			adjacentBlocks++;
		}

		// b= sLevel.getBlockState(pos.below(2).north()).getBlock();
		if (sLevel.getBlockState(pos.below(1).north()).getBlockHolder().is(BlockTags.AIR)
				&& !sLevel.getBlockState(pos.below(2).north()).getBlockHolder().is(BlockTags.AIR)) {
			adjacentBlocks++;
		}

		// b= sLevel.getBlockState(pos.below(2).south()).getBlock();
		if (sLevel.getBlockState(pos.below(1).south()).getBlockHolder().is(BlockTags.AIR)
				&& !sLevel.getBlockState(pos.below(2).south()).getBlockHolder().is(BlockTags.AIR)) {
			adjacentBlocks++;
		}

		if (adjacentBlocks < 4)
		return false;
		return true;
	}

	private static boolean vRoadFixPotholes(Villager ve, ServerLevel serverLevel) {

		Block biomeRoadBlock = helpBiomeRoadBlockType(Utility.GetBiomeName(localBiome)).getBlock();

		if (groundBlock != biomeRoadBlock) {
			return false;
		}

		if (!serverLevel.canSeeSky(ve.blockPosition())) {
			return false;
		}

		if ((footBlock instanceof SnowLayerBlock)) {
			serverLevel.destroyBlock(getAdjustedBlockPos(ve), false);
		}

		if (isRoadPotHole(serverLevel, getAdjustedBlockPos(ve))) {
			if (doDebug)
				Utility.debugMsg(1, ve.blockPosition(), " actual fix pothole in road.");
			serverLevel.setBlockAndUpdate(adjustedPos, biomeRoadBlock.defaultBlockState());
			serverLevel.setBlockAndUpdate(adjustedPos.below(), biomeRoadBlock.defaultBlockState());
			helpJumpAway(ve);
			int gX = groundBlockPos.getX();
			int gY = groundBlockPos.getY();
			int gZ = groundBlockPos.getZ();
			serverLevel.playSound(/* Player */ null, /* Pos */ gX, gY, gZ, /* SoundEvent */ SoundEvents.GRASS_BREAK,
					/* SoundSource */ SoundSource.BLOCKS, /* Volume */ 0.75f, /* Pitch */ 0.75f);
			return true;
		}
		return false;
	}

	private static boolean isRoadPotHole(ServerLevel serverLevel, BlockPos pos) {
		int count = 0;
		if (!serverLevel.getBlockState(pos.east()).getBlockHolder().is(BlockTags.AIR)) {
			count++;
		}
		if (!serverLevel.getBlockState(pos.west()).getBlockHolder().is(BlockTags.AIR)) {
			count++;
				}
		if (!serverLevel.getBlockState(pos.north()).getBlockHolder().is(BlockTags.AIR)) {
			count++;
			}
		if (!serverLevel.getBlockState(pos.south()).getBlockHolder().is(BlockTags.AIR)) {
			count++;
		}

		if (count >= 4)
			return true;
		return false;
		}

	private static boolean vRoadSmoothHeight(Villager ve, ServerLevel serverLevel) {

		if (!serverLevel.canSeeSky(ve.blockPosition())) {
		return false;
		}

		if (!ve.onGround()) {
			return false;
		}

		if (ve.isBaby()) {
			return false;
		}

		BlockPos vePos = getAdjustedBlockPos(ve);

		Block biomeRoadBlock = helpBiomeRoadBlockType(Utility.GetBiomeName(localBiome)).getBlock();

		if (biomeRoadBlock == Blocks.SMOOTH_SANDSTONE) {
			if (!isRoadPiece)
				return false;
		}

		if ((groundBlockState.getBlock() != biomeRoadBlock) && (footBlockState.getBlock() != biomeRoadBlock)) {
			return false;
		}

		BlockState biomeRoadBlockState = biomeRoadBlock.defaultBlockState();

		// Check for nearby point of interests.
		int poiDistance = 3;
		if (Utility.GetBiomeName(localBiome).contains("desert")) {
			poiDistance = 7;
		}

		if (isNearbyPoi(ve, serverLevel, localBiome, vePos, poiDistance)) {
			return false;
		}

		// Check for higher block to smooth up towards
		int veX = vePos.getX();
		int veY = vePos.getY();
		int veZ = vePos.getZ();
		int roadY = 0;
		MutableBlockPos mPos = new MutableBlockPos();

		for (int i = 0; i < 4; i++) {
		    int checkX = veX + dx[i];
		    int checkZ = veZ + dz[i];
		    roadY = serverLevel.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, checkX, checkZ) - 1;

			if (roadY > veY) {
		        mPos.set(checkX, roadY, checkZ);
		        Block tempBlock = serverLevel.getBlockState(mPos).getBlock();

				if (tempBlock == biomeRoadBlock) {
					if (serverLevel.getBlockState(vePos).getBlock() instanceof AirBlock) {
		                mPos.set(veX, veY, veZ);
		                serverLevel.setBlockAndUpdate(mPos, biomeRoadBlockState);
						ve.setDeltaMovement(0.0, 0.4, 0.0);
						return true;
					}
				}
			}
		}
		
		return false;
	}

	private static boolean isOkayToBuildWall(Villager ve, ServerLevel serverLevel) {

		if (MyConfig.getPlayerWallControlBlock() == Blocks.AIR) {
			return false;
		}

		Optional<GlobalPos> optMeetingPoI = ve.getBrain().getMemory(MemoryModuleType.MEETING_POINT);
		if (optMeetingPoI.isEmpty()) {
			return false;
		}

		BlockPos villageMeetingPlacePos = optMeetingPoI.get().pos();
		if (serverLevel.getBlockState(villageMeetingPlacePos).getBlock() != Blocks.BELL) {
			return false;
		}

		return true;

	}

	private static void initWallPlayerControlBlock(ServerLevel serverLevel, BlockPos pos) {

		if (serverLevel.getChunkAt(pos).getInhabitedTime() < 199) { // TODO set to 199
			if (serverLevel.getBlockState(pos.above(1)).getBlock() != MyConfig.getPlayerWallControlBlock()) {
				serverLevel.setBlockAndUpdate(pos.above(1), MyConfig.getPlayerWallControlBlock().defaultBlockState());
			}
		}

	}

	private static boolean vImproveVillageWall(Villager ve, ServerLevel serverLevel, String regrowthActions) {

		if (!isOkayToBuildWall(ve, serverLevel)) {
			return false;
		}

		if (!isOkayToBuildWallHere(ve, serverLevel)) {
				return false;
			}

		BlockPos villageMeetingPlacePos = ve.getBrain().getMemory(MemoryModuleType.MEETING_POINT).get().pos();

		initWallPlayerControlBlock(serverLevel, villageMeetingPlacePos);

		if (serverLevel.getBlockState(villageMeetingPlacePos.above(1)).getBlock() != MyConfig.playerWallControlBlock) {
			return false;
		}

		BlockPos vePos = getAdjustedBlockPos(ve);
		String key = "minecraft:" + biomeCategory;
		key = key.toLowerCase();

		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		int wallRadius = currentWallBiomeDataItem.getWallDiameter();
		wallRadius = (wallRadius / 2) - 1;

		if (isOnWallRadius(ve, wallRadius, villageMeetingPlacePos)) {
			// check for other meeting place bells blocking wall since too close.
			Collection<PoiRecord> result = serverLevel.getPoiManager()
					.getInSquare(t -> t == PoiTypes.MEETING, ve.blockPosition(), 41, Occupancy.ANY)
					.collect(Collectors.toCollection(ArrayList::new));

			// 08/30/20 Collection had bug with range that I couldn't resolve.
			boolean buildWall = true;
			if (!(result.isEmpty())) {

				Iterator<PoiRecord> i = result.iterator();
				while (i.hasNext()) { // in 16.1, finds the point of interest.

					PoiRecord P = i.next();
					if ((villageMeetingPlacePos.getX() == P.getPos().getX())
							&& (villageMeetingPlacePos.getY() == P.getPos().getY())
							&& (villageMeetingPlacePos.getZ() == P.getPos().getZ())) {
						continue; // ignore meeting place that owns this wall segment.
					} else {
						int disX = Math.abs(ve.blockPosition().getX() - P.getPos().getX());
						int disZ = Math.abs(ve.blockPosition().getZ() - P.getPos().getZ());
						if ((disX < wallRadius) && (disZ < wallRadius)) {
							buildWall = false; // another meeting place too close. cancel wall.
							break;
						}
					}
				}

			}

			if (buildWall) {

				if (isVillagerProfession(ve, VillagerProfession.MASON)) {
					tryBuildMasonWalls(serverLevel, ve, wallRadius, villageMeetingPlacePos, currentWallBiomeDataItem);
				}

				BlockState wallTypeBlockState = currentWallBiomeDataItem.getWallBlockState();
				BlockState wallBlock = wallTypeBlockState;

				if (helpPlaceOneWallPiece(ve, serverLevel, wallRadius, wallBlock, villageMeetingPlacePos)) {
					if (regrowthActions.contains("t")) {
				int wallTorchSpacing = (wallRadius + 1) / 4;
						if (isValidTorchLocation(wallRadius, wallTorchSpacing, getAbsVX(ve, villageMeetingPlacePos),
								getAbsVZ(ve, villageMeetingPlacePos), serverLevel.getBlockState(vePos).getBlock())) {
							serverLevel.setBlockAndUpdate(vePos.above(), Blocks.TORCH.defaultBlockState());
						}
					}
					helpJumpAway(ve);
					return true;
				}
			}
		}
		return false;

	}

	private static boolean isWallorLantern(ServerLevel serverLevel, BlockPos mPos, BlockState wallBs) {

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

	private static BlockPos calcSurfaceBlockPos(ServerLevel sLevel, BlockPos mPos) {
		int x = mPos.getX();
		int z = mPos.getZ();
		int y = sLevel.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos m1Pos = new BlockPos(x, y, z);
		return m1Pos;
	}

	private static boolean tryBuildWallCorner(ServerLevel serverLevel, BlockPos mPos, BlockState wallBs) {
		
		if (!(isWallorLantern(serverLevel, mPos, wallBs))) {
			serverLevel.setBlockAndUpdate(mPos, wallBs);
			serverLevel.setBlockAndUpdate(mPos.above(), Blocks.LANTERN.defaultBlockState());
			return true;
		}
		
		return false;
	}
	
	// only happens when a mason build a wall section so performance hit low.
	// TODO :enhance later to fill in a hole on the wall on that side.
	private static boolean tryBuildMasonWalls(ServerLevel sLevel, Villager ve, int wallRadius, BlockPos meetingPlacePos,
			WallBiomeDataManager.WallBiomeDataItem wi) {

		if (doDebug)
			Utility.debugMsg(1, "tryBuildMasonWalls from Meeting Place Pos: " + meetingPlacePos);

		BlockState wallBs = wi.getWallBlockState();
		BlockPos mPos = null;
		
		mPos = calcSurfaceBlockPos(sLevel, meetingPlacePos.east(wallRadius).north(wallRadius + 1));
		if (tryBuildWallCorner(sLevel, mPos, wallBs)) {
			if (doDebug)
				Utility.debugMsg(1, "built north eastern corner at :" + mPos);
			return true;
		}

		mPos = calcSurfaceBlockPos(sLevel, meetingPlacePos.east(wallRadius).south(wallRadius));
		if (tryBuildWallCorner(sLevel, mPos, wallBs)) {
			if (doDebug)
				Utility.debugMsg(1, "built south eastern corner at :" + mPos);
			return true;
	}

		mPos = calcSurfaceBlockPos(sLevel, meetingPlacePos.west(wallRadius + 1).north(wallRadius + 1));
		if (tryBuildWallCorner(sLevel, mPos, wallBs)) {
			if (doDebug)
				Utility.debugMsg(1, "built north western corner at :" + mPos);
			return true;
			}

		mPos = calcSurfaceBlockPos(sLevel, meetingPlacePos.west(wallRadius + 1).south(wallRadius));

		if (tryBuildWallCorner(sLevel, mPos, wallBs)) {
			if (doDebug)
				Utility.debugMsg(1, "built south western corner at :" + mPos);
			return true;
		}

		return false;

	}
	
	private static void vImproveWalls(Villager ve, ServerLevel serverLevel, String key, String regrowthType) {

		if (groundBlockState.isAir()) {
			return; // ignore edge cases where villager is hanging on the edge of a block.
		}

		if (!(ve.getBrain().getMemory(MemoryModuleType.MEETING_POINT)).isPresent())
			return;

		if (vImproveVillageWall(ve, serverLevel, regrowthType)) {
			if (doDebug)
				Utility.debugMsg(1, ve, "Town Wall Improved.");
		}
	}

	private static void vToolMasterHealing(Villager ve, ServerLevel serverLevel) {

		if (!isVillagerProfession(ve, VillagerProfession.TOOLSMITH)) {
			return;
		}

		long daytime = serverLevel.getDayTime() % 24000;

		if (daytime < 9000 || daytime > 11000) {
			return;
		}

			int villagerLevel = ve.getVillagerData().level();
			if (villagerLevel < 1)
				return;
			BlockPos pos = BlockPos.containing(ve.getX(), (ve.getY() + 0.99d), (ve.getZ()));
			AABB box = AABB.encapsulatingFullBlocks(pos.east(6).above(3).north(6), pos.west(6).below(2).south(6));

		List<IronGolem> l = serverLevel.getEntities(EntityType.IRON_GOLEM, box, e -> true);

			for (IronGolem e : l) {
				boolean heal = true;

				if (e.getHealth() >= e.getMaxHealth()) {
					heal = false;
				}
				if (e.getEffect(MobEffects.REGENERATION) != null) {
					heal = false;
				}
				if (heal) {
					e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, villagerLevel * 51, 0), ve);
					ve.addEffect(new MobEffectInstance(MobEffects.REGENERATION, villagerLevel * 11, 0), ve);
				serverLevel.playSound(null, pos, SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.NEUTRAL, 0.5f, 0.5f);
					return;
				}
			}

	}

}
