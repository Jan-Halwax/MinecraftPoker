package dev.halwax.minecraftPoker.game.card;

import java.util.List;

/**
 * Interface für ein Kartendeck mit Basisfunktionalitäten.
 */
public interface Deck {

    /**
     * Setzt das Deck zurück und mischt die Karten.
     */
    void reset();

    /**
     * Zieht die oberste Karte und entfernt sie aus dem Deck.
     *
     * @return Eine Karte oder null, wenn das Deck leer ist.
     */
    Card drawCard();

    /**
     * Zieht mehrere Karten auf einmal.
     *
     * @param amount Anzahl der zu ziehenden Karten
     * @return Liste mit den gezogenen Karten
     */
    List<Card> drawCards(int amount);

    /**
     * Gibt die Anzahl der verbleibenden Karten im Deck zurück.
     */
    int size();
}