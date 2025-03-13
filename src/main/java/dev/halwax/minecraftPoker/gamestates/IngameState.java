package dev.halwax.minecraftPoker.gamestates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import dev.halwax.minecraftPoker.Main;

/**
 * Repräsentiert den aktiven Spielzustand (wenn ein Poker-Spiel läuft).
 */
public class IngameState extends GameState {

    private Main plugin;

    public IngameState(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        Bukkit.getLogger().info("IngameState started!");

        // Nachricht an alle Spieler
        plugin.getPlayers().forEach(player ->
                player.sendMessage(Main.PREFIX + ChatColor.GOLD + "Das Poker-Spiel beginnt!"));
    }

    @Override
    public void stop() {
        Bukkit.getLogger().info("IngameState stopped!");

        // Prüfen, ob ein Spiel läuft und ggf. beenden
        if (plugin.getPokerGame().isGameStarted()) {
            plugin.getPokerGame().stopGame();
        }

        // Nachricht an alle Spieler
        plugin.getPlayers().forEach(player ->
                player.sendMessage(Main.PREFIX + ChatColor.RED + "Das Poker-Spiel wurde beendet!"));
    }
}