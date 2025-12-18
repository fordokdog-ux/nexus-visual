package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.Render3D;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import dev.simplevisuals.client.util.perf.Perf;

public class Trails extends Module implements ThemeManager.ThemeChangeListener {

    private final NumberSetting length = new NumberSetting("setting.trails.length", 20, 5, 200, 1);
    private final BooleanSetting useThemeColor = new BooleanSetting("Цвет от темы", false);
    private final ColorSetting color = new ColorSetting("Цвет", new Color(120, 200, 255, 255).getRGB());

    private record TrailPoint(Vec3d pos, long timeMs) {}

    private static final long DEFAULT_LIFETIME_MS = 500L;

    private final ThemeManager themeManager;
    private final Map<PlayerEntity, Deque<TrailPoint>> trails = new IdentityHashMap<>();

    public Trails() {
        super("Trails", Category.Render, I18n.translate("module.trails.description"));
        this.themeManager = ThemeManager.getInstance();
        themeManager.addThemeChangeListener(this);

        getSettings().add(length);
        getSettings().add(useThemeColor);
        getSettings().add(color);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) { }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        try (var __ = Perf.scopeCpu("Trails.onRender3D")) {
            Render3D.prepare();
            float tickDelta = Render3D.getTickDelta();
            Color base = useThemeColor.getValue() ? ThemeManager.getInstance().getCurrentTheme().getBackgroundColor() : color.getColor();
            int baseR = base.getRed();
            int baseG = base.getGreen();
            int baseB = base.getBlue();
            int baseA = 255;
            int maxPoints = length.getValue().intValue();
            float edgeWidth = Math.max(0.5f, Math.min(6.0f, 0.5f * 0.4f));

            long now = System.currentTimeMillis();

            for (PlayerEntity p : mc.world.getPlayers()) {
                // Only show trails for the local player
                if (p != mc.player) continue;
                
                // Skip if we're in first person view
                if (mc.options.getPerspective().isFirstPerson()) continue;

                Deque<TrailPoint> q = trails.computeIfAbsent(p, k -> new ArrayDeque<>());

                // Remove expired points (fixed lifetime)
                while (!q.isEmpty() && now - q.peekFirst().timeMs > DEFAULT_LIFETIME_MS) q.removeFirst();

                double h = p.getEyeHeight(p.getPose());
                Vec3d origin = p.getLerpedPos(tickDelta);
                Vec3d originTop = origin.add(0.0, h, 0.0);

                if (q.isEmpty() || q.getLast().pos.squaredDistanceTo(origin) > 0.0001) {
                    q.addLast(new TrailPoint(origin, now));
                    while (q.size() > maxPoints) q.removeFirst();
                }

                if (q.size() >= 2) {
                    List<TrailPoint> list = new ArrayList<>(q);
                    int sz = list.size();

                    for (int i = 0; i < sz - 1; i++) {
                        TrailPoint a = list.get(i);
                        TrailPoint b = list.get(i + 1);

                        // Reverse fade: newer segments (closer to end) are more transparent
                        float tA = (float) i / (float) (sz - 1);
                        float tB = (float) (i + 1) / (float) (sz - 1);
                        int ageA = (int) Math.min(255, Math.max(0, baseA * tA));
                        int ageB = (int) Math.min(255, Math.max(0, baseA * tB));
                        Color ca = new Color(baseR, baseG, baseB, ageA);
                        Color cb = new Color(baseR, baseG, baseB, ageB);

                        addQuadVertical(a.pos, b.pos, originTop.subtract(origin), ca, cb);
                    }

                    for (int i = 0; i < sz - 1; i++) {
                        TrailPoint a = list.get(i);
                        TrailPoint b = list.get(i + 1);
                        float t = (float) i / (float) (sz - 1);
                        int age = (int) Math.min(255, Math.max(0, baseA * t));
                        int rgba = new Color(baseR, baseG, baseB, age).getRGB();
                        Render3D.drawLine(a.pos, b.pos, rgba, edgeWidth);
                        Render3D.drawLine(a.pos.add(0, h, 0), b.pos.add(0, h, 0), rgba, edgeWidth);
                    }
                }
            }
            Render3D.render();
        }
    }

    private void addQuadVertical(Vec3d aBottom, Vec3d bBottom, Vec3d up, Color ca, Color cb) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        Vec3d aB = aBottom.subtract(cam);
        Vec3d aT = aBottom.add(up).subtract(cam);
        Vec3d bB = bBottom.subtract(cam);
        Vec3d bT = bBottom.add(up).subtract(cam);

        var matrices = new net.minecraft.client.util.math.MatrixStack();
        var matrix = matrices.peek().getPositionMatrix();
        Render3D.Vertex[] vertices = {
                new Render3D.Vertex(matrix, (float) aB.x, (float) aB.y, (float) aB.z, ca.getRGB()),
                new Render3D.Vertex(matrix, (float) aT.x, (float) aT.y, (float) aT.z, ca.getRGB()),
                new Render3D.Vertex(matrix, (float) bT.x, (float) bT.y, (float) bT.z, cb.getRGB()),
                new Render3D.Vertex(matrix, (float) bB.x, (float) bB.y, (float) bB.z, cb.getRGB())
        };
        Render3D.QUADS.add(new Render3D.VertexCollection(vertices));
    }

    @Override
    public void onDisable() {
        trails.clear();
        super.onDisable();
    }
} 