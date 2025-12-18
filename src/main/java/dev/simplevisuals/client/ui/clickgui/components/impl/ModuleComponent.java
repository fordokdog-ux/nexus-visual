package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.modules.settings.impl.*;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.Setting;
import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.renderer.Render2D;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import dev.simplevisuals.client.managers.ThemeManager;
import net.minecraft.client.resource.language.I18n;
import dev.simplevisuals.client.ui.clickgui.ClickGui;

public class ModuleComponent extends Component {

    private final Module module;
    private boolean open;
    private boolean binding;
    private boolean showBindModeMenu;
    private boolean renderExternally;

    // координаты отображения текста бинда для хитбокса
    private float bindTextX, bindTextY, bindTextW, bindTextH;

    @Getter private final List<Component> components = new ArrayList<>();

    private final Animation hoverAnim   = new Animation(200, 1f, false, Easing.BOTH_SINE);
    private final Animation toggleAnim  = new Animation(200, 1f, false, Easing.BOTH_SINE);
    @Getter private final Animation openAnimation = new Animation(300, 1f, false, Easing.BOTH_SINE);
    private final Animation bindMenuAnimation = new Animation(200, 1f, false, Easing.BOTH_SINE);
    private boolean toggleAnimInitialized;
    private boolean lastToggleState;
    private boolean isToggleAnimating;
    private boolean hoverAnimInitialized;

    private static final float HEADER_HEIGHT = 24f;
    private static final float CHILD_HEIGHT  = 20f;

    public ModuleComponent(Module module) {
        super(module.getName());
        this.module = module;
        this.lastToggleState = module.isToggled();

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) components.add(new BooleanComponent((BooleanSetting) setting));
            else if (setting instanceof NumberSetting) components.add(new SliderComponent((NumberSetting) setting));
            else if (setting instanceof EnumSetting) components.add(new EnumComponent((EnumSetting<?>) setting));
            else if (setting instanceof StringSetting) components.add(new StringComponent((StringSetting) setting));
            else if (setting instanceof ListSetting) components.add(new ListComponent((ListSetting) setting));
            else if (setting instanceof BindSetting) components.add(new BindComponent((BindSetting) setting));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = MathUtils.isHovered(x, y, width, HEADER_HEIGHT, mouseX, mouseY);
        // Показываем описание модуля сверху ClickGUI при ховере
        if (hovered && mc.currentScreen instanceof dev.simplevisuals.client.ui.clickgui.ClickGui clickGui) {
            clickGui.setDescription(module.getDescription());
        }
        // Ховер-анимация зависит только от наведения, не от состояния модуля
        if (!hoverAnimInitialized) {
            hoverAnim.setDuration(0);
            hoverAnim.update(hovered);
            hoverAnim.setDuration(200);
            hoverAnimInitialized = true;
        } else {
            hoverAnim.update(hovered);
        }
        // Обновляем анимацию переключателя только при фактической смене состояния модуля
        boolean currentState = module.isToggled();
        if (!toggleAnimInitialized) {
            toggleAnim.setDuration(0);
            toggleAnim.update(currentState);
            toggleAnim.setDuration(200);
            toggleAnimInitialized = true;
            lastToggleState = currentState;
            isToggleAnimating = false;
        } else if (currentState != lastToggleState) {
            toggleAnim.setDuration(200);
            toggleAnim.update(currentState);
            lastToggleState = currentState;
            isToggleAnimating = true;
        } else if (isToggleAnimating && (toggleAnim.finished(currentState))) {
            isToggleAnimating = false;
        }
        openAnimation.update(open);
        bindMenuAnimation.update(showBindModeMenu);

        // фон
        Color base = new Color(67, 67, 67, (int) (120 * Math.max(0f, Math.min(1f, globalAlpha))));
        Color themeAccent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        Color active = new Color(themeAccent.getRed(), themeAccent.getGreen(), themeAccent.getBlue(), 180);
        Color bg = base;

        float totalHeight = getHeight();
        Render2D.drawRoundedRect(context.getMatrices(), x, y, width, totalHeight, 5f, bg);


