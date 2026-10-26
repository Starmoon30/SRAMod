package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.utils.SRA_BoxUtilFX;
import data.utils.SRAI18nUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class SRA_Adjudicator_systemStats extends BaseShipSystemScript implements DamageDealtModifier {
    public static final float FLUX_DISSIPATION_MULT = 3f;
    public static final float FLUX_PER_ARC = 1000f;
    public static final float RANGE = 2560f;
    public static final float MAX_DURATION = 4f;
    private static final String ARC_KEY = "SRA_Adjudicator_system";
    private static final String IMPACT_KEY = ARC_KEY + "_impact";
    private static final Color INVISIBLE = new Color(0, 0, 0, 0);
    private static final Color FRINGE = new Color(135, 180, 255, 255);
    private static final Color CORE = new Color(245, 250, 255, 255);
    private float elapsed;
    private float spentFlux;
    private float decorationTimer;
    private ShipAPI source;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || !(stats.getEntity() instanceof ShipAPI)) {
            return;
        }
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (state != State.IN && state != State.ACTIVE && state != State.OUT) {
            return;
        }
        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
        ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
        if (ship.getShield() != null && ship.getShield().isOn()) {
            ship.getShield().toggleOff();
        }
        if (state != State.ACTIVE) {
            return;
        }
        if (!ship.isAlive() || ship.getFluxTracker().isOverloadedOrVenting()) {
            ship.getSystem().deactivate();
            return;
        }
        source = ship;
        if (!ship.hasListener(this)) {
            ship.addListener(this);
        }
        float amount = Math.min(Math.max(0f, MAX_DURATION - elapsed),
                engine.getElapsedInLastFrame() * stats.getTimeMult().getModifiedValue());
        elapsed += amount;
        decorationTimer -= amount;
        if (amount > 0f && decorationTimer <= 0f) {
            decorationTimer = 0.12f;
            Vector2f origin = new Vector2f(ship.getLocation());
            for (int arcIndex = 0; arcIndex < 3; arcIndex++) {
                Vector2f end = MathUtils.getPointOnCircumference(origin,
                        MathUtils.getRandomNumberInRange(160f, 360f),
                        MathUtils.getRandomNumberInRange(0f, 360f));
                SRA_BoxUtilFX.addEmpArc(origin, end, 16f, FRINGE, CORE, 0.08f, 0.28f);
            }
        }
        FluxTrackerAPI flux = ship.getFluxTracker();
        float before = flux.getCurrFlux();
        float drainRate = Math.max(0f, stats.getFluxDissipation().getModifiedValue()) * FLUX_DISSIPATION_MULT;
        flux.decreaseFlux(Math.min(before, drainRate * amount));
        spentFlux += Math.max(0f, before - flux.getCurrFlux());
        while (spentFlux >= FLUX_PER_ARC) {
            spentFlux -= FLUX_PER_ARC;
            CombatEntityAPI target = findTarget(engine, ship);
            if (target != null) {
                fireArc(engine, ship, target);
            }
        }
        if (elapsed >= MAX_DURATION || flux.getCurrFlux() <= 0f) {
            ship.getSystem().deactivate();
        }
    }

    private void fireArc(CombatEngineAPI engine, ShipAPI ship, CombatEntityAPI target) {
        Vector2f origin = new Vector2f(ship.getLocation());
        EmpArcEntityAPI arc = engine.spawnEmpArc(ship, origin, ship, target,
                DamageType.ENERGY, 1000f, 500f,
                getRange(ship) + target.getCollisionRadius(), "tachyon_lance_emp_impact",
                0f, INVISIBLE, INVISIBLE);
        if (arc != null) {
            arc.setCustomData(ARC_KEY, Boolean.TRUE);
            if (target instanceof ShipAPI && ((ShipAPI) target).isPhased()) {
                arc.setTargetToShipCenter(origin, (ShipAPI) target);
            }
        }
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage,
                                    Vector2f point, boolean shieldHit) {
        if (source == null || !(param instanceof EmpArcEntityAPI)
                || !Boolean.TRUE.equals(((EmpArcEntityAPI) param).getCustomData().get(ARC_KEY))) {
            return null;
        }
        Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
                source, WeaponAPI.WeaponType.ENERGY, false, damage);
        EmpArcEntityAPI arc = (EmpArcEntityAPI) param;
        if (point != null && !Boolean.TRUE.equals(arc.getCustomData().get(IMPACT_KEY))) {
            arc.setCustomData(IMPACT_KEY, Boolean.TRUE);
            Vector2f impact = new Vector2f(point);
            SRA_BoxUtilFX.addEmpArc(new Vector2f(source.getLocation()), impact,
                    30f, FRINGE, CORE, 0.12f, 0.38f);
            SRA_BoxUtilFX.spawnLightningImpact(impact, FRINGE);
        }
        return ARC_KEY;
    }

    public static float getRange(ShipAPI ship) {
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
    }

    public static CombatEntityAPI findTarget(CombatEngineAPI engine, ShipAPI ship) {
        float range = getRange(ship);
        boolean manual = ship == engine.getPlayerShip() && ship.getShipAI() == null;
        Vector2f aim = manual && ship.getMouseTarget() != null
                ? ship.getMouseTarget() : ship.getLocation();
        CombatEntityAPI nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (ShipAPI candidate : engine.getShips()) {
            if (!isValidTarget(engine, ship, candidate, range)) {
                continue;
            }
            float distance = distanceToEdge(aim, candidate);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        for (MissileAPI candidate : engine.getMissiles()) {
            if (!isValidTarget(engine, ship, candidate, range)) {
                continue;
            }
            float distance = distanceToEdge(aim, candidate);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        if (manual && nearest != null && nearestDistance <= 250f) {
            return nearest;
        }
        if (isValidTarget(engine, ship, ship.getShipTarget(), range)) {
            return ship.getShipTarget();
        }
        return nearest;
    }

    private static boolean isValidTarget(CombatEngineAPI engine, ShipAPI ship,
                                          CombatEntityAPI target, float range) {
        if (target == null || target == ship || target.getOwner() < 0
                || target.getOwner() == 100 || target.getOwner() == ship.getOwner()
                || target.isExpired() || !engine.isEntityInPlay(target)
                || !engine.isAwareOf(ship.getOwner(), target)) {
            return false;
        }
        if (target instanceof ShipAPI) {
            ShipAPI targetShip = (ShipAPI) target;
            if (!targetShip.isAlive() || targetShip.isHulk() || targetShip.isShuttlePod()) {
                return false;
            }
        } else if (target.getCollisionClass() == CollisionClass.NONE) {
            return false;
        }
        return distanceToEdge(ship.getLocation(), target) <= range;
    }

    private static float distanceToEdge(Vector2f location, CombatEntityAPI target) {
        float deltaX = location.x - target.getLocation().x;
        float deltaY = location.y - target.getLocation().y;
        return Math.max(0f, (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY)
                - target.getCollisionRadius());
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        elapsed = 0f;
        spentFlux = 0f;
        decorationTimer = 0f;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (ship == null || !ship.isAlive()) {
            return false;
        }
        if (system != null && system.isActive()) {
            return true;
        }
        return !ship.isPhased() && !ship.getFluxTracker().isOverloadedOrVenting()
                && ship.getFluxTracker().getCurrFlux() >= FLUX_PER_ARC;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) return new StatusData(SRAI18nUtil.getShipSystemString("SRA_Adjudicator_systemInfo"), false);
        return null;
    }
}
