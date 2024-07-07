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

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import de.themoep.snap.Snap;
import de.themoep.snap.forwarding.SnapServerInfo;
import net.md_5.bungee.api.event.PostLoginEvent;

public class PostLoginListener extends ForwardingListener {

    public PostLoginListener(Snap snap) {
        super(snap, PostLoginEvent.class);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void on(PlayerChooseInitialServerEvent event, Continuation continuation) {
        snap.getBungeeAdapter().getPluginManager().callEvent(new PostLoginEvent(
                snap.getPlayer(event.getPlayer()),
                snap.getServerInfo(event.getInitialServer().orElse(null)),
                (e, t) -> {
                    if (e.getTarget() != null) {
                        event.setInitialServer(((SnapServerInfo) e.getTarget()).getServer());
                    } else {
                        event.setInitialServer(null);
                    }
                    if (t != null) {
                        continuation.resumeWithException(t);
                    } else {
                        continuation.resume();
                    }
                }
        ));

    }

}
