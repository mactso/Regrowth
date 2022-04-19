package com.mactso.regrowth.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.core.jmx.Server;

//import org.antlr.v4.runtime.atn.BlockStartState;

import com.google.common.collect.Lists;
import com.mactso.regrowth.config.ModConfigs;
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
import net.minecraft.block.FenceBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.MushroomBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.SnowBlock;
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
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry.Reference;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.Difficulty;
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
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestStorage.OccupationStatus;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.storage.ChunkDataAccess;

public class MoveEntityEvent {

	private int[] dx = { 1, 0, -1, 0 };
	private int[] dz = { 0, 1, 0, -1 };
	private int TICKS_PER_SECOND = 20;
	private int[][] facingArray = { { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 }, { 0, -1 }, { 1, -1 }, { 1, 0 },
			{ 1, 1 } };
	private int lastTorchX = 0;
	private int lastTorchY = 0;
	private int lastTorchZ = 0;
	static final int WALL_CENTER = 0;
	static final int FENCE_CENTER = 0;
	static final int WALL_TYPE_WALL = -1;
	static final int WALL_TYPE_FENCE = -2;

	private BlockState footBlockState;
	private BlockState groundBlockState;
	private Block footBlock;
	private Block groundBlock;
	private Biome localBiome;
	private boolean isRoadPiece = false;

	private Category biomeCategory;
	BlockPos adjustedPos;

	// TODO Mixin for Farmland Trample Event
	public void handleTrampleEvents(FarmlandTrampleEvent event) {
		BlockPos pos = event.getEntity().getBlockPos();
		Utility.debugMsg(1, pos, "Enter FarmlandTrampleEvent");
		if (event.isCancelable()) {
			if (event.getEntity() instanceof VillagerEntity ve) {
				if (ve.getVillagerData().getProfession() != VillagerProfession.FARMER) {
					Utility.debugMsg(2, pos, "Villager Not A Farmer");
					return;
				}
				if (ve.getVillagerData().getLevel() >= 3) {
					event.setCanceled(true);
					Utility.debugMsg(2, pos, "Farmer under level 3.");
					return;
				}
			}
			if ((event.getEntity() instanceof ServerPlayerEntity spe)) {
				if (!spe.isCreative()) {
					return;
				}
				Utility.debugMsg(2, pos, "FarmlandTrampleCancelled");
				event.setCanceled(true);
			}
		}
	}

	// TODO Mixin for Living Update Event
	@SuppressWarnings("deprecation")
	public void handleEntityMoveEvents(LivingUpdateEvent event) {

		Entity entity = event.getEntity();

		if (entity instanceof PlayerEntity)
			return;

		if (entity.getBlockPos() == null) {
			return;
		}
		String registryNameAsString = helperGetRegistryNameAsString(entity);
		RegrowthMobItem currentRegrowthMobItem = RegrowthEntitiesManager.getRegrowthMobInfo(registryNameAsString);
		if (currentRegrowthMobItem == null)
			return;

		if (entity.world instanceof ServerWorld sLevel) {

			adjustedPos = getAdjustedBlockPos(entity);
			Block b = Blocks.BLACK_CARPET;
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

			double regrowthEventOdds = 1 / (currentRegrowthMobItem.getRegrowthEventSeconds() * TICKS_PER_SECOND);
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
//					System.out.println("GameTime:"+((AbstractVillager) entity).getLevel().getTime() + " : Roll:" + randomD100Roll + " < " + regrowthEventOdds);
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

	private String helperGetRegistryNameAsString(Entity entity) {
		EntityType<?> tempType = entity.getType();
		Reference<EntityType<?>> registryName = tempType.getRegistryEntry();
		String registryNameAsString = registryName.toString();
		return registryNameAsString;
	}

	private BlockState getAdjustedFootBlockState(Entity e) {
		if (e.getY() == e.getBlockPos().getY()) {
			return e.world.getBlockState(e.getBlockPos());
		}
		return e.world.getBlockState(e.getBlockPos().up());
	}

	private int getAdjustedY(Entity e) {
		if (e.getY() == e.getBlockPos().getY())
			return 1;
		return 0;
	}

	private BlockState getAdjustedGroundBlockState(Entity e) {
		return e.world.getBlockState(e.getBlockPos().down(getAdjustedY(e)));
	}

	private BlockPos getAdjustedBlockPos(Entity e) {
		if (e.getY() == e.getBlockPos().getY()) {
			return e.getBlockPos();
		}
		return e.getBlockPos().up();
	}

	private void improvePowderedSnow(Entity entity) {
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
				int layers  = 2 + hp;
				BlockState snowBlock = Blocks.SNOW_BLOCK.getDefaultState().with(SnowBlock.LAYERS,layers);
				entity.world.setBlockState(entity.getBlockPos(), snowBlock);
			}
		}
	}

