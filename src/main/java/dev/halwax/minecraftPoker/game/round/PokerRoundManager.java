package dev.halwax.minecraftPoker.game.round;

import java.util.List;

import org.bukkit.entity.Player;

import dev.halwax.minecraftPoker.game.GameMessageBroadcaster;
import dev.halwax.minecraftPoker.game.card.Card;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;
import dev.halwax.minecraftPoker.game.ui.GameUIManager;

/**
 * Verwaltet den Zustand und die Gesamtlogik einer Poker-Runde.
 * Delegiert spezifische Aufgaben an Spezialklassen (Strategy Pattern).
 * Diese Klasse dient als Facade für die komplexe Interaktion zwischen den Komponenten.
 */
public class PokerRoundManager {

    private final List<PokerPlayer> players;
    private final List<Card> communityCards;
    private final GameUIManager uiManager;
    private final GameMessageBroadcaster broadcaster;

    private final BettingRoundManager bettingManager;
    private final PlayerActionManager actionManager;
    private final CardRevealManager cardManager;
    private final BlindManager blindManager;
    private final WinnerDetermination winnerDetermination;

    private int pot;
    private int dealerIndex;

    public PokerRoundManager(List<PokerPlayer> players, List<Card> communityCards,
                             GameUIManager uiManager, GameMessageBroadcaster broadcaster) {
        this.players = players;
        this.communityCards = communityCards;
        this.uiManager = uiManager;
        this.broadcaster = broadcaster;
        this.pot = 0;
        this.dealerIndex = 0;

        // Spezialklassen initialisieren
        this.blindManager = new BlindManager(players, uiManager, broadcaster);
        this.bettingManager = new BettingRoundManager(players, uiManager, broadcaster);
        this.cardManager = new CardRevealManager(communityCards, uiManager, broadcaster);
        this.winnerDetermination = new WinnerDetermination(players, uiManager, broadcaster);
        this.actionManager = new PlayerActionManager(players, uiManager, broadcaster, this);
    }

    /**
     * Setzt Small und Big Blind.
     */
    public void postBlinds() {
        if (players.size() < 2) return;

        // Blind-Positionen berechnen
        int sbIndex = (dealerIndex + 1) % players.size();
        int bbIndex = (sbIndex + 1) % players.size();

        // Blinds setzen und Pot aktualisieren
        pot += blindManager.postBlinds(sbIndex, bbIndex, dealerIndex);

        // Dem BettingRoundManager den BigBlind-Index mitteilen
        bettingManager.setBigBlindIndex(bbIndex);

        // Aktuellen Höchsteinsatz auf den Big Blind setzen
        bettingManager.setCurrentBet(blindManager.getBigBlindAmount());
    }

    /**
     * Rotiert den Dealer-Button zum nächsten Spieler.
     */
    public void rotateDealer() {
        dealerIndex = (dealerIndex + 1) % players.size();
        broadcaster.broadcastMessage(actionManager.createDealerRotationMessage(dealerIndex));

        // Alle vorherigen Einsätze zurücksetzen
        resetBets();

        // Chips aktualisieren
        updateChipDisplayForAllPlayers();

        // Blinds für die nächste Runde setzen
        postBlinds();
    }

    /**
     * Setzt alle Wetten zurück und aktualisiert die Anzeige.
     */
    private void resetBets() {
        for (PokerPlayer pp : players) {
            pp.setCurrentBet(0);
            pp.setFolded(false);

            // Bet-Anzeige leeren
            updateBetDisplay(pp);
        }
    }

    /**
     * Aktualisiert die Chip-Anzeige für alle Spieler.
     */
    private void updateChipDisplayForAllPlayers() {
        for (PokerPlayer pp : players) {
            uiManager.updateChipDisplay(pp, dealerIndex, players);
        }
    }

