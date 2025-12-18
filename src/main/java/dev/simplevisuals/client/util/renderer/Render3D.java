package dev.simplevisuals.client.util.renderer;

import dev.simplevisuals.mixin.accessors.IWorldRenderer;
import dev.simplevisuals.client.util.Wrapper;
import lombok.experimental.UtilityClass;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

@UtilityClass
public class Render3D implements Wrapper {

	public enum VividMode {
		SOFT
	}

	public boolean rendering = false;

	public List<VertexCollection> QUADS = new ArrayList<>();
	public List<VertexCollection> DEBUG_LINES = new ArrayList<>();
	public List<VertexCollection> SHINE_QUADS = new ArrayList<>();
	public List<VertexCollection> SHINE_DEBUG_LINES = new ArrayList<>();
	public List<VertexCollection> OVERLAY_QUADS = new ArrayList<>();
	public List<VertexCollection> OVERLAY_DEBUG_LINES = new ArrayList<>();
	public List<VertexCollection> BLOCK_OVERLAY_LINES = new ArrayList<>();
	public List<VertexCollection> HITBOX_LINES = new ArrayList<>();

	// Отдельные очереди для CustomHitBox
	public List<VertexCollection> CUSTOM_HITBOX_QUADS = new ArrayList<>();
	public List<VertexCollection> CUSTOM_HITBOX_LINES = new ArrayList<>();

	public static float TICK_DELTA = 0.0f;
	public static void setTickDelta(float tickDelta) { TICK_DELTA = tickDelta; }
	public static float getTickDelta() { return TICK_DELTA; }

	public static float BLOCK_OVERLAY_LINE_WIDTH = 1.0f;
	public static float HITBOX_LINE_WIDTH = 1.0f;

	public static void setBlockOverlayLineWidth(float width) { BLOCK_OVERLAY_LINE_WIDTH = width; }
	public static void setHitboxLineWidth(float width) { HITBOX_LINE_WIDTH = width; }

