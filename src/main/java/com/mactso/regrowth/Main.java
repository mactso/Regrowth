package com.mactso.regrowth;

import com.mactso.regrowth.commands.RegrowthCommands;
// import com.mactso.regrowth.Commands.RegrowthCommands;
import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.utility.Utility;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("regrowth")
public class Main {

	    public static final String MODID = "regrowth"; 
	    
	    public Main(FMLJavaModLoadingContext context)
	    {
			context.registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
	        Utility.debugMsg(0,MODID + ": Registering Mod.");
	    }
	    


	    @Mod.EventBusSubscriber()
	    public static class ForgeEvents
	    {
			@SubscribeEvent 		
			public static void onCommandsRegistry(final RegisterCommandsEvent event) {
				System.out.println("Regrowth: Registering Command Dispatcher");
				RegrowthCommands.register(event.getDispatcher());			
			}

			}

}