	private void doMobRegrowthEvents(Entity entity, String key, String regrowthType) {

		if (regrowthType.equals("stumble")) {
			if ((footBlock instanceof TorchBlock) || (footBlock instanceof WallTorchBlock)) {
				mobStumbleAction(entity, key);
			}
			return;
		}

		if (regrowthType.equals("reforest")) {
			mobReforestAction(entity, key);
			return;
		}

		if (regrowthType.equals("mushroom")) {
			mobGrowMushroomAction(entity, key);
			return;
		}

		// all remaining actions currently require a grass block underfoot so if not a
		// grass block- can exit now.
		// this is for performance savings only.
		// looks like meadow_grass_block is not a grassBlock

		if (!isKindOfGrassBlock(groundBlockState)) {
			return;
		}

		if (regrowthType.equals("tall")) {
			mobGrowTallAction(entity, key);
			return;
		}

		if (regrowthType.equals("both")) {
			if (entity.world.random.nextDouble() * 100 > 85.0) {
				regrowthType = "grow";
			} else {
				regrowthType = "eat";
			}
		}

		if (regrowthType.contentEquals("eat")) {
			mobEatPlantsAction(entity, key, regrowthType);
			return;
		}

		if ((regrowthType.equals("grow"))) {
			mobGrowPlantsAction(entity, key);
			return;
		}
	}

