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
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.connection.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.themoep.snap.Snap;
import de.themoep.snap.forwarding.SnapServer;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.event.PluginMessageEvent;

public class PluginMessageListener extends ForwardingListener {

    public PluginMessageListener(Snap snap) {
        super(snap);
    }

    @Subscribe
    public void on(com.velocitypowered.api.event.connection.PluginMessageEvent event) {
        Connection sender = convert(event.source(), event.sink());
        Connection receiver = convert(event.sink(), event.source());

        PluginMessageEvent e = new PluginMessageEvent(sender, receiver, event.channel().toString(), event.rawMessage());
        e.setCancelled(!event.result().isAllowed());
        snap.getBungeeAdapter().getPluginManager().callEvent(e);
        event.setResult(e.isCancelled()
                ? com.velocitypowered.api.event.connection.PluginMessageEvent.GenericResult.denied()
                : com.velocitypowered.api.event.connection.PluginMessageEvent.GenericResult.allowed());
    }

    private Connection convert(Object o, Object other) {
        if (o instanceof Player) {
            return snap.getPlayer((Player) o);
        } else if (o instanceof ServerConnection) {
            return new SnapServer(snap, (ServerConnection) o);
        } else if (o instanceof RegisteredServer) {
            if (other instanceof Player && ((Player) other).connectedServer() != null && ((Player) other).connectedServer().serverInfo() == o) {
                return new SnapServer(snap, ((Player) other).connectedServer());
            } else if (!((RegisteredServer) o).connectedPlayers().isEmpty()) {
                return new SnapServer(snap, ((RegisteredServer) o).connectedPlayers().iterator().next().connectedServer());
            }
        }
        return null;
    }

}
