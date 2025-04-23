package dev.halwax.minecraftPoker.game.card;

public record Card(Suit suit, Rank rank) {

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