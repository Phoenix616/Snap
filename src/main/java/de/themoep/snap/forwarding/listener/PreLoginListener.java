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

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import de.themoep.snap.Snap;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PreLoginListener extends ForwardingListener {

    public PreLoginListener(Snap snap) {
        super(snap, PreLoginEvent.class);
    }

    @Subscribe
    public void on(com.velocitypowered.api.event.connection.PreLoginEvent event, Continuation continuation) {
        if (!event.getResult().isAllowed()) {
            return;
        }

        snap.getBungeeAdapter().getPluginManager().callEvent(new PreLoginEvent(new PendingConnection() {
            @Override
            public String getName() {
                return event.getUsername();
            }

            @Override
            public int getVersion() {
                return event.getConnection().getProtocolVersion().getProtocol();
            }

            @Override
            public InetSocketAddress getVirtualHost() {
                return event.getConnection().getVirtualHost().orElse(null);
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
                if (event.getResult() == com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.forceOnlineMode()) {
                    throw new IllegalStateException("Can only set uuid when online mode is false");
                }
                snap.cacheUuidForGameprofile(event.getUsername(), uuid);
            }

            @Override
            public boolean isOnlineMode() {
                if (event.getResult() == com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.forceOnlineMode()) {
                    return true;
                } else if (event.getResult() == com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.forceOfflineMode()) {
                    return false;
                }
                throw new UnsupportedOperationException("Getting the online mode of a connection on PreLoginEvent is not supported in Snap!");
            }

            @Override
            public void setOnlineMode(boolean onlineMode) {
                if (onlineMode) {
                    event.setResult(com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
                } else {
                    event.setResult(com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
                }
            }

            @Override
            public boolean isLegacy() {
                return event.getConnection().getProtocolVersion().isLegacy();
            }

            @Override
            public boolean isTransferred() {
                return snap.isTransferred(event.getUniqueId());
            }

            @Override
            public CompletableFuture<byte[]> retrieveCookie(String key) {
                return snap.retrieveCookie(event.getConnection(), key);
            }

            @Override
            public InetSocketAddress getAddress() {
                return event.getConnection().getRemoteAddress();
            }

            @Override
            public SocketAddress getSocketAddress() {
                return getAddress();
            }

            @Override
            public void disconnect(String reason) {
                event.setResult(com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.denied(LegacyComponentSerializer.legacySection().deserialize(reason)));
            }

            @Override
            public void disconnect(BaseComponent... reason) {
                event.setResult(com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.denied(BungeeComponentSerializer.get().deserialize(reason)));
            }

            @Override
            public void disconnect(BaseComponent reason) {
                disconnect(new BaseComponent[]{reason});
            }

            @Override
            public boolean isConnected() {
                return event.getConnection().isActive();
            }

            @Override
            public Unsafe unsafe() {
                return (Unsafe) snap.unsupported("Unsafe is not supported in Snap!");
            }
        }, (e, t) -> {
            if (e.isCancelled()) {
                event.setResult(com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.denied(
                        BungeeComponentSerializer.get().deserialize(e .getCancelReasonComponents())));
            }
            if (t != null) {
                continuation.resumeWithException(t);
            } else {
                continuation.resume();
            }
        }));

    }
}
