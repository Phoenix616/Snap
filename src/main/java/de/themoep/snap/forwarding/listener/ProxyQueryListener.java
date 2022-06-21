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
import de.themoep.snap.Snap;
import io.github.waterfallmc.waterfall.QueryResult;
import io.github.waterfallmc.waterfall.event.ProxyQueryEvent;

import java.util.ArrayList;

public class ProxyQueryListener extends ForwardingListener {

    public ProxyQueryListener(Snap snap) {
        super(snap, ProxyQueryEvent.class);
    }

    @Subscribe
    public void on(com.velocitypowered.api.event.query.ProxyQueryEvent event) {
        QueryResult r = snap.getBungeeAdapter().getPluginManager().callEvent(new ProxyQueryEvent(
                snap.getBungeeAdapter().getProxy().getListener(),
                new QueryResult(
                        event.getResponse().getHostname(),
                        "SMP",
                        event.getResponse().getMap(),
                        event.getResponse().getCurrentPlayers(),
                        event.getResponse().getMaxPlayers(),
                        event.getResponse().getProxyPort(),
                        event.getResponse().getProxyHost(),
                        "MINECRAFT",
                        new ArrayList<>(event.getResponse().getPlayers()),
                        event.getResponse().getGameVersion()
                )
        )).getResult();

        event.setResponse(event.getResponse().toBuilder()
                .hostname(r.getMotd())
                //.gameType(r.getGameType()) // TODO: Not supported
                .map(r.getWorldName())
                .currentPlayers(r.getOnlinePlayers())
                .maxPlayers(r.getMaxPlayers())
                .proxyPort(r.getPort())
                .proxyHost(r.getAddress())
                .players(r.getPlayers())
                .gameVersion(r.getVersion())
                .build());
    }
}
