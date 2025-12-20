package com.mactso.regrowth;

import java.nio.file.Path;

import com.mactso.regrowth.commands.RegrowthCommands;
import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import com.mactso.regrowth.config.WallBiomeDataManager;
import com.mactso.regrowth.config.WallFoundationManager;
import com.mactso.regrowth.managers.SaplingManager;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("regrowth")
public class Main {

	public static final String MODID = "regrowth";

	public Main(FMLJavaModLoadingContext context) {
		// context.getModEventBus().register(this);
		context.registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);

	}

	@Mod.EventBusSubscriber()
	public static class ForgeEvents {
		@SubscribeEvent
		public static void onCommandsRegistry(final RegisterCommandsEvent event) {
			System.out.println("Regrowth: Registering Command Dispatcher");
			RegrowthCommands.register(event.getDispatcher());
		}

		@SubscribeEvent
		public static void onServerAboutToStart(final ServerAboutToStartEvent event) {
			System.out.println("Regrowth: Initializing SaplingManager");

	        RegrowthEntitiesManager.regrowthMobInit();
			
			WallFoundationManager.init();
			;
			WallBiomeDataManager.wallBiomeDataInit(event.getServer());

			// Forge config directory
			Path configDir = FMLPaths.CONFIGDIR.get();

			// Initialize SaplingManager (generates files + reads CSV)
			SaplingManager.init(event.getServer(), configDir);
		}

	}

}
