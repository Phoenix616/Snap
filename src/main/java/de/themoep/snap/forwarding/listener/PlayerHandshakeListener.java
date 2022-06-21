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
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import de.themoep.snap.Snap;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.protocol.packet.Handshake;

import java.net.InetSocketAddress;

public class PlayerHandshakeListener extends ForwardingListener {

    public PlayerHandshakeListener(Snap snap) {
        super(snap, PlayerHandshakeEvent.class);
    }

    @Subscribe
    public void on(ConnectionHandshakeEvent event) {
        snap.getBungeeAdapter().getPluginManager().callEvent(new PlayerHandshakeEvent(
                convertConnection(event.getConnection()),
                new Handshake(
                        event.getConnection().getProtocolVersion().getProtocol(),
                        event.getConnection().getVirtualHost().map(InetSocketAddress::getHostString).orElse(null),
                        event.getConnection().getVirtualHost().map(InetSocketAddress::getPort).orElse(0),
                        event.getConnection().getProtocolVersion().getProtocol()
                ) {
                    public void setProtocolVersion(int protocolVersion) {
                        snap.unsupported();
                    }

                    public void setHost(String host) {
                        snap.unsupported();
                    }

                    public void setPort(int port) {
                        snap.unsupported();
                    }

                    public void setRequestedProtocol(int requestedProtocol) {
                        snap.unsupported();
                    }
                }
        ));
    }
}
