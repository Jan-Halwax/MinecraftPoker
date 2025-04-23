package dev.halwax.minecraftPoker.game.round;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import dev.halwax.minecraftPoker.game.GameMessageBroadcaster;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;
import dev.halwax.minecraftPoker.game.ui.GameUIManager;

/**
 * Verwaltet die Blind-Einsätze im Poker-Spiel.
 * Demonstriert das Single Responsibility Principle durch Fokus auf Blind-Logik.
 */
public class BlindManager {

    private static final int SMALL_BLIND = 10;
    private static final int BIG_BLIND = 20;

    private final List<PokerPlayer> players;
    private final GameUIManager uiManager;
    private final GameMessageBroadcaster broadcaster;

    public BlindManager(List<PokerPlayer> players, GameUIManager uiManager, GameMessageBroadcaster broadcaster) {
        this.players = players;
        this.uiManager = uiManager;
        this.broadcaster = broadcaster;
    }

    /**
     * Setzt die Blinds und zieht die entsprechenden Chips ab.
     *
     * @param smallBlindIndex Index des Small Blind Spielers
     * @param bigBlindIndex Index des Big Blind Spielers
     * @param dealerIndex Index des Dealers für UI-Updates
     * @return Gesamtbetrag der Blinds (für Pot-Berechnung)
     */
    public int postBlinds(int smallBlindIndex, int bigBlindIndex, int dealerIndex) {
        if (players.size() < 2) return 0;

        PokerPlayer sb = players.get(smallBlindIndex);
        PokerPlayer bb = players.get(bigBlindIndex);

        // Höhe der Blinds basierend auf verfügbaren Chips begrenzen
        int sbPaid = Math.min(sb.getChips(), SMALL_BLIND);
        int bbPaid = Math.min(bb.getChips(), BIG_BLIND);

        // Chips abziehen
        sb.setChips(sb.getChips() - sbPaid);
        bb.setChips(bb.getChips() - bbPaid);

        // Aktuellen Einsatz setzen
        sb.setCurrentBet(sbPaid);
        bb.setCurrentBet(bbPaid);

        // Chips aktualisieren
        uiManager.updateChipDisplay(sb, dealerIndex, players);
        uiManager.updateChipDisplay(bb, dealerIndex, players);

        // Bet-Anzeige aktualisieren
        uiManager.updateBetDisplay(sb, SMALL_BLIND, BIG_BLIND);
        uiManager.updateBetDisplay(bb, SMALL_BLIND, BIG_BLIND);

        // Blind-Nachricht senden
        sendBlindMessage(sb, bb, sbPaid, bbPaid);

        // Gesamtbetrag der Blinds zurückgeben (für Pot-Berechnung)
        return sbPaid + bbPaid;
    }

    /**
     * Sendet eine Nachricht über die gesetzten Blinds.
     */
    private void sendBlindMessage(PokerPlayer sb, PokerPlayer bb, int sbPaid, int bbPaid) {
        broadcaster.broadcastMessage(ChatColor.YELLOW + "[Blinds] " +
                Bukkit.getPlayer(sb.getUuid()).getName() + " zahlt Small Blind (" + sbPaid + "), " +
                Bukkit.getPlayer(bb.getUuid()).getName() + " zahlt Big Blind (" + bbPaid + ")");
    }

    /**
     * Gibt den Betrag des Small Blinds zurück.
     */
    public int getSmallBlindAmount() {
        return SMALL_BLIND;
    }

    /**
     * Gibt den Betrag des Big Blinds zurück.
     */
    public int getBigBlindAmount() {
        return BIG_BLIND;
    }
}