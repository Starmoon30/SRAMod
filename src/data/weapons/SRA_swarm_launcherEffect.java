package data.weapons;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;

public class SRA_swarm_launcherEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    private static final String WING_ID = "SRA_swarm_launcher_wing";
    private static final float INITIAL_SPAWN_DELAY = 1f;
    private static final float MAX_SWARM_LIFETIME = 120f;
    private static final float IMPACT_VOLUME_MULT = 0.33f;

    private final List<LaunchedSwarm> launched = new ArrayList<LaunchedSwarm>();
    private float elapsed = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI source = weapon.getShip();
        if (source == null) return;

        elapsed += amount;
        advanceLaunchedSwarms(amount, engine);

        boolean canFire = elapsed >= INITIAL_SPAWN_DELAY
                && (!weapon.usesAmmo() || weapon.getAmmo() > 0);

        weapon.setForceDisabled(!canFire);

        if (canFire) {
            weapon.setForceFireOneFrame(true);
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        CombatFleetManagerAPI manager = engine.getFleetManager(projectile.getOwner());
        if (manager == null) {
            engine.removeEntity(projectile);
            return;
        }

        manager.setSuppressDeploymentMessages(true);
        ShipAPI spawned = manager.spawnShipOrWing(WING_ID, projectile.getLocation(), projectile.getFacing(), 0f, null);
        manager.setSuppressDeploymentMessages(false);

        if (spawned != null) {
            FighterWingAPI wing = spawned.getWing();
            if (wing != null && projectile.getSource() != null) {
                wing.setSourceShip(projectile.getSource());
            }

            Vector2f launchVelocity = Misc.getUnitVectorAtDegreeAngle(projectile.getFacing());
            launchVelocity.scale(spawned.getMaxSpeed());

            List<ShipAPI> members = new ArrayList<ShipAPI>();
            if (wing != null) {
                members.addAll(wing.getWingMembers());
            } else {
                members.add(spawned);
            }

            for (ShipAPI member : members) {
                member.setExplosionScale(0.5f);
                member.setHulkChanceOverride(0f);
                member.setImpactVolumeMult(IMPACT_VOLUME_MULT);
                Vector2f.add(member.getVelocity(), launchVelocity, member.getVelocity());
            }

            launched.add(new LaunchedSwarm(wing, members, projectile.getOwner()));
        }

        engine.removeEntity(projectile);
    }

    private void advanceLaunchedSwarms(float amount, CombatEngineAPI engine) {
        Iterator<LaunchedSwarm> iter = launched.iterator();
        while (iter.hasNext()) {
            LaunchedSwarm swarm = iter.next();
            swarm.elapsed += amount;

            removeDeadMembers(swarm, engine);
            if (swarm.members.isEmpty()) {
                removeWing(swarm, engine);
                iter.remove();
                continue;
            }

            if (swarm.elapsed >= MAX_SWARM_LIFETIME) {
                expireSwarm(swarm, engine);
                iter.remove();
            }
        }
    }

    private void removeDeadMembers(LaunchedSwarm swarm, CombatEngineAPI engine) {
        Iterator<ShipAPI> iter = swarm.members.iterator();
        while (iter.hasNext()) {
            ShipAPI member = iter.next();
            if (member == null || !engine.isInPlay(member) || !member.isAlive()) {
                iter.remove();
            }
        }
    }

    private void expireSwarm(LaunchedSwarm swarm, CombatEngineAPI engine) {
        for (ShipAPI member : swarm.members) {
            if (member == null || !engine.isInPlay(member)) continue;

            engine.spawnExplosion(member.getLocation(), member.getVelocity(),
                    new Color(130, 225, 255, 160), 60f, 0.25f);
            engine.removeEntity(member);
        }
        removeWing(swarm, engine);
        swarm.members.clear();
    }

    private void removeWing(LaunchedSwarm swarm, CombatEngineAPI engine) {
        if (swarm.wing == null) return;

        CombatFleetManagerAPI manager = engine.getFleetManager(swarm.owner);
        if (manager != null) {
            manager.removeDeployed(swarm.wing, false);
        }
    }

    private static class LaunchedSwarm {
        private final FighterWingAPI wing;
        private final List<ShipAPI> members;
        private final int owner;
        private float elapsed = 0f;

        private LaunchedSwarm(FighterWingAPI wing, List<ShipAPI> members, int owner) {
            this.wing = wing;
            this.members = members;
            this.owner = owner;
        }
    }
}
