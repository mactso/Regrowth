package com.mactso.regrowth.events;

import com.mactso.regrowth.utility.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class TrampleEventHandler {


	public static boolean handleTrampleEvent(Entity entity) {

		if (entity instanceof LivingEntity le) {
			BlockPos lePos = entity.blockPosition();
			Utility.debugMsg(1, lePos, "Enter handleTrampleEvent");	
			
			if (isCreativePlayer(le, lePos)) {
				return true;
			}
			
			if (isL3Farmer(le, lePos)) {
				return true;
			}
			
		}

		return false;

	}

	private static boolean isL3Farmer (LivingEntity le, BlockPos pos) {
		if (le  instanceof Villager ve) {
			ResourceKey<VillagerProfession> vp = ve.getVillagerData().profession().unwrapKey().get();
			if ((vp == VillagerProfession.FARMER) && (ve.getVillagerData().level() >= 3)) {
				Utility.debugMsg(2, pos, "Level 3 Farmer, Trample Cancelled");
				return true;
			}
		}
		return false;
	}
	
	
	private static boolean isCreativePlayer (LivingEntity le, BlockPos pos) {
		
		if ((le instanceof ServerPlayer spe)) {
			if (spe.isCreative()) {
				Utility.debugMsg(2, pos, "Creative Player, Trample Cancelled");
				return true;
			}
		}
		return false;
		
	}
	
}
