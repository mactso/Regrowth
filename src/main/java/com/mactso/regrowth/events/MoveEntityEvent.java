package com.mactso.regrowth.events;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import com.mactso.regrowth.config.WallBiomeDataManager;
import com.mactso.regrowth.config.WallFoundationDataManager;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockWall;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDesert;
import net.minecraft.world.biome.BiomeTaiga;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@SuppressWarnings("deprecation")
public class MoveEntityEvent {


	private static int TICKS_PER_SECOND = 20;
	private static int[][] facingArray = { { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 }, { 0, -1 }, { 1, -1 }, { 1, 0 },
			{ 1, 1 } };

	static final int WALL_CENTER = 0;
	static final int WALL_TYPE_WALL = -1;

	static final IBlockState RED_STEM = Blocks.RED_MUSHROOM_BLOCK.getStateFromMeta(BlockHugeMushroom.EnumType.STEM.getMetadata());
	static final IBlockState BROWN_STEM = Blocks.BROWN_MUSHROOM_BLOCK.getStateFromMeta(BlockHugeMushroom.EnumType.STEM.getMetadata());
	static final IBlockState OAK_SAPLING = Blocks.SAPLING.getStateFromMeta(0);
	static final IBlockState SPRUCE_SAPLING = Blocks.SAPLING.getStateFromMeta(1);
	static final IBlockState BIRCH_SAPLING = Blocks.SAPLING.getStateFromMeta(2);
	static final IBlockState JUNGLE_SAPLING = Blocks.SAPLING.getStateFromMeta(3);
	static final IBlockState ACACIA_SAPLING = Blocks.SAPLING.getStateFromMeta(4);
	static final IBlockState DARK_OAK_SAPLING = Blocks.SAPLING.getStateFromMeta(5);
	
	
	
	private void helperAgeChildEntity(Entity ent) {
		if (ent instanceof EntityAgeable) {
			EntityAgeable aEnt = (EntityAgeable) ent;
			if (aEnt.isChild()) {
				aEnt.setGrowingAge(aEnt.getGrowingAge() + 30);
			}
		}
	}

	
	private boolean mobEatPlantsAction(Entity eventEntity, Block footBlock, Block groundBlock, String key, String regrowthType,
			double regrowthEventOdds, double randomD100Roll) {

		
		double eatingOdds = regrowthEventOdds;
		boolean ate = false;
		
		// Balance eating and growth odds/timing for "both" case because grow places many blocks and eat only destroys 1 block.
		if ((regrowthType.contentEquals("both"))) {
			eatingOdds = regrowthEventOdds * 15;
		}
		if (eventEntity instanceof AbstractHorse) {
			AbstractHorse h = (AbstractHorse) eventEntity;
			if (!(h.isEatingHaystack())) {
				eatingOdds = 0.0; // Horse, Donkey, Mule, (Llama?) only eat if animation eating.
			} else {
				eatingOdds = regrowthEventOdds * 25; // Increase eating odds during eating animation.
			}
		}
		if ((randomD100Roll <= eatingOdds)) {

			if (mobEatGrassOrFlower(eventEntity, regrowthType, footBlock,
					groundBlock)) {
				MyConfig.debugMsg(2, helperGetBlockPos (eventEntity), key + " ate plants. ");
				ate = true;
				}

		}
		return ate;
	}




	@SubscribeEvent
	public void handleEntityMoveEvents(LivingUpdateEvent event) {

		MyConfig.setaDebugLevel(0);
		// need to find the type of entity here.
		if (event.getEntity() == null) {
			return;
		}
		Entity eventEntity = event.getEntity();

		if (EntityList.getKey(eventEntity) == null) {
			return;
		}
		String registryNameAsString = EntityList.getKey(eventEntity).toString();

		World world = eventEntity.world;

		if (world == null) {
			return;
		}

		if (!(world instanceof WorldServer)) {
			return;
		}

		IBlockState footIBlockState = world.getBlockState(helperGetBlockPos(eventEntity));
		if (footIBlockState == null) {
			return;
		}

		Block footBlock = footIBlockState.getBlock();
		if (footBlock == null) {
			return;
		}

		IBlockState groundIBlockState = null;
		Block groundBlock = null;

		if (footBlock == Blocks.GRASS_PATH) {
			groundIBlockState = footIBlockState;
			groundBlock = footBlock;
		} else {
			groundIBlockState = world.getBlockState(helperGetBlockPos(eventEntity).down());
			if (groundIBlockState == null) {
				return;
			}
			groundBlock = groundIBlockState.getBlock();
		}

		if (groundBlock == null) {
			return;
		}

		if (eventEntity instanceof EntityBat) {

		}

		BlockPos ePos = helperGetBlockPos(eventEntity); // floats
		Biome localBiome = world.getBiome(ePos);
		RegrowthEntitiesManager.RegrowthMobItem currentRegrowthMobItem = RegrowthEntitiesManager
				.getRegrowthMobInfo(registryNameAsString);

		if (currentRegrowthMobItem == null) { // This mob isn't configured to do Regrowth Events
			return;
		}

		String regrowthActions = currentRegrowthMobItem.getRegrowthActions();

		if (isImpossibleRegrowthEvent(footBlock, regrowthActions)) {
			return;
		}

		double regrowthEventOdds = 2 / currentRegrowthMobItem.getRegrowthEventSeconds() * TICKS_PER_SECOND;
		double randomD100Roll = eventEntity.world.rand.nextDouble() * 100;

		if (randomD100Roll <= regrowthEventOdds) {
			if (eventEntity instanceof EntityVillager) {
				doVillagerRegrowthEvents(eventEntity, footBlock, groundBlock, ePos, registryNameAsString,
							regrowthActions, localBiome);
			} 
		}
		doMobRegrowthEvents(eventEntity, footBlock, groundBlock, registryNameAsString, regrowthActions,
				regrowthEventOdds, randomD100Roll, localBiome);
		return;
	}




