package dev.halwax.minecraftPoker;

import dev.halwax.minecraftPoker.gamestates.GameState;
import dev.halwax.minecraftPoker.gamestates.GameStateManager;
import dev.halwax.minecraftPoker.listeners.PlayerConnectionListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;

public class Main extends JavaPlugin {

    public static final String PREFIX = "§7[§cPoker§7] §r",
                               NO_PERMISSION = PREFIX + "§cDazu hast du keine Berechtigung!";

    private GameStateManager gameStateManager;
    private ArrayList<Player> players;

    public void onEnable() {
        System.out.println("[Poker] Plugin aktiviert!");

        gameStateManager = new GameStateManager(this);
        players = new ArrayList<>();

        gameStateManager.setGameState(GameState.LOBBY_STATE);

        init(Bukkit.getPluginManager());
    }

    private void init(PluginManager pluginManager) {
        pluginManager.registerEvents(new PlayerConnectionListener(this), this);
        Objects.requireNonNull(getCommand("start")).setExecutor(new dev.halwax.minecraftPoker.commands.StartCommand(this));
    }

    public void onDisable() {
        System.out.println("[Poker] Plugin deaktiviert!");
    }

    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }
}
