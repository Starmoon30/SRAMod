package data.shipsystems.ai;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class SRA_ExecutorSystemAI implements ShipSystemAIScript {
    private static final float TARGET_RANGE = 1800f;
    private static final float HIGH_FLUX_THRESHOLD = 0.45f;
    private static final float FULL_CHARGE_GRACE_PERIOD = 1.25f;
    private static final float FULL_CHARGE_FORCE_USE_AFTER = 4f;

    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;
    private final IntervalUtil tracker = new IntervalUtil(0.1f, 0.2f);
    private float fullChargeElapsed = 0f;
    private float fullChargeUseWeight = 0f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null || engine.isPaused()) return;

        advanceFullChargePressure(amount);
        tracker.advance(amount);
        if (!tracker.intervalElapsed()) return;
        if (!canUseSystem()) return;

        boolean travelOrRetreat = shouldUseForTravelOrRetreat(target);
        boolean temporalShellUse = shouldUseLikeTemporalShell(missileDangerDir, collisionDangerDir, target);

        if (isAtFullCharges()) {
            if (travelOrRetreat || temporalShellUse || shouldSpendFullChargeFromOverflow()) {
                ship.useSystem();
            }
            return;
        }

        if (travelOrRetreat || temporalShellUse) {
            ship.useSystem();
        }
    }

    private void advanceFullChargePressure(float amount) {
        if (system == null || !isAtFullCharges()) {
            fullChargeElapsed = 0f;
            fullChargeUseWeight = 0f;
            return;
        }

        fullChargeElapsed += amount;
        float pressure = getFullChargePressure();
        fullChargeUseWeight += amount * pressure;
    }

    private boolean canUseSystem() {
        if (ship == null || system == null) return false;
        if (!ship.isAlive() || ship.isHulk() || ship.isPhased()) return false;
        if (system.isOutOfAmmo() || system.getAmmo() <= 0) return false;
        if (system.isActive() || system.isOn() || system.isChargeup() || system.isChargedown()) return false;
        if (system.isCoolingDown() || system.getCooldownRemaining() > 0f) return false;
        return system.canBeActivated();
    }

    private boolean isAtFullCharges() {
        return system.getMaxAmmo() > 0 && system.getAmmo() >= system.getMaxAmmo();
    }

    private boolean shouldSpendFullChargeFromOverflow() {
        if (fullChargeElapsed >= FULL_CHARGE_FORCE_USE_AFTER) return true;
        return fullChargeUseWeight >= 1f;
    }

    private float getFullChargePressure() {
        if (fullChargeElapsed <= FULL_CHARGE_GRACE_PERIOD) return 0f;
        float rampTime = FULL_CHARGE_FORCE_USE_AFTER - FULL_CHARGE_GRACE_PERIOD;
        if (rampTime <= 0f) return 1f;
        return Math.min(1f, (fullChargeElapsed - FULL_CHARGE_GRACE_PERIOD) / rampTime);
    }

    private boolean shouldUseForTravelOrRetreat(ShipAPI target) {
        if (ship.isRetreating()) return true;
        if (hasFlag(AIFlags.RUN_QUICKLY) || hasFlag(AIFlags.TURN_QUICKLY)) return true;
        if (hasFlag(AIFlags.MOVEMENT_DEST) && target == null && !ship.areSignificantEnemiesInRange()) return true;
        if (hasFlag(AIFlags.MANEUVER_TARGET) && target == null && !ship.areSignificantEnemiesInRange()) return true;
        return false;
    }

    private boolean shouldUseLikeTemporalShell(Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (missileDangerDir != null || collisionDangerDir != null) return true;

        FluxTrackerAPI flux = ship.getFluxTracker();
        if (flux != null) {
            if (flux.isOverloadedOrVenting()) return true;
            if (flux.getFluxLevel() >= HIGH_FLUX_THRESHOLD) return true;
        }

        if (hasFlag(AIFlags.SAFE_VENT) || hasFlag(AIFlags.OK_TO_CANCEL_SYSTEM_USE_TO_VENT)) return true;
        if (hasFlag(AIFlags.HAS_INCOMING_DAMAGE) || hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)) return true;
        if (hasFlag(AIFlags.BACK_OFF) || hasFlag(AIFlags.BACKING_OFF)) return true;

        if (target != null && target.isAlive() && !target.isHulk()) {
            float distance = Misc.getDistance(ship.getLocation(), target.getLocation());
            boolean targetInRange = distance <= TARGET_RANGE + ship.getCollisionRadius() + target.getCollisionRadius();
            if (!targetInRange) return false;

            if (hasFlag(AIFlags.PURSUING) || hasFlag(AIFlags.IN_ATTACK_RUN)) return true;
            if (hasFlag(AIFlags.MAINTAINING_STRIKE_RANGE)) return true;
            if (ship.areSignificantEnemiesInRange()) return true;

            FluxTrackerAPI targetFlux = target.getFluxTracker();
            if (targetFlux != null && targetFlux.isOverloadedOrVenting()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasFlag(AIFlags flag) {
        return flags != null && flags.hasFlag(flag);
    }
}
