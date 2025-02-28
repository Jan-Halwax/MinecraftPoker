package dev.halwax.minecraftPoker.commands;

import dev.halwax.minecraftPoker.Main;
import dev.halwax.minecraftPoker.gamestates.GameState;
import dev.halwax.minecraftPoker.gamestates.IngameState;
import dev.halwax.minecraftPoker.gamestates.LobbyState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCommand implements CommandExecutor {

    private Main plugin;

    public StartCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.PREFIX + "§cDieser Befehl kann nur von einem Spieler ausgeführt werden!");
            return false;
        }

        if (!player.hasPermission("poker.start")) {
            player.sendMessage(Main.NO_PERMISSION);
            return false;
        }

        if (args.length != 0) {
            player.sendMessage(Main.PREFIX + "§cVerwendung: /start");
            return false;
        }

        if (plugin.getPlayers().size() < LobbyState.MIN_PLAYERS) {
            player.sendMessage(Main.PREFIX + "§cEs müssen mindestens " + LobbyState.MIN_PLAYERS + " Spieler im Spiel sein!");
            return false;
        }

        if (!(plugin.getGameStateManager().getCurrentGameState() instanceof LobbyState)) {
            player.sendMessage(Main.PREFIX + "§cDas Spiel kann nur im LobbyState gestartet werden!");
            return false;
        }

        plugin.getGameStateManager().setGameState(GameState.INGAME_STATE);
        return true;

    }
}
