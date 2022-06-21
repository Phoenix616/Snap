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
import de.themoep.snap.Snap;
import de.themoep.snap.SnapUtils;
import de.themoep.snap.forwarding.SnapServerInfo;
import net.md_5.bungee.api.event.ServerKickEvent;

public class ServerKickListener extends ForwardingListener {

    public ServerKickListener(Snap snap) {
        super(snap, ServerKickEvent.class);
    }

    @Subscribe
    public void on(KickedFromServerEvent event) {
        ServerKickEvent e = new ServerKickEvent(
                snap.getPlayer(event.getPlayer()),
                snap.getServerInfo(event.getServer()),
                SnapUtils.convertComponent(event.getServerKickReason().orElse(null)),
                event.getResult() instanceof KickedFromServerEvent.RedirectPlayer ? snap.getServerInfo(((KickedFromServerEvent.RedirectPlayer) event.getResult()).getServer()) : null,
                event.kickedDuringServerConnect() ? ServerKickEvent.State.CONNECTING : ServerKickEvent.State.CONNECTED,
                ServerKickEvent.Cause.UNKNOWN
        );
        e.setCancelled(!event.getResult().isAllowed());
        snap.getBungeeAdapter().getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            if (e.getCancelServer() != null) {
                event.setResult(KickedFromServerEvent.RedirectPlayer.create(((SnapServerInfo) e.getCancelServer()).getServer(), SnapUtils.convertComponent(e.getKickReasonComponent())));
            } else {
                event.setResult(KickedFromServerEvent.Notify.create(SnapUtils.convertComponent(e.getKickReasonComponent())));
            }
        } else {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(SnapUtils.convertComponent(e.getKickReasonComponent())));
        }
    }
}
