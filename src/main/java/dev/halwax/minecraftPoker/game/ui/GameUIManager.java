package dev.halwax.minecraftPoker.game.ui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.halwax.minecraftPoker.game.card.Card;
import dev.halwax.minecraftPoker.game.card.Rank;
import dev.halwax.minecraftPoker.game.card.Suit;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;

/**
 * Verwaltet die UI-Elemente des Poker-Spiels.
 * Diese Klasse ist verantwortlich für alle visuellen Aspekte des Spiels im Minecraft-Inventar,
 * was dem Single Responsibility Principle entspricht.
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
            // Den originalen Code als Namen beibehalten
            meta.setDisplayName(card.getCode());

            // Ausführlichen Kartennamen und Farbinfo als Lore hinzufügen
            List<String> lore = new ArrayList<>();

            // Farbe der Karte bestimmen und im Lore anzeigen
            ChatColor cardColor = getCardColor(card.suit());
            String suitName = getSuitName(card.suit());
            String rankName = getRankName(card.rank());

            // Zuerst die Farbe, dann der Wert (z.B. "Herz Ass")
            lore.add(cardColor + suitName + " " + rankName);

            // Je nach Farbe und Wert der Karte zusätzliche Info anzeigen
            if (card.rank() == Rank.ACE) {
                lore.add(ChatColor.GRAY + "Kann als höchste oder niedrigste Karte gelten");
            }

            meta.setLore(lore);
            cardItem.setItemMeta(meta);
        }
        return cardItem;
    }

    /**
     * Bestimmt die Chat-Farbe für eine Kartenfarbe.
     *
     * @param suit Die Kartenfarbe
     * @return Die entsprechende Chat-Farbe
     */
    private ChatColor getCardColor(Suit suit) {
        return switch (suit) {
            case HEARTS, DIAMONDS -> ChatColor.RED;
            case SPADES, CLUBS -> ChatColor.DARK_GRAY;
        };
    }

    /**
     * Gibt den Namen der Kartenfarbe auf Deutsch zurück.
     *
     * @param suit Die Kartenfarbe
     * @return Der deutsche Name der Farbe
     */
    private String getSuitName(Suit suit) {
        return switch (suit) {
            case HEARTS -> "Herz";
            case DIAMONDS -> "Karo";
            case SPADES -> "Pik";
            case CLUBS -> "Kreuz";
        };
    }

    /**
     * Gibt den Namen des Kartenwertes auf Deutsch zurück.
     *
     * @param rank Der Kartenwert
     * @return Der deutsche Name des Wertes
     */
    private String getRankName(Rank rank) {
        return switch (rank) {
            case ACE -> "Ass";
            case TWO -> "Zwei";
            case THREE -> "Drei";
            case FOUR -> "Vier";
            case FIVE -> "Fünf";
            case SIX -> "Sechs";
            case SEVEN -> "Sieben";
            case EIGHT -> "Acht";
            case NINE -> "Neun";
            case TEN -> "Zehn";
            case JACK -> "Bube";
            case QUEEN -> "Dame";
            case KING -> "König";
        };
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

            // Lore für verdeckte Karte
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Diese Karte ist noch verdeckt");
            meta.setLore(lore);

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

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Klicke, um aus dieser Runde auszusteigen");
            lore.add(ChatColor.GRAY + "Du verlierst alle bisher gesetzten Chips");
            fm.setLore(lore);

            fold.setItemMeta(fm);
        }

        ItemStack checkCall = new ItemStack(Material.YELLOW_WOOL, 1);
        ItemMeta cm = checkCall.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.YELLOW + "CHECK/CALL");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "CHECK: Weiterreichen ohne Einsatz");
            lore.add(ChatColor.GRAY + "CALL: Den aktuellen Einsatz mitgehen");
            cm.setLore(lore);

            checkCall.setItemMeta(cm);
        }

        ItemStack betRaise = new ItemStack(Material.GREEN_WOOL, 1);
        ItemMeta bm = betRaise.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(ChatColor.GREEN + "BET/RAISE");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "BET: Einen neuen Einsatz setzen");
            lore.add(ChatColor.GRAY + "RAISE: Den aktuellen Einsatz erhöhen");
            bm.setLore(lore);

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

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Dieser Spieler ist aus der Runde ausgestiegen");
            meta.setLore(lore);

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

            // Zusätzliche Informationen als Lore
            if (potAmount > 0) {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Gesamtwert aller Einsätze in dieser Runde");
                lore.add(ChatColor.GOLD + "Der Gewinner erhält " + potAmount + " Chips");
                meta.setLore(lore);
            }

            potItem.setItemMeta(meta);
        }

        // Pot in Slot 31 anzeigen (Mitte des Inventars)
        gameInventory.setItem(31, potItem);
    }

    /**
     * Aktualisiert die Anzeige des aktuellen Einsatzes im Bet-Slot.
     *
     * @param player Der Spieler, dessen Einsatz angezeigt werden soll
     * @param smallBlindAmount Betrag des Small Blinds
     * @param bigBlindAmount Betrag des Big Blinds
     */
    public void updateBetDisplay(PokerPlayer player, int smallBlindAmount, int bigBlindAmount) {
        if (gameInventory == null) return;

        int betSlot = player.getBetSlot();
        int currentBet = player.getCurrentBet();

        // Wenn Spieler bereits gefoldet hat, nichts tun
        if (player.isFolded()) {
            return;
        }

        if (currentBet == 0) {
            // Wenn kein Einsatz, leeren Slot zeigen
            gameInventory.setItem(betSlot, null);
            return;
        }

        // Bestimme Material basierend auf der Höhe des Einsatzes
        Material material;
        if (currentBet <= smallBlindAmount) {
            material = Material.IRON_NUGGET;  // Small Blind
        } else if (currentBet <= bigBlindAmount) {
            material = Material.GOLD_NUGGET;  // Big Blind
        } else {
            material = Material.GOLD_INGOT;   // Höhere Einsätze
        }

        // Anzahl der angezeigten Items (1-64), skaliert mit dem Einsatz
        int amount = Math.min(64, Math.max(1, currentBet / 10));

        // Bet-Anzeige aktualisieren
        ItemStack betItem = new ItemStack(material, amount);
        ItemMeta meta = betItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Einsatz: " + currentBet + " Chips");
            betItem.setItemMeta(meta);
        }

        gameInventory.setItem(betSlot, betItem);
    }

    /**
     * Gibt das aktuelle Spiel-Inventar zurück.
     */
    public Inventory getGameInventory() {
        return gameInventory;
    }
}