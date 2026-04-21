package com.funniray.minimap.common.jm;

import com.funniray.minimap.common.JavaMinimapPlugin;
import com.funniray.minimap.common.MinimapConfig;
import com.funniray.minimap.common.api.MessageHandler;
import com.funniray.minimap.common.api.MinimapPlayer;
import com.funniray.minimap.common.api.MinimapWorld;
import com.funniray.minimap.common.jm.data.JMConfig;
import com.funniray.minimap.common.jm.data.JMVersion;
import com.funniray.minimap.common.jm.data.JMWorldConfig;
import com.funniray.minimap.common.jm.data.ServerPropType;
import com.funniray.minimap.common.network.NetworkUtils;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Optional;

public class JMHandler implements MessageHandler {
    private final JavaMinimapPlugin plugin;
    private static final byte PACKET_MARKER = 42;
    private static final String VERSION_CHANNEL = "journeymap:version";
    private static final String PERMISSIONS_CHANNEL = "journeymap:perm_req";
    private static final String ADMIN_REQUEST_CHANNEL = "journeymap:admin_req";
    private static final String ADMIN_SAVE_CHANNEL = "journeymap:admin_save";
    private static final String TELEPORT_CHANNEL = "journeymap:teleport_req";
    private static final String MULTIPLAYER_OPTIONS_CHANNEL = "journeymap:mp_options_req";

