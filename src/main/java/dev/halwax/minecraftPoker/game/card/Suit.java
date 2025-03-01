package dev.halwax.minecraftPoker.game.card;

/**
 * Repräsentiert die vier Kartenfarben: Kreuz, Karo, Herz und Pik.
 */
public enum Suit {
    CLUBS("C"),
    DIAMONDS("D"),
    HEARTS("H"),
    SPADES("S");

    private final String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}