package com.fs.starfarer.api.impl.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.AICoreAdminPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.missions.RecoverAPlanetkiller;
import com.fs.starfarer.api.impl.campaign.rulecmd.Nex_IsFactionRuler;
import com.fs.starfarer.api.impl.campaign.shared.PlayerTradeDataForSubmarket;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

public class SRA_effect {
    private static final String MEM_STARTING_FACTION_ID = "$startingFactionId";
    private static final String MEM_NEX_EXISTED_AT_START = "$nex_existed_at_start";
    private static final String MEM_NEX_RECENTLY_CAPTURED_FROM = "$nex_recentlyCapturedFrom";
    private static final String MEM_NEX_RECENTLY_CAPTURED_BY_PLAYER = "$nex_recentlyCapturedByPlayer";
    private static final String MEM_NEX_CAPTURE_STABILIZE_TIMEOUT = "$nex_captureStabilizeTimeout";
    private static final String MEM_NEX_PREVIOUS_OWNER_FOR_INVASION_RESTRICTION = "$nex_previousOwnerForInvasionRestriction";

    private static final String[] SRA_SYSTEM_FACILITY_IDS = {
            "SRA_gate",
            "SRA_A",
            "SRA_B",
            "SRA_C"
    };

    // 通用殖民地转移方法
    public static void SRA_transferMarketToPlayer(MarketAPI market) {
        if (market == null) return;
        String playerFactionId = Global.getSector().getPlayerFaction().getId();
        
        Set<SectorEntityToken> linkedEntities = market.getConnectedEntities();
        if (market.getPlanetEntity() != null) {
            PlanetAPI planet = market.getPlanetEntity();
            SRA_transferEntityToPlayer(planet, playerFactionId);
        }
        
        for (SectorEntityToken entity : linkedEntities)
        {
            SRA_transferEntityToPlayer(entity, playerFactionId);
        }
        
        // Use comm board people instead of market people, 
        // because some appear on the former but not the latter 
        // (specifically when a new market admin is assigned, old one disappears from the market)
        // Also, this way it won't mess with player-assigned admins
        for (CommDirectoryEntryAPI dir : market.getCommDirectory().getEntriesCopy())
        {
            if (dir.getType() != CommDirectoryEntryAPI.EntryType.PERSON) continue;
            PersonAPI person = (PersonAPI)dir.getEntryData();
            person.setFaction(playerFactionId);
        }
        market.setFactionId(playerFactionId);
        market.setPlayerOwned(playerFactionId.equals(Factions.PLAYER));
        SRA_markNexOriginalOwnerAsPlayer(market, playerFactionId);
        
        // 设置实体所属派系
        SectorEntityToken primaryEntity = market.getPrimaryEntity();
        if (primaryEntity != null) {
            SRA_transferEntityToPlayer(primaryEntity, playerFactionId);
        }
        // player: free storage unlock
        if (Nex_IsFactionRuler.isRuler(playerFactionId))
        {
            SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
            if (storage != null)
            {
                StoragePlugin plugin = (StoragePlugin)market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin();
                if (plugin != null)
                    plugin.setPlayerPaidToUnlock(true);
            }
        }
        
        
        market.setPlayerOwned(true);
        // set submarket factions
        List<SubmarketAPI> submarkets = market.getSubmarketsCopy();
        for (SubmarketAPI submarket : submarkets)
        {
            //if (submarket.getFaction() != oldOwner) continue;
            //log.info(String.format("Submarket %s has spec faction %s", submarket.getNameOneLine(), submarket.getSpec().getFactionId()));
            String submarketId = submarket.getSpecId();
            // reset smuggling suspicion
            if (submarketId.equals(Submarkets.SUBMARKET_BLACK)) {  
              PlayerTradeDataForSubmarket tradeData = SharedData.getData().getPlayerActivityTracker().getPlayerTradeData(submarket);  
              tradeData.setTotalPlayerTradeValue(0);
              continue;
            }
            
            submarket.setFaction(Global.getSector().getPlayerFaction());
        }
        
        market.reapplyConditions();
        market.reapplyIndustries();
        
        market.setAdmin(Global.getSector().getPlayerPerson());//设置为市场管理员
    }

    public static void SRA_transferSystemFacilitiesToPlayer() {
        String playerFactionId = Global.getSector().getPlayerFaction().getId();
        for (String entityId : SRA_SYSTEM_FACILITY_IDS) {
            SectorEntityToken entity = Global.getSector().getEntityById(entityId);
            SRA_transferEntityToPlayer(entity, playerFactionId);
        }
    }

    public static void SRA_enableRelationshipSync() {
        if (!Global.getSector().hasScript(SRA_RelationshipSyncScript.class)) {
            Global.getSector().addScript(new SRA_RelationshipSyncScript());
        }
        SRA_RelationshipSyncScript.syncNow();
    }

    private static void SRA_transferEntityToPlayer(SectorEntityToken entity, String playerFactionId) {
        if (entity == null) return;

        entity.setFaction(playerFactionId);
        SRA_markNexOriginalOwnerAsPlayer(entity, playerFactionId);

        MarketAPI entityMarket = entity.getMarket();
        if (entityMarket != null) {
            SRA_markNexOriginalOwnerAsPlayer(entityMarket, playerFactionId);
        }

        CampaignFleetAPI statFleet = Misc.getStationBaseFleet(entity);
        if (statFleet != null) statFleet.setFaction(playerFactionId, true);
        statFleet = Misc.getStationFleet(entity);
        if (statFleet != null) statFleet.setFaction(playerFactionId, true);
    }

    private static void SRA_markNexOriginalOwnerAsPlayer(MarketAPI market, String playerFactionId) {
        if (market == null) return;
        SRA_markNexOriginalOwnerAsPlayer(market.getMemoryWithoutUpdate(), playerFactionId);
    }

    private static void SRA_markNexOriginalOwnerAsPlayer(SectorEntityToken entity, String playerFactionId) {
        if (entity == null) return;
        SRA_markNexOriginalOwnerAsPlayer(entity.getMemoryWithoutUpdate(), playerFactionId);
    }

    private static void SRA_markNexOriginalOwnerAsPlayer(MemoryAPI memory, String playerFactionId) {
        if (memory == null) return;

        memory.set(MEM_STARTING_FACTION_ID, playerFactionId);
        memory.set(MEM_NEX_EXISTED_AT_START, true);
        memory.unset(MEM_NEX_RECENTLY_CAPTURED_FROM);
        memory.unset(MEM_NEX_RECENTLY_CAPTURED_BY_PLAYER);
        memory.unset(MEM_NEX_CAPTURE_STABILIZE_TIMEOUT);
        memory.unset(MEM_NEX_PREVIOUS_OWNER_FOR_INVASION_RESTRICTION);
    }
}
