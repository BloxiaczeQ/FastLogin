package com.github.games647.fastlogin.bukkit.listener;

import com.github.games647.fastlogin.bukkit.auth.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.ForceLoginTask;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * This listener tells authentication plugins if the player has a premium account and we checked it successfully. So the
 * plugin can skip authentication.
 */
public class ConnectionListener implements Listener {

    private static final long DELAY_LOGIN = 20L / 2;

    private final FastLoginBukkit plugin;

    public ConnectionListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent loginEvent) {
        removeBlockedStatus(loginEvent.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent joinEvent) {
        Player player = joinEvent.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // session exists so the player is ready for force login
            // cases: Paper (firing proxy message before PlayerJoinEvent) or not running proxy and already
            // having the login session from the login process
            BukkitLoginSession session = plugin.getSessionManager().getLoginSession(player.getAddress());
            if (session != null) {
                Runnable forceLoginTask = new ForceLoginTask(plugin.getCore(), player, session);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, forceLoginTask);
            }

            plugin.getProxyManager().markJoinEventFired(player);
            // delay the login process to let auth plugins initialize the player
            // Magic number however as there is no direct event from those plugins
        }, DELAY_LOGIN);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent quitEvent) {
        Player player = quitEvent.getPlayer();

        removeBlockedStatus(player);
        plugin.getCore().getPendingConfirms().remove(player.getUniqueId());
        plugin.getPremiumPlayers().remove(player.getUniqueId());
        plugin.getProxyManager().cleanup(player);
    }

    private void removeBlockedStatus(Player player) {
        player.removeMetadata(plugin.getName(), plugin);
    }
}