    public JMHandler(JavaMinimapPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendInitialState(MinimapPlayer player) {
        sendHandshake(player);
        sendPermissions(player);
    }

    public void sendHandshake(MinimapPlayer player) {
        String payload = new Gson().toJson(new JMVersion());
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        NetworkUtils.writeUtf(payload, out);
        player.sendPluginMessage(out.toByteArray(), VERSION_CHANNEL);
    }

    private Optional<MinimapWorld> getWorldFromKeyedName(String keyedWorld) {
        return JavaMinimapPlugin.getInstance().getServer().getWorlds().stream().filter(w->w.getKeyedName().equals(keyedWorld)).findFirst();
    }

    public void sendPermissions(MinimapPlayer player) {
        handlePerm(player, PERMISSIONS_CHANNEL);
    }

    public void handleMPOptions(MinimapPlayer player, byte[] message) {
        if (message.length > 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Saving JourneyMap multiplayer options is not implemented."));
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(PACKET_MARKER);
        NetworkUtils.writeUtf(new Gson().toJson(new MultiplayerOptionsConfig()), out);
        player.sendPluginMessage(out.toByteArray(), MULTIPLAYER_OPTIONS_CHANNEL);
    }

    public void handleTeleport(MinimapPlayer player, byte[] message) {
        if (!player.hasPermission("minimap.jm.teleport")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to teleport."));
            return;
        }
        String teleport = getEffectiveConfig(player).teleportEnabled;
        if (teleport.equalsIgnoreCase("none") || (teleport.equalsIgnoreCase("ops") && !player.hasPermission("minimap.jm.admin"))) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Teleport packet was sent, but teleporting isn't enabled."));
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        double x = in.readDouble();
        double y = in.readDouble();
        double z = in.readDouble();
        String dim = NetworkUtils.readUtf(in);

        Optional<MinimapWorld> world = getWorldFromKeyedName(dim);

        if (!world.isPresent()){
            player.teleport(player.getLocation().getWorld().getLocation(x,y,z));
            return;
        }

        player.teleport(world.get().getLocation(x,y,z));
    }

    public void handleAdminReq(MinimapPlayer player, byte[] message) {
        if (!canViewServerProperties(player)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to view JourneyMap server options."));
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        in.readByte();
        ServerPropType type = ServerPropType.getFromType(in.readInt());
        String dimension = NetworkUtils.readUtf(in);
        NetworkUtils.readUtf(in);
        sendAdminData(player, type, dimension);
    }

    public void handleAdminSave(MinimapPlayer player, byte[] message) {
        if (!player.hasPermission("minimap.jm.admin")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        in.readByte();
        int type = in.readInt();
        String dimension = NetworkUtils.readUtf(in);
        String payload = NetworkUtils.readUtf(in);

        Gson gson = new Gson();
        if (type == 1) {
            JMConfig newConfig = gson.fromJson(payload, JMConfig.class);
            plugin.getConfig().globalJourneymapConfig = newConfig;
        } else if (type == 2 || type == 3) {
            JMWorldConfig newConfig = gson.fromJson(payload, JMWorldConfig.class);
            if (type == 3) {
                MinimapConfig.WorldConfig worldConfig = getWorldConfig(dimension);
                worldConfig.journeymapConfig = newConfig;
            } else {
                plugin.getConfig().defaultWorldConfig = newConfig;
            }
        }
        plugin.saveConfig();
        plugin.getServer().forEachPlayer(this::sendPermissions);
    }

    private void sendAdminData(MinimapPlayer player, ServerPropType type, String dimension) {
        Gson gson = new Gson();
        String payload;
        if (type == ServerPropType.GLOBAL) {
            payload = gson.toJson(plugin.getConfig().globalJourneymapConfig);
            dimension = "";
        } else if (type == ServerPropType.DEFAULT) {
            payload = gson.toJson(plugin.getConfig().defaultWorldConfig);
            dimension = "";
        } else {
            payload = gson.toJson(getWorldConfig(dimension).journeymapConfig);
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(PACKET_MARKER);
        out.writeInt(type.getId());
        NetworkUtils.writeUtf(dimension, out);
        NetworkUtils.writeUtf(payload, out);
        player.sendPluginMessage(out.toByteArray(), ADMIN_REQUEST_CHANNEL);
    }

    public void handleVersion(MinimapPlayer player, byte[] message) {
        Gson gson = new Gson();
        JMVersion serverVersion = new JMVersion();
        ByteArrayDataInput in = ByteStreams.newDataInput(message);

        String sent = NetworkUtils.readUtf(in);
        JMVersion clientVersion = gson.fromJson(sent, JMVersion.class);
        if (clientVersion.journeymap_version.major <= 5) {
            serverVersion.journeymap_version = new JMVersion.VersionDetails(6,0,0,null);
        }

        String payload = gson.toJson(serverVersion);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        NetworkUtils.writeUtf(payload, out);
        player.sendPluginMessage(out.toByteArray(), VERSION_CHANNEL);
        sendPermissions(player);
    }

    public JMConfig getEffectiveConfig(MinimapPlayer player) {
        JMWorldConfig worldConfig = plugin.getConfig().getWorldConfig(player.getLocation().getWorld().getName()).journeymapConfig;
        JMConfig config = plugin.getConfig().globalJourneymapConfig;
        if (worldConfig != null) {
            return worldConfig.applyToConfig(config);
        }
        return config;
    }

    public void handlePerm(MinimapPlayer player, String replyChannel) {
        JMConfig config = getEffectiveConfig(player).normalizeForClient();

        Gson gson = new Gson();
        String payload = gson.toJson(config);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(PACKET_MARKER);
        out.writeBoolean(player.hasPermission("minimap.jm.admin"));
        NetworkUtils.writeUtf(payload, out);
        out.writeBoolean(true);
        player.sendPluginMessage(out.toByteArray(), replyChannel);
    }

    private MinimapConfig.WorldConfig getWorldConfig(String dimension) {
        String worldName = getWorldFromKeyedName(dimension)
                .map(MinimapWorld::getName)
                .orElse(dimension);
        return plugin.getConfig().getWorldConfig(worldName);
    }

    private boolean canViewServerProperties(MinimapPlayer player) {
        return player.hasPermission("minimap.jm.admin")
                || Boolean.parseBoolean(plugin.getConfig().globalJourneymapConfig.viewOnlyServerProperties);
    }

    public void playerLeft(MinimapPlayer player) {
    }

    @Override
    public void onPluginMessage(String channel, MinimapPlayer player, byte[] message) {
        switch (channel.split(":")[1]) {
            case "version":
                handleVersion(player, message);
                break;
            case "perm_req":
                handlePerm(player, channel);
                break;
            case "admin_req":
                handleAdminReq(player, message);
                break;
            case "admin_save":
                handleAdminSave(player, message);
                break;
            case "teleport_req":
                handleTeleport(player, message);
                break;
            case "mp_options_req":
                handleMPOptions(player, message);
                break;
            case "common":
                break;
        }
    }

    private static class MultiplayerOptionsConfig {
        public String loadedChunksEntity = "false";
        public String loadedChunksBlock = "false";
        public String loadedChunksFull = "false";
        public String loadedChunksInaccessible = "false";
        public String visible = "true";
        public String hideSelfUnderground = "false";
        public String configVersion = new JMVersion().journeymap_version.full;
    }
}
