package de.themoep.snap;

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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.CookieReceiveEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.HandshakeIntent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;

import java.util.UUID;

/**
 * Listeners to manage internal states
 */
public class SnapListener {
    private final Snap snap;

    public SnapListener(Snap snap) {
        this.snap = snap;
    }

    @Subscribe
    public void onHandshake(ConnectionHandshakeEvent event) {
        if (event.getIntent() == HandshakeIntent.TRANSFER && event.getConnection() instanceof Player player) {
            snap.markTransferred(player);
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerConnect(LoginEvent event) {
        if (event.getResult().isAllowed()) {
            snap.getPlayer(event.getPlayer());
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerConnectLast(LoginEvent event) {
        if (!event.getResult().isAllowed()) {
            snap.getPlayers().remove(event.getPlayer().getUniqueId());
            snap.getPlayerNames().remove(event.getPlayer().getUsername());
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerQuit(DisconnectEvent event) {
        snap.invalidate(event.getPlayer());
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        snap.getBungeeAdapter().disablePlugins();
    }

    @Subscribe
    public void onGameprofileRequest(GameProfileRequestEvent event) {
        UUID playerId = snap.pullCachedUuidForUsername(event.getUsername());
        if (playerId != null && !event.isOnlineMode() && !event.getGameProfile().getId().equals(playerId)) {
            event.setGameProfile(new GameProfile(playerId, event.getGameProfile().getName(), event.getGameProfile().getProperties()));
        }
    }

    @Subscribe
    public void onCookieReceive(CookieReceiveEvent event) {
        if (snap.completeCookieRequest(event.getPlayer(), event.getResult().getKey(), event.getResult().getData())) {
            event.setResult(CookieReceiveEvent.ForwardResult.handled());
        }
    }
}
