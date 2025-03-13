package dev.halwax.minecraftPoker.game.card;

/**
 * Repräsentiert eine einzelne Spielkarte im Poker-Spiel.
 */
public record Card(Suit suit, Rank rank) {

    /**
     * Gibt den String-Code der Karte zurück (z.B. "H7" für Herz 7).
     */
    public String getCode() {
        return suit.getSymbol() + rank.getSymbol();
    }

    @Override
    public String toString() {
        return getCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Card card = (Card) obj;
        return suit == card.suit && rank == card.rank;
    }

}