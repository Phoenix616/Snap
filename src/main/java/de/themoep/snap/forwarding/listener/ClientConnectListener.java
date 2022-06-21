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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import de.themoep.snap.Snap;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.event.ClientConnectEvent;

public class ClientConnectListener extends ForwardingListener {

    // TODO: Find better implementation as this has no real Velocity equivalent
    public ClientConnectListener(Snap snap) {
        super(snap, ClientConnectEvent.class);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void on(PreLoginEvent event) {
        ClientConnectEvent e = new ClientConnectEvent(
                event.getConnection().getRemoteAddress(),
                snap.getBungeeAdapter().getProxy().getListener()
        );
        e.setCancelled(!event.getResult().isAllowed());
        snap.getBungeeAdapter().getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            boolean originallyAllowed = event.getResult().isAllowed();
            if (originallyAllowed) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("ClientConnectEvent Cancelled")));
            }
        } else {
            event.setResult(PreLoginEvent.PreLoginComponentResult.allowed());
        }
    }
}
