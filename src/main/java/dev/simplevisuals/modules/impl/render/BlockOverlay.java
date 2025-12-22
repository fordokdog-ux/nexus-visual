package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.events.impl.EventRender3D;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ColorSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.awt.Color;
import net.minecraft.client.resource.language.I18n;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;
import net.minecraft.client.gl.ShaderProgramKeys;
import dev.simplevisuals.client.util.perf.Perf;
import net.minecraft.util.shape.VoxelShape;
// no collections needed (render shape boxes directly)

public class BlockOverlay extends Module {

    // Настройки обводки
    private final NumberSetting lineWidth = new NumberSetting("setting.lineWidth", 2.0f, 1.0f, 5.0f, 0.1f);
    private final NumberSetting alpha = new NumberSetting("setting.alpha", 150, 0, 255, 1);

    // Настройки заливки
    private final BooleanSetting fill = new BooleanSetting("setting.fill", false, () -> true);
    private final NumberSetting fillAlpha = new NumberSetting("setting.fillAlpha", 50, 0, 255, 1, () -> fill.getValue());

    // Цвет (по умолчанию НЕ от темы)
    private final BooleanSetting useThemeColor = new BooleanSetting("Цвет от темы", false);
    private final ColorSetting color = new ColorSetting("Цвет", new Color(120, 200, 255, 255).getRGB());
    private final ColorSetting colorSecondary = new ColorSetting("Цвет 2", new Color(120, 200, 255, 255).getRGB());


    private final ThemeManager themeManager;


