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
import net.minecraft.block.TallGrassBlock;
import net.minecraft.block.WallBlock;
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
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber()
public class MoveEntityEvent {
	
	static int[]dx= {1,0,-1,0};
	static int[]dz= {0,1,0,-1};
	
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

		EntityType<?> t = eventEntity.getType();
		ResourceLocation registryName = t.getRegistryName();
		String key = registryName.toString();
		RegrowthEntitiesManager.RegrowthMobItem r = RegrowthEntitiesManager.getRegrowthMobInfo(key);
		if (r==null) {  // This mob isn't configured
			return;
		}
		
		String regrowthType = r.getRegrowthType();
	
		// exit quickly if an impossible case.

		if (isImpossibleRegrowthEvent(footBlock, regrowthType)) {
			return;
		}
		
		double regrowthEventOdds = r.getRegrowthEventOdds();
		double nr = eventEntity.world.rand.nextDouble(); 

		if ((nr*10000.0) > regrowthEventOdds * 10.0) {
			if (eventEntity instanceof VillagerEntity) {
				VillagerEntity ve = (VillagerEntity) eventEntity;
				doVillagerRegrowthEvents(ve, footBlock, groundBlock, key, regrowthType);
			}
		}
	

		doMobRegrowthEvents(eventEntity, footBlock, groundBlock, key, regrowthType, regrowthEventOdds, nr);

