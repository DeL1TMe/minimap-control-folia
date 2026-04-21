package com.funniray.minimap.common.api;

import com.funniray.minimap.common.version.Version;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.UUID;

public interface MinimapPlayer {
    record PluginMessage(String channel, byte[] payload) {
    }

    void sendPluginMessage(byte[] message, String channel);

    default void sendPluginMessages(List<PluginMessage> messages) {
        for (PluginMessage message : messages) {
            sendPluginMessage(message.payload(), message.channel());
        }
    }

    void sendMessage(Component message);
    void teleport(MinimapLocation location);
    MinimapLocation getLocation();
    void disconnect(Component reason);

    UUID getUniqueId();
    String getUsername();
    boolean hasPermission(String string);

    default boolean hasExplicitPermission(String string) {
        return hasPermission(string);
    }

    Version getVersion();
}
