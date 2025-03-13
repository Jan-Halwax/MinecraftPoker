package dev.halwax.minecraftPoker.gamestates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import dev.halwax.minecraftPoker.Main;

/**
 * Repräsentiert den Lobby-Zustand, in dem Spieler dem Spiel beitreten können.
 */
public class LobbyState extends GameState {

    public static final int MIN_PLAYERS = 2,  // Mindestens 2 Spieler erforderlich
            MAX_PLAYERS = 5;  // Maximal 5 Spieler erlaubt

    private Main plugin;

    public LobbyState(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        Bukkit.getLogger().info("LobbyState started!");

        // Nachricht an alle Online-Spieler
        Bukkit.getOnlinePlayers().forEach(player ->
                player.sendMessage(Main.PREFIX + ChatColor.YELLOW + "Ein neues Poker-Spiel wurde erstellt! " +
                        "Nutze /join, um beizutreten. (" + plugin.getPlayers().size() + "/" + MAX_PLAYERS + ")"));
    }

    @Override
    public void stop() {
        Bukkit.getLogger().info("LobbyState stopped!");
    }
}