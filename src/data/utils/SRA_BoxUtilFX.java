package data.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.boxutil.util.RenderingUtil;
import org.boxutil.units.standard.entity.SpriteEntity;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.Color;

public final class SRA_BoxUtilFX {
    private static final CombatEngineLayers FX_LAYER = CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER;
    private static final Color WHITE = new Color(255, 255, 255, 255);

    private SRA_BoxUtilFX() {
    }

    public static void addHitParticle(Vector2f location, Vector2f velocity, float size,
                                      float brightness, float duration, Color color) {
        addParticle("graphics/fx/particlealpha32sq.png", location, velocity, size,
                0f, duration, color, brightness);
    }

    public static void addSmoothParticle(Vector2f location, Vector2f velocity, float size,
                                         float brightness, float duration, Color color) {
        addParticle("graphics/fx/particlealpha32sq.png", location, velocity, size,
                duration * 0.1f, duration * 0.15f, color, brightness);
    }

    public static void spawnImpactExplosion(Vector2f location, Color color, float size,
                                            float duration) {
        SpriteAPI particle = Global.getSettings().getSprite("graphics/fx/explosion0.png");
        SpriteAPI ring = Global.getSettings().getSprite("graphics/fx/explosion_ring0.png");
        float particleSize = size * 0.25f;
        int particleCount = Math.max(12, Math.min(72, (int) (size * 0.25f)));

        RenderingUtil.addCombatParticleField(
                location,
                particleCount,
                0f,
                360f,
                null,
                new Vector2f(size * 0.12f, size * 0.45f),
                new Vector2f(0f, 360f),
                new Vector2f(-45f, 45f),
                new Vector4f(particleSize, particleSize, size * 0.55f, size * 0.55f),
                new Vector4f(size * 0.15f, size * 0.15f, size * 0.35f, size * 0.35f),
                color,
                null,
                color,
                null,
                particle,
                null,
                0.02f,
                duration * 0.18f,
                duration * 0.82f,
                duration * 0.15f,
                true,
                FX_LAYER);

        RenderingUtil.addCombatParticleField(
                location,
                1,
                0f,
                360f,
                null,
                new Vector2f(0f, 0f),
                null,
                null,
                new Vector4f(size * 0.55f, size * 0.55f, size * 0.55f, size * 0.55f),
                new Vector4f(size * 0.9f, size * 0.9f, size * 1.4f, size * 1.4f),
                WHITE,
                null,
                color,
                null,
                ring,
                null,
                0f,
                duration * 0.05f,
                duration * 0.95f,
                0f,
                true,
                FX_LAYER);
    }

