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
    private int bigBlindIndex;
    private boolean bigBlindHasOption;

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
        this.bigBlindIndex = 0;
        this.bigBlindHasOption = false;
    }

    /**
     * Setzt Small und Big Blind.
     */
    public void postBlinds() {
        if (players.size() < 2) return;

        // Small Blind ist der Spieler nach dem Dealer
        int sbIndex = (dealerIndex + 1) % players.size();
        // Big Blind ist der Spieler nach dem Small Blind
        bigBlindIndex = (sbIndex + 1) % players.size();

        PokerPlayer sb = players.get(sbIndex);
        PokerPlayer bb = players.get(bigBlindIndex);

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

        // Spieler nach dem Big Blind beginnt (UTG - Under The Gun)
        currentPlayerIndex = (bigBlindIndex + 1) % players.size();

        // In der ersten Setzrunde (pre-flop) hat der Big Blind das Recht auf eine Option am Ende
        if (isPreFlop()) {
            bigBlindHasOption = true;
        } else {
            bigBlindHasOption = false;
        }

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
            // Aktionsmöglichkeiten basierend auf Spielzustand bestimmen
            String actionText;

            if (currentBet == 0) {
                // Wenn noch kein Einsatz in dieser Runde: Check oder Bet
                actionText = ChatColor.GREEN + "CHECK" + ChatColor.GRAY + " | " +
                        ChatColor.YELLOW + "BET" + ChatColor.GRAY + " | " +
                        ChatColor.RED + "FOLD";
            } else if (currentBet == currentPlayer.getCurrentBet()) {
                // Wenn Spieler bereits den Höchsteinsatz hat: Check oder Raise
                actionText = ChatColor.GREEN + "CHECK" + ChatColor.GRAY + " | " +
                        ChatColor.YELLOW + "RAISE" + ChatColor.GRAY + " | " +
                        ChatColor.RED + "FOLD";
            } else {
                // Wenn es einen Einsatz gibt, den der Spieler noch nicht getätigt hat: Call oder Raise
                int toCall = currentBet - currentPlayer.getCurrentBet();
                actionText = ChatColor.GREEN + "CALL (" + toCall + ")" + ChatColor.GRAY + " | " +
                        ChatColor.YELLOW + "RAISE" + ChatColor.GRAY + " | " +
                        ChatColor.RED + "FOLD";
            }

            // Besondere Anzeige für Big Blind Option
            if (isPreFlop() && currentPlayerIndex == bigBlindIndex && bigBlindHasOption) {
                actionText += ChatColor.GOLD + " - BIG BLIND OPTION";
            }

            // ActionBar-Nachricht (über der Hotbar, auch bei geöffnetem Inventar sichtbar)
            player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                            ChatColor.GOLD + "DU BIST AM ZUG! " + actionText
                    )
            );

            // Aktuellen Spieler im Inventar markieren
            markCurrentPlayerInInventory(currentPlayer);

            // Sound abspielen (lauter und wiederholend für bessere Wahrnehmung)
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            // Einen zweiten Sound nach einer kurzen Verzögerung abspielen
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("MinecraftPoker"),
                    () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.8f),
                    5L
            );

            // Auch im Chat informieren (als Backup)
            player.sendMessage(ChatColor.GOLD + "▶ " + ChatColor.BOLD + "DU BIST AM ZUG! " + actionText);

            // Zusätzliche Info: Aktuelle Einsätze und Pot anzeigen
            StringBuilder betInfo = new StringBuilder(ChatColor.GRAY + "Pot: " + ChatColor.WHITE + pot + ChatColor.GRAY + " | Einsätze: ");

            // Einsätze aller aktiven Spieler anzeigen
            boolean firstPlayer = true;
            for (PokerPlayer pp : players) {
                if (pp.isFolded()) continue;

                Player p = Bukkit.getPlayer(pp.getUuid());
                if (p != null) {
                    if (!firstPlayer) betInfo.append(", ");
                    firstPlayer = false;

                    // Aktuellen Spieler hervorheben
                    if (pp.equals(currentPlayer)) {
                        betInfo.append(ChatColor.YELLOW).append(p.getName()).append(": ").append(pp.getCurrentBet());
                    } else {
                        betInfo.append(ChatColor.WHITE).append(p.getName()).append(": ").append(pp.getCurrentBet());
                    }
                }
            }

            player.sendMessage(betInfo.toString());

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
     * Markiert den aktuellen Spieler im Inventar durch Hervorhebung.
     */
    private void markCurrentPlayerInInventory(PokerPlayer currentPlayer) {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null) return;

        // Alle Hervorhebungen zurücksetzen
        resetPlayerHighlights();

        // Aktuelle Spielerchips durch glühendes Item hervorheben
        int chipSlot = currentPlayer.getChipSlot();
        ItemStack chipItem = inv.getItem(chipSlot);

        if (chipItem != null) {
            // Vorhandenes Item durch glühende Version ersetzen
            ItemStack glowingChips = new ItemStack(Material.GOLD_BLOCK, 1);
            ItemMeta meta = glowingChips.getItemMeta();
            if (meta != null && chipItem.getItemMeta() != null) {
                meta.setDisplayName(chipItem.getItemMeta().getDisplayName() + ChatColor.AQUA + " ⟸ AM ZUG!");
                // Glowing-Effekt hinzufügen
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                glowingChips.setItemMeta(meta);
            }
            inv.setItem(chipSlot, glowingChips);
        }
    }

    /**
     * Setzt alle Spieler-Hervorhebungen im Inventar zurück.
     */
    private void resetPlayerHighlights() {
        Inventory inv = uiManager.getGameInventory();
        if (inv == null) return;

        // Für jeden Spieler die Chips-Anzeige normal aktualisieren
        for (PokerPlayer pp : players) {
            uiManager.updateChipDisplay(pp, dealerIndex, players);
        }
    }

    /**
     * Behandelt eine Spieleraktion (FOLD, CHECK, CALL, BET, RAISE).
     */
    public void handlePlayerAction(PokerPlayer player, String action, int betAmount) {
        if (!bettingRoundActive || player.isFolded() || players.indexOf(player) != currentPlayerIndex) {
            return;
        }

        switch (action.toLowerCase()) {
            case "fold":
                foldPlayer(player);
                break;

            case "check":
                // Check ist nur möglich, wenn kein Einsatz getätigt wurde oder Spieler bereits den höchsten Einsatz hat
                if (currentBet == 0 || player.getCurrentBet() == currentBet) {
                    checkAction(player);
                } else {
                    Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                    if (bukkitPlayer != null) {
                        bukkitPlayer.sendMessage(ChatColor.RED + "Du kannst nicht checken, wenn es einen Einsatz gibt!");
                    }
                    return; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                }
                break;

            case "call":
                // Call ist nur möglich, wenn es einen Einsatz gibt, den der Spieler noch nicht beglichen hat
                if (currentBet > player.getCurrentBet()) {
                    callAction(player);
                } else {
                    Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                    if (bukkitPlayer != null) {
                        bukkitPlayer.sendMessage(ChatColor.RED + "Du kannst nicht callen, wenn es keinen Einsatz gibt oder du bereits den höchsten Einsatz hast!");
                    }
                    return; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                }
                break;

            case "bet":
                // Bet ist nur möglich, wenn noch kein Einsatz in dieser Runde getätigt wurde
                if (currentBet == 0) {
                    // Mindestbetrag für Bet = Big Blind
                    if (betAmount >= BIG_BLIND && player.getChips() >= betAmount) {
                        betAction(player, betAmount);
                    } else {
                        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                        if (bukkitPlayer != null) {
                            bukkitPlayer.sendMessage(ChatColor.RED + "Bet muss mindestens " + BIG_BLIND +
                                    " Chips betragen! Du hast " + player.getChips() + " Chips.");
                        }
                        return; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                    }
                } else {
                    Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                    if (bukkitPlayer != null) {
                        bukkitPlayer.sendMessage(ChatColor.RED + "Du kannst nicht betten, wenn bereits ein Einsatz getätigt wurde! Verwende Raise.");
                    }
                    return; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                }
                break;

            case "raise":
                // Raise ist nur möglich, wenn bereits ein Einsatz getätigt wurde
                if (currentBet > 0) {
                    // Mindestbetrag für Raise = aktueller Einsatz + mindestens Big Blind
                    int minRaise = currentBet + BIG_BLIND;
                    int totalAmount = betAmount + (currentBet - player.getCurrentBet()); // Betrag zum Callen + Raise

                    if (betAmount >= BIG_BLIND && player.getChips() >= totalAmount) {
                        raiseAction(player, betAmount);

                        // Wenn jemand erhöht, hat der Big Blind bereits seine Option gehabt
                        bigBlindHasOption = false;
                    } else {
                        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                        if (bukkitPlayer != null) {
                            bukkitPlayer.sendMessage(ChatColor.RED + "Raise muss mindestens " + BIG_BLIND +
                                    " Chips über dem aktuellen Einsatz sein! Du brauchst " +
                                    totalAmount + " Chips total.");
                        }
                        return; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                    }
                } else {
                    Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                    if (bukkitPlayer != null) {
                        bukkitPlayer.sendMessage(ChatColor.RED + "Du kannst nicht raisen, wenn noch kein Einsatz getätigt wurde! Verwende Bet.");
                    }
                    return; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                }
                break;

            default:
                Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                if (bukkitPlayer != null) {
                    bukkitPlayer.sendMessage(ChatColor.RED + "Ungültige Aktion! Erlaubte Aktionen: FOLD, CHECK, CALL, BET, RAISE");
                }
                return; // Keine Aktion ausgeführt, Spieler bleibt am Zug
        }

        // Bet-Slot mit aktuellen Einsatz des Spielers aktualisieren
        updateBetDisplay(player);

        // Pot-Anzeige aktualisieren
        uiManager.updatePotDisplay(pot);

        // Nächsten Spieler auffordern
        nextPlayer();
    }

    /**
     * Führt einen Check aus (weitergeben ohne Einsatz).
     */
    private void checkAction(PokerPlayer player) {
        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
        String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
        broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " checkt.");

        // Spieler kann checken ohne weitere Änderungen
        if (bukkitPlayer != null) {
            bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF, 1.0f, 1.0f);
        }
    }

    /**
     * Führt einen Bet aus (erster Einsatz in der Runde).
     */
    private void betAction(PokerPlayer player, int betAmount) {
        // Sicherstellen, dass der Spieler genug Chips hat
        if (player.getChips() < betAmount) {
            broadcaster.broadcastMessage(ChatColor.RED + "Nicht genug Chips für diesen Einsatz!");
            return;
        }

        // Chips abziehen
        player.setChips(player.getChips() - betAmount);

        // Bet setzen
        player.setCurrentBet(betAmount);

        // Aktuellen Höchsteinsatz aktualisieren
        currentBet = betAmount;

        // Pot erhöhen
        pot += betAmount;

        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
        String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
        broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " setzt " + betAmount + " Chips.");

        // Chips-Anzeige aktualisieren
        uiManager.updateChipDisplay(player, dealerIndex, players);

        // Sound abspielen
        if (bukkitPlayer != null) {
            bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        }
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

        // Prüfen ob der Spieler genug Chips hat
        if (player.getChips() < toCall) {
            // All-in Situation
            int allInAmount = player.getChips();
            player.setChips(0);
            player.setCurrentBet(player.getCurrentBet() + allInAmount);
            pot += allInAmount;

            Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " geht ALL-IN mit " + allInAmount + " Chips!");

            // Sound für All-In
            if (bukkitPlayer != null) {
                bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
                // Für alle anderen Spieler einen Alarm spielen
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(bukkitPlayer)) {
                        p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 0.8f, 1.0f);
                    }
                }
            }
        } else if (toCall > 0) {
            // Normaler Call
            player.setChips(player.getChips() - toCall);
            player.setCurrentBet(currentBet);
            pot += toCall;

            Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " callt " + toCall + " Chips.");

            // Call-Sound
            if (bukkitPlayer != null) {
                bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
        } else {
            // Wenn nichts zu callen ist, dann ist es ein Check
            checkAction(player);
            return;
        }

        // Chips-Anzeige aktualisieren
        uiManager.updateChipDisplay(player, dealerIndex, players);
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

        // Prüfen, ob der Spieler genug Chips hat
        if (player.getChips() < totalAmount) {
            // All-in Situation
            int allInAmount = player.getChips();
            player.setChips(0);

            int newBet = player.getCurrentBet() + allInAmount;
            player.setCurrentBet(newBet);

            // Aktuellen Höchsteinsatz anpassen, wenn der All-In höher ist
            if (newBet > currentBet) {
                currentBet = newBet;
            }

            pot += allInAmount;

            Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " geht ALL-IN mit insgesamt " + newBet + " Chips!");

            // Sound für All-In
            if (bukkitPlayer != null) {
                bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
                // Für alle anderen Spieler einen Alarm spielen
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(bukkitPlayer)) {
                        p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 0.8f, 1.0f);
                    }
                }
            }
        } else if (raiseAmount >= BIG_BLIND) {
            // Normaler Raise
            player.setChips(player.getChips() - totalAmount);
            player.setCurrentBet(newTotalBet);

            // Höchsteinsatz aktualisieren
            currentBet = newTotalBet;

            // Pot erhöhen
            pot += totalAmount;

            Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " erhöht auf " + currentBet + " Chips total (+" + raiseAmount + ")");

            // Raise-Sound - höherer Ton für Aufmerksamkeit
            if (bukkitPlayer != null) {
                bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.4f);
                // Zusätzlicher Sound für alle Spieler
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.0f);
                }
            }
        } else {
            broadcaster.broadcastMessage(ChatColor.RED + "Ungültiger Raise! (Min: " + BIG_BLIND + " Chips)");
            return;
        }

        // Chips-Anzeige aktualisieren
        uiManager.updateChipDisplay(player, dealerIndex, players);
    }

    /**
     * Wechselt zum nächsten Spieler.
     */
    private void nextPlayer() {
        // Speichern des vorherigen Spielerindex für Big Blind Option-Erkennung
        int previousPlayerIndex = currentPlayerIndex;

        // Nächsten aktiven Spieler finden
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();

            // Wenn wir einmal komplett herum sind und alle Spieler bis auf einen gefoldet haben
            if (getActivePlayerCount() == 1 && currentPlayerIndex == dealerIndex) {
                endRoundWithWinner();
                return;
            }
        } while (players.get(currentPlayerIndex).isFolded());

        // Setzrunde zu Ende?
        boolean roundCompleted = false;

        // Besondere Behandlung für Big Blind Option (nur in der Pre-Flop Phase)
        if (isPreFlop() && bigBlindHasOption && currentPlayerIndex == bigBlindIndex) {
            // Alle Spieler bis zum Big Blind durchgelaufen
            // Der Big Blind bekommt seine Option, wenn niemand erhöht hat
            broadcaster.broadcastMessage(ChatColor.YELLOW + "Big Blind hat eine Option (Check oder Raise)");
            bigBlindHasOption = false;  // Option wird jetzt genutzt
            promptPlayerAction();
            return;
        }

        // Prüfen, ob wir wieder am Anfang der Runde sind (alle haben gehandelt)
        roundCompleted = isFullRoundCompleted(previousPlayerIndex) && !bigBlindHasOption;

        if (roundCompleted) {
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
     * Prüft, ob eine vollständige Runde abgeschlossen ist.
     * Eine Runde ist abgeschlossen, wenn entweder:
     * 1. Alle aktiven Spieler den gleichen Einsatz haben oder all-in sind, oder
     * 2. Nur noch ein Spieler aktiv ist.
     */
    private boolean isFullRoundCompleted(int previousPlayerIndex) {
        // Wenn nur noch ein Spieler aktiv ist
        if (getActivePlayerCount() <= 1) {
            return true;
        }

        // Prüfen, ob alle aktiven Spieler den gleichen Einsatz getätigt haben oder all-in sind
        boolean allPlayersBetEqual = true;

        for (PokerPlayer pp : players) {
            if (!pp.isFolded() && pp.getCurrentBet() != currentBet && pp.getChips() > 0) {
                allPlayersBetEqual = false;
                break;
            }
        }

        // Haben wir eine komplette Runde gemacht? (wieder beim ersten Spieler angekommen)
        // Im Pre-Flop ist der erste Spieler "UTG" (Under the Gun), nach dem Big Blind
        // In den anderen Runden ist der erste Spieler der Small Blind (oder nächste aktive Spieler)
        int firstPositionIndex;
        if (isPreFlop()) {
            firstPositionIndex = (bigBlindIndex + 1) % players.size();
        } else {
            firstPositionIndex = (dealerIndex + 1) % players.size();
            // Überspringen von gefoldeten Spielern
            while (players.get(firstPositionIndex).isFolded()) {
                firstPositionIndex = (firstPositionIndex + 1) % players.size();
            }
        }

        boolean fullCircle = (previousPlayerIndex == firstPositionIndex - 1) ||
                (previousPlayerIndex == players.size() - 1 && firstPositionIndex == 0);

        return allPlayersBetEqual && fullCircle;
    }

    /**
     * Prüft, ob die aktuelle Setzrunde beendet ist.
     * Diese Methode prüft nur, ob alle Spieler gleiche Einsätze haben oder all-in sind.
     * Sie berücksichtigt nicht die Big Blind Option.
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

        // Wenn wir im Pre-Flop sind und der Big Blind noch seine Option hat
        if (isPreFlop() && bigBlindHasOption) {
            return false;
        }

        return true;
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

        // Starte eine neue Setzrunde
        startNewBettingRound();
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

        // Starte eine neue Setzrunde
        startNewBettingRound();
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

        // Nach dem Flop beginnt der nächste aktive Spieler nach dem Dealer (Small Blind Position)
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