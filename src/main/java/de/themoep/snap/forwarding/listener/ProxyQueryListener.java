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
import com.velocitypowered.api.event.connection.ProxyQueryEvent;
import com.velocitypowered.api.proxy.server.QueryResponse;
import de.themoep.snap.Snap;
import io.github.waterfallmc.waterfall.QueryResult;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class ProxyQueryListener extends ForwardingListener {

    public ProxyQueryListener(Snap snap) {
        super(snap);
    }

    @Subscribe
    public void on(ProxyQueryEvent event) {
        QueryResult r = snap.getBungeeAdapter().getPluginManager().callEvent(new io.github.waterfallmc.waterfall.event.ProxyQueryEvent(
                snap.getBungeeAdapter().getProxy().getListener(new InetSocketAddress(
                        event.response().proxyHost(),
                        event.response().proxyPort()
                )),
                new QueryResult(
                        event.response().hostname(),
                        "SMP",
                        event.response().mapName(),
                        event.response().onlinePlayers(),
                        event.response().maxPlayers(),
                        event.response().proxyPort(),
                        event.response().proxyHost(),
                        "MINECRAFT",
                        new ArrayList<>(event.response().players()),
                        event.response().gameVersion()
                )
        )).getResult();

        event.setResponse(QueryResponse.builder()
                .hostname(r.getMotd())
                //.gameType(r.getGameType()) // TODO: Not supported
                .map(r.getWorldName())
                .onlinePlayers(r.getOnlinePlayers())
                .maxPlayers(r.getMaxPlayers())
                .proxyPort(r.getPort())
                .proxyHost(r.getAddress())
                .players(r.getPlayers())
                .gameVersion(r.getVersion())
                .build());
    }
}
