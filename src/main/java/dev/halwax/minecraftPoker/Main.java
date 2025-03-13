package dev.halwax.minecraftPoker;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import dev.halwax.minecraftPoker.commands.PokerCommand;
import dev.halwax.minecraftPoker.game.Game;
import dev.halwax.minecraftPoker.game.GameFactory;
import dev.halwax.minecraftPoker.gamestates.GameState;
import dev.halwax.minecraftPoker.gamestates.GameStateManager;
import dev.halwax.minecraftPoker.listeners.PlayerConnectionListener;
import dev.halwax.minecraftPoker.listeners.PlayerInventoryListener;

public class Main extends JavaPlugin {

    public static final String PREFIX = "§7[§cPoker§7] §r",
            NO_PERMISSION = PREFIX + "§cDazu hast du keine Berechtigung!";

    private GameStateManager gameStateManager;
    private ArrayList<Player> players;
    private Game pokerGame;

    @Override
    public void onEnable() {
        getLogger().info("Plugin aktiviert!");

        // Spielerliste und GameStateManager initialisieren
        gameStateManager = new GameStateManager(this);
        players = new ArrayList<>();

        // Poker-Spiel erstellen
        GameFactory gameFactory = new GameFactory();
        pokerGame = gameFactory.createPokerGame();

        // Mit PreLobby starten
        gameStateManager.setGameState(GameState.PRELOBBY_STATE);

        // Listener und Kommandos registrieren
        init(Bukkit.getPluginManager());
    }

    private void init(PluginManager pluginManager) {
        // Listeners
        pluginManager.registerEvents(new PlayerConnectionListener(this), this);
        pluginManager.registerEvents(new PlayerInventoryListener(this, pokerGame), this);

        // Main command with subcommands
        PokerCommand pokerCommand = new PokerCommand(this);
        getCommand("poker").setExecutor(pokerCommand);
        getCommand("poker").setTabCompleter(pokerCommand);
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin deaktiviert!");

        // Laufendes Spiel beenden, falls vorhanden
        if (pokerGame != null && pokerGame.isGameStarted()) {
            pokerGame.stopGame();
        }
    }

    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public Game getPokerGame() {
        return pokerGame;
    }
}