package dev.halwax.minecraftPoker.gamestates;

public abstract class GameState {

    public static final int PRELOBBY_STATE = 0,
                            LOBBY_STATE = 1,
                            INGAME_STATE = 2;

    public abstract void start();
    public abstract void stop();
}
