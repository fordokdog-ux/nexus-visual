package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.ui.hud.HudStyle;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.animations.infinity.InfinityAnimation;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.perf.Perf;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Potions extends HudElement implements ThemeManager.ThemeChangeListener {

	private final InfinityAnimation hudFade = new InfinityAnimation(Easing.BOTH_SINE);
	private final InfinityAnimation heightAnim = new InfinityAnimation(Easing.OUT_QUAD);
	private final InfinityAnimation widthAnim = new InfinityAnimation(Easing.OUT_QUAD);

	// Per-item анимация 0..1 (1 — полностью видим, 0 — скрыт)
	private final Map<String, InfinityAnimation> itemAlpha = new LinkedHashMap<>();
	// Снапшоты последнего названия/иконки для исчезающих
	private final Map<String, String> lastText = new HashMap<>();
	private final Map<String, String> lastIconKey = new HashMap<>();

	private final ThemeManager themeManager;
	private Color negativeColor;
    private Color highlightColor;

	private static Color withAlpha(Color c, int a) {
		if (c == null) return new Color(0, 0, 0, a);
		int cap = Math.max(0, Math.min(255, a));
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(c.getAlpha(), cap));
	}

	public final BooleanSetting showNegative = new BooleanSetting("ShowNegative", true);
	public final BooleanSetting highlightLowDuration = new BooleanSetting("HighlightLowDuration", true);

	public Potions() {
		super("Potions");
		this.themeManager = ThemeManager.getInstance();
		applyTheme(themeManager.getCurrentTheme());
		themeManager.addThemeChangeListener(this);
		this.getSettings().add(showNegative);
		this.getSettings().add(highlightLowDuration);
	}

	@Override
	public void onDisable() {
		themeManager.removeThemeChangeListener(this);
		super.onDisable();
	}

	@Override
	public void onThemeChanged(ThemeManager.Theme theme) {
		applyTheme(theme);
	}

	private void applyTheme(ThemeManager.Theme theme) {
		this.negativeColor = new Color(200, 80, 80, 220);
		this.highlightColor = themeManager.getAccentColor();
	}

	@Override
	public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        Perf.tryBeginFrame();
        try (var __ = Perf.scopeCpu("Potions.onRender2D")) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

		Collection<StatusEffectInstance> raw = player.getStatusEffects();
		boolean hasAny = !raw.isEmpty();
		boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
		// Без fade: всегда полная альфа для контента и фона
		int contentHudAlpha = 255;

		// Сбор активных эффектов и стабилизация порядка (по названию)
		List<StatusEffectInstance> effects = new ArrayList<>();
		for (StatusEffectInstance eff : raw) {
			StatusEffect type = eff.getEffectType().value();
			if (!type.isBeneficial() && !showNegative.getValue()) continue;
			effects.add(eff);
		}
		effects.sort(Comparator.comparing(a -> a.getEffectType().value().getName().getString()));

		// Построим списки ключей/текстов/иконок
		List<String> keys = new ArrayList<>();
		List<String> texts = new ArrayList<>();
		List<String> icons = new ArrayList<>();
		for (StatusEffectInstance eff : effects) {
			StatusEffect type = eff.getEffectType().value();
			String name = type.getName().getString();
			String level = eff.getAmplifier() > 0 ? " " + toRoman(eff.getAmplifier() + 1) : "";
			String display = name + level;
			Identifier rid = Registries.STATUS_EFFECT.getId(type);
			String effectKey = rid == null ? "" : rid.getPath();
			String key = effectKey; // ключ по типу
			keys.add(key);
			texts.add(display);
			icons.add(effectKey);
			lastText.put(key, display);
			lastIconKey.put(key, effectKey);
		}

		// Режим предпросмотра в чате — показать пример, если эффектов нет
		boolean previewMode = chatOpen && keys.isEmpty();
		if (previewMode) {
			keys.add("speed");
			texts.add("Speed II");
			icons.add("speed");
		}

		// Обновляем цели анимации для активных/неактивных
		for (String k : itemAlpha.keySet()) {
			boolean active = keys.contains(k);
			// При очистке — сразу 0
			itemAlpha.get(k).animate(active ? 1f : 0f, (active ? 220 : (hasAny ? 160 : 0)));
		}
		for (String k : keys) {
			itemAlpha.computeIfAbsent(k, kk -> new InfinityAnimation(Easing.OUT_QUAD)).animate(1f, 220);
		}

		ThemeManager.Theme theme = themeManager.getCurrentTheme();
		Color textColor = themeManager.getTextColor();

		// Вычисляем размеры (целевые)
		float posX = getX();
		float posY = getY();
		float uiScale = 0.92f;
		float headerH = 0f; // минимализм: без отдельного хедера
		float spacing = 6f * uiScale;
		float rowH = 16f * uiScale;
		float pad = 8f * uiScale;
		float icon = 14f * uiScale;
		float font = 8f * uiScale;
		float titleFont = 9f * uiScale;
		float yAdjust = 0f;

		String title = net.minecraft.client.resource.language.I18n.translate("hud.potions.title");
		float titleW = Fonts.BOLD.getWidth(title, titleFont);
		float titleH = Fonts.BOLD.getHeight(titleFont);

		float targetWidth = Math.max(90f, pad * 2 + titleW);
		for (int i = 0; i < keys.size(); i++) {
			float w = pad * 2 + icon + 6f + Fonts.MEDIUM.getWidth(texts.get(i), font) + 36f * uiScale;
			targetWidth = Math.max(targetWidth, w);
		}

		// Считаем видимые строки: активные как 1
		float visibleRows = keys.size();
		if (previewMode) visibleRows = 1f;
		float topTextH = titleH;
		float targetHeight = pad + topTextH + spacing + Math.max(0, visibleRows) * rowH + pad;

		// Оставляем только зжатие/разжатие окна
		heightAnim.animate(targetHeight, (hasAny || previewMode) ? 140 : 100);
		widthAnim.animate(targetWidth, 220);
		float currentHeight = heightAnim.getValue();
		float currentWidth = widthAnim.getValue();

		// Не используем fade для скрытия — рисуем во время схлопывания
		boolean nothingVisible = false;

		// Обновляем границы элемента для корректного перетаскивания/хитбокса
		setBounds(getX(), getY(), currentWidth, Math.max(20f, currentHeight));
		if (nothingVisible) {
			super.onRender2D(e);
			return;
		}

		e.getContext().getMatrices().push();

		HudStyle.drawCard(e.getContext().getMatrices(), posX, posY, currentWidth, currentHeight, 7f * uiScale, theme);

		float titleX = posX + pad;
		float titleY = posY + pad - 0.25f;
		Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(titleFont), title, titleX, titleY,
				HudStyle.alphaCap(textColor, contentHudAlpha));

		float curY = titleY + titleH + spacing;

		// 1) Активные строки (или предпросмотр)
		for (int i = 0; i < keys.size(); i++) {
			String k = keys.get(i);
			InfinityAnimation anim = previewMode ? null : itemAlpha.get(k);
			// Всегда рисуем сразу
			float a = 1f;
			if (a <= 0.01f) a = 0.01f;
			float xOffset = 0f;
			int alpha = (int)(contentHudAlpha * Math.max(a, 0.85f));

			StatusEffectInstance eff = previewMode ? null : effects.get(i);
			StatusEffect type = eff == null ? null : eff.getEffectType().value();
			boolean negative = type != null && !type.isBeneficial();
			boolean low = eff != null && eff.getDuration() <= 200;
            // Берём актуальный акцент из темы на кадр, чтобы поддержать градиенты
			Color liveAccent = themeManager.getAccentColor();
            Color draw = previewMode ? textColor : (low && highlightLowDuration.getValue() ? liveAccent : (negative ? negativeColor.brighter() : textColor));

			String iconKeyActive = icons.get(i) == null ? "" : icons.get(i);
			// Фиксированная схема: иконка слева, текст справа от иконки независимо от стороны экрана
			float iconX = (posX + pad + xOffset);
			float rowCenterY = curY + yAdjust + rowH / 2f;
			String nameText = texts.get(i) == null ? "" : texts.get(i);
			float nameW = Fonts.MEDIUM.getWidth(nameText, font);
			float nameTextX = (iconX + icon + 6f);
			float nameTextY = rowCenterY - Fonts.MEDIUM.getHeight(font) / 2f;

			if (!iconKeyActive.isEmpty()) {
				Identifier tex = Identifier.of("minecraft", "textures/mob_effect/" + iconKeyActive + ".png");
				Render2D.drawTexture(e.getContext().getMatrices(), iconX, curY + (rowH - icon)/2f,
						icon, icon, 2f, 0f, 0f, 1f, 1f, tex, new Color(255, 255, 255, alpha));
			}

			if (!nameText.isBlank()) {
				Render2D.drawFont(e.getContext().getMatrices(), Fonts.MEDIUM.getFont(font), nameText,
						nameTextX, nameTextY,
						new Color(draw.getRed(), draw.getGreen(), draw.getBlue(), alpha));
			}

			// Таймер в собственной плашке в общем правом столбике
			String t = previewMode ? "0:30" : (eff == null ? "" : formatDuration(eff.getDuration()));
			if (!t.isBlank()) {
				float tw = Fonts.MEDIUM.getWidth(t, font);
				float pillW = tw + 6f * uiScale;
				float pillH = rowH - 4f * uiScale;
				// Единая правая колонка для всех строк: выравниваем по правому краю панели
				float rightEdge = posX + currentWidth - pad;
				float pillX = rightEdge - pillW;
				float pillY = curY + 2f * uiScale;
				HudStyle.drawInset(e.getContext().getMatrices(), pillX, pillY, pillW, pillH, 6f * uiScale, theme, 155);
				Render2D.drawFont(e.getContext().getMatrices(), Fonts.MEDIUM.getFont(font), t,
						pillX + (pillW - tw)/2f, rowCenterY - Fonts.MEDIUM.getHeight(font)/2f,
						HudStyle.alphaCap(textColor, alpha));
			}

			curY += rowH;
		}

		// Убираем рендер исчезающих строк — мгновенное скрытие

		        e.getContext().getMatrices().pop();
        super.onRender2D(e);
        }
    }

	private String toRoman(int number) {
		switch(number) {
			case 1: return "I";
			case 2: return "II";
			case 3: return "III";
			case 4: return "IV";
			case 5: return "V";
			default: return String.valueOf(number);
		}
	}

	private String formatDuration(int ticks) {
		int seconds = ticks/20;
		int minutes = seconds/60;
		seconds %= 60;
		return String.format("%d:%02d", minutes, seconds);
	}

	private float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}
}
