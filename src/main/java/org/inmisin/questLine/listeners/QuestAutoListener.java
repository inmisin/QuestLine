package org.inmisin.questLine.listeners;

import org.inmisin.questLine.QuestManager;
import org.inmisin.questLine.QuestLine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class QuestAutoListener implements Listener {

    private final QuestManager questManager;
    private final Plugin plugin;

    public QuestAutoListener(QuestManager questManager, Plugin plugin) {
        this.questManager = questManager;
        this.plugin = plugin;
    }

    // Yardımcı metot: Gecikmeli kontrol
    // Olay tetiklendiğinde eşya henüz envantere girmemiş olabilir, bu yüzden 1 tick sonra kontrol ediyoruz.
    private void scheduleCheck(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                questManager.checkAutoCompletion(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            scheduleCheck(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleCheck(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Oyuncu oyuna girdiğinde de kontrol edelim
        scheduleCheck(event.getPlayer());
    }
}