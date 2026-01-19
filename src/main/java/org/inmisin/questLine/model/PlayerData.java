package org.inmisin.questLine.model;

import java.util.HashSet;
import java.util.Set;

public class PlayerData {
    private String activeQuestId;
    private final Set<String> completedQuests;

    public PlayerData() {
        this.activeQuestId = null;
        this.completedQuests = new HashSet<>();
    }

    public String getActiveQuestId() {
        return activeQuestId;
    }

    public void setActiveQuestId(String activeQuestId) {
        this.activeQuestId = activeQuestId;
    }

    public boolean isQuestCompleted(String questId) {
        return completedQuests.contains(questId);
    }

    public void addCompletedQuest(String questId) {
        completedQuests.add(questId);
    }

    public Set<String> getCompletedQuests() {
        return completedQuests;
    }

}