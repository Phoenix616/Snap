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

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PreLoginEvent;
import de.themoep.snap.Snap;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.PendingConnection;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

public class PreLoginListener extends ForwardingListener {

    public PreLoginListener(Snap snap) {
        super(snap);
    }

    @Subscribe
    public void on(PreLoginEvent event) {
        if (!event.result().isAllowed()) {
            return;
        }

        snap.getBungeeAdapter().getPluginManager().callEvent(new net.md_5.bungee.api.event.PreLoginEvent(new PendingConnection() {
            @Override
            public String getName() {
                return event.username();
            }

            @Override
            public int getVersion() {
                return event.connection().protocolVersion().protocol();
            }

            @Override
            public InetSocketAddress getVirtualHost() {
                return event.connection().connectedHostname();
            }

            @Override
            public ListenerInfo getListener() {
                return snap.getBungeeAdapter().getProxy().getListener(event.connection().connectedHostname());
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
                if (event.onlineMode()) {
                    throw new IllegalStateException("Can only set uuid when online mode is false");
                }
                snap.cacheUuidForGameprofile(event.username(), uuid);
            }

            @Override
            public boolean isOnlineMode() {
                return event.onlineMode();
            }

            @Override
            public void setOnlineMode(boolean onlineMode) {
                event.setOnlineMode(onlineMode);
            }

            @Override
            public boolean isLegacy() {
                return event.connection().protocolVersion().isLegacy();
            }

            @Override
            public InetSocketAddress getAddress() {
                return getSocketAddress() instanceof InetSocketAddress ? (InetSocketAddress) getSocketAddress() : null;
            }

            @Override
            public SocketAddress getSocketAddress() {
                return event.connection().remoteAddress();
            }

            @Override
            public void disconnect(String reason) {
                event.setResult(PreLoginEvent.ComponentResult.denied(LegacyComponentSerializer.legacySection().deserialize(reason)));
            }

            @Override
            public void disconnect(BaseComponent... reason) {
                event.setResult(PreLoginEvent.ComponentResult.denied(BungeeComponentSerializer.get().deserialize(reason)));
            }

            @Override
            public void disconnect(BaseComponent reason) {
                disconnect(new BaseComponent[]{reason});
            }

            @Override
            public boolean isConnected() {
                return event.connection().isActive();
            }

            @Override
            public Unsafe unsafe() {
                return (Unsafe) snap.unsupported("Unsafe is not supported in Snap!");
            }
        }, (e, t) -> {
            if (e.isCancelled()) {
                event.setResult(PreLoginEvent.ComponentResult.denied(BungeeComponentSerializer.get().deserialize(e .getCancelReasonComponents())));
            }
        }));

    }
}
