package com.mactso.regrowth.modloader.config;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;

public class ModConfigProvider implements SimpleConfig.DefaultConfig {

    private String configContents = "";
    private final List<Pair<String,?>> configsList = new ArrayList<>();
    
    public List<Pair<String,?>> getConfigsList() {
        return configsList;
    }



    public void addKeyValuePair(Pair<String, ?> keyValuePair, String comment) {
        configsList.add(keyValuePair);
        configContents += keyValuePair.getFirst() + "=" + keyValuePair.getSecond() + " #"
                + comment + " | default: " + keyValuePair.getSecond() + "\n";
    }

    @Override
    public String get(String namespace) {
        return configContents;
    }
}