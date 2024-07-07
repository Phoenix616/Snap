package de.themoep.snap.forwarding.listener;

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

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import de.themoep.snap.Snap;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.plugin.Event;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class ForwardingListener {
    protected final Snap snap;
    private final Class<? extends Event> forwardedEvent;

    public ForwardingListener(Snap snap, Class<? extends Event> forwardedEvent) {
        this.snap = snap;
        this.forwardedEvent = forwardedEvent;
    }

    public Class<? extends Event> getForwardedEvent() {
        return forwardedEvent;
    }

    protected PendingConnection convertConnection(InboundConnection connection) {
        return new PendingConnection() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public int getVersion() {
                return connection.getProtocolVersion().getProtocol();
            }

            @Override
            public InetSocketAddress getVirtualHost() {
                return connection.getVirtualHost().orElse(null);
            }

            @Override
            public ListenerInfo getListener() {
                return snap.getBungeeAdapter().getProxy().getListener();
            }

            @Override
            public String getUUID() {
                return null;
            }

            @Override
            public UUID getUniqueId() {
                return null;
            }

            @Override
            public void setUniqueId(UUID uuid) {
                snap.unsupported("Setting UUID of a connection on PlayerHandshakeEvent is not supported in Velocity's API!");
            }

            @Override
            public boolean isOnlineMode() {
                return false;
            }

            @Override
            public void setOnlineMode(boolean onlineMode) {
                snap.unsupported("Setting online of a connection on PlayerHandshakeEvent is not supported in Velocity's API!");
            }

            @Override
            public boolean isLegacy() {
                return connection.getProtocolVersion().isLegacy();
            }

            @Override
            public boolean isTransferred() {
                if (connection instanceof Player player) {
                    return snap.isTransferred(player.getUniqueId());
                }
                snap.unsupported("Tried to check an InboundConnection which is not a Player (" + connection.getClass().getName() + ") for whether it was transferred!");
                return false;
            }

            @Override
            public CompletableFuture<byte[]> retrieveCookie(String key) {
                return snap.retrieveCookie(connection, key);
            }

            @Override
            public InetSocketAddress getAddress() {
                return connection.getRemoteAddress();
            }

            @Override
            public SocketAddress getSocketAddress() {
                return getAddress();
            }

            @Override
            public void disconnect(String reason) {

            }

            @Override
            public void disconnect(BaseComponent... reason) {

            }

            @Override
            public void disconnect(BaseComponent reason) {
                disconnect(new BaseComponent[]{reason});
            }

            @Override
            public boolean isConnected() {
                return connection.isActive();
            }

            @Override
            public Unsafe unsafe() {
                return (Unsafe) snap.unsupported("Unsafe is not supported in Snap!");
            }
        };
    }
}
