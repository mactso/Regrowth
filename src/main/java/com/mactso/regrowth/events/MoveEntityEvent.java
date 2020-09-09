package com.mactso.regrowth.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import com.mactso.regrowth.config.WallBiomeDataManager;
import com.mactso.regrowth.config.WallFoundationDataManager;
import com.mactso.regrowth.config.WallFoundationDataManager.wallFoundationItem;

import net.minecraft.block.AirBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.village.PointOfInterest;
import net.minecraft.village.PointOfInterestManager.Status;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber()
public class MoveEntityEvent {
	
	private static int[]dx= {1,0,-1,0};
	private static int[]dz= {0,1,0,-1};
	private static int TICKS_PER_SECOND = 20;	
	private static int[][]facingArray = {{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1},{1,0},{1,1}};
	private static int lastTorchX=0;
	private static int lastTorchY=0;
	private static int lastTorchZ=0;
	static final int WALL_CENTER = 0;
	static final int FENCE_CENTER = 0;
	static final int WALL_TYPE_WALL = -1;
	static final int WALL_TYPE_FENCE = -2;

	
	
	
	
	@SubscribeEvent
    public void doEntityMove(LivingUpdateEvent event) { 

	    // need to find the type of entity here.
		if (event.getEntity() == null) {
			return;
		}

		Entity eventEntity = event.getEntity();
		World world = eventEntity.world;
		
		if (world == null) {
			return;
		}
		
		if (!(world instanceof ServerWorld)) {
			return;
		}

		BlockPos debugBlockPos = getBlockPos(eventEntity);

		BlockState footBlockState = world.getBlockState(getBlockPos(eventEntity));
		if (footBlockState == null) {
			return;
		}	
		
		Block footBlock = footBlockState.getBlock();
		if (footBlock == null) {
			return;
		}
		
//		Future Pig Tree Sapling Feature Research
//		Set tdt = ForgeRegistries.FOLIAGE_PLACER_TYPES.getEntries();
//		List<Block> saplings = BlockTags.SAPLINGS.getAllElements();
		
//		xxzzy BYG/BOP mod support here
//		Tags for grass?
		BlockState groundBlockState = null;
		Block groundBlock = null;


	
		if (footBlock == Blocks.GRASS_PATH) {
			groundBlockState = footBlockState;
			groundBlock = footBlock;
		} else {
			groundBlockState = world.getBlockState(getBlockPos(eventEntity).down());	
			if (groundBlockState == null) {
				return;
			}
			groundBlock = groundBlockState.getBlock();	
		}

		if (groundBlock == null) {
			return;
		}

		BlockPos eventEntityPos = getBlockPos(eventEntity); // floats
		int eventX =  eventEntityPos.getX(); // Int
		int eventY =  eventEntityPos.getY(); // Int
		int eventZ =  eventEntityPos.getZ(); // Int
		Biome localBiome = world.getBiome(eventEntityPos);
		EntityType<?> tempType = eventEntity.getType();
		ResourceLocation registryName = tempType.getRegistryName();
		String registryNameAsString = registryName.toString();
		RegrowthEntitiesManager.RegrowthMobItem currentRegrowthMobItem = RegrowthEntitiesManager.getRegrowthMobInfo(registryNameAsString);

		if (currentRegrowthMobItem==null) {  // This mob isn't configured to do Regrowth Events
			return;
		}
		
		String regrowthActions = currentRegrowthMobItem.getRegrowthActions();
	
		if (isImpossibleRegrowthEvent(footBlock, regrowthActions)) {
			return;
		}
		
		double regrowthEventOdds = 2/currentRegrowthMobItem.getRegrowthEventSeconds() * TICKS_PER_SECOND;
		double randomD100Roll = eventEntity.world.rand.nextDouble() * 100; 
		int debugvalue = 100;
		if (randomD100Roll <= regrowthEventOdds + debugvalue) {
			if (eventEntity instanceof VillagerEntity) {
				VillagerEntity ve = (VillagerEntity) eventEntity;
				// if onGround
				if ((ve.isOnGround())&&(!(footBlock instanceof BedBlock))) {
					if (footBlock == Blocks.SNOW) {
						footBlock = Blocks.AIR;
					}
					doVillagerRegrowthEvents(ve, footBlock, groundBlock, eventEntityPos, registryNameAsString, regrowthActions,
							eventX, eventY, eventZ, world, localBiome);
				}
			}
		}
	
		doMobRegrowthEvents(eventEntity, footBlock, groundBlock, registryNameAsString, regrowthActions, regrowthEventOdds, randomD100Roll,
							eventX, eventY, eventZ, world, localBiome);

		int debugBreakPoint = 0;

	}

