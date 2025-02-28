package dev.halwax.minecraftPoker.listeners;

import dev.halwax.minecraftPoker.Main;
import dev.halwax.minecraftPoker.gamestates.IngameState;
import dev.halwax.minecraftPoker.gamestates.LobbyState;
import dev.halwax.minecraftPoker.gamestates.PreLobbyState;
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
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage(Main.PREFIX + "§7Willkommen auf dem Server!");
            switch (plugin.getGameStateManager().getCurrentGameState().getClass().getSimpleName()) {
                case "PreLobbyState" -> player.sendMessage(Main.PREFIX + "§7Das Spiel hat noch nicht begonnen! Du kannst mit §a/create §7 ein Spiel erstellen!");
                case "LobbyState" -> player.sendMessage(Main.PREFIX + "§7Das Spiel hat noch nicht begonnen! Du kannst mit §a/join §7 dem Spiel beitreten! "
                                    + "§7(" + plugin.getPlayers().size() + "/" + LobbyState.MAX_PLAYERS + ")");
                case "IngameState" -> player.sendMessage(Main.PREFIX + "§cDas Spiel hat bereits begonnen! Du kannst mit §a/spectate §c zuschauen!");
            }
        }, 1L);
    }

    @EventHandler
    public void handlePlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(Main.PREFIX + "§c" + event.getPlayer().getDisplayName() + " §7hat den Server verlassen!");
    }
}
