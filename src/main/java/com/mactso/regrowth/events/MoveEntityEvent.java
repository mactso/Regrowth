package com.mactso.regrowth.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import com.mactso.regrowth.config.WallBiomeDataManager;
import com.mactso.regrowth.config.WallFoundationDataManager;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.FeatureAccess;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.FarmlandWaterManager;
//import net.minecraftforge.common.util.Constants.BlockFlags;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.world.BlockEvent.FarmlandTrampleEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber()
public class MoveEntityEvent {

	private static int[] dx = { 1, 0, -1, 0 };
	private static int[] dz = { 0, 1, 0, -1 };
	private static int TICKS_PER_SECOND = 20;
	private static int[][] facingArray = { { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 }, { 0, -1 }, { 1, -1 }, { 1, 0 },
			{ 1, 1 } };
	private static int lastTorchX = 0;
	private static int lastTorchY = 0;
	private static int lastTorchZ = 0;
	static final int WALL_CENTER = 0;
	static final int FENCE_CENTER = 0;
	static final int WALL_TYPE_WALL = -1;
	static final int WALL_TYPE_FENCE = -2;

	
	
	@SubscribeEvent 
	public void handleTrampleEvents (FarmlandTrampleEvent event) {
		BlockPos pos = event.getEntity().blockPosition();
		MyConfig.debugMsg(1, pos, "Enter FarmlandTrampleEvent");
		if (event.isCancelable()) {
			if (event.getEntity() instanceof Villager ve) {
				if (ve.getVillagerData().getProfession() != VillagerProfession.FARMER) {
					MyConfig.debugMsg(2, pos, "Villager Not A Farmer");
					return;
				}
				if (ve.getVillagerData().getLevel() >=3 ) {
					event.setCanceled(true);
					MyConfig.debugMsg(2, pos, "Farmer under level 3.");
					return;
				}
			}
			if ((event.getEntity() instanceof ServerPlayer spe)) {
				if (!spe.isCreative() ) {
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
		if (entity instanceof Player) {
			return;
		}

		if (!(entity.level instanceof ServerLevel)) {
			return;
		}

		ServerLevel sWorld = (ServerLevel) entity.level;

		BlockPos ePos = getAdjustedBlockPos(entity);

		Block footBlock = sWorld.getBlockState(ePos).getBlock();
		if (footBlock instanceof WoolCarpetBlock) {
			return;
		}
		if (footBlock == Blocks.SNOW) { // ignore snow layers
			footBlock = Blocks.AIR;
		}
		Block groundBlock = sWorld.getBlockState(ePos.below()).getBlock();

		if (entity.isOnGround() && footBlock == Blocks.AIR && groundBlock == Blocks.AIR) {
			// literal edge case... standing with less than half of hit box on a higher
			// block).
			return;
		}

		Biome localBiome = sWorld.getBiome(ePos);
		EntityType<?> tempType = entity.getType();
		ResourceLocation registryName = tempType.getRegistryName();
		String registryNameAsString = registryName.toString();
		RegrowthEntitiesManager.RegrowthMobItem currentRegrowthMobItem = RegrowthEntitiesManager
				.getRegrowthMobInfo(registryNameAsString);

		if (currentRegrowthMobItem == null) { // This mob isn't configured to do Regrowth Events
			return;
		}

		String regrowthActions = currentRegrowthMobItem.getRegrowthActions();

		if (isImpossibleRegrowthEvent(footBlock, regrowthActions)) {
			return;
		}

		double regrowthEventOdds = 1 / (currentRegrowthMobItem.getRegrowthEventSeconds() * TICKS_PER_SECOND);
		if (isHorseTypeEatingNow(entity)) {
			regrowthEventOdds *= 20;
		}
		double randomD100Roll = entity.level.random.nextDouble() ;
		int debugvalue = 0	; // TODO make sure value 0 after debugging.

		if (randomD100Roll <= regrowthEventOdds + debugvalue) {
			if (entity instanceof Villager) {
//				System.out.println("GameTime:"+((AbstractVillager) entity).getLevel().getGameTime() + " : Roll:" + randomD100Roll + " < " + regrowthEventOdds);
				Villager ve = (Villager) entity;
				if (ve.isInPowderSnow) {
					Block a = sWorld.getBlockState(ve.blockPosition().above(2)).getBlock();
					Block b = sWorld.getBlockState(ve.blockPosition().above()).getBlock();
					Block c = sWorld.getBlockState(ve.blockPosition()).getBlock();
					if (b != Blocks.AIR) {
						int x = 1;
					}
					int hp = 0;
					if (sWorld.getBlockState(ve.blockPosition().above(2)).getBlock() == Blocks.POWDER_SNOW) {
						entity.level.destroyBlock(entity.blockPosition().above(2), false);
						hp = 2;
					}
					if (sWorld.getBlockState(ve.blockPosition().above()).getBlock() == Blocks.POWDER_SNOW) {
						entity.level.destroyBlock(entity.blockPosition().above(), false);
						hp += 2;
					}
					if (sWorld.getBlockState(ve.blockPosition()).getBlock() == Blocks.POWDER_SNOW) {
						BlockState SnowLayer = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2 + hp);
						entity.level.setBlockAndUpdate(ve.blockPosition(), SnowLayer);
					}
				}
				// if onGround
				if ((ve.isOnGround()) && (!(footBlock instanceof BedBlock))) {
					doVillagerRegrowthEvents(ve, footBlock, groundBlock, registryNameAsString, regrowthActions,
							localBiome);
				}
			} else {
				doMobRegrowthEvents(entity, footBlock, groundBlock, registryNameAsString, regrowthActions, localBiome);
			}
		}

	}

	// this round partial height blocks up.
	private static BlockPos getAdjustedBlockPos(Entity entity) {
		BlockPos ePos = new BlockPos(entity.getX(), (entity.getY() + 0.99d), (entity.getZ()));
		return ePos;
	}

	private void doMobRegrowthEvents(Entity entity, Block footBlock, Block groundBlock, String key, String regrowthType,
			Biome localBiome) {

		if (regrowthType.equals("stumble")) {
			if ((footBlock instanceof TorchBlock) || (footBlock instanceof WallTorchBlock)) {
				mobStumbleAction(entity, key);
			}
			return;
		}

		if (regrowthType.equals("reforest")) {
			mobReforestAction(entity, footBlock, groundBlock, key, localBiome);
			return;
		}

		if (regrowthType.equals("mushroom")) {
			mobGrowMushroomAction(entity, groundBlock, key);
			return;
		}

		// all remaining actions currently require a grass block underfoot so if not a
		// grass block- can exit now.
		// this is for performance savings only.
		// looks like meadow_grass_block is not a grassBlock

		if (!isKindOfGrassBlock(groundBlock)) {
			return;
		}

		if (regrowthType.equals("tall")) {
			mobGrowTallAction(entity, footBlock, key);
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
			mobEatPlantsAction(entity, footBlock, groundBlock, key, regrowthType);
			return;
		}

		if ((regrowthType.equals("grow"))) {
			mobGrowPlantsAction(entity, footBlock, groundBlock, key);
			return;
		}
	}

	private boolean mobGrowPlantsAction(Entity entity, Block footBlock, Block groundBlock, String key) {
		if (footBlock instanceof AirBlock) {
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
				Random rand = entity.level.random;
				BlockState bs = entity.level.getBlockState(bpos);
				ib.performBonemeal(serverworld, rand, bpos, bs);
				MyConfig.debugMsg(1, bpos, key + " grew plant.");
			} catch (Exception e) {
				MyConfig.debugMsg(1, bpos, key + " caught grow attempt exception.");
			}
		}
		return true;
	}

	   private static boolean isNearWater(LevelReader level, BlockPos pos) {
		      for(BlockPos blockpos : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
		         if (level.getFluidState(blockpos).is(FluidTags.WATER)) {
		            return true;
		         }
		      }
		      // compatability other mods which are not water but hydrate.
		      return FarmlandWaterManager.hasBlockWaterTicket(level, pos);
	   }
	
	
	private static boolean isKindOfGrassBlock(Block groundBlock) {
		if (groundBlock instanceof GrassBlock)
			return true;
		if (groundBlock.getDescriptionId().equals("block.byg.meadow_grass_block"))
			return true;
		return false;
	}

	private static boolean isBlockGrassOrDirt(Block tempBlock) {

		if (isKindOfGrassBlock(tempBlock) || (tempBlock == Blocks.DIRT)) {
			return true;
		}
		return false;
	}

	private BlockState helperSaplingState(Level world, BlockPos pos, Biome localBiome, BlockState sapling) {
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

	private void mobReforestAction(Entity entity, Block footBlock, Block groundBlock, String key, Biome localBiome) {
		if (footBlock != Blocks.AIR)
			return;

		if (!(isBlockGrassOrDirt(groundBlock)))
			return;

		BlockPos ePos = getAdjustedBlockPos(entity);
		int eX = ePos.getX();
		int eY = ePos.getY();
		int eZ = ePos.getZ();
		// only try to plant saplings in about 1/4th of blocks.

		double sinY = Math.sin((double) ((eY + 64) % 256) / 256);

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

	private void mobGrowMushroomAction(Entity entity, Block groundBlock, String key) {
		BlockPos ePos = getAdjustedBlockPos(entity);
		ServerLevel sWorld = (ServerLevel) entity.level;

		if (sWorld.getBlockState(ePos).getBlock() instanceof MushroomBlock) {
			return;
		}

		if (sWorld.canSeeSky(ePos)) {
			return;
		}
		if (!(isGoodMushroomTemperature(entity))) {
			return;
		}

		Random mushRand = new Random(helperLongRandomSeed(ePos));
		
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
			if (groundBlock == Blocks.STONE || groundBlock == Blocks.DIORITE
					|| groundBlock == Blocks.ANDESITE || groundBlock == Blocks.GRANITE) {
				growMushroom = true;
			}
		} else {
			if (!(BlockTags.BASE_STONE_OVERWORLD.contains(groundBlock))) {
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
			if (w.getBlockState(bPos.above(j).east(1)).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.above(j).west(1)).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.above(j).north(1)).getBlock() == searchBlock)
				count++;
			if (w.getBlockState(bPos.above(j).south(1)).getBlock() == searchBlock)
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

		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
				for (int dy = minY; dy <= maxY; dy++) {
					Block b = w.getBlockState(new BlockPos(dx, dy, dz)).getBlock();
					MyConfig.debugMsg(2, "dx:" + dx + ", dz:" + dz + ", dy:" + dy + "  Block:"
							+ b.getRegistryName().toString() + ", count:" + count);
					if (w.getBlockState(new BlockPos(dx, dy, dz)).getBlock() == searchBlock)
						count++;
					if (count >= maxCount)
						return count;
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

		for (int dx = minX; dx <= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
				for (int dy = minY; dy <= maxY; dy++) {
					Block b = w.getBlockState(new BlockPos(dx, dy, dz)).getBlock();
					MyConfig.debugMsg(2, "dx:" + dx + ", dz:" + dz + ", dy:" + dy + "  Block:"
							+ b.getRegistryName().toString() + ", count:" + count);
					if (searchBlock.isInstance(w.getBlockState(new BlockPos(dx, dy, dz)).getBlock())) {
						count++;
					}
					if (count >= maxCount) {
						return count;
					}
				}
			}
		}

		MyConfig.debugMsg(1, bPos, searchBlock.getSimpleName() + " Sparse count:" + count + " countBlockBB ");

		return count;
	}

	private boolean isGoodMushroomTemperature(Entity entity) {
		BlockPos ePos = getAdjustedBlockPos(entity);
		float biomeTemp = entity.level.getBiome(ePos).getBaseTemperature();
		MyConfig.debugMsg(1, ePos, "Mushroom Biome temp: " + biomeTemp + ".");
		if (biomeTemp < MyConfig.getMushroomMinTemp())
			return false;
		if (biomeTemp > MyConfig.getMushroomMaxTemp())
			return false;
		return true;
	}

	private boolean mobEatPlantsAction(Entity entity, Block footBlock, Block groundBlock, String key,
			String regrowthType) {
		if (mobEatGrassOrFlower(entity, regrowthType, footBlock, groundBlock)) {
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

	private void doVillagerRegrowthEvents(Villager ve, Block footBlock, Block groundBlock, String key,
			String regrowthType, Biome localBiome) {

		BlockPos ePos = getAdjustedBlockPos(ve);

		int veX = ePos.getX(); // Int
		int veY = ePos.getY(); // Int
		int veZ = ePos.getZ(); // Int
		// Villagers hopping, falling, etc. are doing improvements.
		if (!(isOnGround(ve))) {
			return;
		}
		// Give custom debugging names to nameless villagers.
		if (MyConfig.aDebugLevel > 0) {
			Component tName = new TextComponent("");
			float veYaw = ve.getViewYRot(1.0f);
			tName = new TextComponent("Reg-" + veX + "," + veZ + ": " + veYaw);
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
		if (vImproveFarm(ve, groundBlock, footBlock, regrowthType)) {
			MyConfig.debugMsg(1, ePos, key + " farm improved.");
		}
		;

		// 'h'eal villagers and players
		if (regrowthType.contains("h")) {
			vClericalHealing(ve);
		}

		
		// cut lea'v'es.
		// remove leaves if facing head height leaves

		if (regrowthType.contains("v")) {
			vImproveLeaves(ve, groundBlock, key, veX, veY, veZ);
		}

		// c = cut down grass (but not flowers for now)
		// to do - maybe remove flowers unless by a road or elevated (air next to them
		// as in the flower beds)
		// to do - replace "c" with a meaningful constant.

		if (regrowthType.contains("c")) {
			if ((footBlock instanceof TallGrassBlock) || (footBlock instanceof DoublePlantBlock)
					|| (footBlock.getDescriptionId().equals("block.byg.short_grass"))) {

				ve.level.destroyBlock(ePos, false);
				MyConfig.debugMsg(1, ePos, key + " grass cut.");
			}
		}
		// improve roads
		// to do - replace "r" with a meaningful constant.f
		if (regrowthType.contains("r")) {
			MyConfig.debugMsg(1, ePos, key + " try road improve.");
			vImproveRoads(ve, footBlock, groundBlock, key, localBiome);
		}

		// note villages may not have a meeting place. Sometimes they change. Sometimes
		// they take a few minutes to form.
		if ((regrowthType.contains("w"))) {
			MyConfig.debugMsg(1, ePos, " try town wall build.");
			vImproveWalls(ve, footBlock, groundBlock, key, regrowthType, localBiome);
			helperJumpAway(ve, footBlock);
		}

		if ((regrowthType.contains("p"))) {
			MyConfig.debugMsg(1, ePos, " try personal fence build.");
			vImproveFences(ve, footBlock, groundBlock, key, regrowthType, localBiome);
			helperJumpAway(ve, footBlock);
		}

		if ((regrowthType.contains("t") && (footBlock != Blocks.TORCH))) {
			if (vImproveLighting(ve, footBlock, groundBlock, localBiome)) {
				MyConfig.debugMsg(1, ePos,
						key + "-" + footBlock + ", " + groundBlock + " pitch: " + ve.getXRot() + " lighting improved.");
			}
		}
	}

	private void helperJumpAway(Villager ve, Block footBlock) {
		// "jump" villagers away if they are inside a wall or fence block.
		if ((footBlock instanceof WallBlock) || (footBlock instanceof FenceBlock)) {
			float veYaw = ve.getViewYRot(1.0f) / 45;
			int facingNdx = Math.round(veYaw);
			if (facingNdx < 0) {
				facingNdx = Math.abs(facingNdx);
			}
			facingNdx %= 8;
			double dx = (facingArray[facingNdx][0]) / 2.0;
			double dz = (facingArray[facingNdx][1]) / 2.0;
			ve.setDeltaMovement(dx, 0.55, dz);
		}
	}

	private boolean mobEatGrassOrFlower(Entity entity, String regrowthType, Block footBlock, Block groundBlock) {

		BlockPos ePos = getAdjustedBlockPos(entity);
		if (!(isGrassOrFlower(footBlock))) {
			return false;
		}
		if (isKindOfGrassBlock(groundBlock)) {
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
		if (e.level instanceof ServerLevel varW ) {
			AABB aabb = new AABB(ePos.east(2).above(2).north(2), ePos.west(2).below(2).south(2));
			List<Entity> l  = new ArrayList<>();
			varW.getEntities().get(e.getType(),
					aabb,
					(entity)->{
						l.add(entity);
						});
			if (l.size() >= 9) {
				varW.setBlockAndUpdate(ePos.below(), Blocks.DIRT_PATH.defaultBlockState());
				e.hurt(DamageSource.IN_WALL,0.25f);
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

	private boolean mobGrowTallAction(Entity ent, Block footBlock, String key) {
		if (footBlock instanceof TallGrassBlock) {
			BlockPos ePos = getAdjustedBlockPos(ent);
			BonemealableBlock ib = (BonemealableBlock) footBlock;
			ib.performBonemeal((ServerLevel) ent.level, ent.level.random, ePos, ent.level.getBlockState(ePos));
			MyConfig.debugMsg(1, ePos, key + " grew and hid in tall plant.");
			return true;
		}
		return false;
	}

	private BlockState helperBiomeRoadBlockType(Biome localBiome) {
		BlockState gateBlockType = Blocks.DIRT_PATH.defaultBlockState();
		if (localBiome.getBiomeCategory() == Biome.BiomeCategory.DESERT) {
			gateBlockType = Blocks.SMOOTH_SANDSTONE.defaultBlockState(); // 16.1 mojang change
		}
		return gateBlockType;
	}

	// if a grassblock in village has farmland next to it on the same level- retill
	// it.
	// todo add hydration check before tilling land.
	private boolean vImproveFarm(Villager ve, Block groundBlock, Block footBlock, String regrowthType) {
		if (ve.getVillagerData().getProfession() != VillagerProfession.FARMER) {
			return false;
		}

		BlockPos vePos = getAdjustedBlockPos(ve);

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
			if ((waterValue > 0) && (BlockTags.LOGS.contains(groundBlock))
					|| (groundBlock == Blocks.SMOOTH_SANDSTONE)) {
				ve.level.setBlock(vePos, Blocks.TORCH.defaultBlockState(), 3);
				lastTorchX = veX;
				lastTorchY = veY;
				lastTorchZ = veZ;
				return true;
			}
		}
		return false;
	}

	private void vClericalHealing (Villager ve) {

		if (ve.getVillagerData().getProfession() != VillagerProfession.CLERIC) {
			return;
		}
		long daytime = ve.level.getDayTime()%24000;
		
		if (daytime < 9000 || daytime > 11000) {
			return;
		}
		if (ve.level instanceof ServerLevel varW ) {
			int clericalLevel = ve.getVillagerData().getLevel();

			BlockPos vePos = new BlockPos(ve.getX(), (ve.getY() + 0.99d), (ve.getZ()));
			AABB aabb = new AABB(vePos.east(4).above(2).north(4), vePos.west(4).below(2).south(4));
			List<Entity> l  = new ArrayList<>();
			varW.getEntities().get(
					aabb,
					(entity)->{
						if (entity instanceof Villager  || entity instanceof Player) {
							l.add(entity);
						} 
						});
			
			for (Entity e : l) {
				boolean heal = true;
	        	LivingEntity le = (LivingEntity) e;
	        	if (le.getHealth() < le.getMaxHealth()) {
	        		if (e instanceof Player pe) {
	        			int rep = ve.getPlayerReputation(pe);
	        			if (rep < 0) {  // I was a bad bad boy.
	        				heal = false;
	        			}
	        		}
	        		if (heal) {
	        			le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, clericalLevel*51, 0), ve);
	        			// ve.getLevel(). playLocalSound( ve.getX(), ve.getY(), ve.getZ(), SoundEvents.NOTE_BLOCK_HARP , SoundSource.NEUTRAL, 0.6f, 0.6f, true); 
	        			return;
	        		}
	        	}
	        }
		}
	}
	
	private void vImproveLeaves(Villager ve, Block groundBlock, String key, int veX, int veY, int veZ) {
		float veYaw = ve.getViewYRot(1.0f) / 45;
		// Local inner class
		BlockPos vePos = getAdjustedBlockPos(ve);
		int facingNdx = Math.round(veYaw);
		if (facingNdx < 0) {
			facingNdx = Math.abs(facingNdx);
		}
		facingNdx %= 8;

		// when standing on a grass path- game reports you 1 block lower. Adjust.
		if (groundBlock == Blocks.DIRT_PATH) {
			veY += 1;
		}
		int dx = facingArray[facingNdx][0];
		int dz = facingArray[facingNdx][1];
		BlockPos tmpBP = null;
		BlockState tempBS = null;
		Block tempBlock = null;
		boolean destroyBlock = false;
		for (int iY = 0; iY < 2; iY++) {
			tmpBP = new BlockPos(veX + dx, veY + iY, veZ + dz);
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
//		        class Inner {
//		        };
//		  
//		        // this method return a Method object representing
//		        // the instantly enclosing method of the method class
//		        String nameofCurrMethod = Inner.class
//		                                      .getEnclosingMethod()
//		                                      .getName();
//		  
//		        System.out.println("Name of current method: "
//		              + nameofCurrMethod);

				ve.level.destroyBlock(tmpBP, false);
				destroyBlock = false;
				MyConfig.debugMsg(1, vePos, key + " cleared " + tempBlock.getDescriptionId().toString());
			}
		}
	}

	private boolean vImproveLighting(Villager ve, Block footBlock, Block groundBlock, Biome localBiome) {
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
		if (footBlock instanceof BedBlock) {
			return false;
		}

		if (isValidGroundBlockToPlaceTorchOn(ve, groundBlock) && (footBlock instanceof AirBlock)) {
			ve.level.setBlock(vePos, Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
		}

		return true;

	}

	private void vImproveRoads(Villager ve, Block footBlock, Block groundBlock, String key, Biome localBiome) {

		boolean test = false;
		if (test) {
			ChunkPos c = new ChunkPos(ve.blockPosition());
			List<StructureStart<?>> sList = getStarts(ve.level, StructureFeature.VILLAGE, c.x, c.z);
			int z = 3;
		}
		if (vImproveRoadsFixUnfinished(ve, groundBlock, localBiome)) {
			MyConfig.debugMsg(1, ve.blockPosition(), key + " fix road.");
		}
		if (vImproveRoadsSmoothHeight(ve, footBlock, groundBlock, localBiome)) {
			MyConfig.debugMsg(1, ve.blockPosition(), key + " Smooth road slope.");
		}
	}

	private boolean vImproveRoadsFixUnfinished(Villager ve, Block groundBlock, Biome localBiome) {

		BlockPos vePos = getAdjustedBlockPos(ve);
		// fix unfinished spots in road with 3-4 grass blocks orthogonal to them.
		// on slopes too.

		int fixHeight = 3;
		if (Biome.BiomeCategory.TAIGA == localBiome.getBiomeCategory()) {
			fixHeight = 5;
		}

		Block biomeRoadBlock = helperBiomeRoadBlockType(localBiome).getBlock();
		if (biomeRoadBlock == Blocks.SMOOTH_SANDSTONE) {
			int skyLightValue = ve.level.getBrightness(LightLayer.SKY, vePos);
			if (skyLightValue < 13) {
				return false;
			}
			if (!(ve.getLevel().canSeeSky(ve.blockPosition()))) {
				return false;
			}
		}

		int veX = vePos.getX();
		int veY = vePos.getY();
		int veZ = vePos.getZ();

		if (groundBlock != biomeRoadBlock) {
			int roadBlockCount = 0;
			for (int dy = -1; dy <= fixHeight; dy++) {
				for (int i = 0; i < 4; i++) {
					Block tempBlock = ve.level.getBlockState(new BlockPos(veX + dx[i], veY + dy, veZ + dz[i]))
							.getBlock();
					if (tempBlock == biomeRoadBlock) {
						roadBlockCount += 1;
						if (roadBlockCount > 2) {
							if (ve.level.getBlockState(ve.blockPosition()).getBlock() instanceof SnowLayerBlock) {
								ve.level.destroyBlock(vePos, false);
							}
							ve.level.setBlockAndUpdate(vePos.below(), biomeRoadBlock.defaultBlockState());
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean vImproveRoadsSmoothHeight(Villager ve, Block footBlock, Block groundBlock, Biome localBiome) {
		BlockPos vePos = getAdjustedBlockPos(ve);
		// to do remove "tempBlock" and put in iff statement. Extract as method.

		int skyLightValue = ve.level.getBrightness(LightLayer.SKY, vePos);
		// don't smooth "inside".
		if (skyLightValue < 14) {
			return false;
		}

		Block biomeRoadBlock = helperBiomeRoadBlockType(localBiome).getBlock();

		if ((groundBlock != biomeRoadBlock) && (footBlock != biomeRoadBlock)) {
			return false;
		}

		BlockState smoothingBlockState = biomeRoadBlock.defaultBlockState();
		Block smoothingBlock = biomeRoadBlock;

		// Check for higher block to smooth up towards
		int poiDistance = 3;
		String key = "minecraft:" + localBiome.getBiomeCategory().toString();
		key = key.toLowerCase();
		if (key.equals("minecraft:desert")) {
			poiDistance = 7;
			if (!(ve.getLevel().canSeeSky(ve.blockPosition()))) {
				return false;
			}
		}

// 08/30/20 Collection pre 16.2 bug returns non empty collections.
//		the collection is not empty when it should be.
// 	    are returned in the collection so have to loop thru it manually.

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
					return false;
				}
			}
		}

		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);

		int yAdjust = 0;
		if (smoothingBlock == Blocks.DIRT_PATH) {
			yAdjust = 1;
		}

		int veX = vePos.getX();
		int veY = vePos.getY();
		int veZ = vePos.getZ();
		for (int dy = 1; dy < 5 + yAdjust; dy++) {
			for (int i = 0; i < 4; i++) {
				Block tempBlock = ve.level.getBlockState(new BlockPos(veX + dx[i], veY + dy, veZ + dz[i])).getBlock();
				if (tempBlock == smoothingBlock) {
					if (ve.level.getBlockState(new BlockPos(veX, veY, veZ)).getBlock() instanceof SnowLayerBlock) {
						ve.level.destroyBlock(vePos, false);
					}
					ve.level.setBlockAndUpdate(new BlockPos(veX, veY, veZ), smoothingBlockState);
					ve.setDeltaMovement(0.0, 0.4, 0.0);
					return true;
				}
			}
		}

		return false;
	}

	private boolean vImproveWallForMeetingPlace(Villager ve, String regrowthActions,
			BlockPos villageMeetingPlaceBlockPos, Block groundBlock, Block footBlock, Biome localBiome) {

		BlockPos vePos = getAdjustedBlockPos(ve);

		String key = "minecraft:" + localBiome.getBiomeCategory().toString();
//		ResourceLocation biomeName = ForgeRegistries.BIOMES.getKey(localBiome);
		key = key.toLowerCase();
		MyConfig.debugMsg(2, vePos, key + " wall improvement.");

		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		MyConfig.debugMsg(1, vePos, key + " biome for wall improvement.");

		if (currentWallBiomeDataItem == null) {
			MyConfig.debugMsg(2, vePos, "wallbiome data null.");
			key = "minecraft:" + localBiome.getBiomeCategory().toString().toLowerCase();
			currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem(key);
			if (currentWallBiomeDataItem == null) {
				currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem("minecraft:plains");
			}
		}
		int wallPerimeter = currentWallBiomeDataItem.getWallDiameter();
		if (wallPerimeter < 32)
			wallPerimeter = 32;
		if (wallPerimeter > 80)
			wallPerimeter = 80;

		wallPerimeter = (wallPerimeter / 2) - 1;

		int absvx = (int) Math.abs(ve.getX() - villageMeetingPlaceBlockPos.getX());
		int absvz = (int) Math.abs(ve.getZ() - villageMeetingPlaceBlockPos.getZ());

		if (isOnWallPerimeter(wallPerimeter, absvx, absvz)) {
			MyConfig.debugMsg(2, ve.blockPosition(), "villager on wall perimeter: " + wallPerimeter);
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
					if ((villageMeetingPlaceBlockPos.getX() == P.getPos().getX())
							&& (villageMeetingPlaceBlockPos.getY() == P.getPos().getY())
							&& (villageMeetingPlaceBlockPos.getZ() == P.getPos().getZ())) {
						continue; // ignore meeting place that owns this wall segment.
					} else {
						int disX = Math.abs(ve.blockPosition().getX() - P.getPos().getX());
						int disZ = Math.abs(ve.blockPosition().getZ() - P.getPos().getZ());
						if ((disX < wallPerimeter) && (disZ < wallPerimeter)) {
							buildWall = false; // another meeting place too close. cancel wall.
							break;
						}
					}
				}
			}

			if (buildWall) {
				BlockState wallTypeBlockState = currentWallBiomeDataItem.getWallBlockState();
				if (wallTypeBlockState == null) {
					wallTypeBlockState = Blocks.COBBLESTONE_WALL.defaultBlockState();
				}
				BlockState wallBlock = wallTypeBlockState;
				BlockState gateBlockType = helperBiomeRoadBlockType(localBiome);

				int wallTorchSpacing = (wallPerimeter + 1) / 4;
				if (helperPlaceOneWallPiece(ve, regrowthActions, wallPerimeter, wallTorchSpacing, gateBlockType,
						wallBlock, absvx, absvz, groundBlock, footBlock)) {
					if (regrowthActions.contains("t")) {
						if (isValidTorchLocation(wallPerimeter, wallTorchSpacing, absvx, absvz,
								ve.level.getBlockState(vePos).getBlock())) {
							ve.level.setBlockAndUpdate(vePos.above(), Blocks.TORCH.defaultBlockState());
						}
					}
					return true;
				}
			}
		}

		return false;

	}

	// villagers build protective walls around their homes. currently 32 out.
	// to do- reduce distance of wall from home.
	private boolean vImproveHomeFence(Villager ve, BlockPos vHomePos, String regrowthActions, Block groundBlock,
			Block footBlock, Biome localBiome) {

		BlockPos vePos = getAdjustedBlockPos(ve);
		String key = "minecraft:" + localBiome.getBiomeCategory().toString();
//		ResourceLocation biomeName = ForgeRegistries.BIOMES.getKey(localBiome);
		key = key.toLowerCase();
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		if (currentWallBiomeDataItem == null) {

			key = "minecraft:" + localBiome.getBiomeCategory().toString().toLowerCase();
			currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem(key);
			if (currentWallBiomeDataItem == null) {
				currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem("minecraft:plains");
			}
		}

		int homeFenceDiameter = currentWallBiomeDataItem.getWallDiameter();
		homeFenceDiameter = homeFenceDiameter / 4; // resize for personal home fence.
		if (homeFenceDiameter < 7)
			homeFenceDiameter = 7;
		if (homeFenceDiameter > 16)
			homeFenceDiameter = 16;
		int wallTorchSpacing = homeFenceDiameter / 4;
		homeFenceDiameter = (homeFenceDiameter / 2) - 1;

		int absvx = (int) Math.abs(ve.getX() - vHomePos.getX());
		int absvz = (int) Math.abs(ve.getZ() - vHomePos.getZ());

		Collection<PoiRecord> result = ((ServerLevel) ve.level).getPoiManager()
				.getInSquare(t -> t == PoiType.HOME, vePos, 17, Occupancy.ANY)
				.collect(Collectors.toCollection(ArrayList::new));

		// 08/30/20 Collection had bug with range that I couldn't resolve.
		boolean buildWall = true;
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
						buildWall = false; // another meeting place too close. cancel wall.
						break;
					}
				}
			}
		} else if ((result.isEmpty())) {
			buildWall = true;
		}

		if (buildWall) {

			BlockState fenceBlockState = currentWallBiomeDataItem.getFenceBlockState();
			if (fenceBlockState == null) {
				fenceBlockState = Blocks.OAK_FENCE.defaultBlockState();
			}
			BlockState wallBlock = fenceBlockState;
			BlockState gateBlockType = helperBiomeRoadBlockType(localBiome);

			boolean buildCenterGate = true;
			if (helperPlaceOneWallPiece(ve, regrowthActions, homeFenceDiameter, wallTorchSpacing, gateBlockType,
					wallBlock, absvx, absvz, groundBlock, footBlock)) {

				if (regrowthActions.contains("t")) {
					if (isValidTorchLocation(homeFenceDiameter, wallTorchSpacing, absvx, absvz,
							ve.level.getBlockState(vePos).getBlock())) {
						ve.level.setBlockAndUpdate(vePos.above(), Blocks.TORCH.defaultBlockState());
					}
				}
				return true;
			}
		}

		return false;
	}

	private void vImproveWalls(Villager ve, Block footBlock, Block groundBlock, String key, String regrowthType,
			Biome localBiome) {

		if (groundBlock instanceof AirBlock) {
			return; // ignore edge cases where villager is hanging on the edge of a block.
		}
		BlockPos vePos = getAdjustedBlockPos(ve);

		Brain<Villager> vb = ve.getBrain();
		Optional<GlobalPos> vMeetingPlace = vb.getMemory(MemoryModuleType.MEETING_POINT);
		if (!(vMeetingPlace.isPresent())) {
			return;
		}

		if (isOkayToBuildWallHere(ve, footBlock, groundBlock)) {
			GlobalPos gVMP = vMeetingPlace.get();
			BlockPos gVMPPos = gVMP.pos();
			long chunkAge = ve.level.getChunkAt(gVMPPos).getInhabitedTime();
			if (chunkAge < 1200) {
				ve.level.setBlock(gVMPPos.above(1), Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
			}
			if (!(ve.level.getBlockState(gVMPPos.above(1)).getBlock() instanceof WallBlock)) {
				return;
			}

			// place one block of a wall on the perimeter around village meeting place
			// Don't block any roads or paths regardless of biome.

			if (regrowthType.contains("w")) {
				MyConfig.debugMsg(1, vePos, "Checking Improve Wall.");
				if (vImproveWallForMeetingPlace(ve, regrowthType, gVMPPos, groundBlock, footBlock, localBiome)) {
					MyConfig.debugMsg(1, vePos, "Meeting Wall Improved.");
				}
			}
		}
	}

	private void vImproveFences(Villager ve, Block footBlock, Block groundBlock, String key, String regrowthType,
			Biome localBiome) {

		BlockPos ePos = ve.blockPosition();

		Brain<Villager> vb = ve.getBrain();
		Optional<GlobalPos> vMeetingPlace = vb.getMemory(MemoryModuleType.MEETING_POINT);
		if (!(vMeetingPlace.isPresent())) {
			return;
		}

		if (isOkayToBuildWallHere(ve, footBlock, groundBlock)) {
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
					if (vImproveHomeFence(ve, villagerHomePos, regrowthType, groundBlock, footBlock, localBiome)) {
						MyConfig.debugMsg(1, ePos, "Home Fence Improved.");
					}
				}
			}
		}
	}

	private static boolean isFootBlockOkayToBuildIn(Block footBlock) {
		if ((footBlock instanceof AirBlock) || (isGrassOrFlower(footBlock))) {
			return true;
		}
		return false;
	}

	private static boolean isGrassOrFlower(Block footBlock) {

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
			if (BlockTags.FLOWERS.contains(footBlock)) {
				return true;
			}
			if (BlockTags.TALL_FLOWERS.contains(footBlock)) {
				return true;
			}
		} catch (Exception e) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println("Tag Exception 1009-1014:" + footBlock.getDescriptionId() + ".");
			}
		}
		// biomes you'll go grass compatibility
		if (footBlock.getDescriptionId() == "block.byg.short_grass") {
			return true;
		}
		if (MyConfig.aDebugLevel > 0) {
			System.out.println("Not grass or Flower:" + footBlock.getDescriptionId() + ".");
		}
		return false;
	}

	private static boolean isImpossibleRegrowthEvent(Block footBlock, String regrowthType) {
		if ((regrowthType.equals("eat")) && (footBlock instanceof AirBlock)) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlock instanceof TallGrassBlock)) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlock instanceof FlowerBlock)) {
			return true;
		}
		if ((regrowthType.equals("tall")) && (!(footBlock instanceof TallGrassBlock))) {
			return true;
		}
		return false;
	}

	private static boolean isOkayToBuildWallHere(Villager ve, Block footBlock, Block groundBlock) {

		BlockPos ePos = ve.blockPosition();

		boolean okayToBuildWalls = true;

		if (!(isOnGround(ve))) {
			okayToBuildWalls = false;
		}
		if (!(isFootBlockOkayToBuildIn(footBlock))) {
			okayToBuildWalls = false;
		}
		if (!(isValidGroundBlockToBuildWallOn(ve, groundBlock))) {
			okayToBuildWalls = false;
		}
		return okayToBuildWalls;
	}

	private static boolean isOnGround(Entity e) {
		return e.isOnGround();
	}

	private static boolean isOnWallPerimeter(int wallPerimeter, int absvx, int absvz) {
		boolean scratch = false;
		if ((absvx == wallPerimeter) && (absvz <= wallPerimeter))
			scratch = true;
		if ((absvz == wallPerimeter) && (absvx <= wallPerimeter))
			scratch = true;
		return scratch;
	}

