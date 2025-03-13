package dev.halwax.minecraftPoker.game.player;

import java.util.UUID;

import dev.halwax.minecraftPoker.game.card.Card;

/**
 * Interface für einen Poker-Spieler mit Grundfunktionalitäten.
 */
public interface Player {

    /**
     * Gibt die UUID des Spielers zurück.
     */
    UUID getUuid();

    /**
     * Gibt die aktuelle Chip-Anzahl des Spielers zurück.
     */
    int getChips();

    /**
     * Setzt die Chip-Anzahl des Spielers.
     */
    void setChips(int chips);

    /**
     * Gibt zurück, ob der Spieler gefoldet hat.
     */
    boolean isFolded();

    /**
     * Setzt den Fold-Status des Spielers.
     */
    void setFolded(boolean folded);

    /**
     * Gibt den aktuellen Einsatz des Spielers in der laufenden Setzrunde zurück.
     */
    int getCurrentBet();

    /**
     * Setzt den aktuellen Einsatz des Spielers in der laufenden Setzrunde.
     */
    void setCurrentBet(int currentBet);

    /**
     * Gibt die erste Hole Card des Spielers zurück.
     */
    Card getFirstCard();

    /**
     * Setzt die erste Hole Card des Spielers.
     */
    void setFirstCard(Card card);

    /**
     * Gibt die zweite Hole Card des Spielers zurück.
     */
    Card getSecondCard();

    /**
     * Setzt die zweite Hole Card des Spielers.
     */
    void setSecondCard(Card card);
}