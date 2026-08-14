package data.weapons;

import java.awt.Color;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;

public class SRA_plasma_shotOnHitEffect implements OnFireEffectPlugin {
    private static final String EXPLODED_KEY = "SRA_plasma_shot_exploded";
    private static final float EXPLOSION_RADIUS = 80f;
    private static final float EXPLOSION_DURATION = 0.12f;
    private static final Color PARTICLE_COLOR = new Color(60, 130, 255, 255);
    private static final Color EXPLOSION_COLOR = new Color(0, 165, 255, 180);

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (projectile == null || engine == null) return;
        engine.addPlugin(new SRAPlasmaExpiryPlugin(projectile, weapon));
    }

    private static void explode(DamagingProjectileAPI projectile, CombatEngineAPI engine, Vector2f point, ExplosionParams params) {
        if (projectile == null || engine == null || point == null || params == null || hasExploded(projectile)) return;

        projectile.setCustomData(EXPLODED_KEY, Boolean.TRUE);
        if (params.source != null) {
            engine.spawnDamagingExplosion(createExplosionSpec(params), params.source, point);
        } else {
            engine.spawnExplosion(point, new Vector2f(), EXPLOSION_COLOR, EXPLOSION_RADIUS, EXPLOSION_DURATION);
        }
        engine.removeEntity(projectile);
    }

    private static DamagingExplosionSpec createExplosionSpec(ExplosionParams params) {
        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                EXPLOSION_DURATION,
                EXPLOSION_RADIUS,
                EXPLOSION_RADIUS,
                params.damage,
                params.damage,
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                7f,
                5f,
                1.5f,
                48,
                PARTICLE_COLOR,
                EXPLOSION_COLOR
        );
        spec.setDamageType(DamageType.FRAGMENTATION);
        spec.setMaxEMPDamage(params.emp);
        spec.setMinEMPDamage(params.emp);
        spec.setSoundSetId("plasma_explosion");
        return spec;
    }

    private static boolean hasExploded(DamagingProjectileAPI projectile) {
        return projectile != null && Boolean.TRUE.equals(projectile.getCustomData().get(EXPLODED_KEY));
    }

    private static class ExplosionParams {
        private final ShipAPI source;
        private final float damage;
        private final float emp;

        private ExplosionParams(DamagingProjectileAPI projectile, WeaponAPI weapon) {
            ShipAPI fallbackSource = weapon == null ? null : weapon.getShip();
            this.source = projectile.getSource() != null ? projectile.getSource() : fallbackSource;
            // getDamageAmount() is DamageAPI.getDamage(): base damage with the projectile's own modifier/multiplier applied.
            this.damage = projectile.getDamageAmount();
            this.emp = projectile.getEmpAmount();
        }
    }

    private static class SRAPlasmaExpiryPlugin extends BaseEveryFrameCombatPlugin {
        private final DamagingProjectileAPI projectile;
        private final Vector2f lastLocation;
        private final ExplosionParams params;
        private boolean done;

        private SRAPlasmaExpiryPlugin(DamagingProjectileAPI projectile, WeaponAPI weapon) {
            this.projectile = projectile;
            this.lastLocation = new Vector2f(projectile.getLocation());
            this.params = new ExplosionParams(projectile, weapon);
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused() || done) return;

            if (projectile == null || hasExploded(projectile)) {
                remove(engine);
                return;
            }

            if (shouldExplode(engine)) {
                explode(projectile, engine, getExplosionPoint(engine), params);
                remove(engine);
                return;
            }

            lastLocation.set(projectile.getLocation());
        }

        private boolean shouldExplode(CombatEngineAPI engine) {
            return projectile.isExpired()
                    || projectile.wasRemoved()
                    || projectile.isFading()
                    || !engine.isEntityInPlay(projectile);
        }

        private Vector2f getExplosionPoint(CombatEngineAPI engine) {
            if (engine.isEntityInPlay(projectile) && projectile.getLocation() != null) {
                return new Vector2f(projectile.getLocation());
            }
            return new Vector2f(lastLocation);
        }

        private void remove(CombatEngineAPI engine) {
            done = true;
            engine.removePlugin(this);
        }
    }
}
