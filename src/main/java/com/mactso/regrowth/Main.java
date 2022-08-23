package com.mactso.regrowth;

import com.mactso.regrowth.commands.RegrowthCommands;

//import com.mactso.regrowth.commands.RegrowthCommands;

// import com.mactso.regrowth.Commands.RegrowthCommands;
import com.mactso.regrowth.config.MyConfig;

import net.fabricmc.api.ModInitializer;
// import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class Main implements ModInitializer {


    public static final String MOD_ID = "regrowth"; 
    
	@Override
	
//	 public void onInitialize() {
//	    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("foo")
//	        .executes(context -> {
//	      // For versions below 1.19, replace "Text.literal" with "new LiteralText".
//	      context.getSource().sendMessage(Text.literal("Called /foo with no arguments"));
//	 
//	      return 1;
//	    })));
//	  }

	public void onInitialize() {
		registerEvents();
		MyConfig.registerConfigs();

	}
	
	private void registerEvents() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			RegrowthCommands.register(dispatcher);
		});
	}


}


