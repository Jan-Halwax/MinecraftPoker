package dev.halwax.minecraftPoker.game.card;

/**
 * Repräsentiert die dreizehn Kartenwerte: Ass (1), 2-10, Bube, Dame, König.
 */
public enum Rank {
    ACE("1", 14),      // Wert 14 für Poker-Regeln (Ass kann höchste Karte sein)
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("X", 10),      // X wird für 10 verwendet
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13);

    private final String symbol;
    private final int value;

    Rank(String symbol, int value) {
        this.symbol = symbol;
        this.value = value;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getValue() {
        return value;
    }

    /**
     * Findet den Rank anhand des Symbols.
     */
    public static Rank fromSymbol(String symbol) {
        for (Rank rank : values()) {
            if (rank.symbol.equals(symbol)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Unbekanntes Rang-Symbol: " + symbol);
    }
}