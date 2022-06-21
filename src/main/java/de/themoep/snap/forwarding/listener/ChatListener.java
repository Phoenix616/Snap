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
import com.velocitypowered.api.event.player.PlayerChatEvent;
import de.themoep.snap.Snap;
import de.themoep.snap.forwarding.SnapPlayer;
import net.md_5.bungee.api.event.ChatEvent;

public class ChatListener extends ForwardingListener {

    public ChatListener(Snap snap) {
        super(snap, ChatEvent.class);
    }

    @Subscribe
    public void on(PlayerChatEvent event) {
        SnapPlayer player = snap.getPlayer(event.getPlayer());

        ChatEvent e = new ChatEvent(
                player, player.getServer(),
                event.getResult().getMessage().orElse(event.getMessage())
        );
        e.setCancelled(!event.getResult().isAllowed());
        snap.getBungeeAdapter().getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
        } else {
            event.setResult(PlayerChatEvent.ChatResult.message(e.getMessage()));
        }
    }
}
