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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import de.themoep.snap.Snap;
import de.themoep.snap.SnapUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;

import java.util.Collection;
import java.util.Collections;

public class SnapCommandSender implements net.md_5.bungee.api.CommandSender {
    protected final Snap snap;
    private final CommandSource source;

    public SnapCommandSender(Snap snap, CommandSource source) {
        this.snap = snap;
        this.source = source;
    }

    @Override
    public String getName() {
        return source instanceof ConsoleCommandSource ? "Console" : "Unknown";
    }

    @Override
    public void sendMessage(String message) {
        source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    @Override
    public void sendMessages(String... messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }

    @Override
    public void sendMessage(BaseComponent... message) {
        source.sendMessage(SnapUtils.convertComponent(message));
    }

    @Override
    public void sendMessage(BaseComponent message) {
        source.sendMessage(SnapUtils.convertComponent(message));
    }

    @Override
    public Collection<String> getGroups() {
        // TODO: Hook into permissions plugins?
        snap.unsupported("Tried to get groups for " + getName() + " which is not supported!");
        return Collections.emptySet();
    }

    @Override
    public void addGroups(String... groups) {
        // TODO: Hook into permissions plugins?
        snap.unsupported("Tried to set groups " + String.join(", ", groups) + " for " + getName() + " which is not supported!");
    }

    @Override
    public void removeGroups(String... groups) {
        // TODO: Hook into permissions plugins?
        snap.unsupported("Tried to remove groups " + String.join(", ", groups) + " for " + getName() + " which is not supported!");
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void setPermission(String permission, boolean value) {
        // TODO: Hook into permissions plugins?
        snap.unsupported("Tried to set permission " + permission + " to " + value + " for " + getName() + " which is not supported!");
    }

    @Override
    public Collection<String> getPermissions() {
        // TODO: Hook into permissions plugins?
        snap.unsupported("Tried to get permissions for " + getName() + " which is not supported!");
        return Collections.emptySet();
    }
}
