package com.funniray.minimap.common.api;

import com.funniray.minimap.common.version.Version;

import java.util.List;
import java.util.function.Consumer;

public interface MinimapServer {
    Version getMinecraftVersion();
    String getLoaderVersion();
    String getLoaderName();

    void forEachPlayer(Consumer<MinimapPlayer> action);
    List<MinimapWorld> getWorlds();
}
