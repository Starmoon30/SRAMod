package data.scripts.ungpbackgrounds;

import java.awt.Color;
import ungp.api.backgrounds.UNGP_BaseBackgroundPlugin;
public class SRA_max_skills extends UNGP_BaseBackgroundPlugin {
   public SRA_max_skills() {
   }

   public float getInheritCreditsFactor() {
      return 1.0F;
   }

   public float getInheritBlueprintsFactor() {
      return 1.0F;
   }

   public void initCycleBonus() {
      this.addCycleBonus(1, new UNGP_BaseBackgroundPlugin.BackgroundBonus(BackgroundBonusType.SKILL_POINTS, new Object[]{25}));
   }

   public Color getOverrideNameColor() {
      return new Color(0, 255, 255);
   }
}
