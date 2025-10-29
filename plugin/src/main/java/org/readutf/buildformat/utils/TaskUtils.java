package org.readutf.buildformat.utils;

import org.bukkit.Bukkit;
import org.readutf.buildformat.BuildFormatPlugin;

public class TaskUtils {

    public static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(BuildFormatPlugin.getProvidingPlugin(BuildFormatPlugin.class), runnable);
    }

}

