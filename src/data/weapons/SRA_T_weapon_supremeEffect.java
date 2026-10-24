package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.Plugin.SRA_explosionPlugin;
import data.utils.SRA_BoxUtilFX;
import org.lazywizard.lazylib.combat.AIUtils;
import org.magiclib.util.MagicAnim;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import data.utils.SRA_Colors;

import java.awt.*;
import java.util.*;
import java.util.List;

public class SRA_T_weapon_supremeEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin{
    boolean AmmoonlyOnce = false;
    private static final Color CHARGEUP_PARTICLE_COLOR = new Color(131, 131, 239, 255);
    private static final Color MUZZLE_FLASH_COLOR = new Color(100, 100, 239, 200);
    private final IntervalUtil interval = new IntervalUtil(0.015F, 0.015F);
    private float lastChargeLevel = 0.0F;
    private int lastWeaponAmmo = 0;
    private boolean shot = false;
    boolean sound = false;
    protected CombatEntityAPI Entity;
    protected SRA_explosionPlugin Plugin;

    public SRA_T_weapon_supremeEffect() {
    }

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused())
            return;
        if (!weapon.getShip().isAlive()){
            return;
        }
        ShipAPI ship = weapon.getShip();

//        if (!ship.hasListenerOfClass(SpicaRangeModifier.class)){
//            ship.addListener(new SpicaRangeModifier());
//        }

        float chargelevel = weapon.getChargeLevel();

        if(chargelevel > 0){
            ship.addAfterimage(SRA_Colors.SRA_BLUE_EXPLOSION, 0, 0, -ship.getVelocity().x, -ship.getVelocity().y, 5 * chargelevel, 0, 0f, 0.2f * chargelevel, false, false, false);
            Vector2f partstartloc = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * MathUtils.getRandomNumberInRange(0.1F, 0.8F), 180.0F);
            Vector2f partvec = Vector2f.sub(partstartloc, ship.getLocation(), (Vector2f) null);
            partvec.scale(1.5F);
            float size = MathUtils.getRandomNumberInRange(1.0F, 5.0F);
            float damage = MathUtils.getRandomNumberInRange(0.6F, 2.0F);
            float brightness = MathUtils.getRandomNumberInRange(0.5F, 1.2F);
            SRA_BoxUtilFX.addSmoothParticle(partstartloc, partvec, size, brightness, damage,
                    SRA_Colors.SRA_BLUE_EXPLOSION);
        }

        //特效
        boolean charging = weapon.getChargeLevel() > 0.5 && weapon.getCooldownRemaining() <= 0;
        if (charging && Entity == null) {
            Plugin = new SRA_explosionPlugin(weapon);
            Entity = Global.getCombatEngine().addLayeredRenderingPlugin(Plugin);
        } else if (!charging && Entity != null) {
            Entity = null;
            Plugin = null;
        }
        if (!engine.isPaused() && charging) {
            float chargeLevel = weapon.getChargeLevel();
            int weaponAmmo = weapon.getAmmo();
            if (!(chargeLevel > this.lastChargeLevel) && weaponAmmo >= this.lastWeaponAmmo) {
                this.shot = false;
            } else {
                Vector2f weaponLocation = weapon.getLocation();
                float shipFacing = weapon.getCurrAngle();
                Vector2f shipVelocity = ship.getVelocity();
                Vector2f muzzleLocation = MathUtils.getPointOnCircumference(weaponLocation, weapon.getSlot().isHardpoint() ? 28.5F : 30.0F, shipFacing);
                this.interval.advance(amount);
                int i;
                float oppositeAngle;
                if (this.interval.intervalElapsed() && weapon.isFiring()) {
                    i = (int) (20.0F * chargeLevel);
                    for (i = 0; i < i; ++i) {
                        oppositeAngle = MathUtils.getRandomNumberInRange(20.0F, 125.0F);
                        float size = MathUtils.getRandomNumberInRange(5.0F, 10.0F);
                        float angle = MathUtils.getRandomNumberInRange(-180.0F, 180.0F);
                        Vector2f spawnLocation = MathUtils.getPointOnCircumference(muzzleLocation, oppositeAngle, angle + shipFacing);
                        float speed = oppositeAngle / 0.75F;
                        Vector2f particleVelocity = MathUtils.getPointOnCircumference(shipVelocity, speed, 180.0F + angle + shipFacing);
                        SRA_BoxUtilFX.addHitParticle(spawnLocation, particleVelocity, size,
                                0.8F * weapon.getChargeLevel(), 0.75F, CHARGEUP_PARTICLE_COLOR);
                    }
                    Vector2f point1 = MathUtils.getRandomPointInCircle(muzzleLocation, (float) Math.random() * weapon.getChargeLevel() * 125.0F + 50.0F);
                    engine.spawnEmpArc(ship, muzzleLocation, new SimpleEntity(muzzleLocation), new SimpleEntity(point1), DamageType.ENERGY, 0.0F, 0.0F, 1000.0F, (String) null, weapon.getChargeLevel() * 5.0F + 5.0F, CHARGEUP_PARTICLE_COLOR, CHARGEUP_PARTICLE_COLOR);
                }

                if (!this.shot && weaponAmmo < this.lastWeaponAmmo) {
                    SRA_BoxUtilFX.spawnImpactExplosion(muzzleLocation, MUZZLE_FLASH_COLOR,
                            250.0F, 0.75F);
                    SRA_BoxUtilFX.addSmoothParticle(muzzleLocation, shipVelocity, 750.0F,
                            1.0F, 0.5F, MUZZLE_FLASH_COLOR);

                    for (i = 0; i < 5; ++i) {
                        Vector2f point1 = MathUtils.getRandomPointInCircle(muzzleLocation, (float) Math.random() * 150.0F + 150.0F);
                        engine.spawnEmpArc(ship, muzzleLocation, new SimpleEntity(muzzleLocation), new SimpleEntity(point1), DamageType.ENERGY, 0.0F, 0.0F, 1000.0F, (String) null, weapon.getChargeLevel() * 15.0F + 15.0F, CHARGEUP_PARTICLE_COLOR, MUZZLE_FLASH_COLOR);
                    }

                    //空间扭曲
                    RippleDistortion ripple = new RippleDistortion(muzzleLocation, ship.getVelocity());
                    ripple.setSize(250.0F);
                    ripple.setIntensity(25.0F);
                    ripple.setFrameRate(120.0F);
                    ripple.fadeInSize(0.5F);
                    ripple.fadeOutIntensity(0.5F);
                    DistortionShader.addDistortion(ripple);

                } else {
                    this.shot = false;
                }
            }
            this.lastChargeLevel = chargeLevel;
            this.lastWeaponAmmo = weaponAmmo;
        }
    }

    public void onFire (DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (Plugin != null) {
            Plugin.attachToProjectile(projectile);
            Plugin = null;
        }
    }
}
