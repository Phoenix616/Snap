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
import com.velocitypowered.api.event.Subscribe;
import de.themoep.snap.Snap;
import de.themoep.snap.SnapUtils;
import net.md_5.bungee.api.event.ProxyPingEvent;

public class ProxyPingListener extends ForwardingListener {

    public ProxyPingListener(Snap snap) {
        super(snap, ProxyPingEvent.class);
    }

    @Subscribe
    public void on(com.velocitypowered.api.event.proxy.ProxyPingEvent event, Continuation continuation) {
        snap.getBungeeAdapter().getPluginManager().callEvent(new ProxyPingEvent(
                convertConnection(event.getConnection()),
                SnapUtils.convertPing(event.getPing()),
                (e, t) -> {
                    event.setPing(SnapUtils.convertPing(e.getResponse()));
                    if (t != null) {
                        continuation.resumeWithException(t);
                    } else {
                        continuation.resume();
                    }
                }));
    }
}
