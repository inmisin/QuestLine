package org.inmisin.questLine;

import org.inmisin.questLine.commands.QuestCommand;
import org.inmisin.questLine.listeners.QuestAutoListener;
import org.inmisin.questLine.commands.QuestTabCompleter;
import org.inmisin.questLine.commands.QuestsCommand;
import org.inmisin.questLine.listeners.NpcListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class QuestLine extends JavaPlugin {

    private QuestManager questManager;

    @Override
    public void onEnable() {
        this.questManager = new QuestManager(this);

        // Komutlar...
        Objects.requireNonNull(getCommand("quest")).setExecutor(new QuestCommand(questManager));
        Objects.requireNonNull(getCommand("quest")).setTabCompleter(new QuestTabCompleter(questManager));
        Objects.requireNonNull(getCommand("quests")).setExecutor(new QuestsCommand(questManager));

        // Listener Kayıtları
        getServer().getPluginManager().registerEvents(new NpcListener(questManager), this);

        // --- YENİ EKLENEN SATIR ---
        // "this" parametresini plugin instance'ı olarak scheduler için gönderiyoruz
        getServer().getPluginManager().registerEvents(new QuestAutoListener(questManager, this), this);

        getLogger().info("QuestLine is active!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
