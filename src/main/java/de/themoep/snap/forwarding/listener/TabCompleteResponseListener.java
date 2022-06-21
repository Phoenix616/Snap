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
import com.velocitypowered.api.event.player.TabCompleteEvent;
import de.themoep.snap.Snap;
import de.themoep.snap.forwarding.SnapServer;
import net.md_5.bungee.api.event.TabCompleteResponseEvent;

public class TabCompleteResponseListener extends ForwardingListener {

    public TabCompleteResponseListener(Snap snap) {
        super(snap, TabCompleteResponseEvent.class);
    }

    @Subscribe
    public void on(TabCompleteEvent event) {
        TabCompleteResponseEvent e = snap.getBungeeAdapter().getPluginManager().callEvent(new TabCompleteResponseEvent(
                event.getPlayer().getCurrentServer().map(s -> new SnapServer(snap, s)).orElse(null),
                snap.getPlayer(event.getPlayer()),
                event.getSuggestions()
        ));
        if (e.isCancelled()) {
            event.getSuggestions().clear();
        }
    }
}