	public static void drawBlockOverlayLine(Vec3d start, Vec3d end, int color) {
		if (!rendering) return;
		MatrixStack matrices = new MatrixStack();
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		Vec3d cam = mc.gameRenderer.getCamera().getPos();
		Vec3d s = start.subtract(cam);
		Vec3d e = end.subtract(cam);
		BLOCK_OVERLAY_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) s.x, (float) s.y, (float) s.z, color),
				new Vertex(matrix, (float) e.x, (float) e.y, (float) e.z, color)
		));
	}

	public static void drawHitboxLine(Vec3d start, Vec3d end, int color) {
		if (!rendering) return;
		MatrixStack matrices = new MatrixStack();
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		Vec3d cam = mc.gameRenderer.getCamera().getPos();
		Vec3d s = start.subtract(cam);
		Vec3d e = end.subtract(cam);
		HITBOX_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) s.x, (float) s.y, (float) s.z, color),
				new Vertex(matrix, (float) e.x, (float) e.y, (float) e.z, color)
		));
	}

	public static void drawCustomHitboxLine(Vec3d start, Vec3d end, int color) {
		if (!rendering) return;
		MatrixStack matrices = new MatrixStack();
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		Vec3d cam = mc.gameRenderer.getCamera().getPos();
		Vec3d s = start.subtract(cam);
		Vec3d e = end.subtract(cam);
		CUSTOM_HITBOX_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) s.x, (float) s.y, (float) s.z, color),
				new Vertex(matrix, (float) e.x, (float) e.y, (float) e.z, color)
		));
	}

	public static void drawLines(List<VertexCollection> debugLines, float width) {
		if (debugLines.isEmpty()) return;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
		for (VertexCollection collection : debugLines) collection.vertex(buffer);
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
		GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(width);

		// Отключаем depth testing для отображения сквозь стены
		RenderSystem.disableDepthTest();
		BufferRenderer.drawWithGlobalProgram(buffer.end());
		RenderSystem.enableDepthTest();

		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}

	public static void drawLinesWithDepth(List<VertexCollection> debugLines, float width) {
		if (debugLines.isEmpty()) return;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
		for (VertexCollection collection : debugLines) collection.vertex(buffer);
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
		GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(width);

		// Включаем depth testing для НЕ отображения сквозь стены
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(true); // Важно: записываем в depth buffer
		BufferRenderer.drawWithGlobalProgram(buffer.end());

		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}

	public static void drawQuadsWithDepth(List<VertexCollection> quads, boolean shine) {
		if (quads.isEmpty()) return;

		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(true); // Важно: записываем в depth buffer

		RenderSystem.enableBlend();
		if (shine) RenderSystem.blendFunc(770, 32772);
		else RenderSystem.blendFuncSeparate(
				GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO
		);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
		for (VertexCollection collection : quads) collection.vertex(buffer);
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
		RenderSystem.disableCull();
		BufferRenderer.drawWithGlobalProgram(buffer.end());
		RenderSystem.enableCull();
	}

	/**
	 * Специальный рендер для CustomHitBox - НЕ виден сквозь стены
	 */
	public static void renderCustomHitBox() {
		// Рендерим заливку с depth testing
		if (!CUSTOM_HITBOX_QUADS.isEmpty()) {
			drawQuadsWithDepth(CUSTOM_HITBOX_QUADS, false);
		}

		// Рендерим обводку с depth testing
		if (!CUSTOM_HITBOX_LINES.isEmpty()) {
			drawLinesWithDepth(CUSTOM_HITBOX_LINES, HITBOX_LINE_WIDTH);
		}
	}

	/**
	 * Специальный рендер для BlockOverlay - виден сквозь стены
	 */
	public static void renderBlockOverlay() {
		// Рендерим заливку сквозь стены
		if (!OVERLAY_QUADS.isEmpty()) {
			drawIgnoringDepth(OVERLAY_QUADS, OVERLAY_DEBUG_LINES, false);
		}

		// Рендерим обводку сквозь стены
		if (!BLOCK_OVERLAY_LINES.isEmpty()) {
			drawLines(BLOCK_OVERLAY_LINES, BLOCK_OVERLAY_LINE_WIDTH);
		}
	}

	/**
	 * Очистка очередей для CustomHitBox
	 */
	public static void clearCustomHitBoxQueues() {
		CUSTOM_HITBOX_QUADS.clear();
		CUSTOM_HITBOX_LINES.clear();
	}

	/**
	 * Очистка очередей для BlockOverlay
	 */
	public static void clearBlockOverlayQueues() {
		OVERLAY_QUADS.clear();
		OVERLAY_DEBUG_LINES.clear();
		BLOCK_OVERLAY_LINES.clear();
	}

	/**
	 * Adds a camera-facing quad ribbon between two points with given half-width and color.
	 */
	public static void drawRibbonSegment(Vec3d a, Vec3d b, float halfWidth, Color color) {
		if (!rendering) return;
		// Compute camera-relative positions
		Vec3d cam = mc.gameRenderer.getCamera().getPos();
		Vec3d pa = a.subtract(cam);
		Vec3d pb = b.subtract(cam);
		Vec3d dir = pb.subtract(pa);
		if (dir.lengthSquared() == 0) return;
		// View direction from camera to midpoint
		Vec3d mid = pa.add(pb).multiply(0.5);
		Vec3d viewDir = mid.normalize();
		// Perpendicular vector in view plane
		Vec3d normal = dir.crossProduct(viewDir);
		if (normal.lengthSquared() == 0) return;
		normal = normal.normalize().multiply(halfWidth);
		// Quad vertices: a +/- n, b +/- n
		Vec3d aL = pa.subtract(normal);
		Vec3d aR = pa.add(normal);
		Vec3d bL = pb.subtract(normal);
		Vec3d bR = pb.add(normal);

		MatrixStack matrices = new MatrixStack();
		Matrix4f matrix = matrices.peek().getPositionMatrix();
		int rgba = color.getRGB();
		QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) aL.x, (float) aL.y, (float) aL.z, rgba),
				new Vertex(matrix, (float) aR.x, (float) aR.y, (float) aR.z, rgba),
				new Vertex(matrix, (float) bR.x, (float) bR.y, (float) bR.z, rgba),
				new Vertex(matrix, (float) bL.x, (float) bL.y, (float) bL.z, rgba)
		));
	}

	public static void drawTexture(MatrixStack matrices, float x0, float y0, float z0,
								   float x1, float y1, float z1, Identifier texture, Color color) {

		RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
				GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO
		);
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

		float r = color.getRed()   / 255.0f;
		float g = color.getGreen() / 255.0f;
		float b = color.getBlue()  / 255.0f;
		float a = color.getAlpha() / 255.0f;

		buffer.vertex(matrix, x0, y0, z0).texture(0.0f, 1.0f).color(r, g, b, a);
		buffer.vertex(matrix, x1, y0, z0).texture(1.0f, 1.0f).color(r, g, b, a);
		buffer.vertex(matrix, x1, y0, z1).texture(1.0f, 0.0f).color(r, g, b, a);
		buffer.vertex(matrix, x0, y0, z1).texture(0.0f, 0.0f).color(r, g, b, a);

		BufferRenderer.drawWithGlobalProgram(buffer.end());

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	// Sprite batching for billboard textures to reduce per-quad state changes
	private static boolean spriteBatchActive = false;
	private static BufferBuilder spriteBuffer = null;
	private static boolean spriteHasVertices = false;

	public static void beginBillboardBatch(Identifier texture) {
		if (spriteBatchActive) return;
		RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
				GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO
		);
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);
		Tessellator tess = Tessellator.getInstance();
		spriteBuffer = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
		spriteHasVertices = false;
		spriteBatchActive = true;
	}

	public static void batchBillboard(MatrixStack matrices, Vec3d worldPos, float size, int rgba) {
		if (!spriteBatchActive) return;
		// compute billboard corners similar to drawBillboardTexture
		Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
		Vec3d toCamera = cameraPos.subtract(worldPos).normalize();
		Vec3d worldUp = new Vec3d(0, 1, 0);
		Vec3d right = worldUp.crossProduct(toCamera).normalize().multiply(size);
		Vec3d up = toCamera.crossProduct(right).normalize().multiply(size);

		Vec3d topLeft = worldPos.add(up).subtract(right);
		Vec3d topRight = worldPos.add(up).add(right);
		Vec3d bottomLeft = worldPos.subtract(up).subtract(right);
		Vec3d bottomRight = worldPos.subtract(up).add(right);

		Vec3d cameraPosVec = mc.getEntityRenderDispatcher().camera.getPos();
		topLeft = topLeft.subtract(cameraPosVec);
		topRight = topRight.subtract(cameraPosVec);
		bottomLeft = bottomLeft.subtract(cameraPosVec);
		bottomRight = bottomRight.subtract(cameraPosVec);

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		float r = ((rgba >> 16) & 0xFF) / 255.0f;
		float g = ((rgba >> 8) & 0xFF) / 255.0f;
		float b = (rgba & 0xFF) / 255.0f;
		float a = ((rgba >>> 24) & 0xFF) / 255.0f;

		spriteBuffer.vertex(matrix, (float) bottomLeft.x, (float) bottomLeft.y, (float) bottomLeft.z)
				.texture(0.0f, 1.0f).color(r, g, b, a);
		spriteBuffer.vertex(matrix, (float) bottomRight.x, (float) bottomRight.y, (float) bottomRight.z)
				.texture(1.0f, 1.0f).color(r, g, b, a);
		spriteBuffer.vertex(matrix, (float) topRight.x, (float) topRight.y, (float) topRight.z)
				.texture(1.0f, 0.0f).color(r, g, b, a);
		spriteBuffer.vertex(matrix, (float) topLeft.x, (float) topLeft.y, (float) topLeft.z)
				.texture(0.0f, 0.0f).color(r, g, b, a);
		spriteHasVertices = true;
	}

	public static void endBillboardBatch() {
		if (!spriteBatchActive) return;
		if (spriteHasVertices) {
			BufferRenderer.drawWithGlobalProgram(spriteBuffer.end());
		}
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		spriteBatchActive = false;
		spriteBuffer = null;
		spriteHasVertices = false;
	}

	/**
	 * Рендерит billboard текстуру, которая всегда смотрит на камеру
	 * @param matrices Стек матриц
	 * @param worldPos Позиция в мире (уже учтена камера)
	 * @param size Размер квада
	 * @param texture Текстура
	 * @param color Цвет
	 */
	public static void drawBillboardTexture(MatrixStack matrices, Vec3d worldPos, float size, Identifier texture, Color color) {
		RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
				GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO
		);
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);

		// Получаем позицию камеры
		Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

		// Вычисляем направление от worldPos к камере
		Vec3d toCamera = cameraPos.subtract(worldPos).normalize();

		// Вычисляем right и up векторы для billboard
		Vec3d worldUp = new Vec3d(0, 1, 0);
		Vec3d right = worldUp.crossProduct(toCamera).normalize();
		Vec3d up = toCamera.crossProduct(right).normalize();

		// Масштабируем векторы на размер
		right = right.multiply(size);
		up = up.multiply(size);

		// Вычисляем углы квада
		Vec3d topLeft = worldPos.add(up).subtract(right);
		Vec3d topRight = worldPos.add(up).add(right);
		Vec3d bottomLeft = worldPos.subtract(up).subtract(right);
		Vec3d bottomRight = worldPos.subtract(up).add(right);

		// Переводим в локальные координаты относительно камеры
		Vec3d cameraPosVec = mc.getEntityRenderDispatcher().camera.getPos();
		topLeft = topLeft.subtract(cameraPosVec);
		topRight = topRight.subtract(cameraPosVec);
		bottomLeft = bottomLeft.subtract(cameraPosVec);
		bottomRight = bottomRight.subtract(cameraPosVec);

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

		float r = color.getRed() / 255.0f;
		float g = color.getGreen() / 255.0f;
		float b = color.getBlue() / 255.0f;
		float a = color.getAlpha() / 255.0f;

		// Рендерим квад (против часовой стрелки)
		buffer.vertex(matrix, (float) bottomLeft.x, (float) bottomLeft.y, (float) bottomLeft.z)
				.texture(0.0f, 1.0f).color(r, g, b, a);
		buffer.vertex(matrix, (float) bottomRight.x, (float) bottomRight.y, (float) bottomRight.z)
				.texture(1.0f, 1.0f).color(r, g, b, a);
		buffer.vertex(matrix, (float) topRight.x, (float) topRight.y, (float) topRight.z)
				.texture(1.0f, 0.0f).color(r, g, b, a);
		buffer.vertex(matrix, (float) topLeft.x, (float) topLeft.y, (float) topLeft.z)
				.texture(0.0f, 0.0f).color(r, g, b, a);

		BufferRenderer.drawWithGlobalProgram(buffer.end());

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	/**
	 * Рендерит billboard текстуру с фиксированным углом поворота
	 * @param matrices Стек матриц
	 * @param worldPos Позиция в мире (уже учтена камера)
	 * @param size Размер квада
	 * @param texture Текстура
	 * @param color Цвет
	 * @param fixedRotation Фиксированный угол поворота в градусах
	 */
	public static void drawFixedBillboardTexture(MatrixStack matrices, Vec3d worldPos, float size, Identifier texture, Color color, float fixedRotation) {
		RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
				GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO
		);
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);

		// Получаем позицию камеры
		Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

		// Вычисляем направление от worldPos к камере
		Vec3d toCamera = cameraPos.subtract(worldPos).normalize();

		// Вычисляем right и up векторы для billboard
		Vec3d worldUp = new Vec3d(0, 1, 0);
		Vec3d right = worldUp.crossProduct(toCamera).normalize();
		Vec3d up = toCamera.crossProduct(right).normalize();

		// Применяем фиксированный поворот
		double radians = Math.toRadians(fixedRotation);
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);

		// Поворачиваем векторы right и up
		Vec3d rotatedRight = new Vec3d(
			right.x * cos - right.z * sin,
			right.y,
			right.x * sin + right.z * cos
		);
		Vec3d rotatedUp = new Vec3d(
			up.x * cos - up.z * sin,
			up.y,
			up.x * sin + up.z * cos
		);

		// Масштабируем векторы на размер
		rotatedRight = rotatedRight.multiply(size);
		rotatedUp = rotatedUp.multiply(size);

		// Вычисляем углы квада
		Vec3d topLeft = worldPos.add(rotatedUp).subtract(rotatedRight);
		Vec3d topRight = worldPos.add(rotatedUp).add(rotatedRight);
		Vec3d bottomLeft = worldPos.subtract(rotatedUp).subtract(rotatedRight);
		Vec3d bottomRight = worldPos.subtract(rotatedUp).add(rotatedRight);

		// Переводим в локальные координаты относительно камеры
		Vec3d cameraPosVec = mc.getEntityRenderDispatcher().camera.getPos();
		topLeft = topLeft.subtract(cameraPosVec);
		topRight = topRight.subtract(cameraPosVec);
		bottomLeft = bottomLeft.subtract(cameraPosVec);
		bottomRight = bottomRight.subtract(cameraPosVec);

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

		float r = color.getRed() / 255.0f;
		float g = color.getGreen() / 255.0f;
		float b = color.getBlue() / 255.0f;
		float a = color.getAlpha() / 255.0f;

		// Рендерим квад (против часовой стрелки)
		buffer.vertex(matrix, (float) bottomLeft.x, (float) bottomLeft.y, (float) bottomLeft.z)
				.texture(0.0f, 1.0f).color(r, g, b, a);
		buffer.vertex(matrix, (float) bottomRight.x, (float) bottomRight.y, (float) bottomRight.z)
				.texture(1.0f, 1.0f).color(r, g, b, a);
		buffer.vertex(matrix, (float) topRight.x, (float) topRight.y, (float) topRight.z)
				.texture(1.0f, 0.0f).color(r, g, b, a);
		buffer.vertex(matrix, (float) topLeft.x, (float) topLeft.y, (float) topLeft.z)
				.texture(0.0f, 0.0f).color(r, g, b, a);

		BufferRenderer.drawWithGlobalProgram(buffer.end());

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	/**
	 * Упрощенная версия billboard текстуры с использованием матриц
	 * @param matrices Стек матриц (уже переведен в локальные координаты)
	 * @param size Размер квада
	 * @param texture Текстура
	 * @param color Цвет
	 */
	public static void drawSimpleBillboardTexture(MatrixStack matrices, float size, Identifier texture, Color color) {
		RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
				GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO
		);
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

		float r = color.getRed() / 255.0f;
		float g = color.getGreen() / 255.0f;
		float b = color.getBlue() / 255.0f;
		float a = color.getAlpha() / 255.0f;

		float half = size / 2.0f;

		// Рендерим плоский квад в XY плоскости (будет повернут матрицами)
		buffer.vertex(matrix, -half, -half, 0).texture(0.0f, 1.0f).color(r, g, b, a);
		buffer.vertex(matrix, half, -half, 0).texture(1.0f, 1.0f).color(r, g, b, a);
		buffer.vertex(matrix, half, half, 0).texture(1.0f, 0.0f).color(r, g, b, a);
		buffer.vertex(matrix, -half, half, 0).texture(0.0f, 0.0f).color(r, g, b, a);

		BufferRenderer.drawWithGlobalProgram(buffer.end());

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	public static void renderCube(MatrixStack matrices, Vec3d vec3d, double size,
								  boolean fill, Color fillColor, boolean outline, Color outlineColor) {
		Box box = new Box(
				vec3d.add(-0.5 * size, -0.5 * size, -0.5 * size),
				vec3d.add(0.5 * size, 0.5 * size, 0.5 * size)
		);
		if (outline) renderBoxOutline(matrices, box, outlineColor);
		if (fill) renderBox(matrices, box, fillColor);
	}

	public static void renderCube(MatrixStack matrices, Box box,
								  boolean fill, Color fillColor, boolean outline, Color outlineColor) {
		if (outline) renderBoxOutline(matrices, box, outlineColor);
		if (fill) renderBox(matrices, box, fillColor);
	}

	public enum FillMode {
		OUTLINE,
		FILL,
		BOTH
	}

	public static void renderBox(MatrixStack matrices, Box box, Color color) {
		renderGradientBox(matrices, box, color, color);
	}

	public static void renderCustomHitboxBox(MatrixStack matrices, Box box, Color color) {
		renderGradientCustomHitboxBox(matrices, box, color, color);
	}

	public static void renderBoxOutline(MatrixStack matrices, Box box, Color color) {
		renderGradientBoxOutline(matrices, box, color, color);
	}

	public static void renderBoxOverlay(MatrixStack matrices, Box box, Color color) {
		renderGradientBoxOverlay(matrices, box, color, color);
	}

	public static void renderBoxOutlineOverlay(MatrixStack matrices, Box box, Color color) {
		renderGradientBoxOutlineOverlay(matrices, box, color, color);
	}

	/**
	 * Рендерит хитбокс с заданными цветами. Можно заполнить внутреннюю часть и/или отрисовать контур.
	 * Использует существующую систему батча Render3D (prepare/render).
	 * @param matrices стек матриц
	 * @param box хитбокс (AABB)
	 * @param fillColor цвет заливки (игнорируется, если fill == false)
	 * @param outlineColor цвет контура (игнорируется, если outline == false)
	 * @param fill включить заливку
	 * @param outline включить контур
	 */
	public static void CustomHixBox(MatrixStack matrices, Box box,
									Color fillColor, Color outlineColor,
									boolean fill, boolean outline) {
		if (outline) renderBoxOutline(matrices, box, outlineColor);
		if (fill) renderBox(matrices, box, fillColor);
	}

	/**
	 * Упрощенная версия: отрисовывает и контур, и заливку одним цветом.
	 */
	public static void CustomHixBox(MatrixStack matrices, Box box, Color color) {
		CustomHixBox(matrices, box, color, color, true, true);
	}

	/**
	 * Версия с выбором режима заполнения. Один цвет применяется и к контуру, и к заливке.
	 * @param matrices стек матриц
	 * @param box хитбокс (AABB)
	 * @param color цвет для контура и/или заливки
	 * @param mode режим: OUTLINE, FILL или BOTH
	 */
	public static void CustomHixBox(MatrixStack matrices, Box box, Color color, FillMode mode) {
		switch (mode) {
			case OUTLINE -> renderBoxOutline(matrices, box, color);
			case FILL -> renderBox(matrices, box, color);
			case BOTH -> {
				renderBoxOutline(matrices, box, color);
				renderBox(matrices, box, color);
			}
		}
	}

	/**
	 * Упрощенный «включатель»: подготовка → отрисовка → финальный вывод.
	 * Один цвет применяется и к контуру, и к заливке согласно выбранному режиму.
	 */
	public static void EnableCustomHixBox(MatrixStack matrices, Box box, Color color, FillMode mode) {
		prepare();
		CustomHixBox(matrices, box, color, mode);
		render();
	}

	/**
	 * Включатель с разными цветами и флагами контура/заливки.
	 */
	public static void EnableCustomHixBox(MatrixStack matrices, Box box,
										  Color fillColor, Color outlineColor,
										  boolean fill, boolean outline) {
		prepare();
		CustomHixBox(matrices, box, fillColor, outlineColor, fill, outline);
		render();
	}

	public static void renderGradientBox(MatrixStack matrices, Box box, Color startColor, Color endColor) {
		if (!rendering || !isVisible(box)) return;

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		box = cameraTransform(box);

		QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));

		QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));

		QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB())
		));
	}

	public static void renderGradientCustomHitboxBox(MatrixStack matrices, Box box, Color startColor, Color endColor) {
		if (!rendering || !isVisible(box)) return;

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		box = cameraTransform(box);

		CUSTOM_HITBOX_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		CUSTOM_HITBOX_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));

		CUSTOM_HITBOX_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		CUSTOM_HITBOX_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		CUSTOM_HITBOX_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));

		CUSTOM_HITBOX_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB())
		));
	}

	public static void renderGradientBoxOutline(MatrixStack matrices, Box box, Color startColor, Color endColor) {
		if (!rendering || !isVisible(box)) return;

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		box = cameraTransform(box);

		// Нижнее основание
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB())
		));
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB())
		));
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB())
		));
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB())
		));

		// Верхнее основание
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		// Вертикальные рёбра
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));
		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));
	}

	public static void renderGradientBoxOverlay(MatrixStack matrices, Box box, Color startColor, Color endColor) {
		if (!rendering || !isVisible(box)) return;

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		box = cameraTransform(box);

		OVERLAY_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		OVERLAY_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));

		OVERLAY_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		OVERLAY_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		OVERLAY_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));

		OVERLAY_QUADS.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB())
		));
	}

	public static void renderGradientBoxOutlineOverlay(MatrixStack matrices, Box box, Color startColor, Color endColor) {
		if (!rendering || !isVisible(box)) return;

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		box = cameraTransform(box);

		// Нижнее основание
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB())
		));
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB())
		));
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB())
		));
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB())
		));

		// Верхнее основание
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));

		// Вертикальные рёбра
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ, startColor.getRGB())
		));
		OVERLAY_DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ, endColor.getRGB()),
				new Vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ, startColor.getRGB())
		));
	}

	public static void prepare() {
		QUADS.clear();
		DEBUG_LINES.clear();
		SHINE_QUADS.clear();
		SHINE_DEBUG_LINES.clear();
		OVERLAY_QUADS.clear();
		OVERLAY_DEBUG_LINES.clear();
		BLOCK_OVERLAY_LINES.clear();
		HITBOX_LINES.clear();
		rendering = true;
	}

	public static void render() {
		draw(QUADS, DEBUG_LINES, false);
		draw(SHINE_QUADS, SHINE_DEBUG_LINES, true);
		rendering = false;
	}

	public static void draw(List<VertexCollection> quads, List<VertexCollection> debugLines, boolean shine) {
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);

		RenderSystem.enableBlend();
		if (shine) RenderSystem.blendFunc(770, 32772);
		else RenderSystem.blendFuncSeparate(
				GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO
		);

		Tessellator tessellator = Tessellator.getInstance();

		if (!quads.isEmpty()) {
			quads.sort(Comparator.comparingDouble(VertexCollection::averageDepth).reversed());
			BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
			for (VertexCollection collection : quads) collection.vertex(buffer);
			RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
			RenderSystem.disableCull();
			BufferRenderer.drawWithGlobalProgram(buffer.end());
			RenderSystem.enableCull();
		}

		if (!debugLines.isEmpty()) {
			BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
			for (VertexCollection collection : debugLines) collection.vertex(buffer);
			RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
			GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glLineWidth(DEBUG_LINE_WIDTH);
			BufferRenderer.drawWithGlobalProgram(buffer.end());
			GL11.glDisable(GL11.GL_LINE_SMOOTH);
		}

		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	public static void drawIgnoringDepth(List<VertexCollection> quads, List<VertexCollection> debugLines, boolean shine) {
		RenderSystem.disableDepthTest();

		RenderSystem.enableBlend();
		if (shine) RenderSystem.blendFunc(770, 32772);
		else RenderSystem.blendFuncSeparate(
				GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO
		);

		Tessellator tessellator = Tessellator.getInstance();

		if (!quads.isEmpty()) {
			BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
			for (VertexCollection collection : quads) collection.vertex(buffer);
			RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
			RenderSystem.disableCull();
			BufferRenderer.drawWithGlobalProgram(buffer.end());
			RenderSystem.enableCull();
		}

		if (!debugLines.isEmpty()) {
			BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
			for (VertexCollection collection : debugLines) collection.vertex(buffer);
			RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
			GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glLineWidth(DEBUG_LINE_WIDTH);
			BufferRenderer.drawWithGlobalProgram(buffer.end());
			GL11.glDisable(GL11.GL_LINE_SMOOTH);
		}

		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
	}

	public static float DEBUG_LINE_WIDTH = 1.0f;

	private static boolean isVisible(Box box) {
		return ((IWorldRenderer) mc.worldRenderer).getFrustum().isVisible(box);
	}

	private static Vec3d cameraTransform(Vec3d vec3d) {
		Vec3d camera = mc.gameRenderer.getCamera().getPos();
		Vector4f vec = new Vector4f(
				(float) (vec3d.x - camera.x),
				(float) (vec3d.y - camera.y),
				(float) (vec3d.z - camera.z),
				1.0f
		);
		vec.mul(new MatrixStack().peek().getPositionMatrix());
		return new Vec3d(vec.x(), vec.y(), vec.z());
	}

	private static Box cameraTransform(Box box) {
		Vec3d min = cameraTransform(new Vec3d(box.minX, box.minY, box.minZ));
		Vec3d max = cameraTransform(new Vec3d(box.maxX, box.maxY, box.maxZ));
		return new Box(min, max);
	}
	public static void drawLine(Vec3d start, Vec3d end, int color, float width) {
		if (!rendering) return;

		MatrixStack matrices = new MatrixStack();
		Matrix4f matrix = matrices.peek().getPositionMatrix();

		Vec3d cam = mc.gameRenderer.getCamera().getPos();
		Vec3d s = start.subtract(cam);
		Vec3d e = end.subtract(cam);

		DEBUG_LINES.add(new VertexCollection(
				new Vertex(matrix, (float) s.x, (float) s.y, (float) s.z, color),
				new Vertex(matrix, (float) e.x, (float) e.y, (float) e.z, color)
		));

		// Ширина линии (чтобы реально учитывалось при render())
		GL11.glLineWidth(width);
	}


	public static void drawTextureVivid(MatrixStack matrices, float x0, float y0, float z0,
									   float x1, float y1, float z1, Identifier texture, Color color, float strength) {
		drawTextureVivid(matrices, x0, y0, z0, x1, y1, z1, texture, color, strength, VividMode.SOFT);
	}

	public static void drawTextureVivid(MatrixStack matrices, float x0, float y0, float z0,
                                       float x1, float y1, float z1, Identifier texture, Color color, float strength, VividMode mode) {
		// Усиление цвета: повышаем насыщенность и яркость в HSB, затем используем аддитивное смешение
		float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		float satBoost = switch (mode) {
			case SOFT -> 0.4f + 0.3f * strength;
		};
		float briBoost = switch (mode) {
			case SOFT -> 0.7f + 0.3f * strength;
		};
		float h = hsb[0];
		float s = Math.min(1f, hsb[1] * satBoost);
		float v = Math.min(1f, hsb[2] * briBoost);
		int vividRgb = Color.HSBtoRGB(h, s, v);
		Color vivid = new Color((vividRgb >> 16) & 0xFF, (vividRgb >> 8) & 0xFF, vividRgb & 0xFF, color.getAlpha());

		RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		// Аддитивное смешение, как для SHINE
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(false);

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

		float r = vivid.getRed()   / 255.0f;
		float g = vivid.getGreen() / 255.0f;
		float b = vivid.getBlue()  / 255.0f;
		float a = vivid.getAlpha() / 255.0f;

		buffer.vertex(matrix, x0, y0, z0).texture(0.0f, 1.0f).color(r, g, b, a);
		buffer.vertex(matrix, x1, y0, z0).texture(1.0f, 1.0f).color(r, g, b, a);
		buffer.vertex(matrix, x1, y0, z1).texture(1.0f, 0.0f).color(r, g, b, a);
		buffer.vertex(matrix, x0, y0, z1).texture(0.0f, 0.0f).color(r, g, b, a);

		BufferRenderer.drawWithGlobalProgram(buffer.end());

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}


	public record VertexCollection(Vertex... vertices) {
		public void vertex(BufferBuilder buffer) {
			for (Vertex vertex : vertices)
				buffer.vertex(vertex.matrix, vertex.x, vertex.y, vertex.z).color(vertex.color);
		}

		public double averageDepth() {
			double sum = 0.0;
			int count = 0;
			for (Vertex v : vertices) {
				sum += v.z;
				count++;
			}
			return count == 0 ? 0.0 : sum / count;
		}
	}

	public record Vertex(Matrix4f matrix, float x, float y, float z, int color) { }
}