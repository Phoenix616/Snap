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

import com.google.common.collect.Lists;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.themoep.snap.forwarding.SnapCommandSender;
import de.themoep.snap.forwarding.SnapProxyServer;
import de.themoep.snap.forwarding.listener.ChatListener;
import de.themoep.snap.forwarding.listener.ClientConnectListener;
import de.themoep.snap.forwarding.listener.ConnectionInitListener;
import de.themoep.snap.forwarding.listener.LoginListener;
import de.themoep.snap.forwarding.listener.PlayerDisconnectListener;
import de.themoep.snap.forwarding.listener.PlayerHandshakeListener;
import de.themoep.snap.forwarding.listener.PluginMessageListener;
import de.themoep.snap.forwarding.listener.PostLoginListener;
import de.themoep.snap.forwarding.listener.PreLoginListener;
import de.themoep.snap.forwarding.listener.ProxyPingListener;
import de.themoep.snap.forwarding.listener.ProxyQueryListener;
import de.themoep.snap.forwarding.listener.ProxyReloadListener;
import de.themoep.snap.forwarding.listener.ServerConnectListener;
import de.themoep.snap.forwarding.listener.ServerConnectedListener;
import de.themoep.snap.forwarding.listener.ServerDisconnectListener;
import de.themoep.snap.forwarding.listener.ServerKickListener;
import de.themoep.snap.forwarding.listener.ServerSwitchListener;
import de.themoep.snap.forwarding.listener.SettingsChangedListener;
import de.themoep.snap.forwarding.listener.TabCompleteResponseListener;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Handler;

public class SnapBungeeAdapter {
    private final SnapProxyServer snapProxy;
    private final PluginManager pluginManager;
    private final File pluginsFolder;
    private final Snap snap;

    SnapBungeeAdapter(Snap snap) throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException, IOException, NoSuchMethodException, InvocationTargetException {
        this.snap = snap;

        pluginsFolder = new File(snap.getDataFolder().toFile(), "plugins");
        if (!pluginsFolder.exists()) {
            pluginsFolder.mkdirs();
        }

        snapProxy = new SnapProxyServer(snap);
        net.md_5.bungee.api.ProxyServer.setInstance(snapProxy);
        getClass().getClassLoader().loadClass(Yaml.class.getName());
        pluginManager = new PluginManager(snapProxy);

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

    void registerEvents() {
        // Register forwarding events
        snap.getProxy().getEventManager().register(snap, new ChatListener(snap));
        snap.getProxy().getEventManager().register(snap, new ClientConnectListener(snap));
        snap.getProxy().getEventManager().register(snap, new ConnectionInitListener(snap));
        snap.getProxy().getEventManager().register(snap, new LoginListener(snap));
        snap.getProxy().getEventManager().register(snap, new PlayerDisconnectListener(snap));
        snap.getProxy().getEventManager().register(snap, new PlayerHandshakeListener(snap));
        snap.getProxy().getEventManager().register(snap, new PluginMessageListener(snap));
        snap.getProxy().getEventManager().register(snap, new PostLoginListener(snap));
        snap.getProxy().getEventManager().register(snap, new PreLoginListener(snap));
        // TODO snap.getProxy().getEventManager().register(snap, new ProxyDefineCommandListener(snap)); hard to convert :S
        snap.getProxy().getEventManager().register(snap, new ProxyPingListener(snap));
        snap.getProxy().getEventManager().register(snap, new ProxyQueryListener(snap));
        snap.getProxy().getEventManager().register(snap, new ProxyReloadListener(snap));
        snap.getProxy().getEventManager().register(snap, new ServerConnectedListener(snap));
        snap.getProxy().getEventManager().register(snap, new ServerConnectListener(snap));
        snap.getProxy().getEventManager().register(snap, new ServerDisconnectListener(snap));
        snap.getProxy().getEventManager().register(snap, new ServerKickListener(snap));
        snap.getProxy().getEventManager().register(snap, new ServerSwitchListener(snap));
        snap.getProxy().getEventManager().register(snap, new SettingsChangedListener(snap));
        // TODO snap.getProxy().getEventManager().register(snap, new TabCompleteListener(snap)); no real Velocity equivalent
        snap.getProxy().getEventManager().register(snap, new TabCompleteResponseListener(snap));
    }

    void loadPlugins() {
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

    public SnapProxyServer getProxy() {
        return snapProxy;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public File getPluginsFolder() {
        return pluginsFolder;
    }

    // Code below is under the following license of BungeeCord:
    /*
     *  Copyright (c) 2012, md_5. All rights reserved.
     *
     *  Redistribution and use in source and binary forms, with or without
     *  modification, are permitted provided that the following conditions are met:
     *
     *  Redistributions of source code must retain the above copyright notice, this
     *  list of conditions and the following disclaimer.
     *
     *  Redistributions in binary form must reproduce the above copyright notice,
     *  this list of conditions and the following disclaimer in the documentation
     *  and/or other materials provided with the distribution.
     *
     *  The name of the author may not be used to endorse or promote products derived
     *  from this software without specific prior written permission.
     *
     *  You may not use the software for commercial software hosting services without
     *  written permission from the author.
     *
     *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
     *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
     *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
     *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
     *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
     *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
     *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
     *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
     *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
     *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
     *  POSSIBILITY OF SUCH DAMAGE.
     */

    public void disablePlugins() {
        // From https://github.com/SpigotMC/BungeeCord/blob/c987ee199d3ec93ce13469deefb1e2787d97147c/proxy/src/main/java/net/md_5/bungee/BungeeCord.java#L465-L480
        for (Plugin plugin : Lists.reverse(new ArrayList<>(getPluginManager().getPlugins()))) {
            try {
                plugin.onDisable();
                for (Handler handler : plugin.getLogger().getHandlers()) {
                    handler.close();
                }
            } catch (Throwable t) {
                snap.getLogger().error("Exception disabling plugin " + plugin.getDescription().getName(), t);
            }
            getProxy().getScheduler().cancel(plugin);
            plugin.getExecutorService().shutdownNow();
        }
    }
}
