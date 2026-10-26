package data.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.boxutil.util.RenderingUtil;
import org.boxutil.units.standard.entity.SpriteEntity;
import org.boxutil.units.standard.entity.TrailEntity;
import org.boxutil.base.api.ControlDataAPI;
import org.boxutil.base.api.RenderDataAPI;
import org.boxutil.manager.CombatRenderingManager;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class SRA_BoxUtilFX {
    private static final CombatEngineLayers FX_LAYER = CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER;
    private static final Color WHITE = new Color(255, 255, 255, 255);

    private SRA_BoxUtilFX() {
    }

    public static void addEmpArc(Vector2f start, Vector2f end, float width,
                                 Color fringe, Color core, float full, float fadeOut) {
        if (start == null || end == null || Vector2f.sub(end, start, new Vector2f()).lengthSquared() < 1f) {
            return;
        }
        full = Math.max(0f, full);
        fadeOut = Math.max(0.3f - full, fadeOut);
        Vector2f displacement = Vector2f.sub(end, start, new Vector2f());
        List<Vector2f> nodes = createEmpArcNodes(displacement, width);
        float lineWidth = Math.min(width, 4f);
        addArcLayers(start, nodes, lineWidth, fringe, full, fadeOut);
        float length = displacement.length();
        if (length > 80f) {
            float normalX = -displacement.y / length;
            float normalY = displacement.x / length;
            boolean longArc = length > 500f;
            int branchCount = longArc ? 10 : 2;
            for (int branchIndex = 0; branchIndex < branchCount; branchIndex++) {
                float nodeFraction = 0.18f + 0.64f * (branchIndex + ThreadLocalRandom.current().nextFloat()) / branchCount;
                int nodeIndex = Math.max(1, Math.min(nodes.size() - 2, (int) (nodes.size() * nodeFraction)));
                Vector2f branchStart = nodes.get(nodeIndex);
                float remaining = Math.max(0f, length - Vector2f.dot(branchStart, displacement) / length);
                float reach = Math.min(remaining * 0.5f, Math.min(320f, length * 0.2f));
                float sideways = reach * (ThreadLocalRandom.current().nextBoolean() ? 0.9f : -0.9f);
                Vector2f branchDelta = new Vector2f(displacement.x / length * reach + normalX * sideways,
                        displacement.y / length * reach + normalY * sideways);
                Vector2f branchOrigin = Vector2f.add(start, branchStart, new Vector2f());
                List<Vector2f> branchNodes = createEmpArcNodes(branchDelta, width * 0.25f);
                if (longArc) {
                    // Scale the entire shape, including its lateral bends.
                    for (Vector2f node : branchNodes) node.scale(0.5f);
                }
                float branchFadeOut = Math.max(0.3f - full, fadeOut * 0.85f);
                addArcLayers(branchOrigin, branchNodes, Math.max(1f, lineWidth * 0.35f),
                        fringe, full, branchFadeOut);
            }
        }
    }

    private static void addArcLayers(Vector2f origin, List<Vector2f> nodes, float coreWidth,
                                     Color fringe, float full, float fadeOut) {
        // Vanilla draws fringe and core separately. Keep a narrow white core over
        // a broader, fully opaque blue fringe instead of tinting both in one pass.
        addArcTrail(origin, nodes, coreWidth * 4f, "SRA_lightning_fringe", fringe, full, fadeOut);
        addArcTrail(origin, nodes, coreWidth, "SRA_lightning_core", WHITE, full, fadeOut);
    }

    private static void addArcTrail(Vector2f origin, List<Vector2f> nodes, float width,
                                    String spriteKey, Color color, float full, float fadeOut) {
        TrailEntity arc = new TrailEntity();
        SpriteAPI texture = Global.getSettings().getSprite("fx", spriteKey);
        arc.getMaterialData().setDiffuse(texture);
        arc.getMaterialData().setEmissive(texture);
        arc.getMaterialData().setColor(color);
        arc.getMaterialData().setEmissiveColor(color);
        arc.getMaterialData().setAlphaToEmissive(0f);
        arc.getMaterialData().setAdditionEmissive(true);
        arc.getMaterialData().setIgnoreIllumination(true);
        arc.setLocation(new Vector2f(origin));
        // TrailEntity clears its node list on removal; layers must not share it.
        arc.setNodes(new ArrayList<>(nodes));
        arc.setStripLineMode(true);
        arc.setStartWidth(width);
        arc.setEndWidth(width);
        arc.setJitterPower(0.1f);
        arc.setFlick(false);
        // Native EMP rendering repeats these beam textures every 128 world units.
        arc.setTexturePixels(128f);
        arc.setTextureSpeed(0f);
        float taper = Math.max(0f, 1f - 4f / Math.max(1f, nodes.get(0).length()));
        arc.setFillStartAlpha(0f);
        arc.setFillEndAlpha(0f);
        arc.setFillStartFactor(taper);
        arc.setFillEndFactor(taper);
        arc.setAdditiveBlend();
        arc.setGlobalTimer(0f, full, fadeOut);
        arc.setControlData(new ArcBrightnessControl(color, full + fadeOut));
        arc.setLayer(FX_LAYER);
        arc.setNodeRefreshAllFromCurrentIndex();
        arc.submitNodes();
        arc.setNodeRenderingCount(arc.getNodes().size());
        CombatRenderingManager.addEntity(arc);
    }

    private static final class ArcBrightnessControl implements ControlDataAPI {
        private final float alpha;
        private final float lifetime;
        private volatile float elapsed;

        private ArcBrightnessControl(Color color, float lifetime) {
            this.alpha = color.getAlpha() / 255f;
            this.lifetime = lifetime;
        }

        @Override
        public void controlAdvance(RenderDataAPI entity, float amount) {
            // BoxUtil advances this clock off-thread and stops it while paused.
            elapsed += amount;
        }

        @Override
        public void controlBeforeRenderingAdvance(RenderDataAPI entity, float lastFrameAmount) {
            // Apply material changes on the rendering thread. Each trail has its own
            // clock so adding branches cannot accelerate the shared pulse pattern.
            float age = elapsed;
            float phase = age % 0.18f;
            float brightness;
            if (phase < 0.07f) brightness = 1f;
            else if (phase < 0.09f) brightness = 1f - (phase - 0.07f) * 5f;
            else if (phase < 0.14f) brightness = 0.9f;
            else if (phase < 0.16f) brightness = 0.9f + (phase - 0.14f) * 5f;
            else brightness = 1f;
            // Keep the dark phase visible; reserve the final quarter for fading out.
            float fade = Math.max(0f, Math.min(1f, (lifetime - age) / (lifetime * 0.25f)));
            TrailEntity arc = (TrailEntity) entity;
            arc.getMaterialData().setColorAlpha(alpha * brightness * fade);
            arc.getMaterialData().setEmissiveColorAlpha(alpha * brightness * fade);
        }

        @Override
        public boolean controlAlphaBasedTimer(RenderDataAPI entity) {
            // The lifetime timer still removes the entity; do not apply its fade twice.
            return false;
        }
    }

    static List<Vector2f> createEmpArcNodes(Vector2f displacement, float width) {
        float length = displacement.length();
        List<Vector2f> nodes = new ArrayList<>();
        nodes.add(new Vector2f(displacement));
        if (length >= 1f) {
            int depth = 3;
            while (depth < 7 && length / (1 << depth) > 22f) {
                depth++;
            }
            float amplitude = Math.min(length * 0.14f, Math.max(width * 4f, 30f));
            subdivideArc(nodes, displacement, 1f, 0f, 0f, 0f, amplitude, depth);
        } else {
            nodes.add(new Vector2f());
        }
        return nodes;
    }

    private static void subdivideArc(List<Vector2f> nodes, Vector2f delta,
                                     float startProgress, float startOffset,
                                     float endProgress, float endOffset, float amplitude, int depth) {
        if (depth == 0) {
            float length = delta.length();
            nodes.add(new Vector2f(delta.x * endProgress - delta.y / length * endOffset,
                    delta.y * endProgress + delta.x / length * endOffset));
            return;
        }
        float fraction = 0.35f + ThreadLocalRandom.current().nextFloat() * 0.3f;
        float progress = startProgress + (endProgress - startProgress) * fraction;
        float offset = startOffset + (endOffset - startOffset) * fraction
                + (ThreadLocalRandom.current().nextFloat() * 2f - 1f) * amplitude;
        subdivideArc(nodes, delta, startProgress, startOffset, progress, offset, amplitude * 0.5f, depth - 1);
        subdivideArc(nodes, delta, progress, offset, endProgress, endOffset, amplitude * 0.5f, depth - 1);
    }

    public static void spawnLightningImpact(Vector2f location, Color color) {
        SpriteAPI glow = Global.getSettings().getSprite("fx", "SRA_Adjudicator_system_glow");
        SpriteAPI spark = Global.getSettings().getSprite("fx", "SRA_Adjudicator_system_spark");
        RenderingUtil.addCombatParticleField(
                new Vector2f(location), 3, 0f, 360f, null,
                new Vector2f(10f, 50f), new Vector2f(0f, 360f), new Vector2f(-45f, 45f),
                new Vector4f(90f, 90f, 150f, 150f),
                new Vector4f(220f, 220f, 360f, 360f),
                color, null, color, null, glow, null,
                0.01f, 0.04f, 0.4f, 0f, true, FX_LAYER);
        RenderingUtil.addCombatParticleField(
                new Vector2f(location), 18, 0f, 360f, null,
                new Vector2f(180f, 650f), new Vector2f(0f, 360f), new Vector2f(-180f, 180f),
                new Vector4f(8f, 8f, 20f, 20f),
                new Vector4f(-12f, -12f, -30f, -30f),
                WHITE, null, color, null, spark, null,
                0.005f, 0.02f, 0.4f, 0f, true, FX_LAYER);
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
