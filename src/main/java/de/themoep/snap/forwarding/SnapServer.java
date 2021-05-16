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
import com.velocitypowered.api.proxy.connection.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
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
    private final Player player;
    private final RegisteredServer server;
    private boolean connected = true;

    public SnapServer(Snap snap, ServerConnection serverConnection) {
        this(snap, serverConnection.player(), serverConnection.target());
    }

    public SnapServer(Snap snap, Player player, RegisteredServer server) {
        this.snap = snap;
        this.player = player;
        this.server = server;
    }

    @Override
    public ServerInfo getInfo() {
        return snap.getServerInfo(server);
    }

    @Override
    public void sendData(String channel, byte[] data) {
        server.sendPluginMessage(SnapUtils.createChannelId(channel), data);
    }

    @Override
    public InetSocketAddress getAddress() {
        return getSocketAddress() instanceof InetSocketAddress ? (InetSocketAddress) getSocketAddress() : null;
    }

    @Override
    public SocketAddress getSocketAddress() {
        return server.serverInfo().address();
    }

    @Override
    public void disconnect(String reason) {
        // TODO: This tries to mirror what Bungee does in that case but might not be exact?
        if (server.serverInfo().name().equals(snap.getProxy().configuration().getAttemptConnectionOrder().get(0))) {
            if (snap.getProxy().configuration().getAttemptConnectionOrder().size() == 1) {
                player.disconnect(LegacyComponentSerializer.legacySection().deserialize(reason));
            } else {
                RegisteredServer s = snap.getProxy().server(snap.getProxy().configuration().getAttemptConnectionOrder().get(1));
                if (s != null) {
                    player.createConnectionRequest(s).fireAndForget();
                }
            }
        } else {
            RegisteredServer s = snap.getProxy().server(snap.getProxy().configuration().getAttemptConnectionOrder().get(0));
            if (s != null) {
                player.createConnectionRequest(s).fireAndForget();
            }
        }
        connected = false;
    }

    @Override
    public void disconnect(BaseComponent... reason) {
        // TODO: This tries to mirror what Bungee does in that case but might not be exact?
        if (server.serverInfo().name().equals(snap.getProxy().configuration().getAttemptConnectionOrder().get(0))) {
            if (snap.getProxy().configuration().getAttemptConnectionOrder().size() == 1) {
                player.disconnect(SnapUtils.convertComponent(reason));
            } else {
                RegisteredServer s = snap.getProxy().server(snap.getProxy().configuration().getAttemptConnectionOrder().get(1));
                if (s != null) {
                    player.createConnectionRequest(s).fireAndForget();
                }
            }
        } else {
            RegisteredServer s = snap.getProxy().server(snap.getProxy().configuration().getAttemptConnectionOrder().get(0));
            if (s != null) {
                player.createConnectionRequest(s).fireAndForget();
            }
        }
        connected = false;
    }

    @Override
    public void disconnect(BaseComponent reason) {
        disconnect(new BaseComponent[]{reason});
    }

    @Override
    public boolean isConnected() {
        return connected && player.isActive();
    }

    @Override
    public Unsafe unsafe() {
        return (Unsafe) snap.unsupported("Unsafe is not supported by Snap!");
    }
}
