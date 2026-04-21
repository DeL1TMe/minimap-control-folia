package com.funniray.minimap.spigot;

import com.funniray.minimap.common.JavaMinimapPlugin;
import com.funniray.minimap.common.api.MinimapServer;
import com.funniray.minimap.spigot.impl.SpigotPlayer;
import com.funniray.minimap.spigot.impl.SpigotServer;
import com.funniray.minimap.spigot.impl.SpigotWorld;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpigotMain extends JavaMinimapPlugin implements PluginMessageListener, Listener {
    public SpigotMinimap plugin;

    public SpigotMain(SpigotMinimap plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerChannel(String channel) {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, channel);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, channel, this);
    }

    @Override
    public MinimapServer getServer() {
        return new SpigotServer();
    }

    @Override
    public ConfigurationLoader<CommentedConfigurationNode> getConfigLoader() {
        File defaultConfig = plugin.getDataFolder();

        if (!defaultConfig.exists()) defaultConfig.mkdirs();
        return YamlConfigurationLoader.builder()
                .defaultOptions(opts -> opts.shouldCopyDefaults(true))
                .path(defaultConfig.toPath().resolve("config.yml"))
                .build();
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                this.onPluginMessage(channel, new SpigotPlayer(player), message);
            }
        }, null);
    }

    private final Map<UUID, ScheduledTask> playerTaskMap = new HashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // The player join event is slightly too early. I unfortunately don't know an event that fires late enough for Xaeros to recognize the packet
        // If anyone knows, please let me know
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(plugin, task -> {
            if (player.isOnline()) {
                this.handlePlayerJoined(new SpigotPlayer(player));
            }
        }, null, 40L);
        ScheduledTask task = player.getScheduler().runAtFixedRate(this.plugin, scheduledTask -> {
            if (player.isOnline()) {
                this.handlePlayerJoinedRepeat(new SpigotPlayer(player));
            }
        }, null, 40L, 40L);
        this.playerTaskMap.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onLeft(PlayerQuitEvent event) {
        ScheduledTask task = this.playerTaskMap.get(event.getPlayer().getUniqueId());
        if (task != null) {
            task.cancel();
            this.playerTaskMap.remove(event.getPlayer().getUniqueId());
        }
        this.handlePlayerLeft(new SpigotPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                this.handleSwitchWorld(new SpigotWorld(player.getWorld()), new SpigotPlayer(player));
            }
        }, null);
    }
}