	private boolean mobGrowPlantsAction(Entity eventEntity, Block footBlock, Block groundBlock, String key) {
		boolean triedToGrow = false;
		if (footBlock instanceof BlockAir) {
			IGrowable ib = (IGrowable) groundBlock;
			if (ib != null) {
				BlockPos ePos = helperGetBlockPos (eventEntity);
				ib.grow((WorldServer) eventEntity.world, eventEntity.world.rand, helperGetBlockPos(eventEntity),
						eventEntity.world.getBlockState(helperGetBlockPos(eventEntity)));
				MyConfig.debugMsg(2, ePos, key + " tried to grow plants.");
				triedToGrow = true;
			}
		}
		return triedToGrow;
	}




	private void helperHealEntity(Entity ent, int amt) {
		EntityLiving lEnt = (EntityLiving) ent;
		if (lEnt.getMaxHealth() > lEnt.getHealth() && (MyConfig.aEatingHeals == 1)) {
			PotionEffect ei = new PotionEffect(Potion.getPotionById(6), amt, 0, false, true);
			lEnt.addPotionEffect(ei);
		}
	}




	private void doMobRegrowthEvents(Entity ent, Block footBlock, Block groundBlock, String key,
			String regrowthType, double regrowthEventOdds, double randomD100Roll, 
			Biome localBiome) {

		BlockPos ePos = helperGetBlockPos (ent);
		World world =  ent.world;

		if (((footBlock instanceof BlockTorch)) && (randomD100Roll <= regrowthEventOdds)) {
			if (regrowthType.equals("stumble")) {
				mobStumbleAction(ent, key, world);
			}
			return;
		}
		if (regrowthType.equals("mushroom") && randomD100Roll <= 0.1 + regrowthEventOdds) {
			mobGrowMushroomsAction(ent, groundBlock, key, world);
			return;
		}

		if (regrowthType.equals("reforest") && randomD100Roll <= regrowthEventOdds) {
			mobReforestAction (ent, footBlock, groundBlock, key, localBiome);
			return;
		}	
		
		// all remaining actions currently require a grass block underfoot so if not a
		// grass block- can exit now.
		// this is for performance savings only.
		// looks like meadow_grass_block is not a grassBlock
		if (!(groundBlock instanceof BlockGrass)) {
			return;
		}

	
		
		if (!(footBlock instanceof BlockAir)) {
			if ((regrowthType.equals("tall")) && (randomD100Roll <= regrowthEventOdds)) {
				mobGrowTallAction(ent,footBlock, key);
				return;
			}
			if ((regrowthType.contentEquals("eat")) || (regrowthType.contentEquals("both"))) {
				if (mobEatPlantsAction(ent, footBlock, groundBlock, key, regrowthType, regrowthEventOdds,
						randomD100Roll)) {
					return;
				}
			}
		} 
		if (footBlock instanceof BlockAir) {
			if (((regrowthType.equals("grow")) || (regrowthType.equals("both"))) && (randomD100Roll <= regrowthEventOdds)) {
				if (mobGrowPlantsAction(ent, footBlock, groundBlock, key)) {
					return;
				}
			}
		}


		return;
	}




