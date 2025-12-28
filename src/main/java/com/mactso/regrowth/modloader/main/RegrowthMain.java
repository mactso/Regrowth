package com.mactso.regrowth.modloader.main;

import com.mactso.regrowth.commands.RegrowthCommands;
import com.mactso.regrowth.managers.ManagerInitializer;
import com.mactso.regrowth.modloader.config.MyConfig;
import com.mactso.regrowth.utilities.MyUtilities;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Main entry point for the Regrowth mod. Handles mod setup, config
 * registration, and event subscriptions.
 */

@Mod("regrowth")
public class RegrowthMain {

	public static final String MODID = "regrowth";

	public RegrowthMain() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
	}

	@Mod.EventBusSubscriber()
	public static class ForgeEvents {
		@SubscribeEvent
		public static void onCommandsRegistry(final RegisterCommandsEvent event) {
			MyUtilities.debugMsg(0, "Regrowth: Registering Commands");
			RegrowthCommands.register(event.getDispatcher());
		}

		@SubscribeEvent
		public static void onServerAboutToStart(final ServerAboutToStartEvent event) {
			MyUtilities.debugMsg(0, "Regrowth: Loading Configured Lists");
			ManagerInitializer.initializeManagers(event.getServer());

		}

	}

}
