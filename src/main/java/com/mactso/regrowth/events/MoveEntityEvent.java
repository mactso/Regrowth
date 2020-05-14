package com.mactso.regrowth.events;

import java.util.Optional;
import java.util.function.DoubleToIntFunction;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;

import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.GrassPathBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.impl.SetBlockCommand;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.merchant.villager.VillagerData;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.horse.DonkeyEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.VillagePieces.Village;
import net.minecraft.world.gen.feature.structure.VillageStructure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber()
public class MoveEntityEvent {
	
	static int[]dx= {1,0,-1,0};
	static int[]dz= {0,1,0,-1};
	
	@SubscribeEvent
    public void EntityMove(LivingUpdateEvent event) { 

	    // need to find the type of entity here.
		if (event.getEntity() == null) {
			return;
		}
		
		Entity e = event.getEntity();
		
		if (!(e.world instanceof ServerWorld)) {
			return;
		}
	
		Block footBlock = e.world.getBlockState(e.getPosition()).getBlock();
		Block groundBlock = e.world.getBlockState(e.getPosition().down()).getBlock();

		EntityType<?> t = e.getType();
		ResourceLocation registryName = t.getRegistryName();
		String key = registryName.toString();
		RegrowthEntitiesManager.RegrowthMobItem r = RegrowthEntitiesManager.getRegrowthMobInfo(key);
		if (r==null) {  // This mob isn't configured
			return;
		}
		
		String regrowthType = r.getRegrowthType();
	
		// exit quickly if an impossible case.

		if ((regrowthType.equals("eat")) && (footBlock instanceof AirBlock)) {
			return;
		}
		if ((regrowthType.equals("grow")) && (footBlock instanceof TallGrassBlock)) {
			return;
		}
		if ((regrowthType.equals("grow")) && (footBlock instanceof FlowerBlock)) {
			return;
		}    		
		if ((regrowthType.equals("tall")) && (!(footBlock instanceof TallGrassBlock))) {
			return;
		}
		
		double regrowthEventPercentage = r.getRegrowthEventPercentage();
		double nr = e.world.rand.nextDouble(); 

		if (e instanceof VillagerEntity) {
			if ((nr*10.0) > regrowthEventPercentage) {
				return;
			}
			VillagerEntity ve = (VillagerEntity) e;		
			VillagerData vd = ve.getVillagerData();
			Brain<VillagerEntity> vb = ve.getBrain();
			Optional<GlobalPos> vMeetingPlace = vb.getMemory(MemoryModuleType.MEETING_POINT);
			Optional<GlobalPos> vHome = vb.getMemory(MemoryModuleType.HOME);
			
			//default = repair farms
			boolean fixFarm = false;
			if (groundBlock instanceof GrassBlock) {
				for(int i=0; i<4; i++) {
					Block tempBlock = e.world.getBlockState(new BlockPos (ve.getPosX()+dx[i],ve.getPosY()-1,ve.getPosZ()+dz[i])).getBlock();
					if (tempBlock == Blocks.FARMLAND) {
						fixFarm = true;
					}
				}
			}
			if (fixFarm) {
				e.world.setBlockState(ve.getPosition().down(), Blocks.FARMLAND.getDefaultState());
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " fix farm at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
				}	
			}
			//cut grass
			if(regrowthType.contains("c")) {
				if (footBlock instanceof TallGrassBlock) {
					e.world.destroyBlock(e.getPosition(), false);
					if (MyConfig.aDebugLevel > 0) {
						System.out.println(key + " cut at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
					}					
				}
			}
			// improve roads
			if(regrowthType.contains("r")) {
				
				BlockPos vePos = ve.getPosition();
				int veX =  vePos.getX();
				int veZ =  vePos.getZ();
				int veY =  vePos.getY();

				// fix potholes
				int grassPathCount = 0;
				if (groundBlock instanceof GrassBlock) {
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
			if (regrowthType.contains("w") && (groundBlock instanceof GrassBlock) && (footBlock instanceof AirBlock)) {
				if (vMeetingPlace.isPresent()) {
					GlobalPos vhp = vMeetingPlace.get();
					BlockPos vhPos = vhp.getPos();
					int absvx = (int) Math.abs(ve.getPosX() - vhPos.getX());
					int absvz = (int) Math.abs(ve.getPosZ() - vhPos.getZ());
					if ((absvx == 31) || (absvz == 31)) {
						if ((absvx<=31)&&(absvz<=31)) {
							e.world.setBlockState(ve.getPosition(), Blocks.BRICK_WALL.getDefaultState());
							if (regrowthType.contains("t")) {
								boolean validTorchBlock = false;
								if (absvz == 31) {
									if ((absvx % 8) == 7) validTorchBlock = true;
								}
								else {
									if ((absvz % 8) == 7) validTorchBlock = true;
								}
								// handle center case (0,31 ; 31,0)	 ; 
								if (validTorchBlock) {
									e.world.setBlockState(ve.getPosition().up(), Blocks.TORCH.getDefaultState());
									if (MyConfig.aDebugLevel > 0) {
										System.out.println(key + " torch at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
									}	
								}
								if (MyConfig.aDebugLevel > 0) {
									System.out.println(key + " wall at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
								}	
							}
							
						}
					}
				}			
			}

			if (regrowthType.contains("p") && (groundBlock instanceof GrassBlock) && (footBlock instanceof AirBlock)) {
				double vMeetingPlaceDistance = 0.0;
				int absVMpX = 0;
				int absVMpZ = 0;
				boolean buildPallisade = true;
			
				if (vMeetingPlace.isPresent()) {
					GlobalPos tMpPos = vMeetingPlace.get();
					BlockPos vMeetingPlaceBlockPos = tMpPos.getPos();
					absVMpX = (int) Math.abs(ve.getPosX() - vMeetingPlaceBlockPos.getX());
					if ( absVMpX < 32 ) buildPallisade = false;
					absVMpZ = (int) Math.abs(ve.getPosZ() - vMeetingPlaceBlockPos.getZ());
					if (absVMpZ < 32 ) buildPallisade = false;
				}
				
				// don't build pallisades until the village assigns a meeting place.
				// don't build pallisades inside the village wall perimeter.

				if ((vHome.isPresent() && (buildPallisade))) {
					GlobalPos vhp = vHome.get();
					BlockPos vHomePos = vhp.getPos();
					int absvx = (int) Math.abs(ve.getPosX() - vHomePos.getX());
					int absvz = (int) Math.abs(ve.getPosZ() - vHomePos.getZ());
					if ((absvx == 31) || (absvz == 31)) {
						e.world.setBlockState(ve.getPosition(), Blocks.COBBLESTONE_WALL.getDefaultState());
						if (MyConfig.aDebugLevel > 0) {
							System.out.println(key + " pallisade at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
						}	
						boolean validTorchBlock = false;
						if ((absvx % 11) == 0) validTorchBlock = true;
						if ((absvz % 11) == 0) validTorchBlock = true;
						if (validTorchBlock) {
							e.world.setBlockState(ve.getPosition().up(), Blocks.TORCH.getDefaultState());
							if (MyConfig.aDebugLevel > 0) {
								System.out.println(key + " torch at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
							}	
						}
					}
				}			
			}

			if(regrowthType.contains("l")) {
				// upgrade lighting
			}
		}
		
		if ((footBlock != Blocks.GRASS_BLOCK) && (groundBlock != Blocks.GRASS_BLOCK)) {
			return;
		}
		
		if ((regrowthType.equals("tall")) && (footBlock instanceof TallGrassBlock )) {
			if ((nr*10000.0) < regrowthEventPercentage) {
	        	IGrowable ib = (IGrowable) footBlock;
	    		ib.grow((ServerWorld)e.world, e.world.rand, e.getPosition(), e.world.getBlockState(e.getPosition()));    			
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " tall at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");

				}
			}
			return;
		}
		if ((regrowthType.equals("grow")) || (regrowthType.equals("both")) ) {
			if (footBlock instanceof AirBlock ) {
			if ((nr*15000.0) < regrowthEventPercentage) {
	        	IGrowable ib = (IGrowable) groundBlock;
	    		ib.grow((ServerWorld)e.world, e.world.rand, e.getPosition(), e.world.getBlockState(e.getPosition()));    			
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " grow at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
				}
				return;
			}	
		}
	
		boolean grassOrFlower = false;
		if (footBlock instanceof GrassBlock) 
		{
			grassOrFlower = true;
		}
		if (footBlock instanceof TallGrassBlock) 
		{
			grassOrFlower = true;
		}
	
		if (grassOrFlower) {
			if ( (regrowthType.equals("eat"))  || 
				 (regrowthType.equals("both")) 
			    )
			{
				if ((nr*10000.0) < regrowthEventPercentage) {
//					e.world.setBlockState(e.getPosition(), Blocks.AIR.getDefaultState(), 3);
					e.world.destroyBlock(e.getPosition(), false);
					if (MyConfig.aDebugLevel > 0) {
						System.out.println(key + " eat at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
					}
		    		LivingEntity le = (LivingEntity) e;
		    		if (e instanceof AgeableEntity) {
		    			AgeableEntity ae = (AgeableEntity) e;
		    			if (ae.isChild()) {
		    				ae.setGrowingAge(ae.getGrowingAge() + 30);
		    			}
		    		}
		    		if (le.getMaxHealth() > le.getHealth() && (MyConfig.aEatingHeals == 1)) {
			    		EffectInstance ei = new EffectInstance (Effects.INSTANT_HEALTH,1,0, false, true );
			    		le.addPotionEffect(ei);
		    		}

	    		}

	    		return;			
			}
				
	    }
		}

		int debugBreakPoint = 0;

	}
	
//	ItemStack iStk = new ItemStack(Items.BONE_MEAL,1);
//	BoneMealItem.applyBonemeal(iStk, e.world,e.getPosition());
//(likely 12.2 and 14.4 call?)	ib.func_225535_a_((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));


}
	