    /**
     * Startet eine neue Setzrunde.
     */
    public void startBettingRound() {
        // Bestimme den ersten Spieler je nach Spielphase
        int firstPlayerIndex;

        if (cardManager.isPreFlop()) {
            // Pre-Flop: Spieler nach dem Big Blind beginnt (UTG - Under The Gun)
            firstPlayerIndex = (bettingManager.getBigBlindIndex() + 1) % players.size();
            bettingManager.setBigBlindHasOption(true);
        } else {
            // Post-Flop: Erster aktiver Spieler nach dem Dealer
            firstPlayerIndex = (dealerIndex + 1) % players.size();
            // Überspringen von gefoldeten Spielern
            while (players.get(firstPlayerIndex).isFolded()) {
                firstPlayerIndex = (firstPlayerIndex + 1) % players.size();
                if (firstPlayerIndex == (dealerIndex + 1) % players.size()) {
                    // Wenn alle gefoldet haben (sollte nicht vorkommen)
                    return;
                }
            }
            bettingManager.setBigBlindHasOption(false);
        }

        // Höchsteinsatz auf 0 setzen (außer im Pre-Flop)
        if (!cardManager.isPreFlop()) {
            bettingManager.setCurrentBet(0);
        }

        // Setzrunde starten
        bettingManager.startBettingRound(firstPlayerIndex);
        broadcaster.broadcastMessage(actionManager.createBettingRoundStartMessage());

        // Ersten Spieler zur Aktion auffordern
        promptNextPlayerAction();
    }

    /**
     * Fordert den nächsten Spieler zur Aktion auf.
     */
    public void promptNextPlayerAction() {
        actionManager.promptPlayerAction(
                bettingManager.getCurrentPlayerIndex(),
                bettingManager.getCurrentBet(),
                bettingManager.getBigBlindIndex(),
                bettingManager.isBigBlindHasOption(),
                cardManager.isPreFlop(),
                pot
        );
    }

    /**
     * Behandelt eine Spieleraktion (FOLD, CHECK, CALL, BET, RAISE).
     */
    public void handlePlayerAction(PokerPlayer player, String action, int betAmount) {
        if (!bettingManager.isBettingRoundActive() ||
                player.isFolded() ||
                players.indexOf(player) != bettingManager.getCurrentPlayerIndex()) {
            return;
        }

        // Aktion an den PlayerActionManager delegieren
        PlayerActionResult result = actionManager.handlePlayerAction(
                player,
                action,
                betAmount,
                bettingManager.getCurrentBet()
        );

        // Wenn eine Aktion durchgeführt wurde
        if (result != null) {
            // Einsatz aktualisieren, falls sich dieser verändert hat
            if (result.newBet() > bettingManager.getCurrentBet()) {
                bettingManager.setCurrentBet(result.newBet());
                // Big Blind verliert seine Option bei einem Raise
                if (cardManager.isPreFlop()) {
                    bettingManager.setBigBlindHasOption(false);
                }
            }

            // Pot aktualisieren
            pot += result.potIncrease();

            // Bet-Anzeigen aktualisieren
            updateBetDisplay(player);

            // Pot-Anzeige aktualisieren
            uiManager.updatePotDisplay(pot);

            // Zum nächsten Spieler wechseln
            processNextPlayer();
        }
    }

    /**
     * Verarbeitet den Übergang zum nächsten Spieler oder zur nächsten Runde.
     */
    private void processNextPlayer() {
        // Speichern des aktuellen Spielerindex
        int previousPlayerIndex = bettingManager.getCurrentPlayerIndex();

        // Zum nächsten aktiven Spieler wechseln
        int nextPlayerIndex = findNextActivePlayer(previousPlayerIndex);

        // Wenn nur noch ein Spieler aktiv ist
        if (getActivePlayerCount() == 1) {
            endRoundWithWinner();
            return;
        }

        // Prüfen, ob die Setzrunde beendet ist
        boolean roundCompleted = isRoundCompleted(previousPlayerIndex);

        if (roundCompleted) {
            // Setzrunde beenden
            bettingManager.setBettingRoundActive(false);

            // Einsätze zum Pot hinzufügen
            collectBetsIntoPot();

            // Je nach Spielphase fortfahren
            if (cardManager.isPreFlop()) {
                broadcaster.broadcastMessage(cardManager.createFlowRevealMessage());
                cardManager.revealFlop(uiManager.getGameInventory());
                startNewBettingRound();
            } else if (cardManager.isPostFlop()) {
                broadcaster.broadcastMessage(cardManager.createTurnRevealMessage());
                cardManager.revealTurn(uiManager.getGameInventory());
                startNewBettingRound();
            } else if (cardManager.isPostTurn()) {
                broadcaster.broadcastMessage(cardManager.createRiverRevealMessage());
                cardManager.revealRiver(uiManager.getGameInventory());
                startNewBettingRound();
            } else if (cardManager.isPostRiver()) {
                // Showdown - Hole Cards aufdecken und Gewinner ermitteln
                broadcaster.broadcastMessage(winnerDetermination.createShowdownMessage());
                determineWinner();
            }
        } else {
            // Nächster Spieler ist dran
            bettingManager.setCurrentPlayerIndex(nextPlayerIndex);
            promptNextPlayerAction();
        }
    }