		int debugBreakPoint = 0;

	}

	private void doMobRegrowthEvents(Entity eventEntity, Block footBlock, Block groundBlock, String key,
			String regrowthType, double regrowthEventOdds, double nr) {

		// all remaining actions currently require a grass block underfoot so if not a grass block- can exit now.

		if ((footBlock != Blocks.GRASS_BLOCK) && (groundBlock != Blocks.GRASS_BLOCK)) {
			return;
		}
		
		if ((nr*10000.0) < regrowthEventOdds) {
			if (regrowthType.equals("tall")) {
				entityGrowTallGrassToDoubleGrass(eventEntity, footBlock, regrowthType);
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " tall at " +  eventEntity.getPosX() +", "+eventEntity.getPosY()+", "+eventEntity.getPosZ()+", ");
				}
				return;
			}
		}	
		
		if ((regrowthType.equals("grow")) || (regrowthType.equals("both")) ) {
				if (footBlock instanceof AirBlock ) {
				if ((nr*10000.0) < regrowthEventOdds) {
		        	IGrowable ib = (IGrowable) groundBlock;
		    		ib.grow((ServerWorld)eventEntity.world, eventEntity.world.rand, eventEntity.getPosition(), eventEntity.world.getBlockState(eventEntity.getPosition()));    			
					if (MyConfig.aDebugLevel > 0) {
						System.out.println(key + " grow at " +  eventEntity.getPosX() +", "+eventEntity.getPosY()+", "+eventEntity.getPosZ()+", ");
					}
					return;
				}	
			}
		}
	
	
		if (isGrassOrFlower(footBlock) && ((nr*10000.0) < regrowthEventOdds * 15.0)) {
			entityEatGrassOrFlower(eventEntity, key, regrowthType, regrowthEventOdds, nr);
			return;
		}
		return;
	}

	private void doVillagerRegrowthEvents(VillagerEntity ve, Block footBlock, Block groundBlock, String key, String regrowthType )
	{



		// note all villagers may not have a home.  poor homeless villagers.
		//default = repair farms
		if (improveFarm(ve, groundBlock)) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " farm improved at " +  ve.getPosX() +", "+ve.getPosY()+", "+ve.getPosZ()+", ");
			}	
		};
		
		// c = cut down grass (but not flowers for now)
		// to do - maybe remove flowers unless by a road or elevated (air next to them as in the flower beds)
		// to do - replace "c" with a meaningful constant.
		
		if(regrowthType.contains("c")) {
			if (footBlock instanceof TallGrassBlock) {
				ve.world.destroyBlock(ve.getPosition(), false);
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " cut at " +  ve.getPosX() +", "+ve.getPosY()+", "+ve.getPosZ()+", ");
				}					
			}
		}
		// improve roads  
		// to do - replace "r" with a meaningful constant.
		if(regrowthType.contains("r")) {
			improveRoads(ve, footBlock, groundBlock, key, ve);
		}


		// note villages may not have a meeting place.  Sometimes they change.  Sometimes they take a few minutes to form.
		Brain<VillagerEntity> vb = ve.getBrain();
		Optional<GlobalPos> vMeetingPlace = vb.getMemory(MemoryModuleType.MEETING_POINT);
	
		boolean okayToBuildWalls = true;
		if (!(vMeetingPlace.isPresent())) {
			okayToBuildWalls = false;
		}
		if (!(isFootBlockOkayToBuildIn(footBlock))) {
			okayToBuildWalls = false;
		}
		if (!(isValidGroundBlockToBuildWallOn(groundBlock))) {
			okayToBuildWalls = false;			
		}


		if (okayToBuildWalls) {
			GlobalPos gVMP = vMeetingPlace.get();
			BlockPos villageMeetingPlaceBlockPos = gVMP.getPos();

			// place one block of a wall on the perimeter around village meeting place
			// Don't block any roads or paths regardless of biome.

			if (regrowthType.contains("w")) {
				if (improveWallForMeetingPlace(ve, regrowthType, villageMeetingPlaceBlockPos)) {
					if (MyConfig.aDebugLevel > 0) {
						System.out.println(key + " Meeting Place wall at " +  ve.getPosX() +", "+ve.getPosY()+", "+ve.getPosZ());
					}
				}

			}

			// build a wall on perimeter of villager's home
			Optional<GlobalPos> villagerHome = vb.getMemory(MemoryModuleType.HOME);
			if (villagerHome.isPresent()) {
				GlobalPos gVHP = villagerHome.get();
				BlockPos villagerHomePos = gVHP.getPos();
				// don't build personal walls inside the village wall perimeter.
				// don't build personal walls until the village has a meeting place.
				if (isOutsideMeetingPlaceWall(ve, vMeetingPlace, vMeetingPlace.get().getPos())) {
					if (regrowthType.contains("p")) {
						if (improveWallForPersonalHome(ve, villagerHomePos, regrowthType)) {
							if (MyConfig.aDebugLevel > 0) {
								System.out.println(key + " personal wall at " +  ve.getPosX() +", "+ve.getPosY()+", "+ve.getPosZ());
							}
						}
					}
				}

			}
		}

		if(regrowthType.contains("l")) {
			// upgrade lighting
		}
		
	}


	private void entityEatGrassOrFlower(Entity eventEntity, String key, String regrowthType, double regrowthEventOdds,
			double nr) {
		if ( (regrowthType.equals("eat"))  || 
			 (regrowthType.equals("both")) 
		    )
		{
			eventEntity.world.destroyBlock(eventEntity.getPosition(), false);
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " eat at " +  eventEntity.getPosX() +", "+eventEntity.getPosY()+", "+eventEntity.getPosZ()+", ");
			}
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
		}
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
	private boolean improveFarm(VillagerEntity ve, Block groundBlock) {
		boolean fixFarm = false;
		if (groundBlock instanceof GrassBlock) {
			for(int i=0; i<4; i++) {
				Block tempBlock = ve.world.getBlockState(new BlockPos (ve.getPosX()+dx[i],ve.getPosY()-1,ve.getPosZ()+dz[i])).getBlock();
				if (tempBlock == Blocks.FARMLAND) {
					fixFarm = true;
				}
			}
		}
		if (fixFarm) {
			ve.world.setBlockState(ve.getPosition().down(), Blocks.FARMLAND.getDefaultState());
			return true;
		}
		return false;
	}	
	private void improveRoads(Entity e, Block footBlock, Block groundBlock, String key, VillagerEntity ve) {
		BlockPos vePos = ve.getPosition();
		int veX =  vePos.getX();
		int veZ =  vePos.getZ();
		int veY =  vePos.getY();

		improveRoadsFixPotholes(e, groundBlock, key, ve, veX, veZ, veY);
		improveRoadsSmoothElevationChanges(e, footBlock, key, ve, veX, veZ, veY);
	}

	private void improveRoadsFixPotholes(Entity e, Block groundBlock, String key, VillagerEntity ve, int veX, int veZ,
			int veY) {
		// fix pot holes - grass spots in road with 3-4 grass blocks orthagonal to them.
		// to do remove "tempBlock" and put in iff statement.  Extract as method.
		// add biome support for dirt, sand, and podzol
		int grassPathCount = 0;
		if ((groundBlock == Blocks.DIRT)&&(groundBlock instanceof GrassBlock)) {
			for(int vi=-1; vi<2; vi++) { // on gentle slopes too
				int veYScan = veY + vi;
				for(int i=0; i<4; i++) {
					Block tempBlock = e.world.getBlockState(new BlockPos (veX+dx[i],veYScan,veZ+dz[i])).getBlock();
					if (tempBlock == Blocks.GRASS_PATH) {
						grassPathCount += 1;
					}
				}
		    } 
			if (grassPathCount >= 3) {
				e.world.setBlockState(ve.getPosition().down(), Blocks.GRASS_PATH.getDefaultState());
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " fix road " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
				}					
			}
		}
	}

	private void improveRoadsSmoothElevationChanges(Entity e, Block footBlock, String key, VillagerEntity ve, int veX,
			int veZ, int veY) {
		// to do remove "tempBlock" and put in iff statement.  Extract as method.			
		if (footBlock instanceof GrassPathBlock) {
			int verticleRoadSmoothingY = veY+2;
			boolean doRoadSmoothing = false;

			for(int i=0; i<4; i++) {
				Block tempBlock = e.world.getBlockState(new BlockPos (veX+dx[i],verticleRoadSmoothingY,veZ+dz[i])).getBlock();
				if (tempBlock == Blocks.GRASS_PATH) {
					doRoadSmoothing = true;
				}
			}
			if (doRoadSmoothing) {
				e.world.setBlockState(new BlockPos (veX,veY+1,veZ), Blocks.GRASS_PATH.getDefaultState());
				ve.setMotion(0.0, 0.4, 0.0);
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " fix road slope" +  veX +", "+ veY+", "+veZ+", ");
				}					
			}
			
		}
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
			wallType = Blocks.SANDSTONE_WALL.getDefaultState();
			gateBlockType = Blocks.SANDSTONE.getDefaultState();
		}
		if (villageBiome.getCategory() == Biome.Category.TAIGA) {
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

	private boolean isOutsideMeetingPlaceWall(VillagerEntity ve, Optional<GlobalPos> vMeetingPlace, BlockPos meetingPlacePos) {
		int absVMpX;  // these just making debugging easier.  remove them later?
		int absVMpZ;
			absVMpX = (int) Math.abs(ve.getPosX() - meetingPlacePos.getX());
			absVMpZ = (int) Math.abs(ve.getPosZ() - meetingPlacePos.getZ());
			if (( absVMpX < 32 )&&( absVMpZ < 32 )) return false;
		return true;
	}

	private boolean isValidGroundBlockToBuildWallOn (Block groundBlock) {
		if ((groundBlock == Blocks.GRASS_PATH) ||
		    (groundBlock == Blocks.SANDSTONE ) ||
		    (groundBlock == Blocks.GRAVEL) ||
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
	

