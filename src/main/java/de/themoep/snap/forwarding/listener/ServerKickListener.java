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
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.themoep.snap.Snap;
import de.themoep.snap.SnapUtils;
import de.themoep.snap.forwarding.SnapServerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerKickEvent;

import java.util.Optional;

public class ServerKickListener extends ForwardingListener {

    public ServerKickListener(Snap snap) {
        super(snap);
    }

    @Subscribe
    public void on(KickedFromServerEvent event) {
        ServerKickEvent e = new ServerKickEvent(
                snap.getPlayer(event.player()),
                snap.getServerInfo(event.server()),
                event.serverKickReason() != null ? SnapUtils.convertComponent(event.serverKickReason()) : null,
                event.kickedDuringServerConnect() ? getNextServer(event.server()) : null,
                event.kickedDuringServerConnect() ? ServerKickEvent.State.CONNECTING : ServerKickEvent.State.CONNECTED,
                ServerKickEvent.Cause.UNKNOWN
        );
        e.setCancelled(!event.result().isAllowed());
        snap.getBungeeAdapter().getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            if (e.getCancelServer() != null) {
                event.setResult(KickedFromServerEvent.RedirectPlayer.create(((SnapServerInfo) e.getCancelServer()).getServer(), SnapUtils.convertComponent(e.getKickReasonComponent())));
            } else {
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(SnapUtils.convertComponent(e.getKickReasonComponent())));
            }
        } else if (event.result() instanceof KickedFromServerEvent.Notify) {
            event.setResult(KickedFromServerEvent.Notify.create(SnapUtils.convertComponent(e.getKickReasonComponent())));
        } else if (event.result() instanceof KickedFromServerEvent.RedirectPlayer) {
            event.setResult(KickedFromServerEvent.RedirectPlayer.create(((KickedFromServerEvent.RedirectPlayer) event.result()).getServer(), SnapUtils.convertComponent(e.getKickReasonComponent())));
        } else if (event.result() instanceof KickedFromServerEvent.DisconnectPlayer) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(SnapUtils.convertComponent(e.getKickReasonComponent())));
        }
    }

    private ServerInfo getNextServer(RegisteredServer server) {
        RegisteredServer next = null;

        boolean found = false;
        for (String serverName : snap.getProxy().configuration().getAttemptConnectionOrder()) {
            if (!found && serverName.equalsIgnoreCase(server.serverInfo().name())) {
                found = true;
            } else if (found) {
                next = snap.getProxy().server(serverName);
                if (next != null) {
                    break;
                }
            }
        }

        return next != null ? snap.getServerInfo(next) : null;
    }
}
