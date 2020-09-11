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
import de.themoep.snap.Snap;
import de.themoep.snap.SnapUtils;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class SnapServerInfo implements net.md_5.bungee.api.config.ServerInfo {
    private final Snap snap;

    private final RegisteredServer server;

    public SnapServerInfo(Snap snap, RegisteredServer server) {
        this.snap = snap;
        this.server = server;
    }

    public RegisteredServer getServer() {
        return server;
    }

    @Override
    public String getName() {
        return server.getServerInfo().getName();
    }

    @Override
    public InetSocketAddress getAddress() {
        return server.getServerInfo().getAddress();
    }

    @Override
    public SocketAddress getSocketAddress() {
        return getAddress();
    }

    @Override
    public Collection<ProxiedPlayer> getPlayers() {
        Set<ProxiedPlayer> players = new LinkedHashSet<>();
        for (Player player : server.getPlayersConnected()) {
            players.add(snap.getPlayer(player));
        }
        return players;
    }

    @Override
    public String getMotd() {
        return (String) snap.unsupported("Servers don't have an MOTD in Velocity!");
    }

    @Override
    public boolean isRestricted() {
        snap.unsupported();
        return false;
    }

    @Override
    public String getPermission() {
        snap.unsupported();
        return null;
    }

    @Override
    public boolean canAccess(CommandSender sender) {
        snap.unsupported();
        return true;
    }

    @Override
    public void sendData(String channel, byte[] data) {
        server.sendPluginMessage(SnapUtils.createChannelIdentifier(channel), data);
    }

    @Override
    public boolean sendData(String channel, byte[] data, boolean queue) {
        return server.sendPluginMessage(SnapUtils.createChannelIdentifier(channel), data);
    }

    @Override
    public void ping(Callback<ServerPing> callback) {
        server.ping().whenComplete((p, e) -> callback.done(SnapUtils.convertPing(p), e));
    }
}
