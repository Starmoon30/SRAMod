package data.weapons.dem;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.dem.DEMScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;

public class SRA_dragonEffect implements OnFireEffectPlugin {
    private static final float DAMAGE_FIELD_RADIUS = 64f;
    private static final float DAMAGE_PER_PULSE = 100f;
    private static final float PULSE_INTERVAL = 0.5f;
    private static final Color DAMAGE_FIELD_COLOR = new Color(15, 15, 255, 150);

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (!(projectile instanceof MissileAPI)) return;

        MissileAPI missile = (MissileAPI) projectile;
        ShipAPI ship = weapon == null ? null : weapon.getShip();
        if (ship == null) return;

        engine.addPlugin(new SRA_dragonDEMScript(missile, ship, weapon));
    }

    private static class SRA_dragonDEMScript extends DEMScript {
        private final IntervalUtil pulseTracker = new IntervalUtil(PULSE_INTERVAL, PULSE_INTERVAL);

        public SRA_dragonDEMScript(MissileAPI missile, ShipAPI ship, WeaponAPI weapon) {
            super(missile, ship, weapon);
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            super.advance(amount, events);

            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;
            if (missile == null || missile.isExpired() || missile.didDamage()) return;
            if (missile.isFading() || missile.isFizzling()) return;
            if (!engine.isEntityInPlay(missile)) return;

            pulseTracker.advance(amount);
            if (!pulseTracker.intervalElapsed()) return;

            engine.spawnDamagingExplosion(createDamageFieldSpec(), ship, missile.getLocation());
        }
    }

    private static DamagingExplosionSpec createDamageFieldSpec() {
        DamagingExplosionSpec explosion = new DamagingExplosionSpec(
                PULSE_INTERVAL,
                DAMAGE_FIELD_RADIUS,
                DAMAGE_FIELD_RADIUS,
                DAMAGE_PER_PULSE,
                DAMAGE_PER_PULSE * 0.75f,
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                0.5f,
                0.5f,
                0.5f,
                0,
                DAMAGE_FIELD_COLOR,
                DAMAGE_FIELD_COLOR
        );
        explosion.setDamageType(DamageType.FRAGMENTATION);
        explosion.setUseDetailedExplosion(false);
        explosion.setShowGraphic(false);
        explosion.setSoundSetId(null);
        return explosion;
    }
}




