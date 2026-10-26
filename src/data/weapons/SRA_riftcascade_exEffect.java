package data.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPluginWithReset;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.utils.SRA_BoxUtilFX;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class SRA_riftcascade_exEffect implements BeamEffectPluginWithReset {
    private BeamAPI lastBeam;
    private Vector2f lastBeamEnd;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) {
            return;
        }
        lastBeam = beam;
        if (beam.getTo() != null) {
            if (lastBeamEnd == null) {
                lastBeamEnd = new Vector2f(beam.getTo());
            } else {
                lastBeamEnd.set(beam.getTo());
            }
        }
        CombatEntityAPI target = beam.getDamageTarget();
        if (beam.didDamageThisFrame() && target instanceof ShipAPI
                && SRA_riftcascadeFireControl.tryTriggerExplosion(beam.getWeapon())) {
            SRA_riftcascadeExplosion.spawn(beam, 320f, 320f, CollisionClass.MISSILE_FF);
        }

        if (beam.getTo() != null) {
            SRA_BoxUtilFX.addHitParticle(beam.getTo(), MathUtils.getPointOnCircumference(null,
                    MathUtils.getRandomNumberInRange(100f, 200f),
                    MathUtils.getRandomNumberInRange(0f, 360f)), 7f, 5f,
                    MathUtils.getRandomNumberInRange(0.5f, 1f), beam.getFringeColor());
        }
    }

    @Override
    public void reset() {
        if (lastBeam != null && lastBeamEnd != null
                && SRA_riftcascadeFireControl.tryTriggerExplosion(lastBeam.getWeapon())) {
            SRA_riftcascadeExplosion.spawn(lastBeam, lastBeamEnd,
                    320f, 320f, CollisionClass.MISSILE_FF);
        }
        lastBeam = null;
        lastBeamEnd = null;
    }
}
