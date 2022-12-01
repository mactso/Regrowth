package com.mactso.regrowth;

import com.mactso.regrowth.commands.RegrowthCommands;

//import com.mactso.regrowth.commands.RegrowthCommands;

// import com.mactso.regrowth.Commands.RegrowthCommands;
import com.mactso.regrowth.config.MyConfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

public class Main implements ModInitializer {


    public static final String MOD_ID = "regrowth"; 
    
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(RegrowthCommands::register);
		MyConfig.registerConfigs();

	}


}


