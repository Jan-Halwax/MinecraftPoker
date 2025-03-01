package dev.halwax.minecraftPoker.game.ui;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.halwax.minecraftPoker.game.card.Card;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;

/**
 * Verwaltet die UI-Elemente des Poker-Spiels.
 */
public class GameUIManager {

    private Inventory gameInventory;

    /**
     * Erstellt ein neues 9×6-Inventar (54 Slots) für das Poker-Spiel.
     *
     * @return Das erstellte Inventar
     */
    public Inventory createGameInventory() {
        gameInventory = Bukkit.createInventory(null, 54, "Poker Table");

        // Platziere dekorative Items für den Tisch
        decorateTable();

        return gameInventory;
    }

    /**
     * Platziert dekorative Items im Inventar, um den Poker-Tisch zu gestalten.
     */
    private void decorateTable() {
        if (gameInventory == null) return;

        // Tischmitte mit grünem Tuch (grüne Wolle)
        ItemStack tableCloth = new ItemStack(Material.GREEN_WOOL, 1);
        ItemMeta tableMeta = tableCloth.getItemMeta();
        if (tableMeta != null) {
            tableMeta.setDisplayName(ChatColor.DARK_GREEN + "Poker-Tisch");
            tableCloth.setItemMeta(tableMeta);
        }

        // Community-Karten-Bereich markieren
        for (int i = 19; i <= 25; i++) {
            // Überspringe die tatsächlichen Karten-Slots (20-24)
            if (i != 20 && i != 21 && i != 22 && i != 23 && i != 24) {
                gameInventory.setItem(i, tableCloth);
            }
        }

        // Markiere den Pot-Slot mit initialem 0-Chip
        updatePotDisplay(0);
    }

    /**
     * Aktualisiert die Chip-Anzeige eines Spielers.
     *
     * @param player Poker-Spieler
     * @param dealerIndex Index des Dealers
     * @param players Liste aller Spieler für Dealer-Markierung
     */
    public void updateChipDisplay(PokerPlayer player, int dealerIndex, List<PokerPlayer> players) {
        if (gameInventory == null) return;

        int slot = player.getChipSlot();
        int chipCount = player.getChips();
        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
        String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Spieler";

        // "(D)" für den Dealer hinzufügen
        String dealerTag = (players.indexOf(player) == dealerIndex) ? " (D)" : "";

        // Anzeige aktualisieren
        ItemStack stack = new ItemStack(Material.RAW_GOLD, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + playerName + "'s Chips: " + chipCount + dealerTag);
            stack.setItemMeta(meta);
        }

        gameInventory.setItem(slot, stack);
    }

    /**
     * Erstellt ein ItemStack für eine Karte.
     *
     * @param card Die darzustellende Karte
     * @return ItemStack für die Karte
     */
    public ItemStack createCardItem(Card card) {
        ItemStack cardItem = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = cardItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(card.getCode());
            cardItem.setItemMeta(meta);
        }
        return cardItem;
    }

    /**
     * Erstellt ein ItemStack für eine verdeckte Karte.
     *
     * @return ItemStack für eine verdeckte Karte
     */
    public ItemStack createHiddenCardItem() {
        ItemStack card = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = card.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("???");
            card.setItemMeta(meta);
        }
        return card;
    }

    /**
     * Legt dem Spieler in die Slots 15, 16, 17 drei Wollblöcke: FOLD, CHECK/CALL, BET/RAISE.
     *
     * @param player Bukkit-Spieler
     */
    public void giveWoolButtonsToPlayer(Player player) {
        ItemStack fold = new ItemStack(Material.RED_WOOL, 1);
        ItemMeta fm = fold.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(ChatColor.RED + "FOLD");
            fold.setItemMeta(fm);
        }

        ItemStack checkCall = new ItemStack(Material.YELLOW_WOOL, 1);
        ItemMeta cm = checkCall.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.YELLOW + "CHECK/CALL");
            checkCall.setItemMeta(cm);
        }

        ItemStack betRaise = new ItemStack(Material.GREEN_WOOL, 1);
        ItemMeta bm = betRaise.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(ChatColor.GREEN + "BET/RAISE");
            betRaise.setItemMeta(bm);
        }

        player.getInventory().setItem(15, fold);
        player.getInventory().setItem(16, checkCall);
        player.getInventory().setItem(17, betRaise);
    }

    /**
     * Markiert einen Spieler als gefoldet.
     *
     * @param player Poker-Spieler
     */
    public void markPlayerAsFolded(PokerPlayer player) {
        if (gameInventory == null) return;

        ItemStack foldedWool = new ItemStack(Material.RED_WOOL, 1);
        ItemMeta meta = foldedWool.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "FOLDED");
            foldedWool.setItemMeta(meta);
        }

        // Bet-Slot ermitteln
        int betSlot = player.getBetSlot();
        gameInventory.setItem(betSlot, foldedWool);
    }

    /**
     * Aktualisiert die Pot-Anzeige im Inventar.
     *
     * @param potAmount Aktueller Wert des Pots
     */
    public void updatePotDisplay(int potAmount) {
        if (gameInventory == null) return;

        // Pot-Item mit Emeralds darstellen (Menge basierend auf Pot-Größe)
        int amount = Math.min(64, Math.max(1, potAmount / 50)); // 1 Emerald für je 50 Chips
        ItemStack potItem = new ItemStack(Material.EMERALD, amount);
        ItemMeta meta = potItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Pot: " + potAmount + " Chips");
            potItem.setItemMeta(meta);
        }

        // Pot in Slot 31 anzeigen (Mitte des Inventars)
        gameInventory.setItem(31, potItem);
    }

    /**
     * Gibt das aktuelle Spiel-Inventar zurück.
     */
    public Inventory getGameInventory() {
        return gameInventory;
    }
}