	private void mobGrowMushroomsAction(Entity ent, Block groundBlock, String key, World world) {

		BlockPos ePos = helperGetBlockPos (ent);
		int eX = ePos.getX();		int eY = ePos.getY();		int eZ = ePos.getZ();

		if (world.canSeeSky(ePos)) 
			return;

		float temp = world.getBiome(ePos).getTemperature(ePos);
		if (temp < MyConfig.getMushroomMinTemp()) 
			return;
		if (temp > MyConfig.getMushroomMaxTemp()) 
			return;
		MyConfig.debugMsg(1, ePos, key + " Biome temp: " + temp + ".");
		
		if (Math.abs(eX + eZ + eY) % MyConfig.getMushroomXDensity() != 1) 
			return;
		if ((Math.abs(eX) + Math.abs(eZ)) % MyConfig.getMushroomZDensity() == 1) 
			return;
		if (helperCountBlocksOrthagonalBB(Blocks.BROWN_MUSHROOM_BLOCK, 1, world, ePos, 1) > 0) 
			return;
		if (helperCountBlocksOrthagonalBB(Blocks.RED_MUSHROOM_BLOCK, 1, world, ePos, 1) > 0) 
			return;
		for (int mY = -1; mY < 7; mY++) {
			for (int mX = -5; mX < 6; mX++) {
				for (int mZ = -5; mZ < 6; mZ++) {
					if (world.getBlockState(new BlockPos(mX, mY, mZ)).getBlock() instanceof BlockMushroom) {
						MyConfig.debugMsg(1, ePos, key + " mushroom too crowded.");
						return;
					}
				}
			}
		}
		
		boolean growMushroom = false;
		if (groundBlock == Blocks.STONE) {
			growMushroom = true;
		}

		for (EntityPlayer player : world.playerEntities) {
			if (ePos.distanceSq(player.posX, player.posY, player.posZ) < 144) {
				growMushroom = false;
			}
		}

		if (growMushroom) {
			boolean red = false;
			BlockPos mtBP = this.helperGetBlockPos(ent);
			ent.motionX += 0.5;
			ent.motionY += -0.3;
			ent.motionZ += -0.3;

			world.setBlockState(mtBP.down(), Blocks.MYCELIUM.getDefaultState());
			if (world.rand.nextDouble() * 100.0 > 75.0) {
				world.setBlockState(mtBP, Blocks.RED_MUSHROOM.getDefaultState());
				red = true;
			} else {
				world.setBlockState(mtBP, Blocks.BROWN_MUSHROOM.getDefaultState());
			}
			BlockMushroom mb = (BlockMushroom) world.getBlockState(helperGetBlockPos(ent)).getBlock();
			mb.grow(world, world.rand, mtBP, world.getBlockState(helperGetBlockPos(ent)));
			if (red) {
				for (int y = 9; y > 2; y--) {
					if (world.getBlockState(mtBP.up(y)) == RED_STEM) { 
						world.setBlockState(mtBP.up(y), Blocks.GLOWSTONE.getDefaultState());
						break;
					}
				}
			}
		}
		MyConfig.debugMsg(1, ePos, key + " grow mushroom.");
		return;
	}
	
	
	
	private void mobReforestAction(Entity ent, Block footBlock, Block groundBlock, String key, Biome localBiome) {
		if (footBlock != Blocks.AIR)
			return;

		if (!(groundBlock instanceof BlockGrass) && !(groundBlock instanceof BlockDirt)) 
			return;

		BlockPos ePos = helperGetBlockPos(ent);
		int eX = ePos.getX(); 		int eY = ePos.getY();		int eZ = ePos.getZ();
		// only try to plant saplings in about 1/4th of blocks.
		if ((Math.abs(eX)%2 + Math.abs(eZ)%2) != 0) {
			return;
		}

		double sinY = Math.sin((double)((eY+64)%256)/256);
		if (ent.world.rand.nextDouble() > Math.abs(sinY))
			return;

		IBlockState sapling = null;
		// are we in a biome that has saplings in a spot a sapling can be planted?
		sapling = helperSaplingState(ePos, localBiome, sapling);
		if (sapling == null) 
			return;

		// check if there is room for a new tree.  Original trees.
		// don't plant a sapling near another sapling
		if (helperCountBlocksBB(Blocks.SAPLING, 1, ent.world, ePos, 5, 0) > 0) 
			return;
		
		// check if there is room for a new tree.  Original trees.
		int leafCount = 0;
		if ((sapling == ACACIA_SAPLING) || (sapling == DARK_OAK_SAPLING)) {
			leafCount =		 helperCountBlocksBB(Blocks.LEAVES2, 1, ent.world, ePos.up(5), 7, 0);
			if (leafCount > 0) {
				leafCount =		 helperCountBlocksBB(Blocks.LEAVES2, 1, ent.world, ePos.up(6), 7, 0);
			}
		} else {
			leafCount =		 helperCountBlocksBB(Blocks.LEAVES, 1, ent.world, ePos.up(4), 7, 0);
		}
		
		if (leafCount > 0) 
			return;

		ent.world.setBlockState(ePos, sapling);
		MyConfig.debugMsg(1, ePos, key + " planted sapling.");
	}

	
	
	private void mobStumbleAction(Entity eventEntity, String key, World sWorld) {
		BlockPos ePos = helperGetBlockPos (eventEntity);
		int eX = ePos.getX();		int eY = ePos.getY();		int eZ = ePos.getZ();

		sWorld.destroyBlock(ePos, true);

		MyConfig.debugMsg(1, ePos, key + " stumbled over torch.");
	}

	
	
	
	private void mobGrowTallAction(Entity eventEntity, Block footBlock, String key) {

		if (footBlock instanceof BlockTallGrass) {
			BlockPos ePos = helperGetBlockPos (eventEntity);
			if (helperCountBlocksBB(Blocks.DOUBLE_PLANT, 3, eventEntity.world, ePos, 5, 1) < 3) {
				mobGrowTallGrassToDoubleGrass(eventEntity, footBlock);
				MyConfig.debugMsg(1, ePos, key + " grew and hid in tall plant.");
			}
		}
	}




