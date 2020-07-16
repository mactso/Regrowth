// 15.2 - 1.0.0.0 regrowth
package com.mactso.regrowth;

import com.mactso.regrowth.Commands.RegrowthCommands;
// import com.mactso.regrowth.Commands.RegrowthCommands;
import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.events.MoveEntityEvent;


import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@Mod("regrowth")
public class Main {

	    public static final String MODID = "regrowth"; 
	    
	    public Main()
	    {

			FMLJavaModLoadingContext.get().getModEventBus().register(this);
	        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,MyConfig.SERVER_SPEC );
			MinecraftForge.EVENT_BUS.register(this);
			
	    }

	    // Register ourselves for server and other game events we are interested in
		@SubscribeEvent 
		public void preInit (final FMLCommonSetupEvent event) {
			System.out.println("Regrowth: Registering Handler");
			MinecraftForge.EVENT_BUS.register(new MoveEntityEvent ());
			
		}
		@SubscribeEvent 		
		public void onCommandsRegistry(final RegisterCommandsEvent event) {
			RegrowthCommands.register(event.getDispatcher());			
		}

		// in 14.4 and later, config file loads when the server starts when the world starts.
		@SubscribeEvent 
		public void onServerStarting (FMLServerStartingEvent event) {

		}
}


