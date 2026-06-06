package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.RuleBasedDialog;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.AICoreAdminPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.missions.RecoverAPlanetkiller;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.impl.campaign.SRA_effect;

public class SRA_event_effect extends BaseCommandPlugin {
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params,
            Map<String, MemoryAPI> memoryMap) {
        String command = params.get(0).getString(memoryMap);
        switch (command) {
            case "SRA_buy_planet":
                String[] targetMarkets = {
                  "SRA_planet2_market", 
                  "SRA_planet3_market",
                  "SRA_planet4_market",
                  "SRA_planet5_market"
                };
                // 批量处理所有目标市场
                for (String marketId : targetMarkets) {
                    MarketAPI market = Global.getSector().getEconomy().getMarket(marketId);
                    SRA_effect.SRA_transferMarketToPlayer(market);
                }
                SRA_effect.SRA_transferSystemFacilitiesToPlayer();
                SRA_effect.SRA_enableRelationshipSync();
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(-271828182);
                AddRemoveCommodity.addCreditsLossText(271828182, dialog.getTextPanel());
                break;
            default:
                break;
        }
        return false;
    }

}
