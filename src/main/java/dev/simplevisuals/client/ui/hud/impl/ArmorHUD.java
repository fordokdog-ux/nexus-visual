package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.ui.hud.HudStyle;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.animations.infinity.InfinityAnimation;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.perf.Perf;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;

public class ArmorHUD extends HudElement implements ThemeManager.ThemeChangeListener {

    private final InfinityAnimation fadeAnimation = new InfinityAnimation(Easing.BOTH_SINE);

    // ThemeManager
    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color lowDurabilityColor;
    private Color headerTextColor;

    private static Color withAlpha(Color c, int a) {
        if (c == null) return new Color(0, 0, 0, a);
        int cap = Math.max(0, Math.min(255, a));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(c.getAlpha(), cap));
    }

    // Per-slot appearance animations (0..1)
    private final Map<Integer, InfinityAnimation> slotAlpha = new HashMap<>();
    // Last non-empty item per slot for fade-out animation
    private final Map<Integer, ItemStack> lastSlotItem = new HashMap<>();

    // Layout setting (multi-select)
    private final ListSetting layoutModes = new ListSetting(
            "setting.layout",
            new BooleanSetting("setting.horizontal", true),
            new BooleanSetting("setting.vertical", false)
    );

    public ArmorHUD() {
        super("ArmorHUD");

        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);

        // init per-slot animations
        for (int i = 0; i < 4; i++) {
            slotAlpha.put(i, new InfinityAnimation(Easing.OUT_QUAD));
            lastSlotItem.put(i, ItemStack.EMPTY);
        }

        layoutModes.setSingleSelect(true);
        getSettings().add(layoutModes);
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
        this.bgColor = theme.getBackgroundColor();
        this.textColor = theme.getTextColor();
        this.headerTextColor = this.textColor;
        this.lowDurabilityColor = new Color(200, 80, 80, 220);
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        Perf.tryBeginFrame();
        try (var __ = Perf.scopeCpu("ArmorHUD.onRender2D")) {

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        List<ItemStack> armorItems = new ArrayList<>();
        List<ItemStack> armorSlots = player.getInventory().armor;
        for (int i = armorSlots.size() - 1; i >= 0; i--) {
            ItemStack stack = armorSlots.get(i);
            armorItems.add(stack.isEmpty() ? ItemStack.EMPTY : stack);
        }

        boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        boolean allEmpty = armorItems.stream().allMatch(ItemStack::isEmpty);
        boolean previewMode = chatOpen && allEmpty;

        // В режиме чата всегда показываем элемент для возможности перетаскивания
        if (chatOpen) {
            fadeAnimation.animate(1f, 160);
            previewMode = true;
        } else if (allEmpty && !previewMode) {
            fadeAnimation.animate(0f, 160);
            if (fadeAnimation.getValue() <= 0) return;
        } else {
            fadeAnimation.animate(1f, 160);
        }

        // Update per-slot animations (visible when has item or in preview)
        for (int i = 0; i < 4; i++) {
            ItemStack presentStack = (i < armorItems.size() ? armorItems.get(i) : ItemStack.EMPTY);
            boolean present = previewMode || !presentStack.isEmpty();
            if (!presentStack.isEmpty()) lastSlotItem.put(i, presentStack);
            slotAlpha.get(i).animate(present ? 1f : 0f, 150);
        }

        float posX = getX();
        float posY = getY();
        ThemeManager.Theme theme = themeManager.getCurrentTheme();

        float iconSize = 16f;
        float slotBox = 18f;
        float cardPadding = 4f;
        float slotGap = 3f;
        float fontSize = 5.5f;
        float cardRadius = 6f;
        float insetRadius = 5f;

        float labelH = Fonts.MEDIUM.getHeight(fontSize) + 2f;

        float fade = fadeAnimation.getValue();

        float totalWidth = 0f;
        float totalHeight = 0f;

        e.getContext().getMatrices().push();

        float cursorY = posY;
        float cursorX = posX;

        boolean drawHorizontal = layoutModes.getName("setting.horizontal") != null && layoutModes.getName("setting.horizontal").getValue();
        boolean drawVertical = layoutModes.getName("setting.vertical") != null && layoutModes.getName("setting.vertical").getValue();

        // If none selected, fallback to horizontal
        if (!drawHorizontal && !drawVertical) drawHorizontal = true;

        boolean rightAnchored = getX() > mc.getWindow().getScaledWidth() / 2f;

        if (drawHorizontal) {
            float cardW = cardPadding * 2f + slotBox * 4f + slotGap * 3f;
            float cardH = cardPadding * 2f + slotBox + labelH;
            HudStyle.drawCard(
                    e.getContext().getMatrices(),
                    cursorX,
                    cursorY,
                    cardW,
                    cardH,
                    cardRadius,
                    theme,
                    0,
                    (int) (170 * fade)
            );

            for (int i = 0; i < 4; i++) {
                boolean presentNow = previewMode || (i < armorItems.size() && !armorItems.get(i).isEmpty());
                ItemStack renderStack = (previewMode ? new ItemStack(
                        i == 0 ? net.minecraft.item.Items.DIAMOND_HELMET :
                        i == 1 ? net.minecraft.item.Items.DIAMOND_CHESTPLATE :
                        i == 2 ? net.minecraft.item.Items.DIAMOND_LEGGINGS :
                                 net.minecraft.item.Items.DIAMOND_BOOTS
                ) : (i < armorItems.size() ? armorItems.get(i) : ItemStack.EMPTY));
                if (renderStack.isEmpty()) renderStack = lastSlotItem.getOrDefault(i, ItemStack.EMPTY);
                float slotX = cursorX + cardPadding + i * (slotBox + slotGap);
                float slotY = cursorY + cardPadding;

                float a = slotAlpha.get(i).getValue();
                if (a <= 0.01f) {
                    if (!presentNow) lastSlotItem.put(i, ItemStack.EMPTY);
                    continue;
                }
                float vis = a * fade;
                int itemAlpha = (int) (255 * vis);
                float appearYOffset = (1f - a) * 3f;

                if (!renderStack.isEmpty()) {
                    // Slot: border-only (no fill)
                    HudStyle.drawCard(
                        e.getContext().getMatrices(),
                        slotX,
                        slotY + appearYOffset,
                        slotBox,
                        slotBox,
                        insetRadius,
                        theme,
                        0,
                        (int) (140 * vis)
                    );

                    String durabilityText;
                    boolean low = false;
                    if (!renderStack.isDamageable() || renderStack.getMaxDamage() <= 0) {
                        durabilityText = "∞";
                    } else {
                        int pct = (int) (((float) (renderStack.getMaxDamage() - renderStack.getDamage()) / renderStack.getMaxDamage()) * 100f);
                        pct = Math.max(0, Math.min(100, pct));
                        low = pct <= 25;
                        durabilityText = pct + "%";
                    }

                        float textW = Fonts.MEDIUM.getWidth(durabilityText, fontSize);
                        float textH = Fonts.MEDIUM.getHeight(fontSize);
                        // Place durability UNDER the item so it never overlaps the icon
                        float textX = slotX + (slotBox - textW) / 2f;
                        float textY = slotY + slotBox + 1f + appearYOffset;
                    Color durColor = low ? lowDurabilityColor : textColor;
                    Render2D.drawFont(
                            e.getContext().getMatrices(),
                            Fonts.MEDIUM.getFont(fontSize),
                            durabilityText,
                            textX,
                            textY,
                            new Color(durColor.getRed(), durColor.getGreen(), durColor.getBlue(), itemAlpha)
                    );

                    var itemId = Registries.ITEM.getId(renderStack.getItem());
                    String itemPath = itemId.getPath();
                    Identifier texture = Identifier.of(itemId.getNamespace(), "textures/item/" + itemPath + ".png");
                    try {
                        Render2D.drawTexture(
                                e.getContext().getMatrices(),
                            slotX + (slotBox - iconSize) / 2f,
                            slotY + (slotBox - iconSize) / 2f + appearYOffset,
                                iconSize,
                                iconSize,
                                2f, 0f, 0f, 1f, 1f,
                                texture,
                                new Color(255, 255, 255, itemAlpha)
                        );
                    } catch (Exception ex) {
                        Render2D.drawFont(
                                e.getContext().getMatrices(),
                                Fonts.MEDIUM.getFont(fontSize),
                                "[No Icon]",
                                slotX,
                                slotY + appearYOffset,
                                new Color(255, 50, 50, itemAlpha)
                        );
                    }
                }
                if (!presentNow && a <= 0.02f) lastSlotItem.put(i, ItemStack.EMPTY);
            }
            totalWidth = Math.max(totalWidth, cardW);
            totalHeight += cardH + slotGap;
            cursorY += cardH + slotGap;
        }

        if (drawVertical) {
            float cardW = cardPadding * 2f + slotBox;
            float cardH = cardPadding * 2f + (slotBox + labelH) * 4f + slotGap * 3f;
            HudStyle.drawCard(
                    e.getContext().getMatrices(),
                    cursorX,
                    cursorY,
                    cardW,
                    cardH,
                    cardRadius,
                    theme,
                    0,
                    (int) (170 * fade)
            );

            for (int i = 0; i < 4; i++) {
                boolean presentNow = previewMode || (i < armorItems.size() && !armorItems.get(i).isEmpty());
                ItemStack renderStack = (previewMode ? new ItemStack(
                        i == 0 ? net.minecraft.item.Items.DIAMOND_HELMET :
                        i == 1 ? net.minecraft.item.Items.DIAMOND_CHESTPLATE :
                        i == 2 ? net.minecraft.item.Items.DIAMOND_LEGGINGS :
                                 net.minecraft.item.Items.DIAMOND_BOOTS
                ) : (i < armorItems.size() ? armorItems.get(i) : ItemStack.EMPTY));
                if (renderStack.isEmpty()) renderStack = lastSlotItem.getOrDefault(i, ItemStack.EMPTY);
                float slotX = cursorX + cardPadding;
                float slotY = cursorY + cardPadding + i * (slotBox + labelH + slotGap);

                float a = slotAlpha.get(i).getValue();
                if (a <= 0.01f) {
                    if (!presentNow) lastSlotItem.put(i, ItemStack.EMPTY);
                    continue;
                }
                float vis = a * fade;
                int itemAlpha = (int) (255 * vis);
                float appearXOffset = (1f - a) * (rightAnchored ? -3f : 3f);

                if (!renderStack.isEmpty()) {
                    // Slot: border-only (no fill)
                    HudStyle.drawCard(
                        e.getContext().getMatrices(),
                        slotX + appearXOffset,
                        slotY,
                        slotBox,
                        slotBox,
                        insetRadius,
                        theme,
                        0,
                        (int) (140 * vis)
                    );

                    String durabilityText;
                    boolean low = false;
                    if (!renderStack.isDamageable() || renderStack.getMaxDamage() <= 0) {
                        durabilityText = "∞";
                    } else {
                        int pct = (int) (((float) (renderStack.getMaxDamage() - renderStack.getDamage()) / renderStack.getMaxDamage()) * 100f);
                        pct = Math.max(0, Math.min(100, pct));
                        low = pct <= 25;
                        durabilityText = pct + "%";
                    }

                        float textW = Fonts.MEDIUM.getWidth(durabilityText, fontSize);
                        float textH = Fonts.MEDIUM.getHeight(fontSize);
                        // Under item (centered)
                        float textX = slotX + (slotBox - textW) / 2f + appearXOffset;
                        float textY = slotY + slotBox + 1f;
                    Color durColor = low ? lowDurabilityColor : textColor;
                    Render2D.drawFont(
                            e.getContext().getMatrices(),
                            Fonts.MEDIUM.getFont(fontSize),
                            durabilityText,
                            textX,
                            textY,
                            new Color(durColor.getRed(), durColor.getGreen(), durColor.getBlue(), itemAlpha)
                    );

                    var itemId = Registries.ITEM.getId(renderStack.getItem());
                    String itemPath = itemId.getPath();
                    Identifier texture = Identifier.of(itemId.getNamespace(), "textures/item/" + itemPath + ".png");
                    try {
                        Render2D.drawTexture(
                                e.getContext().getMatrices(),
                                slotX + (slotBox - iconSize) / 2f + appearXOffset,
                                slotY + (slotBox - iconSize) / 2f,
                                iconSize,
                                iconSize,
                                2f, 0f, 0f, 1f, 1f,
                                texture,
                                new Color(255, 255, 255, itemAlpha)
                        );
                    } catch (Exception ex) {
                        Render2D.drawFont(
                                e.getContext().getMatrices(),
                                Fonts.MEDIUM.getFont(fontSize),
                                "[No Icon]",
                                slotX + appearXOffset,
                                slotY,
                                new Color(255, 50, 50, itemAlpha)
                        );
                    }
                }
                if (!presentNow && a <= 0.02f) lastSlotItem.put(i, ItemStack.EMPTY);
            }
            totalWidth = Math.max(totalWidth, cardW);
            totalHeight += cardH;
        }

        e.getContext().getMatrices().pop();

        if (totalWidth <= 0f) totalWidth = cardPadding * 2f + slotBox * 4f + slotGap * 3f;
        if (totalHeight <= 0f) totalHeight = cardPadding * 2f + slotBox + labelH;

        setBounds(posX, posY, totalWidth, totalHeight);
        super.onRender2D(e);
        }
    }

    @Override
    public void onMouse(dev.simplevisuals.client.events.impl.EventMouse e) {
        // Специальная логика для ArmorHUD - разрешаем перетаскивание в строку чата
        if (!(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) || fullNullCheck()) return;

        // Вызываем базовую логику, но с дополнительными проверками
        super.onMouse(e);
    }

    private String formatDurability(ItemStack stack) {
        if (!stack.isDamageable()) return "∞";
        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamage();
        return (maxDamage - currentDamage) + "/" + maxDamage;
    }
}
