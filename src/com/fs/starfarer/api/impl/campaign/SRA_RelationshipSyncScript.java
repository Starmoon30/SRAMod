package com.fs.starfarer.api.impl.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.PersonAPI;

public class SRA_RelationshipSyncScript extends BaseCampaignEventListener implements EveryFrameScript {
    public static final String SRA_FACTION_ID = "SRA_AT_Wisdom_Pivot_Order";
    private static final float SYNC_INTERVAL_DAYS = 1f;

    private float daysSinceSync = 0f;

    public SRA_RelationshipSyncScript() {
        super(true);
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;

        daysSinceSync += sector.getClock().convertToDays(amount);
        if (daysSinceSync < SYNC_INTERVAL_DAYS) return;

        daysSinceSync = 0f;
        syncNow();
    }

    @Override
    public void reportPlayerReputationChange(String factionId, float delta) {
        syncNow();
    }

    @Override
    public void reportPlayerReputationChange(PersonAPI person, float delta) {
        syncNow();
    }

    public static void syncNow() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;

        FactionAPI sra = sector.getFaction(SRA_FACTION_ID);
        FactionAPI player = sector.getPlayerFaction();
        if (sra == null || player == null) return;

        String playerFactionId = player.getId();
        for (FactionAPI faction : sector.getAllFactions()) {
            if (faction == null) continue;

            String factionId = faction.getId();
            if (factionId == null) continue;
            if (SRA_FACTION_ID.equals(factionId) || playerFactionId.equals(factionId)) continue;

            sra.setRelationship(factionId, player.getRelationship(factionId));
        }

        sra.setRelationship(playerFactionId, 1f);
        player.setRelationship(SRA_FACTION_ID, 1f);
    }
}
