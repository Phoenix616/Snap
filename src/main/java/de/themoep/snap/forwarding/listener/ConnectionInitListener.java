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
import com.velocitypowered.api.event.connection.PreLoginEvent;
import de.themoep.snap.Snap;
import io.github.waterfallmc.waterfall.event.ConnectionInitEvent;
import net.kyori.adventure.text.Component;

public class ConnectionInitListener extends ForwardingListener {

    // TODO: Find better implementation as this has no real Velocity equivalent
    public ConnectionInitListener(Snap snap) {
        super(snap, ConnectionInitEvent.class);
    }

    @Subscribe(order = PostOrder.EARLY)
    public void on(PreLoginEvent event, Continuation continuation) {
        if (!event.getResult().isAllowed()) {
            return;
        }

        snap.getBungeeAdapter().getPluginManager().callEvent(new ConnectionInitEvent(
                event.getConnection().getRemoteAddress(),
                snap.getBungeeAdapter().getProxy().getListener(),
                (e, t) -> {
                    if (e.isCancelled()) {
                        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("ConnectionInitEvent Cancelled")));
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
