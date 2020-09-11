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

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import de.themoep.snap.Snap;
import de.themoep.snap.SnapUtils;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;

import static com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

public class ForwardingListener {
    private final Snap snap;

    public ForwardingListener(Snap snap) {
        this.snap = snap;
    }

    @Subscribe
    public void onPlayerConnect(com.velocitypowered.api.event.connection.PreLoginEvent event) {
        PreLoginEvent ple = snap.getBungeeAdapter().getPluginManager().callEvent(new PreLoginEvent(new PendingConnection() {
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
                return ProxyServer.getInstance().getConfig().getListeners().iterator().next();
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
                snap.unsupported("Setting UUID of a connection on PreLoginEvent is not supported in Snap!");
            }

            @Override
            public boolean isOnlineMode() {
                if (event.getResult() == PreLoginComponentResult.forceOnlineMode()) {
                    return true;
                } else if (event.getResult() == PreLoginComponentResult.forceOfflineMode()) {
                    return false;
                }
                throw new UnsupportedOperationException("Getting the online mode of a connection on PreLoginEvent is not supported in Snap!");
            }

            @Override
            public void setOnlineMode(boolean onlineMode) {
                if (onlineMode) {
                    event.setResult(PreLoginComponentResult.forceOnlineMode());
                } else {
                    event.setResult(PreLoginComponentResult.forceOfflineMode());
                }
            }

            @Override
            public boolean isLegacy() {
                snap.unsupported();
                return false;
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
                event.setResult(PreLoginComponentResult.denied(LegacyComponentSerializer.legacySection().deserialize(reason)));
            }

            @Override
            public void disconnect(BaseComponent... reason) {
                event.setResult(PreLoginComponentResult.denied(BungeeComponentSerializer.get().deserialize(reason)));
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
            // TODO: What do we do with this?
        }));
        if (ple.isCancelled()) {
            event.setResult(PreLoginComponentResult.denied(BungeeComponentSerializer.get().deserialize(ple.getCancelReasonComponents())));
        }
    }

    @Subscribe
    public void onPlayerConnect(LoginEvent event) {
        net.md_5.bungee.api.event.LoginEvent e = new net.md_5.bungee.api.event.LoginEvent(
                snap.getPlayer(event.getPlayer()).getPendingConnection(), null);
        if (!event.getResult().isAllowed()) {
            e.setCancelled(true);
            event.getResult().getReasonComponent().ifPresent(c -> {
                e.setCancelReason(SnapUtils.convertComponent(c));
            });
        }
        snap.getBungeeAdapter().getPluginManager().callEvent(e);
        if (e.isCancelled() && event.getResult().isAllowed()) {
            event.setResult(ResultedEvent.ComponentResult.denied(SnapUtils.convertComponent(e.getCancelReasonComponents())));
        } else if (!e.isCancelled() && !event.getResult().isAllowed()) {
            event.setResult(ResultedEvent.ComponentResult.allowed());
        }
    }

    @Subscribe
    public void onPlayerConnect(PostLoginEvent event) {
        snap.getBungeeAdapter().getPluginManager().callEvent(new net.md_5.bungee.api.event.PostLoginEvent(snap.getPlayer(event.getPlayer())));
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        snap.getBungeeAdapter().getPluginManager().callEvent(new net.md_5.bungee.api.event.PlayerDisconnectEvent(snap.getPlayer(event.getPlayer())));
    }
}