    public BlockOverlay() {
        super("BlockOverlay", Category.Render, I18n.translate("module.blockoverlay.description"));

        this.themeManager = ThemeManager.getInstance();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        try (var __ = Perf.scopeCpu("BlockOverlay.onRender3D")) {
            // Обычная логика для блока под курсором
            HitResult hitResult = mc.crosshairTarget;
            if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHitResult.getBlockPos();

            // Проверяем расстояние с учетом tickdelta для плавного движения
            Vec3d playerPos = mc.player.getLerpedPos(event.getTickDelta());
            Vec3d blockCenter = Vec3d.ofCenter(blockPos);
            double distance = playerPos.distanceTo(blockCenter);

            if (distance > 100.0f) return;

            // Получаем цвета из темы для градиента
            Color overlayColor = getOverlayColor();
            Color overlayColorSecondary = getOverlayColorSecondary();

            // Получаем фактическую форму контура блока и рендерим каждый хитбокс
            VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);

            // Если форма пустая, используем полный блок как запасной вариант
            if (shape.isEmpty()) {
                Box blockBox = new Box(blockPos).expand(0.001);
                renderBlockOverlayDirect(event.getMatrices(), blockBox, overlayColor, overlayColorSecondary, event.getTickDelta());
                return;
            }

            // Рендерим каждый AABB формы напрямую (без лишних аллокаций списка)
            shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                Box b = new Box(minX, minY, minZ, maxX, maxY, maxZ).offset(blockPos).expand(0.001);
                renderBlockOverlayDirect(event.getMatrices(), b, overlayColor, overlayColorSecondary, event.getTickDelta());
            });
        }
    }



    private Color getOverlayColor() {
        Color themeColor = useThemeColor.getValue() ? themeManager.getBackgroundColor() : color.getColor();
        return new Color(
                themeColor.getRed(),
                themeColor.getGreen(),
                themeColor.getBlue(),
                alpha.getValue().intValue()
        );
    }

    private Color getOverlayColorSecondary() {
        Color themeColorSecondary = useThemeColor.getValue() ? themeManager.getSecondaryBackgroundColor() : colorSecondary.getColor();
        return new Color(
                themeColorSecondary.getRed(),
                themeColorSecondary.getGreen(),
                themeColorSecondary.getBlue(),
                alpha.getValue().intValue()
        );
    }

    private void renderBlockOverlayDirect(MatrixStack matrices, Box box, Color overlayColor, Color overlayColorSecondary, float tickDelta) {
        // Настройки для рендеринга сквозь стены
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Рендерим заливку с градиентом
        if (fill.getValue()) {
            Color fillColor = new Color(
                    overlayColor.getRed(),
                    overlayColor.getGreen(),
                    overlayColor.getBlue(),
                    fillAlpha.getValue().intValue()
            );
            Color fillColorSecondary = new Color(
                    overlayColorSecondary.getRed(),
                    overlayColorSecondary.getGreen(),
                    overlayColorSecondary.getBlue(),
                    fillAlpha.getValue().intValue()
            );
            renderBoxFillGradient(matrices, box, fillColor, fillColorSecondary);
        }
        
        // Рендерим обводку с градиентом
        Color outlineColor = new Color(
                overlayColor.getRed(),
                overlayColor.getGreen(),
                overlayColor.getBlue(),
                alpha.getValue().intValue()
        );
        Color outlineColorSecondary = new Color(
                overlayColorSecondary.getRed(),
                overlayColorSecondary.getGreen(),
                overlayColorSecondary.getBlue(),
                alpha.getValue().intValue()
        );
        renderBoxOutlineGradient(matrices, box, outlineColor, outlineColorSecondary, lineWidth.getValue().floatValue());
        
        // Восстанавливаем настройки
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    
    private void renderBoxFill(MatrixStack matrices, Box box, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        
        // Переводим в локальные координаты
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);
        
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;
        
        // Верхняя грань
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        
        // Нижняя грань
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        
        // Передняя грань
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        
        // Задняя грань
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        
        // Левая грань
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        
        // Правая грань
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void renderBoxOutline(MatrixStack matrices, Box box, Color color, float lineWidth) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        
        // Вычисляем расстояние до центра блока
        Vec3d boxCenter = box.getCenter();
        double distance = camera.distanceTo(boxCenter);
        
        // Корректируем толщину линии в зависимости от расстояния
        // Чем дальше объект, тем тоньше линия (но не меньше 0.5)
        float adjustedLineWidth = Math.max(0.5f, lineWidth * (float)(10.0 / Math.max(distance, 1.0)));
        
        // Устанавливаем скорректированную ширину линии ПЕРЕД рендерингом
        GL11.glLineWidth(adjustedLineWidth);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        // Переводим в локальные координаты
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);
        
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;
        
        // Нижнее основание
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        
        // Верхнее основание
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        
        // Вертикальные ребра
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void renderBoxFillGradient(MatrixStack matrices, Box box, Color startColor, Color endColor) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        
        // Переводим в локальные координаты
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);
        
        float r1 = startColor.getRed() / 255.0f;
        float g1 = startColor.getGreen() / 255.0f;
        float b1 = startColor.getBlue() / 255.0f;
        float a1 = startColor.getAlpha() / 255.0f;
        
        float r2 = endColor.getRed() / 255.0f;
        float g2 = endColor.getGreen() / 255.0f;
        float b2 = endColor.getBlue() / 255.0f;
        float a2 = endColor.getAlpha() / 255.0f;
        
        // Верхняя грань (градиент от startColor к endColor)
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        
        // Нижняя грань (градиент от endColor к startColor)
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        
        // Передняя грань (вертикальный градиент)
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        
        // Задняя грань (вертикальный градиент)
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        
        // Левая грань (вертикальный градиент)
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        
        // Правая грань (вертикальный градиент)
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void renderBoxOutlineGradient(MatrixStack matrices, Box box, Color startColor, Color endColor, float lineWidth) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        
        // Вычисляем расстояние до центра блока
        Vec3d boxCenter = box.getCenter();
        double distance = camera.distanceTo(boxCenter);
        
        // Корректируем толщину линии в зависимости от расстояния
        float adjustedLineWidth = Math.max(0.5f, lineWidth * (float)(10.0 / Math.max(distance, 1.0)));
        
        // Устанавливаем скорректированную ширину линии ПЕРЕД рендерингом
        GL11.glLineWidth(adjustedLineWidth);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        // Переводим в локальные координаты
        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);
        
        float r1 = startColor.getRed() / 255.0f;
        float g1 = startColor.getGreen() / 255.0f;
        float b1 = startColor.getBlue() / 255.0f;
        float a1 = startColor.getAlpha() / 255.0f;
        
        float r2 = endColor.getRed() / 255.0f;
        float g2 = endColor.getGreen() / 255.0f;
        float b2 = endColor.getBlue() / 255.0f;
        float a2 = endColor.getAlpha() / 255.0f;
        
        // Нижнее основание (градиент)
        buffer.vertex(matrix, minX, minY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r1, g1, b1, a1);
        
        // Верхнее основание (градиент)
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        
        // Вертикальные ребра (вертикальный градиент)
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);
        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}