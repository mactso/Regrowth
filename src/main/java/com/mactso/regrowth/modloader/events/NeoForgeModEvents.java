package com.mactso.regrowth.modloader.events;

import com.mactso.regrowth.commands.RegrowthCommands;
import com.mactso.regrowth.utilities.MyUtilities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

public class NeoForgeModEvents {

    @SubscribeEvent
    public void onCommandsRegistry(RegisterCommandsEvent event) {
        MyUtilities.debugMsg(0, "Regrowth: Registering Commands");
        RegrowthCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        MyUtilities.debugMsg(0, "Regrowth: Loading Configured Lists");
        // Load your managers here
        // ManagerInitializer.initializeManagers(event.getServer());
    }
}