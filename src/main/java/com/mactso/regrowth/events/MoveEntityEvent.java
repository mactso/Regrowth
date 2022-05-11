package com.mactso.regrowth.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.atn.BlockStartState;

import com.google.common.collect.Lists;
import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import com.mactso.regrowth.config.RegrowthEntitiesManager.RegrowthMobItem;
import com.mactso.regrowth.config.WallBiomeDataManager;
import com.mactso.regrowth.config.WallFoundationDataManager;
import com.mactso.regrowth.utility.Utility;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeCategory;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.CoralTreeFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.FarmlandWaterManager;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.world.BlockEvent.FarmlandTrampleEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber()
public class MoveEntityEvent {

	private Random moveRand = new Random();
	private final Block[] coralPlants = {Blocks.TALL_SEAGRASS,Blocks.SEAGRASS,Blocks.SEA_PICKLE, Blocks.BRAIN_CORAL_FAN,Blocks.BUBBLE_CORAL_FAN,Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN,Blocks.TUBE_CORAL_FAN,Blocks.BRAIN_CORAL_FAN,Blocks.BUBBLE_CORAL_FAN,Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN,Blocks.TUBE_CORAL_FAN};
	private final Block[] coralfans = { Blocks.BRAIN_CORAL_WALL_FAN,Blocks.BUBBLE_CORAL_WALL_FAN,Blocks.FIRE_CORAL_WALL_FAN, Blocks.HORN_CORAL_WALL_FAN,Blocks.TUBE_CORAL_WALL_FAN };
	private final Rotation[] coralfanrotations = {Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90, Rotation.CLOCKWISE_90, Rotation.NONE};

	private int[] dx = { 1, 0, -1, 0 };
	private int[] dz = { 0, 1, 0, -1 };
	private int CHECKS_PER_SECOND = 10;
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


	private BiomeCategory biomeCategory;
	BlockPos adjustedPos;

