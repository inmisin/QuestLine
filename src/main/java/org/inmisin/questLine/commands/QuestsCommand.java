package org.inmisin.questLine.commands;

import org.bukkit.entity.Player;
import org.inmisin.questLine.QuestManager;
import org.inmisin.questLine.model.PlayerData;
import org.inmisin.questLine.model.Quest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class QuestsCommand implements CommandExecutor {

    private final QuestManager questManager;

    public QuestsCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        PlayerData data = questManager.getPlayerData(player);
        sender.sendMessage(Component.text("--- QUEST LIST ---", NamedTextColor.GOLD));

        boolean found = false;
        for (Quest q : questManager.getAllQuests()) {

            // Eğer görevin bir ön koşulu varsa VE oyuncu bunu bitirmediyse GÖSTERME (Gizli görev)
            // Veya "KİLİTLİ" olarak gösterebilirsin. Ben gizlemeyi tercih ettim.
            if (q.getRequiredQuestId() != null && !data.isQuestCompleted(q.getRequiredQuestId())) {
                continue;
            }

            // Görev tamamlandıysa üstünü çiz veya yeşil yap
            if (data.isQuestCompleted(q.getId())) {
                sender.sendMessage(Component.text("✔ " + q.getTitle(), NamedTextColor.GREEN)
                        .append(Component.text(" (Finished)", NamedTextColor.GRAY)));
            }
            // Aktif görevse
            else if (q.getId().equals(data.getActiveQuestId())) {
                sender.sendMessage(Component.text("➤ " + q.getTitle(), NamedTextColor.YELLOW)
                        .append(Component.text(" (On going)", NamedTextColor.AQUA)));
            }
            // Yapılabilir durumdaysa
            else {
                sender.sendMessage(Component.text("- " + q.getTitle(), NamedTextColor.WHITE)
                        .append(Component.text(" (/quest start " + q.getId() + ")", NamedTextColor.GRAY)));
            }
            found = true;
        }

        if (!found) {
            sender.sendMessage(Component.text("There is no quest right now", NamedTextColor.GRAY));
        }

        return true;
    }
}