	private void doMobRegrowthEvents(Entity eventEntity, Block footBlock, Block groundBlock, String key,
			String regrowthType, double regrowthEventOdds, double randomD100Roll,
			int eX, int eY, int eZ, World world, Biome localBiome) {

		// all remaining actions currently require a grass block underfoot so if not a grass block- can exit now.
		// this is for performance savings only.
		// looks like meadow_grass_block is not a grassBlock
		if (!(groundBlock instanceof GrassBlock) 
			&& (!(groundBlock.getTranslationKey().equals("block.byg.meadow_grass_block")))
				) {
			return; 
		} 
		
		if ((regrowthType.equals("tall")) && (randomD100Roll <= regrowthEventOdds)){
			entityGrowTallGrassToDoubleGrass(eventEntity, footBlock, regrowthType);
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " tall at " +  eX +", "+ eY +", "+ eZ +".");
			}
			return;
		}	
		
		double eatingOdds = regrowthEventOdds;
		if  ((regrowthType.contentEquals("eat")) ||(regrowthType.contentEquals("both"))) {

			// Balance eating and growth odds/timing for "both" case.
			if  ((regrowthType.contentEquals("both"))) {
				eatingOdds = regrowthEventOdds * 15;				
			}
			if (eventEntity instanceof AbstractHorseEntity) {
				AbstractHorseEntity h = (AbstractHorseEntity) eventEntity;
				BlockPos debugPos = getBlockPos(eventEntity);
				if (!(h.isEatingHaystack())) {
					eatingOdds = 0.0;  // Horse, Donkey, Mule, (Llama?) only eat if animation eating.
				} else {
					eatingOdds = regrowthEventOdds * 25;   // Increase eating odds during eating animation.
				}
			}			
			if ((randomD100Roll <= eatingOdds)) {

				if (entityEatGrassOrFlower(eventEntity, getBlockPos(eventEntity), regrowthType, footBlock, groundBlock)) {
 					if (MyConfig.aDebugLevel > 0) {
						System.out.println(key + " eat at " +  eX +", "+ eY +", "+ eZ +".");
					}
				}
				return;
			}
		}

		randomD100Roll = eventEntity.world.rand.nextDouble() * 100; 
		if ( ((regrowthType.equals("grow")) || (regrowthType.equals("both"))) && (randomD100Roll <= regrowthEventOdds) ) {
			if (footBlock instanceof AirBlock ) {
	        	IGrowable ib = (IGrowable) groundBlock;
	        	if (ib == null) {
	        		return;
	        	}
				if (MyConfig.aDebugLevel > 1) {
					System.out.println(key + " trying to grow plants sat " +  eX +", "+ eY +", "+ eZ +".");
				}
	        	ib.grow((ServerWorld)eventEntity.world, eventEntity.world.rand, getBlockPos(eventEntity), eventEntity.world.getBlockState(getBlockPos(eventEntity)));    			
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " grow at " +  eX +", "+ eY +", "+ eZ +".");
				}
				return;
			}
		}
		
		return;
	}

	private void doVillagerRegrowthEvents(VillagerEntity ve, Block footBlock, Block groundBlock, BlockPos eventEntityPos, String key, String regrowthType,
											int veX, int veY, int veZ, World world, Biome localBiome)
	{
		// Villagers hopping, falling, etc. are doing improvements.
		if (!(isOnGround (ve))) {
			return;
		}
		// Give custom debugging names to nameless villagers.
		if (MyConfig.aDebugLevel > 0) {
			ITextComponent tName = new StringTextComponent ("");
			float veYaw=ve.getYaw(1.0f);
			tName = new StringTextComponent ("Reg-"+ veX+","+veZ+": " + veYaw);
			ve.setCustomName(tName);
		}
		else { // remove custom debugging names added by Regrowth
			if (ve.getCustomName() != null) {
				if (ve.getCustomName().toString().contains("Reg-")){
					ve.setCustomName(null);
				}
			}
		}
		
		// note all villagers may not have a home.  poor homeless villagers.
		//default = repair farms
		if (improveFarm(ve,groundBlock,footBlock, regrowthType,veX,veY,veZ)) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " farm improved at " +  +  veX +", "+veY +", "+veZ+", ");
			}	
		};
		
		// cut lea'v'es.
		// remove leaves if facing head height leaves

		if (regrowthType.contains("v")) {
			improveLeaves(ve, groundBlock, key, veX, veY, veZ);
		}
		
		// c = cut down grass (but not flowers for now)
		// to do - maybe remove flowers unless by a road or elevated (air next to them as in the flower beds)
		// to do - replace "c" with a meaningful constant.
		
		if(regrowthType.contains("c")) {
			if ((footBlock instanceof TallGrassBlock)
					||(footBlock instanceof DoublePlantBlock)
					||(footBlock.getTranslationKey().equals("block.byg.short_grass"))
					) {

				ve.world.destroyBlock(eventEntityPos, false);
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " cut at " +  veX +", "+veY+", "+veZ+".");
				}					
			}
		}
		// improve roads  
		// to do - replace "r" with a meaningful constant.f
		if(regrowthType.contains("r")) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " enter Improve Roads " +  veX +", "+veY+", "+veZ+".");
			}	
			improveRoads(ve, footBlock, groundBlock, key, localBiome);
		}


		// note villages may not have a meeting place.  Sometimes they change.  Sometimes they take a few minutes to form.
		if ((regrowthType.contains("w")||(regrowthType.contains("p")))) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " enter Improve Walls & Fences " +  veX +", "+veY+", "+veZ+".");
			}	
			improveWalls(ve, footBlock, groundBlock, key, regrowthType,veX,veY,veZ, localBiome);
				// "jump" villagers away if they are inside a wall or fence block.
				if ((footBlock instanceof WallBlock) || (footBlock instanceof FenceBlock)) {
					float veYaw=ve.getYaw(1.0f)/45;
					int facingNdx = Math.round(veYaw);
					if (facingNdx<0) {
						facingNdx = Math.abs(facingNdx);
					}
					facingNdx %= 8;
					double dx = (facingArray[facingNdx][0])/2.0;
					double dz = (facingArray[facingNdx][1])/2.0;
					ve.setMotion(dx, 0.55, dz);
				}
		}
		

		if((regrowthType.contains("t")&&(footBlock != Blocks.TORCH))) {
			if (improveLighting(ve, footBlock, groundBlock, veX, veY, veZ, localBiome)) {
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key +"- "+ footBlock +", " + groundBlock +" pitch: "+ve.rotationPitch + " improve lighting at " +  ve.getPosX() +", "+ve.getPosY()+", "+ve.getPosZ());
				}				
			}
		}
	}

	private boolean entityEatGrassOrFlower(Entity eventEntity, BlockPos eventEntityPos, String regrowthType, Block footBlock, Block groundBlock) {

		if (MyConfig.aDebugLevel > 0) {
			System.out.println("entityEatGrassOrFlower Enter:");
		}
		if (!(isGrassOrFlower(footBlock))) {
			return false;
		}
		if ( !(regrowthType.equals("eat")) && 
			 !(regrowthType.equals("both")) ) {
			return false;
		}
		eventEntity.world.destroyBlock(eventEntityPos, false);
		double randomD100Roll = eventEntity.world.rand.nextDouble() * 100;
		// note bop: origin grass and bop: meadow grass do recover but maybe slower than normal grass.
		if ((randomD100Roll >40) && (groundBlock instanceof GrassBlock)) {
			eventEntity.world.setBlockState(eventEntityPos.down(), Blocks.DIRT.getDefaultState());
			int evX = eventEntityPos.getX(); int evY = eventEntityPos.getY(); int evZ = eventEntityPos.getZ();
			int dirtCount = 0;
			for(int i=0;i<4; i++) {
				Block tempBlock = eventEntity.world.getBlockState(new BlockPos (evX+dx[i],evY,evZ+dz[i])).getBlock();
				if ((tempBlock == Blocks.GRASS_PATH) || (tempBlock == Blocks.DIRT)) {
					dirtCount += 1;
				}
			}
			if (dirtCount == 4) {
				eventEntity.world.setBlockState(new BlockPos (evX,evY,evZ), Blocks.GRASS_PATH.getDefaultState());
			}
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
		return true;
	}

	private boolean entityGrowTallGrassToDoubleGrass(Entity eventEntity, Block footBlock, String regrowthType) {
		if (footBlock instanceof TallGrassBlock ) {
			IGrowable ib = (IGrowable) footBlock;
			ib.grow((ServerWorld)eventEntity.world, eventEntity.world.rand, getBlockPos(eventEntity), eventEntity.world.getBlockState(getBlockPos(eventEntity)));    			
			return true;
		}
		return false;
	}

	private BlockState getBiomeRoadBlockType(Biome localBiome) {
		BlockState gateBlockType = Blocks.GRASS_PATH.getDefaultState();
		if (localBiome.getCategory() == Biome.Category.DESERT ) {
			gateBlockType = Blocks.SMOOTH_SANDSTONE.getDefaultState();  // 16.1 mojang change
		}
		return gateBlockType;
	}


	private BlockPos getBlockPos (Entity e) {
		return e.getPosition();
	}

	// if a grassblock in village has farmland next to it on the same level- retill it. 
	// todo add hydration check before tilling land.
	private boolean improveFarm(VillagerEntity ve, Block groundBlock, Block footBlock, String regrowthType,
								int veX, int veY, int veZ) {
		boolean nextToFarmBlock = false;
		boolean nextToWaterBlock = false;
		boolean torchExploit = false;
		
		
		if ( ve.getVillagerData().getProfession() == VillagerProfession.FARMER) {
			if ((lastTorchX==veX) && (lastTorchY == veY) && (lastTorchZ == veZ)) {
				torchExploit = true;
			}
			for(int i=0; i<4; i++) {
				Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY-1,veZ+dz[i])).getBlock();
				if (tempBlock == Blocks.FARMLAND) {
					nextToFarmBlock = true;
					i = 4;
				}
			}
			

			if ((BlockTags.LOGS.contains(groundBlock))){
				for(int i=0; i<4; i++) {
					Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY-1,veZ+dz[i])).getBlock();
					if (tempBlock == Blocks.WATER) {
						nextToWaterBlock = true;
						i = 4;
					}
				}
				if (regrowthType.contains("t") && (nextToWaterBlock)) {
					if ((footBlock == Blocks.AIR) && (!torchExploit)) {
						ve.world.setBlockState( getBlockPos(ve), Blocks.TORCH.getDefaultState());
						lastTorchX=veX;
						lastTorchY=veY;
						lastTorchZ=veZ;
					}
				}
			}

			if (nextToFarmBlock) {
				if (groundBlock == Blocks.SMOOTH_SANDSTONE) {
					if (regrowthType.contains("t")) {
						ve.world.setBlockState(getBlockPos(ve), Blocks.TORCH.getDefaultState());
						lastTorchX=veX;
						lastTorchY=veY;
						lastTorchZ=veZ;
					}
				}
				if (groundBlock instanceof GrassBlock) {
					ve.world.setBlockState(getBlockPos(ve).down(), Blocks.FARMLAND.getDefaultState());
					return true;
				}
			}
			
		}
		return false;
	}
	
	private void improveLeaves(VillagerEntity ve, Block groundBlock, String key, int veX, int veY, int veZ) {
		float veYaw=ve.getYaw(1.0f)/45;
		int facingNdx = Math.round(veYaw);
		if (facingNdx<0) {
			facingNdx = Math.abs(facingNdx);
		}
		facingNdx %= 8;

		// when standing on a grass path- game reports you 1 block lower.  Adjust.
		if (groundBlock == Blocks.GRASS_PATH) {
			veY +=1;
		}
		int dx = facingArray[facingNdx][0];
		int dz = facingArray[facingNdx][1];
		BlockPos tmpBP = null;
		BlockState tempBS = null;
		Block tempBlock = null;
		boolean destroyBlock = false;
		for (int iY=0;iY<2;iY++) {
			tmpBP = new BlockPos (veX+dx,veY+iY,veZ+dz);
			tempBS = ve.world.getBlockState(tmpBP);
			tempBlock = tempBS.getBlock();
			if (tempBlock instanceof LeavesBlock) {
				boolean persistantLeaves = tempBS.get(LeavesBlock.PERSISTENT);
				if (!(persistantLeaves)) {
					destroyBlock = true;
				}
			}
			if 	((tempBlock instanceof CactusBlock)) {
				destroyBlock = true;
			}
			if (destroyBlock) {
				ve.world.destroyBlock(tmpBP, false);
				destroyBlock = false;
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " clear "+ tempBlock.getTranslationKey().toString() +" at" +  +  veX +", "+veY+iY +", "+veZ+", ");
				}
			}
		}
	}

	private boolean improveLighting(VillagerEntity ve, Block footBlock, Block groundBlock,
							int veX, int veY, int veZ, Biome localBiome ) {
		int blockLightValue = ve.world.getLightFor(LightType.BLOCK, getBlockPos(ve));

		int skyLightValue = ve.world.getLightFor(LightType.SKY, getBlockPos(ve));
		if (blockLightValue > 8) return false;
		if (skyLightValue > 13) return false; 

		if (ve.isSleeping()) { 
			return false;
		}
		if (footBlock instanceof BedBlock) {
			return false;
		}
		
		int x= 2;
		if (isValidGroundBlockToPlaceTorchOn(ve, groundBlock) && (footBlock instanceof AirBlock)) {
			ve.world.setBlockState(getBlockPos(ve), Blocks.TORCH.getDefaultState());
		}
		
		return true;

	}

	private void improveRoads(VillagerEntity ve, Block footBlock, Block groundBlock, String key, Biome localBiome) {
		BlockPos vePos = getBlockPos(ve);
		int veX =  vePos.getX();
		int veY =  vePos.getY();
		int veZ =  vePos.getZ();
		
		if (improveRoadsFixUnfinished(ve, groundBlock,veX, veY, veZ, localBiome)) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " fix road " +  veX +", "+veY+", "+veZ+". ");
			}	
		}
		if (improveRoadsSmoothHeight(ve, footBlock, groundBlock, veX, veY, veZ, localBiome)) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " smooth road slope" +  veX +", "+ veY+", "+veZ+", ");
			}
			
		}
	}

	private boolean improveRoadsFixUnfinished(VillagerEntity ve, Block groundBlock,  
			int veX, int veY, int veZ, Biome localBiome) {
		// fix unfinished spots in road with 3-4 grass blocks orthagonal to them.
		// on slopes too.
		int fixHeight = 3;
		if (Biome.Category.TAIGA == localBiome.getCategory()) {
			fixHeight = 5;
		}
		Block biomeRoadBlock = getBiomeRoadBlockType(localBiome).getBlock();
		if (groundBlock != biomeRoadBlock) {
			int roadBlockCount = 0;
			for(int dy=-1; dy<=fixHeight; dy++) {
				for(int i=0; i<4; i++) {
					Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY+dy,veZ+dz[i])).getBlock();
					if (tempBlock == biomeRoadBlock) {
						roadBlockCount += 1;
						if (roadBlockCount > 2) {
							ve.world.setBlockState(getBlockPos(ve).down(), biomeRoadBlock.getDefaultState());
							return true;
						}			
					}
				}
			}	
		}
		return false;
	}

	private boolean improveRoadsSmoothHeight(VillagerEntity ve, Block footBlock, Block groundBlock, 
													int veX, int veY, int veZ, Biome localBiome) {
		// to do remove "tempBlock" and put in iff statement.  Extract as method.


		int skyLightValue = ve.world.getLightFor(LightType.SKY, getBlockPos(ve));
		// don't smooth "inside".
		if (skyLightValue <14) {
			return false;
		}

		Block biomeRoadBlock = getBiomeRoadBlockType(localBiome).getBlock();
		
		if ((groundBlock != biomeRoadBlock) && (footBlock != biomeRoadBlock)){
			return false;
		}

		
		BlockState smoothingBlockState = biomeRoadBlock.getDefaultState();		
		Block smoothingBlock = biomeRoadBlock;

		// Check for higher block to smooth up towards
		int poiDistance = 3;
		String key = "minecraft:"+localBiome.getCategory().toString();
		key = key.toLowerCase();
		if (key.equals("minecraft:desert")) {
			poiDistance = 7;
		}

// 08/30/20 Collection pre 16.2 bug returns non empty collections.
//		the collection is not empty when it should be.
// 	    are returned in the collection so have to loop thru it manually.

		Collection<PointOfInterest> result = ((ServerWorld) ve.world).getPointOfInterestManager()
				.getInSquare(t -> true, ve.getPosition(), poiDistance, Status.ANY)
				.collect(Collectors.toCollection(ArrayList::new));

		if (!(result.isEmpty())) {
			Iterator<PointOfInterest> i = result.iterator();
			while (i.hasNext()) { // in 16.1, finds the point of interest.
				PointOfInterest P = i.next();
				int disX = Math.abs(ve.getPosition().getX() - P.getPos().getX());
				int disZ = Math.abs(ve.getPosition().getZ() - P.getPos().getZ());
				if ((disX < poiDistance) && (disZ < poiDistance)) {
					if (MyConfig.aDebugLevel > 1) {
						System.out.println("Point of Interest Found: " + P.getType().toString() + ", " + veX + " " + veY + " " + veZ +".");
					}
					return false;
				}
			}
		}

		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem(key);
		
		int yAdjust = 0;
		if (smoothingBlock == Blocks.GRASS_PATH) {
			yAdjust = 1;
		}
		for (int dy=1+yAdjust;dy<5+yAdjust;dy++) {
			for(int i=0;i<4; i++) {
				Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY+dy,veZ+dz[i])).getBlock();
//				System.out.println ("MoveEntity: " + (veX+dx[i]) + " " + (veY+dy)+ " " + (veZ+dz[i]) +": "+ tempBlock.toString());
				
				if (tempBlock == smoothingBlock) {
					ve.world.setBlockState(new BlockPos (veX,veY+yAdjust,veZ), smoothingBlockState);
					ve.setMotion(0.0, 0.4, 0.0);
					return true;
				}
			}
		}

		return false;
	}

	private boolean improveWallForMeetingPlace(VillagerEntity ve, 
			String regrowthActions, BlockPos villageMeetingPlaceBlockPos, Block groundBlock, Block footBlock, Biome localBiome) {

		int debug3= 1;
		String key = "minecraft:"+localBiome.getCategory().toString();
		ResourceLocation biomeName = ForgeRegistries.BIOMES.getKey(localBiome);
		key = key.toLowerCase();
		int dbg = 3;
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem(key);
		if (MyConfig.aDebugLevel == 1) {
			System.out.println("111 WallbiomeData Key:" + key + " at " + (int) ve.getPosX() +", "+(int) ve.getPosY() +", "+(int) ve.getPosZ() +".");
		}
		if (currentWallBiomeDataItem == null) {
			if (MyConfig.aDebugLevel == 2) {
				System.out.println("222 WallbiomeData was null at " + (int) ve.getPosX() + ":, "+(int) ve.getPosY() +", "+(int) ve.getPosZ() +".");

			}
			key = "minecraft:"+localBiome.getCategory().toString().toLowerCase();
			currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem(key);
			if (currentWallBiomeDataItem == null) {
			currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem("minecraft:plains");
			}
		}
		int wallPerimeter = currentWallBiomeDataItem.getWallDiameter();
		if (wallPerimeter < 32) wallPerimeter = 32;
		if (wallPerimeter > 80) wallPerimeter = 80;

		
		wallPerimeter = (wallPerimeter/2) - 1;

		int absvx = (int) Math.abs(ve.getPosX() - villageMeetingPlaceBlockPos.getX());
		int absvz = (int) Math.abs(ve.getPosZ() - villageMeetingPlaceBlockPos.getZ());

		if (isOnWallPerimeter(wallPerimeter, absvx, absvz)) {
			if (MyConfig.aDebugLevel > 1) {
				System.out.println("222: Villager on wall perimeter " + wallPerimeter + " at" + (int) ve.getPosX()
						+ ", " + (int) ve.getPosY() + ", " + (int) ve.getPosZ() + ".");
			}

			// check for other meeting place bells blocking wall since too close.
			Collection<PointOfInterest> result = ((ServerWorld) ve.world).getPointOfInterestManager()
					.getInSquare(t -> t == PointOfInterestType.MEETING, ve.getPosition(), 41, Status.ANY)
					.collect(Collectors.toCollection(ArrayList::new));

			// 08/30/20  Collection had bug with range that I couldn't resolve.
			boolean buildWall = true;
			if (!(result.isEmpty())) {
				Iterator<PointOfInterest> i = result.iterator();
				while (i.hasNext()) { // in 16.1, finds the point of interest.
					PointOfInterest P = i.next();
					if ((villageMeetingPlaceBlockPos.getX() == P.getPos().getX())
							&& (villageMeetingPlaceBlockPos.getY() == P.getPos().getY())
							&& (villageMeetingPlaceBlockPos.getZ() == P.getPos().getZ())) {
						continue; // ignore meeting place that owns this wall segment.
					} else {
						int disX = Math.abs(ve.getPosition().getX() - P.getPos().getX());
						int disZ = Math.abs(ve.getPosition().getZ() - P.getPos().getZ());
						if ((disX < wallPerimeter) && (disZ < wallPerimeter)) {
							buildWall = false;  // another meeting place too close.  cancel wall.
							break;
						}
					}
				}
			} else if ((result.isEmpty())) {
				buildWall = true;
				if (MyConfig.aDebugLevel > 1) {
					System.out.println("222: No extra meeting places found by villager on wall perimeter at"
							+ (int) ve.getPosX() + ", " + (int) ve.getPosY() + ", " + (int) ve.getPosZ() + ".");
				}
			}

			if (buildWall) {

				BlockState wallTypeBlockState = currentWallBiomeDataItem.getWallBlockState();
				if (wallTypeBlockState == null) {
					wallTypeBlockState = Blocks.COBBLESTONE_WALL.getDefaultState();
				}
				BlockState wallBlock = wallTypeBlockState;
				BlockState gateBlockType = getBiomeRoadBlockType(localBiome);
				if (MyConfig.aDebugLevel > 1) {
					System.out.println(
							"222: wallBlock (" + wallTypeBlockState.getBlock().getRegistryName().toString() + ") at"
									+ (int) ve.getPosX() + ", " + (int) ve.getPosY() + ", " + (int) ve.getPosZ() + ".");
				}

				int wallTorchSpacing = (wallPerimeter + 1) / 4;
				final int wallCenter = 0;
				boolean buildCenterGate = true;
				if (placeOneWallPiece(ve, regrowthActions, wallPerimeter, wallTorchSpacing, gateBlockType,
						buildCenterGate, wallBlock, absvx, absvz, groundBlock, footBlock)) {
					if (MyConfig.aDebugLevel > 1) {
						System.out.println("222: wall built at" + (int) ve.getPosX() + ", " + (int) ve.getPosY() + ", "
								+ (int) ve.getPosZ() + ".");
					}
					if (regrowthActions.contains("t")) {
						if (isValidTorchLocation(wallPerimeter, wallTorchSpacing, absvx, absvz,
								ve.world.getBlockState(getBlockPos(ve)).getBlock())) {
							ve.world.setBlockState(getBlockPos(ve).up(), Blocks.TORCH.getDefaultState());
						}
					}
					return true;
				}
				if (MyConfig.aDebugLevel > 1) {
					System.out.println("222: wall NOT built at" + (int) ve.getPosX() + ", " + (int) ve.getPosY() + ", "
							+ (int) ve.getPosZ() + ".");
				}
			}
		}

		return false;
	}

	// villagers build protective walls around their homes. currently 32 out.
	// to do- reduce distance of wall from home.
	private boolean improveHomeFence(VillagerEntity ve, BlockPos vHomePos, String regrowthActions, Block groundBlock,
			Block footBlock, Biome localBiome) {

		String key = "minecraft:" + localBiome.getCategory().toString();
		ResourceLocation biomeName = ForgeRegistries.BIOMES.getKey(localBiome);
		key = key.toLowerCase();
		int dbg = 3;
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager
				.getWallBiomeDataItem(key);
		if (MyConfig.aDebugLevel == 1) {
			Block fence = currentWallBiomeDataItem.getFenceBlockState().getBlock();
			System.out.println("111 improveWallForPersonalHome: WallbiomeData Key:" + key + " at " + (int) ve.getPosX()
					+ ", " + (int) ve.getPosY() + ", " + (int) ve.getPosZ() + ".");
		}

		if (currentWallBiomeDataItem == null) {
			if (MyConfig.aDebugLevel == 2) {
				System.out.println("222 WallbiomeData was null at " + (int) ve.getPosX() + ":, " + (int) ve.getPosY()
						+ ", " + (int) ve.getPosZ() + ".");

			}

			key = "minecraft:" + localBiome.getCategory().toString().toLowerCase();
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

		int absvx = (int) Math.abs(ve.getPosX() - vHomePos.getX());
		int absvz = (int) Math.abs(ve.getPosZ() - vHomePos.getZ());

		Collection<PointOfInterest> result = ((ServerWorld) ve.world).getPointOfInterestManager()
				.getInSquare(t -> t == PointOfInterestType.HOME, ve.getPosition(), 17, Status.ANY)
				.collect(Collectors.toCollection(ArrayList::new));

		// 08/30/20 Collection had bug with range that I couldn't resolve.
		boolean buildWall = true;
		if (!(result.isEmpty())) {
			Iterator<PointOfInterest> i = result.iterator();
			while (i.hasNext()) { // in 16.1, finds the point of interest.
				PointOfInterest P = i.next();
				if ((vHomePos.getX() == P.getPos().getX())
						&& (vHomePos.getY() == P.getPos().getY())
						&& (vHomePos.getZ() == P.getPos().getZ())) {
					continue; // ignore meeting place that owns this wall segment.
				} else {
					int disX = Math.abs(ve.getPosition().getX() - P.getPos().getX());
					int disZ = Math.abs(ve.getPosition().getZ() - P.getPos().getZ());
					if (MyConfig.aDebugLevel > 0) {
						int veX = ve.getPosition().getX();
						int veY = ve.getPosition().getX();
						int veZ = ve.getPosition().getZ();
						int poX = ve.getPosition().getX();
						int poY = ve.getPosition().getX();
						int poZ = ve.getPosition().getZ();
						System.out.println(key + " Extra Point of Interest:"+ P.getType().toString() + " at "+  poX +", "+poY+", "+poZ+"."+ " for Villager at "+  veX +", "+veY+", "+veZ+".");
					}	

					if ((disX < homeFenceDiameter) && (disZ < homeFenceDiameter)) {
						buildWall = false; // another meeting place too close. cancel wall.
						break;
					}
				}
			}
		} else if ((result.isEmpty())) {
			buildWall = true;
			if (MyConfig.aDebugLevel > 1) {
				System.out.println("222: No extra beds found by villager on fence perimeter at" + (int) ve.getPosX()
						+ ", " + (int) ve.getPosY() + ", " + (int) ve.getPosZ() + ".");
			}
		}

		if (buildWall) {

			BlockState fenceBlockState = currentWallBiomeDataItem.getFenceBlockState();
			if (fenceBlockState == null) {
				fenceBlockState = Blocks.OAK_FENCE.getDefaultState();
			}
			BlockState wallBlock = fenceBlockState;
			BlockState gateBlockType = getBiomeRoadBlockType(localBiome);
			if (MyConfig.aDebugLevel > 1) {
				System.out.println("222: wallBlock (" + fenceBlockState.getBlock().getRegistryName().toString()
						+ ") at" + (int) ve.getPosX() + ", " + (int) ve.getPosY() + ", " + (int) ve.getPosZ() + ".");
			}

			boolean buildCenterGate = true;
			if (placeOneWallPiece(ve, regrowthActions, homeFenceDiameter, wallTorchSpacing, gateBlockType, buildCenterGate,
					wallBlock, absvx, absvz, groundBlock, footBlock)) {
				if (MyConfig.aDebugLevel > 1) {
					System.out.println("222: wall built at" + (int) ve.getPosX() + ", " + (int) ve.getPosY() + ", "
							+ (int) ve.getPosZ() + ".");
				}
				if (regrowthActions.contains("t")) {
					if (isValidTorchLocation(homeFenceDiameter, wallTorchSpacing, absvx, absvz,
							ve.world.getBlockState(getBlockPos(ve)).getBlock())) {
						ve.world.setBlockState(getBlockPos(ve).up(), Blocks.TORCH.getDefaultState());
					}
				}
				return true;
			}
			if (MyConfig.aDebugLevel > 1) {
				System.out.println("222: fence NOT built at" + (int) ve.getPosX() + ", " + (int) ve.getPosY() + ", "
						+ (int) ve.getPosZ() + ".");
			}
		}

		return false;
	}

	private void improveWalls(VillagerEntity ve, Block footBlock, Block groundBlock, String key, String regrowthType,
								int veX, int veY, int veZ, Biome localBiome ) {
		
		Brain<VillagerEntity> vb = ve.getBrain();
		Optional<GlobalPos> vMeetingPlace = vb.getMemory(MemoryModuleType.MEETING_POINT);
		if (!(vMeetingPlace.isPresent())) {
			return;
		}
		
		if (isOkayToBuildWallHere(ve, footBlock, groundBlock, veX, veY, veZ)) {
			GlobalPos gVMP = vMeetingPlace.get();
			BlockPos villageMeetingPlaceBlockPos = gVMP.getPos();

			// place one block of a wall on the perimeter around village meeting place
			// Don't block any roads or paths regardless of biome.

			if (regrowthType.contains("w")) {
				if (MyConfig.aDebugLevel > 1) {
					System.out.println(key + " Checking Improve Wall " +  veX +", "+veY+", "+veZ+".");
				}	
				if (improveWallForMeetingPlace(ve, regrowthType, villageMeetingPlaceBlockPos, groundBlock, footBlock, localBiome)) {
					if (MyConfig.aDebugLevel > 0) {
						System.out.println(key + "222 Meeting Place improved wall at " +  veX +", "+veY+", "+veZ+".");
					}
				}
			}

			// build a wall on perimeter of villager's home
			if (regrowthType.contains("p")) {
				if (MyConfig.aDebugLevel > 1) {
					System.out.println(key + " Checking Improve Fence " +  veX +", "+veY+", "+veZ+".");
				}	

				Optional<GlobalPos> villagerHome = vb.getMemory(MemoryModuleType.HOME);
				if (!(villagerHome.isPresent())) {
					return;
				}
				GlobalPos gVHP = villagerHome.get();
				BlockPos villagerHomePos = gVHP.getPos();
				// don't build personal walls inside the village wall perimeter.
				// don't build personal walls until the village has a meeting place.
				if (isOutsideMeetingPlaceWall(ve, vMeetingPlace, vMeetingPlace.get().getPos(), veX, veY, veZ,
						localBiome)) {
					if (MyConfig.aDebugLevel > 1) {
						System.out.println(key + " Improve Fence Outside Meeting Place Wall " +  veX +", "+veY+", "+veZ+".");
					}	

					if (improveHomeFence(ve, villagerHomePos, regrowthType, groundBlock, footBlock, localBiome)) {
						if (MyConfig.aDebugLevel > 0) {
							System.out.println(key + " personal wall at " + veX + ", " + veY + ", " + veZ + ".");
						}
					}
				}
			}
		}
	}


	private boolean isFootBlockOkayToBuildIn (Block footBlock) {
		if ((footBlock instanceof AirBlock)||(isGrassOrFlower(footBlock))) {
			return true;		
		}
		return false;
	}

	private boolean isGrassOrFlower(Block footBlock) {
		
		if (footBlock instanceof TallGrassBlock) 
		{
			return true;
		}
		if (footBlock instanceof FlowerBlock) 
		{
			return true;
		}
		if (footBlock instanceof DoublePlantBlock) 
		{
			return true;
		}
		if (footBlock == Blocks.FERN) 
		{
			return true;
		}
		if (footBlock == Blocks.LARGE_FERN) 
		{
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
        }
        catch (Exception e) {
    		if (MyConfig.aDebugLevel > 0) {
    			System.out.println("Tag Exception 1009-1014:" + footBlock.getTranslationKey() + ".");
    		}
        }
        // biomes you'll go grass compatibility
        if (footBlock.getTranslationKey() == "block.byg.short_grass") {
        	return true;
        }
		if (MyConfig.aDebugLevel > 0) {
			System.out.println("Not grass or Flower:" + footBlock.getTranslationKey() + ".");
		}
        return false;
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
			int veX, int veY, int veZ) {
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

	private boolean isOnGround (Entity e) {
		return e.isOnGround();
	}

	private boolean isOnWallPerimeter(int wallPerimeter, int absvx, int absvz) {
		boolean scratch = false;
		if ((absvx == wallPerimeter) && (absvz <= wallPerimeter)) scratch = true;
		if ((absvz == wallPerimeter) && (absvx <= wallPerimeter)) scratch = true;
		return scratch;
	}
	
//	ItemStack iStk = new ItemStack(Items.BONE_MEAL,1);
//	BoneMealItem.applyBonemeal(iStk, e.world,e.getPosition());
// (likely 12.2 and 14.4 call?)	ib.func_225535_a_((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));\
	
	private boolean isOutsideMeetingPlaceWall(VillagerEntity ve, Optional<GlobalPos> vMeetingPlace, BlockPos meetingPlacePos,
												int veX, int veY, int veZ, Biome localBiome) {

		String key = "minecraft:"+localBiome.getCategory().toString();
		ResourceLocation biomeName = ForgeRegistries.BIOMES.getKey(localBiome);
		key = key.toLowerCase();
		int dbg = 3;
		int fenceDiameter= 64;
		WallBiomeDataManager.WallBiomeDataItem currentWallBiomeDataItem = WallBiomeDataManager.getWallBiomeDataItem(key);
		if (!(currentWallBiomeDataItem == null)) {
			fenceDiameter = currentWallBiomeDataItem.getWallDiameter();
		}
		fenceDiameter = (fenceDiameter/ 2) - 1;
		int absVMpX = (int) Math.abs(veX - meetingPlacePos.getX());;  
		int absVMpZ = (int) Math.abs(veZ - meetingPlacePos.getZ());
		if (( absVMpX > fenceDiameter+1)) return true;
		if (( absVMpZ > fenceDiameter+1)) return true;
		return false;
		
	}

	private boolean isValidGroundBlockToPlaceTorchOn (VillagerEntity ve,Block groundBlock) {

		String key = groundBlock.getRegistryName().toString(); // broken out for easier debugging
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager.getWallFoundationInfo(key);
		if (currentWallFoundationItem==null) return false;		
		
		return true;
		
	}	
	
	private boolean isValidGroundBlockToBuildWallOn (VillagerEntity ve,Block groundBlock) {

		int blockSkyLightValue = ve.world.getLightFor(LightType.SKY, getBlockPos(ve));

		if (blockSkyLightValue < 13) return false;
		
		String key = groundBlock.getRegistryName().toString(); // broken out for easier debugging
		WallFoundationDataManager.wallFoundationItem currentWallFoundationItem = WallFoundationDataManager.getWallFoundationInfo(key);
		if (currentWallFoundationItem==null) return false;		
		
		return true;
		
	}
	
	private boolean isValidTorchLocation(int wallPerimeter,int wallTorchSpacing, int absvx, int absvz, Block wallFenceBlock) {

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
			int wallPerimeter, int wallTorchSpacing, BlockState gateBlockType, 
			boolean buildCenterGate, BlockState wallType, int absvx, 
			int absvz,Block groundBlock, Block footBlock) 
	{
		if (MyConfig.aDebugLevel > 1) {
			System.out.println("222: Enter :placeOneWallPiece: " + (int) ve.getPosX() +", "+ (int) ve.getPosY()+", "+ (int) ve.getPosZ()+".");
		}
		
		BlockState bs = ve.world.getBlockState(getBlockPos(ve).down());
		Block blockBs = bs.getBlock();

		if (MyConfig.aDebugLevel > 1) {
			System.out.println("222: About to Place wall piece: " + (int) ve.getPosX() +", "+ (int) ve.getPosY()+", "+ (int) ve.getPosZ()+".");
			System.out.println("222: wallPerimeter:"+wallPerimeter+" absvx:"+absvx +" absvz:"+absvz+ " l= " + (int) ve.getPosX() +" "+ (int) ve.getPosY()+" "+ (int) ve.getPosZ()+".");
		}


		// Build North and South Walls (and corners)
		if (absvx == wallPerimeter) {
			if (absvz <= wallPerimeter) {
				if (MyConfig.aDebugLevel > 1) {
					System.out.println("222: placeOneWallPiece Calling ns Wall Helper absvz : " + (int) ve.getPosX() +", "+ (int) ve.getPosY()+", "+ (int) ve.getPosZ()+".");
				}
				return placeWallHelper(ve, gateBlockType, buildCenterGate, wallType, absvz);
			}
		}
		// Build East and West Walls (and corners)
		if (absvz == wallPerimeter) {
			if (absvx <= wallPerimeter) {
				if (MyConfig.aDebugLevel > 1) {
					System.out.println("222: placeOneWallPiece Calling ew Wall Helper absvx: " + (int) ve.getPosX() +", "+ (int) ve.getPosY()+", "+ (int) ve.getPosZ()+".");
				}
				return placeWallHelper(ve, gateBlockType, buildCenterGate, wallType, absvx);
			}
		}
		return false;
	}

	
	private boolean placeWallHelper(VillagerEntity ve, BlockState gateBlockType, boolean buildCenterGate,
			BlockState wallType, int absva) {

		if (MyConfig.aDebugLevel > 1) {
			System.out.println("222: Enter :placeWallHelper: " + (int) ve.getPosX() +", "+ (int) ve.getPosY()+", "+ (int) ve.getPosZ()+".");
		}
		if (absva == WALL_CENTER) {
			if (MyConfig.aDebugLevel > 1) {
				System.out.println("222: Placing gatez at: " + (int) ve.getPosX() +", "+ (int) ve.getPosY()+", "+ (int) ve.getPosZ()+".");
			}
			if (buildCenterGate) {
				ve.world.setBlockState(getBlockPos(ve).down(), gateBlockType);
				return true;
			} else {
				return false;
			}
		} else {
			if (MyConfig.aDebugLevel > 1) {
				System.out.println("222: Wall Helper about to set wallBlock ("+ wallType.getBlock().getRegistryName().toString() + ") at" + (int) ve.getPosX() +", "+ (int) ve.getPosY()+", "+ (int) ve.getPosZ()+".");
			}
			BlockState b = ve.world.getBlockState(getBlockPos(ve).down());
			Block block = b.getBlock();
			if (	(block instanceof AirBlock)||
					(block instanceof TallGrassBlock)||
					(block instanceof FlowerBlock ||
					(block instanceof DoublePlantBlock))
					) {
				ve.world.setBlockState(getBlockPos(ve).down(), wallType);
				if (MyConfig.aDebugLevel > 1) {
					System.out.println("222: Wall Helper set.down() wallBlock ("+ wallType.getBlock().getRegistryName().toString() + ") at" + (int) ve.getPosX() +", "+ (int) ve.getPosY()+", "+ (int) ve.getPosZ()+".");
				}

			} else {
				ve.world.setBlockState(getBlockPos(ve), wallType);
				if (MyConfig.aDebugLevel > 1) {
					System.out.println("222: set Wall Helper wallBlock ("+ wallType.getBlock().getRegistryName().toString() + ") at" + (int) ve.getPosX() +", "+ (int) ve.getPosY()+", "+ (int) ve.getPosZ()+".");
				}

			}
			if (MyConfig.aDebugLevel > 1) {
				System.out.println("222: exit true set Wall Helper wallBlock ("+ wallType.getBlock().getRegistryName().toString() + ") at" + (int) ve.getPosX() +", "+ (int) ve.getPosY()+", "+ (int) ve.getPosZ()+".");
			}
			return true;
		}
		
	}

}
	

