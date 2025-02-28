package dev.halwax.minecraftPoker.commands;

import dev.halwax.minecraftPoker.Main;
import dev.halwax.minecraftPoker.gamestates.GameState;
import dev.halwax.minecraftPoker.gamestates.LobbyState;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinCommand implements CommandExecutor {

    private Main plugin;

    public JoinCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.PREFIX + "§cDieser Befehl kann nur von einem Spieler ausgeführt werden!");
            return false;
        }

        if (!player.hasPermission("poker.join")) {
            player.sendMessage(Main.NO_PERMISSION);
            return false;
        }

        if (args.length != 0) {
            player.sendMessage(Main.PREFIX + "§cVerwendung: /join");
            return false;
        }

        if (!(plugin.getGameStateManager().getCurrentGameState() instanceof LobbyState)) {
            player.sendMessage(Main.PREFIX + "§cDem Spiel kann nur im LobbyState gejoint werden!");
            return false;
        }

        if (plugin.getPlayers().size() >= LobbyState.MAX_PLAYERS) {
            player.sendMessage(Main.PREFIX + "§cDas Spiel ist bereits voll! §7(" + plugin.getPlayers().size() + "/" + LobbyState.MAX_PLAYERS + ")");
            return false;
        }

        if (plugin.getPlayers().contains(player)) {
            player.sendMessage(Main.PREFIX + "§cDu bist bereits im Spiel!");
            return false;
        }

        int index = plugin.getPlayers().size();
        int hole1 = 0, hole2 = 0, chipSlot = 0, betSlot = 0;
        switch (index) {
            case 0:
                hole1 = 48; hole2 = 50; chipSlot = 49; betSlot = 40;
                break;
            case 1:
                hole1 = 18;  hole2 = 36; chipSlot = 27; betSlot = 28;
                break;
            case 2:
                hole1 = 1;  hole2 = 3;  chipSlot = 2;  betSlot = 11;
                break;
            case 3:
                hole1 = 5;  hole2 = 7;  chipSlot = 6;  betSlot = 15;
                break;
            case 4:
                hole1 = 26; hole2 = 44; chipSlot = 35; betSlot = 34;
                break;
        }

        plugin.getPlayers().add(player);
        player.sendMessage(Main.PREFIX + "§7Du bist dem Spiel beigetreten!");
        Bukkit.broadcastMessage(Main.PREFIX + "§a" + player.getDisplayName() + " §7hat das Spiel betreten! §7(" + plugin.getPlayers().size() + "/" + LobbyState.MAX_PLAYERS + ")");
        return true;
    }
}