	private void mobTrodGrassBlock(World world, Block groundBlock, BlockPos ePos ) {
		if (groundBlock instanceof BlockGrass) {
			int dirtCount = 0;
			world.setBlockState(ePos.down(), Blocks.DIRT.getDefaultState());
			if (isBlockGrassPathOrDirt(world.getBlockState(ePos.east(1)).getBlock()))
				dirtCount += 1;
			if (isBlockGrassPathOrDirt(world.getBlockState(ePos.west(1)).getBlock()))
				dirtCount += 1;
			if (isBlockGrassPathOrDirt(world.getBlockState(ePos.north(1)).getBlock()))
				dirtCount += 1;
			if (isBlockGrassPathOrDirt(world.getBlockState(ePos.south(1)).getBlock()))
				dirtCount += 1;
			if (dirtCount == 4) {
				world.setBlockState(ePos, Blocks.GRASS_PATH.getDefaultState());
			}
			
		}
	}




	private void doVillagerRegrowthEvents(Entity eventEntity, Block footBlock, Block groundBlock,
			BlockPos eventEntityPos, String key, String regrowthType, Biome localBiome) {

		EntityVillager ve = (EntityVillager) eventEntity;
		if (!(isOnGround(ve))) {
			return;
		}

		// villagers ignore snow layers when placing walls, torches, etc.
		if (footBlock.getRegistryName().toString().equals("minecraft:snow_layer")) { 
			footBlock = Blocks.AIR;
		}
		
		// note all villagers may not have a home. poor homeless villagers.
		// default = repair farms

		if (regrowthType.contains("v")) {
			vImproveLeaves(ve, groundBlock, key);
		}

		if (regrowthType.contains("c")) {
			vImproveGrass(ve, footBlock, key);
		}

		if (regrowthType.contains("r")) {
			vImproveRoads(ve, footBlock, groundBlock, key, localBiome);
		}

		if (regrowthType.contains("w") ) {
			vImproveWalls(ve, footBlock, groundBlock, key, regrowthType,  localBiome);
			// "jump" villagers away if they are inside a wall or fence block.
		}

		if (regrowthType.contains("t") ) {
			vImproveLighting(ve, footBlock, groundBlock,localBiome); 
		}
	}




	private boolean mobEatGrassOrFlower(Entity ent, String regrowthType,
			Block footBlock, Block groundBlock) {


		BlockPos ePos = helperGetBlockPos(ent);
		MyConfig.debugMsg(2, ePos, "entityEatGrassOrFlower");

		if (!(isGrassOrFlower(footBlock))) {
			return false;
		}
		
		if (!(regrowthType.equals("eat")) && !(regrowthType.equals("both"))) {
			return false;
		}
		

		World world = ent.world;
		ent.world.destroyBlock(ePos, false);
		double randomD100Roll = ent.world.rand.nextDouble() * 100;
		// note bop: origin grass and bop: meadow grass do recover but maybe slower than
		// normal grass.
		if ((randomD100Roll > 40)) {
			mobTrodGrassBlock(world, groundBlock, ePos);
		}
		helperAgeChildEntity(ent);
		helperHealEntity(ent,1);
		return true;

	}




	private boolean mobGrowTallGrassToDoubleGrass(Entity eventEntity, Block footBlock) {
		if (footBlock instanceof BlockTallGrass) {
			IGrowable ib = (IGrowable) footBlock;
			ib.grow((WorldServer) eventEntity.world, eventEntity.world.rand, helperGetBlockPos(eventEntity),
					eventEntity.world.getBlockState(helperGetBlockPos(eventEntity)));
			return true;
		}
		return false;
	}
	
	
	
	
	private IBlockState helperBiomeRoadBlockType(Biome localBiome) {
		IBlockState roadBlockType = Blocks.GRASS_PATH.getDefaultState();
		if (localBiome instanceof BiomeDesert) {
			roadBlockType = Blocks.GRAVEL.getDefaultState();; // 16.1 mojang change
		}
		return roadBlockType;
	}

