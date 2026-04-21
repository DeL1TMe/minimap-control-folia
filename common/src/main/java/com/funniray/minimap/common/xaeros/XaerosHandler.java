package com.funniray.minimap.common.xaeros;

import com.funniray.minimap.common.JavaMinimapPlugin;
import com.funniray.minimap.common.api.MessageHandler;
import com.funniray.minimap.common.api.MinimapPlayer;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XaerosHandler implements MessageHandler {
    private final JavaMinimapPlugin plugin;

    public static final String XAEROS_CHANNEL = "xaerominimap:main";
    public static final String XAEROS_MAP_CHANNEL = "xaeroworldmap:main";
    public static final String XAEROLIB_CHANNEL = "xaerolib:main";
    private static final int XAEROS_NETWORK_VERSION = 3;

    public XaerosHandler(JavaMinimapPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendXaerosJoinPackets(MinimapPlayer player) {
        List<MinimapPlayer.PluginMessage> messages = new ArrayList<>();
        addXaerosHandshakeMessages(messages);
        addXaerosConfigMessages(messages, getEffectiveConfig(player));
        player.sendPluginMessages(messages);
    }

    public void sendXaerosWorldChangePackets(MinimapPlayer player) {
        List<MinimapPlayer.PluginMessage> messages = new ArrayList<>();
        messages.add(pluginMessage(XAEROLIB_CHANNEL, oneByteXaeroLibPacket(1)));
        addXaerosConfigMessages(messages, getEffectiveConfig(player));
        player.sendPluginMessages(messages);
    }

    public void sendXaerosHandshake(MinimapPlayer player) {
        List<MinimapPlayer.PluginMessage> messages = new ArrayList<>();
        addXaerosHandshakeMessages(messages);
        player.sendPluginMessages(messages);
    }

    private void addXaerosHandshakeMessages(List<MinimapPlayer.PluginMessage> messages) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(1);
        out.writeInt(XAEROS_NETWORK_VERSION);
        byte[] legacyHandshake = out.toByteArray();
        messages.add(pluginMessage(XAEROS_CHANNEL, legacyHandshake));
        messages.add(pluginMessage(XAEROS_MAP_CHANNEL, legacyHandshake));
        messages.add(pluginMessage(XAEROLIB_CHANNEL, oneByteXaeroLibPacket(0)));
        messages.add(pluginMessage(XAEROLIB_CHANNEL, oneByteXaeroLibPacket(1)));
    }

    public void sendXaeroLibDimensionHandshake(MinimapPlayer player) {
        player.sendPluginMessages(List.of(pluginMessage(XAEROLIB_CHANNEL, oneByteXaeroLibPacket(1))));
    }

    public void sendXaerosConfig(MinimapPlayer player) {
        List<MinimapPlayer.PluginMessage> messages = new ArrayList<>();
        addXaerosConfigMessages(messages, getEffectiveConfig(player));
        player.sendPluginMessages(messages);
    }

    private XaerosConfig getEffectiveConfig(MinimapPlayer player) {
        XaerosWorldConfig worldConfig = plugin.getConfig().getWorldConfig(player.getLocation().getWorld().getName()).xaerosConfig;
        XaerosConfig config = plugin.getConfig().globalXaerosConfig;
        if (worldConfig != null && worldConfig.enabled) {
            config = worldConfig;
        }

        return config.applyOverrides(player);
    }

    private void addXaerosConfigMessages(List<MinimapPlayer.PluginMessage> messages, XaerosConfig config) {
        addLegacyRulesConfig(messages, config);
        addXaeroLibConfig(messages, config);
    }

    private void addLegacyRulesConfig(List<MinimapPlayer.PluginMessage> messages, XaerosConfig config) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(4);
        CompoundBinaryTag tag = CompoundBinaryTag.builder()
                .putBoolean("cm", config.caveMode)
                .putBoolean("ncm", config.netherCaveMode)
                .putBoolean("r", config.radar)
                .build();
        try {
            writeCurrentNbt(tag, out);
            messages.add(pluginMessage(XAEROS_CHANNEL, out.toByteArray()));

            out = ByteStreams.newDataOutput();
            out.writeByte(4);
            tag = CompoundBinaryTag.builder()
                    .putBoolean("cm", config.caveMode)
                    .putBoolean("ncm", config.netherCaveMode)
                    .build();
            writeCurrentNbt(tag, out);
            messages.add(pluginMessage(XAEROS_MAP_CHANNEL, out.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendXaeroLibServerHandshake(MinimapPlayer player) {
        player.sendPluginMessages(List.of(pluginMessage(XAEROLIB_CHANNEL, oneByteXaeroLibPacket(0))));
    }

    private byte[] oneByteXaeroLibPacket(int packetIndex) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(packetIndex);
        out.writeByte(1);
        return out.toByteArray();
    }

    private void addXaeroLibConfig(List<MinimapPlayer.PluginMessage> messages, XaerosConfig config) {
        messages.add(pluginMessage(XAEROLIB_CHANNEL, configChannelPacket(XAEROS_CHANNEL)));
        messages.add(pluginMessage(XAEROLIB_CHANNEL, enforcedConfigPacket(minimapOptions(config))));
        messages.add(pluginMessage(XAEROLIB_CHANNEL, configChannelPacket(XAEROS_MAP_CHANNEL)));
        messages.add(pluginMessage(XAEROLIB_CHANNEL, enforcedConfigPacket(worldMapOptions(config))));
    }

    private MinimapPlayer.PluginMessage pluginMessage(String channel, byte[] payload) {
        return new MinimapPlayer.PluginMessage(channel, payload);
    }

    private byte[] configChannelPacket(String configChannelId) {
        CompoundBinaryTag tag = CompoundBinaryTag.builder()
                .putString("c", configChannelId)
                .build();
        return nbtXaeroLibPacket(2, tag);
    }

    private byte[] enforcedConfigPacket(CompoundBinaryTag options) {
        CompoundBinaryTag tag = CompoundBinaryTag.builder()
                .putBoolean("r", false)
                .put("o", options)
                .build();
        return nbtXaeroLibPacket(3, tag);
    }

    private byte[] nbtXaeroLibPacket(int packetIndex, CompoundBinaryTag tag) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(packetIndex);
        try {
            writeCurrentNbt(tag, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    private CompoundBinaryTag minimapOptions(XaerosConfig config) {
        return CompoundBinaryTag.builder()
                .putBoolean("minimap_cave_mode_allowed", config.caveMode || config.netherCaveMode)
                .put("minimap_cave_mode_allowed_dimensions", caveModeDimensions(config))
                .putBoolean("display_radar", config.radar)
                .build();
    }

    private CompoundBinaryTag worldMapOptions(XaerosConfig config) {
        return CompoundBinaryTag.builder()
                .putBoolean("cave_mode_allowed", config.caveMode || config.netherCaveMode)
                .put("cave_mode_allowed_dimensions", caveModeDimensions(config))
                .putBoolean("display_minimap_radar", config.radar)
                .build();
    }

    private ListBinaryTag caveModeDimensions(XaerosConfig config) {
        if (config.caveMode && config.netherCaveMode) {
            return ListBinaryTag.empty();
        }

        List<BinaryTag> dimensions = new ArrayList<>();
        if (config.caveMode) {
            dimensions.add(StringBinaryTag.stringBinaryTag("minecraft:overworld"));
            dimensions.add(StringBinaryTag.stringBinaryTag("minecraft:the_end"));
        }
        if (config.netherCaveMode) {
            dimensions.add(StringBinaryTag.stringBinaryTag("minecraft:the_nether"));
        }
        return ListBinaryTag.listBinaryTag(BinaryTagTypes.STRING, dimensions);
    }

    private void writeCurrentNbt(CompoundBinaryTag tag, ByteArrayDataOutput out) throws IOException {
        BinaryTagIO.writer().writeNameless(tag, out);
    }

    @Override
    public void onPluginMessage(String channel, MinimapPlayer player, byte[] message) {
        if (channel.equals(XAEROLIB_CHANNEL)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        if (in.readByte() == 1) {
            int version = in.readInt();
            if (version < 2) {
                player.disconnect(MiniMessage.miniMessage().deserialize("<red>Xaero's Minimap is outdated.\n Please update to 23.7.0 or later."));
                return;
            }

            sendXaerosConfig(player);
        }
    }
}
