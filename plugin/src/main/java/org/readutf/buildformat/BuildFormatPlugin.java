package org.readutf.buildformat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.readutf.buildformat.tools.RegionSelectionTool;

public class BuildFormatPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        Bukkit.getPluginManager().registerEvents(new RegionSelectionTool(), this);


    }
}