	private BlockPos helperGetBlockPos(Entity e) {
		return e.getPosition();
	}

	
	
	
	public int helperCountBlocksBB (Block searchBlock, int maxCount, World w, BlockPos bPos, int boxSize){
		return helperCountBlocksBB(searchBlock, maxCount, w, bPos, boxSize, boxSize); // "square" box subcase
	}


	
	public int helperCountBlocksBB (Block searchBlock, int maxCount, World w, BlockPos bPos, int boxSize, int ySize){
		int count = 0;
		int minX = bPos.getX()-boxSize; int maxX = bPos.getX()+boxSize;
		int minZ = bPos.getZ()-boxSize;   int maxZ = bPos.getZ()+boxSize;
		int minY = bPos.getY()-ySize; int maxY = bPos.getY()+ySize;


		for (int dx = minX; dx<= maxX; dx++) {
			for (int dz = minZ; dz <= maxZ; dz++) {
				for (int dy = minY; dy<= maxY; dy++) {
					Block b = w.getBlockState(new BlockPos(dx,dy,dz)).getBlock();
					MyConfig.debugMsg(2, "dx:"+dx+", dz:"+dz+", dy:"+dy +"  Block:"+b.getRegistryName().toString() + ", count:"+count);
					if (w.getBlockState(new BlockPos(dx,dy,dz)).getBlock() == searchBlock)
						count++;
					if (count >= maxCount)
						return count;
				}
				int jj = dz;
			}
			int ii = dx;
		}

		MyConfig.debugMsg(1, bPos, searchBlock.getRegistryName().toString()+" Sparse count:"+count+" countBlockBB ");

		return count;
	}

	
	
	// this routine returns a count of the searchblock immediately orthagonal to Blockpos, exiting if a max count is exceeded.
	public int helperCountBlocksOrthagonalBB (Block searchBlock, int maxCount, World w, BlockPos bPos, int boundY){
		return helperCountBlocksOrthagonalBB(searchBlock, maxCount, w, bPos, 0-boundY, 0+boundY);
	}


	
	public int helperCountBlocksOrthagonalBB (Block searchBlock, int maxCount, World w, BlockPos bPos, int lowerBoundY, int upperBoundY){
		int count = 0;
			for (int j = lowerBoundY; j <= upperBoundY; j++) {
					if (w.getBlockState(bPos.up(j).east(1)).getBlock() == searchBlock) 
						count++;
					if (w.getBlockState(bPos.up(j).west(1)).getBlock() == searchBlock) 
						count++;
					if (w.getBlockState(bPos.up(j).north(1)).getBlock() == searchBlock) 
						count++;
					if (w.getBlockState(bPos.up(j).south(1)).getBlock() == searchBlock) 
						count++;
					if (count >= maxCount)
						return count;
				}

		return count;
		
	}

	private BlockPos helperFindRealVillageCenter(EntityVillager ve) {

		BlockPos villagePos = ve.getHomePosition();
		if (villagePos != BlockPos.ORIGIN) {

			int vCenterX = ve.getHomePosition().getX();
			int vCenterY = ve.getHomePosition().getY();
			int vCenterZ = ve.getHomePosition().getZ();

			if (ve.world.getBlockState(new BlockPos(vCenterX, 1, vCenterZ)).getBlock() == Blocks.COBBLESTONE_WALL) {
				// take no action.
			} else {
				boolean villageMarkerFound = false;
				for (int i = -9; i <= 9; i++) {
					for (int j = -9; j <= 9; j++) {
						if (ve.world.getBlockState(new BlockPos(vCenterX + i, 1, vCenterZ + j))
								.getBlock() == Blocks.COBBLESTONE_WALL) {
							vCenterX = vCenterX + i;
							vCenterZ = vCenterZ + j;
							villageMarkerFound = true;
							break;
						}
					}
				}
				villagePos = new BlockPos(vCenterX, vCenterY, vCenterZ);
				if (villageMarkerFound == false) {
					ve.world.setBlockState(new BlockPos(vCenterX, 1, vCenterZ),
							Blocks.COBBLESTONE_WALL.getDefaultState());
				}
			}
		}

		return villagePos;
	}

	
	
	private boolean helperPlaceWallPiece(EntityVillager ve, IBlockState gateBlockType, boolean buildCenterGate,
			IBlockState wallType, int absva) {

		if (absva == WALL_CENTER) {
			if (buildCenterGate) {
				ve.world.setBlockState(helperGetBlockPos(ve).down(), gateBlockType);
				return true;
			} else {
				return false;
			}
		} else {
			IBlockState b = ve.world.getBlockState(helperGetBlockPos(ve).down());
			Block block = b.getBlock();
			if ((block instanceof BlockAir) || (block instanceof BlockTallGrass)
					|| (block instanceof BlockFlower || (block instanceof BlockDoublePlant))) {
				ve.world.setBlockState(helperGetBlockPos(ve).down(), wallType);

			} else {
				ve.world.setBlockState(helperGetBlockPos(ve), wallType);
			}
			return true;
		}

	}

	
	
