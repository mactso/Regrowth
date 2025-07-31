package com.mactso.regrowth.events;

import com.mactso.regrowth.utility.Utility;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.event.level.BlockEvent.FarmlandTrampleEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber()
public class TrampleEventHandler {

	@SubscribeEvent
	public static void handleTrampleEvents(FarmlandTrampleEvent event) {
		Utility.debugMsg(0,"enter Handle Trample Events");		

		if (event.getEntity() instanceof LivingEntity le) {
			Utility.debugMsg(1, le, "FarmlandTrampleEvent");
			if (event.isCancelable()) {
				if (le instanceof Villager ve) {
					if (ve.getVillagerData().profession() != VillagerProfession.FARMER) {
						return;
					}
					if (ve.getVillagerData().level() >= 3) {
						event.setCanceled(true);
						return;
					}
				}
				if ((le instanceof ServerPlayer spe)) {
					if (!spe.isCreative()) {
						return;
					}
					event.setCanceled(true);
				}
			}
		}
		Utility.debugMsg(1,"fall out of Handle Trample Events");		

	}
	
}
