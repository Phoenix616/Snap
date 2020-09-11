package de.themoep.snap.forwarding;

/*
 * Snap
 * Copyright (c) 2020 Max Lee aka Phoenix616 (max@themoep.de)
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

import de.themoep.snap.SnapUtils;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.time.Duration;

public class SnapTitle implements Title {
    private boolean clear = false;
    private BaseComponent[] title = null;
    private BaseComponent[] subTitle = null;
    private int fadeIn = 20;
    private int stay = 60;
    private int fadeOut = 20;

    @Override
    public Title title(BaseComponent text) {
        return title(new BaseComponent[]{text});
    }

    @Override
    public Title title(BaseComponent... text) {
        this.title = text;
        return this;
    }

    @Override
    public Title subTitle(BaseComponent text) {
        return subTitle(new BaseComponent[]{text});
    }

    @Override
    public Title subTitle(BaseComponent... text) {
        this.subTitle = text;
        return this;
    }

    @Override
    public Title fadeIn(int ticks) {
        this.fadeIn = ticks;
        return this;
    }

    @Override
    public Title stay(int ticks) {
        this.stay = ticks;
        return this;
    }

    @Override
    public Title fadeOut(int ticks) {
        this.fadeOut = ticks;
        return this;
    }

    @Override
    public Title clear() {
        clear = true;
        return this;
    }

    @Override
    public Title reset() {
        clear();
        title = null;
        subTitle = null;
        fadeIn = 20;
        stay = 60;
        fadeOut = 20;
        return this;
    }

    @Override
    public Title send(ProxiedPlayer player) {
        if (player instanceof SnapPlayer) {
            if (clear) {
                ((SnapPlayer) player).getPlayer().clearTitle();
            } else {
                ((SnapPlayer) player).getPlayer().showTitle(net.kyori.adventure.title.Title.title(
                        SnapUtils.convertComponent(title),
                        SnapUtils.convertComponent(subTitle),
                        net.kyori.adventure.title.Title.Times.of(
                                Duration.ofMillis(fadeIn * 50),
                                Duration.ofMillis(stay * 50),
                                Duration.ofMillis(fadeOut * 50)
                        )
                ));
            }
        }
        return this;
    }
}
