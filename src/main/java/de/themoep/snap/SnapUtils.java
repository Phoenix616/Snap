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

import com.velocitypowered.api.proxy.messages.PluginChannelId;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import com.velocitypowered.api.proxy.server.ServerPing.Version;
import com.velocitypowered.api.util.ModInfo;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SnapUtils {

    public static PluginChannelId createChannelId(String channel) {
        if (channel.contains(":")) {
            String[] split = channel.split(":", 2);
            return PluginChannelId.wrap(Key.key(split[0], split[1]));
        }
        return PluginChannelId.wrap(Key.key("BungeeCord", channel));
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

    public static ServerPing convertPing(com.velocitypowered.api.proxy.server.ServerPing vPing) {
        BaseComponent motd = new net.md_5.bungee.api.chat.TextComponent();
        motd.setExtra(Arrays.asList(convertComponent(vPing.description())));
        ServerPing bPing = new ServerPing(
                new ServerPing.Protocol(vPing.version().name(), vPing.version().protocol()),
                        vPing.players() != null ? new ServerPing.Players(
                                vPing.players().maximum(),
                                vPing.players().online(),
                                vPing.players().sample().stream()
                                        .map(s -> new ServerPing.PlayerInfo(s.name(), s.id()))
                                        .toArray(ServerPing.PlayerInfo[]::new)
                        ) : null,
                motd,
                vPing.favicon() != null ? Favicon.create(vPing.favicon().getBase64Url()) : null
        );
        if (vPing.modInfo() != null) {
            bPing.getModinfo().setType(vPing.modInfo().getType());
            bPing.getModinfo().setModList(vPing.modInfo().getMods().stream()
                    .map(m -> new ServerPing.ModItem(m.getId(), m.getVersion()))
                    .collect(Collectors.toList()));
        }

        return bPing;
    }

    public static com.velocitypowered.api.proxy.server.ServerPing convertPing(ServerPing ping) {
        return new com.velocitypowered.api.proxy.server.ServerPing(
                new Version(ping.getVersion().getProtocol(), ping.getVersion().getName()),
                ping.getPlayers() != null ? new Players(
                        ping.getPlayers().getOnline(),
                        ping.getPlayers().getMax(),
                        Arrays.stream(ping.getPlayers().getSample())
                                .map(p -> new com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer(p.getName(), p.getUniqueId()))
                                .collect(Collectors.toList())
                ) : null,
                convertComponent(ping.getDescriptionComponent()),
                ping.getFaviconObject() != null ? new com.velocitypowered.api.util.Favicon(ping.getFaviconObject().getEncoded()) : null,
                new ModInfo(ping.getModinfo().getType(), ping.getModinfo().getModList().stream()
                        .map(m -> new ModInfo.Mod(m.getModid(), m.getVersion()))
                        .collect(Collectors.toList()))
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
