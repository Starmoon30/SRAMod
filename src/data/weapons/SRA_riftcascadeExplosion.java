package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual.NEParams;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

final class SRA_riftcascadeExplosion {
    private SRA_riftcascadeExplosion() {
    }

    static void spawn(BeamAPI beam, float radius, float thickness,
                      CollisionClass collisionClass) {
        if (beam == null || beam.getTo() == null) {
            return;
        }
        spawn(beam, new Vector2f(beam.getTo()), radius, thickness, collisionClass);
    }

    static void spawn(BeamAPI beam, Vector2f explosionPoint, float radius,
                      float thickness, CollisionClass collisionClass) {
        if (beam == null || beam.getSource() == null || explosionPoint == null) {
            return;
        }

        DamageAPI beamDamage = beam.getDamage();
        float baseDamage = beamDamage == null ? beam.getWeapon().getDamage().getBaseDamage()
                : beamDamage.getBaseDamage();
        if (baseDamage <= 0f) {
            baseDamage = beam.getWeapon().getDamage().getDamage();
        }

        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                0.1f,
                radius,
                radius * 0.5f,
                baseDamage,
                baseDamage * 0.75f,
                collisionClass,
                collisionClass,
                0.5f,
                0.5f,
                0.5f,
                0,
                new Color(0, 0, 0, 0),
                new Color(0, 0, 0, 0));
        spec.setDamageType(beamDamage == null ? DamageType.ENERGY : beamDamage.getType());
        spec.setUseDetailedExplosion(false);
        spec.setSoundSetId("riftcascade_rift");

        DamagingProjectileAPI explosion = Global.getCombatEngine().spawnDamagingExplosion(
                spec, beam.getSource(), explosionPoint);
        if (explosion == null) {
            return;
        }

        if (beamDamage != null) {
            copyDamage(beamDamage, explosion.getDamage());
        }

        NEParams params = RiftCascadeMineExplosion.createStandardRiftParams(
                beam.getFringeColor(), 10f);
        params.thickness = thickness;
        RiftCascadeMineExplosion.spawnStandardRift(explosion, params);
    }

    private static void copyDamage(DamageAPI source, DamageAPI target) {
        target.setDamage(source.getBaseDamage());
        target.setType(source.getType());
        target.setMultiplier(source.getMultiplier());
        target.setFluxComponent(source.getFluxComponent());
        target.setSoftFlux(source.isSoftFlux());
        target.setDpsDuration(source.getDpsDuration());
        target.setForceHardFlux(source.isForceHardFlux());
        target.setMissile(source.isMissile());
        if (source.getStats() != null) {
            target.setStats(source.getStats());
        }
        target.getModifier().applyMods(source.getModifier());
    }
}
