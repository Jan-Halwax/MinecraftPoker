package dev.halwax.minecraftPoker.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.halwax.minecraftPoker.Main;
import dev.halwax.minecraftPoker.gamestates.GameState;
import dev.halwax.minecraftPoker.gamestates.IngameState;
import dev.halwax.minecraftPoker.gamestates.LobbyState;
import dev.halwax.minecraftPoker.gamestates.PreLobbyState;

/**
 * Reagiert auf Spieler-Verbindungsereignisse (Join/Quit).
 */
public class PlayerConnectionListener implements Listener {

    private final Main plugin;

    public PlayerConnectionListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(Main.PREFIX + "§a" + player.getDisplayName() + " §7hat den Server betreten!");

        // Nachricht mit kurzer Verzögerung senden
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage(Main.PREFIX + "§7Willkommen auf dem Server!");

            // Unterschiedliche Nachricht je nach aktuellem GameState
            String gameStateClassName = plugin.getGameStateManager().getCurrentGameState().getClass().getSimpleName();
            switch (gameStateClassName) {
                case "PreLobbyState" -> {
                                    if (player.hasPermission("poker.create")) {
                                        player.sendMessage(Main.PREFIX + "§7Das Spiel hat noch nicht begonnen! " +
                                                "Du kannst mit §a/poker create §7 ein Spiel erstellen!");
                                    }
                                }
                case "LobbyState" ->
                        player.sendMessage(Main.PREFIX + "§7Das Spiel hat noch nicht begonnen! " +
                                "Du kannst mit §a/poker join §7 dem Spiel beitreten! " +
                                "§7(" + plugin.getPlayers().size() + "/" + LobbyState.MAX_PLAYERS + ")");
                case "IngameState" ->
                        player.sendMessage(Main.PREFIX + "§cDas Spiel hat bereits begonnen! " +
                                "Du kannst mit §a/poker spectate §c zuschauen!");
            }
        }, 1L);
    }

    @EventHandler
    public void handlePlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(Main.PREFIX + "§c" + player.getDisplayName() + " §7hat den Server verlassen!");

        // Spieler aus der Spielerliste entfernen
        if (plugin.getPlayers().contains(player)) {
            plugin.getPlayers().remove(player);

            // Wenn keine Spieler mehr da sind und wir im Lobby-State sind, zurück zu PreLobby
            if (plugin.getPlayers().isEmpty() &&
                    plugin.getGameStateManager().getCurrentGameState() instanceof LobbyState) {
                plugin.getGameStateManager().setGameState(GameState.PRELOBBY_STATE);
            }
        }
    }
}