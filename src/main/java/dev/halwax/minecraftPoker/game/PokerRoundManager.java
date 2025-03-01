package dev.halwax.minecraftPoker.game;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.halwax.minecraftPoker.game.card.Card;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;
import dev.halwax.minecraftPoker.game.ui.GameUIManager;

/**
 * Verwaltet den Zustand und die Logik einer Poker-Runde.
 */
public class PokerRoundManager {

    private static final int SMALL_BLIND = 10;
    private static final int BIG_BLIND = 20;

    private final List<PokerPlayer> players;
    private final List<Card> communityCards;
    private final GameUIManager uiManager;
    private final GameMessageBroadcaster broadcaster;

    private int pot;
    private int dealerIndex;
    private int currentBet;
    private int currentPlayerIndex;
    private boolean bettingRoundActive;

    public PokerRoundManager(List<PokerPlayer> players, List<Card> communityCards,
                             GameUIManager uiManager, GameMessageBroadcaster broadcaster) {
        this.players = players;
        this.communityCards = communityCards;
        this.uiManager = uiManager;
        this.broadcaster = broadcaster;
        this.pot = 0;
        this.dealerIndex = 0;
        this.currentBet = 0;
        this.currentPlayerIndex = 0;
        this.bettingRoundActive = false;
    }

    /**
     * Setzt Small und Big Blind.
     */
    public void postBlinds() {
        if (players.size() < 2) return;

        // Small Blind ist der Spieler nach dem Dealer
        int sbIndex = (dealerIndex + 1) % players.size();
        // Big Blind ist der Spieler nach dem Small Blind
        int bbIndex = (sbIndex + 1) % players.size();

        PokerPlayer sb = players.get(sbIndex);
        PokerPlayer bb = players.get(bbIndex);

        int sbPaid = Math.min(sb.getChips(), SMALL_BLIND);
        int bbPaid = Math.min(bb.getChips(), BIG_BLIND);

        // Chips abziehen
        sb.setChips(sb.getChips() - sbPaid);
        bb.setChips(bb.getChips() - bbPaid);

        // Aktuellen Einsatz setzen
        sb.setCurrentBet(sbPaid);
        bb.setCurrentBet(bbPaid);

        // Pot erhöhen
        pot += sbPaid + bbPaid;

        // Chips aktualisieren
        uiManager.updateChipDisplay(sb, dealerIndex, players);
        uiManager.updateChipDisplay(bb, dealerIndex, players);

        // Bet-Anzeige aktualisieren
        updateBetDisplay(sb);
        updateBetDisplay(bb);

        // Pot-Anzeige aktualisieren
        uiManager.updatePotDisplay(pot);

        broadcaster.broadcastMessage(ChatColor.YELLOW + "[Blinds] " +
                Bukkit.getPlayer(sb.getUuid()).getName() + " zahlt Small Blind (" + sbPaid + "), " +
                Bukkit.getPlayer(bb.getUuid()).getName() + " zahlt Big Blind (" + bbPaid + ")");

        // Setze den aktuellen Höchsteinsatz auf den Big Blind
        currentBet = bbPaid;
    }

    /**
     * Rotiert den Dealer-Button zum nächsten Spieler.
     */
    public void rotateDealer() {
        dealerIndex = (dealerIndex + 1) % players.size();
        broadcaster.broadcastMessage(ChatColor.GOLD + "Der Dealer-Button wurde an " +
                Bukkit.getPlayer(players.get(dealerIndex).getUuid()).getName() + " weitergegeben.");

        // Chips aktualisieren (für alle Spieler)
        for (PokerPlayer pp : players) {
            uiManager.updateChipDisplay(pp, dealerIndex, players);
        }

        // Alle vorherigen Einsätze zurücksetzen
        for (PokerPlayer pp : players) {
            pp.setCurrentBet(0);
            pp.setFolded(false);

            // Bet-Anzeige leeren
            Inventory inv = uiManager.getGameInventory();
            if (inv != null) {
                inv.setItem(pp.getBetSlot(), null);
            }
        }

        // Blinds für die nächste Runde setzen
        postBlinds();
    }

