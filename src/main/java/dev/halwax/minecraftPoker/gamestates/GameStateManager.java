package dev.halwax.minecraftPoker.gamestates;

import dev.halwax.minecraftPoker.Main;

/**
 * Verwaltet die verschiedenen GameStates des Spiels.
 */
public class GameStateManager {

    private final Main plugin;
    private final GameState[] gameStates;
    private GameState currentGameState;

    public GameStateManager(Main plugin) {
        this.plugin = plugin;
        gameStates = new GameState[3];

        // GameStates mit Plugin-Referenz initialisieren
        gameStates[GameState.PRELOBBY_STATE] = new PreLobbyState(plugin);
        gameStates[GameState.LOBBY_STATE] = new LobbyState(plugin);
        gameStates[GameState.INGAME_STATE] = new IngameState(plugin);
    }

    /**
     * Setzt den aktuellen GameState.
     *
     * @param gameStateID ID des GameStates
     */
    public void setGameState(int gameStateID) {
        if (currentGameState != null) {
            currentGameState.stop();
        }

        currentGameState = gameStates[gameStateID];
        currentGameState.start();
    }

    /**
     * Stoppt den aktuellen GameState.
     */
    public void stopCurrentGameState() {
        if (currentGameState != null) {
            currentGameState.stop();
        }
        currentGameState = null;
    }

    /**
     * Gibt den aktuellen GameState zurück.
     */
    public GameState getCurrentGameState() {
        return currentGameState;
    }
}