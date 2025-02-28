package dev.halwax.minecraftPoker.gamestates;

public class LobbyState extends GameState {

    public static final int MIN_PLAYERS = 1, // TODO: Change to 2
                            MAX_PLAYERS = 5;

    public void start() {
        System.out.println("LobbyState started!");
    }

    public void stop() {
        System.out.println("LobbyState stopped!");
    }
}
