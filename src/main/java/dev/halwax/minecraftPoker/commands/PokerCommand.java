package dev.halwax.minecraftPoker.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev.halwax.minecraftPoker.Main;
import dev.halwax.minecraftPoker.gamestates.GameState;
import dev.halwax.minecraftPoker.gamestates.IngameState;
import dev.halwax.minecraftPoker.gamestates.LobbyState;
import dev.halwax.minecraftPoker.gamestates.PreLobbyState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Haupt-Command für alle Poker-bezogenen Aktionen.
 * Unterstützt verschiedene Unterkommandos: create, join, start, stop
 */
public class PokerCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final List<String> subCommands = Arrays.asList("create", "join", "start", "stop", "rejoin", "help");

    public PokerCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> handleCreateCommand(sender, args);
            case "join" -> handleJoinCommand(sender, args);
            case "start" -> handleStartCommand(sender, args);
            case "stop" -> handleStopCommand(sender, args);
            case "rejoin" -> handleRejoinCommand(sender, args);
            case "help" -> sendHelpMessage(sender);
            default -> {
                sender.sendMessage(Main.PREFIX + ChatColor.RED + "Unbekanntes Kommando. Verwende /poker help für Hilfe.");
                return false;
            }
        }

        return true;
    }

    /**
     * Verarbeitet den Befehl zum Erstellen eines neuen Spiels.
     */
    private void handleCreateCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.PREFIX + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden!");
            return;
        }

        if (!player.hasPermission("poker.create")) {
            player.sendMessage(Main.NO_PERMISSION);
            return;
        }

        // Prüfen, ob wir im PreLobby-Zustand sind
        if (plugin.getGameStateManager().getCurrentGameState() instanceof PreLobbyState) {
            // GameState auf LOBBY_STATE setzen
            plugin.getGameStateManager().setGameState(GameState.LOBBY_STATE);

            // Den Ersteller automatisch hinzufügen
            plugin.getServer().dispatchCommand(player, "poker join");

            player.sendMessage(Main.PREFIX + ChatColor.GREEN + "Du hast erfolgreich ein Poker-Spiel erstellt!");
        } else {
            player.sendMessage(Main.PREFIX + ChatColor.RED + "Es kann nur im PreLobbyState ein Spiel erstellt werden!");
        }
    }

    /**
     * Verarbeitet den Befehl zum Beitreten zu einem Spiel.
     */
    private void handleJoinCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.PREFIX + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden!");
            return;
        }

        if (!player.hasPermission("poker.join")) {
            player.sendMessage(Main.NO_PERMISSION);
            return;
        }

        if (!(plugin.getGameStateManager().getCurrentGameState() instanceof LobbyState)) {
            player.sendMessage(Main.PREFIX + ChatColor.RED + "Dem Spiel kann nur im LobbyState gejoint werden!");
            return;
        }

        if (plugin.getPlayers().size() >= LobbyState.MAX_PLAYERS) {
            player.sendMessage(Main.PREFIX + ChatColor.RED + "Das Spiel ist bereits voll! (" +
                    plugin.getPlayers().size() + "/" + LobbyState.MAX_PLAYERS + ")");
            return;
        }

        if (plugin.getPlayers().contains(player)) {
            player.sendMessage(Main.PREFIX + ChatColor.RED + "Du bist bereits im Spiel!");
            return;
        }

        // Slot-Positionen je nach Spieler-Index zuweisen
        int index = plugin.getPlayers().size();
        int hole1 = 0, hole2 = 0, chipSlot = 0, betSlot = 0;
        switch (index) {
            case 0 -> { hole1 = 48; hole2 = 50; chipSlot = 49; betSlot = 40; }
            case 1 -> { hole1 = 18; hole2 = 36; chipSlot = 27; betSlot = 28; }
            case 2 -> { hole1 = 1;  hole2 = 3;  chipSlot = 2;  betSlot = 11; }
            case 3 -> { hole1 = 5;  hole2 = 7;  chipSlot = 6;  betSlot = 15; }
            case 4 -> { hole1 = 26; hole2 = 44; chipSlot = 35; betSlot = 34; }
        }

        // Spieler zur Spielerliste hinzufügen
        plugin.getPlayers().add(player);

        // Auch zum eigentlichen Poker-Spiel hinzufügen
        plugin.getPokerGame().addPlayer(player, hole1, hole2, chipSlot, betSlot);

        player.sendMessage(Main.PREFIX + ChatColor.GREEN + "Du bist dem Spiel beigetreten!");
        Bukkit.broadcastMessage(Main.PREFIX + ChatColor.GREEN + player.getDisplayName() + ChatColor.GRAY +
                " hat das Spiel betreten! (" + plugin.getPlayers().size() + "/" + LobbyState.MAX_PLAYERS + ")");
    }

    /**
     * Verarbeitet den Befehl zum Starten eines Spiels.
     */
    private void handleStartCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.PREFIX + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden!");
            return;
        }

        if (!player.hasPermission("poker.start")) {
            player.sendMessage(Main.NO_PERMISSION);
            return;
        }

        if (plugin.getPlayers().size() < LobbyState.MIN_PLAYERS) {
            player.sendMessage(Main.PREFIX + ChatColor.RED + "Es müssen mindestens " +
                    LobbyState.MIN_PLAYERS + " Spieler im Spiel sein!");
            return;
        }

        if (!(plugin.getGameStateManager().getCurrentGameState() instanceof LobbyState)) {
            player.sendMessage(Main.PREFIX + ChatColor.RED + "Das Spiel kann nur im LobbyState gestartet werden!");
            return;
        }

        // GameState auf INGAME_STATE setzen
        plugin.getGameStateManager().setGameState(GameState.INGAME_STATE);

        // Das eigentliche Poker-Spiel starten
        plugin.getPokerGame().startGame();

        player.sendMessage(Main.PREFIX + ChatColor.GREEN + "Du hast das Poker-Spiel gestartet!");
    }

    /**
     * Verarbeitet den Befehl zum Stoppen eines Spiels.
     */
    private void handleStopCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.PREFIX + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden!");
            return;
        }

        if (!player.hasPermission("poker.stop")) {
            player.sendMessage(Main.NO_PERMISSION);
            return;
        }

        if (!plugin.getPokerGame().isGameStarted()) {
            player.sendMessage(Main.PREFIX + ChatColor.RED + "Es läuft kein Spiel, das gestoppt werden könnte!");
            return;
        }

        // Spiel stoppen
        plugin.getPokerGame().stopGame();

        // Zurück in den PreLobby-Zustand wechseln
        plugin.getGameStateManager().setGameState(GameState.PRELOBBY_STATE);

        // Spielerliste leeren
        plugin.getPlayers().clear();

        player.sendMessage(Main.PREFIX + ChatColor.GREEN + "Du hast das Poker-Spiel gestoppt!");
        Bukkit.broadcastMessage(Main.PREFIX + ChatColor.RED + "Das Poker-Spiel wurde von " +
                player.getDisplayName() + " beendet!");
    }

    /**
     * Verarbeitet den Befehl zum erneuten Beitreten zum Spiel (bei unbeabsichtigtem Inventar-Schließen).
     */
    private void handleRejoinCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.PREFIX + ChatColor.RED + "Dieser Befehl kann nur von einem Spieler ausgeführt werden!");
            return;
        }

        if (!player.hasPermission("poker.rejoin")) {
            player.sendMessage(Main.NO_PERMISSION);
            return;
        }

        // Prüfen, ob das Spiel läuft
        if (!plugin.getPokerGame().isGameStarted()) {
            player.sendMessage(Main.PREFIX + ChatColor.RED + "Es läuft aktuell kein Poker-Spiel!");
            return;
        }

        // Prüfen, ob der Spieler im Spiel ist
        if (!plugin.getPlayers().contains(player)) {
            player.sendMessage(Main.PREFIX + ChatColor.RED + "Du bist nicht im aktuellen Poker-Spiel!");
            return;
        }

        // Inventar wieder öffnen
        player.openInventory(plugin.getPokerGame().getGameInventory());
        player.sendMessage(Main.PREFIX + ChatColor.GREEN + "Du hast dich wieder mit dem Poker-Spiel verbunden!");
    }

    /**
     * Sendet eine Hilfe-Nachricht mit allen verfügbaren Kommandos.
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(Main.PREFIX + ChatColor.YELLOW + "=== Poker Hilfe ===");
        sender.sendMessage(Main.PREFIX + ChatColor.GOLD + "/poker create" + ChatColor.GRAY + " - Erstellt ein neues Poker-Spiel");
        sender.sendMessage(Main.PREFIX + ChatColor.GOLD + "/poker join" + ChatColor.GRAY + " - Tritt dem aktuellen Poker-Spiel bei");
        sender.sendMessage(Main.PREFIX + ChatColor.GOLD + "/poker start" + ChatColor.GRAY + " - Startet das Poker-Spiel");
        sender.sendMessage(Main.PREFIX + ChatColor.GOLD + "/poker stop" + ChatColor.GRAY + " - Stoppt das laufende Poker-Spiel");
        sender.sendMessage(Main.PREFIX + ChatColor.GOLD + "/poker rejoin" + ChatColor.GRAY + " - Verbindet dich wieder mit dem Poker-Spiel");
        sender.sendMessage(Main.PREFIX + ChatColor.GOLD + "/poker help" + ChatColor.GRAY + " - Zeigt diese Hilfe an");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}