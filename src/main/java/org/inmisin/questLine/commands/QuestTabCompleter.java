package org.inmisin.questLine.commands;

import org.inmisin.questLine.QuestManager;
import org.inmisin.questLine.model.Quest;
import org.inmisin.questLine.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestTabCompleter implements TabCompleter {

    private final QuestManager questManager;

    public QuestTabCompleter(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        // --- 1. ARGÜMAN (/quest ...) ---
        if (args.length == 1) {
            suggestions.add("start");
            suggestions.add("create");
            suggestions.add("quit");
            suggestions.add("setmsg");

            // Bilgi almak için ID'leri de göster (Burada hepsi görünebilir)
            for (Quest q : questManager.getAllQuests()) {
                suggestions.add(q.getId());
            }
            return filter(suggestions, args[0]);
        }

        String subCommand = args[0].toLowerCase();

        // --- START KOMUTU (FİLTRELİ) ---
        // Sadece "Başlayabileceği" görevleri göster
        if (subCommand.equals("start")) {
            if (!(sender instanceof Player player)) return Collections.emptyList();

            PlayerData data = questManager.getPlayerData(player);

            for (Quest q : questManager.getAllQuests()) {
                // 1. Zaten bu görev aktifse gösterme
                if (q.getId().equals(data.getActiveQuestId())) continue;

                // 2. Zaten tamamlandıysa gösterme
                if (data.isQuestCompleted(q.getId())) continue;

                // 3. Ön koşulu varsa ve tamamlanmadıysa gösterme (KİLİTLİ)
                if (q.getRequiredQuestId() != null) {
                    if (!data.isQuestCompleted(q.getRequiredQuestId())) {
                        continue;
                    }
                }

                // Tüm şartları sağladıysa listeye ekle
                suggestions.add(q.getId());
            }
            return filter(suggestions, args[1]);
        }

        // --- SETMSG (Admin için hepsi görünmeli) ---
        if (subCommand.equals("setmsg")) {
            for (Quest q : questManager.getAllQuests()) {
                suggestions.add(q.getId());
            }
            return filter(suggestions, args[1]);
        }

        // --- CREATE KOMUTU ---
        if (subCommand.equals("create")) {
            if (args.length == 2) {
                suggestions.add("<Title>");
                return suggestions;
            }

            String previousArg = args[args.length - 2];

            // NPC Seçimi sonrası -> Gereksinim Öner
            if (previousArg.equals("1") || previousArg.equals("0")) {
                suggestions.add("talk");
                suggestions.add("10:DIAMOND");
                suggestions.add("5:GOLD_INGOT");
                suggestions.add("64:COBBLESTONE");
                return filter(suggestions, args[args.length - 1]);
            }

            // Gereksinim sonrası -> Questline (ID) Öner
            if (previousArg.equalsIgnoreCase("talk") || previousArg.contains(":")) {
                suggestions.add("<previous_quest>");
                for (Quest q : questManager.getAllQuests()) {
                    suggestions.add(q.getId());
                }
                return filter(suggestions, args[args.length - 1]);
            }

            // Açıklama kısmı
            suggestions.add("<Desctiption>");
            suggestions.add("1");
            suggestions.add("0");

            return filter(suggestions, args[args.length - 1]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> suggestions, String input) {
        List<String> result = new ArrayList<>();
        String lowerInput = input.toLowerCase();
        for (String s : suggestions) {
            if (s.toLowerCase().contains(lowerInput)) {
                result.add(s);
            }
        }
        return result;
    }
}