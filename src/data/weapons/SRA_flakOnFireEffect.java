package data.weapons;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;

public class SRA_flakOnFireEffect implements OnFireEffectPlugin {
    private static final float PROXIMITY_FUSE_RANGE = 64f;

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (projectile == null || weapon == null || engine == null) return;

        TeleportPick pick = findTeleportPoint(projectile, weapon, engine);
        projectile.getLocation().set(pick.point);
        // Keep the original projectile damage chain; the native PROXIMITY_FUSE handles the explosion.
    }

    private TeleportPick findTeleportPoint(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f start = new Vector2f(weapon.getLocation());
        float range = weapon.getRange();
        Vector2f direction = Misc.getUnitVectorAtDegreeAngle(weapon.getCurrAngle());
        Vector2f fallback = new Vector2f(start.x + direction.x * range, start.y + direction.y * range);

        TargetPick best = null;
        for (MissileAPI missile : engine.getMissiles()) {
            if (!isValidMissileTarget(projectile, missile)) continue;
            best = pickBetterTarget(best, missile, start, direction, fallback, range, 0);
        }
        for (ShipAPI target : engine.getShips()) {
            if (!isValidShipTarget(projectile, target)) continue;
            int priority = target.isFighter() ? 1 : 2;
            best = pickBetterTarget(best, target, start, direction, fallback, range, priority);
        }

        if (best != null) return new TeleportPick(best.point);
        return new TeleportPick(fallback);
    }

    private boolean isValidMissileTarget(DamagingProjectileAPI projectile, MissileAPI missile) {
        if (!isValidTarget(projectile, missile)) return false;
        return !missile.isFizzling() && !missile.isFading();
    }

    private boolean isValidShipTarget(DamagingProjectileAPI projectile, ShipAPI target) {
        if (!isValidTarget(projectile, target)) return false;
        return target.isAlive() && !target.isHulk() && !target.isPhased();
    }

    private boolean isValidTarget(DamagingProjectileAPI projectile, CombatEntityAPI target) {
        if (target == null) return false;
        if (target.getOwner() == projectile.getOwner()) return false;
        if (target.getOwner() < 0) return false;
        return !target.isExpired() && !target.wasRemoved();
    }

    private TargetPick pickBetterTarget(TargetPick best, CombatEntityAPI target, Vector2f start,
            Vector2f direction, Vector2f fallback, float range, int priority) {
        Vector2f toTarget = Vector2f.sub(target.getLocation(), start, null);
        float along = toTarget.x * direction.x + toTarget.y * direction.y;
        if (along < 0f || along > range + target.getCollisionRadius()) return best;

        Vector2f closest = Misc.closestPointOnSegmentToPoint(start, fallback, target.getLocation());
        float distanceFromAim = Misc.getDistance(closest, target.getLocation());
        float allowedDistance = target.getCollisionRadius() + PROXIMITY_FUSE_RANGE;
        if (distanceFromAim > allowedDistance) return best;

        float score = priority * 1000000f + distanceFromAim * 1000f + Math.max(0f, along);
        if (best == null || score < best.score) {
            return new TargetPick(new Vector2f(closest), score);
        }
        return best;
    }

    private static class TargetPick {
        private final Vector2f point;
        private final float score;

        private TargetPick(Vector2f point, float score) {
            this.point = point;
            this.score = score;
        }
    }

    private static class TeleportPick {
        private final Vector2f point;

        private TeleportPick(Vector2f point) {
            this.point = point;
        }
    }
}
