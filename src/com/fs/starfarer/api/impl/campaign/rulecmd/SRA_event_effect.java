package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.awt.Color;
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
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.RecoverAPlanetkiller;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.impl.campaign.SRA_effect;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import exerelin.campaign.intel.SpecialContactIntel;

public class SRA_event_effect extends BaseCommandPlugin {
    private static final Color SRA_TEXT_BLUE = new Color(20, 151, 255, 255);
    private static final String[] CONTACT_CHAT_RESPONSES = {
            "闲聊文本七个字。",
            "这是第一个闲聊示例。",
            "这是第二个闲聊示例。",
            "这是第三个闲聊示例。"
    };
    private static final String[] CONTACT_GREETING_RESPONSES = {
            "通讯已重新建立，$playerName。你的来意是什么？",
            "再次见到你，$playerName。看来你又有新的计划。",
            "你好，$playerName。修会已经准备好听取你的请求。",
            "欢迎回来，$playerName。希望这次通讯能带来一些有价值的结果。"
    };

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
            case "SRA_unlock_contact":
                PersonAPI officer = Global.getSector().getImportantPeople().getPerson("SRA_Person_Name1");
                if (officer != null) {
                    officer.addTag(Tags.CONTACT_MILITARY);
                    officer.addTag(Tags.CONTACT_TRADE);
                }
                addContact(officer, dialog);
                break;
            case "SRA_contact_chat":
                if (dialog != null && dialog.getTextPanel() != null) {
                    String response = CONTACT_CHAT_RESPONSES[Misc.random.nextInt(CONTACT_CHAT_RESPONSES.length)];
                    dialog.getTextPanel().addPara(response, SRA_TEXT_BLUE);
                }
                break;
            case "SRA_contact_greeting":
                if (dialog != null && dialog.getTextPanel() != null) {
                    String response = CONTACT_GREETING_RESPONSES[Misc.random.nextInt(CONTACT_GREETING_RESPONSES.length)];
                    PersonAPI player = Global.getSector().getPlayerPerson();
                    if (player != null) {
                        response = response.replace("$playerName", player.getNameString());
                    }
                    dialog.getTextPanel().addPara(response, SRA_TEXT_BLUE);
                }
                break;
            case "SRA_close_defeat_dialog":
                if (dialog != null) {
                    dialog.dismiss();
                }
                break;
            default:
                break;
        }
        return false;
    }

    private void addContact(PersonAPI officer, InteractionDialogAPI dialog) {
        if (officer == null) {
            return;
        }

        boolean nexerelinEnabled = Global.getSettings().getModManager().isModEnabled("nexerelin");
        ContactIntel existing = ContactIntel.getContactIntel(officer);
        if (existing != null) {
            return;
        }

        if (nexerelinEnabled) {
            addNexSpecialContact(officer, dialog);
            return;
        }

        ContactIntel.addPotentialContact(100f, officer, officer.getMarket(),
                dialog == null ? null : dialog.getTextPanel());
    }

    private void addNexSpecialContact(PersonAPI officer, InteractionDialogAPI dialog) {
        SpecialContactIntel contact = new SpecialContactIntel(officer, officer.getMarket());
        Global.getSector().getIntelManager().addIntel(contact, false,
                dialog == null ? null : dialog.getTextPanel());
    }

}
