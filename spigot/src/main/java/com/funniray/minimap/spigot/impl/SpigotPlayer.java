package com.funniray.minimap.spigot.impl;

import com.funniray.minimap.common.api.MinimapLocation;
import com.funniray.minimap.common.api.MinimapPlayer;
import com.funniray.minimap.common.version.Version;
import com.funniray.minimap.spigot.SpigotMinimap;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class SpigotPlayer implements MinimapPlayer {
    private final Player nativePlayer;

    public SpigotPlayer(Player player) {
        nativePlayer = player;
    }

    @Override
    public void sendPluginMessage(byte[] message, String channel) {
        sendPluginMessages(List.of(new PluginMessage(channel, message)));
    }

    @Override
    public void sendPluginMessages(List<PluginMessage> messages) {
        nativePlayer.getScheduler().run(SpigotMinimap.getInstance(), task -> {
            if (nativePlayer.isOnline()) {
                for (PluginMessage message : messages) {
                    nativePlayer.sendPluginMessage(SpigotMinimap.getInstance(), message.channel(), message.payload());
                }
            }
        }, null);
    }

    @Override
    public void sendMessage(Component message) {
        nativePlayer.sendMessage(message);
    }

    @Override
    public void teleport(MinimapLocation location) {
        nativePlayer.teleportAsync(((SpigotLocation) location).getNativeLocation());
    }

    @Override
    public MinimapLocation getLocation() {
        return new SpigotLocation(nativePlayer.getLocation());
    }

    @Override
    public void disconnect(Component reason) {
        nativePlayer.kick(reason);
    }

    @Override
    public UUID getUniqueId() {
        return nativePlayer.getUniqueId();
    }

    @Override
    public String getUsername() {
        return nativePlayer.getName();
    }

    @Override
    public boolean hasPermission(String string) {
        return nativePlayer.hasPermission(string);
    }

    @Override
    public boolean hasExplicitPermission(String string) {
        return nativePlayer.isPermissionSet(string) && nativePlayer.hasPermission(string);
    }

    @Override
    public Version getVersion() {
        SpigotMinimap plugin = SpigotMinimap.getInstance();
        if (plugin.viaHooked) {
            return plugin.viaHook.getPlayerVersion(this);
        } else {
            return new SpigotServer().getMinecraftVersion();
        }
    }

    public Player getNativePlayer() {
        return nativePlayer;
    }
}
