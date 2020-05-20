package com.mactso.regrowth.events;

import java.util.Optional;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;

import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.GrassPathBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WoodType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.extensions.IForgeBlockState;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber()
public class MoveEntityEvent {
	
	static int[]dx= {1,0,-1,0};
	static int[]dz= {0,1,0,-1};
	static int TICKS_PER_SECOND = 20;	
	
	@SubscribeEvent
    public void doEntityMove(LivingUpdateEvent event) { 

	    // need to find the type of entity here.
		if (event.getEntity() == null) {
			return;
		}
		
		Entity eventEntity = event.getEntity();
		
		if (!(eventEntity.world instanceof ServerWorld)) {
			return;
		}
	
		Block footBlock = eventEntity.world.getBlockState(eventEntity.getPosition()).getBlock();
		Block groundBlock = eventEntity.world.getBlockState(eventEntity.getPosition().down()).getBlock();

		BlockPos eventEntityPos = eventEntity.getPosition(); // floats
		int eventEntityX =  eventEntityPos.getX(); // Int
		int eventEntityY =  eventEntityPos.getY(); // Int
		int eventEntityZ =  eventEntityPos.getZ(); // Int
		
		EntityType<?> tempType = eventEntity.getType();
		ResourceLocation registryName = tempType.getRegistryName();
		String registryNameAsString = registryName.toString();
		RegrowthEntitiesManager.RegrowthMobItem currentRegrowthMobItem = RegrowthEntitiesManager.getRegrowthMobInfo(registryNameAsString);
		if (currentRegrowthMobItem==null) {  // This mob isn't configured to do Regrowth Events
			return;
		}
		
		String regrowthType = currentRegrowthMobItem.getRegrowthType();
	
		if (isImpossibleRegrowthEvent(footBlock, regrowthType)) {
			return;
		}
		
		double regrowthEventOdds = 2/currentRegrowthMobItem.getRegrowthEventSeconds() * TICKS_PER_SECOND;
		double randomD100Roll = eventEntity.world.rand.nextDouble() * 100; 

		if (randomD100Roll <= regrowthEventOdds) {
			if (eventEntity instanceof VillagerEntity) {
				VillagerEntity ve = (VillagerEntity) eventEntity;
				doVillagerRegrowthEvents(ve, footBlock, groundBlock, registryNameAsString, regrowthType,
						eventEntityX, eventEntityY, eventEntityZ);
			}
		}
	

		doMobRegrowthEvents(eventEntity, footBlock, groundBlock, registryNameAsString, regrowthType, regrowthEventOdds, randomD100Roll,
							eventEntityX, eventEntityY, eventEntityZ);

		int debugBreakPoint = 0;

	}

