package dev.halwax.minecraftPoker.gamestates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import dev.halwax.minecraftPoker.Main;

/**
 * Initialer Zustand des Spiels, bevor ein Poker-Spiel erstellt wurde.
 */
public class PreLobbyState extends GameState {

    private Main plugin;

    public PreLobbyState(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        Bukkit.getLogger().info("PreLobbyState started!");

        // Nachricht an alle Online-Spieler
        Bukkit.getOnlinePlayers().forEach(player ->
                player.sendMessage(Main.PREFIX + ChatColor.AQUA + "Das Poker-Plugin wurde geladen. " +
                        "Nutze /create, um ein neues Spiel zu erstellen!"));
    }

    @Override
    public void stop() {
        Bukkit.getLogger().info("PreLobbyState stopped!");
    }
}