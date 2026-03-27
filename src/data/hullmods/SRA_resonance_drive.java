package data.hullmods;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
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
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import data.utils.SRAI18nUtil;

import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;

public class SRA_resonance_drive extends BaseHullMod {
	private Color SRA_headtextcolor = new Color(100, 253, 253, 255);
	private Color SRA_bgtextcolor = new Color(33, 33, 33, 255);
	
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		if (ship == null) return;
		tooltip.addSectionHeading(SRAI18nUtil.getHullModString("SRASectionHeading1"),SRA_headtextcolor,SRA_bgtextcolor, Alignment.MID, 10f);
		tooltip.addPara(SRAI18nUtil.getHullModString("SRA_resonance_drive_1"), 10f, Misc.getPositiveHighlightColor(), "75%","75%");
		tooltip.addPara(SRAI18nUtil.getHullModString("SRA_resonance_drive_2"), 10f, Misc.getNegativeHighlightColor(), "50%","50%");
		
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
		if (!ship.isAlive()) {
			return;
		}
		if (ship.getFluxLevel() >= 0.75F) {
			ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_2");
			ship.getMutableStats().getBeamWeaponFluxCostMult().unmodifyPercent("SRA_resonance_drive_2");
			ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_2");
			ship.getMutableStats().getMissileWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_2");
			ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyPercent("SRA_resonance_drive_1", -75.0F);
			ship.getMutableStats().getBeamWeaponFluxCostMult().modifyPercent("SRA_resonance_drive_1", -75.0F);
			ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyPercent("SRA_resonance_drive_1", -75.0F);
			ship.getMutableStats().getMissileWeaponFluxCostMod().modifyPercent("SRA_resonance_drive_1", -75.0F);
			if (Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship) {
				Global.getCombatEngine().maintainStatusForPlayerShip("SRA_resonance_drive_1_TOOLTIP", "graphics/icons/tactical/overloaded2.png",SRAI18nUtil.getHullModString("SRA_resonance_drive_1_TOOLTIP_title"), SRAI18nUtil.getHullModString("SRA_resonance_drive_1_TOOLTIP_desc"), false);
			}
		} else if (ship.getFluxLevel() <= 0.50F) {
			ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_1");
			ship.getMutableStats().getBeamWeaponFluxCostMult().unmodifyPercent("SRA_resonance_drive_1");
			ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_1");
			ship.getMutableStats().getMissileWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_1");
			ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyPercent("SRA_resonance_drive_2", 50.0F);
			ship.getMutableStats().getBeamWeaponFluxCostMult().modifyPercent("SRA_resonance_drive_2", 50.0F);
			ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyPercent("SRA_resonance_drive_2", 50.0F);
			ship.getMutableStats().getMissileWeaponFluxCostMod().modifyPercent("SRA_resonance_drive_2", 50.0F);
			if (Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship) {
				Global.getCombatEngine().maintainStatusForPlayerShip("SRA_resonance_drive_2_TOOLTIP", "graphics/icons/tactical/overloaded2.png",SRAI18nUtil.getHullModString("SRA_resonance_drive_2_TOOLTIP_title"), SRAI18nUtil.getHullModString("SRA_resonance_drive_2_TOOLTIP_desc"), true);
			}
		} else {
			ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_1");
			ship.getMutableStats().getBeamWeaponFluxCostMult().unmodifyPercent("SRA_resonance_drive_1");
			ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_1");
			ship.getMutableStats().getMissileWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_1");
			ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_2");
			ship.getMutableStats().getBeamWeaponFluxCostMult().unmodifyPercent("SRA_resonance_drive_2");
			ship.getMutableStats().getEnergyWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_2");
			ship.getMutableStats().getMissileWeaponFluxCostMod().unmodifyPercent("SRA_resonance_drive_2");
		}
	}
}
