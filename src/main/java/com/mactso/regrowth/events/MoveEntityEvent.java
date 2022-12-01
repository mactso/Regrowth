package com.mactso.regrowth.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import com.mactso.regrowth.config.RegrowthEntitiesManager.RegrowthMobItem;
import com.mactso.regrowth.config.WallBiomeDataManager;
import com.mactso.regrowth.config.WallFoundationDataManager;
import com.mactso.regrowth.utility.Utility;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.CoralBlock;
import net.minecraft.block.CoralBlockBlock;
import net.minecraft.block.CoralWallFanBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FernBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.MushroomPlantBlock;
import net.minecraft.block.MyceliumBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.TallFlowerBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Category;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage.OccupationStatus;
import net.minecraft.world.poi.PointOfInterestType;

public class MoveEntityEvent {
	public static final DirectionProperty FACING = HorizontalFacingBlock.FACING; 
	public static final BooleanProperty OPEN = FenceGateBlock.OPEN; 
	private static Random moveRand = new Random();
	private static final Block[] coralPlants = { Blocks.TALL_SEAGRASS, Blocks.SEAGRASS, Blocks.SEA_PICKLE,
			Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN,
			Blocks.TUBE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN,
			Blocks.HORN_CORAL_FAN, Blocks.TUBE_CORAL_FAN };
	private static final Block[] coralfans = { Blocks.BRAIN_CORAL_WALL_FAN, Blocks.BUBBLE_CORAL_WALL_FAN,
			Blocks.FIRE_CORAL_WALL_FAN, Blocks.HORN_CORAL_WALL_FAN, Blocks.TUBE_CORAL_WALL_FAN };
	private static final BlockRotation[] coralfanrotations = { BlockRotation.CLOCKWISE_180, BlockRotation.COUNTERCLOCKWISE_90,
			BlockRotation.CLOCKWISE_90, BlockRotation.NONE };

	private static int[] dx = { 1, 0, -1, 0 };
	private static int[] dz = { 0, 1, 0, -1 };
	private static int CHECKS_PER_SECOND = 10;
	private static int[][] facingArray = { { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 }, { 0, -1 }, { 1, -1 }, { 1, 0 },
			{ 1, 1 } };
	private static int lastTorchX = 0;
	private static int lastTorchY = 0;
	private static int lastTorchZ = 0;
	static final int WALL_CENTER = 0;
	static final int FENCE_CENTER = 0;
	static final int WALL_TYPE_WALL = -1;
	static final int WALL_TYPE_FENCE = -2;

	static final String ACTION_GROW = "grow";
	static final String ACTION_EAT = "eat";
	static final String ACTION_BOTH = "both";
	static final String ACTION_TALL = "tall";
	static final String ACTION_MUSHROOM = "mushroom";
	static final String ACTION_STUMBLE = "stumble";
	static final String ACTION_REFOREST = "reforest";
	static final String ACTION_CORAL = "coral";

	private static BlockState footBlockState;
	private static BlockState groundBlockState;
	private static Block footBlock;
	private static Block groundBlock;
	private static Biome localBiome;
	private static boolean isRoadPiece = false;
	private static Category biomeCategory;
	static BlockPos adjustedPos;


	public static boolean handleTrampleEvent(Entity entity) {

		BlockPos pos = entity.getBlockPos();
		Utility.debugMsg(1, pos, "Enter handleTrampleEvent");

		if (entity instanceof VillagerEntity ve) {
			if ((ve.getVillagerData().getProfession() == VillagerProfession.FARMER)
					&& (ve.getVillagerData().getLevel() > 3)) {
				Utility.debugMsg(1, pos, "Villager is L3 farmer");
				return true;
			}
		}
		if ((entity instanceof ServerPlayerEntity spe)) {
			if (spe.isCreative()) {
				return true;
			}
			Utility.debugMsg(1, pos, "FarmlandTrampleCancelled");
		}

		return false;
	}


	@SuppressWarnings("deprecation")
	public static void handleEntityMoveEvents(LivingEntity entity) {

		if (entity instanceof PlayerEntity)
			return;

		if (entity.getId()%2 == entity.world.getTime()%2) 
			return;
		
		if (entity.getBlockPos() == null) 
			return;

		String registryNameAsString = helperGetRegistryNameAsString(entity);
		RegrowthMobItem currentRegrowthMobItem = RegrowthEntitiesManager.getRegrowthMobInfo(registryNameAsString);
		if (currentRegrowthMobItem == null)
			return;

		if (entity.world instanceof ServerWorld sLevel) {

			adjustedPos = getAdjustedBlockPos(entity);

			footBlockState = getAdjustedFootBlockState(entity);
			footBlock = footBlockState.getBlock();

			if (footBlock instanceof CarpetBlock)
				return;

			groundBlockState = getAdjustedGroundBlockState(entity);
			groundBlock = groundBlockState.getBlock();
			if (groundBlockState.isAir())
				return;

			localBiome = entity.world.getBiome(entity.getBlockPos()).value();
			biomeCategory = Biome.getCategory(sLevel.getBiome(entity.getBlockPos()));

			String regrowthActions = currentRegrowthMobItem.getRegrowthActions();

			if (isImpossibleRegrowthEvent(regrowthActions))
				return;

			double regrowthEventOdds = 1 / (currentRegrowthMobItem.getRegrowthEventSeconds() * CHECKS_PER_SECOND);
			if (isHorseTypeEatingNow(entity)) {
				regrowthEventOdds *= 20;
			}
			double randomD100Roll = entity.world.random.nextDouble();
			int debugvalue = 0; // TODO make sure value 0 after debugging.

			long chunkAge = entity.world.getChunk(entity.getBlockPos()).getInhabitedTime();

			// improve village roads and walls faster for the first 200 minutes;
			if (chunkAge < 480000) {
				if (entity instanceof VillagerEntity ve) {
					if (ve.world.getTime() % 12 == 0) {
						if (regrowthActions.contains("r")) {
							vImproveRoads(ve, "preRoad");
						}
						if (regrowthActions.contains("w")) {
							vImproveVillageWall(ve, regrowthActions);
						}

					}
				}
			}

			if (randomD100Roll <= regrowthEventOdds + debugvalue) {
				if (entity instanceof VillagerEntity ve) {
					improvePowderedSnow(entity);
					// if onGround and not on bed.
					if ((ve.isOnGround()) && (!(footBlock instanceof BedBlock))) {
						doVillagerRegrowthEvents(ve, registryNameAsString, regrowthActions);
					}
				} else {
					doMobRegrowthEvents(entity, registryNameAsString, regrowthActions);
				}
			}

		}

	}

	private static String helperGetRegistryNameAsString(Entity entity) {
		return Utility.getResourceLocationString(entity);
	}

	private static BlockState getAdjustedFootBlockState(Entity e) {
		if (e.getY() == e.getBlockPos().getY()) {
			return e.world.getBlockState(e.getBlockPos());
		}
		return e.world.getBlockState(e.getBlockPos().up());
	}

	private static int getAdjustedY(Entity e) {
		if (e.getY() == e.getBlockPos().getY())
			return 1;
		return 0;
	}

	private static BlockState getAdjustedGroundBlockState(Entity e) {
		return e.world.getBlockState(e.getBlockPos().down(getAdjustedY(e)));
	}

	private static BlockPos getAdjustedBlockPos(Entity e) {
		if (e.getY() == e.getBlockPos().getY()) {
			return e.getBlockPos();
		}
		return e.getBlockPos().up();
	}