	private boolean vImproveRoadsFixUnfinished(EntityVillager ve, Block groundBlock, Biome localBiome) {


		if (groundBlock instanceof BlockAir) {
			return false;
		}
		
		// fix unfinished spots in road with 3-4 grass blocks orthogonal to them.
		// on slopes too.
		int fixHeight = 4;
		if (localBiome instanceof BiomeTaiga ) {
			fixHeight = 6;
		}

		BlockPos vePos = helperGetBlockPos (ve);
	
		Block biomeRoadBlock = helperBiomeRoadBlockType(localBiome).getBlock();
		if (groundBlock != biomeRoadBlock) {
			if (helperCountBlocksOrthagonalBB(biomeRoadBlock, 3, ve.world, vePos, -1, fixHeight) > 2) {
				ve.world.setBlockState(helperGetBlockPos(ve).down(), biomeRoadBlock.getDefaultState());
				return true;
			}
		}
		return false;
	}

	
	private boolean vImproveRoadsSmoothHeight(EntityVillager ve, Block footBlock, Block groundBlock, Biome localBiome) {

		if (!(footBlock instanceof BlockAir))
			return false;

		if (ve.world.getLightFor(EnumSkyBlock.SKY, helperGetBlockPos(ve)) < 14)   	// don't smooth "inside".
			return false;

		Block biomeRoadBlock = helperBiomeRoadBlockType(localBiome).getBlock();
		if (groundBlock != biomeRoadBlock)
			return false;

		IBlockState smoothingIBlockState = biomeRoadBlock.getDefaultState();
		Block smoothingBlock = biomeRoadBlock;

		BlockPos vePos = helperGetBlockPos (ve);
		
		if (helperCountBlocksOrthagonalBB(smoothingBlock, 1, ve.world, vePos, 1, 5) > 0) {
			ve.world.setBlockState(vePos, smoothingIBlockState);
			ve.motionX += 0.1; ve.motionY += 0.6; ve.motionZ += 0.1;
			return true;
		}
		
		return false;
	}

	private boolean vImproveVillageWall(EntityVillager ve, String regrowthActions,
			BlockPos villageMeetingPlaceBlockPos, Block groundBlock, Block footBlock, Biome localBiome) {

		String key = localBiome.getRegistryName().toString();
	
		BlockPos vePos = helperGetBlockPos (ve);
		int veX = vePos.getX();		int veY = vePos.getY();		int veZ = vePos.getZ();
		MyConfig.debugMsg(2, vePos, " Improve Walls Key: " + key + ".");

		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		
		int wallPerimeter = currentWallBiomeDataItem.getWallDiameter();;
		
		if (wallPerimeter < 32)
			wallPerimeter = 32;
		if (wallPerimeter > 80)
			wallPerimeter = 80;

		wallPerimeter = (wallPerimeter / 2) - 1;

		int absvx = (int) Math.abs(veX - villageMeetingPlaceBlockPos.getX());
		int absvz = (int) Math.abs(veZ - villageMeetingPlaceBlockPos.getZ());

		if (isOnWallPerimeter(wallPerimeter, absvx, absvz)) {
			MyConfig.debugMsg(2, vePos, "Villager on wall perimeter " + wallPerimeter + ".");

			boolean buildWall = isOkayToBuildWallHere(ve, footBlock, groundBlock);
			
			if (buildWall) {
				IBlockState wallTypeBlockState = currentWallBiomeDataItem.getWallBlockState();
				if (wallTypeBlockState == null) {
					wallTypeBlockState = Blocks.COBBLESTONE_WALL.getDefaultState();
				}
				IBlockState wallBlock = wallTypeBlockState;
				IBlockState gateBlockType = helperBiomeRoadBlockType(localBiome);

				int wallTorchSpacing = (wallPerimeter + 1) / 4;

				boolean buildCenterGate = true;
				if (helperPlaceOneWallPiece(ve, regrowthActions, wallPerimeter, wallTorchSpacing, gateBlockType,
						buildCenterGate, wallBlock, absvx, absvz, groundBlock, footBlock)) {
					MyConfig.debugMsg(2, vePos, "Villager built Wall "+".");
					if (regrowthActions.contains("t")) {
						if (isValidTorchLocation(wallPerimeter, wallTorchSpacing, absvx, absvz,
								ve.world.getBlockState(helperGetBlockPos(ve)).getBlock())) {
							ve.world.setBlockState(helperGetBlockPos(ve).up(), Blocks.TORCH.getDefaultState());
						}
					}
					if ((footBlock instanceof BlockWall)||(footBlock instanceof BlockFence)) {
						float veYaw = ve.rotationYaw / 45; 
						int facingNdx = Math.round(veYaw);
						if (facingNdx < 0) {
							facingNdx = Math.abs(facingNdx);
						}
						facingNdx %= 8;
						double dx = (facingArray[facingNdx][0]) / 2.0; 
						
						double dz = (facingArray[facingNdx][1]) / 2.0;
						ve.motionX = dx;
						ve.motionY = 0.55;
						ve.motionZ = dz;
					}
					return true;
				}
					MyConfig.debugMsg(2, vePos, " Wall NOT built.");
			}
		} else {
			// wall has moved-- villager destroys old wall sections.
			if ((footBlock instanceof BlockWall)||(footBlock instanceof BlockFence)) {
				ve.world.destroyBlock(vePos.up(), false);			
   			    ve.world.destroyBlock(vePos, false);			
			}
		}
		return false;
	}


	private boolean isBlockGrassPathOrDirt(Block tempBlock) {

		if ((tempBlock == Blocks.GRASS_PATH) || (tempBlock == Blocks.DIRT)) {
			return true;
		}
		return false;
	}

