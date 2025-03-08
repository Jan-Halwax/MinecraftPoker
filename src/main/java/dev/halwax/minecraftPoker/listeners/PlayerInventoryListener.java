package dev.halwax.minecraftPoker.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import dev.halwax.minecraftPoker.Main;
import dev.halwax.minecraftPoker.game.Game;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Reagiert auf Klicks in Inventaren, um Poker-Aktionen zu verarbeiten.
 */
public class PlayerInventoryListener implements Listener {

    private final Main plugin;
    private final Game game;
    private Inventory betInventory;
    private PokerPlayer currentBettingPlayer;
    private String currentAction;
    private int lastRaiseAmount = BIG_BLIND; // Standard-Erhöhungsbetrag

    // Map, um zu speichern, ob ein Spieler gerade das Poker-Inventar geschlossen hat
    private final Map<UUID, BukkitTask> rejoinReminderTasks = new HashMap<>();

    // Map zum Speichern, ob ein Spieler kürzlich das Inventar geschlossen hat (für Sneak-Rejoin)
    private final Map<UUID, Long> recentInventoryClose = new HashMap<>();
    private static final long SNEAK_REJOIN_TIMEOUT = 30000; // 30 Sekunden Zeitfenster für Sneak-Rejoin

    private static final int BIG_BLIND = 20;
    private static final int[] BET_OPTIONS = {BIG_BLIND, 2*BIG_BLIND, 3*BIG_BLIND, 5*BIG_BLIND, 10*BIG_BLIND};

    public PlayerInventoryListener(Main plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Abbrechen, wenn das Spiel nicht läuft
        if (!game.isGameStarted()) {
            return;
        }

        // Prüfen, ob wir im Bet-Menü sind
        if (event.getInventory().equals(betInventory)) {
            event.setCancelled(true);
            handleBetMenuClick(player, clickedItem);
            return;
        }

        // Prüfen, ob es ein Woll-Button ist
        if (clickedItem.getType() == Material.RED_WOOL ||
                clickedItem.getType() == Material.YELLOW_WOOL ||
                clickedItem.getType() == Material.GREEN_WOOL) {

            // Event abbrechen, damit der Button nicht bewegt wird
            event.setCancelled(true);

            // Spieler-Objekt finden
            PokerPlayer pp = game.getPlayerByUUID(player.getUniqueId());
            if (pp == null) {
                return;
            }

            // Aktion je nach Wollfarbe ausführen
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();

                if (displayName.equals(ChatColor.RED + "FOLD")) {
                    game.handlePlayerAction(pp, "fold", 0);
                }
                else if (displayName.equals(ChatColor.YELLOW + "CHECK/CALL")) {
                    // Je nach Spielzustand entweder Check oder Call
                    int currentBet = getCurrentBet();

                    if (currentBet == 0 || currentBet == pp.getCurrentBet()) {
                        // Wenn kein Einsatz oder Spieler hat bereits den Höchsteinsatz, dann Check
                        game.handlePlayerAction(pp, "check", 0);
                    } else {
                        // Sonst Call
                        game.handlePlayerAction(pp, "call", 0);
                    }
                }
                else if (displayName.equals(ChatColor.GREEN + "BET/RAISE")) {
                    // Je nach Spielzustand entweder Bet oder Raise
                    int currentBet = getCurrentBet();

                    if (currentBet == 0) {
                        // Wenn kein Einsatz, dann Bet
                        currentAction = "bet";
                        currentBettingPlayer = pp;
                        showBetOptions(player, pp);
                    } else {
                        // Sonst Raise
                        currentAction = "raise";
                        currentBettingPlayer = pp;
                        showRaiseOptions(player, pp);
                    }
                }
            }
        }