//	ItemStack iStk = new ItemStack(Items.BONE_MEAL,1);
//	BoneMealItem.applyBonemeal(iStk, e.world,e.getPosition());
// (likely 12.2 and 14.4 call?)	ib.performBonemeal((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));\

	private static boolean isOutsideMeetingPlaceWall(Villager ve, Optional<GlobalPos> vMeetingPlace,
			BlockPos meetingPlacePos, Biome localBiome) {

		BlockPos vePos = getAdjustedBlockPos(ve);
		String key = "minecraft:" + localBiome.getBiomeCategory().toString();
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

	private static boolean isValidGroundBlockToPlaceTorchOn(Villager ve, Block groundBlock) {

		String key = groundBlock.getRegistryName().toString(); // broken out for easier debugging
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(key);
		if (currentWallFoundationItem == null)
			return false;

		return true;

	}

	private static boolean isValidGroundBlockToBuildWallOn(Villager ve, Block groundBlock) {
		BlockPos vePos = getAdjustedBlockPos(ve);
		int blockSkyLightValue = ve.level.getBrightness(LightLayer.SKY, vePos);

		if (blockSkyLightValue < 13)
			return false;

		String key = groundBlock.getRegistryName().toString(); // broken out for easier debugging
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(key);
		if (currentWallFoundationItem == null)
			return false;

		return true;

	}

	private static boolean isValidTorchLocation(int wallPerimeter, int wallTorchSpacing, int absvx, int absvz,
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

	private boolean helperPlaceOneWallPiece(Villager ve, String regrowthType, int wallPerimeter,
			int wallTorchSpacing, BlockState gateBlockType, BlockState wallType, int absvx, int absvz,
			Block groundBlock, Block footBlock) {

		// Build North and South Walls (and corners)
		if (absvx == wallPerimeter) {
			if (absvz <= wallPerimeter) {
				return helperPlaceWallPiece(ve, gateBlockType, wallType, absvz);
			}
		}
		// Build East and West Walls (and corners)
		if (absvz == wallPerimeter) {
			if (absvx <= wallPerimeter) {
				return helperPlaceWallPiece(ve, gateBlockType, wallType, absvx);
			}
		}
		return false;
	}

	private boolean helperPlaceWallPiece(Villager ve, BlockState gateBlockType, BlockState wallType, int absva) {

		BlockPos vePos = getAdjustedBlockPos(ve);
		Block b = ve.level.getBlockState(vePos).getBlock();

		if (b instanceof SnowLayerBlock) {
			ve.level.destroyBlock(vePos, false);
		}

		if ((b instanceof SaplingBlock) || (b instanceof TallGrassBlock) || (b instanceof FlowerBlock)
				|| (b instanceof DoublePlantBlock)) {
			ve.level.destroyBlock(vePos, true);
		}

		if (absva == WALL_CENTER) {
			ve.level.setBlockAndUpdate(vePos.below(), gateBlockType);
			return true;
		}
		
		if (ve.level.setBlockAndUpdate(vePos, wallType)) {
			return true;
		} else {
			MyConfig.debugMsg(1, ve.blockPosition(),
					"Building Wall Fail: SetBlockAndUpdate Time End = " + ve.level.getGameTime());
			return false;
		}

		

	}
	
	  private static List<StructureStart<?>> getStarts(LevelAccessor worldIn, StructureFeature<?> struct, int x, int z)
	    {
	        List<StructureStart<?>> list = Lists.newArrayList();
	        ChunkAccess ichunk = worldIn.getChunk(x, z, ChunkStatus.STRUCTURE_REFERENCES);
	        LongSet set = ichunk.getAllReferences().get(struct);

	        if (set != null)
	        {
	            LongIterator longiterator = set.iterator();
	            while (longiterator.hasNext())
	            {
	                long i = longiterator.nextLong();
	                FeatureAccess istructurereader = worldIn.getChunk(ChunkPos.getX(i), ChunkPos.getZ(i),
	                        ChunkStatus.STRUCTURE_STARTS);
	                StructureStart<?> structurestart = istructurereader.getStartForFeature(struct);
	                if (structurestart != null)
	                    list.add(structurestart);
	            }
	        }

	        return list;
	    }

}
