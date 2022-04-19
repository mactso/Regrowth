package com.mactso.regrowth;

import com.mactso.regrowth.commands.RegrowthCommands;

// import com.mactso.regrowth.Commands.RegrowthCommands;
import com.mactso.regrowth.config.ModConfigs;

import net.fabricmc.api.ModInitializer;

public class Main implements ModInitializer {


    public static final String MOD_ID = "regrowth"; 
    
	@Override
	public void onInitialize() {

		ModConfigs.registerConfigs();

	}


}


