package de.themoep.snap.forwarding.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import de.themoep.snap.Snap;
import de.themoep.snap.forwarding.SnapServer;

public class TabCompleteListener extends ForwardingListener {

    public TabCompleteListener(Snap snap) {
        super(snap, net.md_5.bungee.api.event.TabCompleteEvent.class);
    }

    @Subscribe
    public void on(com.velocitypowered.api.event.player.TabCompleteEvent event) {
        net.md_5.bungee.api.event.TabCompleteEvent e = snap.getBungeeAdapter().getPluginManager().callEvent(new net.md_5.bungee.api.event.TabCompleteEvent(
                snap.getPlayer(event.getPlayer()),
                event.getPlayer().getCurrentServer().map(s -> new SnapServer(snap, s)).orElse(null),
                event.getPartialMessage(),
                event.getSuggestions()
        ));
        if (e.isCancelled()) {
            event.getSuggestions().clear();
        }
    }
}
