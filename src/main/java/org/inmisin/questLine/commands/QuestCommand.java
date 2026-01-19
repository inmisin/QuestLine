package org.inmisin.questLine.commands;

import org.inmisin.questLine.QuestManager;
import org.inmisin.questLine.model.Quest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class QuestCommand implements CommandExecutor {

    private final QuestManager questManager;

    public QuestCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        // --- 1. DURUM: HİÇ ARGÜMAN YOKSA (/quest) ---
        if (args.length == 0) {
            Quest activeQuest = questManager.getActiveQuest(player);
            if (activeQuest != null) {
                player.sendMessage(Component.text("--- ACTIVE QUEST ---", NamedTextColor.AQUA));
                showQuestInfo(player, activeQuest);
            } else {
                player.sendMessage(Component.text("You don't have any active quest.", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("To take quest: /quest start <id>", NamedTextColor.AQUA)); // Güncellendi
                player.sendMessage(Component.text("To list all quests: /quests", NamedTextColor.AQUA));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ENGLISH);

        // --- 2. DURUM: GÖREV BAŞLATMA (/quest start <id>) ---
        if (subCommand.equals("start")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Usage: /quest start <id>", NamedTextColor.RED));
                return true;
            }

            String questId = args[1].toLowerCase(Locale.ENGLISH);

            if (!questManager.questExists(questId)) {
                player.sendMessage(Component.text("There is no quest named: " + questId, NamedTextColor.RED));
                return true;
            }

            Quest quest = questManager.getQuest(questId);
            questManager.startQuest(player, questId);

            // Başarılı olursa (startQuest içinde zaten kontrol yapılıyor ama burada da mesaj atabiliriz)
            if (questId.equals(questManager.getPlayerData(player).getActiveQuestId())) {
                player.sendMessage(Component.text("Quest started: " + quest.getTitle(), NamedTextColor.GREEN));
                showQuestInfo(player, quest);
            }
            return true;
        }

        // --- 3. DURUM: GÖREV OLUŞTURMA (/quest create ...) ---
        if (subCommand.equals("create")) {
            return handleCreate(player, args);
        }

        // --- 4. DURUM: GÖREV İPTALİ (/quest quit) ---
        if (subCommand.equals("quit")) {
            questManager.quitQuest(player);
            return true;
        }

        // --- 5. DURUM: MESAJ AYARLAMA (/quest setmsg ...) ---
        if (subCommand.equals("setmsg")) {
            return handleSetMsg(player, args);
        }

        // --- 6. DURUM: GÖREV BİLGİSİ GÖRME (/quest <id>) ---
        // Eğer yukarıdaki komutlardan biri değilse, oyuncu ID yazmış demektir.
        if (questManager.questExists(subCommand)) {
            Quest quest = questManager.getQuest(subCommand);
            showQuestInfo(player, quest);
            return true;
        }

        player.sendMessage(Component.text("unknown command or quest cannot be found.", NamedTextColor.RED));
        return true;
    }

    private boolean handleSetMsg(Player player, String[] args) {
        if (!player.hasPermission("simplequests.admin")) return true;
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /quest setmsg <id> <message>", NamedTextColor.RED));
            return true;
        }
        String qId = args[1].toLowerCase(Locale.ENGLISH);
        if(!questManager.questExists(qId)) return true;

        StringBuilder sb = new StringBuilder();
        for(int i=2; i<args.length; i++) sb.append(args[i]).append(" ");
        questManager.getQuest(qId).setCompletionMessage(sb.toString().trim());
        player.sendMessage(Component.text("Message set.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleCreate(Player player, String[] args) {
        // Yetki kontrolü
        if (!player.hasPermission("simplequests.admin")) {
            player.sendMessage(Component.text("You don't have an authority!", NamedTextColor.RED));
            return true;
        }

        // Argüman sayısı kontrolü
        // Format: /quest create <Baslik> <Aciklama...> <NPC_1/0> <Gereksinimler> [Oncelik_ID]
        // En az 5 parça olmalı: create + baslik + aciklama(1) + npc + req
        if (args.length < 5) {
            player.sendMessage(Component.text("Wrong usage!", NamedTextColor.RED));
            player.sendMessage(Component.text("Format: /quest create <Title> <Description> <NPC:1/0> <Requirements> [previous_quest]", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Exp: /quest create Test Ekmek_Getir 1 10:BREAD,10:DIAMOND (Optional: oncekigorev)", NamedTextColor.GRAY));
            return true;
        }

        // --- DEĞİŞKENLERİ AYIKLAMA (Sondan başa doğru okuma) ---
        // Çünkü açıklama kısmının kaç kelime olacağını bilmiyoruz.

        String lastArg = args[args.length - 1];
        String requiredQuestId = null;
        String reqString;
        String npcOptionStr;
        int descEndIndex;

        // Kontrol: Son argüman var olan bir Görev ID'si mi?
        // Eğer öyleyse, bu bir zincirleme görevdir.
        if (questManager.questExists(lastArg.toLowerCase(Locale.ENGLISH))) {
            requiredQuestId = lastArg.toLowerCase(Locale.ENGLISH);

            // Son argüman ID ise, diğerleri bir geriye kayar
            reqString = args[args.length - 2];
            npcOptionStr = args[args.length - 3];
            descEndIndex = args.length - 3;
        } else {
            // Son argüman ID değilse, standart formattır (zincir yok)
            reqString = args[args.length - 1];
            npcOptionStr = args[args.length - 2];
            descEndIndex = args.length - 2;
        }

        // --- BAŞLIK VE AÇIKLAMA ---
        String title = args[1]; // Başlık her zaman 2. sıradadır (index 1)

        StringBuilder descBuilder = new StringBuilder();
        for (int i = 2; i < descEndIndex; i++) {
            descBuilder.append(args[i]).append(" ");
        }
        String description = descBuilder.toString().trim();

        // --- NPC İŞLEMLERİ ---
        boolean useNpc = npcOptionStr.equals("1");
        UUID npcId = null;
        if (useNpc) {
            Entity target = player.getTargetEntity(10); // 10 blok menzil

            if (target == null) {
                player.sendMessage(Component.text("Error: You're not looking at any NPCs/Mobs!", NamedTextColor.RED));
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage(Component.text("BUG: You cannot make yourself a quest NPC.", NamedTextColor.RED));
                return true;
            }

            npcId = target.getUniqueId();
            player.sendMessage(Component.text("NPC selected: " + target.getType().name(), NamedTextColor.GREEN));
        }

        // --- GEREKSİNİM (REQUIREMENTS) PARSE İŞLEMİ ---
        Map<Material, Integer> requirements = new HashMap<>();

        if (!reqString.equalsIgnoreCase("talk")) {
            // Örnek format: 10:BREAD,1:GOLD_INGOT
            String[] splitReqs = reqString.split(","); // Virgülle ayır

            for (String req : splitReqs) {
                try {
                    String[] parts = req.split(":"); // İki nokta ile ayır (Adet:Malzeme)
                    int amount = Integer.parseInt(parts[0]);
                    String matName = parts[1].toUpperCase(Locale.ENGLISH);

                    Material mat = Material.matchMaterial(matName);
                    if (mat == null) {
                        player.sendMessage(Component.text("ERROR: Invalid item name: " + matName, NamedTextColor.RED));
                        return true;
                    }
                    requirements.put(mat, amount);

                } catch (Exception e) {
                    player.sendMessage(Component.text("ERROR: Requirement format is incorrect!", NamedTextColor.RED));
                    player.sendMessage(Component.text("Correct format: Quantity:ITEM_NAME(Exp: 10:BREAD)", NamedTextColor.GRAY));
                    return true;
                }
            }
        }

        // --- KAYDETME ---
        String id = title.toLowerCase(Locale.ENGLISH);

        // Eğer aynı isimde görev varsa uyar
        if (questManager.questExists(id)) {
            player.sendMessage(Component.text("A quest with this name already exists! Choose another title.", NamedTextColor.RED));
            return true;
        }

        // Manager'a gönder (Otomatik olarak dosyaya da kaydeder)
        questManager.createQuest(id, title, description, npcId, requirements, requiredQuestId);

        // --- GERİ BİLDİRİM ---
        player.sendMessage(Component.text("Task created successfully: " + title, NamedTextColor.GREEN));

        if (requiredQuestId != null) {
            player.sendMessage(Component.text("Note: This mission will unlock after the '" + requiredQuestId + "' quest.", NamedTextColor.YELLOW));
        }

        if (requirements.isEmpty()) {
            player.sendMessage(Component.text("Requirement: Just talk.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Requirements recorded.", NamedTextColor.GRAY));
        }

        return true;
    }

    private void showQuestInfo(Player player, Quest quest) {
        player.sendMessage(Component.text("-------------------------", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text(" Quest: ", NamedTextColor.GOLD).append(Component.text(quest.getTitle(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text(" Description: ", NamedTextColor.GOLD).append(Component.text(quest.getDescription(), NamedTextColor.GRAY)));

        if (quest.getRequirements() != null && !quest.getRequirements().isEmpty()) {
            player.sendMessage(Component.text(" REQUIREMENT: ", NamedTextColor.GOLD));
            for (Map.Entry<Material, Integer> entry : quest.getRequirements().entrySet()) {
                player.sendMessage(Component.text("  # " + entry.getValue() + " - " + entry.getKey().name(), NamedTextColor.AQUA));
            }
        } else {
            player.sendMessage(Component.text(" QUEST TYPE: ", NamedTextColor.GOLD).append(Component.text("Talk with NPC", NamedTextColor.GREEN)));
        }

        // Eğer görev alınmamışsa nasıl alınacağını göster
        if (!quest.getId().equals(questManager.getPlayerData(player).getActiveQuestId())) {
            player.sendMessage(Component.text("To start: /quest start " + quest.getId(), NamedTextColor.YELLOW)); // Güncellendi
        }

        player.sendMessage(Component.text("-------------------------", NamedTextColor.DARK_GRAY));
    }
}