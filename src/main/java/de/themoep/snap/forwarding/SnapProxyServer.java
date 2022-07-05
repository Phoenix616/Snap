package de.themoep.snap.forwarding;

/*
 * Snap
 * Copyright (c) 2020 Max Lee aka Phoenix616 (max@themoep.de)
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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.themoep.snap.Snap;
import de.themoep.snap.SnapUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyConfig;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;

import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SnapProxyServer extends ProxyServer {
    private final Snap snap;

    private String statsId;
    private Field fIdentifierMap;
    private Set<String> channels = new HashSet<>();
    private final ListenerInfo listener;
    private Collection<ListenerInfo> listeners;
    private Logger logger = Logger.getLogger("Snap");
    private TaskScheduler scheduler;

    public SnapProxyServer(Snap snap) {
        this.snap = snap;
        com.velocitypowered.api.proxy.config.ProxyConfig config = snap.getProxy().getConfiguration();

        statsId = snap.getConfig().getString("stats-id");
        if (statsId == null) {
            statsId = UUID.randomUUID().toString();
        }

        listener = new ListenerInfo(
                snap.getProxy().getBoundAddress(),
                LegacyComponentSerializer.legacySection().serialize(config.getMotd()),
                config.getShowMaxPlayers(),
                60, // Default?
                config.getAttemptConnectionOrder(),
                true,
                config.getForcedHosts().entrySet().stream()
                        .filter(e -> !e.getValue().isEmpty())
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0))),
                "GLOBAL_PING",
                true,
                false, // TODO: Read ping passthrough from Velocity config?
                config.getQueryPort(),
                config.isQueryEnabled(),
                false
        );
        listeners = Collections.singleton(listener);

        scheduler = new TaskScheduler() {

            private Map<Integer, ScheduledTask> scheduledTasks = new HashMap<>();

            @Override
            public void cancel(int i) {
                Optional.ofNullable(scheduledTasks.remove(i)).ifPresent(this::cancel);
            }

            @Override
            public void cancel(ScheduledTask scheduledTask) {
                scheduledTask.cancel();
            }

            @Override
            public int cancel(Plugin plugin) {
                int i = 0;
                for (ScheduledTask task : new ArrayList<>(scheduledTasks.values())) {
                    if (task.getOwner() == plugin) {
                        cancel(task);
                        i++;
                    }
                }
                return i;
            }

            @Override
            public ScheduledTask runAsync(Plugin plugin, Runnable runnable) {
                ScheduledTask bTask = SnapUtils.convertTask(plugin, runnable, snap.getProxy().getScheduler().buildTask(snap, runnable).schedule());
                scheduledTasks.put(bTask.hashCode(), bTask);
                return bTask;
            }

            @Override
            public ScheduledTask schedule(Plugin plugin, Runnable runnable, long delay, TimeUnit timeUnit) {
                ScheduledTask bTask = SnapUtils.convertTask(plugin, runnable, snap.getProxy().getScheduler().buildTask(snap, runnable).delay(delay, timeUnit).schedule());
                scheduledTasks.put(bTask.hashCode(), bTask);
                return bTask;
            }

            @Override
            public ScheduledTask schedule(Plugin plugin, Runnable runnable, long delay, long period, TimeUnit timeUnit) {
                ScheduledTask bTask = SnapUtils.convertTask(plugin, runnable, snap.getProxy().getScheduler().buildTask(snap, runnable).delay(delay, timeUnit).repeat(period, timeUnit).schedule());
                scheduledTasks.put(bTask.hashCode(), bTask);
                return bTask;
            }

            @Override
            public Unsafe unsafe() {
                return (Unsafe) snap.unsupported("Unsafe is not supported by Snap!");
            }
        };

        try {
            fIdentifierMap = snap.getProxy().getChannelRegistrar().getClass().getDeclaredField("identifierMap");
            fIdentifierMap.setAccessible(true);
            fIdentifierMap.get(snap.getProxy().getChannelRegistrar());
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
            e.printStackTrace();
            fIdentifierMap = null;
        }
    }

    public ListenerInfo getListener() {
        return listener;
    }

    @Override
    public String getName() {
        return snap.getProxy().getVersion().getName();
    }

    @Override
    public String getVersion() {
        return snap.getProxy().getVersion().getVersion();
    }

    @Override
    public String getTranslation(String name, Object... args) {
        snap.unsupported("Tried to get translation " + name + " but translations aren't supported (yet)!");
        return null;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Collection<ProxiedPlayer> getPlayers() {
        return snap.getPlayers().values().stream().map(p -> (ProxiedPlayer) p).collect(Collectors.toList());
    }

    @Override
    public ProxiedPlayer getPlayer(String name) {
        return snap.getPlayerNames().get(name);
    }

    @Override
    public ProxiedPlayer getPlayer(UUID uuid) {
        return snap.getPlayers().get(uuid);
    }

    @Override
    public Map<String, ServerInfo> getServers() {
        return snap.getProxy().getAllServers().stream().map(snap::getServerInfo).collect(Collectors.toMap(SnapServerInfo::getName, s -> s));
    }

    @Override
    public Map<String, ServerInfo> getServersCopy() {
        return Collections.unmodifiableMap(getServers());
    }

    @Override
    public ServerInfo getServerInfo(String name) {
        // Bungee returns null if the server name is null instead of throwing an error...
        if (name == null) return null;
        return snap.getProxy().getServer(name).map(snap::getServerInfo).orElse(null);
    }

    @Override
    public PluginManager getPluginManager() {
        return snap.getBungeeAdapter().getPluginManager();
    }

    @Override
    public ConfigurationAdapter getConfigurationAdapter() {
        // TODO: Implement
        return (ConfigurationAdapter) snap.unsupported();
    }

    @Override
    public void setConfigurationAdapter(ConfigurationAdapter adapter) {
        // TODO: Implement
        snap.unsupported();
    }

    @Override
    public ReconnectHandler getReconnectHandler() {
        // TODO: Implement
        return (ReconnectHandler) snap.unsupported();
    }

    @Override
    public void setReconnectHandler(ReconnectHandler handler) {
        // TODO: Implement
        snap.unsupported();
    }

    @Override
    public void stop() {
        snap.getProxy().shutdown();
    }

    @Override
    public void stop(String reason) {
        snap.getProxy().shutdown(LegacyComponentSerializer.legacySection().deserialize(reason));
    }

    @Override
    public void registerChannel(String channel) {
        snap.getProxy().getChannelRegistrar().register(SnapUtils.createChannelIdentifier(channel));
        channels.add(channel);
    }

    @Override
    public void unregisterChannel(String channel) {
        snap.getProxy().getChannelRegistrar().unregister(SnapUtils.createChannelIdentifier(channel));
        channels.remove(channel);
    }

    @Override
    public Collection<String> getChannels() {
        // TODO: Non-reflection way to access registered channels
        if (fIdentifierMap != null) {
            try {
                Map<String, ChannelIdentifier> identifierMap = (Map<String, ChannelIdentifier>) fIdentifierMap.get(snap.getProxy().getChannelRegistrar());
                return identifierMap.keySet();
            } catch (IllegalAccessException | ClassCastException e) {
                e.printStackTrace();
            }
        }
        return Collections.unmodifiableSet(channels);
    }

    @Override
    public String getGameVersion() {
        return snap.getProxy().getVersion().getVersion();
    }

    @Override
    public int getProtocolVersion() {
        // TODO: Get supported protocol versions
        snap.unsupported();
        return 0;
    }

    @Override
    public ServerInfo constructServerInfo(String name, InetSocketAddress address, String motd, boolean restricted) {
        // TODO: Server info support
        return (ServerInfo) snap.unsupported();
    }

    @Override
    public ServerInfo constructServerInfo(String name, SocketAddress address, String motd, boolean restricted) {
        // TODO: Server info support
        return (ServerInfo) snap.unsupported();
    }

    @Override
    public CommandSender getConsole() {
        return new SnapCommandSender(snap, snap.getProxy().getConsoleCommandSource());
    }

    @Override
    public File getPluginsFolder() {
        return snap.getBungeeAdapter().getPluginsFolder();
    }

    @Override
    public TaskScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public int getOnlineCount() {
        return snap.getProxy().getPlayerCount();
    }

    @Override
    public void broadcast(String message) {
        broadcast(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    @Override
    public void broadcast(BaseComponent... message) {
        broadcast(SnapUtils.convertComponent(message));
    }

    @Override
    public void broadcast(BaseComponent message) {
        broadcast(SnapUtils.convertComponent(message));
    }

    private void broadcast(Component component) {
        for (Player player : snap.getProxy().getAllPlayers()) {
            player.sendMessage(component);
        }
    }

    @Override
    public Collection<String> getDisabledCommands() {
        return Collections.emptySet();
    }

    @Override
    public ProxyConfig getConfig() {
        com.velocitypowered.api.proxy.config.ProxyConfig config = snap.getProxy().getConfiguration();
        return new ProxyConfig() {
            @Override
            public int getTimeout() {
                return config.getReadTimeout();
            }

            @Override
            public String getUuid() {
                return statsId;
            }

            @Override
            public Collection<ListenerInfo> getListeners() {
                return listeners;
            }

            @Override
            public Map<String, ServerInfo> getServers() {
                return SnapProxyServer.this.getServers();
            }

            @Override
            public Map<String, ServerInfo> getServersCopy() {
                return SnapProxyServer.this.getServersCopy();
            }

            @Override
            public ServerInfo getServerInfo(String name) {
                return getServers().get(name);
            }

            @Override
            public ServerInfo addServer(ServerInfo server) {
                Optional<RegisteredServer> previous = snap.getProxy().getServer(server.getName());

                if (previous.isPresent()) {
                    if (previous.get().getServerInfo().getName().equals(server.getName())
                            && previous.get().getServerInfo().getAddress().equals(server.getAddress())) {
                        // Don't register the same server twice
                        return server;
                    }
                    snap.getProxy().unregisterServer(previous.get().getServerInfo());
                    snap.getServers().remove(previous.get().getServerInfo().getName());
                }

                ServerInfo previousInfo = snap.getServerInfo(previous.orElse(null));

                RegisteredServer rs = snap.getProxy().registerServer(
                        new com.velocitypowered.api.proxy.server.ServerInfo(server.getName(), server.getAddress()));
                snap.getServerInfo(rs);
                return previousInfo;
            }

            @Override
            public boolean addServers(Collection<ServerInfo> servers) {
                boolean changed = false;
                for (ServerInfo server : servers) {
                    if (server != addServer(server)) changed = true;
                }
                return changed;
            }

            @Override
            public ServerInfo removeServerNamed(String name) {
                return removeServer(getServerInfo(name));
            }

            @Override
            public ServerInfo removeServer(ServerInfo server) {
                if (server instanceof SnapServerInfo) {
                    snap.getProxy().unregisterServer(((SnapServerInfo) server).getServer().getServerInfo());
                    getServers().remove(server.getName());
                    return server;
                }
                return null;
            }

            @Override
            public boolean removeServersNamed(Collection<String> names) {
                boolean changed = false;
                for (String name : names) {
                    if (null != removeServerNamed(name)) changed = true;
                }
                return changed;
            }

            @Override
            public boolean removeServers(Collection<ServerInfo> servers) {
                boolean changed = false;
                for (ServerInfo server : servers) {
                    if (null != removeServer(server)) changed = true;
                }
                return changed;
            }

            @Override
            public boolean isOnlineMode() {
                return config.isOnlineMode();
            }

            @Override
            public boolean isLogCommands() {
                snap.unsupported();
                return true; // TODO: Why can't one read that?
            }

            @Override
            public int getRemotePingCache() {
                snap.unsupported();
                return 0;
            }

            @Override
            public int getPlayerLimit() {
                return config.getShowMaxPlayers();
            }

            @Override
            public Collection<String> getDisabledCommands() {
                return SnapProxyServer.this.getDisabledCommands();
            }

            @Override
            public int getServerConnectTimeout() {
                return config.getConnectTimeout();
            }

            @Override
            public int getRemotePingTimeout() {
                return config.getReadTimeout();
            }

            @Override
            public int getThrottle() {
                return config.getLoginRatelimit();
            }

            @Override
            public boolean isIpForward() {
                snap.unsupported();
                return true;
            }

            @Override
            public String getFavicon() {
                return config.getFavicon().map(f -> f.getBase64Url()).orElse("");
            }

            @Override
            public Favicon getFaviconObject() {
                return config.getFavicon().map(f -> Favicon.create(f.getBase64Url())).orElse(null);
            }

            @Override
            public boolean isLogInitialHandlerConnections() {
                // TODO: Somehow read "show-ping-requests" from Velocity config... why is this not exposed?!?
                snap.unsupported();
                return false;
            }

            @Override
            public String getGameVersion() {
                // TODO: Allow configuring this?
                return snap.getProxy().getVersion().getVersion();
            }

            @Override
            public boolean isUseNettyDnsResolver() {
                snap.unsupported();
                return false;
            }

            @Override
            public int getTabThrottle() {
                snap.unsupported();
                return 0;
            }

            @Override
            public boolean isDisableModernTabLimiter() {
                snap.unsupported();
                return false;
            }

            @Override
            public boolean isDisableEntityMetadataRewrite() {
                snap.unsupported();
                return true;
            }

            @Override
            public boolean isDisableTabListRewrite() {
                snap.unsupported();
                return true;
            }

            @Override
            public int getPluginChannelLimit() {
                snap.unsupported();
                return Integer.MAX_VALUE;
            }

            @Override
            public int getPluginChannelNameLimit() {
                snap.unsupported();
                return Integer.MAX_VALUE;
            }
        };
    }

    @Override
    public Collection<ProxiedPlayer> matchPlayer(String match) {
        SnapPlayer p = snap.getPlayerNames().get(match);
        if (p != null) {
            return Collections.singleton(p);
        }
        Set<ProxiedPlayer> matched = new HashSet<>();
        for (SnapPlayer value : snap.getPlayers().values()) {
            if (value.getName().toLowerCase(Locale.ROOT).startsWith(match.toLowerCase(Locale.ROOT))) {
                matched.add(value);
            }
        }
        return matched;
    }

    @Override
    public Title createTitle() {
        return new SnapTitle();
    }
}