	private static void improvePowderedSnow(Entity entity) {
		World sLevel = entity.world;
		if (entity.inPowderSnow) {
			int hp = 0;
			if (sLevel.getBlockState(entity.getBlockPos().up(2)).getBlock() == Blocks.POWDER_SNOW) {
				entity.world.breakBlock(entity.getBlockPos().up(2), false);
				hp = 2;
			}
			if (sLevel.getBlockState(entity.getBlockPos().up()).getBlock() == Blocks.POWDER_SNOW) {
				entity.world.breakBlock(entity.getBlockPos().up(), false);
				hp += 2;
			}
			if (sLevel.getBlockState(entity.getBlockPos()).getBlock() == Blocks.POWDER_SNOW) {
				// SnowBlock.Layers is an IntProperty
				int layers = 2 + hp;
				BlockState snowBlock = Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, layers);
				entity.world.setBlockState(entity.getBlockPos(), snowBlock);
			}
		}
	}

	private static void doMobRegrowthEvents(Entity entity, String key, String regrowthType) {

		if (regrowthType.equals(ACTION_STUMBLE)) {
			if ((footBlock instanceof TorchBlock) || (footBlock instanceof WallTorchBlock)) {
				mobStumbleAction(entity, key);
			}
			return;
		}

		if (regrowthType.equals(ACTION_REFOREST)) {
			mobReforestAction(entity, key);
			return;
		}

		if (regrowthType.equals(ACTION_MUSHROOM)) {
			mobGrowMushroomAction(entity, key);
			return;
		}

		if (regrowthType.equals(ACTION_CORAL)) {
			mobGrowCoralAction(entity, key);
			return;
		}

		mobHandleOverCrowding (entity, key);
			
		// all remaining actions currently require a grass block underfoot so if not a
		// grass block- can exit now.
		// this is for performance savings only.
		// looks like meadow_grass_block is not a grassBlock

		if (!isKindOfGrassBlock(groundBlockState)) {
			return;
		}

		if (regrowthType.equals(ACTION_TALL)) {
			mobGrowTallAction(entity, key);
			return;
		}

		if (regrowthType.equals(ACTION_BOTH)) {
			if (entity.world.random.nextDouble() * 100 > 85.0) {
				regrowthType = ACTION_GROW;
			} else {
				regrowthType = ACTION_EAT;
			}
		}

		if (regrowthType.contentEquals(ACTION_EAT)) {
			mobEatPlantsAction(entity, key, regrowthType);
			return;
		}

		if ((regrowthType.equals(ACTION_GROW))) {
			mobGrowPlantsAction(entity, key);
			return;
		}
	}

	private static void mobHandleOverCrowding(Entity e, String key) {
		BlockPos ePos = new BlockPos(e.getX(), e.getY(), e.getZ());
		if (e instanceof AnimalEntity a) {
			if (e.world instanceof ServerWorld world) {
				Box box = new Box(ePos.east(3).up(2).north(3), ePos.west(3).down(2).south(3));
				int excess = world.getEntitiesByType(e.getType(), box, (entity) -> true).size()-16;

				if (excess > 0) {
					if (excess > 16) {
						e.world.playSound(e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_COW_DEATH, SoundCategory.NEUTRAL, 1.1f, 0.54f, true);
						e.setPosition(e.getX(), -66, e.getZ());

					} else {
						float hurt = excess + (world.getRandom().nextFloat()/6);
						e.damage(DamageSource.IN_WALL, hurt);

						
					}
				}
			}
		}
	}


	private static boolean mobGrowCoralAction(Entity e, String key) {

		World level = e.world;
		int sealevel = level.getSeaLevel();
		Random rand = level.getRandom();

		// Block bwf = Blocks.BRAIN_CORAL_WALL_FAN;
		
		moveRand.setSeed(e.getBlockY()*1151+e.getBlockX()*51+e.getBlockZ()*31);  // "predictable" random.
		double docoralplant = moveRand.nextDouble();
		docoralplant = moveRand.nextDouble();
		double docoralfan = moveRand.nextDouble();
		int coralfanDirection = moveRand.nextInt(4);
		int minCoraldepth = sealevel -4 + moveRand.nextInt(2);
		int maxCoraldepth = sealevel -16;
		
		if (e.getBlockY() > minCoraldepth) return false;
		if (e.getBlockY() < maxCoraldepth) return false;

		BlockPos pos = e.getBlockPos();


		
		if (level.getBlockState(pos.down(0)).getBlock() != Blocks.WATER) return false; // should be impossible.
		if (level.getBlockState(pos.down(1)).getBlock() != Blocks.WATER) return false;
		Block b = level.getBlockState(pos.down(2)).getBlock();

		if (level.getBlockState(pos.down(2)).getBlock() != Blocks.WATER) {
			if ((b != Blocks.STONE) && (b!= Blocks.GLASS)) {
				Utility.debugMsg(1,pos, "Block: " + level.getBlockState(pos.down(2)).getBlock().getTranslationKey() );
				
			}
		}

		boolean fabpatch = false;
		if (level.getBlockState(pos.down(2)).getBlock() instanceof CoralBlockBlock) {
			fabpatch = true;
		}
		if (level.getBlockState(pos.down(2)).getBlock() instanceof CoralBlock) {
			fabpatch = true;
		}
		
		if (fabpatch) {
//			Utility.debugMsg(1, pos, "Coral double:" + docoralplant);
//			Utility.debugMsg(1, pos, "Coral fan double:" + docoralfan);
			Utility.debugMsg(1, pos, "Coral plant opportunity:" +e.getType().getRegistryEntry().toString() +" .");

			if (docoralfan < 0.3) {  // TODO set back to 0.3
				Direction d = Direction.fromHorizontal(coralfanDirection);
				BlockPos fanPos = e.getBlockPos().down(2).offset(d);
				if (level.getBlockState(fanPos).getBlock() == Blocks.WATER) {
					level.setBlockState(fanPos, coralfans[rand.nextInt(coralfans.length)].getDefaultState().with(CoralWallFanBlock.FACING,d));
				}
			}
			
//			int x = 3;
			int count = countCoral(e);
//			Utility.debugMsg(1, pos, "CORAL count = :" + count + ", "+e.getType().getRegistryEntry().toString() +" .");
			if (count > 5) return false;
			BlockState theCoralBlock = level.getBlockState(pos.down(2)); // grow same kind of coral block
			if ((count < 6) && (e.getBlockY() == minCoraldepth)) {
				if (docoralplant < 0.30) return false;
				Utility.debugMsg(1, pos, "CORAL Plant grows over Coral Block:" +e.getType().getRegistryEntry().toString() +" .");
				level.setBlockState(pos.down(1), coralPlants[rand.nextInt(coralPlants.length)].getDefaultState());
				level.playSound(null, pos, SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundCategory.AMBIENT, 0.9f, 1.4f);
				return true;
			} else if ( (e.getBlockY() < minCoraldepth)) {
				int ew = rand.nextInt(3)-1;
				int ns = rand.nextInt(3)-1;
				if (level.getBlockState(pos.down(1).east(ew).north(ns)).getBlock() != Blocks.WATER) return false;
				Utility.debugMsg(1, pos, "CORAL Block grows over Coral Block:" +e.getType().getRegistryEntry().toString() +" .");
				level.setBlockState(pos.down(1).east(ew).north(ns), theCoralBlock);
				level.playSound(null, pos, SoundEvents.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.AMBIENT, 0.9f, 1.4f);
				Utility.debugMsg(1, pos, "CORAL:" +e.getType().getRegistryEntry().toString() +" new block set at near " + pos.down(1)+" .");

			}

			
		}
		
		return true;
	}

	private static int countCoral(Entity e) {
		int c = 0;
		for (int ud = -1; ud <= 0; ud++) {
			for (int ew = -1; ew <= 1; ew++) {
				for (int ns = -1; ns <= 1; ns++) {
					if (e.world.getBlockState(e.getBlockPos().down(1).east(ew).north(ns)).getBlock() instanceof CoralBlock) {
						c++;
					}
				}
			}
		}
		return c;
	}

	private static boolean mobGrowPlantsAction(Entity entity, String key) {

		if (footBlockState.isAir()) {
			if (!(groundBlock instanceof Fertilizable)) {
				return false;
			}
			BlockPos bpos = entity.getBlockPos();
			if (bpos == null) {
				Utility.debugMsg(1, "ERROR:" + key + "grow plant null position.");
				return false;
			}
			Fertilizable ib = (Fertilizable) groundBlock;
			Utility.debugMsg(1, entity.getBlockPos(), key + " growable plant found.");
			try {
				ServerWorld serverworld = (ServerWorld) entity.world;
				BlockState bs = entity.world.getBlockState(bpos);
				ib.grow(serverworld, entity.world.random, bpos, bs);
				Utility.debugMsg(1, bpos, key + " grew plant.");
			} catch (Exception e) {
				Utility.debugMsg(1, bpos, key + " caught grow attempt exception.");
			}
		}
		return true;
	}

	private static boolean isNearWater(World world, BlockPos pos) {
		Box box = new Box(pos.east(4).north(4), pos.west(4).south(4));
		return world.containsFluid(box);
		// FarmlandWaterManager for mod compatability- forge only?
	}

	private static boolean isKindOfGrassBlock(BlockState groundBlockState) {
		if (groundBlockState.getBlock() instanceof GrassBlock)
			return true;
		if (groundBlockState.getBlock().getTranslationKey().equals("block.byg.meadow_grass_block"))
			return true;
		return false;
	}

	private static boolean isBlockGrassOrDirt(BlockState tempBlockState) {

		if (isKindOfGrassBlock(tempBlockState) || (tempBlockState.getBlock() == Blocks.DIRT)) {
			return true;
		}
		return false;
	}

	private static BlockState helperSaplingState(World world, BlockPos pos, Biome localBiome, BlockState sapling) {

		// TODO use new BlockTag.SAPLING
		sapling = Blocks.OAK_SAPLING.getDefaultState();
		RegistryKey<Registry<Biome>> k = Registry.BIOME_KEY;

		String biomeName = world.getRegistryManager().get(k).getKey(localBiome).get().getValue().toString();

		if (biomeName.contains("birch")) {
			sapling = Blocks.BIRCH_SAPLING.getDefaultState();
		}
		if (biomeName.contains("taiga")) {
			sapling = Blocks.SPRUCE_SAPLING.getDefaultState();
		}
		if (biomeName.contains("jungle")) {
			sapling = Blocks.JUNGLE_SAPLING.getDefaultState();
		}
		if (biomeName.contains("savanna")) {
			sapling = Blocks.ACACIA_SAPLING.getDefaultState();
		}
		if (biomeName.contains("desert")) {
			sapling = Blocks.ACACIA_SAPLING.getDefaultState();
		}
		return sapling;
	}

	private static void mobReforestAction(Entity entity, String key) {

		if (footBlock != Blocks.AIR)
			return;

		if (!(isBlockGrassOrDirt(groundBlockState)))
			return;

		BlockPos ePos = getAdjustedBlockPos(entity);
		// only try to plant saplings in about 1/4th of blocks.
		double sinY = Math.sin((double) ((ePos.getY() + 64) % 256) / 256);

		if (entity.world.random.nextDouble() > Math.abs(sinY))
			return;

		BlockState sapling = null;
		// are we in a biome that has saplings in a spot a sapling can be planted?
		sapling = helperSaplingState(entity.world, ePos, localBiome, sapling);

		// check if there is room for a new tree. Original trees.
		// don't plant a sapling near another sapling
		// TEST: The SaplingBlock
		int hval = 5;
		int yval = 0;
		int yrange = 0;

		if (helperCountBlocksBB(SaplingBlock.class, 1, entity.world, ePos, hval, yrange) > 0)
			return;

		// check if there is room for a new tree.

		int leafCount = 0;
		yval = 4;
		yrange = 0;
		hval = 4;

		if (sapling == Blocks.ACACIA_SAPLING.getDefaultState()) {
			yval = 5;
			hval = 7;
		}
		// TEST: The LeavesBlock
		leafCount = helperCountBlocksBB(LeavesBlock.class, 1, entity.world, ePos.up(yval), hval, yrange);
		if (leafCount > 0)
			return;

		entity.world.setBlockState(ePos, sapling);
		Utility.debugMsg(1, ePos, key + " planted sapling.");
	}

	private static void mobGrowMushroomAction(Entity entity, String key) {
		ServerWorld sWorld = (ServerWorld) entity.world;
		BlockPos ePos = entity.getBlockPos();
		if (sWorld.getBlockState(ePos).getBlock() instanceof MushroomBlock) {
			return;
		}

		if (sWorld.isSkyVisible(ePos)) {
			return;
		}
		if (!(isGoodMushroomTemperature(entity))) {
			return;
		}

		Random mushRand = new Random(helperLongRandomSeed(entity.getBlockPos()));

		double fertilityDouble = mushRand.nextDouble();
		fertilityDouble = mushRand.nextDouble();

		if (fertilityDouble < .75) {
			Utility.debugMsg(1, ePos, key + " Mushroom fertility (" + fertilityDouble + ") non-growing spot.");
			return;
		}

		int smallMushroomCount = helperCountBlocksBB(MushroomBlock.class, 4, sWorld, ePos, 4, 1);

		if (smallMushroomCount > 3) {
			Utility.debugMsg(1, ePos, key + " smallMushroomCount (" + smallMushroomCount + ") mushroom too crowded.");
			return;
		}

		int myceliumCount = helperCountBlocksBB(MyceliumBlock.class, 4, sWorld, ePos, 4, 1);
		if (myceliumCount > 2) {
			Utility.debugMsg(1, ePos, key + " myceliumCount (" + myceliumCount + ") mycelium too crowded.");
			return;
		}

		// dust the top of giant mushrooms with little mushrooms of the same type.

		if (groundBlock == Blocks.RED_MUSHROOM_BLOCK) {
			sWorld.setBlockState(ePos, Blocks.RED_MUSHROOM.getDefaultState());
			return;
		}

		if (groundBlock == Blocks.BROWN_MUSHROOM_BLOCK) {
			sWorld.setBlockState(ePos, Blocks.BROWN_MUSHROOM.getDefaultState());
			return;
		}

		int hugeMushroomCount = helperCountBlocksBB(MushroomBlock.class, 1, sWorld, ePos, 1, 1);
		if (hugeMushroomCount > 0) {
			// if right next to a huge mushroom let it grow if it got past above density
			// check.
		} else {
			int huge = helperCountBlocksBB(MushroomBlock.class, 1, sWorld, ePos, MyConfig.getMushroomDensity(), 1);
			if (huge > 0) {
				Utility.debugMsg(1, ePos, key + " huge (" + huge + ") mushroom too crowded.");
				return;
			}
		}

		boolean growMushroom = false;
		if (BlockTags.BASE_STONE_OVERWORLD == null) {
			Utility.warn ( "BlockTags.BASE_STONE_OVERWORLD missing.");
			if (groundBlock == Blocks.STONE || groundBlock == Blocks.DIORITE || groundBlock == Blocks.ANDESITE
					|| groundBlock == Blocks.GRANITE) {
				growMushroom = true;
			}
		} else {
			if (!groundBlockState.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
				return;
			}
			growMushroom = true;
		}

		if (sWorld.isPlayerInRange((double) ePos.getX(), (double) ePos.getY(), (double) ePos.getZ(), 12.0)) {
			growMushroom = false;
		}

		if (growMushroom) {

			double vx = entity.getPos().getX() - (ePos.getX() + 0.5d);
			double vz = entity.getPos().getZ() - (ePos.getZ() + 0.5d);

			Vec3d vM = new Vec3d(vx, 0.0d, vz).normalize().multiply(1.0d).add(0, 0.5, 0);
			entity.setVelocity(entity.getVelocity().add(vM));
			if (fertilityDouble > 0.9) {
				sWorld.setBlockState(ePos.down(), Blocks.MYCELIUM.getDefaultState());
			}

			Block theBlock = null;
			if (sWorld.random.nextDouble() * 100.0 > 75.0) {
				theBlock = Blocks.RED_MUSHROOM;
			} else {
				theBlock = Blocks.BROWN_MUSHROOM;
			}
			sWorld.setBlockState(ePos, theBlock.getDefaultState());
			MushroomPlantBlock mb = (MushroomPlantBlock) theBlock;
			// missing method growMushroom
			try {
				mb.grow(sWorld, sWorld.random, ePos, theBlock.getDefaultState());
			} catch (Exception e) {
				// technically an "impossible" error but it's happened so this should
				// bulletproof it.
			}

			// light the top stem inside the cap with glowshroom.
			if (theBlock == Blocks.RED_MUSHROOM) {
				for (int y = 9; y > 3; y--) {
					Block b = sWorld.getBlockState(ePos.up(y)).getBlock();
					if (b == Blocks.MUSHROOM_STEM) {
						sWorld.setBlockState(ePos.up(y), Blocks.SHROOMLIGHT.getDefaultState());
						break;
					}
				}
			}

			Utility.debugMsg(1, ePos, key + " grow mushroom.");
		}

	}

	private static long helperLongRandomSeed(BlockPos ePos) {
		return (long) Math.abs(ePos.getX() * 31) + Math.abs(ePos.getZ() * 11) + Math.abs(ePos.getY() * 7);
	}

	// this routine returns a count of the searchBlock immediately orthogonal to
	// BlockPos, exiting if a max count is exceeded.
	public static int helperCountBlocksOrthogonalBB(Block searchBlock, int maxCount, World w, BlockPos bPos,
			int boundY) {
		return helperCountBlocksOrthogonalBB(searchBlock, maxCount, w, bPos, 0 - boundY, 0 + boundY);
	}

	public static int helperCountBlocksOrthogonalBB(Block searchBlock, int maxCount, World w, BlockPos bPos,
			int lowerBoundY, int upperBoundY) {
		int count = 0;
		for (int j = lowerBoundY; j <= upperBoundY; j++) {
			if (w.getBlockState(bPos.up(j).east()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.up(j).west()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.up(j).north()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.up(j).south()).getBlock() == searchBlock)
				count++;
			if (count >= maxCount)
				return count;
		}

		return count;

	}

	public static int helperCountBlocksBB(Block searchBlock, int maxCount, World w, BlockPos bPos, int boxSize) {
		return helperCountBlocksBB(searchBlock, maxCount, w, bPos, boxSize, boxSize); // "square" box subcase
	}

	public static int helperCountBlocksBB(Block searchBlock, int maxCount, World w, BlockPos bPos, int boxSize,
			int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;


		Mutable mPos = new Mutable();
		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
				for (int dy = minY; dy <= maxY; dy++) {
					mPos.set(dx, dy, dz);
					if (w.getBlockState(mPos).getBlock() == searchBlock) {
						if (++count >= maxCount)
							return count;
					}
				}
			}
		}

		Utility.debugMsg(1, bPos,
				Utility.getResourceLocationString(searchBlock) + " Sparse count:" + count + " countBlockBB ");

		return count;
	}

	public static int helperCountBlocksBB(Class<? extends Block> searchBlock, int maxCount, World w, BlockPos bPos,
			int boxSize) {
		return helperCountBlocksBB(searchBlock, maxCount, w, bPos, boxSize, 0);
	}

	public static int helperCountBlocksBB(Class<? extends Block> searchBlock, int maxCount, World w, BlockPos bPos,
			int boxSize, int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;

		Mutable mPos = new Mutable();

		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
				for (int dy = minY; dy <= maxY; dy++) {
					mPos.set(dx, dy, dz);
					if (searchBlock.isInstance(w.getBlockState(mPos).getBlock())) {
						if (++count >= maxCount) {
							return count;
						}
					}
				}
			}
		}

		Utility.debugMsg(1, bPos, searchBlock.getSimpleName() + " Sparse count:" + count + " countBlockBB ");

		return count;
	}

	private static boolean isGoodMushroomTemperature(Entity entity) {
		BlockPos ePos = getAdjustedBlockPos(entity);
		float biomeTemp = entity.world.getBiome(ePos).value().getTemperature();
		Utility.debugMsg(1, ePos, "Mushroom Biome temp: " + biomeTemp + ".");
		if (biomeTemp < MyConfig.getMushroomMinTemp())
			return false;
		if (biomeTemp > MyConfig.getMushroomMaxTemp())
			return false;
		return true;
	}

	private static boolean mobEatPlantsAction(Entity entity, String key, String regrowthType) {
		if (mobEatGrassOrFlower(entity, regrowthType)) {
			Utility.debugMsg(1, getAdjustedBlockPos(entity), key + " ate plants.");
			return true;
		}
		return false;
	}

	private static boolean isHorseTypeEatingNow(Entity entity) {
		if (entity instanceof HorseBaseEntity) {
			HorseBaseEntity h = (HorseBaseEntity) entity;
			if (h.isEatingGrass()) {
				return true;
			}
		}
		return false;
	}

	private static void mobStumbleAction(Entity entity, String key) {
		entity.world.breakBlock(getAdjustedBlockPos(entity), true);
		Utility.debugMsg(1, getAdjustedBlockPos(entity), key + " stumbled over torch.");
	}

	private static void doVillagerRegrowthEvents(VillagerEntity ve, String debugKey, String regrowthActions) {

		// Villagers hopping, falling, etc. are doing improvements.
		if (!(ve.isOnGround()))
			return;
		if (groundBlockState.getBlock() instanceof TorchBlock)
			return;
		if (groundBlockState.getBlock() instanceof WallTorchBlock)
			return;

		// Give custom debugging names to nameless villagers.
		if (MyConfig.getDebugLevel() > 0) {
			Text tName = new LiteralText("");
			float veYaw = ve.getYaw(1.0f);
			tName = new LiteralText("Reg-" + ve.getX() + "," + ve.getZ() + ": " + veYaw);
			ve.setCustomName(tName);
		} else { // remove custom debugging names added by Regrowth
			if (ve.getCustomName() != null) {
				if (ve.getCustomName().toString().contains("Reg-")) {
					ve.setCustomName(null);
				}
			}
		}

		// note all villagers may not have a home. poor homeless villagers.
		// default = repair farms
		if (vImproveFarm(ve, regrowthActions)) {
			Utility.debugMsg(1, ve.getBlockPos(), debugKey + " farm improved.");
		}
		;

		// 'h'eal villagers and players
		if (regrowthActions.contains("h")) {
			vClericalHealing(ve);
			vToolMasterHealing(ve);
		}

		vBeeKeeperFlowers(ve);

		// cut lea'v'es.
		// remove leaves if facing head height leaves

		if (regrowthActions.contains("v")) {
			vImproveLeaves(ve, debugKey);
		}

		// c = cut down grass (but not flowers for now)
		// to do - maybe remove flowers unless by a road or elevated (air next to them
		// as in the flower beds)
		// to do - replace "c" with a meaningful constant.

		if (regrowthActions.contains("c")) {
			if ((footBlock == Blocks.TALL_GRASS) || (footBlock instanceof TallPlantBlock)
					|| (footBlock.getTranslationKey().equals("block.byg.short_grass"))) {

				ve.world.breakBlock(ve.getBlockPos(), false);
				Utility.debugMsg(1, ve.getBlockPos(), debugKey + " grass cut.");
			}
		}
		// improve roads
		// to do - replace "r" with a meaningful constant.f
		if (regrowthActions.contains("r")) {
			Utility.debugMsg(1, ve.getBlockPos(), debugKey + " try road improve.");
			vImproveRoads(ve, debugKey);
		}

		// note villages may not have a meeting place. Sometimes they change. Sometimes
		// they take a few minutes to form.
		if ((regrowthActions.contains("w"))) {
			Utility.debugMsg(1, ve.getBlockPos(), " try town wall build.");
			vImproveWalls(ve, debugKey, regrowthActions);

		}

		if ((regrowthActions.contains("p"))) {
			Utility.debugMsg(1, ve.getBlockPos(), " try personal fence build.");
			vImproveFences(ve, debugKey, regrowthActions);

		}

		if ((regrowthActions.contains("t") && (footBlock != Blocks.TORCH))) {
			if (vImproveLighting(ve)) {
				Utility.debugMsg(1, ve.getBlockPos(), debugKey + "-" + footBlock + ", " + groundBlock + " pitch: "
						+ ve.getHeadYaw() + " lighting improved.");
			}
		}
	}

	private static void helperJumpAway(Entity e) {
		// "jump" villagers away if they are inside a wall, fence, or dirtPath block.
		Block postActionFootBlock = getAdjustedFootBlockState(e).getBlock();
		if (postActionFootBlock == Blocks.DIRT_PATH) {
			e.setVelocity(0, 0.33, 0);
			return;
		}
		if ((postActionFootBlock instanceof WallBlock) || (postActionFootBlock instanceof FenceBlock)) {
			// TODO how is getYaw(1.0f) different than getHeadYaw()
			float veYaw = e.getYaw(1.0f) / 45;
			int facingNdx = Math.round(veYaw);
			if (facingNdx < 0) {
				facingNdx = Math.abs(facingNdx);
			}
			facingNdx %= 8;
			double dx = (facingArray[facingNdx][0]) / 2.0;
			double dz = (facingArray[facingNdx][1]) / 2.0;
			e.setVelocity(dx, 0.55, dz);
		}
	}

	private static boolean mobEatGrassOrFlower(Entity entity, String regrowthType) {

		BlockPos ePos = getAdjustedBlockPos(entity);
		if (!(isGrassOrFlower(footBlockState))) {
			return false;
		}
		if (isKindOfGrassBlock(groundBlockState)) {
			mobTrodGrassBlock(entity);
		}
		entity.world.breakBlock(ePos, false);
		LivingEntity le = (LivingEntity) entity;
		helperChildAgeEntity(entity);
		if (le.getMaxHealth() > le.getHealth() && (MyConfig.getEatingHeals())) {
			StatusEffectInstance ei = new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 1, 0, false, true);
			le.addStatusEffect(ei);
		}
		return true;
	}

	private static void mobTrodGrassBlock(Entity e) {

		BlockPos ePos = new BlockPos(e.getX(), e.getY(), e.getZ());
		if (e.world instanceof ServerWorld world) {
			Box box = new Box(ePos.east(2).up(2).north(2), ePos.west(2).down(2).south(2));
			List<?> entityList = world.getEntitiesByType(e.getType(), box, (entity) -> true);
			if (entityList.size() >= 9) {
				world.setBlockState(ePos.down(), Blocks.DIRT_PATH.getDefaultState());
				e.damage(DamageSource.IN_WALL, 0.25f);
			}
		}

	}

	private static boolean isBlockGrassPathOrDirt(Block tempBlock) {

		if ((tempBlock == Blocks.DIRT_PATH) || (tempBlock == Blocks.DIRT)) {
			return true;
		}
		return false;
	}

	private static void helperChildAgeEntity(Entity ent) {
		if (ent.age < 0) {
			ent.age += 30;
		}
	}

	private static boolean mobGrowTallAction(Entity ent, String key) {
		if (footBlock == Blocks.GRASS) {
			BlockPos ePos = getAdjustedBlockPos(ent);
			Fertilizable ib = (Fertilizable) footBlock;
			try {
				ib.grow((ServerWorld) ent.world, ent.world.random, ePos, ent.world.getBlockState(ePos));
				Utility.debugMsg(1, ePos, key + " grew and hid in tall plant.");
			} catch (Exception e) {
				Utility.debugMsg(1, ent.getBlockPos(), key + " caught grow attempt exception.");
			}
			return true;
		}
		return false;
	}

	private static BlockState helperBiomeRoadBlockType(Biome localBiome) {

		BlockState gateBlockType = Blocks.DIRT_PATH.getDefaultState();

		if (biomeCategory == Biome.Category.DESERT) {
			gateBlockType = Blocks.ACACIA_PLANKS.getDefaultState(); // 16.1 mojang change
		}
		return gateBlockType;
	}

	// if a grassblock in village has farmland next to it on the same level- retill
	// it.
	// todo add hydration check before tilling land.
	private static boolean vImproveFarm(VillagerEntity ve, String regrowthType) {
		if (ve.getVillagerData().getProfession() != VillagerProfession.FARMER) {
			return false;
		}

		BlockPos vePos = getAdjustedBlockPos(ve);
		Block groundBlock = groundBlockState.getBlock();
		Block footBlock = footBlockState.getBlock();

		if (helperCountBlocksOrthogonalBB(Blocks.FARMLAND, 1, ve.world, vePos.down(1), 0) > 0) {
			if (isNearWater(ve.world, vePos.down(1))) {
				if (groundBlock instanceof GrassBlock) {
					ve.world.setBlockState(vePos.down(), Blocks.FARMLAND.getDefaultState());
					return true;
				}
			}

			if (!regrowthType.contains("t") || (footBlock != Blocks.AIR)) {
				return false;
			}

			// Special farm lighting torches.
			if (ve.world.getLightLevel(LightType.BLOCK, vePos) > 12) {
				return false; // block already bright enough
			}

			int veX = vePos.getX();
			int veY = vePos.getY();
			int veZ = vePos.getZ();

			if ((lastTorchX == veX) && (lastTorchY == veY) && (lastTorchZ == veZ)) {
				return false; // Anti torch-exploit
			}

			boolean placeTorch = false;
			int waterValue = helperCountBlocksOrthogonalBB(Blocks.WATER, 1, ve.world, vePos.down(), 0);
			if ((waterValue > 0) && (groundBlockState.isIn(BlockTags.LOGS))
					|| (groundBlock == Blocks.SMOOTH_SANDSTONE)) {
				ve.world.setBlockState(vePos, Blocks.TORCH.getDefaultState());
				lastTorchX = veX;
				lastTorchY = veY;
				lastTorchZ = veZ;
				return true;
			}
		}
		return false;
	}

	private static void vBeeKeeperFlowers(VillagerEntity ve) {
		if (!ve.getVillagerData().getProfession().getId().contains("beekeeper")) {
			return;
		}
		if ((ve.getX() % 6 == 0) && (ve.getZ() % 7 == 0)) {
			if (isBlockGrassOrDirt(groundBlockState)) {
				if (helperCountBlocksOrthogonalBB(Blocks.DIRT_PATH, 1, ve.world, ve.getBlockPos().down(), 0) == 1) {
					BlockState flowerBlockState = Blocks.AZURE_BLUET.getDefaultState();
					ve.world.setBlockState(adjustedPos, flowerBlockState);
				}
			}
		}
	}

	private static void vToolMasterHealing(VillagerEntity ve) {

		if (ve.getVillagerData().getProfession() != VillagerProfession.TOOLSMITH) {
			return;
		}
		long daytime = ve.world.getTimeOfDay() % 24000;

		if (daytime < 9000 || daytime > 11000) {
			return;
		}

		if (ve.world instanceof ServerWorld varW) {
			int villagerLevel = ve.getVillagerData().getLevel();
			if (villagerLevel < 1)
				return;
			BlockPos vePos = new BlockPos(ve.getX(), (ve.getY() + 0.99d), (ve.getZ()));
			Box box = new Box(vePos.east(6).up(3).north(6), vePos.west(6).down(2).south(6));

			List<IronGolemEntity> l = varW.getEntitiesByType(EntityType.IRON_GOLEM, box, e -> true);

			for (IronGolemEntity e : l) {
				boolean heal = true;

				if (e.getHealth() >= e.getMaxHealth()) {
					heal = false;
				}
				if (e.getStatusEffect(StatusEffects.REGENERATION) != null) {
					heal = false;
				}
				if (heal) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, villagerLevel * 51, 0), ve);
					ve.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, villagerLevel * 11, 0), ve);
					ve.world.playSound(null, vePos, SoundEvents.ENTITY_VILLAGER_WORK_TOOLSMITH, SoundCategory.NEUTRAL,
							0.5f, 0.5f);
					return;
				}
			}
		}
	}

	private static void vClericalHealing(VillagerEntity ve) {

		if (ve.getVillagerData().getProfession() != VillagerProfession.CLERIC) {
			return;
		}
		long daytime = ve.world.getTimeOfDay() % 24000;

		if (daytime < 9000 || daytime > 11000) {
			return;
		}
		if (ve.world instanceof ServerWorld varW) {
			int clericalLevel = ve.getVillagerData().getLevel();

			BlockPos vePos = new BlockPos(ve.getX(), (ve.getY() + 0.99d), (ve.getZ()));
			Box box = new Box(vePos.east(4).up(2).north(4), vePos.west(4).down(2).south(4));

			List<Entity> l = varW.getOtherEntities(null, box, (e) -> {
				return ((e instanceof VillagerEntity) || (e instanceof PlayerEntity));
			});

			for (Entity e : l) {
				boolean heal = true;
				LivingEntity le = (LivingEntity) e;
				if (le.getHealth() >= le.getMaxHealth()) {
					heal = false;
				}
				if (le.getStatusEffect(StatusEffects.REGENERATION) != null) {
					heal = false;
				}
				if (e instanceof PlayerEntity pe) {
					int rep = ve.getReputation(pe);
					if (rep < 0) { // I was a bad bad boy.
						heal = false;
					}
				}
				if (heal) {
					le.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, clericalLevel * 51, 0), ve);
					ve.world.playSound(null, vePos, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.NEUTRAL,
							1.2f, 1.52f);
					return;
				}

			}
		}
	}

	private static void vImproveLeaves(VillagerEntity ve, String key) {



		// May just need to get headYaw or now.
		float veYaw = ve.getYaw(1.0f) / 45;

		BlockPos vePos = getAdjustedBlockPos(ve);
		int facingNdx = Math.round(veYaw);
		if (facingNdx < 0) {
			facingNdx = Math.abs(facingNdx);
		}
		facingNdx %= 8;


		int dx = facingArray[facingNdx][0];
		int dz = facingArray[facingNdx][1];

		BlockPos tmpBP = null;
		BlockState tempBS = null;
		Block tempBlock = null;
		boolean destroyBlock = false;

		for (int iY = 0; iY < 2; iY++) {
			tmpBP = new BlockPos(vePos.getX() + dx, vePos.getY() + iY, vePos.getZ() + dz);
			tempBS = ve.world.getBlockState(tmpBP);
			tempBlock = tempBS.getBlock();
			if (tempBlock instanceof LeavesBlock) {
				boolean persistantLeaves = tempBS.get(LeavesBlock.PERSISTENT);
				if (!(persistantLeaves)) {
					destroyBlock = true;
				}
			}
			if ((tempBlock instanceof CactusBlock)) {
				destroyBlock = true;
			}
			if (destroyBlock) {
				ve.world.breakBlock(tmpBP, false);
				destroyBlock = false;
				Utility.debugMsg(1, vePos, key + " cleared " + tempBlock.getTranslationKey().toString());
			}
		}
	}

	private static boolean vImproveLighting(VillagerEntity ve) {
		BlockPos vePos = getAdjustedBlockPos(ve);

		int blockLightValue = ve.world.getLightLevel(LightType.BLOCK, vePos);
		int skyLightValue = ve.world.getLightLevel(LightType.SKY, vePos);

		if (blockLightValue > MyConfig.getTorchLightLevel())
			return false;
		if (skyLightValue > 11)
			return false;

		if (ve.isSleeping()) {
			return false;
		}
		if (footBlockState.getBlock() instanceof BedBlock) {
			return false;
		}

		if (isValidGroundBlockToPlaceTorchOn(ve) && (footBlockState.isAir())||isNatProgPebbleOrStick()) {
			ve.world.setBlockState(vePos, Blocks.TORCH.getDefaultState(), Block.NOTIFY_ALL);
		}

		return true;

	}

	private static void vImproveRoads(VillagerEntity ve, String debugkey) {

		Text tName = new LiteralText("-");  // used for debugging.
//		ve.setCustomName(tName);

		isRoadPiece = false;

		boolean isInsideStructurePiece = false;
		boolean test = true;
		BlockPos piecePos = null;
		List<StructureStart> sList = new ArrayList<StructureStart>();
		if (test) {
			ChunkPos c = new ChunkPos(ve.getBlockPos());
			sList = getStarts(ve.world, StructureFeature.VILLAGE, c.x, c.z);
		}
		if (!sList.isEmpty()) {

			for (StructurePiece piece : sList.get(0).getChildren()) {
				piecePos = piece.getCenter();

				if (piece.getBoundingBox().contains(ve.getBlockPos())) {
					piecePos = piece.getCenter();
					// System.out.println("inside" + piece);
					if (piece.toString().contains("streets")) {
						isRoadPiece = true;
					}
					int i = piece.toString().indexOf("minecraft");
					if (i >= 0) {
						tName = new LiteralText(isRoadPiece + " " + piece.toString().substring(i));
					} else {
						i = piece.toString().indexOf("minecraft");
						if (i >= 0)
							tName = new LiteralText(isRoadPiece + " " + piece.toString().substring(i));

					}
//					ve.setCustomName(tName);
					isInsideStructurePiece = true;
					break;
				}
			}
		}

		if (vImproveRoadsClearSnow(ve)) {
			Utility.debugMsg(1, ve.getBlockPos(), debugkey + " clear snow on road.");
		}

		if (vImproveRoadsFixPatches(ve)) {
			Utility.debugMsg(1, ve.getBlockPos(), debugkey + " fix patches on road.");
		}
		if (vImproveRoadsFixPotholes(ve)) {
			Utility.debugMsg(1, ve.getBlockPos(), debugkey + " fix potholes in road.");
		}
		if (vImproveRoadsSmoothHeight(ve)) {
			Utility.debugMsg(1, ve.getBlockPos(), debugkey + " Smooth road slope.");
		}
	}

	private static List<StructureStart> getStarts(World worldIn, StructureFeature<?> struct, int x, int z) {
		List<StructureStart> list = Lists.newArrayList();
		Chunk ichunk = worldIn.getChunk(x, z, ChunkStatus.STRUCTURE_REFERENCES);

		for (Entry<ConfiguredStructureFeature<?, ?>, LongSet> r : ichunk.getStructureReferences().entrySet()) {
			if (r.getKey().feature == struct) {
				LongIterator longiterator = r.getValue().iterator();
				while (longiterator.hasNext()) {
					long i = longiterator.nextLong();

					Chunk istructurereader = worldIn.getChunk(ChunkPos.getPackedX(i), ChunkPos.getPackedZ(i),
							ChunkStatus.STRUCTURE_STARTS);
					StructureStart structurestart = istructurereader.getStructureStart(r.getKey());
					if (structurestart != null)
						list.add(structurestart);
				}
			}
		}

		return list;
	}

	// Clear generated snow off of roads. Naturally falling snow doesn't stick on
	// roads.
	private static boolean vImproveRoadsClearSnow(Entity e) {
		Block biomeRoadBlock = helperBiomeRoadBlockType(localBiome).getBlock();
		if (groundBlock != biomeRoadBlock) {
			return false;
		}
		if (footBlock == Blocks.SNOW) {
			e.world.breakBlock(adjustedPos, false);
			footBlockState = Blocks.AIR.getDefaultState();
			footBlock = footBlockState.getBlock();
			return true;
		}
		return false;
	}

	// fix unfinished spots in road with 3-4 grass blocks orthogonal to them.
	// on slopes too.

	private static boolean vImproveRoadsFixPatches(Entity e) {

		if (!e.world.isSkyVisible(e.getBlockPos())) {
			return false;
		}

		Block biomeRoadBlock = helperBiomeRoadBlockType(localBiome).getBlock();

		if (groundBlock == biomeRoadBlock)
			return false;

		int roadY = 0;
		int roadBlocks = 0;
		BlockPos vePos = getAdjustedBlockPos(e);
		for (int i = 0; i < 4; i++) {
			// TODO look in world to determine correct method.
			roadY = e.world.getTopY(Type.MOTION_BLOCKING_NO_LEAVES, vePos.getX() + dx[i], vePos.getZ() + dz[i]) - 1;
			Block tempBlock = e.world.getBlockState(new BlockPos(vePos.getX() + dx[i], roadY, vePos.getZ() + dz[i]))
					.getBlock();
			if (tempBlock == biomeRoadBlock) {
				roadBlocks++;
				if (roadBlocks >= 3) {
					if (footBlock instanceof SnowBlock) {
						e.world.breakBlock(adjustedPos, false);
						footBlockState = Blocks.AIR.getDefaultState();
						footBlock = footBlockState.getBlock();
					}
					e.world.setBlockState(adjustedPos.down(), biomeRoadBlock.getDefaultState());
//					System.out.println( " rb:" + roadBlocks);
					return true;
				}
			}
		}
//		if (roadBlocks > 0) 
//		System.out.println( " rb:" + roadBlocks);
		return false;
	}

	// fix unfinished spots in road with 3-4 grass blocks orthogonal to them.
	// on slopes too.

	private static boolean vImproveRoadsFixPotholes(Entity e) {

		if (!e.world.isSkyVisible(e.getBlockPos())) {
			return false;
		}

		Block biomeRoadBlock = helperBiomeRoadBlockType(localBiome).getBlock();
		if ((groundBlock == biomeRoadBlock) && (footBlock instanceof SnowBlock)) {
			e.world.breakBlock(getAdjustedBlockPos(e), false);
		}

		BlockPos vePos = e.getBlockPos();

		int veX = vePos.getX();
		int veY = vePos.getY();
		int veZ = vePos.getZ();

		int roadY = 0;
		int higherRoadBlocks = 0;
		for (int i = 0; i < 4; i++) {
			// TODO Find correct method for these parms
			roadY = e.world.getTopY(Type.MOTION_BLOCKING_NO_LEAVES, veX + dx[i], veZ + dz[i]) - 1;
			Block tempBlock = e.world.getBlockState(new BlockPos(veX + dx[i], roadY, veZ + dz[i])).getBlock();
			if (tempBlock == biomeRoadBlock) {
				if (roadY > veY) {
					higherRoadBlocks++;
				}
			}
		}
		if (higherRoadBlocks == 4) {
			e.world.setBlockState(adjustedPos, biomeRoadBlock.getDefaultState());
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private static boolean vImproveRoadsSmoothHeight(VillagerEntity ve) {

		if (!ve.isOnGround()) {
			return false;
		}

		if (ve.isBaby()) {
			return false;
		}

		BlockPos vePos = getAdjustedBlockPos(ve);

		if (!ve.world.isSkyVisible(ve.getBlockPos())) {
			return false;
		}

		Block biomeRoadBlock = helperBiomeRoadBlockType(localBiome).getBlock();

		if (biomeRoadBlock == Blocks.SMOOTH_SANDSTONE) {
			if (!isRoadPiece)
				return false;
		}

		if ((groundBlockState.getBlock() != biomeRoadBlock) && (footBlockState.getBlock() != biomeRoadBlock)) {
			return false;
		}

		BlockState biomeRoadBlockState = biomeRoadBlock.getDefaultState();

		// Check for nearby point of interests.
		int poiDistance = 3;
		if (Biome.getCategory(ve.world.getBiome(vePos)) == Biome.Category.DESERT) {
			poiDistance = 7;
		}

		if (isNearbyPoi(ve, localBiome, vePos, poiDistance)) {
			return false;
		}

		// Check for higher block to smooth up towards
		int veX = vePos.getX();
		int veY = vePos.getY();
		int veZ = vePos.getZ();
		int roadY = 0;

		for (int i = 0; i < 4; i++) {
			roadY = ve.world.getTopY(Type.MOTION_BLOCKING_NO_LEAVES, veX + dx[i], veZ + dz[i]) - 1;
			if (roadY > veY) {
				Block tempBlock = ve.world.getBlockState(new BlockPos(veX + dx[i], roadY, veZ + dz[i])).getBlock();
				if (tempBlock == biomeRoadBlock) {
					ve.world.setBlockState(new BlockPos(veX, veY, veZ), biomeRoadBlockState);
					if (ve.world.getBlockState(vePos.up(2)).getBlock() == biomeRoadBlockState.getBlock()) {
						ve.world.breakBlock(vePos.up(2), true);
					} else {
						if (ve.world.getBlockState(vePos.up(3)).getBlock() == biomeRoadBlockState.getBlock()) {
							ve.world.breakBlock(vePos.up(3), true);
						}
					}
					ve.setVelocity(0.0, 0.4, 0.0);
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isNearbyPoi(VillagerEntity ve, Biome localBiome, BlockPos vePos, int poiDistance) {

		// 08/30/20 Collection pre 16.2 bug returns non empty collections.
		// the collection is not empty when it should be.
		// are returned in the collection so have to loop thru it manually.
		// possible options:

		Collection<PointOfInterest> result = ((ServerWorld) ve.world).getPointOfInterestStorage()
				.getInSquare(t -> true, ve.getBlockPos(), poiDistance, OccupationStatus.ANY)
				.collect(Collectors.toCollection(ArrayList::new));

		if (!(result.isEmpty())) {
			Iterator<PointOfInterest> i = result.iterator();
			while (i.hasNext()) { // in 16.1, finds the point of interest.
				PointOfInterest p = i.next();
				int disX = Math.abs(ve.getBlockPos().getX() - p.getPos().getX());
				int disZ = Math.abs(ve.getBlockPos().getZ() - p.getPos().getZ());
				if ((disX < poiDistance) && (disZ < poiDistance)) {
					Utility.debugMsg(1, vePos, "Point of Interest too Close: " + p.getType().toString() + ".");
					return true;
				}
			}
		}
		return false;
	}

	private static boolean vImproveVillageWall(VillagerEntity ve, String regrowthActions) {
		if (!(ve.getBrain().getOptionalMemory(MemoryModuleType.MEETING_POINT)).isPresent())
			return false;

		if (!isOkayToBuildWallHere(ve)) {
			return false;
		}

		BlockPos gVMPPos = ve.getBrain().getOptionalMemory(MemoryModuleType.MEETING_POINT).get().getPos();

		if (MyConfig.getPlayerWallControlBlock() != Blocks.AIR) {
			if (ve.world.getChunk(gVMPPos).getInhabitedTime() < 200) // Bell
				ve.world.setBlockState(gVMPPos.up(1), MyConfig.getPlayerWallControlBlock().getDefaultState());

			if (ve.world.getBlockState(gVMPPos.up(1)).getBlock() != MyConfig.playerWallControlBlock) {
				return false;
			}
		}

		BlockPos vePos = getAdjustedBlockPos(ve);

		String key = "minecraft:" + biomeCategory.toString();
		key = key.toLowerCase();
		Utility.debugMsg(1, vePos, key + " wall improvement.");
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		Utility.debugMsg(1, vePos, key + " biome for wall improvement. ");

		int wallRadius = currentWallBiomeDataItem.getWallDiameter();

		wallRadius = (wallRadius / 2) - 1;

		if (isOnWallRadius(ve, wallRadius, gVMPPos)) {
			Utility.debugMsg(1, ve.getBlockPos(), "villager on wall perimeter: " + wallRadius);
			// check for other meeting place bells blocking wall since too close.
			Collection<PointOfInterest> result = ((ServerWorld) ve.world).getPointOfInterestStorage()
					.getInSquare(t -> t == PointOfInterestType.MEETING, ve.getBlockPos(), 41, OccupationStatus.ANY)
					.collect(Collectors.toCollection(ArrayList::new));

			// 08/30/20 Collection had bug with range that I couldn't resolve.
			boolean buildWall = true;
			if (!(result.isEmpty())) {
				Iterator<PointOfInterest> i = result.iterator();
				while (i.hasNext()) { // in 16.1, finds the point of interest.
					PointOfInterest P = i.next();
					if ((gVMPPos.getX() == P.getPos().getX()) && (gVMPPos.getY() == P.getPos().getY())
							&& (gVMPPos.getZ() == P.getPos().getZ())) {
						continue; // ignore meeting place that owns this wall segment.
					} else {
						int disX = Math.abs(ve.getBlockPos().getX() - P.getPos().getX());
						int disZ = Math.abs(ve.getBlockPos().getZ() - P.getPos().getZ());
						if ((disX < wallRadius) && (disZ < wallRadius)) {
							buildWall = false; // another meeting place too close. cancel wall.
							break;
						}
					}
				}

			}

			if (buildWall) {
				BlockState wallTypeBlockState = currentWallBiomeDataItem.getWallBlockState();

				BlockState wallBlock = wallTypeBlockState;

				int wallTorchSpacing = (wallRadius + 1) / 4;
				if (helperPlaceOneWallPiece(ve, wallRadius, wallTorchSpacing, wallBlock, gVMPPos)) {
					if (regrowthActions.contains("t")) {
						if (isValidTorchLocation(wallRadius, wallTorchSpacing, getAbsVX(ve, gVMPPos), getAbsVZ(ve, gVMPPos),
								ve.world.getBlockState(vePos).getBlock())) {
							ve.world.setBlockState(vePos.up(), Blocks.TORCH.getDefaultState(),Block.NOTIFY_ALL);
						}
					}
					helperJumpAway(ve);
					return true;
				}
			}
		}

		return false;

	}

	// villagers build protective walls around their homes. currently 32 out.
	// to do- reduce distance of wall from home.
	private static boolean vImproveHomeFence(VillagerEntity ve, BlockPos vHomePos, String regrowthActions) {

		BlockPos vePos = getAdjustedBlockPos(ve);
		String key = "minecraft:" + biomeCategory.toString();
//		ResourceLocation biomeName = ForgeRegistries.BIOMES.getKey(localBiome);
		key = key.toLowerCase();
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		if (currentWallBiomeDataItem == null) {

			key = "minecraft:" + biomeCategory.toString().toLowerCase();
			currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem(key);
			if (currentWallBiomeDataItem == null) {
				currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem("minecraft:plains");
			}
		}

		int homeFenceDiameter = currentWallBiomeDataItem.getWallDiameter();
		homeFenceDiameter = homeFenceDiameter / 4; // resize for personal home fence.

		int wallTorchSpacing = homeFenceDiameter / 4;
		homeFenceDiameter = (homeFenceDiameter / 2) - 1;

		int absvx = (int) Math.abs(ve.getX() - vHomePos.getX());
		int absvz = (int) Math.abs(ve.getZ() - vHomePos.getZ());

		Collection<PointOfInterest> result = ((ServerWorld) ve.world).getPointOfInterestStorage()
				.getInSquare(t -> t == PointOfInterestType.HOME, vePos, 17, OccupationStatus.ANY)
				.collect(Collectors.toCollection(ArrayList::new));

		// 08/30/20 Collection had bug with range that I couldn't resolve.
		boolean buildFence = true;
		if (!(result.isEmpty())) {
			Iterator<PointOfInterest> i = result.iterator();
			while (i.hasNext()) { // in 16.1, finds the point of interest.
				PointOfInterest P = i.next();
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



			if (helperPlaceOneWallPiece(ve, homeFenceDiameter, wallTorchSpacing, fenceBlockState,
					vHomePos)) {

				if (regrowthActions.contains("t")) {
					if (isValidTorchLocation(homeFenceDiameter, wallTorchSpacing, absvx, absvz,
							ve.world.getBlockState(vePos).getBlock())) {
						ve.world.setBlockState(vePos.up(), Blocks.TORCH.getDefaultState());
					}
				}
				helperJumpAway(ve);
				return true;
			}
		}

		return false;
	}

	private static void vImproveWalls(VillagerEntity ve, String key, String regrowthType) {

		if (groundBlockState.isAir()) {
			return; // ignore edge cases where villager is hanging on the edge of a block.
		}
		BlockPos vePos = getAdjustedBlockPos(ve);

		if (!(ve.getBrain().getOptionalMemory(MemoryModuleType.MEETING_POINT)).isPresent())
			return;

		Utility.debugMsg(1, vePos, "Checking Improve Wall.");
		if (vImproveVillageWall(ve, regrowthType)) {
			Utility.debugMsg(1, vePos, "Meeting Wall Improved.");
		}
	}

	private static void vImproveFences(VillagerEntity ve, String key, String regrowthType) {

		BlockPos ePos = ve.getBlockPos();

		Brain<VillagerEntity> vb = ve.getBrain();
		Optional<GlobalPos> vMeetingPlace = vb.getOptionalMemory(MemoryModuleType.MEETING_POINT);
		if (!(vMeetingPlace.isPresent())) {
			return;
		}

		if (isOkayToBuildWallHere(ve)) {
			GlobalPos gVMP = vMeetingPlace.get();
			BlockPos villageMeetingPlaceBlockPos = gVMP.getPos();

			if (!(ve.world.getBlockState(villageMeetingPlaceBlockPos.up(1)).getBlock() instanceof WallBlock)) {
				return;
			}

			// build a wall on perimeter of villager's home
			if (regrowthType.contains("p")) {
				Utility.debugMsg(1, ePos, "Checking Improve Fence.");

				Optional<GlobalPos> villagerHome = vb.getOptionalMemory(MemoryModuleType.HOME);
				if (!(villagerHome.isPresent())) {
					return;
				}
				GlobalPos gVHP = villagerHome.get();
				BlockPos villagerHomePos = gVHP.getPos();
				// don't build personal walls inside the village wall perimeter.
				// don't build personal walls until the village has a meeting place.
				if (isOutsideMeetingPlaceWall(ve, vMeetingPlace, vMeetingPlace.get().getPos(), localBiome)) {
					Utility.debugMsg(1, ePos, "Outside meeting place wall.");
					if (vImproveHomeFence(ve, villagerHomePos, regrowthType)) {
						Utility.debugMsg(1, ePos, "Home Fence Improved.");
					}
				}
			}
		}
	}

	private static boolean isFootBlockOkayToBuildIn(BlockState footBlockState) {
		if ((footBlockState.isAir()) || (isGrassOrFlower(footBlockState))) {
			return true;
		}
		if (footBlockState.getBlock() instanceof SnowBlock) {
			return true;
		}
		if (isNatProgPebbleOrStick())
			return true;

		return false;
	}

	// handle natural progression pebbles and sticks
	private static boolean isNatProgPebbleOrStick() {

		String rl = Utility.getResourceLocationString(footBlock);

		if ((rl.contains("natprog")) && (rl.contains("pebble")))
			return true;

		if ((rl.equals("natprog")) && (rl.contains("twigs")))
			return true;
		
		if ((rl.equals("minecraft")) && (rl.contains("button"))) {
			return true;
		}
		
		return false;

	}

	private static boolean isGrassOrFlower(BlockState footBlockState) {
		Block footBlock = footBlockState.getBlock();

		if (footBlock instanceof TallFlowerBlock) {
			return true;
		}
		if (footBlock instanceof FlowerBlock) {
			return true;
		}
		if (footBlock instanceof TallPlantBlock) {
			return true;
		}
		if (footBlock instanceof FernBlock) {
			return true;
		}

		// compatibility with other biome mods.
		try {
			if (footBlockState.isIn(BlockTags.FLOWERS)) {
				return true;
			}
			if (footBlockState.isIn(BlockTags.TALL_FLOWERS)) {
				return true;
			}
		} catch (Exception e) {
			if (MyConfig.getDebugLevel() > 0) {
				System.out.println("Tag Exception 1009-1014:" + footBlock.getTranslationKey() + ".");
			}
		}
		// biomes you'll go grass compatibility
		if (footBlock.getTranslationKey().equals("block.byg.short_grass")) {
			return true;
		}
		if (MyConfig.getDebugLevel() > 0) {
			System.out.println("Not grass or Flower:" + footBlock.getTranslationKey() + ".");
		}
		return false;
	}

	private static boolean isImpossibleRegrowthEvent(String regrowthType) {
		if ((regrowthType.equals("eat")) && (footBlockState.isAir())) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlockState.getBlock() instanceof TallPlantBlock)) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlockState.getBlock() instanceof FlowerBlock)) {
			return true;
		}
		if ((regrowthType.equals("tall")) && ((footBlockState.getBlock() instanceof TallPlantBlock))) {
			return true;
		}

		return false;
	}

	private static boolean isOkayToBuildWallHere(VillagerEntity ve) {

		boolean okayToBuildWalls = true;

		if (!(isOnGround(ve))) {
			okayToBuildWalls = false;
		}
		if (!(isFootBlockOkayToBuildIn(footBlockState))) {
			okayToBuildWalls = false;
		}
		if (!(isValidGroundBlockToBuildWallOn(ve))) {
			okayToBuildWalls = false;
		}
		return okayToBuildWalls;
	}

	private static boolean isOnGround(Entity e) {
		return e.isOnGround();
	}



//	ItemStack iStk = new ItemStack(Items.BONE_MEAL,1);
//	BoneMealItem.applyBonemeal(iStk, e.world,e.getPosition());
// (likely 12.2 and 14.4 call?)	ib.performBonemeal((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));\

	private static boolean isOutsideMeetingPlaceWall(VillagerEntity ve, Optional<GlobalPos> vMeetingPlace,
			BlockPos meetingPlacePos, Biome localBiome) {

		BlockPos vePos = getAdjustedBlockPos(ve);
		String key = "minecraft:" + Utility.getBiomeCategory(localBiome).toString();
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

	private static boolean isValidGroundBlockToPlaceTorchOn(VillagerEntity ve) {

		String key = Utility.getResourceLocationString(groundBlockState);
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(key);
		if (currentWallFoundationItem == null)
			return false;

		return true;

	}

	private static boolean isValidGroundBlockToBuildWallOn(Entity e) {

		if (e.world.getLightLevel(LightType.SKY, e.getBlockPos()) < 13)
			return false;

		if (groundBlock instanceof SnowBlock)
			return false;
		if (groundBlock instanceof TorchBlock)
			return false; // includes WallTorchBlock

		if (e.world.getBlockState(e.getBlockPos().up()).getBlock() instanceof WallBlock) {
			return false;
		}
		if (e.world.getBlockState(e.getBlockPos().down()).getBlock() instanceof WallBlock) {
			return false;
		}
		if (e.world.getBlockState(e.getBlockPos().down(1)).getBlock() instanceof WallBlock) {
			return false;
		}
		if (e.world.getBlockState(e.getBlockPos().down(2)).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (e.world.getBlockState(e.getBlockPos().up()).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (e.world.getBlockState(e.getBlockPos().down()).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (e.world.getBlockState(e.getBlockPos().down(1)).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (e.world.getBlockState(e.getBlockPos().down(2)).getBlock() instanceof TorchBlock) {
			return false;
		}

		groundBlock = groundBlockState.getBlock();

		Utility.debugMsg(1, e.getBlockPos(), "Build Wall : gb" + Utility.getResourceLocationString(groundBlock)
				+ ", fb:" + Utility.getResourceLocationString(footBlock));
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(Utility.getResourceLocationString(groundBlock));

		if (currentWallFoundationItem == null)
			return false;

		return true;

	}

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

	private static boolean isOnWallRadius(Entity e, int wallRadius, BlockPos gVMPPos) {
		if ((getAbsVX(e, gVMPPos) == wallRadius) && (getAbsVZ(e, gVMPPos)<= wallRadius))
			return true;
		if ((getAbsVZ(e, gVMPPos) == wallRadius) && (getAbsVX(e, gVMPPos) <= wallRadius))
			return true;
		return false;
	}

	private static int getAbsVZ(Entity e, BlockPos gVMPPos) {
		return (int) Math.abs(getVZ(e, gVMPPos));
	}

	private static int getAbsVX(Entity e, BlockPos gVMPPos) {
		int absvx = (int) Math.abs(getVX(e, gVMPPos));
		return absvx;
	}

	private static int getVZ(Entity e, BlockPos gVMPPos) {
		return (int) (e.getZ() - gVMPPos.getZ());
	}

	private static int getVX(Entity e, BlockPos gVMPPos) {
		return (int) (e.getX() - gVMPPos.getX());
	}
	
	private static boolean helperPlaceOneWallPiece(Entity e, int wallRadius, int wallTorchSpacing, 
			BlockState wallType, BlockPos gVMPPos) {

		// Build East and West Walls (and corners)
		BlockState gateBlockState = Blocks.OAK_FENCE_GATE.getDefaultState().with(FACING, Direction.EAST).with(OPEN, true);
		Direction d = null;
		if (getAbsVX(e, gVMPPos) == wallRadius) {
			if (getAbsVZ(e, gVMPPos) <= wallRadius) {
				d = (getVX(e, gVMPPos) > 0) ? Direction.EAST : Direction.WEST;
				return helperPlaceWallPiece(e, wallType, gateBlockState.with(FACING, d), getVZ(e, gVMPPos) );
			}
		}
		// Build North and South Walls (and corners)
		if (getAbsVZ(e, gVMPPos) == wallRadius) {
			if (getAbsVX(e, gVMPPos) <= wallRadius) {
				d = (getVZ(e, gVMPPos) > 0) ? Direction.NORTH : Direction.SOUTH;
				return helperPlaceWallPiece(e, wallType, gateBlockState.with(FACING, d), getVX(e, gVMPPos));
			}
		}

		return false;
	}

	private static boolean helperPlaceWallPiece(Entity e, BlockState wallType, BlockState gateBlockType, int va) {

		BlockPos vePos = getAdjustedBlockPos(e);

		if (footBlock == Blocks.SNOW) {
			e.world.breakBlock(vePos, false);
		}

		if (isNatProgPebbleOrStick()) {
			e.world.breakBlock(vePos, true);
		}

		if ((footBlock instanceof SaplingBlock) || (footBlock == Blocks.GRASS) || (footBlock instanceof FlowerBlock)
				|| (footBlock instanceof TallPlantBlock)) {
			e.world.breakBlock(vePos, true);
		}
		
		int absva = Math.abs(va);
		if (absva == WALL_CENTER) {
			e.world.setBlockState(vePos, gateBlockType,3);
			return true;
		}

		if (e.world.setBlockState(vePos, wallType,3)) {
			return true;
		} else {
			Utility.debugMsg(1, e.getBlockPos(),
					"Building Wall Fail: SetBlockAndUpdate Time End = " + e.world.getTime());
			return false;
		}

	}

}
