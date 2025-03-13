package dev.halwax.minecraftPoker.game;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import dev.halwax.minecraftPoker.game.player.PokerPlayer;

/**
 * Interface für ein Poker-Spiel mit Grundfunktionalitäten.
 */
public interface Game {

    /**
     * Fügt einen Spieler zum Spiel hinzu.
     *
     * @param player        Bukkit-Spieler
     * @param holeCardSlot1 Slot für die erste Hole Card
     * @param holeCardSlot2 Slot für die zweite Hole Card
     * @param chipSlot      Slot für die Chip-Anzeige
     * @param betSlot       Slot für die Bet-Anzeige
     */
    boolean addPlayer(Player player, int holeCardSlot1, int holeCardSlot2, int chipSlot, int betSlot);

    /**
     * Startet das Spiel.
     */
    void startGame();

    /**
     * Stoppt das Spiel.
     */
    void stopGame();

    /**
     * Gibt zurück, ob das Spiel gestartet wurde.
     */
    boolean isGameStarted();

    /**
     * Gibt die Liste der Poker-Spieler zurück.
     */
    List<PokerPlayer> getPlayers();

    /**
     * Gibt den Pot (Gesamteinsatz) zurück.
     */
    int getPot();

    /**
     * Gibt das Spiel-Inventar zurück.
     */
    Inventory getGameInventory();

    /**
     * Sucht einen Poker-Spieler anhand seiner UUID.
     *
     * @param uuid UUID des Spielers
     * @return PokerPlayer oder null, wenn nicht gefunden
     */
    PokerPlayer getPlayerByUUID(UUID uuid);

    /**
     * Behandelt eine Spieleraktion (FOLD, CALL, RAISE).
     *
     * @param player Poker-Spieler
     * @param action Name der Aktion ("fold", "call", "raise")
     * @param raiseAmount Einsatz bei RAISE (bei anderen Aktionen egal)
     */
    void handlePlayerAction(PokerPlayer player, String action, int raiseAmount);
}