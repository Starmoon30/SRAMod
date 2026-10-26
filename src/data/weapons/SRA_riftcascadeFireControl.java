package data.weapons;

import com.fs.starfarer.api.combat.AmmoTrackerAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class SRA_riftcascadeFireControl implements EveryFrameWeaponEffectPlugin {
    private int lastAmmo = -1;
    private float lastReloadProgress;
    private boolean explosionAvailable;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }
        updateAmmo(weapon);
        if (weapon.getShip() == null) {
            return;
        }

        if (weapon.getChargeLevel() <= 0f && !weapon.isFiring()) {
            explosionAvailable = false;
            FluxTrackerAPI flux = weapon.getShip().getFluxTracker();
            if (flux.isOverloadedOrVenting()
                    || flux.getMaxFlux() - flux.getCurrFlux() < weapon.getFluxCostToFire()) {
                weapon.setForceNoFireOneFrame(true);
            }
        }
    }

    private void updateAmmo(WeaponAPI weapon) {
        int ammo = weapon.getAmmo();
        AmmoTrackerAPI tracker = weapon.getAmmoTracker();
        float reloadProgress = tracker.getReloadProgress();
        int reloadedAmmo = 0;
        if (lastAmmo >= 0 && lastAmmo < tracker.getMaxAmmo()
                && tracker.getAmmoPerSecond() > 0f && reloadProgress < lastReloadProgress) {
            reloadedAmmo = Math.min((int) tracker.getReloadSize(), tracker.getMaxAmmo() - lastAmmo);
        }
        if (lastAmmo >= 0 && ammo < lastAmmo + reloadedAmmo) {
            explosionAvailable = true;
        }
        lastAmmo = ammo;
        lastReloadProgress = reloadProgress;
    }

    public static boolean tryTriggerExplosion(WeaponAPI weapon) {
        if (weapon == null || !(weapon.getEffectPlugin() instanceof SRA_riftcascadeFireControl)) {
            return false;
        }
        SRA_riftcascadeFireControl control = (SRA_riftcascadeFireControl) weapon.getEffectPlugin();
        control.updateAmmo(weapon);
        if (!control.explosionAvailable) {
            return false;
        }
        control.explosionAvailable = false;
        return true;
    }
}
