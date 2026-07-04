// This is free and unencumbered software released into the public domain.
package net.trueog.havenog;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public final class HavenOG extends JavaPlugin {

    public static final String HAVEN_FLAG_NAME = "haven";

    private StateFlag havenFlag;
    private boolean flagReady;

    @Override
    public void onLoad() {

        final FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        final Flag<?> existing = registry.get(HAVEN_FLAG_NAME);
        if (existing instanceof StateFlag stateFlag) {

            havenFlag = stateFlag;
            flagReady = true;

            return;

        }

        if (existing != null) {

            getLogger().severe("Flag '" + HAVEN_FLAG_NAME + "' already exists with a different type.");

            return;

        }

        try {

            havenFlag = new StateFlag(HAVEN_FLAG_NAME, false);
            registry.register(havenFlag);
            flagReady = true;

        } catch (FlagConflictException flagConflictException) {

            getLogger()
                    .severe("Could not register flag '" + HAVEN_FLAG_NAME + "': " + flagConflictException.getMessage());

        }

    }

    @Override
    public void onEnable() {

        if (!flagReady) {

            Bukkit.getPluginManager().disablePlugin(this);
            return;

        }

        saveDefaultConfig();

        final RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();

        final PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new HavenListener(this, query, havenFlag), this);

    }

}