	private void doMobRegrowthEvents(Entity eventEntity, Block footBlock, Block groundBlock, String key,
			String regrowthType, double regrowthEventOdds, double randomD100Roll,
			int eX, int eY, int eZ) {

		// all remaining actions currently require a grass block underfoot so if not a grass block- can exit now.

		if ((groundBlock != Blocks.GRASS_BLOCK)) {
			return;
		}

		if ((regrowthType.equals("tall")) && (randomD100Roll <= regrowthEventOdds)){
			entityGrowTallGrassToDoubleGrass(eventEntity, footBlock, regrowthType);
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " tall at " +  eX +", "+ eY +", "+ eZ +".");
			}
			return;
		}	
		
		if ( ((regrowthType.equals("grow")) || (regrowthType.equals("both"))) && (randomD100Roll <= regrowthEventOdds) ) {
			if (footBlock instanceof AirBlock ) {
	        	IGrowable ib = (IGrowable) groundBlock;
	    		ib.grow((ServerWorld)eventEntity.world, eventEntity.world.rand, eventEntity.getPosition(), eventEntity.world.getBlockState(eventEntity.getPosition()));    			
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " grow at " +  eX +", "+ eY +", "+ eZ +".");
				}
				return;
			}
		}
	    
		// Note: growth creates many blocks.  Eating only eats one block.  So make more common.
		if ((randomD100Roll <= regrowthEventOdds*10)) {
			if (entityEatGrassOrFlower(eventEntity, regrowthType, footBlock)) {
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " eat at " +  eX +", "+ eY +", "+ eZ +".");
				}
			}
			return;
		}
		return;
	}

	private void doVillagerRegrowthEvents(VillagerEntity ve, Block footBlock, Block groundBlock, String key, String regrowthType,
											int veX, int veY, int veZ)
	{

		// note all villagers may not have a home.  poor homeless villagers.
		//default = repair farms
		if (improveFarm(ve,groundBlock,regrowthType,veX,veY,veZ)) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " farm improved at " +  +  veX +", "+veY +", "+veZ+", ");
			}	
		};
		
		// c = cut down grass (but not flowers for now)
		// to do - maybe remove flowers unless by a road or elevated (air next to them as in the flower beds)
		// to do - replace "c" with a meaningful constant.
		
		if(regrowthType.contains("c")) {
			if (footBlock instanceof TallGrassBlock) {
				ve.world.destroyBlock(ve.getPosition(), false);
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " cut at " +  veX +", "+veY+", "+veZ+".");
				}					
			}
		}
		// improve roads  
		// to do - replace "r" with a meaningful constant.
		if(regrowthType.contains("r")) {
			improveRoads(ve, footBlock, groundBlock, key);
		}


		// note villages may not have a meeting place.  Sometimes they change.  Sometimes they take a few minutes to form.
		if (regrowthType.contains("w")) {
			if (!(ve.isAirBorne)) {
				improveWalls(ve, footBlock, groundBlock, key, regrowthType,veX,veY,veZ);
			}
		}
		

		if((regrowthType.contains("l")&&(footBlock != Blocks.TORCH))) {
			if (improveLighting(ve, footBlock, groundBlock, veX, veY, veZ)) {
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key +"- "+ footBlock +", " + groundBlock +" pitch: "+ve.rotationPitch + " improve lighting at " +  ve.getPosX() +", "+ve.getPosY()+", "+ve.getPosZ());
				}				
			}
		}
	}

	private boolean entityEatGrassOrFlower(Entity eventEntity, String regrowthType, Block footBlock) {

		if (!(isGrassOrFlower(footBlock))) {
			return false;
		}
		if ( !(regrowthType.equals("eat")) && 
			 !(regrowthType.equals("both")) ) {
			return false;
		}
		eventEntity.world.destroyBlock(eventEntity.getPosition(), false);
		LivingEntity le = (LivingEntity) eventEntity;
		if (eventEntity instanceof AgeableEntity) {
			AgeableEntity ae = (AgeableEntity) eventEntity;
			if (ae.isChild()) {
				ae.setGrowingAge(ae.getGrowingAge() + 30);
			}
		}
		if (le.getMaxHealth() > le.getHealth() && (MyConfig.aEatingHeals == 1)) {
    		EffectInstance ei = new EffectInstance (Effects.INSTANT_HEALTH,1,0, false, true );
    		le.addPotionEffect(ei);
		}
		return true;
	}

	private boolean entityGrowTallGrassToDoubleGrass(Entity eventEntity, Block footBlock, String regrowthType) {
		if (footBlock instanceof TallGrassBlock ) {
			IGrowable ib = (IGrowable) footBlock;
			ib.grow((ServerWorld)eventEntity.world, eventEntity.world.rand, eventEntity.getPosition(), eventEntity.world.getBlockState(eventEntity.getPosition()));    			
			return true;
		}
		return false;
	}

	// if a grassblock in village has farmland next to it on the same level- retill it. 
	// todo add hydration check before tilling land.
	private boolean improveFarm(VillagerEntity ve, Block groundBlock, String regrowthType,
								int veX, int veY, int veZ) {
		boolean nextToFarmBlock = false;
		for(int i=0; i<4; i++) {
			Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY-1,veZ+dz[i])).getBlock();
			if (tempBlock == Blocks.FARMLAND) {
				nextToFarmBlock = true;
			}
		}

		if (nextToFarmBlock) {
			if (groundBlock == Blocks.SMOOTH_SANDSTONE) {
				if (regrowthType.contains("t")) {
					ve.world.setBlockState(ve.getPosition(), Blocks.TORCH.getDefaultState());
				}
			}
			if (groundBlock instanceof GrassBlock) {
				ve.world.setBlockState(ve.getPosition().down(), Blocks.FARMLAND.getDefaultState());
				return true;
			}
		}
		return false;
	}


	private boolean improveLighting(VillagerEntity ve, Block footBlock, Block groundBlock,
							int veX, int veY, int veZ ) {
		int skylightValue = ve.world.getLightFor(LightType.SKY, ve.getPosition());
		Biome.Category villageBiomeCategory = ve.world.getBiome(ve.getPosition()).getCategory();
		ITextComponent tName = new StringTextComponent (veX+","+veZ+": ");
		ve.setCustomName(tName);
		
		if (ve.isSleeping()) { 
			return false;
		}
		if ((groundBlock == Blocks.OAK_PLANKS) ||
			(groundBlock == Blocks.SPRUCE_PLANKS) ||
			(groundBlock == Blocks.BIRCH_PLANKS) ||
			(groundBlock == Blocks.JUNGLE_PLANKS) ||
			(groundBlock == Blocks.ACACIA_PLANKS) ||
			((groundBlock == Blocks.SMOOTH_SANDSTONE) && (skylightValue<14)) ||
			((groundBlock == Blocks.COBBLESTONE) && (skylightValue<14) && (villageBiomeCategory == Biome.Category.TAIGA))
			) {
				int blockLightValue = ve.world.getLightFor(LightType.BLOCK, ve.getPosition());
				if (blockLightValue < 8) {
 					ve.world.setBlockState(ve.getPosition(), Blocks.TORCH.getDefaultState());
					return true;
				}

		}
		return false;
	}


	private void improveRoads(VillagerEntity ve, Block footBlock, Block groundBlock, String key) {
		BlockPos vePos = ve.getPosition();
		int veX =  vePos.getX();
		int veZ =  vePos.getZ();
		int veY =  vePos.getY();

		if (improveRoadsFixPotholes(ve, groundBlock,veX, veZ, veY)) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " fix road " +  veX +", "+veY+", "+veZ+". ");
			}	
		}
		if (improveRoadsSmoothHeight(ve, footBlock, veX, veZ, veY)) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " fix road slope" +  veX +", "+ veY+", "+veZ+", ");
			}
			
		}
	}

	private boolean improveRoadsFixPotholes(VillagerEntity ve, Block groundBlock,  
			int veX, int veZ, int veY) {
		// fix pot holes - grass spots in road with 3-4 grass blocks orthagonal to them.
		// to do remove "tempBlock" and put in iff statement.  Extract as method.
		// add biome support for dirt, sand, and podzol
		int grassPathCount = 0;
		int fixHeight = 1;
		if (Biome.Category.TAIGA == ve.world.getBiome(ve.getPosition()).getCategory()) {
			fixHeight = 4;
		}
		if ((groundBlock == Blocks.DIRT)||(groundBlock instanceof GrassBlock)) {
			for(int dy=-1; dy<=fixHeight; dy++) { // on gentle slopes too
				for(int i=0; ((i<4)&&(grassPathCount<3)); i++) {
					Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY+dy,veZ+dz[i])).getBlock();
					if (tempBlock == Blocks.GRASS_PATH) {
						grassPathCount += 1;
					}
				}
		    } 
			if (grassPathCount >= 3) {
				ve.world.setBlockState(ve.getPosition().down(), Blocks.GRASS_PATH.getDefaultState());
				return true;
			}
		}
		return false;
	}
	
	private boolean improveRoadsSmoothHeight(VillagerEntity ve, Block footBlock,  
													int veX, int veZ, int veY) {
		// to do remove "tempBlock" and put in iff statement.  Extract as method.			
		if (footBlock instanceof GrassPathBlock) {
			boolean doRoadSmoothing = false;
			for (int dy=2;((dy<5) && (doRoadSmoothing==false));dy++) {
				for(int i=0; ((i<4) && (doRoadSmoothing==false)); i++) {
					Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY+dy,veZ+dz[i])).getBlock();
					if (tempBlock == Blocks.GRASS_PATH) {
						doRoadSmoothing = true;
					}
				}
				
			}
			if (doRoadSmoothing) {
				ve.world.setBlockState(new BlockPos (veX,veY+1,veZ), Blocks.GRASS_PATH.getDefaultState());
				ve.setMotion(0.0, 0.4, 0.0);
				return true;
			}
		}
		return false;
	}

	private boolean improveWallForMeetingPlace(VillagerEntity ve, 
			String regrowthType, BlockPos villageMeetingPlaceBlockPos) {

		Biome villageBiome = ve.world.getBiome(villageMeetingPlaceBlockPos);		
		int wallPerimeter = 31;
		int wallTorchSpacing = 8;
		final int wallCenter = 0;
		BlockState wallType = Blocks.COBBLESTONE_WALL.getDefaultState();
		BlockState gateBlockType = Blocks.GRASS_PATH.getDefaultState();
		
		if (villageBiome.getCategory() == Biome.Category.DESERT ) {
			wallPerimeter = 23;
			wallType = Blocks.SANDSTONE_WALL.getDefaultState();
			gateBlockType = Blocks.SANDSTONE.getDefaultState();
		}
		if (villageBiome.getCategory() == Biome.Category.TAIGA) {
			wallPerimeter = 23;
			wallType = Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState();
		}
		
		int absvx = (int) Math.abs(ve.getPosX() - villageMeetingPlaceBlockPos.getX());
		int absvz = (int) Math.abs(ve.getPosZ() - villageMeetingPlaceBlockPos.getZ());
		boolean buildCenterGate = true;
		if (placeOneWallPiece(ve, regrowthType, wallPerimeter, wallTorchSpacing, wallCenter, gateBlockType, buildCenterGate, wallType,
				absvx, absvz)) {
			if (regrowthType.contains("t")) {
				if (isValidTorchLocation(wallPerimeter, wallTorchSpacing, absvx, absvz)) {
					ve.world.setBlockState(ve.getPosition().up(), Blocks.TORCH.getDefaultState());
				}
			}
			return true;
		}
		return false;
	}

	// villagers build protective walls around their homes. currently 32 out.
	// to do- reduce distance of wall from home.
	private boolean improveWallForPersonalHome(VillagerEntity ve, BlockPos vHomePos,String regrowthType ) {

		Biome villageBiome = ve.world.getBiome(vHomePos);
		int wallPerimeter = 5;
		int wallTorchSpacing = 4;
		final int wallCenter = 0;
		BlockState wallType = Blocks.OAK_FENCE.getDefaultState();
		BlockState gateBlockType = Blocks.GRASS_PATH.getDefaultState();
		
		if (villageBiome.getCategory() == Biome.Category.DESERT ) {
			wallPerimeter += 2;
			wallTorchSpacing = 6;
			wallType = Blocks.ACACIA_FENCE.getDefaultState();
			gateBlockType = Blocks.SANDSTONE.getDefaultState();
		}
		if (villageBiome.getCategory() == Biome.Category.TAIGA) {
			wallTorchSpacing = 3;
			wallType = Blocks.SPRUCE_FENCE.getDefaultState();
			gateBlockType = Blocks.SANDSTONE.getDefaultState();
		}

		int absvx = (int) Math.abs(ve.getPosX() - vHomePos.getX());
		int absvz = (int) Math.abs(ve.getPosZ() - vHomePos.getZ());
		boolean buildCenterGate = false;
		
		if (placeOneWallPiece(ve, regrowthType, wallPerimeter, wallTorchSpacing, wallCenter, gateBlockType, buildCenterGate, wallType,
				absvx, absvz)) {
			if (regrowthType.contains("t")) {
				if (isValidTorchLocation(wallPerimeter, wallTorchSpacing, absvx, absvz)) {
					ve.world.setBlockState(ve.getPosition().up(), Blocks.TORCH.getDefaultState());
				}
			}			
			return true;
		}
		return false;

	}

	private void improveWalls(VillagerEntity ve, Block footBlock, Block groundBlock, String key, String regrowthType,
								int veX, int veY, int veZ ) {
		Brain<VillagerEntity> vb = ve.getBrain();
		Optional<GlobalPos> vMeetingPlace = vb.getMemory(MemoryModuleType.MEETING_POINT);

		if (isOkayToBuildWallHere(ve, footBlock, groundBlock, vMeetingPlace, veX, veY, veZ)) {
			GlobalPos gVMP = vMeetingPlace.get();
			BlockPos villageMeetingPlaceBlockPos = gVMP.getPos();

			// place one block of a wall on the perimeter around village meeting place
			// Don't block any roads or paths regardless of biome.

			if (regrowthType.contains("w")) {
				if (improveWallForMeetingPlace(ve, regrowthType, villageMeetingPlaceBlockPos)) {
					if (MyConfig.aDebugLevel > 0) {
						System.out.println(key + " Meeting Place wall at " +  veX +", "+veY+", "+veZ+".");
					}
				}
			}

			// build a wall on perimeter of villager's home
			if (regrowthType.contains("p")) {
				Optional<GlobalPos> villagerHome = vb.getMemory(MemoryModuleType.HOME);
				if (villagerHome.isPresent()) {
					GlobalPos gVHP = villagerHome.get();
					BlockPos villagerHomePos = gVHP.getPos();
					// don't build personal walls inside the village wall perimeter.
					// don't build personal walls until the village has a meeting place.
					if (isOutsideMeetingPlaceWall(ve, vMeetingPlace, vMeetingPlace.get().getPos(), veX, veY, veZ)) {
						if (improveWallForPersonalHome(ve, villagerHomePos, regrowthType)) {
							if (MyConfig.aDebugLevel > 0) {
								System.out.println(key + " personal wall at " +veX+", "+veY+", "+veZ+".");
							}
						}
					}
				}
			}
		}
	}


	private boolean isFootBlockOkayToBuildIn (Block footBlock) {
		if (footBlock instanceof AirBlock) {
			return true;		
		}
		return false;
	}

	private boolean isGrassOrFlower(Block footBlock) {
		boolean grassOrFlower = false;
		if (footBlock instanceof GrassBlock) 
		{
			grassOrFlower = true;
		}
		if (footBlock instanceof TallGrassBlock) 
		{
			grassOrFlower = true;
		}
		return grassOrFlower;
	}

	private boolean isImpossibleRegrowthEvent(Block footBlock, String regrowthType) {
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

	private boolean isOkayToBuildWallHere(VillagerEntity ve, Block footBlock, Block groundBlock,
			Optional<GlobalPos> vMeetingPlace, int veX, int veY, int veZ) {
		boolean okayToBuildWalls = true;
		if (!(vMeetingPlace.isPresent())) {
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

	private boolean isOutsideMeetingPlaceWall(VillagerEntity ve, Optional<GlobalPos> vMeetingPlace, BlockPos meetingPlacePos,
												int veX, int veY, int veZ) {
		int absVMpX;  // these just making debugging easier.  remove them later?
		int absVMpZ;
			absVMpX = (int) Math.abs(veX - meetingPlacePos.getX());
			absVMpZ = (int) Math.abs(veZ - meetingPlacePos.getZ());
			if (( absVMpX < 32 )&&( absVMpZ < 32 )) return false;
		return true;
	}

	private boolean isValidGroundBlockToBuildWallOn (VillagerEntity ve,Block groundBlock) {
		int blockSkyLightValue = ve.world.getLightFor(LightType.SKY, ve.getPosition());
		int z=4;
		if (blockSkyLightValue < 14) {
			return false;
		}
		if ((groundBlock == Blocks.SAND)  ||
			(groundBlock == Blocks.GRASS) ||
			(groundBlock == Blocks.PODZOL)||
			(groundBlock == Blocks.FERN)) {
			return true;
		}
		if ((groundBlock == Blocks.GRASS_PATH) ||
		    (groundBlock == Blocks.SMOOTH_SANDSTONE) ||
		    (groundBlock == Blocks.GRAVEL) ||
		    (groundBlock == Blocks.HAY_BLOCK) ||
		    (groundBlock instanceof WallBlock) ||
		    (groundBlock instanceof FenceBlock)
			){
			return false;
		}
		return true;
	}
	
//	ItemStack iStk = new ItemStack(Items.BONE_MEAL,1);
//	BoneMealItem.applyBonemeal(iStk, e.world,e.getPosition());
// (likely 12.2 and 14.4 call?)	ib.func_225535_a_((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));\
	
	private boolean isValidTorchLocation(int wallPerimeter,int wallTorchSpacing, int absvx, int absvz) {
			if  ((absvx == wallPerimeter ) && ((absvz % wallTorchSpacing) == 1)) {
				return true;
			}
			if  (((absvx % wallTorchSpacing) == 1) && (absvz == wallPerimeter )) {
				return true;
			}
			if ((absvx == wallPerimeter) && (absvz == wallPerimeter)) {
				return true;
			}
			return false;
	}

	private boolean placeOneWallPiece(VillagerEntity ve, String regrowthType, 
			int wallPerimeter, int wallTorchSpacing, final int wallCenter, 
			BlockState gateBlockType, boolean buildCenterGate, BlockState wallType, 
			int absvx,int absvz) 
	{
		// Build North and South Walls (and corners)
		if (absvx == wallPerimeter) {
			if (absvz <= wallPerimeter) {
				if (absvz == wallCenter) {
					if (buildCenterGate) {
						ve.world.setBlockState(ve.getPosition().down(), gateBlockType);
						return true;
					} else {
						return false;
					}
				} else {
					ve.world.setBlockState(ve.getPosition(), wallType);
					return true;
				}
			}
		}
		// Build East and West Walls (and corners)
		if (absvz == wallPerimeter) {
			if (absvx <= wallPerimeter) {
				if (absvx == wallCenter) {
					if (buildCenterGate) {
						ve.world.setBlockState(ve.getPosition().down(), gateBlockType);
						return true;
					} else {
						return false;
					}
				} else {
					ve.world.setBlockState(ve.getPosition(), wallType);
					return true;
				}
			}
		}
		return false;
	}
}
	

