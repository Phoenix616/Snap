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

import com.velocitypowered.api.proxy.ServerConnection;
import de.themoep.snap.Snap;
import de.themoep.snap.SnapUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SnapServer implements Server {
    private final Snap snap;
    private final ServerConnection serverConnection;
    private boolean connected = true;

    public SnapServer(Snap snap, ServerConnection serverConnection) {
        this.snap = snap;
        this.serverConnection = serverConnection;
    }

    @Override
    public ServerInfo getInfo() {
        return snap.getServerInfo(serverConnection.getServer());
    }

    @Override
    public void sendData(String channel, byte[] data) {
        serverConnection.sendPluginMessage(SnapUtils.createChannelIdentifier(channel), data);
    }

    @Override
    public InetSocketAddress getAddress() {
        return serverConnection.getServerInfo().getAddress();
    }

    @Override
    public SocketAddress getSocketAddress() {
        return serverConnection.getServerInfo().getAddress();
    }

    @Override
    public void disconnect(String reason) {
        // TODO: This tries to mirror what Bungee does in that case but might not be exact?
        if (serverConnection.getServerInfo().getName().equals(snap.getProxy().getConfiguration().getAttemptConnectionOrder().get(0))) {
            if (snap.getProxy().getConfiguration().getAttemptConnectionOrder().size() == 1) {
                serverConnection.getPlayer().disconnect(LegacyComponentSerializer.legacySection().deserialize(reason));
            } else {
                snap.getProxy().getServer(snap.getProxy().getConfiguration().getAttemptConnectionOrder().get(1))
                        .ifPresent(s -> serverConnection.getPlayer().createConnectionRequest(s).fireAndForget());
            }
        } else {
            snap.getProxy().getServer(snap.getProxy().getConfiguration().getAttemptConnectionOrder().get(0))
                    .ifPresent(s -> serverConnection.getPlayer().createConnectionRequest(s).fireAndForget());
        }
        connected = false;
    }

    @Override
    public void disconnect(BaseComponent... reason) {
        // TODO: This tries to mirror what Bungee does in that case but might not be exact?
        if (serverConnection.getServerInfo().getName().equals(snap.getProxy().getConfiguration().getAttemptConnectionOrder().get(0))) {
            if (snap.getProxy().getConfiguration().getAttemptConnectionOrder().size() == 1) {
                serverConnection.getPlayer().disconnect(SnapUtils.convertComponent(reason));
            } else {
                snap.getProxy().getServer(snap.getProxy().getConfiguration().getAttemptConnectionOrder().get(1))
                        .ifPresent(s -> serverConnection.getPlayer().createConnectionRequest(s).fireAndForget());
            }
        } else {
            snap.getProxy().getServer(snap.getProxy().getConfiguration().getAttemptConnectionOrder().get(0))
                    .ifPresent(s -> serverConnection.getPlayer().createConnectionRequest(s).fireAndForget());
        }
        connected = false;
    }

    @Override
    public void disconnect(BaseComponent reason) {
        disconnect(new BaseComponent[]{reason});
    }

    @Override
    public boolean isConnected() {
        return connected && serverConnection.getPlayer().isActive();
    }

    @Override
    public Unsafe unsafe() {
        return (Unsafe) snap.unsupported("Unsafe is not supported by Snap!");
    }
}
