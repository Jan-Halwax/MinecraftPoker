package dev.halwax.minecraftPoker.game.player;

/**
 * Enthält Display-Informationen für Spieler (Slots im Inventar).
 */
public interface PlayerDisplayInfo {

    /**
     * Gibt den Slot für die erste Hole Card zurück.
     */
    int getHoleCardSlot1();

    /**
     * Gibt den Slot für die zweite Hole Card zurück.
     */
    int getHoleCardSlot2();

    /**
     * Gibt den Slot für die Chip-Anzeige zurück.
     */
    int getChipSlot();

    /**
     * Gibt den Slot für die Bet-Anzeige zurück.
     */
    int getBetSlot();
}