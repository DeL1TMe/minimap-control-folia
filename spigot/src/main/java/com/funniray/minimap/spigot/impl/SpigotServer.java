package com.funniray.minimap.spigot.impl;

import com.funniray.minimap.common.api.MinimapPlayer;
import com.funniray.minimap.common.api.MinimapServer;
import com.funniray.minimap.common.api.MinimapWorld;
import com.funniray.minimap.common.version.Version;
import com.funniray.minimap.spigot.SpigotMinimap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

public class SpigotServer implements MinimapServer {
    @Override
    public Version getMinecraftVersion() {
        String[] ver = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        try {
            int major = parseInt(ver[0]);
            int minor = ver.length > 1 ? parseInt(ver[1]) : 0;
            int patch = ver.length > 2 ? parseInt(ver[2]) : 0;
            return new Version(major, minor, patch);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot parse version " + Bukkit.getBukkitVersion());
        }
    }

    @Override
    public String getLoaderVersion() {
        return Bukkit.getVersion();
    }

    @Override
    public String getLoaderName() {
        return Bukkit.getName();
    }

    @Override
    public void forEachPlayer(Consumer<MinimapPlayer> action) {
        SpigotMinimap plugin = SpigotMinimap.getInstance();
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().run(plugin, playerTask -> {
                    if (player.isOnline()) {
                        action.accept(new SpigotPlayer(player));
                    }
                }, null);
            }
        });
    }

    @Override
    public List<MinimapWorld> getWorlds() {
        return Bukkit.getWorlds().stream()
                .map(SpigotWorld::new)
                .collect(Collectors.toList());
    }
}
