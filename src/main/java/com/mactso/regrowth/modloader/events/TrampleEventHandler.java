package com.mactso.regrowth.modloader.events;

import com.mactso.regrowth.actions.TrampleAction;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraftforge.event.level.BlockEvent.FarmlandTrampleEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber()
public class TrampleEventHandler {

	@SubscribeEvent
	public static boolean handleTrampleEvents(FarmlandTrampleEvent event) {
		MyUtilities.debugMsg(0, "enter Handle Trample Events");

		if (TrampleAction.doTrampleAction(event.getEntity())) {
			return true;
		}
		return false;

	}
}
