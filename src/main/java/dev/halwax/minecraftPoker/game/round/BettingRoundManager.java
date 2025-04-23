package dev.halwax.minecraftPoker.game.round;

import java.util.List;

import dev.halwax.minecraftPoker.game.GameMessageBroadcaster;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;
import dev.halwax.minecraftPoker.game.ui.GameUIManager;

/**
 * Verwaltet den Zustand einer Setzrunde und regelt den Ablauf.
 * Implementiert das Single Responsibility Principle durch Fokus auf Setzrunden-Logik.
 */
public class BettingRoundManager {

    private final List<PokerPlayer> players;
    private final GameUIManager uiManager;
    private final GameMessageBroadcaster broadcaster;

    private int currentBet;
    private int currentPlayerIndex;
    private boolean bettingRoundActive;
    private int bigBlindIndex;
    private boolean bigBlindHasOption;
    private int firstActivePlayerIndex;

    public BettingRoundManager(List<PokerPlayer> players, GameUIManager uiManager, GameMessageBroadcaster broadcaster) {
        this.players = players;
        this.uiManager = uiManager;
        this.broadcaster = broadcaster;
        this.currentBet = 0;
        this.currentPlayerIndex = 0;
        this.bettingRoundActive = false;
        this.bigBlindIndex = 0;
        this.bigBlindHasOption = false;
        this.firstActivePlayerIndex = 0;
    }

    /**
     * Startet eine neue Setzrunde mit dem angegebenen ersten Spieler.
     */
    public void startBettingRound(int firstPlayerIndex) {
        bettingRoundActive = true;
        currentPlayerIndex = firstPlayerIndex;
        firstActivePlayerIndex = firstPlayerIndex;
    }

    /**
     * Prüft, ob eine komplette Runde (von einem bestimmten Spieler ausgehend) beendet ist.
     */
    public boolean isFullCircleCompleted(int previousPlayerIndex) {
        // Haben wir eine komplette Runde gemacht? (wieder beim ersten Spieler angekommen)
        boolean fullCircle = (previousPlayerIndex == firstActivePlayerIndex - 1) ||
                (previousPlayerIndex == players.size() - 1 && firstActivePlayerIndex == 0);

        return fullCircle;
    }

    /**
     * Prüft, ob die aktuelle Setzrunde beendet ist.
     * Diese Methode prüft nur, ob alle Spieler gleiche Einsätze haben oder all-in sind.
     * Sie berücksichtigt nicht die Big Blind Option.
     */
    public boolean isBettingRoundComplete() {
        // Zähle aktive Spieler
        int activePlayers = countActivePlayers();

        if (activePlayers <= 1) {
            return true; // Nur noch ein Spieler übrig
        }

        // Für jeden aktiven Spieler prüfen, ob sein Einsatz dem aktuellen Höchsteinsatz entspricht
        // oder ob er all-in ist (keine Chips mehr hat)
        for (PokerPlayer pp : players) {
            if (!pp.isFolded() && pp.getCurrentBet() != currentBet && pp.getChips() > 0) {
                return false; // Mindestens ein aktiver Spieler muss noch handeln
            }
        }

        return true;
    }

    /**
     * Zählt aktive Spieler.
     */
    private int countActivePlayers() {
        int count = 0;
        for (PokerPlayer pp : players) {
            if (!pp.isFolded()) {
                count++;
            }
        }
        return count;
    }

    // Getter und Setter
    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public boolean isBettingRoundActive() {
        return bettingRoundActive;
    }

    public void setBettingRoundActive(boolean bettingRoundActive) {
        this.bettingRoundActive = bettingRoundActive;
    }

    public int getBigBlindIndex() {
        return bigBlindIndex;
    }

    public void setBigBlindIndex(int bigBlindIndex) {
        this.bigBlindIndex = bigBlindIndex;
    }

    public boolean isBigBlindHasOption() {
        return bigBlindHasOption;
    }

    public void setBigBlindHasOption(boolean bigBlindHasOption) {
        this.bigBlindHasOption = bigBlindHasOption;
    }
}