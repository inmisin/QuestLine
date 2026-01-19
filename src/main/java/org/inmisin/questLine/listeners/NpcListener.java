package org.inmisin.questLine.listeners;

import org.inmisin.questLine.QuestManager;
import org.inmisin.questLine.model.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class NpcListener implements Listener {

    private final QuestManager questManager;

    public NpcListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    // NPC Koruma (Vurmayı engelle)
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (questManager.isNpcProtected(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // YENİ: NPC ile Etkileşim (Sağ Tıklama) - GÖREV TESLİMİ
    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        // Çift tetiklenmeyi önlemek için sadece ana el (sağ el) tıklamasını al
        if (event.getHand() != EquipmentSlot.HAND) return;

        // Tıklanan entity bizim korumalı NPC'miz mi?
        if (questManager.isNpcProtected(event.getRightClicked().getUniqueId())) {
            Player player = event.getPlayer();

            // Oyuncunun aktif bir görevi var mı?
            Quest activeQuestId = questManager.getActiveQuest(player);
            if (activeQuestId == null) return; // Görevi yoksa tepki verme

            Quest quest = questManager.getQuest(activeQuestId.getId());

            // Eğer bu NPC, o görevin teslim NPC'si ise
            if (quest.getNpcUuid() != null && quest.getNpcUuid().equals(event.getRightClicked().getUniqueId())) {
                // Görevi teslim etmeyi dene
                questManager.tryCompleteQuest(player, quest);
            }
        }
    }


}
