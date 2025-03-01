package dev.halwax.minecraftPoker.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.halwax.minecraftPoker.Main;
import dev.halwax.minecraftPoker.game.Game;
import dev.halwax.minecraftPoker.game.player.PokerPlayer;

/**
 * Reagiert auf Klicks in Inventaren, um Poker-Aktionen zu verarbeiten.
 */
public class PlayerInventoryListener implements Listener {

    private final Main plugin;
    private final Game game;

    public PlayerInventoryListener(Main plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (!game.isGameStarted()) {
            return;
        }

        // Prüfen, ob es ein Woll-Button ist
        if (clickedItem.getType() == Material.RED_WOOL ||
                clickedItem.getType() == Material.YELLOW_WOOL ||
                clickedItem.getType() == Material.GREEN_WOOL) {

            // Event abbrechen, damit der Button nicht bewegt wird
            event.setCancelled(true);

            // Spieler-Objekt finden
            PokerPlayer pp = game.getPlayerByUUID(player.getUniqueId());
            if (pp == null) {
                return;
            }

            // Aktion je nach Wollfarbe ausführen
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();

                if (displayName.equals(ChatColor.RED + "FOLD")) {
                    game.handlePlayerAction(pp, "fold", 0);
                }
                else if (displayName.equals(ChatColor.YELLOW + "CHECK/CALL")) {
                    game.handlePlayerAction(pp, "call", 0);
                }
                else if (displayName.equals(ChatColor.GREEN + "BET/RAISE")) {
                    // Für dieses einfache Beispiel ein fester Raise-Betrag von 20 Chips
                    int raiseAmount = 20;
                    game.handlePlayerAction(pp, "raise", raiseAmount);

                    // Spiele einen Raise-Sound für alle Spieler
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
                    }
                }
            }
        }

        // Wenn im Spiel-Inventar geklickt wurde, Klick abbrechen
        if (event.getInventory().equals(game.getGameInventory())) {
            event.setCancelled(true);
        }
    }
}