	private boolean mobGrowPlantsAction(Entity entity, String key) {

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

	private boolean isNearWater(World world, BlockPos pos) {
		// TODO offset has different arguments
		Box box = new Box(pos.east(4).north(4), pos.west(4).south(4));
		return world.containsFluid (box);
		// compatability other mods which are not water but hydrate.
		// TODO Forge? Is there a Fabric Way?
//		return FarmlandWaterManager.hasBlockWaterTicket(level, pos);
	}

	private boolean isKindOfGrassBlock(BlockState groundBlockState) {
		if (groundBlockState.getBlock() instanceof GrassBlock)
			return true;
		// TODO test
		if (groundBlockState.getBlock().getTranslationKey().equals("block.byg.meadow_grass_block"))
			return true;
		return false;
	}

	private boolean isBlockGrassOrDirt(BlockState tempBlockState) {

		if (isKindOfGrassBlock(tempBlockState) || (tempBlockState.getBlock() == Blocks.DIRT)) {
			return true;
		}
		return false;
	}

	private BlockState helperSaplingState(World world, BlockPos pos, Biome localBiome, BlockState sapling) {

		// TODO use new BlockTag.SAPLING
		sapling = Blocks.OAK_SAPLING.getDefaultState();
		RegistryKey<Registry<Biome>> k = Registry.BIOME_KEY;
		String biomeName = world.registryAccess().registryOrThrow(k).getKey(localBiome).toString();

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

	private void mobReforestAction(Entity entity, String key) {

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

	private void mobGrowMushroomAction(Entity entity, String key) {
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
			int huge = helperCountBlocksBB(MushroomBlock.class, 1, sWorld, ePos, ModConfigs.getMushroomDensity(), 1);
			if (huge > 0) {
				Utility.debugMsg(1, ePos, key + " huge (" + huge + ") mushroom too crowded.");
				return;
			}
		}

		boolean growMushroom = false;
		if (BlockTags.BASE_STONE_OVERWORLD == null) {
			Utility.debugMsg(0, "BlockTags.BASE_STONE_OVERWORLD missing.");
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

			// TODO which vector is correct one? Also delta Movement...
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
			MushroomBlock mb = (MushroomBlock) theBlock;
			// missing method growMushroom
			try {
				mb.growMushroom(sWorld, ePos, theBlock.getDefaultState(), sWorld.random);
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

	private long helperLongRandomSeed(BlockPos ePos) {
		return (long) Math.abs(ePos.getX() * 31) + Math.abs(ePos.getZ() * 11) + Math.abs(ePos.getY() * 7);
	}

	// this routine returns a count of the searchBlock immediately orthogonal to
	// BlockPos, exiting if a max count is exceeded.
	public int helperCountBlocksOrthogonalBB(Block searchBlock, int maxCount, World w, BlockPos bPos, int boundY) {
		return helperCountBlocksOrthogonalBB(searchBlock, maxCount, w, bPos, 0 - boundY, 0 + boundY);
	}

	public int helperCountBlocksOrthogonalBB(Block searchBlock, int maxCount, World w, BlockPos bPos, int lowerBoundY,
			int upperBoundY) {
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

	public int helperCountBlocksBB(Block searchBlock, int maxCount, World w, BlockPos bPos, int boxSize) {
		return helperCountBlocksBB(searchBlock, maxCount, w, bPos, boxSize, boxSize); // "square" box subcase
	}

	public int helperCountBlocksBB(Block searchBlock, int maxCount, World w, BlockPos bPos, int boxSize, int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;
		BlockPos b;

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
				searchBlock.getRegistryEntry().toString() + " Sparse count:" + count + " countBlockBB ");

		return count;
	}

	public int helperCountBlocksBB(Class<? extends Block> searchBlock, int maxCount, World w, BlockPos bPos,
			int boxSize) {
		return helperCountBlocksBB(searchBlock, maxCount, w, bPos, boxSize, 0);
	}

	public int helperCountBlocksBB(Class<? extends Block> searchBlock, int maxCount, World w, BlockPos bPos,
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

	private boolean isGoodMushroomTemperature(Entity entity) {
		BlockPos ePos = getAdjustedBlockPos(entity);
		float biomeTemp = entity.world.getBiome(ePos).value().getTemperature();
		Utility.debugMsg(1, ePos, "Mushroom Biome temp: " + biomeTemp + ".");
		if (biomeTemp < ModConfigs.getMushroomMinTemp())
			return false;
		if (biomeTemp > ModConfigs.getMushroomMaxTemp())
			return false;
		return true;
	}

	private boolean mobEatPlantsAction(Entity entity, String key, String regrowthType) {
		if (mobEatGrassOrFlower(entity, regrowthType)) {
			Utility.debugMsg(1, getAdjustedBlockPos(entity), key + " ate plants.");
			return true;
		}
		return false;
	}

	private boolean isHorseTypeEatingNow(Entity entity) {
		if (entity instanceof HorseBaseEntity) {
			HorseBaseEntity h = (HorseBaseEntity) entity;
			if (h.isEatingGrass()) {
				return true;
			}
		}
		return false;
	}

	private void mobStumbleAction(Entity entity, String key) {
		entity.world.breakBlock(getAdjustedBlockPos(entity), true);
		Utility.debugMsg(1, getAdjustedBlockPos(entity), key + " stumbled over torch.");
	}

	private void doVillagerRegrowthEvents(VillagerEntity ve, String debugKey, String regrowthActions) {

		// Villagers hopping, falling, etc. are doing improvements.
		if (!(ve.isOnGround()))
			return;
		if (groundBlockState.getBlock() instanceof TorchBlock)
			return;
		if (groundBlockState.getBlock() instanceof WallTorchBlock)
			return;

		// Give custom debugging names to nameless villagers.
		if (ModConfigs.getDebugLevel() > 0) {
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

	private void helperJumpAway(Entity e) {
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

	private boolean mobEatGrassOrFlower(Entity entity, String regrowthType) {

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
		if (le.getMaxHealth() > le.getHealth() && (ModConfigs.getEatingHeals())) {
			StatusEffectInstance ei = new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 1, 0, false, true);
			le.addStatusEffect(ei);
		}
		return true;
	}

	private void mobTrodGrassBlock(Entity e) {

		BlockPos ePos = new BlockPos(e.getX(), e.getY(), e.getZ());
		if (e.world instanceof ServerWorld varWorld) {
			Box box = new Box(ePos.east(2).up(2).north(2), ePos.west(2).down(2).south(2));
			List<Entity> l = new ArrayList<>();
			varWorld.getEntities().get(e.getType(), box, (entity) -> {
				l.add(entity);
			});
			if (l.size() >= 9) {
				varWorld.setBlockState(ePos.down(), Blocks.DIRT_PATH.getDefaultState());
				e.damage(DamageSource.IN_WALL, 0.25f);
			}
		}

	}

	private boolean isBlockGrassPathOrDirt(Block tempBlock) {

		if ((tempBlock == Blocks.DIRT_PATH) || (tempBlock == Blocks.DIRT)) {
			return true;
		}
		return false;
	}

	private void helperChildAgeEntity(Entity ent) {
		if (ent.age < 0) {
			ent.age += 30;
		}
	}

	private boolean mobGrowTallAction(Entity ent, String key) {
		if (footBlock == Blocks.TALL_GRASS) {
			BlockPos ePos = getAdjustedBlockPos(ent);
			Fertilizable ib = (Fertilizable) footBlock;
			ib.grow((ServerWorld) ent.world, ent.world.random, ePos, ent.world.getBlockState(ePos));
			Utility.debugMsg(1, ePos, key + " grew and hid in tall plant.");
			return true;

			// TODO: Flower Types for Postion here
//			List<ConfiguredFeature<?, ?>> list = ((Biome) world.getBiome(blockPos2).value())
//					.getGenerationSettings().getFlowerFeatures();
		}
		return false;
	}

	private BlockState helperBiomeRoadBlockType(Biome localBiome) {

		BlockState gateBlockType = Blocks.DIRT_PATH.getDefaultState();

		if (biomeCategory == Biome.Category.DESERT) {
			gateBlockType = Blocks.ACACIA_PLANKS.getDefaultState(); // 16.1 mojang change
		}
		return gateBlockType;
	}

	// if a grassblock in village has farmland next to it on the same level- retill
	// it.
	// todo add hydration check before tilling land.
	private boolean vImproveFarm(VillagerEntity ve, String regrowthType) {
		if (ve.getVillagerData().getProfession() != VillagerProfession.FARMER) {
			return false;
		}

		BlockPos vePos = getAdjustedBlockPos(ve);
		Block groundBlock = groundBlockState.getBlock();
		Block footBlock = footBlockState.getBlock();

		if (helperCountBlocksOrthogonalBB(Blocks.FARMLAND, 1, ve.world, vePos.below(1), 0) > 0) {
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

	private void vBeeKeeperFlowers(VillagerEntity ve) {
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

	private void vToolMasterHealing(VillagerEntity ve) {

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
			List<Entity> l = new ArrayList<>();
			// TODO method may be private/protected
			varW.getEntities().get(box, (entity) -> {
				if (entity instanceof IronGolem) {
					l.add(entity);
				}
			});
			for (Entity e : l) {
				boolean heal = true;
				LivingEntity le = (LivingEntity) e;
				if (le.getHealth() < le.getMaxHealth()) {
					if (heal) {
						le.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, villagerLevel * 51, 0),
								ve);
						ve.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, villagerLevel * 11, 0),
								ve);
						ve.world.playSound(null, vePos, SoundEvents.ENTITY_VILLAGER_WORK_TOOLSMITH,
								SoundCategory.NEUTRAL, 0.5f, 0.5f);
						return;
					}
				}
			}
		}
	}

	private void vClericalHealing(VillagerEntity ve) {

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
			List<Entity> l = new ArrayList<>();
			// TODO problem with getEntitiesByType and getOtherEntities
			varW.getOtherEntities().get(box, (entity) -> {
				if (entity instanceof VillagerEntity || entity instanceof Player) {
					l.add(entity);
				}
			});

			for (Entity e : l) {
				boolean heal = true;
				LivingEntity le = (LivingEntity) e;
				if (le.getHealth() < le.getMaxHealth()) {
					if (e instanceof PlayerEntity pe) {
						int rep = ve.getReputation(pe);
						if (rep < 0) { // I was a bad bad boy.
							heal = false;
						}
					}
					if (heal) {
						le.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, clericalLevel * 51, 0),
								ve);
						// TODO AmethystBreak missing at this time.
						ve.world.playSound(null, vePos, SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.NEUTRAL, 1.2f,
								1.52f);
						return;
					}
				}
			}
		}
	}

	private void vImproveLeaves(VillagerEntity ve, String key) {

		int partialBlockAdjust = 0;

		// May just need to get headYaw or now.
		float veYaw = ve.getYaw(1.0f) / 45;

		BlockPos vePos = getAdjustedBlockPos(ve);
		int facingNdx = Math.round(veYaw);
		if (facingNdx < 0) {
			facingNdx = Math.abs(facingNdx);
		}
		facingNdx %= 8;

		// when standing on a grass path- game reports you 1 block lower. Adjust.
		if (groundBlock == Blocks.DIRT_PATH) {
			partialBlockAdjust = 1;
		}
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
				// TODO this may not show local language
				Utility.debugMsg(1, vePos, key + " cleared " + tempBlock.getTranslationKey().toString());
			}
		}
	}

	private boolean vImproveLighting(VillagerEntity ve) {
		BlockPos vePos = getAdjustedBlockPos(ve);

		int blockLightValue = ve.world.getLightLevel(LightType.BLOCK, vePos);
		int skyLightValue = ve.world.getLightLevel(LightType.SKY, vePos);

		if (blockLightValue > ModConfigs.getTorchLightLevel())
			return false;
		if (skyLightValue > 13)
			return false;

		if (ve.isSleeping()) {
			return false;
		}
		if (footBlockState.getBlock() instanceof BedBlock) {
			return false;
		}

		if (isValidGroundBlockToPlaceTorchOn(ve) && (footBlockState.isAir())) {
			ve.world.setBlockState(vePos, Blocks.TORCH.getDefaultState());
		}

		return true;

	}

	private void vImproveRoads(VillagerEntity ve, String debugkey) {

		Text tName = new LiteralText("-");
//		ve.setCustomName(tName);

		isRoadPiece = false;

		boolean isInsideStructurePiece = false;
		boolean test = true;
		BlockPos piecePos = null;
		List<StructureStart> sList = new ArrayList<StructureStart>();
		if (test) {
			ChunkPos c = new ChunkPos(ve.getBlockPos());
// lupexp	List<StructureStart> x2 = getStarts(world, StructureFeature.VILLAGE, 0, 0);
			sList = getStarts(ve.world, StructureFeature.VILLAGE, c.x, c.z);
		}
		if (!sList.isEmpty()) {

			for (StructurePiece piece : sList.get(0).getChildren()) {
				piecePos = piece.getCenter();
				// TODO encompass()?
				if (piece.getBoundingBox().isInside(ve.getBlockPos())) {
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

	private List<StructureStart> getStarts(World worldIn, StructureFeature<?> struct, int x, int z) {
		List<StructureStart> list = Lists.newArrayList();
		Chunk ichunk = worldIn.getChunk(x, z, ChunkStatus.STRUCTURE_REFERENCES);

		for (Entry<ConfiguredStructureFeature<?, ?>, LongSet> r : ichunk.getStructureReferences().entrySet()) {
			if (r.getKey().feature == struct) {
				LongIterator longiterator = r.getValue().iterator();
				while (longiterator.hasNext()) {
					long i = longiterator.nextLong();
					// TODO line below... "PackedX" correct?
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
	private boolean vImproveRoadsClearSnow(Entity e) {
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

	private boolean vImproveRoadsFixPatches(Entity e) {

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
			roadY = e.world.getTopPosition(Type.MOTION_BLOCKING_NO_LEAVES, vePos.getX() + dx[i], vePos.getZ() + dz[i])
					- 1;
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

	private boolean vImproveRoadsFixPotholes(Entity e) {

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
			roadY = e.world.getHeight(Type.MOTION_BLOCKING_NO_LEAVES, veX + dx[i], veZ + dz[i]) - 1;
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
	private boolean vImproveRoadsSmoothHeight(VillagerEntity ve) {

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
			roadY = ve.world.getHeight(Type.MOTION_BLOCKING_NO_LEAVES, veX + dx[i], veZ + dz[i]) - 1;
			if (roadY > veY) {
				Block tempBlock = ve.world.getBlockState(new BlockPos(veX + dx[i], roadY, veZ + dz[i])).getBlock();
				if (tempBlock == biomeRoadBlock) {
					ve.world.setBlockState(new BlockPos(veX, veY, veZ), biomeRoadBlockState);
					ve.setVelocity(0.0, 0.4, 0.0);
					return true;
				}
			}
		}

		return false;
	}

	private boolean isNearbyPoi(VillagerEntity ve, Biome localBiome, BlockPos vePos, int poiDistance) {

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

	private boolean vImproveVillageWall(VillagerEntity ve, String regrowthActions) {
		if (!(ve.getBrain().getOptionalMemory(MemoryModuleType.MEETING_POINT)).isPresent())
			return false;

		if (!isOkayToBuildWallHere(ve)) {
			return false;
		}

		BlockPos gVMPPos = ve.getBrain().getOptionalMemory(MemoryModuleType.MEETING_POINT).get().getPos();

		if (ModConfigs.playerWallControlBlock != Blocks.AIR) {
			if (ve.world.getChunk(gVMPPos).getInhabitedTime() < 200) // Bell
				ve.world.setBlock(gVMPPos.up(1), ModConfigs.playerWallControlBlock.getDefaultState(), 3);

			if (ve.world.getBlockState(gVMPPos.up(1)).getBlock() != ModConfigs.playerWallControlBlock) {
				return false;
			}
		}

		BlockPos vePos = getAdjustedBlockPos(ve);

		String key = "minecraft:" + biomeCategory.toString();
//		ResourceLocation biomeName = ForgeRegistries.BIOMES.getKey(localBiome);
		key = key.toLowerCase();
		Utility.debugMsg(2, vePos, key + " wall improvement.");

		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		Utility.debugMsg(1, vePos, key + " biome for wall improvement. ");

		int wallRadius = currentWallBiomeDataItem.getWallDiameter();

		wallRadius = (wallRadius / 2) - 1;

		if (isOnWallPerimeter(ve, wallRadius, gVMPPos)) {
			Utility.debugMsg(2, ve.getBlockPos(), "villager on wall perimeter: " + wallRadius);
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
				BlockState gateBlockType = helperBiomeRoadBlockType(localBiome);

				int wallTorchSpacing = (wallRadius + 1) / 4;
				if (helperPlaceOneWallPiece(ve, wallRadius, wallTorchSpacing, gateBlockType, wallBlock, gVMPPos)) {
					if (regrowthActions.contains("t")) {
						int absvx = (int) Math.abs(ve.getX() - gVMPPos.getX());
						int absvz = (int) Math.abs(ve.getZ() - gVMPPos.getZ());
						if (isValidTorchLocation(wallRadius, wallTorchSpacing, absvx, absvz,
								ve.world.getBlockState(vePos).getBlock())) {
							ve.world.setBlockState(vePos.up(), Blocks.TORCH.getDefaultState());
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
	private boolean vImproveHomeFence(VillagerEntity ve, BlockPos vHomePos, String regrowthActions) {

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
			BlockState gateBlockType = helperBiomeRoadBlockType(localBiome);

			boolean buildCenterGate = true;
			if (helperPlaceOneWallPiece(ve, homeFenceDiameter, wallTorchSpacing, gateBlockType, fenceBlockState,
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

	private void vImproveWalls(VillagerEntity ve, String key, String regrowthType) {

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

	private void vImproveFences(VillagerEntity ve, String key, String regrowthType) {

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

	private boolean isFootBlockOkayToBuildIn(BlockState footBlockState) {
		if ((footBlockState.isAir()) || (isGrassOrFlower(footBlockState))) {
			return true;
		}
		if (footBlockState.getBlock() instanceof SnowBlock) {
			return true;
		}
		return false;
	}

	private boolean isGrassOrFlower(BlockState footBlockState) {
		Block footBlock = footBlockState.getBlock();

		// TODO instanceof tall grass block?
		if (footBlock == Blocks.TALL_GRASS) {
			return true;
		}
		if (footBlock instanceof FlowerBlock) {
			return true;
		}
		if (footBlock instanceof TallPlantBlock) {
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
			if (footBlockState.isIn(BlockTags.FLOWERS)) {
				return true;
			}
			if (footBlockState.isIn(BlockTags.TALL_FLOWERS)) {
				return true;
			}
		} catch (Exception e) {
			if (ModConfigs.getDebugLevel() > 0) {
				System.out.println("Tag Exception 1009-1014:" + footBlock.getDescriptionId() + ".");
			}
		}
		// biomes you'll go grass compatibility
		if (footBlock.getTranslationKey() == "block.byg.short_grass") {
			return true;
		}
		if (ModConfigs.getDebugLevel() > 0) {
			System.out.println("Not grass or Flower:" + footBlock.getTranslationKey() + ".");
		}
		return false;
	}

	private boolean isImpossibleRegrowthEvent(String regrowthType) {
		if ((regrowthType.equals("eat")) && (footBlockState.isAir())) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlockState.getBlock() == Blocks.TALL_GRASS)) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlockState.getBlock() instanceof FlowerBlock)) {
			return true;
		}
		if ((regrowthType.equals("tall")) && (!(footBlockState.getBlock() == Blocks.TALL_GRASS))) {
			return true;
		}
		return false;
	}

	private boolean isOkayToBuildWallHere(VillagerEntity ve) {

		BlockPos ePos = ve.getBlockPos();

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

	private boolean isOnGround(Entity e) {
		return e.isOnGround();
	}

	private boolean isOnWallPerimeter(Entity e, int wallRadius, BlockPos gVMPPos) {
		boolean onPerimeter = false;
		int absvx = (int) Math.abs(e.getX() - gVMPPos.getX());
		int absvz = (int) Math.abs(e.getZ() - gVMPPos.getZ());
		if ((absvx == wallRadius) && (absvz <= wallRadius))
			onPerimeter = true;
		if ((absvz == wallRadius) && (absvx <= wallRadius))
			onPerimeter = true;
		return onPerimeter;
	}

//	ItemStack iStk = new ItemStack(Items.BONE_MEAL,1);
//	BoneMealItem.applyBonemeal(iStk, e.world,e.getPosition());
// (likely 12.2 and 14.4 call?)	ib.performBonemeal((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));\

	private boolean isOutsideMeetingPlaceWall(VillagerEntity ve, Optional<GlobalPos> vMeetingPlace,
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

	private boolean isValidGroundBlockToPlaceTorchOn(VillagerEntity ve) {

		String key = groundBlockState.getBlock().getRegistryEntry().toString(); // broken out for easier debugging
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(key);
		if (currentWallFoundationItem == null)
			return false;

		return true;

	}

	private boolean isValidGroundBlockToBuildWallOn(Entity e) {

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
		BlockState testBlockState = getAdjustedGroundBlockState(e);
		groundBlock = groundBlockState.getBlock();

		Utility.debugMsg(1, e.getBlockPos(),
				"Build Wall : gb" + groundBlock.toString() + ", fb:" + footBlock.toString());
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(groundBlock.getRegistryEntry().toString());

		if (currentWallFoundationItem == null)
			return false;

		return true;

	}

	private boolean isValidTorchLocation(int wallPerimeter, int wallTorchSpacing, int absvx, int absvz,
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
		if ((absvx == wallPerimeter) && ((absvz % wallTorchSpacing) == 1)) {
			return true;
		}
		if (((absvx % wallTorchSpacing) == 1) && (absvz == wallPerimeter)) {
			return true;
		}
		if ((absvx == wallPerimeter) && (absvz == wallPerimeter)) {
			return true;
		}

		return false;
	}

	private boolean helperPlaceOneWallPiece(Entity e, int wallPerimeter, int wallTorchSpacing, BlockState gateBlockType,
			BlockState wallType, BlockPos gVMPPos) {

		int absvx = (int) Math.abs(e.getX() - gVMPPos.getX());
		int absvz = (int) Math.abs(e.getZ() - gVMPPos.getZ());
		// Build North and South Walls (and corners)
		if (absvx == wallPerimeter) {
			if (absvz <= wallPerimeter) {
				return helperPlaceWallPiece(e, gateBlockType, wallType, absvz);
			}
		}
		// Build East and West Walls (and corners)
		if (absvz == wallPerimeter) {
			if (absvx <= wallPerimeter) {
				return helperPlaceWallPiece(e, gateBlockType, wallType, absvx);
			}
		}
		return false;
	}

	private boolean helperPlaceWallPiece(Entity e, BlockState gateBlockType, BlockState wallType, int absva) {

		BlockPos vePos = getAdjustedBlockPos(e);

		if (footBlock == Blocks.SNOW) {
			e.world.breakBlock(vePos, false);
		}
		// TODO instanceof TallGrassBlock

		if ((footBlock instanceof SaplingBlock) || (footBlock == Blocks.GRASS) || (footBlock instanceof FlowerBlock)
				|| (footBlock instanceof TallPlantBlock)) {
			e.world.breakBlock(vePos, true);
		}

		if (absva == WALL_CENTER) {
			e.world.setBlockState(vePos, gateBlockType);
			return true;
		}

		if (e.world.setBlockState(vePos, wallType)) {
			return true;
		} else {
			Utility.debugMsg(1, e.getBlockPos(),
					"Building Wall Fail: SetBlockAndUpdate Time End = " + e.world.getTime());
			return false;
		}

	}

}
