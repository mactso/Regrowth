package com.mactso.regrowth.modloader.events;

import com.mactso.regrowth.actions.TrampleAction;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraftforge.event.level.BlockEvent.FarmlandTrampleEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber()
public class TrampleEventHandler {

	@SubscribeEvent
	public static void handleTrampleEvents(FarmlandTrampleEvent event) {
		MyUtilities.debugMsg(0, "enter Handle Trample Events");

		if (!TrampleAction.doTrampleAction(event.getEntity())) {
			return;
		}
		event.setCanceled(true);

	}
}
