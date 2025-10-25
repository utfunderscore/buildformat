package org.readutf.buildformat;

import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.readutf.buildformat.commands.BuildCommand;
import org.readutf.buildformat.tools.ClickableManager;
import org.readutf.buildformat.tools.PositionTool;
import org.readutf.buildformat.tools.RegionSelectionTool;

public class BuildFormatPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        LiteBukkitFactory.builder(this).commands(new BuildCommand()).build();

        Bukkit.getPluginManager().registerEvents(new RegionSelectionTool(), this);
        Bukkit.getPluginManager().registerEvents(new PositionTool(), this);
        Bukkit.getPluginManager().registerEvents(new ClickableManager(), this);
    }
}