    /**
     * Startet eine neue Setzrunde.
     */
    public void startBettingRound() {
        // Aktueller Bet bleibt bestehen (wurde in postBlinds auf BIG_BLIND gesetzt)
        bettingRoundActive = true;
        currentPlayerIndex = (dealerIndex + 3) % players.size(); // Spieler nach dem Big Blind

        broadcaster.broadcastMessage(ChatColor.GOLD + "Die Setzrunde beginnt!");
        promptPlayerAction();
    }

    /**
     * Fordert den aktuellen Spieler zur Aktion auf.
     */
    private void promptPlayerAction() {
        PokerPlayer currentPlayer = players.get(currentPlayerIndex);
        Player player = Bukkit.getPlayer(currentPlayer.getUuid());
        if (player != null && player.isOnline()) {
            // Nachricht senden
            player.sendMessage(ChatColor.GOLD + "Du bist am Zug! FOLD, CALL oder RAISE?");

            // Sound abspielen (NOTE_PLING = hoher Glockenton)
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

            // Nachricht für andere Spieler
            for (PokerPlayer pp : players) {
                if (!pp.equals(currentPlayer)) {
                    Player p = Bukkit.getPlayer(pp.getUuid());
                    if (p != null && p.isOnline()) {
                        p.sendMessage(ChatColor.GRAY + player.getName() + " ist jetzt am Zug.");
                    }
                }
            }
        }
    }

    /**
     * Behandelt eine Spieleraktion (FOLD, CALL, RAISE).
     */
    public void handlePlayerAction(PokerPlayer player, String action, int raiseAmount) {
        if (!bettingRoundActive || player.isFolded() || players.indexOf(player) != currentPlayerIndex) {
            return;
        }

        switch (action.toLowerCase()) {
            case "fold":
                foldPlayer(player);
                break;

            case "call":
                callAction(player);
                break;

            case "raise":
                raiseAction(player, raiseAmount);
                break;
        }

        // Bet-Slot mit aktuellen Einsatz des Spielers aktualisieren
        updateBetDisplay(player);

        // Pot-Anzeige aktualisieren
        uiManager.updatePotDisplay(pot);

        // Nächsten Spieler auffordern
        nextPlayer();
    }

