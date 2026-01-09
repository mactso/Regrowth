package com.mactso.regrowth.modloader.main;

import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.modloader.events.HandleNeoForgeEntityMoveEvent;
import com.mactso.regrowth.modloader.events.NeoForgeModEvents;
import com.mactso.regrowth.modloader.events.TrampleEventHandler;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Main entry point for the Regrowth mod. Handles mod setup, config
 * registration, and event subscriptions.
 */

@Mod("regrowth")
public class RegrowthMain {

	public static final String MODID = "regrowth";

	public RegrowthMain(IEventBus modEventBus, ModContainer modContainer) {

	    // NeoForge.EVENT_BUS.register(this);
	    
        HandleNeoForgeEntityMoveEvent.register();
        TrampleEventHandler.register();
        NeoForge.EVENT_BUS.register(new NeoForgeModEvents());
        
		modContainer.registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
		
	}

}
