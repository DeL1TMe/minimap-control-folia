package com.funniray.minimap.common.voxel.data;

import com.funniray.minimap.common.api.MinimapPlayer;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class VoxelConfig {
    public boolean radarAllowed = true;
    public boolean radarMobsAllowed = true;
    public boolean radarPlayersAllowed = true;
    public boolean cavesAllowed = true;
    public String teleportCommand = "tp %p %x %y %z";

    private boolean getOverride(String nodeSuffix, boolean def, MinimapPlayer player) {
        if (player.hasExplicitPermission("minimap.override."+nodeSuffix+".disabled")) {
            return false;
        } else if (player.hasExplicitPermission("minimap.override."+nodeSuffix+".enabled")) {
            return true;
        }

        return def;
    }

    public VoxelConfig applyOverrides(MinimapPlayer player) {
        VoxelConfig newConfig = new VoxelConfig();
        newConfig.radarAllowed = getOverride("radar", this.radarAllowed, player);
        newConfig.radarMobsAllowed = getOverride("radar-mobs", this.radarMobsAllowed, player);
        newConfig.radarPlayersAllowed = getOverride("radar-players", this.radarPlayersAllowed, player);
        newConfig.cavesAllowed = getOverride("cave-mode", this.cavesAllowed, player);
        newConfig.teleportCommand = this.teleportCommand;

        return newConfig;
    }
}