        Color textColor = new Color(255, 255, 255, (int) ((200 + 55 * hoverAnim.getValue()) * Math.max(0f, Math.min(1f, globalAlpha))));
        float titleFontSize = 8f;
        float titleFontH = Fonts.REGULAR.getHeight(titleFontSize);
        float titleY = y + (HEADER_HEIGHT - titleFontH) / 2f;
        String localizedModuleName = I18n.translate(module.getName());
        Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(titleFontSize),
                localizedModuleName, x + 8f, titleY, textColor);

        // биндинг — вычисляем позицию и хитбокс
        String bindText = "";
        if (binding) bindText = "Press key...";
        else if (module.getBind() != null && module.getBind().getKey() != -1)
            bindText = module.getBind().toString().replace("_", " ");

        // маленький текст бинда снизу
        float bindFontSize = 6f;
        bindTextW = bindText.isEmpty() ? 0f : Fonts.REGULAR.getWidth(bindText, bindFontSize);
        bindTextH = Fonts.REGULAR.getHeight(bindFontSize);
        // координаты будут обновлены после вычисления totalHeight
        // временно сбросим X/Y, чтобы не ломать хитбокс до отрисовки
        bindTextX = Float.NaN;
        bindTextY = Float.NaN;

        // Синхронизация состояния ползунка без анимации при первом рендере,
        // чтобы избежать ложного отображения статуса при открытии GUI
        if (!toggleAnimInitialized) {
            toggleAnim.setDuration(0);
            toggleAnim.update(module.isToggled());
            // восстановить стандартную длительность анимации (200 мс по умолчанию)
            toggleAnim.setDuration(200);
            toggleAnimInitialized = true;
        }

                // переключатель (ползунок) состояния модуля, как у BooleanSetting
        float switchW = 20f;
        float switchH = 10f;
        float reservedRight = 0f; // фиксированное положение: без учёта текста бинда
        float switchX = x + width - reservedRight - 24f; // 24f = ширина переключателя + отступ
        float switchY = y + (HEADER_HEIGHT - switchH) / 2f;

                boolean lightUi = ClickGui.isGuiLightMode();
                Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
                int accentSum = accent.getRed() + accent.getGreen() + accent.getBlue();
                // если акцент слишком белый — делаем его серее, чтобы не "заливало" весь тумблер
                Color safeAccent = accentSum > 720
                    ? (lightUi ? new Color(140, 140, 140) : new Color(200, 200, 200))
                    : accent;

        float progress = isToggleAnimating ? (float) toggleAnim.getValue() : (currentState ? 1f : 0f);

                int ga = (int) (255 * Math.max(0f, Math.min(1f, globalAlpha)));
                Color trackBase = lightUi
                    ? new Color(230, 230, 230, (int) (170 * ga / 255f))
                    : new Color(23, 23, 23, (int) (160 * ga / 255f));
                Color trackOn = new Color(safeAccent.getRed(), safeAccent.getGreen(), safeAccent.getBlue(), (int) (190 * progress * ga / 255f));

                // базовая дорожка
                Render2D.drawRoundedRect(context.getMatrices(), switchX, switchY, switchW, switchH, 4f, trackBase);
                // подсветка включения (не 255, чтобы не было "белого кирпича")
                if (progress > 0.01f) {
                    Render2D.drawRoundedRect(context.getMatrices(), switchX, switchY, switchW, switchH, 4f, trackOn);
                }

        float thumbW = 8f;
        float thumbH = 8f;
        // Увеличенный симметричный отступ 1.5f с обеих сторон
        float padding = 1.5f;
        float thumbX = switchX + padding + (switchW - thumbW - 2f * padding) * progress;
        float thumbY = switchY + padding + (switchH - thumbH - 2f * padding) / 2f;
                Color thumb = lightUi ? new Color(35, 35, 35, ga) : new Color(245, 245, 245, ga);
                Color thumbBorder = lightUi ? new Color(0, 0, 0, (int) (90 * ga / 255f)) : new Color(255, 255, 255, (int) (60 * ga / 255f));
                Render2D.drawRoundedRect(context.getMatrices(), thumbX, thumbY, thumbW, thumbH, 3f, thumb);
                Render2D.drawBorder(context.getMatrices(), thumbX, thumbY, thumbW, thumbH, 3f, 0.6f, 0.6f, thumbBorder);

        // дочерние компоненты (без анимации раскрытия)
        if (open && !renderExternally) {
            float childY = y + HEADER_HEIGHT;
            float visibleH = getChildrenFullHeight();

            // клип ограничен рамкой модуля
            context.enableScissor((int) x, (int) (y + HEADER_HEIGHT),
                    (int) (x + width), (int) (y + HEADER_HEIGHT + visibleH));

            for (Component component : components) {
                if (!component.getVisible().get()) continue;
                component.setX(x + 5f);
                component.setY(childY);
                component.setWidth(width - 10f);
                component.setHeight(CHILD_HEIGHT);
                component.setGlobalAlpha(globalAlpha);
                component.render(context, mouseX, mouseY, delta);
                childY += component.getHeight() + component.getAddHeight().get();
            }

            context.disableScissor();
        }

        // теперь отрисуем маленький текст бинда снизу (после детей, чтобы не клипался)
        if (!bindText.isEmpty()) {
            float totalHeightAfter = getHeight();
            bindTextX = x + 8f;
            bindTextY = y + totalHeightAfter - bindTextH - 1f;
            Render2D.drawFont(context.getMatrices(), Fonts.REGULAR.getFont(bindFontSize),
                    bindText, bindTextX, bindTextY,
                    new Color(200, 200, 200, (int) (180 * Math.max(0f, Math.min(1f, globalAlpha)))));
        }

        // Контекстное меню режима бинда (мгновенное появление, без анимации масштаба/альфы)
        if (showBindModeMenu) {
            float itemW = 80f;
            float itemH = 15f; // увеличено с 12f до 15f

            // Позиционирование: рядом с текстом бинда и ВЫШЕ, чтобы не уходить за нижнюю границу
            float desiredX = (Float.isNaN(bindTextX) ? (x + width - 90f) : (bindTextX + bindTextW + 6f));
            float menuX = Math.min(desiredX, x + width - itemW - 6f);
            // Если нет места справа — прижать слева от текста бинда
            if (!Float.isNaN(bindTextX) && menuX < bindTextX) {
                float leftCandidate = bindTextX - itemW - 6f;
                if (leftCandidate >= x + 6f) menuX = leftCandidate;
            }
            float menuYBase = (Float.isNaN(bindTextY) ? (y + HEADER_HEIGHT + 2f) : (bindTextY - (itemH * 2f + 7f) - 4f));

            // Анимация появления: альфа и слайд-вверх
            float a = (float) Math.max(0f, Math.min(1f, bindMenuAnimation.getValue()));
            float slideOffset = (1f - a) * 6f;
            float menuY = menuYBase + slideOffset;

            Color menuBg = new Color(20, 20, 20, (int) (220 * a * Math.max(0f, Math.min(1f, globalAlpha))));
            Render2D.drawRoundedRect(context.getMatrices(), menuX, menuY, itemW, itemH * 2 + 7f, 3f, menuBg);

            String opt1 = "Toggle";
            String opt2 = "Hold";

            boolean isToggle = module.getBind() != null && module.getBind().getMode() == Bind.Mode.TOGGLE;
            boolean isHold = module.getBind() != null && module.getBind().getMode() == Bind.Mode.HOLD;

            int textAlpha = (int) (255 * a * Math.max(0f, Math.min(1f, globalAlpha)));
            // Toggle row
            Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(7f), opt1, menuX + 18f, menuY + 5f, new Color(255, 255, 255, textAlpha));
            Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(10f), "D", menuX + 6f, menuY + 5.5f,
                    new Color(0, 0, 0, (int) ((isToggle ? 255 : 0) * a * Math.max(0f, Math.min(1f, globalAlpha)))));

            // Hold row
            Render2D.drawFont(context.getMatrices(), Fonts.BOLD.getFont(7f), opt2, menuX + 18f, menuY + 5f + itemH, new Color(255, 255, 255, textAlpha));
            Render2D.drawFont(context.getMatrices(), Fonts.ICONS.getFont(10f), "D", menuX + 6f, menuY + 5.5f + itemH,
                    new Color(0, 0, 0, (int) ((isHold ? 255 : 0) * a * Math.max(0f, Math.min(1f, globalAlpha)))));
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean onHeader = MathUtils.isHovered(x, y, width, HEADER_HEIGHT, (float) mouseX, (float) mouseY);
        boolean onBindText = (!Float.isNaN(bindTextX)) && MathUtils.isHovered(bindTextX, bindTextY - 1f, bindTextW, bindTextH + 2f, (float) mouseX, (float) mouseY);

        // клик по нижнему тексту бинда — открыть меню режима бинда
        if (button == 0 && onBindText && !binding) {
            if (module.getBind() != null && !module.getBind().isEmpty()) {
                showBindModeMenu = !showBindModeMenu;
                return;
            }
        }

        if (onHeader) {
            if (button == 0 && !binding) { // ЛКМ — вкл/выкл
                module.toggle();
                return;
            } else if (button == 1 && !components.isEmpty() && !binding && !renderExternally) { // ПКМ — открыть/закрыть список
                open = !open;
                return;
            } else if (button == 2 && !binding) { // СКМ — бинд
                binding = true;
                return;
            }
        }

        // клик по пунктам меню режима бинда
        if (showBindModeMenu) {
            float itemW = 80f;
            float itemH = 15f; // увеличено с 12f до 15f

            float desiredX = (Float.isNaN(bindTextX) ? (x + width - 90f) : (bindTextX + bindTextW + 6f));
            float menuX = Math.min(desiredX, x + width - itemW - 6f);
            if (!Float.isNaN(bindTextX) && menuX < bindTextX) {
                float leftCandidate = bindTextX - itemW - 6f;
                if (leftCandidate >= x + 6f) menuX = leftCandidate;
            }
            float menuY = (Float.isNaN(bindTextY) ? (y + HEADER_HEIGHT + 2f) : (bindTextY - (itemH * 2f + 7f) - 4f));

            if (MathUtils.isHovered(menuX, menuY, itemW, itemH, (float) mouseX, (float) mouseY)) {
                module.getBind().setMode(Bind.Mode.TOGGLE);
                showBindModeMenu = false;
                return;
            }
            if (MathUtils.isHovered(menuX, menuY + itemH, itemW, itemH, (float) mouseX, (float) mouseY)) {
                module.getBind().setMode(Bind.Mode.HOLD);
                showBindModeMenu = false;
                return;
            }
            // клик снаружи — закрыть меню и поглотить событие
            if (!MathUtils.isHovered(menuX, menuY, itemW, itemH * 2f + 7f, (float) mouseX, (float) mouseY)) {
                showBindModeMenu = false;
                return;
            }
            // В любом случае при открытом меню — поглощаем событие
            return;
        }

        if (open && !renderExternally) {
            for (Component component : components) component.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        // При открытом меню режима бинда — поглощаем отпускание, чтобы не передавалось детям
        if (showBindModeMenu) return;
        if (open && !renderExternally) {
            for (Component component : components) component.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
                module.setBind(new Bind(-1, false)); // снять бинд
            } else {
                // сохраняем текущий режим при переназначении
                Bind.Mode mode = module.getBind() != null ? module.getBind().getMode() : Bind.Mode.TOGGLE;
                module.setBind(new Bind(keyCode, false, mode));
            }
            binding = false;
            return;
        }

        if (open && !renderExternally) {
            for (Component component : components) component.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void keyReleased(int keyCode, int scanCode, int modifiers) {
        if (open && !renderExternally) {
            for (Component component : components) component.keyReleased(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (open && !renderExternally) {
            for (Component component : components) component.charTyped(chr, modifiers);
        }
    }

    // высота = только заголовок, если внешний рендер настроек активен
    @Override
    public float getHeight() {
        if (renderExternally) return HEADER_HEIGHT;
        return HEADER_HEIGHT + (open ? getChildrenFullHeight() : 0f);
    }

    public float getChildrenFullHeight() {
        float h = 0f;
        for (Component c : components) {
            if (!c.getVisible().get()) continue;
            h += CHILD_HEIGHT + c.getAddHeight().get();
        }
        return h;
    }

    private static Color lerpColor(Color c1, Color c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (c1.getRed()   + (c2.getRed()   - c1.getRed())   * t);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int) (c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * t);
        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * t);
        return new Color(r, g, b, a);
    }
    public Module getModule() {
        return module;
    }

    public void setRenderExternally(boolean value) {
        this.renderExternally = value;
    }

    public boolean isRenderExternally() {
        return renderExternally;
    }

    public boolean isBindModeMenuOpen() {
        return showBindModeMenu;
    }

    public float renderSettingsExternally(DrawContext context, float contentX, float contentY, float contentW,
                                          float clipX, float clipY, float clipW, float clipH, int mouseX, int mouseY, float delta, float scrollY) {
        float childY = contentY - scrollY;
        float totalHeight = 0f;
        context.enableScissor((int) clipX, (int) clipY, (int) (clipX + clipW), (int) (clipY + clipH));
        for (Component component : components) {
            if (!component.getVisible().get()) continue;
            component.setX(contentX + 5f);
            component.setY(childY);
            component.setWidth(contentW - 10f);
            component.setHeight(CHILD_HEIGHT);
            component.setGlobalAlpha(globalAlpha);
            component.render(context, mouseX, mouseY, delta);
            float h = component.getHeight() + component.getAddHeight().get();
            childY += h;
            totalHeight += h;
        }
        context.disableScissor();
        return totalHeight;
    }

    public void mouseClickedExternal(double mouseX, double mouseY, int button) {
        for (Component component : components) component.mouseClicked(mouseX, mouseY, button);
    }

    public void mouseReleasedExternal(double mouseX, double mouseY, int button) {
        for (Component component : components) component.mouseReleased(mouseX, mouseY, button);
    }

    public void keyPressedExternal(int keyCode, int scanCode, int modifiers) {
        for (Component component : components) component.keyPressed(keyCode, scanCode, modifiers);
    }

    public void keyReleasedExternal(int keyCode, int scanCode, int modifiers) {
        for (Component component : components) component.keyReleased(keyCode, scanCode, modifiers);
    }

    public void charTypedExternal(char chr, int modifiers) {
        for (Component component : components) component.charTyped(chr, modifiers);
    }
}