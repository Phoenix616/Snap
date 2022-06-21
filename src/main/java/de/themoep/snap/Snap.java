package de.themoep.snap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.themoep.snap.forwarding.SnapPlayer;
import de.themoep.snap.forwarding.SnapServerInfo;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

public class Snap {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataFolder;
    private SnapBungeeAdapter bungeeAdapter;

    private PluginConfig config;

    private boolean throwUnsupportedException = true;
    private boolean registerAllForwardingListeners = false;

    private Map<UUID, SnapPlayer> players = new ConcurrentHashMap<>();
    private Map<String, SnapPlayer> playerNames = new ConcurrentHashMap<>();
    private Map<String, SnapServerInfo> servers = new ConcurrentHashMap<>();

    private Cache<String, UUID> gameprofileUuidCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.SECONDS).build();

    static {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
    }

    @Inject
    public Snap(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, IOException {
        if (loadConfig()) {
            if (!config.has("stats-id") || config.getString("stats-id").isEmpty()) {
                config.set("stats-id", UUID.randomUUID().toString());
                config.save();
            }
            bungeeAdapter = new SnapBungeeAdapter(this);
            bungeeAdapter.loadPlugins();
            getProxy().getEventManager().register(this, new SnapListener(this));
        } else {
            getLogger().error("Unable to load config! Plugin will not enable.");
        }
    }

    private boolean loadConfig() {
        config = new PluginConfig(this, dataFolder.resolve("snap.conf"));
        try {
            config.createDefaultConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (config.load()) {
            throwUnsupportedException = config.getBoolean("throw-unsupported-exception", throwUnsupportedException);
            registerAllForwardingListeners = config.getBoolean("register-all-listeners", registerAllForwardingListeners);
            return true;
        }
        return false;
    }

    public PluginConfig getConfig() {
        return config;
    }

    public boolean shouldRegisterAllForwardingListeners() {
        return registerAllForwardingListeners;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public SnapBungeeAdapter getBungeeAdapter() {
        return bungeeAdapter;
    }

    public Map<UUID, SnapPlayer> getPlayers() {
        return players;
    }

    public Map<String, SnapPlayer> getPlayerNames() {
        return playerNames;
    }

    public SnapPlayer getPlayer(Player player) {
        SnapPlayer p = players.computeIfAbsent(player.getUniqueId(), u -> new SnapPlayer(this, player));
        playerNames.putIfAbsent(p.getName(), p);
        return p;
    }

    public SnapServerInfo getServerInfo(RegisteredServer server) {
        if (server == null) {
            return null;
        }
        return servers.computeIfAbsent(server.getServerInfo().getName(), u -> new SnapServerInfo(this, server));
    }

    public Map<String, SnapServerInfo> getServers() {
        return servers;
    }

    public Object unsupported(String... message) {
        if (throwUnsupportedException) {
            throw new UnsupportedOperationException(message.length > 0 ? String.join("\n", message): "Not implemented (yet)!");
        }
        return null;
    }

    public void cacheUuidForGameprofile(String username, UUID uuid) {
        gameprofileUuidCache.put(username, uuid);
    }

    public UUID pullCachedUuidForUsername(String username) {
        UUID playerId = gameprofileUuidCache.getIfPresent(username);
        if (playerId != null) {
            gameprofileUuidCache.invalidate(username);
        }
        return playerId;
    }
}