    public static void spawnExpandingImpactExplosion(Vector2f location, Color color,
                                                     float size, float duration) {
        SpriteAPI halo = Global.getSettings().getSprite("fx", "SRA_supreme_halo");
        SpriteAPI flairs = Global.getSettings().getSprite("fx", "SRA_supreme_sun_flairs");
        SpriteAPI fire = Global.getSettings().getSprite("fx", "SRA_supreme_05fire");
        SpriteAPI glow = Global.getSettings().getSprite("fx", "SRA_supreme_glow");
        SpriteAPI cloud = Global.getSettings().getSprite("fx", "SRA_supreme_cloud");
        SpriteAPI fragments = Global.getSettings().getSprite("fx", "SRA_supreme_fragments");
        Color shockwaveColor = new Color(165, 195, 255, 250);
        Color fireColor = new Color(80, 105, 255, 190);
        Color glowColor = new Color(130, 155, 255, 170);
        Color cloudColor = new Color(60, 85, 220, 105);
        Color fragmentColor = new Color(135, 170, 255, 220);

        RenderingUtil.addCombatParticleField(
                location,
                1,
                0f,
                360f,
                null,
                new Vector2f(0f, 0f),
                null,
                null,
                new Vector4f(size * 0.9f, size * 0.9f, size * 1.25f, size * 1.25f),
                new Vector4f(size * 8f, size * 8f, size * 11f, size * 11f),
                shockwaveColor,
                null,
                shockwaveColor,
                null,
                halo,
                null,
                0.01f,
                0.03f,
                0.45f,
                0f,
                true,
                FX_LAYER);

        RenderingUtil.addCombatParticleField(
                location,
                2,
                0f,
                360f,
                null,
                new Vector2f(size * 0.35f, size * 0.65f),
                new Vector2f(0f, 360f),
                new Vector2f(-140f, 140f),
                new Vector4f(size * 0.85f, size * 0.85f, size * 1.2f, size * 1.2f),
                new Vector4f(size * 5.5f, size * 5.5f, size * 7.5f, size * 7.5f),
                shockwaveColor,
                null,
                shockwaveColor,
                null,
                flairs,
                null,
                0.01f,
                0.03f,
                1.5f,
                0f,
                true,
                FX_LAYER);

        var fireField = RenderingUtil.addCombatParticleField(
                location,
                20,
                0f,
                360f,
                null,
                new Vector2f(size * 0.08f, size * 0.28f),
                new Vector2f(0f, 360f),
                new Vector2f(-80f, 80f),
                new Vector4f(size * 0.18f, size * 0.18f, size * 0.42f, size * 0.42f),
                new Vector4f(size * 0.1f, size * 0.1f, size * 0.28f, size * 0.28f),
                fireColor,
                null,
                fireColor,
                null,
                fire,
                null,
                0.02f,
                duration * 0.2f,
                duration * 1.78f,
                0f,
                true,
                FX_LAYER);
        configureSpriteSheet(fireField == null ? null : fireField.one, 2, 2);

        RenderingUtil.addCombatParticleField(
                location,
                3,
                0f,
                360f,
                null,
                new Vector2f(size * 0.02f, size * 0.08f),
                new Vector2f(0f, 360f),
                new Vector2f(-25f, 25f),
                new Vector4f(size * 0.42f, size * 0.42f, size * 0.72f, size * 0.72f),
                new Vector4f(size * 0.12f, size * 0.12f, size * 0.24f, size * 0.24f),
                glowColor,
                null,
                glowColor,
                null,
                glow,
                null,
                0.03f,
                duration * 0.25f,
                duration * 1.72f,
                0f,
                true,
                FX_LAYER);

        RenderingUtil.addCombatParticleField(
                location,
                10,
                0f,
                360f,
                null,
                new Vector2f(size * 0.04f, size * 0.16f),
                new Vector2f(0f, 360f),
                new Vector2f(-35f, 35f),
                new Vector4f(size * 0.3f, size * 0.3f, size * 0.6f, size * 0.6f),
                new Vector4f(size * 0.08f, size * 0.08f, size * 0.2f, size * 0.2f),
                cloudColor,
                null,
                cloudColor,
                null,
                cloud,
                null,
                0.04f,
                duration * 0.3f,
                4f,
                duration * 0.08f,
                true,
                FX_LAYER);

        RenderingUtil.addCombatParticleField(
                location,
                28,
                0f,
                360f,
                null,
                new Vector2f(size * 6f, size * 12f),
                new Vector2f(0f, 360f),
                new Vector2f(-180f, 180f),
                new Vector4f(size * 0.04f, size * 0.04f, size * 0.12f, size * 0.12f),
                new Vector4f(-size * 0.25f, -size * 0.25f, -size * 0.55f, -size * 0.55f),
                fragmentColor,
                null,
                fragmentColor,
                null,
                fragments,
                null,
                0.005f,
                0.08f,
                0.45f,
                duration * 0.08f,
                true,
                FX_LAYER);
    }

    private static void configureSpriteSheet(SpriteEntity entity, int columns, int rows) {
        if (entity == null) {
            return;
        }
        entity.setTileSize(columns, rows);
        entity.setRandomTile(true);
        entity.setRandomTileEachInstance(true);
        entity.submitEntityData();
    }

    public static void addBeam(Vector2f start, Vector2f end, float width,
                               Color coreColor, Color fringeColor, float full, float fadeOut) {
        float length = Vector2f.sub(end, start, new Vector2f()).length();
        if (length <= 0f) {
            return;
        }
        float facing = (float) Math.toDegrees(Math.atan2(end.y - start.y, end.x - start.x));
        RenderingUtil.addCombatBeamVisual(
                start,
                facing,
                length,
                width,
                coreColor,
                fringeColor,
                Global.getSettings().getSprite("graphics/fx/beamcoreb.png"),
                Global.getSettings().getSprite("graphics/fx/beamfringeb.png"),
                0f,
                full,
                fadeOut,
                true,
                FX_LAYER);
    }

    private static void addParticle(String spritePath, Vector2f location, Vector2f velocity,
                                    float size, float fadeIn, float fadeOut,
                                    Color color, float brightness) {
        Color particleColor = new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                Math.min(255, Math.max(0, Math.round(color.getAlpha() * brightness))));
        SpriteAPI sprite = Global.getSettings().getSprite(spritePath);
        float facing = velocity.lengthSquared() <= 0f
                ? 0f
                : (float) Math.toDegrees(Math.atan2(velocity.y, velocity.x));
        RenderingUtil.addCombatParticleField(
                location,
                1,
                facing,
                0f,
                null,
                new Vector2f(velocity.length(), velocity.length()),
                null,
                new Vector2f(0f, 0f),
                new Vector4f(size, size, size, size),
                null,
                particleColor,
                null,
                particleColor,
                null,
                sprite,
                null,
                fadeIn,
                0f,
                fadeOut,
                0f,
                true,
                FX_LAYER);
    }
}
