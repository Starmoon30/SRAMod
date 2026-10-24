package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import data.scripts.world.systems.SRA_AT_Wisdom_Pivot_Order_System_A;
import data.weapons.*;
import org.boxutil.BoxUtilModPlugin;
import org.boxutil.manager.StaticTrailManager;
import java.util.ArrayList;
import java.util.List;
import static com.fs.starfarer.api.Global.getSettings;

public class SRA_ModPlugin extends BaseModPlugin {
    public static String txt(String id) {
        return Global.getSettings().getString("campaign", id);
    }
    @Override
    public void onApplicationLoad() {
        //前置mod支持

        boolean hasLazyLib = getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("SRA requires LazyLib! 星规阵列需要LazyLib作为前置");
        }
        boolean hasMagicLib = getSettings().getModManager().isModEnabled("MagicLib");
        if (!hasMagicLib) {
            throw new RuntimeException("SRA requires MagicLib! 星规阵列需要MagicLib作为前置");
        }
        boolean hasGraphicLib = getSettings().getModManager().isModEnabled("shaderLib");
        if (!hasGraphicLib) {
            throw new RuntimeException("SRA requires GraphicLib! 星规阵列需要GraphicLib作为前置");
        }
        BoxUtilModPlugin.initPre();
        StaticTrailManager.loadTrailData("data/config/modFiles/SRA_trail_data.csv");
    }

    @Override
    public void onNewGame() {
        //势力争霸支持
        //Nex compatibility setting, if there is no nex or corvus mode(Nex), just generate the system
        boolean haveNexerelin = getSettings().getModManager().isModEnabled("nexerelin");
        //if (!haveNexerelin || SectorManager.getCorvusMode()) {
        generate(Global.getSector());
        //}
    }

    private void generate(SectorAPI sector) {
        FactionAPI SRA_AT_Wisdom_Pivot_Order = sector.getFaction("SRA_AT_Wisdom_Pivot_Order");
        //设置与(待定）的初始好感为欢迎
        // FactionAPI 待定 = sector.getFaction("待定");
        // if (待定 != null) {
        //     待定.setRelationship(SRA_AT_Wisdom_Pivot_Order.getId(), RepLevel.WELCOMING);
        // }
        // 设置星系
        new SRA_AT_Wisdom_Pivot_Order_System_A().generate(sector);
        // 设置所属势力
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("SRA_AT_Wisdom_Pivot_Order");
        // 设置势力与其他势力好感度
        SRA_AT_Wisdom_Pivot_Order.setRelationship(Factions.LUDDIC_CHURCH, -0.7f);
        SRA_AT_Wisdom_Pivot_Order.setRelationship(Factions.LUDDIC_PATH, -0.5f);
        SRA_AT_Wisdom_Pivot_Order.setRelationship(Factions.PERSEAN, 0.3f);
        SRA_AT_Wisdom_Pivot_Order.setRelationship(Factions.PIRATES, -0.75f);
        SRA_AT_Wisdom_Pivot_Order.setRelationship(Factions.HEGEMONY, -0.6f);
    }

    //经济系统初始化完毕后
    @Override
    public void onNewGameAfterEconomyLoad() {
        ImportantPeopleAPI people = Global.getSector().getImportantPeople();
        MarketAPI market = Global.getSector().getEconomy().getMarket("SRA_planet1_market");
        //事先删除整个market里的所有人物，只留一个我们新建的
        if (market != null) {
            for (PersonAPI p : market.getPeopleCopy()) {
                if (p.getRankId().equals(Ranks.SPACE_COMMANDER)){
                    market.removePerson(p);
                    people.removePerson(p);
                    market.getCommDirectory().removePerson(p);
                }
            }
            ////
            List<String> SRA_Person_Name1 = new ArrayList<>();
            SRA_Person_Name1.add(Skills.HYPERCOGNITION);
            SRA_Person_Name1.add(Skills.INDUSTRIAL_PLANNING );

            PersonAPI officer = Global.getFactory().createPerson();

            for (String HF_skill : SRA_Person_Name1) {
                officer.getStats().setSkillLevel(HF_skill, 2);
            }
            OfficerLevelupPlugin plugin = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");

            officer.getStats().addXP(plugin.getXPForLevel(1));

            officer.setPersonality(Personalities.RECKLESS);
            officer.setPortraitSprite(Global.getSettings().getSpriteName("characters", "SRA_StellarRegulator"));
            officer.setName(new FullName("", txt("SRA_Person_Name1"), FullName.Gender.FEMALE));
            ////
            officer.setId("SRA_Person_Name1");//人物id，游戏中可以唯一找到它的识别名
            officer.setPostId(Ranks.FACTION_LEADER);//设置该人物的职位
            officer.setRankId(Ranks.FACTION_LEADER);//设置该人物的军衔
            officer.setFaction(market.getFactionId());

            officer.addTag(Tags.CONTACT_MILITARY);
            officer.addTag(Tags.CONTACT_TRADE);
            officer.setImportanceAndVoice(PersonImportance.VERY_HIGH, StarSystemGenerator.random);//设置人物的重要性，至于Voice是角色打招呼的语气，例如voice = faithful就会说“卢德保佑你”之类，可在rules中自定义
            officer.setVoice("SRA_stellar_regulator");

            people.addPerson(officer);//只有加入ImportantPeople，该人物才能被rules和missionHub识别
            people.getData(officer).getLocation().setMarket(market);//将人物传送到指定market里
            people.checkOutPerson(officer, "permanent_staff");//这个的意思是把人物以'永久成员(permanent_staff)'的理由签发出去，如此一来就不会成为某些随机任务的目标。

            market.setAdmin(officer);//设置为市场管理员
            market.getCommDirectory().addPerson(officer, 0);//将其加入通讯录中
            market.addPerson(officer);//将该person加入市场的人物列表，使某些按市场寻人的方法可以找到

            //这里是设置该人物拥有多少个额外任务上限，若不填，则每次只能刷出一个任务，若填1，则每次最多能刷出2个人物，填2则最多刷出3个。
            officer.getMemoryWithoutUpdate().set(BaseMissionHub.NUM_BONUS_MISSIONS, 0);
            //为人物添加MissionHub
            BaseMissionHub.set(officer, new BaseMissionHub(officer));
        }
    }

}