    /**
     * Setzt den Spieler auf gefoldet.
     */
    private void foldPlayer(PokerPlayer player) {
        // Spieler als gefoldet markieren
        player.setFolded(true);

        // UI aktualisieren
        uiManager.markPlayerAsFolded(player);

        // Nachricht senden
        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
        String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
        broadcaster.broadcastMessage(ChatColor.RED + playerName + " hat gefoldet!");

        // Fold-Sound abspielen (tieferer Ton)
        if (bukkitPlayer != null) {
            bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }

    /**
     * Führt einen Call des Spielers durch.
     */
    private void callAction(PokerPlayer player) {
        int toCall = currentBet - player.getCurrentBet();
        if (player.getChips() >= toCall && toCall > 0) {
            player.setChips(player.getChips() - toCall);
            player.setCurrentBet(currentBet);
            pot += toCall;

            Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " callt " + toCall + " Chips.");

            // Chips-Anzeige aktualisieren
            uiManager.updateChipDisplay(player, dealerIndex, players);

            // Pot-Anzeige aktualisieren
            uiManager.updatePotDisplay(pot);
        } else if (toCall == 0) {
            // Wenn nichts zu callen ist, dann ist es ein Check
            Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " checkt.");
        } else {
            broadcaster.broadcastMessage(ChatColor.RED + "Nicht genug Chips zum Callen!");
        }
    }

    /**
     * Führt einen Raise des Spielers durch.
     */
    private void raiseAction(PokerPlayer player, int raiseAmount) {
        // Zuerst berechnen wieviel der Spieler callen muss
        int callAmount = currentBet - player.getCurrentBet();

        // Dann den gesamten Einsatz (Call + Raise) berechnen
        int totalAmount = callAmount + raiseAmount;

        // Neuer Gesamteinsatz des Spielers nach dem Raise
        int newTotalBet = player.getCurrentBet() + totalAmount;

        if (raiseAmount >= BIG_BLIND && player.getChips() >= totalAmount) {
            // Chips abziehen
            player.setChips(player.getChips() - totalAmount);

            // Aktuellen Einsatz des Spielers aktualisieren
            player.setCurrentBet(newTotalBet);

            // Höchsteinsatz aktualisieren
            currentBet = newTotalBet;

            // Pot erhöhen
            pot += totalAmount;

            Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " erhöht auf " + currentBet + " Chips total.");

            // Chips-Anzeige aktualisieren
            uiManager.updateChipDisplay(player, dealerIndex, players);

            // Pot-Anzeige aktualisieren
            uiManager.updatePotDisplay(pot);
        } else {
            broadcaster.broadcastMessage(ChatColor.RED + "Ungültiger Raise! (Min: " + BIG_BLIND + " Chips, verfügbar: " + player.getChips() + " Chips)");
        }
    }

    /**
     * Wechselt zum nächsten Spieler.
     */
    private void nextPlayer() {
        // Nächsten aktiven Spieler finden
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();

            // Wenn wir einmal komplett herum sind und alle Spieler bis auf einen gefoldet haben
            if (getActivePlayerCount() == 1 && currentPlayerIndex == dealerIndex) {
                endRoundWithWinner();
                return;
            }
        } while (players.get(currentPlayerIndex).isFolded());

        // Prüfen, ob die Setzrunde beendet ist
        if (isBettingRoundComplete()) {
            bettingRoundActive = false;

            // Alle Einsätze sammeln und zum Pot hinzufügen
            collectBetsIntoPot();

            // Je nach Spiel-Zustand unterschiedliche Karten aufdecken
            if (isPreFlop()) {
                broadcaster.broadcastMessage(ChatColor.GREEN + "Setzrunde abgeschlossen. Der Flop wird aufgedeckt!");
                revealFlop();
            } else if (isPostFlop()) {
                broadcaster.broadcastMessage(ChatColor.GREEN + "Setzrunde abgeschlossen. Der Turn wird aufgedeckt!");
                revealTurn();
            } else if (isPostTurn()) {
                broadcaster.broadcastMessage(ChatColor.GREEN + "Setzrunde abgeschlossen. Der River wird aufgedeckt!");
                revealRiver();
            } else if (isPostRiver()) {
                // Showdown - Hole Cards aufdecken und Gewinner ermitteln
                broadcaster.broadcastMessage(ChatColor.GREEN + "Finale Setzrunde abgeschlossen. Showdown!");
                determineWinner();
            }
        } else {
            promptPlayerAction();
        }
    }

    /**
     * Prüft, ob wir uns in der Pre-Flop Phase befinden (keine Community-Karten aufgedeckt).
     */
    private boolean isPreFlop() {
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
    private boolean isPostFlop() {
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
    private boolean isPostTurn() {
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
    private boolean isPostRiver() {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null) return false;

        // Prüfen, ob River aufgedeckt ist
        ItemStack riverItem = inv.getItem(24);
        return riverItem != null && riverItem.getItemMeta() != null &&
                !riverItem.getItemMeta().getDisplayName().equals("???");
    }

    /**
     * Bestimmt den Gewinner nach dem Showdown.
     * In dieser einfachen Version gewinnt der Spieler links vom Dealer.
     * Eine echte Implementierung würde die Poker-Hände vergleichen.
     */
    private void determineWinner() {
        // Finde den ersten nicht gefoldeten Spieler links vom Dealer
        PokerPlayer winner = null;
        int startIndex = (dealerIndex + 1) % players.size();
        int currentIndex = startIndex;

        do {
            PokerPlayer pp = players.get(currentIndex);
            if (!pp.isFolded()) {
                winner = pp;
                break;
            }
            currentIndex = (currentIndex + 1) % players.size();
        } while (currentIndex != startIndex);

        if (winner != null) {
            // Gib dem Gewinner den Pot
            winner.setChips(winner.getChips() + pot);

            Player bukkitPlayer = Bukkit.getPlayer(winner.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.GREEN + playerName + " gewinnt den Pot mit " +
                    pot + " Chips!");

            // Erfolgs-Sound für alle abspielen
            for (PokerPlayer pp : players) {
                Player p = Bukkit.getPlayer(pp.getUuid());
                if (p != null && p.isOnline()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }

            // Chips-Anzeige aktualisieren
            uiManager.updateChipDisplay(winner, dealerIndex, players);

            // Pot zurücksetzen
            pot = 0;
            uiManager.updatePotDisplay(pot);

            // Dealer rotieren und neue Runde starten
            rotateDealer();
        }
    }

    /**
     * Sammelt alle aktuellen Einsätze und fügt sie zum Pot hinzu.
     * Einsätze werden auf 0 zurückgesetzt, aber Anzeige bleibt erhalten.
     */
    private void collectBetsIntoPot() {
        for (PokerPlayer pp : players) {
            // Keine zusätzlichen Chips sammeln, da sie bereits zum Pot hinzugefügt wurden
            // Setze nur den aktuellen Einsatz auf 0
            pp.setCurrentBet(0);
        }

        broadcaster.broadcastMessage(ChatColor.GREEN + "Alle Einsätze wurden in den Pot gesammelt. " +
                "Aktueller Pot: " + pot + " Chips.");

        // Aktualisiere die Pot-Anzeige
        uiManager.updatePotDisplay(pot);
    }

    /**
     * Zählt die Anzahl der aktiven (nicht gefoldeten) Spieler.
     */
    private int getActivePlayerCount() {
        int count = 0;
        for (PokerPlayer pp : players) {
            if (!pp.isFolded()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Beendet die Runde mit einem Gewinner, wenn alle anderen gefoldet haben.
     */
    private void endRoundWithWinner() {
        // Finde den letzten aktiven Spieler
        PokerPlayer winner = null;
        for (PokerPlayer pp : players) {
            if (!pp.isFolded()) {
                winner = pp;
                break;
            }
        }

        if (winner != null) {
            // Gib dem Gewinner den Pot
            winner.setChips(winner.getChips() + pot);

            Player bukkitPlayer = Bukkit.getPlayer(winner.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.GREEN + playerName + " gewinnt den Pot mit " +
                    pot + " Chips, da alle anderen gefoldet haben!");

            // Erfolgs-Sound für alle abspielen
            for (PokerPlayer pp : players) {
                Player p = Bukkit.getPlayer(pp.getUuid());
                if (p != null && p.isOnline()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }

            // Chips-Anzeige aktualisieren
            uiManager.updateChipDisplay(winner, dealerIndex, players);

            // Pot zurücksetzen
            pot = 0;
            uiManager.updatePotDisplay(pot);

            // Dealer rotieren und neue Runde starten
            rotateDealer();
        }
    }

    /**
     * Prüft, ob die aktuelle Setzrunde beendet ist.
     */
    private boolean isBettingRoundComplete() {
        // Zähle aktive Spieler
        int activePlayers = getActivePlayerCount();

        if (activePlayers <= 1) {
            return true; // Nur noch ein Spieler übrig
        }

        // Für jeden aktiven Spieler prüfen, ob sein Einsatz dem aktuellen Höchsteinsatz entspricht
        // oder ob er all-in ist (keine Chips mehr hat)
        for (PokerPlayer pp : players) {
            if (!pp.isFolded() && pp.getCurrentBet() != currentBet && pp.getChips() > 0) {
                return false; // Mindestens ein aktiver Spieler muss noch handeln
            }
        }
        return true;
    }

    /**
     * Deckt den Flop auf (die ersten drei Community-Karten).
     */
    public void revealFlop() {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null || communityCards.size() < 3) return;

        // Slots 20..22 => communityCards(0..2)
        for (int i = 0; i < 3; i++) {
            int slot = 20 + i;
            Card card = communityCards.get(i);
            inv.setItem(slot, uiManager.createCardItem(card));
        }
        broadcaster.broadcastMessage(ChatColor.GOLD + "Flop aufgedeckt!");

        // Starte eine neue Setzrunde
        startNewBettingRound();
    }

    /**
     * Deckt den Turn auf (die vierte Community-Karte).
     */
    public void revealTurn() {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null || communityCards.size() < 4) return;

        int slot = 20 + 3; // =23
        Card card = communityCards.get(3);
        inv.setItem(slot, uiManager.createCardItem(card));
        broadcaster.broadcastMessage(ChatColor.GOLD + "Turn aufgedeckt!");
    }

    /**
     * Deckt den River auf (die fünfte Community-Karte).
     */
    public void revealRiver() {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null || communityCards.size() < 5) return;

        int slot = 20 + 4; // =24
        Card card = communityCards.get(4);
        inv.setItem(slot, uiManager.createCardItem(card));
        broadcaster.broadcastMessage(ChatColor.GOLD + "River aufgedeckt!");
    }

    /**
     * Gibt den aktuellen Pot zurück.
     */
    public int getPot() {
        return pot;
    }

    /**
     * Gibt den Index des aktuellen Dealers zurück.
     */
    public int getDealerIndex() {
        return dealerIndex;
    }

    /**
     * Aktualisiert die Anzeige des aktuellen Einsatzes im Bet-Slot.
     */
    public void updateBetDisplay(PokerPlayer player) {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null) return;

        int betSlot = player.getBetSlot();
        int currentBet = player.getCurrentBet();

        // Wenn Spieler bereits gefoldet hat, nichts tun
        if (player.isFolded()) {
            return;
        }

        if (currentBet == 0) {
            // Wenn kein Einsatz, leeren Slot zeigen
            inv.setItem(betSlot, null);
            return;
        }

        // Bestimme Material basierend auf der Höhe des Einsatzes
        Material material;
        if (currentBet <= SMALL_BLIND) {
            material = Material.IRON_NUGGET;  // Small Blind
        } else if (currentBet <= BIG_BLIND) {
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

        inv.setItem(betSlot, betItem);
    }

    /**
     * Startet eine neue Setzrunde nach einer Kartenaufdeckung.
     */
    public void startNewBettingRound() {
        // Aktuellen Einsatz zurücksetzen
        currentBet = 0;

        // Alle Spieler als noch nicht gesetzt markieren
        for (PokerPlayer pp : players) {
            // Aber gefoldete Spieler bleiben gefoldet
            if (!pp.isFolded()) {
                pp.setCurrentBet(0);

                // Bet-Anzeige leeren
                Inventory inv = uiManager.getGameInventory();
                if (inv != null) {
                    inv.setItem(pp.getBetSlot(), null);
                }
            }
        }

        // Setzrunde aktivieren
        bettingRoundActive = true;

        // Nächster Spieler nach dem Dealer beginnt (Small Blind Position)
        currentPlayerIndex = (dealerIndex + 1) % players.size();

        // Überspringen von gefoldeten Spielern
        while (players.get(currentPlayerIndex).isFolded()) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();

            // Wenn alle gefoldet haben (sollte nicht vorkommen)
            if (currentPlayerIndex == (dealerIndex + 1) % players.size()) {
                return;
            }
        }

        // Nachricht senden
        broadcaster.broadcastMessage(ChatColor.GOLD + "Eine neue Setzrunde beginnt!");

        // Spieleraktion anfordern
        promptPlayerAction();
    }
}