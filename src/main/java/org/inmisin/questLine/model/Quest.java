package org.inmisin.questLine.model;

import org.bukkit.Material;
import java.util.Map;
import java.util.UUID;

public class Quest {
    private final String id;
    private final String title;
    private final String description;
    private final UUID npcUuid;
    private final Map<Material, Integer> requirements;

    // YENİ: Bu görevi görmek için bitirilmesi gereken görev ID'si
    private final String requiredQuestId;
    private String completionMessage;

    public Quest(String id, String title, String description, UUID npcUuid, Map<Material, Integer> requirements, String requiredQuestId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.npcUuid = npcUuid;
        this.requirements = requirements;
        this.requiredQuestId = requiredQuestId;
        this.completionMessage = null;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public UUID getNpcUuid() { return npcUuid; }
    public Map<Material, Integer> getRequirements() { return requirements; }


    public String getRequiredQuestId() { return requiredQuestId; }
    public String getCompletionMessage() { return completionMessage; }
    public void setCompletionMessage(String completionMessage) { this.completionMessage = completionMessage; }
}
