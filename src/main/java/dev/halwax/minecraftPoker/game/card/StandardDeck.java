package dev.halwax.minecraftPoker.game.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementierung eines Standard-Kartendecks mit 52 Karten.
 */
public class StandardDeck implements Deck {

    private final List<Card> cards = new ArrayList<>();

    public StandardDeck() {
        reset();
    }

    @Override
    public void reset() {
        cards.clear();

        // Alle 52 Karten erzeugen und ins Deck legen
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }

        // Deck mischen
        Collections.shuffle(cards);
    }

    @Override
    public Card drawCard() {
        if (cards.isEmpty()) {
            return null; // oder Exception werfen
        }
        return cards.remove(0);
    }

    @Override
    public List<Card> drawCards(int amount) {
        List<Card> drawnCards = new ArrayList<>();
        for (int i = 0; i < amount && !cards.isEmpty(); i++) {
            drawnCards.add(drawCard());
        }
        return drawnCards;
    }

    @Override
    public int size() {
        return cards.size();
    }
}