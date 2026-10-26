package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.SRA_Adjudicator_systemStats;
import org.lwjgl.util.vector.Vector2f;

public class SRA_AdjudicatorSystemAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;
    private final IntervalUtil interval = new IntervalUtil(0.1f, 0.2f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine.isPaused() || !ship.isAlive()) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;
        boolean hasTarget = SRA_Adjudicator_systemStats.findTarget(engine, ship) != null;
        if (system.isActive()) {
            if (!hasTarget) system.deactivate();
            return;
        }
        if (!hasTarget || !system.canBeActivated() || ship.isPhased()
                || ship.getFluxTracker().isOverloadedOrVenting()) return;
        if (ship.getFluxTracker().getFluxLevel() > 0.8f) {
            ship.useSystem();
        }
    }
}
