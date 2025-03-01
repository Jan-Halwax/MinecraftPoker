package dev.halwax.minecraftPoker.game.player;

import java.util.UUID;

import dev.halwax.minecraftPoker.game.card.Card;

/**
 * Implementierung eines Poker-Spielers mit Display-Informationen.
 */
public class PokerPlayer implements Player, PlayerDisplayInfo {

    private final UUID uuid;
    private int chips;
    private final int holeCardSlot1;
    private final int holeCardSlot2;
    private final int chipSlot;
    private final int betSlot;
    private boolean folded;
    private int currentBet;
    private Card firstCard;
    private Card secondCard;

    public PokerPlayer(UUID uuid, int holeCardSlot1, int holeCardSlot2, int chipSlot, int betSlot) {
        this.uuid = uuid;
        this.chips = 0; // wird beim StartGame() verteilt
        this.holeCardSlot1 = holeCardSlot1;
        this.holeCardSlot2 = holeCardSlot2;
        this.chipSlot = chipSlot;
        this.betSlot = betSlot;
        this.folded = false;
        this.currentBet = 0;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public int getChips() {
        return chips;
    }

    @Override
    public void setChips(int chips) {
        this.chips = chips;
    }

    @Override
    public int getHoleCardSlot1() {
        return holeCardSlot1;
    }

    @Override
    public int getHoleCardSlot2() {
        return holeCardSlot2;
    }

    @Override
    public int getChipSlot() {
        return chipSlot;
    }

    @Override
    public int getBetSlot() {
        return betSlot;
    }

    @Override
    public boolean isFolded() {
        return folded;
    }

    @Override
    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    @Override
    public int getCurrentBet() {
        return currentBet;
    }

    @Override
    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    @Override
    public Card getFirstCard() {
        return firstCard;
    }

    @Override
    public void setFirstCard(Card card) {
        this.firstCard = card;
    }

    @Override
    public Card getSecondCard() {
        return secondCard;
    }

    @Override
    public void setSecondCard(Card card) {
        this.secondCard = card;
    }
}