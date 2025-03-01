package dev.halwax.minecraftPoker.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.halwax.minecraftPoker.game.card.Card;
import dev.halwax.minecraftPoker.game.card.Deck;
import dev.halwax.minecraftPoker.game.card.StandardDeck;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;
import dev.halwax.minecraftPoker.game.ui.GameUIManager;

/**
 * Implementierung eines Poker-Spiels.
 */
public class PokerGame implements Game {

    private final List<PokerPlayer> players;
    private final Deck deck;
    private final List<Card> communityCards;
    private final GameUIManager uiManager;
    private final GameMessageBroadcaster broadcaster;
    private PokerRoundManager roundManager;

    private boolean gameStarted;

    public PokerGame() {
        this.players = new ArrayList<>();
        this.deck = new StandardDeck();
        this.communityCards = new ArrayList<>();
        this.uiManager = new GameUIManager();
        this.broadcaster = new GameMessageBroadcaster(players);
        this.gameStarted = false;
    }

    @Override
    public boolean addPlayer(Player player, int holeCardSlot1, int holeCardSlot2, int chipSlot, int betSlot) {
        if (gameStarted) {
            player.sendMessage(ChatColor.RED + "Das Spiel hat bereits begonnen.");
            return false;
        }
        if (players.size() >= 5) {
            player.sendMessage(ChatColor.RED + "Es sind bereits 5 Spieler im Spiel.");
            return false;
        }

        PokerPlayer pp = new PokerPlayer(player.getUniqueId(), holeCardSlot1, holeCardSlot2, chipSlot, betSlot);
        players.add(pp);

        player.sendMessage(ChatColor.GREEN + "Du bist dem Poker-Spiel beigetreten!");
        return true;
    }

    @Override
    public void startGame() {
        if (players.size() < 2) {
            broadcaster.broadcastMessage(ChatColor.RED + "Mindestens 2 Spieler erforderlich!");
            return;
        }
        if (gameStarted) {
            broadcaster.broadcastMessage(ChatColor.RED + "Das Spiel läuft bereits.");
            return;
        }

        gameStarted = true;

        // Spieler-Chips initialisieren (1000 pro Spieler)
        for (PokerPlayer pp : players) {
            pp.setChips(1000);
            pp.setCurrentBet(0);
            pp.setFolded(false);
        }

        // Deck neu mischen
        deck.reset();

        // Spielinventar erstellen
        Inventory gameInventory = uiManager.createGameInventory();

        // Hole Cards austeilen und Chips anzeigen
        for (PokerPlayer pp : players) {
            // Zwei Karten ziehen und dem Spieler zuweisen
            Card card1 = deck.drawCard();
            Card card2 = deck.drawCard();
            pp.setFirstCard(card1);
            pp.setSecondCard(card2);

            // Karten im Inventar platzieren
            gameInventory.setItem(pp.getHoleCardSlot1(), uiManager.createCardItem(card1));
            gameInventory.setItem(pp.getHoleCardSlot2(), uiManager.createCardItem(card2));

            // Chip-Anzeige aktualisieren
            uiManager.updateChipDisplay(pp, 0, players);
        }

        // Community-Karten ziehen und verdeckt platzieren
        communityCards.clear();
        communityCards.addAll(deck.drawCards(5));

        // Verdeckte Karten platzieren
        for (int slot = 20; slot <= 24; slot++) {
            gameInventory.setItem(slot, uiManager.createHiddenCardItem());
        }

        // Woll-Buttons für alle Spieler
        for (PokerPlayer pp : players) {
            Player p = Bukkit.getPlayer(pp.getUuid());
            if (p != null && p.isOnline()) {
                uiManager.giveWoolButtonsToPlayer(p);
            }
        }

        // Allen Spielern das Inventar öffnen
        for (PokerPlayer pp : players) {
            Player p = Bukkit.getPlayer(pp.getUuid());
            if (p != null && p.isOnline()) {
                p.openInventory(gameInventory);
            }
        }

        // Initialer Pot: 0
        uiManager.updatePotDisplay(0);

        // Runden-Manager erstellen und Blinds setzen
        roundManager = new PokerRoundManager(players, communityCards, uiManager, broadcaster);
        roundManager.postBlinds();

        // Erste Setzrunde starten
        roundManager.startBettingRound();

        broadcaster.broadcastMessage(ChatColor.GREEN + "Spiel gestartet. Blinds wurden gesetzt!");
    }

    @Override
    public void stopGame() {
        if (!gameStarted) {
            broadcaster.broadcastMessage(ChatColor.RED + "Es läuft kein Spiel.");
            return;
        }

        broadcaster.broadcastMessage(ChatColor.RED + "Spiel wird beendet...");
        for (PokerPlayer pp : players) {
            Player p = Bukkit.getPlayer(pp.getUuid());
            if (p != null && p.isOnline()) {
                p.closeInventory();
                p.sendMessage(ChatColor.RED + "Poker-Spiel beendet!");
            }
        }
        players.clear();
        communityCards.clear();
        gameStarted = false;
    }

    @Override
    public boolean isGameStarted() {
        return gameStarted;
    }

    @Override
    public List<PokerPlayer> getPlayers() {
        return players;
    }

    @Override
    public int getPot() {
        return roundManager != null ? roundManager.getPot() : 0;
    }

    @Override
    public Inventory getGameInventory() {
        return uiManager.getGameInventory();
    }

    @Override
    public PokerPlayer getPlayerByUUID(UUID uuid) {
        for (PokerPlayer pp : players) {
            if (pp.getUuid().equals(uuid)) {
                return pp;
            }
        }
        return null;
    }

    @Override
    public void handlePlayerAction(PokerPlayer player, String action, int raiseAmount) {
        if (roundManager != null) {
            roundManager.handlePlayerAction(player, action, raiseAmount);
        }
    }

    /**
     * Aktualisiert die Anzeige des aktuellen Einsatzes im Bet-Slot.
     * Diese Methode leitet den Aufruf an den PokerRoundManager weiter.
     */
    private void updateBetDisplay(PokerPlayer player) {
        if (roundManager != null) {
            roundManager.updateBetDisplay(player);
        }
    }
}