package data.Plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.CombatEntityPluginWithParticles;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import data.utils.SRA_BoxUtilFX;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SRA_explosionPlugin extends CombatEntityPluginWithParticles {

    private static final float EXPLOSION_RADIUS = 160f;
    protected WeaponAPI weapon;
    protected DamagingProjectileAPI proj;
    protected IntervalUtil ExpInterval = new IntervalUtil(0.1f, 0.1f);
    protected float delay = 0f;
    protected Vector2f previousLocation;

    public SRA_explosionPlugin(WeaponAPI weapon) {
        super();
        this.weapon = weapon;
        ExpInterval = new IntervalUtil(0.02f, 0.02f);
        delay = 0f;
    }

    public void attachToProjectile (DamagingProjectileAPI proj) {
        this.proj = proj;
        this.previousLocation = proj == null || proj.getLocation() == null
                ? null : new Vector2f(proj.getLocation());
    }

    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;
        if (proj != null) {
            entity.getLocation().set(proj.getLocation());
        } else {
            entity.getLocation().set(weapon.getFirePoint(0));
        }
        super.advance(amount);

        if (proj != null && !isProjectileExpired(proj) && !proj.isFading()) {
            ShipAPI ship = weapon.getShip();
            delay -= amount;
            if (delay <= 0) {
                ExpInterval.advance(amount);
                if (ExpInterval.intervalElapsed()) {
                    Vector2f currentLocation = new Vector2f(proj.getLocation());
                    PathHit hit = findPathHit(previousLocation, currentLocation, ship);
                    if (previousLocation != null) {
                        previousLocation.set(currentLocation);
                    }
                    if (hit != null) {
                        Vector2f point = hit.point;
                        boolean shieldHit = hit.target instanceof ShipAPI
                                && ((ShipAPI) hit.target).getShield() != null
                                && ((ShipAPI) hit.target).getShield().isOn()
                                && ((ShipAPI) hit.target).getShield().isWithinArc(point);
                        DamagingProjectileAPI explosion = Global.getCombatEngine().spawnDamagingExplosion(
                                shieldHit ? SRA_T_weapon_supremeshot_explosion3(proj)
                                        : SRA_T_weapon_supremeshot_explosion2(proj),
                                proj.getSource(), point);
                        spawnSupremeImpactVisual(point);
                        spawnRiftVisual(explosion);
                    }
                }

            }
        }

    }
    //无光效爆炸
    public DamagingExplosionSpec SRA_T_weapon_supremeshot_explosion(DamagingProjectileAPI proj) {
        float damage = 3200;
        DamagingExplosionSpec Explosion=new DamagingExplosionSpec(
                0.05f, // duration
                160f, // radius
                80f, // coreRadius
                damage, // maxDamage
                damage * 0.75f, // minDamage
                CollisionClass.PROJECTILE_NO_FF, // collisionClass
                CollisionClass.PROJECTILE_FIGHTER, // collisionClassByFighter
                0.5f, // particleSizeMin
                0.5f, // particleSizeRange
                0.5f, // particleDuration
                10, // particleCount
                new Color(15, 15, 255, 0), // particleColor
                new Color(15, 15, 255, 0)  // explosionColor
        );
        Explosion.setDamageType(DamageType.ENERGY);
        Explosion.setUseDetailedExplosion(false);
        Explosion.setShowGraphic(false);
        Explosion.setSoundSetId(null);
        return Explosion;
    }

    //正常爆炸
    public DamagingExplosionSpec SRA_T_weapon_supremeshot_explosion2(DamagingProjectileAPI proj) {
        float damage = 3200;
        DamagingExplosionSpec Explosion=new DamagingExplosionSpec(
                0.1f, // duration
                160f, // radius
                80f, // coreRadius
                damage, // maxDamage
                damage * 0.75f, // minDamage
                CollisionClass.PROJECTILE_NO_FF, // collisionClass
                CollisionClass.PROJECTILE_FIGHTER, // collisionClassByFighter
                0.5f, // particleSizeMin
                0.5f, // particleSizeRange
                1f, // particleDuration
                10, // particleCount
                new Color(105,105,205, 225), // particleColor
                new Color(105,105,205, 180)  // explosionColor
        );
        Explosion.setDamageType(DamageType.ENERGY);
        Explosion.setUseDetailedExplosion(false);
        Explosion.setShowGraphic(false);
        Explosion.setSoundSetId("SRA_star_eater_weapon_hit_01");
        return Explosion;
    }

    //击中护盾时的爆炸
    public DamagingExplosionSpec SRA_T_weapon_supremeshot_explosion3(DamagingProjectileAPI proj) {
        float damage = 3200;
        DamagingExplosionSpec Explosion=new DamagingExplosionSpec(
                0.05f, // duration
                160f, // radius
                80f, // coreRadius
                damage, // maxDamage
                damage * 0.75f, // minDamage
                CollisionClass.PROJECTILE_NO_FF, // collisionClass
                CollisionClass.PROJECTILE_FIGHTER, // collisionClassByFighter
                0.5f, // particleSizeMin
                0.5f, // particleSizeRange
                0.5f, // particleDuration
                10, // particleCount
                new Color(105,105,205, 225), // particleColor
                new Color(105,105,205, 180)  // explosionColor
        );
        Explosion.setDamageType(DamageType.ENERGY);
        Explosion.setUseDetailedExplosion(false);
        Explosion.setShowGraphic(false);
        Explosion.setSoundSetId(null);
        return Explosion;
    }
    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        super.render(layer, viewport, null);
    }

    public boolean isExpired() {
        boolean keepSpawningParticles = isWeaponCharging(weapon) ||
                (proj != null && !isProjectileExpired(proj) && !proj.isFading());
        return super.isExpired() && (!keepSpawningParticles || (!weapon.getShip().isAlive() && proj == null));
    }

    public float getRenderRadius() {
        return 500f;
    }

    @Override
    protected float getGlobalAlphaMult() {
        if (proj != null && proj.isFading()) {
            return proj.getBrightness();
        }
        return super.getGlobalAlphaMult();
    }

    public static boolean isProjectileExpired(DamagingProjectileAPI proj) {
        return proj.isExpired() || proj.didDamage() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    public static boolean isWeaponCharging(WeaponAPI weapon) {
        return weapon.getChargeLevel() > 0 && weapon.getCooldownRemaining() <= 0;
    }

    private PathHit findPathHit(Vector2f start, Vector2f end, ShipAPI source) {
        if (start == null || end == null) {
            return null;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        PathHit closestHit = null;
        for (MissileAPI missile : engine.getMissiles()) {
            closestHit = selectPathHit(closestHit, missile, start, end, source);
        }
        for (ShipAPI target : engine.getShips()) {
            closestHit = selectPathHit(closestHit, target, start, end, source);
        }
        return closestHit;
    }

    private PathHit selectPathHit(PathHit current, CombatEntityAPI target,
                                  Vector2f start, Vector2f end, ShipAPI source) {
        if (target == null || target == source || target.isExpired()
                || (target instanceof ShipAPI && !((ShipAPI) target).isAlive())
                || (source != null && target.getOwner() == source.getOwner())) {
            return current;
        }

        Vector2f closestPoint = Misc.closestPointOnSegmentToPoint(start, end, target.getLocation());
        float distance = MathUtils.getDistance(closestPoint, target.getLocation());
        float hitRadius = EXPLOSION_RADIUS + Math.max(0f, target.getCollisionRadius());
        if (distance > hitRadius || (current != null && distance >= current.distance)) {
            return current;
        }
        return new PathHit(target, closestPoint, distance);
    }

    private void spawnSupremeImpactVisual(Vector2f point) {
        Color particleColor = new Color(70, 70, 211, 255);
        SRA_BoxUtilFX.spawnExpandingImpactExplosion(point, particleColor, 640f, 1f);
    }

    private void spawnRiftVisual(DamagingProjectileAPI explosion) {
        if (explosion == null) {
            return;
        }
        NEParams params = RiftCascadeMineExplosion.createStandardRiftParams(
                new Color(60, 60, 255, 255), 10f);
        params.thickness = 320f;
        params.fadeOut = 0.8f;
        RiftCascadeMineExplosion.spawnStandardRift(explosion, params);
    }

    private static class PathHit {
        private final CombatEntityAPI target;
        private final Vector2f point;
        private final float distance;

        private PathHit(CombatEntityAPI target, Vector2f point, float distance) {
            this.target = target;
            this.point = point;
            this.distance = distance;
        }
    }

}
