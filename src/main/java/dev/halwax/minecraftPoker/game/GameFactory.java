package dev.halwax.minecraftPoker.game;

/**
 * Factory-Klasse für Game-Objekte.
 * Implementiert das Factory Pattern, um verschiedene Spieltypen zu erstellen.
 */
public class GameFactory {

    /**
     * Erstellt ein neues Poker-Spiel.
     *
     * @return Ein neues Game-Objekt
     */
    public Game createPokerGame() {
        return new PokerGame();
    }
}