	private boolean isFootBlockOkayToBuildIn(Block footBlock) {
		if ((footBlock instanceof BlockAir) || (isGrassOrFlower(footBlock))) {
			return true;
		}
		return false;
	}

	private boolean isGrassOrFlower(Block footBlock) {

		// short flowers 1 block high
		if (footBlock instanceof BlockFlower) {
			return true;
		}
		// 1 block high grass, bushes, ferns
		if (footBlock instanceof BlockTallGrass) {
			return true;
		}
		// large flowers, large plants
		if (footBlock instanceof BlockDoublePlant) {
			return true;
		}

		return false;
	}

	private boolean isImpossibleRegrowthEvent(Block footBlock, String regrowthType) {
		if ((regrowthType.equals("eat")) && (footBlock instanceof BlockAir)) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlock instanceof BlockTallGrass)) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlock instanceof BlockFlower)) {
			return true;
		}
		if ((regrowthType.equals("tall")) && (!(footBlock instanceof BlockTallGrass))) {
			return true;
		}
		return false;
	}

	private boolean isOkayToBuildWallHere(EntityVillager ve, Block footBlock, Block groundBlock ) {
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

	private boolean isOnGround(Entity e) {
		return e.onGround ;
	}

	private boolean isOnWallPerimeter(int wallPerimeter, int absvx, int absvz) {
		boolean scratch = false;
		if ((absvx == wallPerimeter) && (absvz <= wallPerimeter))
			scratch = true;
		if ((absvz == wallPerimeter) && (absvx <= wallPerimeter))
			scratch = true;
		return scratch;
	}


	private boolean isValidGroundBlockToBuildWallOn(EntityVillager ve, Block groundBlock) {

		int blockSkyLightValue = ve.world.getLightFor(EnumSkyBlock.SKY, helperGetBlockPos(ve));

		if (blockSkyLightValue < 13)
			return false;

		String key = groundBlock.getRegistryName().toString(); // broken out for easier debugging
		WallFoundationDataManager.WallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(key);
		if (currentWallFoundationItem == null)
			return false;

		return true;

	}

	private boolean isValidGroundBlockToPlaceTorchOn(EntityVillager ve, Block groundBlock) {

		String key = groundBlock.getRegistryName().toString(); // broken out for easier debugging
		if (key.equals("minecraft:cobblestone")) {
			return true;
		}
		if (key.equals("minecraft:planks")) {
			return true;
		}

		WallFoundationDataManager.WallFoundationItem currentWallFoundationItem = WallFoundationDataManager
				.getWallFoundationInfo(key);
		if (currentWallFoundationItem == null)
			return false;

		return true;

	}

	private boolean isValidTorchLocation(int wallPerimeter, int wallTorchSpacing, int absvx, int absvz,
			Block wallBlockFence) {

		boolean hasAWallUnderIt = false;
		if (wallBlockFence instanceof BlockWall) {
			hasAWallUnderIt = true;
		}
		if (wallBlockFence instanceof BlockFence) {
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

	private boolean helperPlaceOneWallPiece(EntityVillager ve, String regrowthType, int wallPerimeter, int wallTorchSpacing,
			IBlockState gateBlockType, boolean buildCenterGate, IBlockState wallType, int absvx, int absvz,
			Block groundBlock, Block footBlock) {
		
		BlockPos vePos = helperGetBlockPos (ve);
		int veX = vePos.getX();		int veY = vePos.getY();		int veZ = vePos.getZ();
		MyConfig.debugMsg(2, vePos, "placeOneWallPiece.");


		// Build North and South Walls (and corners)
		if (absvx == wallPerimeter) {
			if (absvz <= wallPerimeter) {
				MyConfig.debugMsg(2, "N / S placeWallHelper Z: " + veX +", "+ veY+", " + veZ + ".");
				return helperPlaceWallPiece(ve, gateBlockType, buildCenterGate, wallType, absvz);
			}
		}
		// Build East and West Walls (and corners)
		if (absvz == wallPerimeter) {
			if (absvx <= wallPerimeter) {
				if (MyConfig.aDebugLevel > 1) {
					MyConfig.debugMsg(2, "E / W placeWallHelper X : " + veX +", "+ veY+", " + veZ + ".");
				}
				return helperPlaceWallPiece(ve, gateBlockType, buildCenterGate, wallType, absvx);
			}
		}
		return false;
	}

	private IBlockState helperSaplingState(BlockPos pos, Biome localBiome, IBlockState sapling) {
		if (localBiome.getRegistryName().getResourcePath().contains("roofed")) {
			sapling = OAK_SAPLING;
		}
		if (localBiome.getRegistryName().getResourcePath().contains("birch")) {
			sapling = BIRCH_SAPLING;
		}
		if (localBiome.getRegistryName().getResourcePath().contains("taiga")) {
			sapling = SPRUCE_SAPLING;
		}
		if (localBiome.getRegistryName().getResourcePath().contains("jungle")) {
			sapling = JUNGLE_SAPLING;
		}
		if (localBiome.getRegistryName().getResourcePath().contains("savanna")) {
			sapling = ACACIA_SAPLING;
		}
		if (localBiome.getRegistryName().getResourcePath().contains("forest")) {
			sapling = OAK_SAPLING;
		}
		if (localBiome.getRegistryName().getResourcePath().contains("plains")) {
			sapling = OAK_SAPLING;
		}
		return sapling;
	}
	
	
	// this routine returns a count of the searchblock, exiting if a max count is exceeded.

	private void vImproveGrass(EntityVillager ve, Block footBlock, String key ) {
		BlockPos vePos = helperGetBlockPos (ve);
		int veX = vePos.getX();	int veY = vePos.getY();	int veZ = vePos.getZ();
		
		if ((footBlock instanceof BlockTallGrass) || (footBlock instanceof BlockDoublePlant)) {
			ve.world.destroyBlock(vePos, false);
			MyConfig.debugMsg(1, vePos, key + " cut grass.");
		}
	}

	private void vImproveLeaves(EntityVillager ve, Block groundBlock, String key) {

		BlockPos vePos = helperGetBlockPos (ve);
		int veX = vePos.getX();
		int veY = vePos.getY();
		int veZ = vePos.getZ();

		float veYaw = ve.rotationYaw / 45;  // validate Yaw has a value like 0-7 (was yaw(1.0) )
		int facingNdx = Math.round(veYaw);
		if (facingNdx < 0) {
			facingNdx = Math.abs(facingNdx);
		}
		facingNdx %= 8;

		int dx = facingArray[facingNdx][0];
		int dz = facingArray[facingNdx][1];
		BlockPos tmpBP = null;
		IBlockState tempBS = null;
		Block tempBlock = null;
		boolean destroyBlock = false;
		for (int iY = 0; iY < 2; iY++) {
			tmpBP = new BlockPos(veX + dx, veY + iY, veZ + dz);
			tempBS = ve.world.getBlockState(tmpBP);
			tempBlock = tempBS.getBlock();
			if (tempBlock instanceof BlockLeaves) {  // TODO don't destroy leaves placed by players.
				destroyBlock = true;
				if (tempBS.getValue(BlockLeaves.DECAYABLE).booleanValue() == false) {
					destroyBlock = false;
				}
			}
			if ((tempBlock instanceof BlockCactus)) {
				destroyBlock = true;
			}
			if (destroyBlock) {
				ve.world.destroyBlock(tmpBP, false);
				destroyBlock = false;
				MyConfig.debugMsg(2, key + " clear " + tempBlock.getLocalizedName() + " at" + +veX + ", " + (veY + iY) + ", " + veZ + ", ");
			}
		}
	}
	
	private boolean vImproveLighting(EntityVillager ve, Block footBlock, Block groundBlock, Biome localBiome) {
		
		if (footBlock == Blocks.TORCH)
			return false;
		
		int blockLightValue = ve.world.getLightFor(EnumSkyBlock.BLOCK, helperGetBlockPos(ve));
		if (blockLightValue >= MyConfig.getImproveTorchLightLevel())
			return false;

		int skyLightValue = ve.world.getLightFor(EnumSkyBlock.SKY, helperGetBlockPos(ve));
		if (skyLightValue > 13)
			return false;

		if (isValidGroundBlockToPlaceTorchOn(ve, groundBlock) && (footBlock instanceof BlockAir)) {
			ve.world.setBlockState(helperGetBlockPos(ve), Blocks.TORCH.getDefaultState());
		}

		return true;

	}

	private void vImproveRoads(EntityVillager ve, Block footBlock, Block groundBlock, String key, Biome localBiome) {

		BlockPos vePos = helperGetBlockPos (ve);
		
		if (vImproveRoadsFixUnfinished(ve, groundBlock, localBiome)) {
			MyConfig.debugMsg(1, vePos, key + " fix unfinished road spots.");
		}
		if (vImproveRoadsSmoothHeight(ve, footBlock, groundBlock, localBiome)) {
			MyConfig.debugMsg(1, vePos, key + " smooth road slopes.");
		}
	}

	
	
	private void vImproveWalls(EntityVillager ve, Block footBlock, Block groundBlock, String key, String regrowthType,
			Biome localBiome) {

		BlockPos vePos = helperGetBlockPos(ve);
		BlockPos villagePos = helperFindRealVillageCenter(ve);

		if (villagePos != BlockPos.ORIGIN) {
			// place one block of a wall on the perimeter around village meeting place
			// Don't block any roads or paths regardless of biome.
			MyConfig.debugMsg(2, villagePos, key + " Try to Improve Town Wall.");
			if (vImproveVillageWall(ve, regrowthType, villagePos, groundBlock, footBlock, localBiome)) {
				MyConfig.debugMsg(1, villagePos, key + " Improved Town Wall.");
			}
			
		}
	}

}
