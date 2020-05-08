package com.mactso.regrowth.events;

import java.util.function.DoubleToIntFunction;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;

import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.impl.SetBlockCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber()
public class MoveEntityEvent {
	

	@SubscribeEvent
    public void EntityMove(LivingUpdateEvent event) { 


	    // need to find the type of entity here.
		if (event.getEntity() == null) {
			return;
		}
		
		Entity e = event.getEntity();
		
		if (e.world instanceof ClientWorld) {
			return;
		}
	
		Block footBlock = e.world.getBlockState(e.getPosition()).getBlock();
		Block groundBlock = e.world.getBlockState(e.getPosition().down()).getBlock();
	
		if ((footBlock != Blocks.GRASS_BLOCK) && (groundBlock != Blocks.GRASS_BLOCK)) {
			return;
		}
	
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
				 (regrowthType.equals("both")) ||  	
			     (regrowthType.equals("cut"))
			    )
			{
				if ((nr*1000.0) < regrowthEventPercentage) {
//					e.world.setBlockState(e.getPosition(), Blocks.AIR.getDefaultState(), 3);
					e.world.destroyBlock(e.getPosition(), false);
					if (MyConfig.aDebugLevel > 0) {
						System.out.println(key + " eat at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
					}
					if (regrowthType.equals("cut")) {
		    			return;
		    		}
		    		LivingEntity le = (LivingEntity) e;
		    		if (le.getMaxHealth() > le.getHealth()) {
		    			return;
		    		}
		    		EffectInstance ei = new EffectInstance (Effects.INSTANT_HEALTH,1,0, false, true );
		    		le.addPotionEffect(ei);
		    		if (e instanceof CowEntity) {
		    			CowEntity ce = (CowEntity) e;
		    			if (ce.isChild()) {
		    				ce.setGrowingAge (ce.getGrowingAge() + 30);
		    			}
		    		}
		    		if (e instanceof SheepEntity) {
		    			SheepEntity se = (SheepEntity) e;
		    			if (se.isChild()) {
		    				se.setGrowingAge (se.getGrowingAge() + 30);
		    			}
		    		}
		    		if (e instanceof HorseEntity) {
		    			HorseEntity se = (HorseEntity) e;
		    			if (se.isChild()) {
		    				se.setGrowingAge (se.getGrowingAge() + 30);
		    			}
		    		}
		    		if (e instanceof DonkeyEntity) {
		    			DonkeyEntity se = (DonkeyEntity) e;
		    			if (se.isChild()) {
		    				se.setGrowingAge (se.getGrowingAge() + 30);
		    			}
		    		}		    		
				}

	    		return;			
			}
				
	    }

		int debugBreakPoint = 0;

	}
	
//	ItemStack iStk = new ItemStack(Items.BONE_MEAL,1);
//	BoneMealItem.applyBonemeal(iStk, e.world,e.getPosition());
//(likely 12.2 and 14.4 call?)	ib.func_225535_a_((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));


}
	

