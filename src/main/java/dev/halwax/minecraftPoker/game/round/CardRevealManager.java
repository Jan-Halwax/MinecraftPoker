package dev.halwax.minecraftPoker.game.round;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import dev.halwax.minecraftPoker.game.GameMessageBroadcaster;
import dev.halwax.minecraftPoker.game.card.Card;
import dev.halwax.minecraftPoker.game.ui.GameUIManager;

/**
 * Verwaltet das Aufdecken der Community-Karten (Flop, Turn, River).
 * Implementiert das Single Responsibility Principle durch Fokus auf Kartenaufdecklogik.
 */
public class CardRevealManager {

    private final List<Card> communityCards;
    private final GameUIManager uiManager;
    private final GameMessageBroadcaster broadcaster;

    public CardRevealManager(List<Card> communityCards, GameUIManager uiManager, GameMessageBroadcaster broadcaster) {
        this.communityCards = communityCards;
        this.uiManager = uiManager;
        this.broadcaster = broadcaster;
    }

    /**
     * Deckt den Flop auf (die ersten drei Community-Karten).
     */
    public void revealFlop(Inventory inventory) {
        if (inventory == null || communityCards.size() < 3) return;

        // Slots 20..22 => communityCards(0..2)
        for (int i = 0; i < 3; i++) {
            int slot = 20 + i;
            Card card = communityCards.get(i);
            inventory.setItem(slot, uiManager.createCardItem(card));
        }
    }

    /**
     * Deckt den Turn auf (die vierte Community-Karte).
     */
    public void revealTurn(Inventory inventory) {
        if (inventory == null || communityCards.size() < 4) return;

        int slot = 20 + 3; // =23
        Card card = communityCards.get(3);
        inventory.setItem(slot, uiManager.createCardItem(card));
    }

    /**
     * Deckt den River auf (die fünfte Community-Karte).
     */
    public void revealRiver(Inventory inventory) {
        if (inventory == null || communityCards.size() < 5) return;

        int slot = 20 + 4; // =24
        Card card = communityCards.get(4);
        inventory.setItem(slot, uiManager.createCardItem(card));
    }

    /**
     * Prüft, ob wir uns in der Pre-Flop Phase befinden (keine Community-Karten aufgedeckt).
     */
    public boolean isPreFlop() {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null) return false;

        // Prüfen, ob die erste Community-Karte noch verdeckt ist
        ItemStack cardItem = inv.getItem(20);
        return cardItem != null && cardItem.getItemMeta() != null &&
                cardItem.getItemMeta().getDisplayName().equals("???");
    }

    /**
     * Prüft, ob wir uns in der Post-Flop Phase befinden (Flop aufgedeckt, Turn verdeckt).
     */
    public boolean isPostFlop() {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null) return false;

        // Prüfen, ob die erste Community-Karte aufgedeckt, aber Turn noch verdeckt ist
        ItemStack flopItem = inv.getItem(20);
        ItemStack turnItem = inv.getItem(23);

        return flopItem != null && flopItem.getItemMeta() != null &&
                !flopItem.getItemMeta().getDisplayName().equals("???") &&
                turnItem != null && turnItem.getItemMeta() != null &&
                turnItem.getItemMeta().getDisplayName().equals("???");
    }

    /**
     * Prüft, ob wir uns in der Post-Turn Phase befinden (Turn aufgedeckt, River verdeckt).
     */
    public boolean isPostTurn() {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null) return false;

        // Prüfen, ob Turn aufgedeckt, aber River noch verdeckt ist
        ItemStack turnItem = inv.getItem(23);
        ItemStack riverItem = inv.getItem(24);

        return turnItem != null && turnItem.getItemMeta() != null &&
                !turnItem.getItemMeta().getDisplayName().equals("???") &&
                riverItem != null && riverItem.getItemMeta() != null &&
                riverItem.getItemMeta().getDisplayName().equals("???");
    }

    /**
     * Prüft, ob wir uns in der Post-River Phase befinden (alle Community-Karten aufgedeckt).
     */
    public boolean isPostRiver() {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null) return false;

        // Prüfen, ob River aufgedeckt ist
        ItemStack riverItem = inv.getItem(24);
        return riverItem != null && riverItem.getItemMeta() != null &&
                !riverItem.getItemMeta().getDisplayName().equals("???");
    }

    /**
     * Erstellt eine Nachricht für das Aufdecken des Flops.
     */
    public String createFlowRevealMessage() {
        return ChatColor.GREEN + "Setzrunde abgeschlossen. Der Flop wird aufgedeckt!";
    }

    /**
     * Erstellt eine Nachricht für das Aufdecken des Turns.
     */
    public String createTurnRevealMessage() {
        return ChatColor.GREEN + "Setzrunde abgeschlossen. Der Turn wird aufgedeckt!";
    }

    /**
     * Erstellt eine Nachricht für das Aufdecken des Rivers.
     */
    public String createRiverRevealMessage() {
        return ChatColor.GREEN + "Setzrunde abgeschlossen. Der River wird aufgedeckt!";
    }
}