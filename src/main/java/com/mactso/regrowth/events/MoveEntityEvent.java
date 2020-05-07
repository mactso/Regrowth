package com.mactso.regrowth.events;

import java.util.function.DoubleToIntFunction;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;

import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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
    	World w = e.world;
    	if (e.world instanceof ClientWorld) {
    		return;
    	}
		Block b = w.getBlockState(e.getPosition()).getBlock();
		Block ground = w.getBlockState(e.getPosition()).getBlock();


		if ((b instanceof AirBlock) ||
        	(b instanceof TallGrassBlock) ||
        	(b instanceof DoublePlantBlock))
        {
            b = e.world.getBlockState(e.getPosition().down()).getBlock();		            	
        } 
        if (b == Blocks.GRASS_BLOCK)
        {
        	
        	EntityType t = e.getType();
        	ResourceLocation registryName = t.getRegistryName();
        	String key = registryName.toString();
    		RegrowthEntitiesManager.RegrowthMobItem r = RegrowthEntitiesManager.getRegrowthMobInfo(key);
    		if (r==null) {  // This mob isn't configured
    			return;
    		}
    		String eatgrowbothType = r.getRegrowthType();
    		double regrowthEventPercentage = r.getRegrowthEventPercentage();
    		double nr = e.world.rand.nextDouble(); 
    		if ((nr * 10000.0)  < regrowthEventPercentage) {
        		System.out.println("grow at " +  e.getPosX() +", "+e.getPosY()+", "+e.getPosZ()+", ");
    			
    		// if has grass
    			
	            	IGrowable ib = (IGrowable) b;
//	            	ItemStack iStk = new ItemStack(Items.BONE_MEAL,1);
//  	          	BoneMealItem.applyBonemeal(iStk, e.world,e.getPosition());
	            	if (e.world instanceof ServerWorld) {
/* 15.1,15.2 */		    ib.grow((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));
//(likely 12.2 and 14.4 call?)	ib.func_225535_a_((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));

	            	}
    		}
    			
        }
        
    	
        int z = 0;
	}
}
	

