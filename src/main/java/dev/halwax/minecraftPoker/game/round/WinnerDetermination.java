package dev.halwax.minecraftPoker.game.round;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import dev.halwax.minecraftPoker.game.GameMessageBroadcaster;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;
import dev.halwax.minecraftPoker.game.ui.GameUIManager;

/**
 * Verantwortlich für die Ermittlung der Gewinner und das Verteilen der Gewinne.
 * Implementiert das Single Responsibility Principle durch Fokus auf Gewinnermittlung.
 */
public class WinnerDetermination {

    private final List<PokerPlayer> players;
    private final GameUIManager uiManager;
    private final GameMessageBroadcaster broadcaster;

    public WinnerDetermination(List<PokerPlayer> players, GameUIManager uiManager, GameMessageBroadcaster broadcaster) {
        this.players = players;
        this.uiManager = uiManager;
        this.broadcaster = broadcaster;
    }

    /**
     * Bestimmt den Gewinner nach dem Showdown.
     * In dieser einfachen Version gewinnt der Spieler links vom Dealer.
     * Eine echte Implementierung würde die Poker-Hände vergleichen.
     *
     * @param dealerIndex Der aktuelle Dealer-Index
     * @return Der ermittelte Gewinner-Spieler
     */
    public PokerPlayer determineWinner(int dealerIndex) {
        // Finde den ersten nicht gefoldeten Spieler links vom Dealer
        PokerPlayer winner = null;
        int startIndex = (dealerIndex + 1) % players.size();
        int currentIndex = startIndex;

        do {
            PokerPlayer pp = players.get(currentIndex);
            if (!pp.isFolded()) {
                winner = pp;
                break;
            }
            currentIndex = (currentIndex + 1) % players.size();
        } while (currentIndex != startIndex);

        return winner;
    }

    /**
     * Findet den letzten aktiven (nicht gefoldeten) Spieler.
     *
     * @param players Liste der Spieler
     * @return Der letzte aktive Spieler oder null, wenn alle gefoldet haben
     */
    public PokerPlayer determineLastActivePlayer(List<PokerPlayer> players) {
        for (PokerPlayer pp : players) {
            if (!pp.isFolded()) {
                return pp;
            }
        }
        return null;
    }

    /**
     * Verkündet den Gewinner und spielt Erfolgs-Sounds.
     *
     * @param winner Der Gewinner
     * @param pot Der Pot, den der Gewinner erhält
     */
    public void announceWinner(PokerPlayer winner, int pot) {
        Player bukkitPlayer = Bukkit.getPlayer(winner.getUuid());
        String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
        broadcaster.broadcastMessage(ChatColor.GREEN + playerName + " gewinnt den Pot mit " +
                pot + " Chips!");

        // Erfolgs-Sound für alle abspielen
        playWinSoundForAll();
    }

    /**
     * Verkündet den Gewinner, wenn alle anderen gefoldet haben.
     *
     * @param winner Der Gewinner
     * @param pot Der Pot, den der Gewinner erhält
     */
    public void announceFoldWinner(PokerPlayer winner, int pot) {
        Player bukkitPlayer = Bukkit.getPlayer(winner.getUuid());
        String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
        broadcaster.broadcastMessage(ChatColor.GREEN + playerName + " gewinnt den Pot mit " +
                pot + " Chips, da alle anderen gefoldet haben!");

        // Erfolgs-Sound für alle abspielen
        playWinSoundForAll();
    }

    /**
     * Spielt einen Gewinn-Sound für alle Spieler.
     */
    private void playWinSoundForAll() {
        for (PokerPlayer pp : players) {
            Player p = Bukkit.getPlayer(pp.getUuid());
            if (p != null && p.isOnline()) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Erstellt eine Nachricht für den Showdown.
     */
    public String createShowdownMessage() {
        return ChatColor.GREEN + "Finale Setzrunde abgeschlossen. Showdown!";
    }
}