	@SubscribeEvent
	public void handleTrampleEvents(FarmlandTrampleEvent event) {
		BlockPos pos = event.getEntity().blockPosition();
		MyConfig.debugMsg(1, pos, "Enter FarmlandTrampleEvent");
		if (event.isCancelable()) {
			if (event.getEntity() instanceof Villager ve) {
				if (ve.getVillagerData().getProfession() != VillagerProfession.FARMER) {
					MyConfig.debugMsg(2, pos, "Villager Not A Farmer");
					return;
				}
				if (ve.getVillagerData().getLevel() >= 3) {
					event.setCanceled(true);
					MyConfig.debugMsg(2, pos, "Farmer under level 3.");
					return;
				}
			}
			if ((event.getEntity() instanceof ServerPlayer spe)) {
				if (!spe.isCreative()) {
					return;
				}
				MyConfig.debugMsg(2, pos, "FarmlandTrampleCancelled");
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void handleEntityMoveEvents(LivingUpdateEvent event) {

		Entity entity = event.getEntity();

		if (entity instanceof Player)
			return;

		if (entity.getId()%2 == entity.level.getGameTime()%2) 
			return;
		
		if (entity.blockPosition() == null) 
			return;
		
		String registryNameAsString = helperGetRegistryNameAsString(entity);
		RegrowthMobItem currentRegrowthMobItem = RegrowthEntitiesManager.getRegrowthMobInfo(registryNameAsString);
		if (currentRegrowthMobItem == null)
			return;

		if (entity.level instanceof ServerLevel sLevel) {

			adjustedPos = getAdjustedBlockPos(entity);

			footBlockState = getAdjustedFootBlockState(entity);
			footBlock = footBlockState.getBlock();
			if (footBlock instanceof WoolCarpetBlock)
				return;

			groundBlockState = getAdjustedGroundBlockState(entity);
			groundBlock = groundBlockState.getBlock();
			if (groundBlockState.isAir())
				return;

			localBiome = entity.level.getBiome(entity.blockPosition()).value();
			biomeCategory = Biome.getBiomeCategory(sLevel.getBiome(entity.blockPosition()));

			String regrowthActions = currentRegrowthMobItem.getRegrowthActions();

			if (isImpossibleRegrowthEvent(regrowthActions))
				return;

			double regrowthEventOdds = 1 / (currentRegrowthMobItem.getRegrowthEventSeconds() * CHECKS_PER_SECOND);
			if (isHorseTypeEatingNow(entity)) {
				regrowthEventOdds *= 20;
			}
			double randomD100Roll = entity.level.random.nextDouble();
			int debugvalue = 0; // TODO make sure value 0 after debugging.

			long chunkAge = entity.level.getChunkAt(entity.blockPosition()).getInhabitedTime();

			// improve village roads and walls faster for the first 200 minutes;
			if (chunkAge < 480000) {
				if (entity instanceof Villager ve) {
					if (ve.level.getGameTime() % 12 == 0) {
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
				if (entity instanceof Villager ve) {
//					System.out.println("GameTime:"+((AbstractVillager) entity).getLevel().getGameTime() + " : Roll:" + randomD100Roll + " < " + regrowthEventOdds);
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
		ResourceLocation registryName = tempType.getRegistryName();
		String registryNameAsString = registryName.toString();
		return registryNameAsString;
	}

	private BlockState getAdjustedFootBlockState(Entity e) {
		if (e.getY() == e.blockPosition().getY()) {
			return e.level.getBlockState(e.blockPosition());
		}
		return e.level.getBlockState(e.blockPosition().above());
	}

	private int getAdjustedY(Entity e) {
		if (e.getY() == e.blockPosition().getY())
			return 1;
		return 0;
	}

	private BlockState getAdjustedGroundBlockState(Entity e) {
		return e.level.getBlockState(e.blockPosition().below(getAdjustedY(e)));
	}

	private BlockPos getAdjustedBlockPos(Entity e) {
		if (e.getY() == e.blockPosition().getY()) {
			return e.blockPosition();
		}
		return e.blockPosition().above();
	}

	private void improvePowderedSnow(Entity entity) {
		Level sLevel = entity.level;
		if (entity.isInPowderSnow) {
			int hp = 0;
			if (sLevel.getBlockState(entity.blockPosition().above(2)).getBlock() == Blocks.POWDER_SNOW) {
				entity.level.destroyBlock(entity.blockPosition().above(2), false);
				hp = 2;
			}
			if (sLevel.getBlockState(entity.blockPosition().above()).getBlock() == Blocks.POWDER_SNOW) {
				entity.level.destroyBlock(entity.blockPosition().above(), false);
				hp += 2;
			}
			if (sLevel.getBlockState(entity.blockPosition()).getBlock() == Blocks.POWDER_SNOW) {
				BlockState SnowLayer = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2 + hp);
				entity.level.setBlockAndUpdate(entity.blockPosition(), SnowLayer);
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
		
		if (regrowthType.equals("coral")) {
			mobGrowCoralAction(entity, key);
			return;
		}

		int x=3;
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
			if (entity.level.random.nextDouble() * 100 > 85.0) {
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

	private boolean mobGrowCoralAction(Entity e, String key) {

		Level level = e.level;
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

		BlockPos pos = e.blockPosition();


		
		if (level.getBlockState(pos.below(0)).getBlock() != Blocks.WATER) return false; // should be impossible.
		if (level.getBlockState(pos.below(1)).getBlock() != Blocks.WATER) return false;

		if (level.getBlockState(pos.below(2)).getBlock() instanceof CoralBlock) {
			MyConfig.debugMsg(2, pos, "Coral double:" + docoralplant);
			MyConfig.debugMsg(2, pos, "Coral fan double:" + docoralfan);
			
			MyConfig.debugMsg(2, pos, "Coral plant opportunity:" +e.getType().getRegistryName().toString() +" .");

			if (docoralfan < 0.3) {
				Direction d = Direction.from2DDataValue(coralfanDirection);
				BlockPos fanPos = e.blockPosition().below(2).relative(d);
				if (level.getBlockState(fanPos).getBlock() == Blocks.WATER) {
					level.setBlockAndUpdate(fanPos, coralfans[rand.nextInt(coralfans.length)].defaultBlockState().setValue(CoralWallFanBlock.FACING,d));
				}
			}
			
			
			int count = countCoral(e);
			MyConfig.debugMsg(2, pos, "CORAL count = :" + count + ", "+e.getType().getRegistryName().toString() +" .");
			if (count > 5) return false;
			BlockState theCoralBlock = level.getBlockState(pos.below(2)); // grow same kind of coral block
			if ((count < 6) && (e.getBlockY() == minCoraldepth)) {
				if (docoralplant < 0.30) return false;
				MyConfig.debugMsg(2, pos, "CORAL Plant grows over Coral Block:" +e.getType().getRegistryName().toString() +" .");
				level.setBlockAndUpdate(pos.below(1), coralPlants[rand.nextInt(coralPlants.length)].defaultBlockState());
				level.playSound(null, pos, SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.AMBIENT, 0.9f, 1.4f);
				return true;
			} else if ( (e.getBlockY() < minCoraldepth)) {
				int ew = rand.nextInt(3)-1;
				int ns = rand.nextInt(3)-1;
				if (level.getBlockState(pos.below(1).east(ew).north(ns)).getBlock() != Blocks.WATER) return false;
				MyConfig.debugMsg(2, pos, "CORAL Block grows over Coral Block:" +e.getType().getRegistryName().toString() +" .");
				level.setBlockAndUpdate(pos.below(1).east(ew).north(ns), theCoralBlock);
				level.playSound(null, pos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.AMBIENT, 0.9f, 1.4f);
				MyConfig.debugMsg(2, pos, "CORAL:" +e.getType().getRegistryName().toString() +" new block set at near " + pos.below(1)+" .");

			}

			
		}
		
		return true;
	}

	private int countCoral(Entity e) {
		int c = 0;
		for (int ud=-1; ud<=0; ud++) {
			for (int ew=-1; ew<=1; ew++) {
				for (int ns = -1; ns<=1; ns++) {
					if (e.level.getBlockState(e.blockPosition().below(1).east(ew).north(ns)).getBlock() instanceof CoralBlock) {
						c++;
					}
				}
			}
		}
		return c;
	}

	private boolean mobGrowPlantsAction(Entity entity, String key) {

		if (footBlockState.isAir()) {
			if (!(groundBlock instanceof BonemealableBlock)) {
				return false;
			}
			BlockPos bpos = entity.blockPosition();
			if (bpos == null) {
				MyConfig.debugMsg(1, "ERROR:" + key + "grow plant null position.");
				return false;
			}
			BonemealableBlock ib = (BonemealableBlock) groundBlock;
			MyConfig.debugMsg(1, entity.blockPosition(), key + " growable plant found.");
			try {
				ServerLevel serverworld = (ServerLevel) entity.level;
				BlockState bs = entity.level.getBlockState(bpos);
				ib.performBonemeal(serverworld, entity.level.random, bpos, bs);
				MyConfig.debugMsg(1, bpos, key + " grew plant.");
			} catch (Exception e) {
				MyConfig.debugMsg(1, bpos, key + " caught grow attempt exception.");
			}
		}
		return true;
	}

	private boolean isNearWater(LevelReader level, BlockPos pos) {
		for (BlockPos blockpos : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
			if (level.getFluidState(blockpos).is(FluidTags.WATER)) {
				return true;
			}
		}
		// compatability other mods which are not water but hydrate.
		return FarmlandWaterManager.hasBlockWaterTicket(level, pos);
	}

	private boolean isKindOfGrassBlock(BlockState groundBlockState) {
		if (groundBlockState.getBlock() instanceof GrassBlock)
			return true;
		if (groundBlockState.getBlock().getDescriptionId().equals("block.byg.meadow_grass_block"))
			return true;
		return false;
	}

	private boolean isBlockGrassOrDirt(BlockState tempBlockState) {

		if (isKindOfGrassBlock(tempBlockState) || (tempBlockState.getBlock() == Blocks.DIRT)) {
			return true;
		}
		return false;
	}

	private BlockState helperSaplingState(Level world, BlockPos pos, Biome localBiome, BlockState sapling) {

		// TODO use new BlockTag.SAPLING
		sapling = Blocks.OAK_SAPLING.defaultBlockState();
		ResourceKey<Registry<Biome>> k = Registry.BIOME_REGISTRY;
		String biomeName = world.registryAccess().registryOrThrow(k).getKey(localBiome).toString();

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

		if (entity.level.random.nextDouble() > Math.abs(sinY))
			return;

		BlockState sapling = null;
		// are we in a biome that has saplings in a spot a sapling can be planted?
		sapling = helperSaplingState(entity.level, ePos, localBiome, sapling);

		// check if there is room for a new tree. Original trees.
		// don't plant a sapling near another sapling
		// TEST: The SaplingBlock
		int hval = 5;
		int yval = 0;
		int yrange = 0;

		if (helperCountBlocksBB(SaplingBlock.class, 1, entity.level, ePos, hval, yrange) > 0)
			return;

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
		leafCount = helperCountBlocksBB(LeavesBlock.class, 1, entity.level, ePos.above(yval), hval, yrange);
		if (leafCount > 0)
			return;

		entity.level.setBlockAndUpdate(ePos, sapling);
		MyConfig.debugMsg(1, ePos, key + " planted sapling.");
	}

	private void mobGrowMushroomAction(Entity entity, String key) {
		ServerLevel sWorld = (ServerLevel) entity.level;
		BlockPos ePos = entity.blockPosition();
		if (sWorld.getBlockState(ePos).getBlock() instanceof MushroomBlock) {
			return;
		}

		if (sWorld.canSeeSky(ePos)) {
			return;
		}
		if (!(isGoodMushroomTemperature(entity))) {
			return;
		}

		Random mushRand = new Random(helperLongRandomSeed(entity.blockPosition()));

		double fertilityDouble = mushRand.nextDouble();
		fertilityDouble = mushRand.nextDouble();

		if (fertilityDouble < .75) {
			MyConfig.debugMsg(1, ePos, key + " Mushroom fertility (" + fertilityDouble + ") non-growing spot.");
			return;
		}

		int smallMushroomCount = helperCountBlocksBB(MushroomBlock.class, 4, sWorld, ePos, 4, 1);

		if (smallMushroomCount > 3) {
			MyConfig.debugMsg(1, ePos, key + " smallMushroomCount (" + smallMushroomCount + ") mushroom too crowded.");
			return;
		}

		// dust the top of giant mushrooms with little mushrooms of the same type.

		if (groundBlock == Blocks.RED_MUSHROOM_BLOCK) {
			sWorld.setBlockAndUpdate(ePos, Blocks.RED_MUSHROOM.defaultBlockState());
			return;
		}

		if (groundBlock == Blocks.BROWN_MUSHROOM_BLOCK) {
			sWorld.setBlockAndUpdate(ePos, Blocks.BROWN_MUSHROOM.defaultBlockState());
			return;
		}

		int hugeMushroomCount = helperCountBlocksBB(HugeMushroomBlock.class, 1, sWorld, ePos, 1, 1);
		if (hugeMushroomCount > 0) {
			// if right next to a huge mushroom let it grow if it got past above density
			// check.
		} else {
			int huge = helperCountBlocksBB(HugeMushroomBlock.class, 1, sWorld, ePos, MyConfig.getMushroomDensity(), 1);
			if (huge > 0) {
				MyConfig.debugMsg(1, ePos, key + " huge (" + huge + ") mushroom too crowded.");
				return;
			}
		}

		boolean growMushroom = false;
		if (BlockTags.BASE_STONE_OVERWORLD == null) {
			MyConfig.debugMsg(0, "BlockTags.BASE_STONE_OVERWORLD missing.");
			if (groundBlock == Blocks.STONE || groundBlock == Blocks.DIORITE || groundBlock == Blocks.ANDESITE
					|| groundBlock == Blocks.GRANITE) {
				growMushroom = true;
			}
		} else {
			if (!groundBlockState.is(BlockTags.BASE_STONE_OVERWORLD)) {
				return;
			}
			growMushroom = true;
		}

		if (sWorld.hasNearbyAlivePlayer((double) ePos.getX(), (double) ePos.getY(), (double) ePos.getZ(), 12.0)) {
			growMushroom = false;
		}

		if (growMushroom) {

			double vx = entity.position().x() - (ePos.getX() + 0.5d);
			double vz = entity.position().z() - (ePos.getZ() + 0.5d);

			Vec3 vM = new Vec3(vx, 0.0d, vz).normalize().scale(1.0d).add(0, 0.5, 0);
			entity.setDeltaMovement(entity.getDeltaMovement().add(vM));
			if (fertilityDouble > 0.9) {
				sWorld.setBlockAndUpdate(ePos.below(), Blocks.MYCELIUM.defaultBlockState());
			}

			Block theBlock = null;
			if (sWorld.random.nextDouble() * 100.0 > 75.0) {
				theBlock = Blocks.RED_MUSHROOM;
			} else {
				theBlock = Blocks.BROWN_MUSHROOM;
			}
			sWorld.setBlockAndUpdate(ePos, theBlock.defaultBlockState());
			MushroomBlock mb = (MushroomBlock) theBlock;
			try {
				mb.growMushroom(sWorld, ePos, theBlock.defaultBlockState(), sWorld.random);
			} catch (Exception e) {
				// technically an "impossible" error but it's happened so this should
				// bulletproof it.
			}

			// light the top stem inside the cap with glowshroom.
			if (theBlock == Blocks.RED_MUSHROOM) {
				for (int y = 9; y > 3; y--) {
					Block b = sWorld.getBlockState(ePos.above(y)).getBlock();
					if (b == Blocks.MUSHROOM_STEM) {
						sWorld.setBlockAndUpdate(ePos.above(y), Blocks.SHROOMLIGHT.defaultBlockState());
						break;
					}
				}
			}

			MyConfig.debugMsg(1, ePos, key + " grow mushroom.");
		}

	}

	private long helperLongRandomSeed(BlockPos ePos) {
		return (long) Math.abs(ePos.getX() * 31) + Math.abs(ePos.getZ() * 11) + Math.abs(ePos.getY() * 7);
	}

	// this routine returns a count of the searchBlock immediately orthogonal to
	// BlockPos, exiting if a max count is exceeded.
	public int helperCountBlocksOrthogonalBB(Block searchBlock, int maxCount, Level w, BlockPos bPos, int boundY) {
		return helperCountBlocksOrthogonalBB(searchBlock, maxCount, w, bPos, 0 - boundY, 0 + boundY);
	}

	public int helperCountBlocksOrthogonalBB(Block searchBlock, int maxCount, Level w, BlockPos bPos, int lowerBoundY,
			int upperBoundY) {
		int count = 0;
		for (int j = lowerBoundY; j <= upperBoundY; j++) {
			if (w.getBlockState(bPos.above(j).east()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.above(j).west()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.above(j).north()).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.above(j).south()).getBlock() == searchBlock)
				count++;
			if (count >= maxCount)
				return count;
		}

		return count;

	}

	public int helperCountBlocksBB(Block searchBlock, int maxCount, Level w, BlockPos bPos, int boxSize) {
		return helperCountBlocksBB(searchBlock, maxCount, w, bPos, boxSize, boxSize); // "square" box subcase
	}

	public int helperCountBlocksBB(Block searchBlock, int maxCount, Level w, BlockPos bPos, int boxSize, int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;

		MutableBlockPos mPos = new MutableBlockPos();
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

		MyConfig.debugMsg(1, bPos,
				searchBlock.getRegistryName().toString() + " Sparse count:" + count + " countBlockBB ");

		return count;
	}

	public int helperCountBlocksBB(Class<? extends Block> searchBlock, int maxCount, Level w, BlockPos bPos,
			int boxSize) {
		return helperCountBlocksBB(searchBlock, maxCount, w, bPos, boxSize, 0);
	}

	public int helperCountBlocksBB(Class<? extends Block> searchBlock, int maxCount, Level w, BlockPos bPos,
			int boxSize, int ySize) {
		int count = 0;
		int minX = bPos.getX() - boxSize;
		int maxX = bPos.getX() + boxSize;
		int minZ = bPos.getZ() - boxSize;
		int maxZ = bPos.getZ() + boxSize;
		int minY = bPos.getY() - ySize;
		int maxY = bPos.getY() + ySize;

		MutableBlockPos mPos = new MutableBlockPos();

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

		MyConfig.debugMsg(1, bPos, searchBlock.getSimpleName() + " Sparse count:" + count + " countBlockBB ");

		return count;
	}

	private boolean isGoodMushroomTemperature(Entity entity) {
		BlockPos ePos = getAdjustedBlockPos(entity);
		float biomeTemp = entity.level.getBiome(ePos).value().getBaseTemperature();
		MyConfig.debugMsg(1, ePos, "Mushroom Biome temp: " + biomeTemp + ".");
		if (biomeTemp < MyConfig.getMushroomMinTemp())
			return false;
		if (biomeTemp > MyConfig.getMushroomMaxTemp())
			return false;
		return true;
	}

	private boolean mobEatPlantsAction(Entity entity, String key, String regrowthType) {
		if (mobEatGrassOrFlower(entity, regrowthType)) {
			MyConfig.debugMsg(1, getAdjustedBlockPos(entity), key + " ate plants.");
			return true;
		}
		return false;
	}

	private boolean isHorseTypeEatingNow(Entity entity) {
		if (entity instanceof AbstractHorse) {
			AbstractHorse h = (AbstractHorse) entity;
			if (h.isEating()) {
				return true;
			}
		}
		return false;
	}

	private void mobStumbleAction(Entity entity, String key) {
		entity.level.destroyBlock(getAdjustedBlockPos(entity), true);
		MyConfig.debugMsg(1, getAdjustedBlockPos(entity), key + " stumbled over torch.");
	}

	private void doVillagerRegrowthEvents(Villager ve, String debugKey, String regrowthActions) {

		// Villagers hopping, falling, etc. are doing improvements.
		if (!(ve.isOnGround()))
			return;
		if (groundBlockState.getBlock() instanceof TorchBlock)
			return;
		if (groundBlockState.getBlock() instanceof WallTorchBlock)
			return;

		// Give custom debugging names to nameless villagers.
		if (MyConfig.aDebugLevel > 0) {
			Component tName = new TextComponent("");
			float veYaw = ve.getViewYRot(1.0f);
			tName = new TextComponent("Reg-" + ve.getX() + "," + ve.getZ() + ": " + veYaw);
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
			MyConfig.debugMsg(1, ve.blockPosition(), debugKey + " farm improved.");
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
			if ((footBlock instanceof TallGrassBlock) || (footBlock instanceof DoublePlantBlock)
					|| (footBlock.getDescriptionId().equals("block.byg.short_grass"))) {

				ve.level.destroyBlock(ve.blockPosition(), false);
				MyConfig.debugMsg(1, ve.blockPosition(), debugKey + " grass cut.");
			}
		}
		// improve roads
		// to do - replace "r" with a meaningful constant.f
		if (regrowthActions.contains("r")) {
			MyConfig.debugMsg(1, ve.blockPosition(), debugKey + " try road improve.");
			vImproveRoads(ve, debugKey);
		}

		// note villages may not have a meeting place. Sometimes they change. Sometimes
		// they take a few minutes to form.
		if ((regrowthActions.contains("w"))) {
			MyConfig.debugMsg(1, ve.blockPosition(), " try town wall build.");
			vImproveWalls(ve, debugKey, regrowthActions);

		}

		if ((regrowthActions.contains("p"))) {
			MyConfig.debugMsg(1, ve.blockPosition(), " try personal fence build.");
			vImproveFences(ve, debugKey, regrowthActions);

		}

		if ((regrowthActions.contains("t") && (footBlock != Blocks.TORCH))) {
			if (vImproveLighting(ve)) {
				MyConfig.debugMsg(1, ve.blockPosition(), debugKey + "-" + footBlock + ", " + groundBlock + " pitch: "
						+ ve.getXRot() + " lighting improved.");
			}
		}
	}

	private void helperJumpAway(Entity e) {
		// "jump" villagers away if they are inside a wall, fence, or dirtPath block.
		Block postActionFootBlock = getAdjustedFootBlockState(e).getBlock();
		if (postActionFootBlock == Blocks.DIRT_PATH) {
			e.setDeltaMovement(0, 0.33, 0);
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

	private boolean mobEatGrassOrFlower(Entity entity, String regrowthType) {

		BlockPos ePos = getAdjustedBlockPos(entity);
		if (!(isGrassOrFlower(footBlockState))) {
			return false;
		}
		if (isKindOfGrassBlock(groundBlockState)) {
			mobTrodGrassBlock(entity);
		}
		entity.level.destroyBlock(ePos, false);
		LivingEntity le = (LivingEntity) entity;
		helperChildAgeEntity(entity);
		if (le.getMaxHealth() > le.getHealth() && (MyConfig.aEatingHeals == 1)) {
			MobEffectInstance ei = new MobEffectInstance(MobEffects.HEAL, 1, 0, false, true);
			le.addEffect(ei);
		}
		return true;
	}

	private void mobTrodGrassBlock(Entity e) {

		BlockPos ePos = new BlockPos(e.getX(), e.getY(), e.getZ());
		if (e.level instanceof ServerLevel varLevel) {
			AABB aabb = new AABB(ePos.east(2).above(2).north(2), ePos.west(2).below(2).south(2));
			List<Entity> l = new ArrayList<>();
			varLevel.getEntities().get(e.getType(), aabb, (entity) -> {
				l.add(entity);
			});
			if (l.size() >= 9) {
				varLevel.setBlockAndUpdate(ePos.below(), Blocks.DIRT_PATH.defaultBlockState());
				e.hurt(DamageSource.IN_WALL, 0.25f);
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
		if (ent instanceof AgeableMob) {
			AgeableMob aEnt = (AgeableMob) ent;
			if (aEnt.isBaby()) {
				aEnt.setAge(aEnt.getAge() + 30);
			}
		}
	}

	private boolean mobGrowTallAction(Entity ent, String key) {
		if (footBlock instanceof TallGrassBlock) {
			BlockPos ePos = getAdjustedBlockPos(ent);
			try {
				BonemealableBlock ib = (BonemealableBlock) footBlock;
				ib.performBonemeal((ServerLevel) ent.level, ent.level.random, ePos, ent.level.getBlockState(ePos));
				MyConfig.debugMsg(1, ePos, key + " grew and hid in tall plant.");			
				return false;

			} catch (Exception e) {
				MyConfig.debugMsg(1, ePos, key + " caught grow attempt exception.");
				return false;
			}
		}
		return false;
	}

	private BlockState helperBiomeRoadBlockType(Biome localBiome) {

		BlockState gateBlockType = Blocks.DIRT_PATH.defaultBlockState();

		if (biomeCategory == Biome.BiomeCategory.DESERT) {
			gateBlockType = Blocks.ACACIA_PLANKS.defaultBlockState(); // 16.1 mojang change
		}
		return gateBlockType;
	}

	// if a grassblock in village has farmland next to it on the same level- retill
	// it.
	// todo add hydration check before tilling land.
	private boolean vImproveFarm(Villager ve, String regrowthType) {
		if (ve.getVillagerData().getProfession() != VillagerProfession.FARMER) {
			return false;
		}

		BlockPos vePos = getAdjustedBlockPos(ve);
		Block groundBlock = groundBlockState.getBlock();
		Block footBlock = footBlockState.getBlock();

		if (helperCountBlocksOrthogonalBB(Blocks.FARMLAND, 1, ve.level, vePos.below(1), 0) > 0) {
			if (isNearWater(ve.level, vePos.below(1))) {
				if (groundBlock instanceof GrassBlock) {
					ve.level.setBlockAndUpdate(vePos.below(), Blocks.FARMLAND.defaultBlockState());
					return true;
				}
			}

			if (!regrowthType.contains("t") || (footBlock != Blocks.AIR)) {
				return false;
			}

			// Special farm lighting torches.
			if (ve.level.getBrightness(LightLayer.BLOCK, vePos) > 12) {
				return false; // block already bright enough
			}

			int veX = vePos.getX();
			int veY = vePos.getY();
			int veZ = vePos.getZ();

			if ((lastTorchX == veX) && (lastTorchY == veY) && (lastTorchZ == veZ)) {
				return false; // Anti torch-exploit
			}

			boolean placeTorch = false;
			int waterValue = helperCountBlocksOrthogonalBB(Blocks.WATER, 1, ve.level, vePos.below(), 0);
			if ((waterValue > 0) && (groundBlockState.is(BlockTags.LOGS)) || (groundBlock == Blocks.SMOOTH_SANDSTONE)) {
				ve.level.setBlock(vePos, Blocks.TORCH.defaultBlockState(), 3);
				lastTorchX = veX;
				lastTorchY = veY;
				lastTorchZ = veZ;
				return true;
			}
		}
		return false;
	}

	private void vBeeKeeperFlowers(Villager ve) {
		if (!ve.getVillagerData().getProfession().getName().contains("beekeeper")) {
			return;
		}
		if ((ve.getX() % 6 == 0) && (ve.getZ() % 7 == 0)) {
			if (isBlockGrassOrDirt(groundBlockState)) {
				if (helperCountBlocksOrthogonalBB(Blocks.DIRT_PATH, 1, ve.level, ve.blockPosition().below(), 0) == 1) {
					BlockState flowerBlockState = Blocks.AZURE_BLUET.defaultBlockState();
					ve.level.setBlockAndUpdate(adjustedPos, flowerBlockState);
				}
			}
		}
	}

	private void vToolMasterHealing(Villager ve) {

		if (ve.getVillagerData().getProfession() != VillagerProfession.TOOLSMITH) {
			return;
		}
		long daytime = ve.level.getDayTime() % 24000;

		if (daytime < 9000 || daytime > 11000) {
			return;
		}

		if (ve.level instanceof ServerLevel varW) {
			int villagerLevel = ve.getVillagerData().getLevel();
			if (villagerLevel < 1)
				return;
			BlockPos vePos = new BlockPos(ve.getX(), (ve.getY() + 0.99d), (ve.getZ()));
			AABB aabb = new AABB(vePos.east(6).above(3).north(6), vePos.west(6).below(2).south(6));
			List<Entity> l = new ArrayList<>();
			varW.getEntities().get(aabb, (entity) -> {
				if (entity instanceof IronGolem) {
					l.add(entity);
				}
			});
			for (Entity e : l) {
				boolean heal = true;
				LivingEntity le = (LivingEntity) e;
				if (le.getHealth() < le.getMaxHealth()) {
					if (heal) {
						le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, villagerLevel * 51, 0), ve);
						ve.addEffect(new MobEffectInstance(MobEffects.REGENERATION, villagerLevel * 11, 0), ve);
						ve.level.playSound(null, vePos, SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.NEUTRAL, 0.5f,
								0.5f);
						return;
					}
				}
			}
		}
	}

	private void vClericalHealing(Villager ve) {

		if (ve.getVillagerData().getProfession() != VillagerProfession.CLERIC) {
			return;
		}
		long daytime = ve.level.getDayTime() % 24000;

		if (daytime < 9000 || daytime > 11000) {
			return;
		}
		if (ve.level instanceof ServerLevel varW) {
			int clericalLevel = ve.getVillagerData().getLevel();

			BlockPos vePos = new BlockPos(ve.getX(), (ve.getY() + 0.99d), (ve.getZ()));
			AABB aabb = new AABB(vePos.east(4).above(2).north(4), vePos.west(4).below(2).south(4));
			List<Entity> l = new ArrayList<>();
			varW.getEntities().get(aabb, (entity) -> {
				if (entity instanceof Villager || entity instanceof Player) {
					l.add(entity);
				}
			});

			for (Entity e : l) {
				boolean heal = true;
				LivingEntity le = (LivingEntity) e;
				if (le.getHealth() < le.getMaxHealth()) {
					if (e instanceof Player pe) {
						int rep = ve.getPlayerReputation(pe);
						if (rep < 0) { // I was a bad bad boy.
							heal = false;
						}
					}
					if (heal) {
						le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, clericalLevel * 51, 0), ve);
						ve.level.playSound(null, vePos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.2f,
								1.52f);
						return;
					}
				}
			}
		}
	}

	private void vImproveLeaves(Villager ve, String key) {

		int partialBlockAdjust = 0;

		float veYaw = ve.getViewYRot(1.0f) / 45;

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
			tempBS = ve.level.getBlockState(tmpBP);
			tempBlock = tempBS.getBlock();
			if (tempBlock instanceof LeavesBlock) {
				boolean persistantLeaves = tempBS.getValue(LeavesBlock.PERSISTENT);
				if (!(persistantLeaves)) {
					destroyBlock = true;
				}
			}
			if ((tempBlock instanceof CactusBlock)) {
				destroyBlock = true;
			}
			if (destroyBlock) {
				ve.level.destroyBlock(tmpBP, false);
				destroyBlock = false;
				MyConfig.debugMsg(1, vePos, key + " cleared " + tempBlock.getDescriptionId().toString());
			}
		}
	}

	private boolean vImproveLighting(Villager ve) {
		BlockPos vePos = getAdjustedBlockPos(ve);

		int blockLightValue = ve.level.getBrightness(LightLayer.BLOCK, vePos);
		int skyLightValue = ve.level.getBrightness(LightLayer.SKY, vePos);

		if (blockLightValue > MyConfig.getTorchLightLevel())
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
			ve.level.setBlock(vePos, Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
		}

		return true;

	}

	private void vImproveRoads(Villager ve, String debugkey) {

		TextComponent tName = new TextComponent("-");
//		ve.setCustomName(tName);

		isRoadPiece = false;

		boolean isInsideStructurePiece = false;
		boolean test = true;
		BlockPos piecePos = null;
		List<StructureStart> sList = new ArrayList<StructureStart>();
		if (test) {
			ChunkPos c = new ChunkPos(ve.blockPosition());
// lupexp	List<StructureStart> x2 = getStarts(world, StructureFeature.VILLAGE, 0, 0);
			sList = getStarts(ve.level, StructureFeature.VILLAGE, c.x, c.z);
		}
		if (!sList.isEmpty()) {

			for (StructurePiece piece : sList.get(0).getPieces()) {
				piecePos = piece.getLocatorPosition();
				if (piece.getBoundingBox().isInside(ve.blockPosition())) {
					piecePos = piece.getLocatorPosition();
					// System.out.println("inside" + piece);
					if (piece.toString().contains("streets")) {
						isRoadPiece = true;
					}
					int i = piece.toString().indexOf("minecraft");
					if (i >= 0) {
						tName = new TextComponent(isRoadPiece + " " + piece.toString().substring(i));
					} else {
						i = piece.toString().indexOf("minecraft");
						if (i >= 0)
							tName = new TextComponent(isRoadPiece + " " + piece.toString().substring(i));

					}
//					ve.setCustomName(tName);
					isInsideStructurePiece = true;
					break;
				}
			}
		}

		if (vImproveRoadsClearSnow(ve)) {
			MyConfig.debugMsg(1, ve.blockPosition(), debugkey + " clear snow on road.");
		}

		if (vImproveRoadsFixPatches(ve)) {
			MyConfig.debugMsg(1, ve.blockPosition(), debugkey + " fix patches on road.");
		}
		if (vImproveRoadsFixPotholes(ve)) {
			MyConfig.debugMsg(1, ve.blockPosition(), debugkey + " fix potholes in road.");
		}
		if (vImproveRoadsSmoothHeight(ve)) {
			MyConfig.debugMsg(1, ve.blockPosition(), debugkey + " Smooth road slope.");
		}
	}

	private List<StructureStart> getStarts(LevelAccessor worldIn, StructureFeature<?> struct, int x, int z) {
		List<StructureStart> list = Lists.newArrayList();
		ChunkAccess ichunk = worldIn.getChunk(x, z, ChunkStatus.STRUCTURE_REFERENCES);

		for (Entry<ConfiguredStructureFeature<?, ?>, LongSet> r : ichunk.getAllReferences().entrySet()) {
			if (r.getKey().feature == struct) {
				LongIterator longiterator = r.getValue().iterator();
				while (longiterator.hasNext()) {
					long i = longiterator.nextLong();
					ChunkAccess istructurereader = worldIn.getChunk(ChunkPos.getX(i), ChunkPos.getZ(i),
							ChunkStatus.STRUCTURE_STARTS);
					StructureStart structurestart = istructurereader.getStartForFeature(r.getKey());
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
		if (footBlock instanceof SnowLayerBlock) {
			e.level.destroyBlock(adjustedPos, false);
			footBlockState = Blocks.AIR.defaultBlockState();
			footBlock = footBlockState.getBlock();
			return true;
		}
		return false;
	}

	// fix unfinished spots in road with 3-4 grass blocks orthogonal to them.
	// on slopes too.

	private boolean vImproveRoadsFixPatches(Entity e) {

		if (!e.level.canSeeSky(e.blockPosition())) {
			return false;
		}

		Block biomeRoadBlock = helperBiomeRoadBlockType(localBiome).getBlock();

		if (groundBlock == biomeRoadBlock)
			return false;

		int roadY = 0;
		int roadBlocks = 0;
		BlockPos vePos = getAdjustedBlockPos(e);
		for (int i = 0; i < 4; i++) {
			roadY = e.level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, vePos.getX() + dx[i], vePos.getZ() + dz[i]) - 1;
			Block tempBlock = e.level.getBlockState(new BlockPos(vePos.getX() + dx[i], roadY, vePos.getZ() + dz[i]))
					.getBlock();
			if (tempBlock == biomeRoadBlock) {
				roadBlocks++;
				if (roadBlocks >= 3) {
					if (footBlock instanceof SnowLayerBlock) {
						e.level.destroyBlock(adjustedPos, false);
						footBlockState = Blocks.AIR.defaultBlockState();
						footBlock = footBlockState.getBlock();
					}
					e.level.setBlockAndUpdate(adjustedPos.below(), biomeRoadBlock.defaultBlockState());
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

		if (!e.level.canSeeSky(e.blockPosition())) {
			return false;
		}

		Block biomeRoadBlock = helperBiomeRoadBlockType(localBiome).getBlock();
		if ((groundBlock == biomeRoadBlock) && (footBlock instanceof SnowLayerBlock)) {
			e.level.destroyBlock(getAdjustedBlockPos(e), false);
		}

		BlockPos vePos = e.blockPosition();

		int veX = vePos.getX();
		int veY = vePos.getY();
		int veZ = vePos.getZ();

		int roadY = 0;
		int higherRoadBlocks = 0;
		for (int i = 0; i < 4; i++) {
			roadY = e.level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, veX + dx[i], veZ + dz[i]) - 1;
			Block tempBlock = e.level.getBlockState(new BlockPos(veX + dx[i], roadY, veZ + dz[i])).getBlock();
			if (tempBlock == biomeRoadBlock) {
				if (roadY > veY) {
					higherRoadBlocks++;
				}
			}
		}
		if (higherRoadBlocks == 4) {
			e.level.setBlockAndUpdate(adjustedPos, biomeRoadBlock.defaultBlockState());
			return true;
		}
		return false;
	}

	private boolean vImproveRoadsSmoothHeight(Villager ve) {

		if (!ve.isOnGround()) {
			return false;
		}

		if (ve.isBaby()) {
			return false;
		}

		BlockPos vePos = getAdjustedBlockPos(ve);

		if (!ve.level.canSeeSky(ve.blockPosition())) {
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

		BlockState biomeRoadBlockState = biomeRoadBlock.defaultBlockState();

		// Check for nearby point of interests.
		int poiDistance = 3;
		if (Biome.getBiomeCategory(ve.level.getBiome(vePos)) == BiomeCategory.DESERT) {
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
			roadY = ve.level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, veX + dx[i], veZ + dz[i]) - 1;
			if (roadY > veY) {
				Block tempBlock = ve.level.getBlockState(new BlockPos(veX + dx[i], roadY, veZ + dz[i])).getBlock();
				if (tempBlock == biomeRoadBlock) {
					ve.level.setBlockAndUpdate(new BlockPos(veX, veY, veZ), biomeRoadBlockState);
					ve.setDeltaMovement(0.0, 0.4, 0.0);
					return true;
				}
			}
		}

		return false;
	}

	private boolean isNearbyPoi(Villager ve, Biome localBiome, BlockPos vePos, int poiDistance) {

		// 08/30/20 Collection pre 16.2 bug returns non empty collections.
		// the collection is not empty when it should be.
		// are returned in the collection so have to loop thru it manually.

		Collection<PoiRecord> result = ((ServerLevel) ve.level).getPoiManager()
				.getInSquare(t -> true, ve.blockPosition(), poiDistance, Occupancy.ANY)
				.collect(Collectors.toCollection(ArrayList::new));

		if (!(result.isEmpty())) {
			Iterator<PoiRecord> i = result.iterator();
			while (i.hasNext()) { // in 16.1, finds the point of interest.
				PoiRecord P = i.next();
				int disX = Math.abs(ve.blockPosition().getX() - P.getPos().getX());
				int disZ = Math.abs(ve.blockPosition().getZ() - P.getPos().getZ());
				if ((disX < poiDistance) && (disZ < poiDistance)) {
					MyConfig.debugMsg(1, vePos, "Point of Interest too Close: " + P.getPoiType().toString() + ".");
					return true;
				}
			}
		}
		return false;
	}

	private boolean vImproveVillageWall(Villager ve, String regrowthActions) {
		if (!(ve.getBrain().getMemory(MemoryModuleType.MEETING_POINT)).isPresent())
			return false;

		if (!isOkayToBuildWallHere(ve)) {
			return false;
		}
		
		BlockPos gVMPPos = ve.getBrain().getMemory(MemoryModuleType.MEETING_POINT).get().pos();

		if (MyConfig.playerWallControlBlock != Blocks.AIR) {
			if (ve.level.getChunkAt(gVMPPos).getInhabitedTime() < 200)  // Bell
				ve.level.setBlock(gVMPPos.above(1), MyConfig.playerWallControlBlock.defaultBlockState(), 3);
	
			if (ve.level.getBlockState(gVMPPos.above(1)).getBlock() != MyConfig.playerWallControlBlock) {
				return false;
			}
		}
		
		BlockPos vePos = getAdjustedBlockPos(ve);

		String key = "minecraft:" + biomeCategory.toString();
//		ResourceLocation biomeName = ForgeRegistries.BIOMES.getKey(localBiome);
		key = key.toLowerCase();
		MyConfig.debugMsg(2, vePos, key + " wall improvement.");

		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		MyConfig.debugMsg(1, vePos, key + " biome for wall improvement. ");

		int wallRadius = currentWallBiomeDataItem.getWallDiameter();

		wallRadius = (wallRadius / 2) - 1;

		if (isOnWallPerimeter(ve, wallRadius, gVMPPos)) {
			MyConfig.debugMsg(2, ve.blockPosition(), "villager on wall perimeter: " + wallRadius);
			// check for other meeting place bells blocking wall since too close.
			Collection<PoiRecord> result = ((ServerLevel) ve.level).getPoiManager()
					.getInSquare(t -> t == PoiType.MEETING, ve.blockPosition(), 41, Occupancy.ANY)
					.collect(Collectors.toCollection(ArrayList::new));

			// 08/30/20 Collection had bug with range that I couldn't resolve.
			boolean buildWall = true;
			if (!(result.isEmpty())) {
				Iterator<PoiRecord> i = result.iterator();
				while (i.hasNext()) { // in 16.1, finds the point of interest.
					PoiRecord P = i.next();
					if ((gVMPPos.getX() == P.getPos().getX()) && (gVMPPos.getY() == P.getPos().getY())
							&& (gVMPPos.getZ() == P.getPos().getZ())) {
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
				BlockState wallTypeBlockState = currentWallBiomeDataItem.getWallBlockState();

				BlockState wallBlock = wallTypeBlockState;
				BlockState gateBlockType = helperBiomeRoadBlockType(localBiome);

				int wallTorchSpacing = (wallRadius + 1) / 4;
				if (helperPlaceOneWallPiece(ve, wallRadius, wallTorchSpacing, gateBlockType, wallBlock, gVMPPos)) {
					if (regrowthActions.contains("t")) {
						int absvx = (int) Math.abs(ve.getX() - gVMPPos.getX());
						int absvz = (int) Math.abs(ve.getZ() - gVMPPos.getZ());
						if (isValidTorchLocation(wallRadius, wallTorchSpacing, absvx, absvz,
								ve.level.getBlockState(vePos).getBlock())) {
							ve.level.setBlockAndUpdate(vePos.above(), Blocks.TORCH.defaultBlockState());
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
	private boolean vImproveHomeFence(Villager ve, BlockPos vHomePos, String regrowthActions) {

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

		Collection<PoiRecord> result = ((ServerLevel) ve.level).getPoiManager()
				.getInSquare(t -> t == PoiType.HOME, vePos, 17, Occupancy.ANY)
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
					MyConfig.debugMsg(1, P.getPos(), "extra Point of Interest Found.");
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
							ve.level.getBlockState(vePos).getBlock())) {
						ve.level.setBlockAndUpdate(vePos.above(), Blocks.TORCH.defaultBlockState());
					}
				}
				helperJumpAway(ve);
				return true;
			}
		}

		return false;
	}

	private void vImproveWalls(Villager ve, String key, String regrowthType) {

		if (groundBlockState.isAir()) {
			return; // ignore edge cases where villager is hanging on the edge of a block.
		}
		BlockPos vePos = getAdjustedBlockPos(ve);

		if (!(ve.getBrain().getMemory(MemoryModuleType.MEETING_POINT)).isPresent())
			return;

		MyConfig.debugMsg(1, vePos, "Checking Improve Wall.");
		if (vImproveVillageWall(ve, regrowthType)) {
			MyConfig.debugMsg(1, vePos, "Meeting Wall Improved.");
		}
	}

	private void vImproveFences(Villager ve, String key, String regrowthType) {

		BlockPos ePos = ve.blockPosition();

		Brain<Villager> vb = ve.getBrain();
		Optional<GlobalPos> vMeetingPlace = vb.getMemory(MemoryModuleType.MEETING_POINT);
		if (!(vMeetingPlace.isPresent())) {
			return;
		}

		if (isOkayToBuildWallHere(ve)) {
			GlobalPos gVMP = vMeetingPlace.get();
			BlockPos villageMeetingPlaceBlockPos = gVMP.pos();

			if (!(ve.level.getBlockState(villageMeetingPlaceBlockPos.above(1)).getBlock() instanceof WallBlock)) {
				return;
			}

			// build a wall on perimeter of villager's home
			if (regrowthType.contains("p")) {
				MyConfig.debugMsg(1, ePos, "Checking Improve Fence.");

				Optional<GlobalPos> villagerHome = vb.getMemory(MemoryModuleType.HOME);
				if (!(villagerHome.isPresent())) {
					return;
				}
				GlobalPos gVHP = villagerHome.get();
				BlockPos villagerHomePos = gVHP.pos();
				// don't build personal walls inside the village wall perimeter.
				// don't build personal walls until the village has a meeting place.
				if (isOutsideMeetingPlaceWall(ve, vMeetingPlace, vMeetingPlace.get().pos(), localBiome)) {
					MyConfig.debugMsg(1, ePos, "Outside meeting place wall.");
					if (vImproveHomeFence(ve, villagerHomePos, regrowthType)) {
						MyConfig.debugMsg(1, ePos, "Home Fence Improved.");
					}
				}
			}
		}
	}

	private boolean isFootBlockOkayToBuildIn(BlockState footBlockState) {
		if ((footBlockState.isAir()) || (isGrassOrFlower(footBlockState))) {
			return true;
		}
		if (footBlockState.getBlock() instanceof SnowLayerBlock) {
			return true;
		}
		return false;
	}

	private boolean isGrassOrFlower(BlockState footBlockState) {
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
			if (MyConfig.aDebugLevel > 0) {
				System.out.println("Tag Exception 1009-1014:" + footBlock.getDescriptionId() + ".");
			}
		}
		// biomes you'll go grass compatibility
		if (footBlock.getDescriptionId().equals("block.byg.short_grass")) {
			return true;
		}
		if (MyConfig.aDebugLevel > 0) {
			System.out.println("Not grass or Flower:" + footBlock.getDescriptionId() + ".");
		}
		return false;
	}

	private boolean isImpossibleRegrowthEvent(String regrowthType) {
		if ((regrowthType.equals("eat")) && (footBlockState.isAir())) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlockState.getBlock() instanceof TallGrassBlock)) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlockState.getBlock() instanceof FlowerBlock)) {
			return true;
		}
		if ((regrowthType.equals("tall")) && (!(footBlockState.getBlock() instanceof TallGrassBlock))) {
			return true;
		}
		return false;
	}

	private boolean isOkayToBuildWallHere(Villager ve) {

		BlockPos ePos = ve.blockPosition();

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

	private boolean isOutsideMeetingPlaceWall(Villager ve, Optional<GlobalPos> vMeetingPlace, BlockPos meetingPlacePos,
			Biome localBiome) {

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

	private boolean isValidGroundBlockToPlaceTorchOn(Villager ve) {

		String key = groundBlockState.getBlock().getRegistryName().toString(); // broken out for easier debugging
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(key);
		if (currentWallFoundationItem == null)
			return false;

		return true;

	}

	private boolean isValidGroundBlockToBuildWallOn(Entity e) {

		if (e.level.getBrightness(LightLayer.SKY, e.blockPosition()) < 13)
			return false;

		if (groundBlock instanceof SnowLayerBlock)
			return false;
		if (groundBlock instanceof TorchBlock)
			return false; // includes WallTorchBlock

		if (e.level.getBlockState(e.blockPosition().above()).getBlock() instanceof WallBlock) {
			return false;
		}
		if (e.level.getBlockState(e.blockPosition().below()).getBlock() instanceof WallBlock) {
			return false;
		}
		if (e.level.getBlockState(e.blockPosition().below(1)).getBlock() instanceof WallBlock) {
			return false;
		}
		if (e.level.getBlockState(e.blockPosition().below(2)).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (e.level.getBlockState(e.blockPosition().above()).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (e.level.getBlockState(e.blockPosition().below()).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (e.level.getBlockState(e.blockPosition().below(1)).getBlock() instanceof TorchBlock) {
			return false;
		}
		if (e.level.getBlockState(e.blockPosition().below(2)).getBlock() instanceof TorchBlock) {
			return false;
		}
		BlockState testBlockState = getAdjustedGroundBlockState(e);
		groundBlock = groundBlockState.getBlock();

		MyConfig.debugMsg(1, e.blockPosition(),
				"Build Wall : gb" + groundBlock.toString() + ", fb:" + footBlock.toString());
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(groundBlock.getRegistryName().toString());

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

		if (footBlock instanceof SnowLayerBlock) {
			e.level.destroyBlock(vePos, false);
		}

		if ((footBlock instanceof SaplingBlock) || (footBlock instanceof TallGrassBlock)
				|| (footBlock instanceof FlowerBlock) || (footBlock instanceof DoublePlantBlock)) {
			e.level.destroyBlock(vePos, true);
		}

		if (absva == WALL_CENTER) {
			e.level.setBlockAndUpdate(vePos.below(), gateBlockType);
			return true;
		}

		if (e.level.setBlockAndUpdate(vePos, wallType)) {
			return true;
		} else {
			MyConfig.debugMsg(1, e.blockPosition(),
					"Building Wall Fail: SetBlockAndUpdate Time End = " + e.level.getGameTime());
			return false;
		}

	}

}
