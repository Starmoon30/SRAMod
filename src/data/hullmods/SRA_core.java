package data.hullmods;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.BaseLogisticsHullMod;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import data.utils.SRAI18nUtil;

import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;

public class SRA_core extends BaseHullMod {
	public static float VentRate = 100f;
	public static float ShieldUnfoldRate = 400f;
	public static float CombatRepairTime = -50f;
	public static float HardFluxDissipation = 0.25f;
	private IntervalUtil repairTracker = new IntervalUtil(1f, 1f);
	private Color SRA_headtextcolor = new Color(100, 253, 253, 255);
    private Color SRA_bgtextcolor = new Color(33, 33, 33, 255);
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

		stats.getVentRateMult().modifyPercent(id, VentRate);					//强制Flux消散速度			
		
		stats.getShieldUnfoldRateMult().modifyPercent(id, ShieldUnfoldRate);			//护盾展开速度

		stats.getCombatEngineRepairTimeMult().modifyPercent(id, CombatRepairTime);			//战斗修理引擎时间
		stats.getCombatWeaponRepairTimeMult().modifyPercent(id, CombatRepairTime);			//战斗修理武器时间
			
		stats.getEmpDamageTakenMult().modifyMult(id,0f);
		
		stats.getDynamic().getStat(Stats.CORONA_EFFECT_MULT).modifyMult(id, 0f);			//免疫日冕
		
		stats.getHardFluxDissipationFraction().modifyFlat(id, HardFluxDissipation);			//允许舰船以25%的正常耗散速率在护盾开启时耗散硬幅能。
		
        stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, 10f/100f);	//零幅能加速的阈值提高至幅能容量的10%
        if (stats.getVariant().getHullMods().contains("safetyoverrides")) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(stats.getVariant(), "safetyoverrides", "SRA_core");
        }
	}
	
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (ship == null) return;
        TooltipMakerAPI text;
        float pad = 10f;
        tooltip.addSectionHeading(SRAI18nUtil.getHullModString("SRASectionHeading1"),SRA_headtextcolor,SRA_bgtextcolor, Alignment.MID, 10f);
        tooltip.addPara(SRAI18nUtil.getHullModString("SRA_core"), 10f, Misc.getPositiveHighlightColor(), "100%","400%","50%",SRAI18nUtil.getHullModString("SRA_immune"),SRAI18nUtil.getHullModString("SRA_immune"),"25%","0.5%","10%");
		
        tooltip.addSectionHeading(SRAI18nUtil.getHullModString("SRASectionHeadingIncompatible"),SRA_headtextcolor,SRA_bgtextcolor, Alignment.MID, 10f);
    	text = tooltip.beginImageWithText("graphics/hullmods/safety_overrides.png", 32);
        text.addPara(SRAI18nUtil.getHullModString("SRAParaIncompatible"), 2f, Misc.getNegativeHighlightColor(), SRAI18nUtil.getHullModString("SRAsafetyoverrides"));
        tooltip.addImageWithText(pad);
    }

	// public String getDescriptionParam(int index, HullSize hullSize) {
	// 	if (index == 0) return "" + (int) VentRate + "%";
	// 	if (index == 1) return "" + (int) ShieldUnfoldRate + "%";
	// 	if (index == 2) return "" + (int) CombatRepairTime + "%";
	// 	if (index == 3) return "25%";
	// 	if (index == 4) return "0.5%";
	// 	if (index == 5) return "10%";
	// 	return null;
	// }

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		float ARMOR_REGENT = (Float) 0.005f;//装甲再生百分比
		float HULL_REGENT = (Float) 0.005f;//装甲再生百分比
		if (!ship.isAlive()) {
			return;
		}
		repairTracker.advance(amount);
	
		if (repairTracker.intervalElapsed()) {
			// 恢复装甲
			ArmorGridAPI armorGrid = ship.getArmorGrid();
			if (armorGrid != null) {
				float cellMaxArmor = armorGrid.getMaxArmorInCell();
				float[][] armorGridValues = armorGrid.getGrid();
				int rows = armorGridValues.length;
				int cols = armorGridValues[0].length;
	
				float totalMaxArmor = rows * cols * cellMaxArmor;
				float totalCurrentArmor = 0;
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						totalCurrentArmor += armorGrid.getArmorValue(i, j);
					}
				}
				float armorToRepair = ARMOR_REGENT * totalMaxArmor;// 每次修复的装甲
				float armorNeeded = totalMaxArmor - totalCurrentArmor;
				if (armorToRepair > armorNeeded) {
					armorToRepair = armorNeeded;
				}
				// 均匀分配恢复的装甲值到每个单元格
				float perCellRepair = armorToRepair / (rows * cols);
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						float currentArmor = armorGrid.getArmorValue(i, j);
						float newArmor = Math.min(currentArmor + perCellRepair, cellMaxArmor);
						armorGrid.setArmorValue(i, j, newArmor);
					}
				}
			}
			// 恢复结构
			float maxStructure = ship.getMaxHitpoints();
			float currentStructure = ship.getHitpoints();
			float structureToRepair = HULL_REGENT * maxStructure; // 每次修复的结构
			if (currentStructure < maxStructure) {
				ship.setHitpoints(currentStructure + structureToRepair);
			}
		}
	}
}