    /**
     * Findet den nächsten aktiven Spieler.
     */
    private int findNextActivePlayer(int currentIndex) {
        int nextIndex = (currentIndex + 1) % players.size();

        // Suche den nächsten nicht gefoldeten Spieler
        while (players.get(nextIndex).isFolded()) {
            nextIndex = (nextIndex + 1) % players.size();

            // Wenn wir einmal komplett herum sind und wieder beim Ausgangspunkt landen
            if (nextIndex == currentIndex) {
                break;
            }
        }

        return nextIndex;
    }

    /**
     * Prüft, ob eine vollständige Runde abgeschlossen ist.
     */
    private boolean isRoundCompleted(int previousPlayerIndex) {
        // Wenn nur noch ein Spieler aktiv ist
        if (getActivePlayerCount() <= 1) {
            return true;
        }

        // Prüfen, ob alle aktiven Spieler den gleichen Einsatz getätigt haben oder all-in sind
        int currentBet = bettingManager.getCurrentBet();
        boolean allPlayersBetEqual = true;

        for (PokerPlayer pp : players) {
            if (!pp.isFolded() && pp.getCurrentBet() != currentBet && pp.getChips() > 0) {
                allPlayersBetEqual = false;
                break;
            }
        }

        // Prüfen, ob wir eine komplette Runde gemacht haben
        boolean fullCircle = bettingManager.isFullCircleCompleted(previousPlayerIndex);

        return allPlayersBetEqual && fullCircle &&
                !(cardManager.isPreFlop() &&
                        bettingManager.isBigBlindHasOption() &&
                        isNextPlayerBigBlind(previousPlayerIndex));
    }

    /**
     * Prüft, ob der nächste Spieler der Big Blind ist.
     */
    private boolean isNextPlayerBigBlind(int previousPlayerIndex) {
        int nextIndex = (previousPlayerIndex + 1) % players.size();
        return nextIndex == bettingManager.getBigBlindIndex();
    }

    /**
     * Bestimmt den Gewinner nach dem Showdown.
     */
    private void determineWinner() {
        // Gewinner ermitteln
        PokerPlayer winner = winnerDetermination.determineWinner(dealerIndex);

        if (winner != null) {
            // Gib dem Gewinner den Pot
            winner.setChips(winner.getChips() + pot);

            // Erfolgs-Sound für alle abspielen und Nachricht senden
            winnerDetermination.announceWinner(winner, pot);

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

        broadcaster.broadcastMessage(actionManager.createPotCollectionMessage(pot));

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
        // Gewinner ermitteln und Pot vergeben
        PokerPlayer winner = winnerDetermination.determineLastActivePlayer(players);

        if (winner != null) {
            // Gib dem Gewinner den Pot
            winner.setChips(winner.getChips() + pot);

            // Erfolgsbenachrichtigung
            winnerDetermination.announceFoldWinner(winner, pot);

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
     * Startet eine neue Setzrunde nach einer Kartenaufdeckung.
     */
    public void startNewBettingRound() {
        // Einsätze für alle Spieler zurücksetzen (außer gefoldete)
        for (PokerPlayer pp : players) {
            if (!pp.isFolded()) {
                pp.setCurrentBet(0);
                updateBetDisplay(pp);
            }
        }

        // Neue Setzrunde starten
        startBettingRound();
    }

    /**
     * Aktualisiert die Anzeige des aktuellen Einsatzes im Bet-Slot.
     */
    public void updateBetDisplay(PokerPlayer player) {
        uiManager.updateBetDisplay(player, blindManager.getSmallBlindAmount(), blindManager.getBigBlindAmount());
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
     * Erhöht den Pot um einen bestimmten Betrag.
     */
    public void increasePot(int amount) {
        pot += amount;
        uiManager.updatePotDisplay(pot);
    }

    /**
     * Gibt den CardRevealManager zurück.
     */
    public CardRevealManager getCardManager() {
        return cardManager;
    }

    /**
     * Gibt den BettingRoundManager zurück.
     */
    public BettingRoundManager getBettingManager() {
        return bettingManager;
    }
}