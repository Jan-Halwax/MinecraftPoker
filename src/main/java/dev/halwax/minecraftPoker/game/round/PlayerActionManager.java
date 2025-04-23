package dev.halwax.minecraftPoker.game.round;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.halwax.minecraftPoker.game.GameMessageBroadcaster;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;
import dev.halwax.minecraftPoker.game.ui.GameUIManager;

/**
 * Verwaltet die Spieleraktionen (FOLD, CHECK, CALL, BET, RAISE).
 * Implementiert das Single Responsibility Principle durch Fokus auf Spieleraktionen.
 */
public class PlayerActionManager {

    private static final int BIG_BLIND = 20;

    private final List<PokerPlayer> players;
    private final GameUIManager uiManager;
    private final GameMessageBroadcaster broadcaster;
    private final PokerRoundManager roundManager;

    public PlayerActionManager(List<PokerPlayer> players, GameUIManager uiManager,
                               GameMessageBroadcaster broadcaster, PokerRoundManager roundManager) {
        this.players = players;
        this.uiManager = uiManager;
        this.broadcaster = broadcaster;
        this.roundManager = roundManager;
    }

    /**
     * Fordert den aktuellen Spieler zur Aktion auf.
     */
    public void promptPlayerAction(int currentPlayerIndex, int currentBet, int bigBlindIndex,
                                   boolean bigBlindHasOption, boolean isPreFlop, int pot) {
        PokerPlayer currentPlayer = players.get(currentPlayerIndex);
        Player player = Bukkit.getPlayer(currentPlayer.getUuid());
        if (player == null || !player.isOnline()) return;

        // Aktionsmöglichkeiten basierend auf Spielzustand bestimmen
        String actionText = determineActionText(currentPlayer, currentBet, currentPlayerIndex,
                bigBlindIndex, bigBlindHasOption, isPreFlop);

        // ActionBar-Nachricht (über der Hotbar, auch bei geöffnetem Inventar sichtbar)
        player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        ChatColor.GOLD + "DU BIST AM ZUG! " + actionText
                )
        );

        // Aktuellen Spieler im Inventar markieren
        markCurrentPlayerInInventory(currentPlayer);

        // Sound abspielen
        playTurnSound(player);

        // Chat-Nachrichten senden
        sendTurnInfoMessages(player, currentPlayer, pot);
    }

    /**
     * Bestimmt den Aktionstext basierend auf dem Spielzustand.
     */
    private String determineActionText(PokerPlayer player, int currentBet, int currentPlayerIndex,
                                       int bigBlindIndex, boolean bigBlindHasOption, boolean isPreFlop) {
        String actionText;

        if (currentBet == 0) {
            // Wenn noch kein Einsatz in dieser Runde: Check oder Bet
            actionText = ChatColor.GREEN + "CHECK" + ChatColor.GRAY + " | " +
                    ChatColor.YELLOW + "BET" + ChatColor.GRAY + " | " +
                    ChatColor.RED + "FOLD";
        } else if (currentBet == player.getCurrentBet()) {
            // Wenn Spieler bereits den Höchsteinsatz hat: Check oder Raise
            actionText = ChatColor.GREEN + "CHECK" + ChatColor.GRAY + " | " +
                    ChatColor.YELLOW + "RAISE" + ChatColor.GRAY + " | " +
                    ChatColor.RED + "FOLD";
        } else {
            // Wenn es einen Einsatz gibt, den der Spieler noch nicht getätigt hat: Call oder Raise
            int toCall = currentBet - player.getCurrentBet();
            actionText = ChatColor.GREEN + "CALL (" + toCall + ")" + ChatColor.GRAY + " | " +
                    ChatColor.YELLOW + "RAISE" + ChatColor.GRAY + " | " +
                    ChatColor.RED + "FOLD";
        }

        // Besondere Anzeige für Big Blind Option
        if (isPreFlop && currentPlayerIndex == bigBlindIndex && bigBlindHasOption) {
            actionText += ChatColor.GOLD + " - BIG BLIND OPTION";
        }

        return actionText;
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
        // Für jeden Spieler die Chips-Anzeige normal aktualisieren
        for (PokerPlayer pp : players) {
            uiManager.updateChipDisplay(pp, roundManager.getDealerIndex(), players);
        }
    }

    /**
     * Spielt einen Sound für den aktuellen Spieler ab.
     */
    private void playTurnSound(Player player) {
        // Sound abspielen (lauter und wiederholend für bessere Wahrnehmung)
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        // Einen zweiten Sound nach einer kurzen Verzögerung abspielen
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("MinecraftPoker"),
                () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.8f),
                5L
        );
    }

    /**
     * Sendet Informationen über den aktuellen Zug an den Spieler und andere.
     */
    private void sendTurnInfoMessages(Player player, PokerPlayer currentPlayer, int pot) {
        // Auch im Chat informieren (als Backup)
        String actionText = determineActionText(currentPlayer, roundManager.getBettingManager().getCurrentBet(),
                players.indexOf(currentPlayer), roundManager.getBettingManager().getBigBlindIndex(),
                roundManager.getBettingManager().isBigBlindHasOption(),
                roundManager.getCardManager().isPreFlop());

        player.sendMessage(ChatColor.GOLD + "▶ " + ChatColor.BOLD + "DU BIST AM ZUG! " + actionText);

        // Zusätzliche Info: Aktuelle Einsätze und Pot anzeigen
        String betInfo = createBetInfoMessage(currentPlayer, pot);
        player.sendMessage(betInfo.toString());

        // Nachricht für andere Spieler
        notifyOtherPlayers(player, currentPlayer);
    }

    /**
     * Erstellt eine Information über aktuelle Einsätze und Pot.
     */
    private String createBetInfoMessage(PokerPlayer currentPlayer, int pot) {
        String betInfo = ChatColor.GRAY + "Pot: " + ChatColor.WHITE + pot +
                ChatColor.GRAY + " | Einsätze: ";

        // Einsätze aller aktiven Spieler anzeigen
        StringBuilder sb = new StringBuilder(betInfo);
        boolean firstPlayer = true;
        for (PokerPlayer pp : players) {
            if (pp.isFolded()) continue;

            Player p = Bukkit.getPlayer(pp.getUuid());
            if (p != null) {
                if (!firstPlayer) sb.append(", ");
                firstPlayer = false;

                // Aktuellen Spieler hervorheben
                if (pp.equals(currentPlayer)) {
                    sb.append(ChatColor.YELLOW).append(p.getName()).append(": ").append(pp.getCurrentBet());
                } else {
                    sb.append(ChatColor.WHITE).append(p.getName()).append(": ").append(pp.getCurrentBet());
                }
            }
        }

        return sb.toString();
    }

    /**
     * Benachrichtigt andere Spieler, dass ein bestimmter Spieler am Zug ist.
     */
    private void notifyOtherPlayers(Player currentBukkitPlayer, PokerPlayer currentPlayer) {
        for (PokerPlayer pp : players) {
            if (!pp.equals(currentPlayer)) {
                Player p = Bukkit.getPlayer(pp.getUuid());
                if (p != null && p.isOnline()) {
                    p.sendMessage(ChatColor.GRAY + currentBukkitPlayer.getName() + " ist jetzt am Zug.");
                }
            }
        }
    }

    /**
     * Behandelt eine Spieleraktion und gibt das Ergebnis zurück.
     *
     * @param player Der Spieler, der die Aktion ausführt
     * @param action Die Aktionsbezeichnung (fold, check, call, bet, raise)
     * @param betAmount Betrag für bet/raise (bei anderen Aktionen irrelevant)
     * @param currentBet Der aktuelle Höchsteinsatz
     * @return Ein PlayerActionResult mit Einsatz und Pot-Änderung oder null, wenn die Aktion ungültig war
     */
    public PlayerActionResult handlePlayerAction(PokerPlayer player, String action, int betAmount, int currentBet) {
        switch (action.toLowerCase()) {
            case "fold":
                return foldPlayer(player);

            case "check":
                // Check ist nur möglich, wenn kein Einsatz getätigt wurde oder Spieler bereits den höchsten Einsatz hat
                if (currentBet == 0 || player.getCurrentBet() == currentBet) {
                    return checkAction(player);
                } else {
                    Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                    if (bukkitPlayer != null) {
                        bukkitPlayer.sendMessage(ChatColor.RED + "Du kannst nicht checken, wenn es einen Einsatz gibt!");
                    }
                    return null; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                }

            case "call":
                // Call ist nur möglich, wenn es einen Einsatz gibt, den der Spieler noch nicht beglichen hat
                if (currentBet > player.getCurrentBet()) {
                    return callAction(player, currentBet);
                } else {
                    Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                    if (bukkitPlayer != null) {
                        bukkitPlayer.sendMessage(ChatColor.RED +
                                "Du kannst nicht callen, wenn es keinen Einsatz gibt oder du bereits den höchsten Einsatz hast!");
                    }
                    return null; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                }

            case "bet":
                // Bet ist nur möglich, wenn noch kein Einsatz in dieser Runde getätigt wurde
                if (currentBet == 0) {
                    // Mindestbetrag für Bet = Big Blind
                    if (betAmount >= BIG_BLIND && player.getChips() >= betAmount) {
                        return betAction(player, betAmount);
                    } else {
                        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                        if (bukkitPlayer != null) {
                            bukkitPlayer.sendMessage(ChatColor.RED + "Bet muss mindestens " + BIG_BLIND +
                                    " Chips betragen! Du hast " + player.getChips() + " Chips.");
                        }
                        return null; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                    }
                } else {
                    Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                    if (bukkitPlayer != null) {
                        bukkitPlayer.sendMessage(ChatColor.RED +
                                "Du kannst nicht betten, wenn bereits ein Einsatz getätigt wurde! Verwende Raise.");
                    }
                    return null; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                }

            case "raise":
                // Raise ist nur möglich, wenn bereits ein Einsatz getätigt wurde
                if (currentBet > 0) {
                    // Mindestbetrag für Raise = aktueller Einsatz + mindestens Big Blind
                    int totalAmount = betAmount + (currentBet - player.getCurrentBet()); // Betrag zum Callen + Raise

                    if (betAmount >= BIG_BLIND && player.getChips() >= totalAmount) {
                        return raiseAction(player, betAmount, currentBet);
                    } else {
                        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                        if (bukkitPlayer != null) {
                            bukkitPlayer.sendMessage(ChatColor.RED + "Raise muss mindestens " + BIG_BLIND +
                                    " Chips über dem aktuellen Einsatz sein! Du brauchst " +
                                    totalAmount + " Chips total.");
                        }
                        return null; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                    }
                } else {
                    Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                    if (bukkitPlayer != null) {
                        bukkitPlayer.sendMessage(ChatColor.RED +
                                "Du kannst nicht raisen, wenn noch kein Einsatz getätigt wurde! Verwende Bet.");
                    }
                    return null; // Keine Aktion ausgeführt, Spieler bleibt am Zug
                }

            default:
                Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
                if (bukkitPlayer != null) {
                    bukkitPlayer.sendMessage(ChatColor.RED +
                            "Ungültige Aktion! Erlaubte Aktionen: FOLD, CHECK, CALL, BET, RAISE");
                }
                return null; // Keine Aktion ausgeführt, Spieler bleibt am Zug
        }
    }

    /**
     * Führt einen Check aus (weitergeben ohne Einsatz).
     */
    private PlayerActionResult checkAction(PokerPlayer player) {
        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
        String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
        broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " checkt.");

        // Spieler kann checken ohne weitere Änderungen
        if (bukkitPlayer != null) {
            bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF, 1.0f, 1.0f);
        }

        // Bei Check ändert sich nichts am Einsatz und Pot
        return new PlayerActionResult(player.getCurrentBet(), 0);
    }

    /**
     * Führt einen Bet aus (erster Einsatz in der Runde).
     */
    private PlayerActionResult betAction(PokerPlayer player, int betAmount) {
        // Sicherstellen, dass der Spieler genug Chips hat
        if (player.getChips() < betAmount) {
            broadcaster.broadcastMessage(ChatColor.RED + "Nicht genug Chips für diesen Einsatz!");
            return null;
        }

        // Chips abziehen
        player.setChips(player.getChips() - betAmount);

        // Bet setzen
        player.setCurrentBet(betAmount);

        Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
        String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
        broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " setzt " + betAmount + " Chips.");

        // Chips-Anzeige aktualisieren
        uiManager.updateChipDisplay(player, roundManager.getDealerIndex(), players);

        // Sound abspielen
        if (bukkitPlayer != null) {
            bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        }

        // Neuer Einsatz und Pot-Erhöhung zurückgeben
        return new PlayerActionResult(betAmount, betAmount);
    }

    /**
     * Setzt den Spieler auf gefoldet.
     */
    private PlayerActionResult foldPlayer(PokerPlayer player) {
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

        // Bei Fold ändert sich nichts am Pot, der aktuelle Einsatz bleibt bestehen
        return new PlayerActionResult(player.getCurrentBet(), 0);
    }

    /**
     * Führt einen Call des Spielers durch.
     */
    private PlayerActionResult callAction(PokerPlayer player, int currentBet) {
        int toCall = currentBet - player.getCurrentBet();
        int potIncrease = 0;
        int newPlayerBet = player.getCurrentBet();

        // Prüfen ob der Spieler genug Chips hat
        if (player.getChips() < toCall) {
            // All-in Situation
            int allInAmount = player.getChips();
            player.setChips(0);
            newPlayerBet = player.getCurrentBet() + allInAmount;
            player.setCurrentBet(newPlayerBet);
            potIncrease = allInAmount;

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
            newPlayerBet = currentBet;
            potIncrease = toCall;

            Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " callt " + toCall + " Chips.");

            // Call-Sound
            if (bukkitPlayer != null) {
                bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
        } else {
            // Wenn nichts zu callen ist, dann ist es ein Check
            return checkAction(player);
        }

        // Chips-Anzeige aktualisieren
        uiManager.updateChipDisplay(player, roundManager.getDealerIndex(), players);

        // Neuen Einsatz und Pot-Erhöhung zurückgeben
        return new PlayerActionResult(newPlayerBet, potIncrease);
    }

    /**
     * Führt einen Raise des Spielers durch.
     */
    private PlayerActionResult raiseAction(PokerPlayer player, int raiseAmount, int currentBet) {
        // Zuerst berechnen wieviel der Spieler callen muss
        int callAmount = currentBet - player.getCurrentBet();

        // Dann den gesamten Einsatz (Call + Raise) berechnen
        int totalAmount = callAmount + raiseAmount;

        // Neuer Gesamteinsatz des Spielers nach dem Raise
        int newTotalBet = player.getCurrentBet() + totalAmount;
        int potIncrease = 0;

        // Prüfen, ob der Spieler genug Chips hat
        if (player.getChips() < totalAmount) {
            // All-in Situation
            int allInAmount = player.getChips();
            player.setChips(0);

            int newBet = player.getCurrentBet() + allInAmount;
            player.setCurrentBet(newBet);
            potIncrease = allInAmount;

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

            // Neuen Einsatz und Pot-Erhöhung zurückgeben
            return new PlayerActionResult(newBet, potIncrease);

        } else if (raiseAmount >= BIG_BLIND) {
            // Normaler Raise
            player.setChips(player.getChips() - totalAmount);
            player.setCurrentBet(newTotalBet);
            potIncrease = totalAmount;

            Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            String playerName = (bukkitPlayer != null) ? bukkitPlayer.getName() : "Unbekannt";
            broadcaster.broadcastMessage(ChatColor.YELLOW + playerName + " erhöht auf " + newTotalBet +
                    " Chips total (+" + raiseAmount + ")");

            // Raise-Sound - höherer Ton für Aufmerksamkeit
            if (bukkitPlayer != null) {
                bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.4f);
                // Zusätzlicher Sound für alle Spieler
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.0f);
                }
            }

            // Neuen Einsatz und Pot-Erhöhung zurückgeben
            return new PlayerActionResult(newTotalBet, potIncrease);

        } else {
            broadcaster.broadcastMessage(ChatColor.RED + "Ungültiger Raise! (Min: " + BIG_BLIND + " Chips)");
            return null;
        }
    }

    /**
     * Erzeugt eine Nachricht für die Pot-Sammlung.
     */
    public String createPotCollectionMessage(int pot) {
        return ChatColor.GREEN + "Alle Einsätze wurden in den Pot gesammelt. " +
                "Aktueller Pot: " + pot + " Chips.";
    }

    /**
     * Erzeugt eine Nachricht für die Dealer-Rotation.
     */
    public String createDealerRotationMessage(int dealerIndex) {
        return ChatColor.GOLD + "Der Dealer-Button wurde an " +
                Bukkit.getPlayer(players.get(dealerIndex).getUuid()).getName() + " weitergegeben.";
    }

    /**
     * Erzeugt eine Nachricht für den Start einer Setzrunde.
     */
    public String createBettingRoundStartMessage() {
        return ChatColor.GOLD + "Die Setzrunde beginnt!";
    }
}