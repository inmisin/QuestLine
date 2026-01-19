package org.inmisin.questLine;

import org.inmisin.questLine.model.PlayerData;
import org.inmisin.questLine.model.Quest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class QuestManager {

    private final JavaPlugin plugin;
    private final Map<String, Quest> quests = new HashMap<>();
    private final Set<UUID> protectedNpcUuids = new HashSet<>();
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    // Dosya Nesneleri
    private final File questsFile;
    private final File playersFile;

    public QuestManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // Plugin klasörünü oluştur (plugins/SimpleQuests)
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.questsFile = new File(plugin.getDataFolder(), "quests.yml");
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");

        // Başlangıçta verileri yükle
        loadData();
    }

    // --- VERİ KAYDETME VE YÜKLEME (PERSISTENCE) ---

    public void saveData() {
        // 1. GÖREVLERİ KAYDET
        YamlConfiguration questConfig = new YamlConfiguration();

        for (Quest q : quests.values()) {
            String path = q.getId();
            questConfig.set(path + ".title", q.getTitle());
            questConfig.set(path + ".description", q.getDescription());
            if (q.getNpcUuid() != null) {
                questConfig.set(path + ".npc", q.getNpcUuid().toString());
            }
            if (q.getRequiredQuestId() != null) {
                questConfig.set(path + ".requiredQuest", q.getRequiredQuestId());
            }
            if (q.getCompletionMessage() != null) {
                questConfig.set(path + ".completionMessage", q.getCompletionMessage());
            }

            // Gereksinimleri kaydet (Map<Material, Integer> -> String)
            if (q.getRequirements() != null && !q.getRequirements().isEmpty()) {
                Map<String, Integer> reqMap = new HashMap<>();
                q.getRequirements().forEach((mat, amount) -> reqMap.put(mat.name(), amount));
                questConfig.set(path + ".requirements", reqMap);
            }
        }

        // 2. OYUNCULARI KAYDET
        YamlConfiguration playerConfig = new YamlConfiguration();

        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            String path = entry.getKey().toString();
            PlayerData data = entry.getValue();

            if (data.getActiveQuestId() != null) {
                playerConfig.set(path + ".active", data.getActiveQuestId());
            }
            // Tamamlananları liste olarak kaydet
            playerConfig.set(path + ".completed", new ArrayList<>(data.getCompletedQuests())); // Set'i List'e çeviriyoruz
        }

        try {
            questConfig.save(questsFile);
            playerConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("An error occurred while saving data!");
            e.printStackTrace();
        }
    }

    public void loadData() {
        // 1. GÖREVLERİ YÜKLE
        if (questsFile.exists()) {
            YamlConfiguration questConfig = YamlConfiguration.loadConfiguration(questsFile);
            for (String key : questConfig.getKeys(false)) {
                try {
                    String title = questConfig.getString(key + ".title");
                    String desc = questConfig.getString(key + ".description");
                    String reqQuestId = questConfig.getString(key + ".requiredQuest");
                    String compMsg = questConfig.getString(key + ".completionMessage");

                    String npcStr = questConfig.getString(key + ".npc");
                    UUID npcUuid = (npcStr != null) ? UUID.fromString(npcStr) : null;

                    // Gereksinimleri yükle
                    Map<Material, Integer> requirements = new HashMap<>();
                    ConfigurationSection reqSection = questConfig.getConfigurationSection(key + ".requirements");
                    if (reqSection != null) {
                        for (String matName : reqSection.getKeys(false)) {
                            Material mat = Material.matchMaterial(matName);
                            int amt = reqSection.getInt(matName);
                            if (mat != null) requirements.put(mat, amt);
                        }
                    }

                    Quest q = new Quest(key, title, desc, npcUuid, requirements, reqQuestId);
                    q.setCompletionMessage(compMsg);

                    quests.put(key, q);
                    if (npcUuid != null) protectedNpcUuids.add(npcUuid);

                } catch (Exception e) {
                    plugin.getLogger().warning("Error loading task: " + key);
                }
            }
        }

        // 2. OYUNCULARI YÜKLE
        if (playersFile.exists()) {
            YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playersFile);
            for (String uuidStr : playerConfig.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    PlayerData data = new PlayerData();

                    String active = playerConfig.getString(uuidStr + ".active");
                    data.setActiveQuestId(active);

                    List<String> completed = playerConfig.getStringList(uuidStr + ".completed");
                    for (String c : completed) {
                        data.addCompletedQuest(c);
                    }

                    playerDataMap.put(uuid, data);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error loading player data: " + uuidStr);
                }
            }
        }
    }

    // --- BURADAN AŞAĞISI ESKİ METOTLARIN AYNISI ---

    public void createQuest(String id, String title, String description, UUID npcUuid, Map<Material, Integer> requirements, String requiredQuestId) {
        Quest newQuest = new Quest(id, title, description, npcUuid, requirements, requiredQuestId);
        quests.put(id, newQuest);

        if (npcUuid != null) {
            protectedNpcUuids.add(npcUuid);
            Entity npc = Bukkit.getEntity(npcUuid);
            if (npc != null) npc.setGlowing(true);
        }
        saveData(); // Oluşturunca hemen kaydet
    }

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData());
    }

    public Quest getQuest(String id) { return quests.get(id); }
    public Collection<Quest> getAllQuests() { return quests.values(); }
    public boolean questExists(String id) { return quests.containsKey(id); }
    public boolean isNpcProtected(UUID uuid) { return protectedNpcUuids.contains(uuid); }

    public void startQuest(Player player, String questId) {
        PlayerData data = getPlayerData(player);

        if (data.getActiveQuestId() != null) {
            player.sendMessage(Component.text("You already have an active duty! Finish that first.", NamedTextColor.RED));
            return;
        }

        Quest quest = getQuest(questId);
        if (quest.getRequiredQuestId() != null) {
            if (!data.isQuestCompleted(quest.getRequiredQuestId())) {
                player.sendMessage(Component.text("To get this quest you must complete the previous quest!", NamedTextColor.RED));
                return;
            }
        }

        if (data.isQuestCompleted(questId)) {
            player.sendMessage(Component.text("You've already completed this mission before.", NamedTextColor.YELLOW));
            return; // İstersen tekrar yapmayı engelle
        }

        data.setActiveQuestId(questId);
        saveData(); // Veri değişti, kaydet
    }

    public void quitQuest(Player player) {
        PlayerData data = getPlayerData(player);
        if (data.getActiveQuestId() == null) {
            player.sendMessage(Component.text("You don't have an active quest anyway!", NamedTextColor.RED));
            return;
        }
        String currentId = data.getActiveQuestId();
        Quest quest = quests.get(currentId);
        String title = (quest != null) ? quest.getTitle() : currentId;
        data.setActiveQuestId(null);
        player.sendMessage(Component.text("Quest canceled: " + title, NamedTextColor.RED));
        saveData(); // Veri değişti, kaydet
    }

    public Quest getActiveQuest(Player player) {
        String id = getPlayerData(player).getActiveQuestId();
        return id != null ? quests.get(id) : null;
    }

    public void checkAutoCompletion(Player player) {
        Quest activeQuest = getActiveQuest(player);
        if (activeQuest == null) return;
        if (activeQuest.getNpcUuid() != null) return;

        Map<Material, Integer> reqs = activeQuest.getRequirements();
        if (reqs == null || reqs.isEmpty()) return;

        boolean hasAll = true;
        for (Map.Entry<Material, Integer> entry : reqs.entrySet()) {
            if (!player.getInventory().containsAtLeast(new ItemStack(entry.getKey()), entry.getValue())) {
                hasAll = false;
                break;
            }
        }
        if (hasAll) {
            tryCompleteQuest(player, activeQuest);
        }
    }

    public void tryCompleteQuest(Player player, Quest quest) {
        Map<Material, Integer> reqs = quest.getRequirements();

        if (reqs != null && !reqs.isEmpty()) {
            for (Map.Entry<Material, Integer> entry : reqs.entrySet()) {
                if (!player.getInventory().containsAtLeast(new ItemStack(entry.getKey()), entry.getValue())) {
                    player.sendMessage(Component.text("Missing item: " + entry.getValue() + " - " + entry.getKey().name(), NamedTextColor.RED));
                    return;
                }
            }
            for (Map.Entry<Material, Integer> entry : reqs.entrySet()) {
                player.getInventory().removeItem(new ItemStack(entry.getKey(), entry.getValue()));
            }
        }

        finishQuest(player, quest);
    }

    private void finishQuest(Player player, Quest quest) {
        PlayerData data = getPlayerData(player);
        data.setActiveQuestId(null);
        data.addCompletedQuest(quest.getId());

        if (quest.getCompletionMessage() != null && !quest.getCompletionMessage().isEmpty()) {
            player.sendMessage(Component.text("-------------------------", NamedTextColor.GRAY));
            player.sendMessage(Component.text("NPC: ", NamedTextColor.YELLOW).append(Component.text(quest.getCompletionMessage(), NamedTextColor.WHITE)));
            player.sendMessage(Component.text("-------------------------", NamedTextColor.GRAY));
        }

        player.sendMessage(Component.text("CONGRATULATIONS! The job is done: " + quest.getTitle(), NamedTextColor.GREEN));

        checkForNextQuest(player, quest.getId());
        saveData(); // Veri değişti, kaydet
    }

    private void checkForNextQuest(Player player, String completedQuestId) {
        for (Quest q : quests.values()) {
            if (completedQuestId.equals(q.getRequiredQuestId())) {
                player.sendMessage(Component.text("NEW MISSION OPENED: " + q.getTitle(), NamedTextColor.GOLD));
                player.sendMessage(Component.text("To start: /quest start " + q.getId(), NamedTextColor.YELLOW));
            }
        }
    }
}