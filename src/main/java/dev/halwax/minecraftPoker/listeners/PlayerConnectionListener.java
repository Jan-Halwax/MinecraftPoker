package dev.halwax.minecraftPoker.listeners;

import dev.halwax.minecraftPoker.Main;
import dev.halwax.minecraftPoker.gamestates.LobbyState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.nio.Buffer;

public class PlayerConnectionListener implements Listener {

    private Main plugin;

    public PlayerConnectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(Main.PREFIX + "§a" + player.getDisplayName() + " §7hat den Server betreten!");
        player.sendMessage(Main.PREFIX + "§7Willkommen auf dem Server!");
        if (!(plugin.getGameStateManager().getCurrentGameState() instanceof LobbyState)) {
            player.sendMessage(Main.PREFIX + "§cDas Spiel hat bereits begonnen! Du kannst mit §a/spectate §c zuschauen!");
            return;
        }
        player.sendMessage(Main.PREFIX + "§7Benutze §a/join §7um dem Spiel beizutreten!"
                + " §7(" + plugin.getPlayers().size() + "/" + LobbyState.MAX_PLAYERS + ")");
    }

    @EventHandler
    public void handlePlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(Main.PREFIX + "§c" + event.getPlayer().getDisplayName() + " §7hat den Server verlassen!");
    }
}
