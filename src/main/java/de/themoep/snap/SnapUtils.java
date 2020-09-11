package de.themoep.snap;

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

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.Arrays;

public class SnapUtils {

    public static ChannelIdentifier createChannelIdentifier(String channel) {
        if (channel.contains(":")) {
            String[] split = channel.split(":", 2);
            return MinecraftChannelIdentifier.create(split[0], split[1]);
        }
        return new LegacyChannelIdentifier(channel);
    }

    public static <T extends Enum, S extends Enum> T convertEnum(S source, T def) {
        try {
            return (T) Enum.valueOf(def.getClass(), source.name());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    public static BaseComponent[] convertComponent(Component component) {
        return component == null ? new ComponentBuilder().create() : BungeeComponentSerializer.get().serialize(component);
    }

    public static Component convertComponent(BaseComponent... components) {
        return components == null ? Component.empty() : BungeeComponentSerializer.get().deserialize(components);
    }

    public static ServerPing convertPing(com.velocitypowered.api.proxy.server.ServerPing ping) {
        BaseComponent motd = new net.md_5.bungee.api.chat.TextComponent();
        motd.setExtra(Arrays.asList(convertComponent(ping.getDescriptionComponent())));
        return new ServerPing(
                new ServerPing.Protocol(ping.getVersion().getName(), ping.getVersion().getProtocol()),
                ping.getPlayers().map(p -> new ServerPing.Players(
                        p.getMax(),
                        p.getOnline(),
                        p.getSample().stream()
                                .map(s -> new ServerPing.PlayerInfo(s.getName(), s.getId()))
                                .toArray(ServerPing.PlayerInfo[]::new)
                )).orElse(null),
                motd,
                ping.getFavicon().map(f -> Favicon.create(f.getBase64Url())).orElse(null)
        );
    }

    public static ScheduledTask convertTask(Plugin plugin, Runnable runnable, com.velocitypowered.api.scheduler.ScheduledTask vTask) {
        return new ScheduledTask() {
            @Override
            public int getId() {
                return vTask.hashCode();
            }

            @Override
            public Plugin getOwner() {
                return plugin;
            }

            @Override
            public Runnable getTask() {
                return runnable;
            }

            @Override
            public void cancel() {
                vTask.cancel();
            }
        };
    }
}
