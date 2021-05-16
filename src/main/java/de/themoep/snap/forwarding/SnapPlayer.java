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

import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.ModInfo;
import de.themoep.snap.Snap;
import de.themoep.snap.SnapUtils;
import net.kyori.adventure.audience.MessageType;
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
import java.util.UUID;
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
                return player.username();
            }

            @Override
            public int getVersion() {
                return player.protocolVersion().protocol();
            }

            @Override
            public InetSocketAddress getVirtualHost() {
                return player.connectedHostname();
            }

            @Override
            public ListenerInfo getListener() {
                return snap.getBungeeAdapter().getProxy().getConfig().getListeners().iterator().next();
            }

            @Override
            public String getUUID() {
                return getUniqueId().toString();
            }

            @Override
            public UUID getUniqueId() {
                return player.id();
            }

            @Override
            public void setUniqueId(UUID uuid) {
                throw new IllegalStateException("Can only set uuid while state is username");
            }

            @Override
            public boolean isOnlineMode() {
                return player.onlineMode();
            }

            @Override
            public void setOnlineMode(boolean onlineMode) {
                throw new IllegalStateException("Can only set online mode while state is username");
            }

            @Override
            public boolean isLegacy() {
                return player.protocolVersion().isLegacy();
            }

            @Override
            public InetSocketAddress getAddress() {
                return getSocketAddress() instanceof InetSocketAddress ? (InetSocketAddress) getSocketAddress() : null;
            }

            @Override
            public SocketAddress getSocketAddress() {
                return player.remoteAddress();
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
        displayName = player.username();
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
    public void connect(ServerInfo target) {
        RegisteredServer server = snap.getProxy().server(target.getName());
        if (server != null) {
            player.createConnectionRequest(server).fireAndForget();
        }
    }

    @Override
    public void connect(ServerInfo target, ServerConnectEvent.Reason reason) {
        // TODO: How to reason?
        connect(target);
    }

    @Override
    public void connect(ServerInfo target, Callback<Boolean> callback) {
        RegisteredServer server = snap.getProxy().server(target.getName());
        if (server != null) {
            player.createConnectionRequest(server).connectWithIndication().thenAccept(r -> callback.done(r, null));
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
        RegisteredServer server = snap.getProxy().server(request.getTarget().getName());
        if (server != null) {
            player.createConnectionRequest(server).connect().thenAccept(r -> {
                ServerConnectRequest.Result status;
                switch (r.status()) {
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
                Throwable error = r.failureReason() != null
                        ? new Exception(LegacyComponentSerializer.legacySection().serialize(r.failureReason()))
                        : null;
                request.getCallback().done(status, error);
            });
        } else {
            request.getCallback().done(ServerConnectRequest.Result.FAIL, new Exception("Server not found"));
        }
    }

    @Override
    public Server getServer() {
        return player.connectedServer() != null ? new SnapServer(snap, player.connectedServer()) : null;
    }

    @Override
    public int getPing() {
        return player.ping() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) player.ping();
    }

    @Override
    public void sendData(String channel, byte[] data) {
        player.sendPluginMessage(SnapUtils.createChannelId(channel), data);
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
        return player.clientSettings().getLocale();
    }

    @Override
    public byte getViewDistance() {
        return player.clientSettings().getViewDistance();
    }

    @Override
    public ChatMode getChatMode() {
        return SnapUtils.convertEnum(player.clientSettings().getChatMode(), ChatMode.SHOWN);
    }

    @Override
    public boolean hasChatColors() {
        return player.clientSettings().hasChatColors();
    }

    @Override
    public SkinConfiguration getSkinParts() {
        return new SkinConfiguration() {
            @Override
            public boolean hasCape() {
                return player.clientSettings().getSkinParts().hasCape();
            }

            @Override
            public boolean hasJacket() {
                return player.clientSettings().getSkinParts().hasJacket();
            }

            @Override
            public boolean hasLeftSleeve() {
                return player.clientSettings().getSkinParts().hasLeftSleeve();
            }

            @Override
            public boolean hasRightSleeve() {
                return player.clientSettings().getSkinParts().hasRightSleeve();
            }

            @Override
            public boolean hasLeftPants() {
                return player.clientSettings().getSkinParts().hasLeftPants();
            }

            @Override
            public boolean hasRightPants() {
                return player.clientSettings().getSkinParts().hasRightPants();
            }

            @Override
            public boolean hasHat() {
                return player.clientSettings().getSkinParts().hasHat();
            }
        };
    }

    @Override
    public MainHand getMainHand() {
        return SnapUtils.convertEnum(player.clientSettings().getMainHand(), MainHand.RIGHT);
    }

    @Override
    public void setTabHeader(BaseComponent header, BaseComponent footer) {
        player.tabList().setHeaderAndFooter(SnapUtils.convertComponent(header), SnapUtils.convertComponent(footer));
    }

    @Override
    public void setTabHeader(BaseComponent[] header, BaseComponent[] footer) {
        player.tabList().setHeaderAndFooter(SnapUtils.convertComponent(header), SnapUtils.convertComponent(footer));
    }

    @Override
    public void resetTabHeader() {
        player.tabList().clearHeaderAndFooter();
    }

    @Override
    public void sendTitle(Title title) {
        title.send(this);
    }

    @Override
    public boolean isForgeUser() {
        if (player.modInfo() != null) {
            return player.modInfo().getType().equalsIgnoreCase("FML");
        }
        return false;
    }

    @Override
    public Map<String, String> getModList() {
        if (isForgeUser()) {
            return player.modInfo().getMods().stream()
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
    public String getName() {
        return player.username();
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
