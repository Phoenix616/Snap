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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.ModInfo;
import de.themoep.snap.Snap;
import de.themoep.snap.SnapUtils;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.SkinConfiguration;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.score.Scoreboard;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SnapPlayer extends SnapCommandSender implements ProxiedPlayer {
    private final Player player;
    private final PendingConnection connection;
    private String displayName;

    public SnapPlayer(Snap snap, Player player) {
        super(snap, player);
        this.player = player;
        connection = new PendingConnection() {
            @Override
            public String getName() {
                return SnapPlayer.this.player.getUsername();
            }

            @Override
            public int getVersion() {
                return player.getProtocolVersion().getProtocol();
            }

            @Override
            public InetSocketAddress getVirtualHost() {
                return player.getVirtualHost().orElse(null);
            }

            @Override
            public ListenerInfo getListener() {
                return snap.getBungeeAdapter().getProxy().getListener();
            }

            @Override
            public String getUUID() {
                return getUniqueId().toString();
            }

            @Override
            public UUID getUniqueId() {
                return player.getUniqueId();
            }

            @Override
            public void setUniqueId(UUID uuid) {
                throw new IllegalStateException("Can only set uuid while state is username");
            }

            @Override
            public boolean isOnlineMode() {
                return player.isOnlineMode();
            }

            @Override
            public void setOnlineMode(boolean onlineMode) {
                throw new IllegalStateException("Can only set online mode while state is username");
            }

            @Override
            public boolean isLegacy() {
                return player.getProtocolVersion().isLegacy();
            }

            @Override
            public boolean isTransferred() {
                return snap.isTransferred(player.getUniqueId());
            }

            @Override
            public CompletableFuture<byte[]> retrieveCookie(String key) {
                return snap.retrieveCookie(player, key);
            }

            @Override
            public InetSocketAddress getAddress() {
                return player.getRemoteAddress();
            }

            @Override
            public SocketAddress getSocketAddress() {
                return getAddress();
            }

            @Override
            public void disconnect(String reason) {
                SnapPlayer.this.disconnect(reason);
            }

            @Override
            public void disconnect(BaseComponent... reason) {
                player.disconnect(SnapUtils.convertComponent(reason));
            }

            @Override
            public void disconnect(BaseComponent reason) {
                player.disconnect(SnapUtils.convertComponent(reason));
            }

            @Override
            public boolean isConnected() {
                return player.isActive();
            }

            @Override
            public Unsafe unsafe() {
                return (Unsafe) snap.unsupported("Unsafe is not supported by Snap!");
            }
        };
        displayName = player.getUsername();
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    @Override
    public void sendMessage(ChatMessageType position, BaseComponent... message) {
        if (position == ChatMessageType.ACTION_BAR) {
            player.sendActionBar(SnapUtils.convertComponent(message));
        } else {
            player.sendMessage(SnapUtils.convertComponent(message), SnapUtils.convertEnum(position, MessageType.SYSTEM));
        }
    }

    @Override
    public void sendMessage(ChatMessageType position, BaseComponent message) {
        sendMessage(position, new BaseComponent[]{message});
    }

    @Override
    public void sendMessage(UUID uuid, BaseComponent... message) {
        player.sendMessage(Identity.identity(uuid), SnapUtils.convertComponent(message));
    }

    @Override
    public void sendMessage(UUID uuid, BaseComponent message) {
        player.sendMessage(Identity.identity(uuid), SnapUtils.convertComponent(message));
    }

    @Override
    public void connect(ServerInfo target) {
        snap.getProxy().getServer(target.getName()).ifPresent(s -> player.createConnectionRequest(s).fireAndForget());
    }

    @Override
    public void connect(ServerInfo target, ServerConnectEvent.Reason reason) {
        // TODO: How to reason?
        connect(target);
    }

    @Override
    public void connect(ServerInfo target, Callback<Boolean> callback) {
        Optional<RegisteredServer> server = snap.getProxy().getServer(target.getName());
        if (server.isPresent()) {
            player.createConnectionRequest(server.get()).connectWithIndication().thenAccept(r -> callback.done(r, null));
        } else {
            callback.done(false, null);
        }
    }

    @Override
    public void connect(ServerInfo serverInfo, Callback<Boolean> callback, boolean retry) {
        connect(serverInfo, (b, e) -> {
            if (!b) {
                connect(serverInfo, callback, b);
            } else {
                callback.done(b, e);
            }
        });
    }

    @Override
    public void connect(ServerInfo serverInfo, Callback<Boolean> callback, boolean retry, int timeout) {
        // TODO: Support timeouts?
        connect(serverInfo, callback, retry);
    }

    @Override
    public void connect(ServerInfo target, Callback<Boolean> callback, ServerConnectEvent.Reason reason) {
        // TODO: How to reason?
        connect(target, callback);
    }

    @Override
    public void connect(ServerInfo serverInfo, Callback<Boolean> callback, boolean retry, ServerConnectEvent.Reason reason, int timeout) {
        // TODO: Reason and timeout
        connect(serverInfo, callback, retry);
    }

    @Override
    public void connect(ServerConnectRequest request) {
        Optional<RegisteredServer> server = snap.getProxy().getServer(request.getTarget().getName());
        if (server.isPresent()) {
            player.createConnectionRequest(server.get()).connect().thenAccept(r -> {
                ServerConnectRequest.Result status;
                switch (r.getStatus()) {
                    case SUCCESS:
                        status = ServerConnectRequest.Result.SUCCESS;
                        break;
                    case ALREADY_CONNECTED:
                        status = ServerConnectRequest.Result.ALREADY_CONNECTED;
                        break;
                    case CONNECTION_IN_PROGRESS:
                        status = ServerConnectRequest.Result.ALREADY_CONNECTING;
                        break;
                    case CONNECTION_CANCELLED:
                        status = ServerConnectRequest.Result.EVENT_CANCEL;
                        break;
                    case SERVER_DISCONNECTED:
                    default:
                        status = ServerConnectRequest.Result.FAIL;
                        break;
                }
                Throwable error = r.getReasonComponent().isPresent()
                        ? new Exception(LegacyComponentSerializer.legacySection().serialize(r.getReasonComponent().get()))
                        : null;
                request.getCallback().done(status, error);
            });
        } else {
            request.getCallback().done(ServerConnectRequest.Result.FAIL, new Exception("Server not found"));
        }
    }

    @Override
    public Server getServer() {
        return player.getCurrentServer().map(s -> new SnapServer(snap, s)).orElse(null);
    }

    @Override
    public int getPing() {
        return player.getPing() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) player.getPing();
    }

    @Override
    public void sendData(String channel, byte[] data) {
        player.sendPluginMessage(SnapUtils.createChannelIdentifier(channel), data);
    }

    @Override
    public PendingConnection getPendingConnection() {
        return connection;
    }

    @Override
    public void chat(String message) {
        player.spoofChatInput(message);
    }

    @Override
    public ServerInfo getReconnectServer() {
        // TODO: Nah
        return (ServerInfo) snap.unsupported();
    }

    @Override
    public void setReconnectServer(ServerInfo server) {
        // TODO: Nah
        snap.unsupported("Setting the reconnect server of a player is not supported in Snap!");
    }

    @Override
    public String getUUID() {
        return getPendingConnection().getUUID();
    }

    @Override
    public UUID getUniqueId() {
        return getPendingConnection().getUniqueId();
    }

    @Override
    public Locale getLocale() {
        return player.getPlayerSettings().getLocale();
    }

    @Override
    public byte getViewDistance() {
        return player.getPlayerSettings().getViewDistance();
    }

    @Override
    public ChatMode getChatMode() {
        return SnapUtils.convertEnum(player.getPlayerSettings().getChatMode(), ChatMode.SHOWN);
    }

    @Override
    public boolean hasChatColors() {
        return player.getPlayerSettings().hasChatColors();
    }

    @Override
    public SkinConfiguration getSkinParts() {
        return new SkinConfiguration() {
            @Override
            public boolean hasCape() {
                return player.getPlayerSettings().getSkinParts().hasCape();
            }

            @Override
            public boolean hasJacket() {
                return player.getPlayerSettings().getSkinParts().hasJacket();
            }

            @Override
            public boolean hasLeftSleeve() {
                return player.getPlayerSettings().getSkinParts().hasLeftSleeve();
            }

            @Override
            public boolean hasRightSleeve() {
                return player.getPlayerSettings().getSkinParts().hasRightSleeve();
            }

            @Override
            public boolean hasLeftPants() {
                return player.getPlayerSettings().getSkinParts().hasLeftPants();
            }

            @Override
            public boolean hasRightPants() {
                return player.getPlayerSettings().getSkinParts().hasRightPants();
            }

            @Override
            public boolean hasHat() {
                return player.getPlayerSettings().getSkinParts().hasHat();
            }
        };
    }

    @Override
    public MainHand getMainHand() {
        return SnapUtils.convertEnum(player.getPlayerSettings().getMainHand(), MainHand.RIGHT);
    }

    @Override
    public void setTabHeader(BaseComponent header, BaseComponent footer) {
        player.getTabList().setHeaderAndFooter(SnapUtils.convertComponent(header), SnapUtils.convertComponent(footer));
    }

    @Override
    public void setTabHeader(BaseComponent[] header, BaseComponent[] footer) {
        player.getTabList().setHeaderAndFooter(SnapUtils.convertComponent(header), SnapUtils.convertComponent(footer));
    }

    @Override
    public void resetTabHeader() {
        player.getTabList().clearHeaderAndFooter();
    }

    @Override
    public void sendTitle(Title title) {
        title.send(this);
    }

    @Override
    public boolean isForgeUser() {
        if (player.getModInfo().isPresent()) {
            return player.getModInfo().get().getType().equalsIgnoreCase("FML");
        }
        return false;
    }

    @Override
    public Map<String, String> getModList() {
        if (isForgeUser()) {
            return player.getModInfo().get().getMods().stream()
                    .collect(Collectors.toMap(ModInfo.Mod::getId, ModInfo.Mod::getVersion));
        }
        return Collections.emptyMap();
    }

    @Override
    public Scoreboard getScoreboard() {
        // TODO: Support that? How? Velocity doesn't do this.
        return (Scoreboard) snap.unsupported("Scoreboards are not supported by Snap");
    }

    @Override
    public CompletableFuture<byte[]> retrieveCookie(String s) {
        return getPendingConnection().retrieveCookie(s);
    }

    @Override
    public void storeCookie(String key, byte[] bytes) {
        try {
            player.storeCookie(Key.key(key), bytes);
        } catch (InvalidKeyException e) {
            snap.unsupported("Tried to store cookie at key '" + key + "' but the provided key was invalid!");
        }
    }

    @Override
    public void transfer(String host, int port) {
        player.transferToHost(new InetSocketAddress(host, port));
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public InetSocketAddress getAddress() {
        return getPendingConnection().getAddress();
    }

    @Override
    public SocketAddress getSocketAddress() {
        return getPendingConnection().getSocketAddress();
    }

    @Override
    public void disconnect(String reason) {
        player.disconnect(LegacyComponentSerializer.legacySection().deserialize(reason));
    }

    @Override
    public void disconnect(BaseComponent... reason) {
        getPendingConnection().disconnect(reason);
    }

    @Override
    public void disconnect(BaseComponent reason) {
        getPendingConnection().disconnect(reason);
    }

    @Override
    public boolean isConnected() {
        return player.isActive();
    }

    @Override
    public Unsafe unsafe() {
        return (Unsafe) snap.unsupported("Unsafe is not supported by Snap!");
    }
}
