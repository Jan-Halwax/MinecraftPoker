package dev.halwax.minecraftPoker.game;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev.halwax.minecraftPoker.game.player.PokerPlayer;

/**
 * Zuständig für das Senden von Nachrichten an alle Spieler.
 */
public class GameMessageBroadcaster {

    private final List<PokerPlayer> players;

    public GameMessageBroadcaster(List<PokerPlayer> players) {
        this.players = players;
    }

    /**
     * Sendet eine Nachricht an alle Spieler im Spiel.
     *
     * @param message Die zu sendende Nachricht
     */
    public void broadcastMessage(String message) {
        for (PokerPlayer pp : players) {
            Player p = Bukkit.getPlayer(pp.getUuid());
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }
}