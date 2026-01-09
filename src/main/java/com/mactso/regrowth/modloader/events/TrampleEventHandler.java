package com.mactso.regrowth.modloader.events;

import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent;


public class TrampleEventHandler {

	
    public static void register() {
        NeoForge.EVENT_BUS.register(new TrampleEventHandler());
    }
	
	@SubscribeEvent
	public void handleTrampleEvents(FarmlandTrampleEvent event) {
		MyUtilities.debugMsg(0, "enter Handle Trample Events");

		if (event.getEntity() instanceof LivingEntity le) {
			MyUtilities.debugMsg(1, le, "FarmlandTrampleEvent");

			if (le instanceof Villager ve) {
				if (ve.getVillagerData().getProfession() != VillagerProfession.FARMER) {
					return;
				}
				if (ve.getVillagerData().getLevel() >= 3) {
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
		MyUtilities.debugMsg(1, "fall out of Handle Trample Events");

	}

}