        // Wenn im Spiel-Inventar geklickt wurde, Klick abbrechen
        if (event.getInventory().equals(game.getGameInventory())) {
            event.setCancelled(true);
        }
    }

    /**
     * Erkennt Sneaking-Ereignisse für den Rejoin.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Nur reagieren, wenn der Spieler anfängt zu sneaken (nicht wenn er aufhört)
        if (!event.isSneaking()) {
            return;
        }

        // Prüfen, ob das Spiel läuft
        if (!game.isGameStarted()) {
            return;
        }

        // Prüfen, ob der Spieler im Spiel ist
        if (!plugin.getPlayers().contains(player)) {
            return;
        }

        // Prüfen, ob das Inventar bereits geöffnet ist
        if (player.getOpenInventory().getTopInventory() == game.getGameInventory()) {
            return;
        }

        // Prüfen, ob der Spieler kürzlich das Inventar geschlossen hat (innerhalb des Zeitfensters)
        Long closeTime = recentInventoryClose.get(player.getUniqueId());
        if (closeTime == null || System.currentTimeMillis() - closeTime > SNEAK_REJOIN_TIMEOUT) {
            return;
        }

        // Inventar wieder öffnen
        player.openInventory(game.getGameInventory());

        // Bestätigungsnachricht senden
        player.sendMessage(Main.PREFIX + ChatColor.GREEN + "Du bist durch Sneaken zum Poker-Spiel zurückgekehrt!");

        // Sound abspielen
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);

        // Zeitstempel aus der Map entfernen
        recentInventoryClose.remove(player.getUniqueId());
    }

    /**
     * Zeigt die Bet-Optionen in einem eigenen Inventar an.
     */
    private void showBetOptions(Player player, PokerPlayer pp) {
        // Inventar mit Bet-Optionen erstellen
        betInventory = Bukkit.createInventory(null, 9, ChatColor.GOLD + "Wähle deinen Einsatz");

        // Verfügbare Chips des Spielers
        int availableChips = pp.getChips();

        // Bet-Optionen hinzufügen
        for (int i = 0; i < BET_OPTIONS.length; i++) {
            int betAmount = BET_OPTIONS[i];

            // Nur Optionen anzeigen, die der Spieler sich leisten kann
            if (betAmount <= availableChips) {
                ItemStack betOption = createBetOption(betAmount);
                betInventory.setItem(i, betOption);
            }
        }

        // All-In Option hinzufügen
        if (availableChips > BIG_BLIND) {
            ItemStack allInOption = createAllInOption(availableChips);
            betInventory.setItem(8, allInOption);
        }

        // Abbrechen-Option
        ItemStack cancelOption = createCancelOption();
        betInventory.setItem(7, cancelOption);

        // Menü öffnen
        player.openInventory(betInventory);
    }

    /**
     * Zeigt die Raise-Optionen in einem eigenen Inventar an.
     */
    private void showRaiseOptions(Player player, PokerPlayer pp) {
        // Inventar mit Raise-Optionen erstellen
        betInventory = Bukkit.createInventory(null, 9, ChatColor.GOLD + "Wähle deinen Raise-Betrag");

        // Verfügbare Chips des Spielers
        int availableChips = pp.getChips();
        int currentBet = getCurrentBet();
        int playerCurrentBet = pp.getCurrentBet();
        int toCall = currentBet - playerCurrentBet;

        // Mindesterhöhung berechnen (basierend auf dem letzten Raise, mindestens aber BIG_BLIND)
        int minRaise = Math.max(lastRaiseAmount, BIG_BLIND);

        // Verschiedene Raise-Optionen hinzufügen
        int[] raiseMultipliers = {1, 2, 3, 4, 5};

        for (int i = 0; i < raiseMultipliers.length; i++) {
            int raiseAmount = minRaise * raiseMultipliers[i];
            int totalNeeded = toCall + raiseAmount;

            // Nur Optionen anzeigen, die der Spieler sich leisten kann
            if (totalNeeded <= availableChips) {
                ItemStack raiseOption = createRaiseOption(raiseAmount, currentBet + raiseAmount);
                betInventory.setItem(i, raiseOption);
            }
        }

        // All-In Option hinzufügen
        if (availableChips > (toCall + minRaise)) {
            ItemStack allInOption = createAllInOption(availableChips);
            betInventory.setItem(8, allInOption);
        }

        // Abbrechen-Option
        ItemStack cancelOption = createCancelOption();
        betInventory.setItem(7, cancelOption);

        // Menü öffnen
        player.openInventory(betInventory);
    }

    /**
     * Erstellt ein ItemStack für eine Bet-Option.
     */
    private ItemStack createBetOption(int amount) {
        ItemStack option = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = option.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Setze " + amount + " Chips");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Klicke, um " + amount + " Chips zu setzen");
            meta.setLore(lore);

            option.setItemMeta(meta);
        }
        return option;
    }

    /**
     * Erstellt ein ItemStack für eine Raise-Option.
     */
    private ItemStack createRaiseOption(int raiseAmount, int newTotal) {
        ItemStack option = new ItemStack(Material.DIAMOND, 1);
        ItemMeta meta = option.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Erhöhe um " + raiseAmount + " auf " + newTotal + " Chips");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Klicke, um den Einsatz um " + raiseAmount);
            lore.add(ChatColor.GRAY + "auf insgesamt " + newTotal + " Chips zu erhöhen");
            meta.setLore(lore);

            option.setItemMeta(meta);
        }
        return option;
    }

    /**
     * Erstellt ein ItemStack für die All-In Option.
     */
    private ItemStack createAllInOption(int amount) {
        ItemStack option = new ItemStack(Material.GOLD_BLOCK, 1);
        ItemMeta meta = option.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "ALL-IN: " + amount + " Chips");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.RED + "Klicke, um ALL-IN zu gehen");
            lore.add(ChatColor.GRAY + "Du setzt alle deine " + amount + " Chips");
            meta.setLore(lore);

            option.setItemMeta(meta);
        }
        return option;
    }

    /**
     * Erstellt ein ItemStack für die Abbrechen-Option.
     */
    private ItemStack createCancelOption() {
        ItemStack option = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = option.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Abbrechen");
            option.setItemMeta(meta);
        }
        return option;
    }

    /**
     * Behandelt Klicks im Bet-Menü.
     */
    private void handleBetMenuClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || currentBettingPlayer == null) return;

        // Abbrechen, wenn auf Barrier geklickt wurde
        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            player.openInventory(game.getGameInventory());
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String displayName = meta.getDisplayName();
        int amount;

        if (displayName.startsWith(ChatColor.GREEN + "Setze ")) {
            // Bet-Option wurde ausgewählt
            amount = extractNumberFromString(displayName);
            player.closeInventory();
            game.handlePlayerAction(currentBettingPlayer, "bet", amount);
            player.openInventory(game.getGameInventory());

            // Letzten Raise-Betrag speichern für Mindest-Re-Raises
            lastRaiseAmount = amount;
        }
        else if (displayName.startsWith(ChatColor.AQUA + "Erhöhe um ")) {
            // Raise-Option wurde ausgewählt
            amount = extractRaiseAmountFromString(displayName);
            player.closeInventory();
            game.handlePlayerAction(currentBettingPlayer, "raise", amount);
            player.openInventory(game.getGameInventory());

            // Letzten Raise-Betrag speichern für Mindest-Re-Raises
            lastRaiseAmount = amount;
        }
        else if (displayName.startsWith(ChatColor.GOLD + "ALL-IN")) {
            // All-In Option wurde ausgewählt
            amount = currentBettingPlayer.getChips();
            player.closeInventory();

            if (currentAction.equals("bet")) {
                game.handlePlayerAction(currentBettingPlayer, "bet", amount);
            } else {
                game.handlePlayerAction(currentBettingPlayer, "raise", amount);
            }

            player.openInventory(game.getGameInventory());
        }

        // Nach der Aktion Referenzen zurücksetzen
        currentBettingPlayer = null;
        currentAction = null;
    }

    /**
     * Extrahiert den Zahlenwert aus einem String wie "Setze 20 Chips".
     */
    private int extractNumberFromString(String text) {
        try {
            String[] parts = text.split(" ");
            for (String part : parts) {
                if (part.matches("\\d+")) {
                    return Integer.parseInt(part);
                }
            }
        } catch (Exception e) {
            return BIG_BLIND; // Fallback auf Standard-Blind
        }
        return BIG_BLIND;
    }

    /**
     * Extrahiert den Erhöhungsbetrag aus einem String wie "Erhöhe um 20 auf 40 Chips".
     */
    private int extractRaiseAmountFromString(String text) {
        try {
            String[] parts = text.split(" ");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("um") && i + 1 < parts.length) {
                    return Integer.parseInt(parts[i + 1]);
                }
            }
        } catch (Exception e) {
            return BIG_BLIND; // Fallback auf Standard-Blind
        }
        return BIG_BLIND;
    }

    /**
     * Hilfsmethode, um den aktuellen Höchsteinsatz zu ermitteln.
     */
    private int getCurrentBet() {
        // In einer echten Implementierung würden wir direkt auf den Wert zugreifen
        // Da wir keinen direkten Zugriff auf die interne Variable haben,
        // gehen wir hier den Umweg über die Spieler
        int maxBet = 0;
        for (PokerPlayer pp : game.getPlayers()) {
            if (pp.getCurrentBet() > maxBet) {
                maxBet = pp.getCurrentBet();
            }
        }
        return maxBet;
    }

    /**
     * Behandelt das Schließen des Inventars.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Wenn Bet-Menü geschlossen wird, öffne das Poker-Inventar wieder
        if (event.getInventory().equals(betInventory) && currentBettingPlayer != null &&
                player.getUniqueId().equals(currentBettingPlayer.getUuid())) {

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (game.isGameStarted() && plugin.getPlayers().contains(player)) {
                    player.openInventory(game.getGameInventory());
                }

                // Referenzen zurücksetzen
                currentBettingPlayer = null;
                currentAction = null;
            }, 1L);

            return;
        }

        // Wenn Poker-Inventar geschlossen wird und der Spieler im Spiel ist
        if (event.getInventory().equals(game.getGameInventory()) &&
                plugin.getPlayers().contains(player) &&
                game.isGameStarted()) {

            // Zeitstempel für Sneak-Rejoin speichern
            recentInventoryClose.put(player.getUniqueId(), System.currentTimeMillis());

            // Bestehenden Task abbrechen, falls vorhanden
            BukkitTask existingTask = rejoinReminderTasks.get(player.getUniqueId());
            if (existingTask != null) {
                existingTask.cancel();
            }

            // Nachricht mit Rejoin-Button anzeigen (nach kurzer Verzögerung)
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Nur die Nachricht anzeigen, wenn der Spieler immer noch im Spiel ist und das Inventar nicht schon wieder geöffnet hat
                if (plugin.getPlayers().contains(player) && game.isGameStarted() && player.getOpenInventory().getTopInventory() != game.getGameInventory()) {
                    sendRejoinMessage(player);
                }

                // Task aus der Map entfernen
                rejoinReminderTasks.remove(player.getUniqueId());
            }, 1L); // 1 Tick Verzögerung

            // Task in der Map speichern
            rejoinReminderTasks.put(player.getUniqueId(), task);
        }
    }

    /**
     * Sendet eine Chat-Nachricht mit einem klickbaren Rejoin-Button.
     */
    private void sendRejoinMessage(Player player) {
        // Linie erstellen
        TextComponent line = new TextComponent("\n" + ChatColor.DARK_GRAY + "---------------" +
                ChatColor.GRAY + "[ " + ChatColor.GOLD + "Poker" + ChatColor.GRAY + " ]" +
                ChatColor.DARK_GRAY + "---------------\n");

        // Frage erstellen
        TextComponent question = new TextComponent(ChatColor.YELLOW + "Möchtest du wieder zum Poker-Spiel zurückkehren?\n");

        // Rejoin-Button erstellen
        TextComponent rejoinButton = new TextComponent(ChatColor.GREEN + "[ Klicke hier zum Zurückkehren ]");
        rejoinButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/poker rejoin"));
        rejoinButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(ChatColor.GREEN + "Klicke, um zum Poker-Spiel zurückzukehren!").create()));

        // Sneak-Info erstellen
        TextComponent sneakInfo = new TextComponent("\n" + ChatColor.AQUA + "Tipp: " +
                ChatColor.WHITE + "Du kannst auch SHIFT drücken (sneaken), um zurückzukehren!");

        // Abschlusszeile erstellen
        TextComponent closingLine = new TextComponent("\n" + ChatColor.DARK_GRAY +
                "---------------------------------------------\n");

        // Nachricht zusammenbauen und senden
        player.spigot().sendMessage(line, question, rejoinButton, sneakInfo, closingLine);

        // Sound abspielen, um auf die Nachricht aufmerksam zu machen
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
    }
}