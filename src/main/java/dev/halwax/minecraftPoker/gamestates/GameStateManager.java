package dev.halwax.minecraftPoker.gamestates;

import dev.halwax.minecraftPoker.Main;

public class GameStateManager {

    private Main plugin;
    private GameState[] gameStates;
    private GameState currentGameState;

    public GameStateManager(Main plugin) {
        this.plugin = plugin;
        gameStates = new GameState[3];

        gameStates[GameState.PRELOBBY_STATE] = new PreLobbyState();
        gameStates[GameState.LOBBY_STATE] = new LobbyState();
        gameStates[GameState.INGAME_STATE] = new IngameState();
    }

    public void setGameState(int gameStateID) {
        if(currentGameState != null) {
            currentGameState.stop();
        }

        currentGameState = gameStates[gameStateID];
        currentGameState.start();
    }

    public void stopCurrentGameState() {
        if(currentGameState != null) {
            currentGameState.stop();
        }
        currentGameState = null;
    }


    public GameState getCurrentGameState() {
        return currentGameState;
    }
}
