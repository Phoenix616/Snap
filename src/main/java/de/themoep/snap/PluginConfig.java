package de.themoep.snap;

/*
 * Snap
 * Copyright (c) 2021 Max Lee aka Phoenix616 (max@themoep.de)
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class PluginConfig {

    private static final Pattern PATH_PATTERN = Pattern.compile("\\.");

    private final Snap plugin;
    private final Path configFile;
    private final String defaultFile;
    private final HoconConfigurationLoader configLoader;
    private ConfigurationNode config;
    private ConfigurationNode defaultConfig;

    public PluginConfig(Snap plugin, Path configFile) {
        this(plugin, configFile, configFile.getFileName().toString());
    }

    public PluginConfig(Snap plugin, Path configFile, String defaultFile) {
        this.plugin = plugin;
        this.configFile = configFile;
        this.defaultFile = defaultFile;
        configLoader = HoconConfigurationLoader.builder()
                .path(configFile)
                .build();
    }

    public boolean load() {
        try {
            config = configLoader.load();
            if (defaultFile != null && plugin.getClass().getClassLoader().getResource(defaultFile) != null) {
                defaultConfig = HoconConfigurationLoader.builder()
                        .path(configFile)
                        .source(() -> new BufferedReader(new InputStreamReader(plugin.getClass().getClassLoader().getResourceAsStream(defaultFile))))
                        .build()
                        .load();
                if (config.empty()) {
                    config = defaultConfig.copy();
                }
            }
            plugin.getLogger().info("Loaded " + configFile.getFileName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().error("Unable to load configuration file " + configFile.getFileName(), e);
            return false;
        }
    }

    public boolean createDefaultConfig() throws IOException {
        try (InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(defaultFile)) {
            if (in == null) {
                plugin.getLogger().warn("No default config '" + defaultFile + "' found in " + plugin.getClass().getSimpleName() + "!");
                return false;
            }
            if (!Files.exists(configFile)) {
                Path parent = configFile.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                try {
                    Files.copy(in, configFile);
                    return true;
                } catch (IOException ex) {
                    plugin.getLogger().error("Could not save '" + defaultFile + "' to " + configFile, ex);
                }
            }
        } catch (IOException ex) {
            plugin.getLogger().error("Could not load default config from " + defaultFile, ex);
        }
        return false;
    }

    public void save() {
        try {
            configLoader.save(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object set(String path, Object value) {
        ConfigurationNode node = config.node(splitPath(path));
        Object prev = node.raw();
        try {
            node.set(value);
        } catch (SerializationException e) {
            plugin.getLogger().error("Could not set node at " + path, e);
        }
        return prev;
    }

    public ConfigurationNode remove(String path) {
        ConfigurationNode node = config.node(splitPath(path));
        try {
            return node.virtual() ? node : node.set(null);
        } catch (SerializationException e) {
            plugin.getLogger().error("Could not remove node at " + path, e);
            return null;
        }
    }

    public ConfigurationNode getRawConfig() {
        return config;
    }

    public ConfigurationNode getRawConfig(String path) {
        return getRawConfig().node(splitPath(path));
    }

    public boolean has(String path) {
        return !getRawConfig(path).virtual();
    }

    public boolean isSection(String path) {
        return getRawConfig(path).isMap();
    }
    
    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int def) {
        return getRawConfig(path).getInt(def);
    }
    
    public double getDouble(String path) {
        return getDouble(path, 0);
    }

    public double getDouble(String path, double def) {
        return getRawConfig(path).getDouble(def);
    }
    
    public String getString(String path) {
        return getRawConfig(path).getString();
    }

    public String getString(String path, String def) {
        return getRawConfig(path).getString(def);
    }
    
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean def) {
        return getRawConfig(path).getBoolean(def);
    }

    private static Object[] splitPath(String key) {
        return PATH_PATTERN.split(key);
    }
}
