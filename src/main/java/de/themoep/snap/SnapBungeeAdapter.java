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

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.themoep.snap.forwarding.ForwardingListener;
import de.themoep.snap.forwarding.SnapCommandSender;
import de.themoep.snap.forwarding.SnapProxyServer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.PluginManager;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class SnapBungeeAdapter {
    private final PluginManager pluginManager;
    private final File pluginsFolder;
    private final Snap snap;

    public SnapBungeeAdapter(Snap snap) throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException, IOException, NoSuchMethodException, InvocationTargetException {
        this.snap = snap;

        pluginsFolder = new File(snap.getDataFolder().toFile(), "plugins");
        if (!pluginsFolder.exists()) {
            pluginsFolder.mkdirs();
        }

        net.md_5.bungee.api.ProxyServer snapProxy = new SnapProxyServer(snap);
        net.md_5.bungee.api.ProxyServer.setInstance(snapProxy);
        getClass().getClassLoader().loadClass(Yaml.class.getName());
        pluginManager = new PluginManager(snapProxy);
        snap.getProxy().getEventManager().register(snap, new SnapListener(snap));
        snap.getProxy().getEventManager().register(snap, new ForwardingListener(snap));

        // Replace Yaml instance with one with proper class loader
        Field fYaml = pluginManager.getClass().getDeclaredField("yaml");
        fYaml.setAccessible(true);
        org.yaml.snakeyaml.constructor.Constructor constructor = new CustomClassLoaderConstructor(snap.getClass().getClassLoader());
        PropertyUtils properties = constructor.getPropertyUtils();
        properties.setSkipMissingProperties(true);
        constructor.setPropertyUtils(properties);
        Yaml yaml = new Yaml(constructor);
        fYaml.set(pluginManager, yaml);
    }

    public void loadPlugins() {
        pluginManager.detectPlugins(pluginsFolder);
        pluginManager.loadPlugins();
        pluginManager.enablePlugins();

        CommandManager cm = snap.getProxy().getCommandManager();
        for (Map.Entry<String, Command> e : pluginManager.getCommands()) {
            Command command = e.getValue();
            cm.register(
                    cm.metaBuilder(command.getName()).aliases(command.getAliases()).build(),
                    new SimpleCommand() {

                        @Override
                        public void execute(Invocation invocation) {
                            command.execute(convert(invocation.source()), invocation.arguments());
                        }

                        @Override
                        public boolean hasPermission(Invocation invocation) {
                            return command.hasPermission(convert(invocation.source()));
                        }

                        private CommandSender convert(CommandSource source) {
                            SnapCommandSender sender;
                            if (source instanceof Player) {
                                sender = snap.getPlayer((Player) source);
                            } else {
                                sender = new SnapCommandSender(snap, source);
                            }
                            return sender;
                        }
                    }
            );
        }

        snap.getLogger().info("Loaded " + pluginManager.getPlugins().size() + " plugins!");
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public File getPluginsFolder() {
        return pluginsFolder;
    }
}
