package dev.simplevisuals.client.ui.mainmenu;

import dev.simplevisuals.client.managers.AltManager;
import dev.simplevisuals.client.render.builders.impl.RectangleBuilder;
import dev.simplevisuals.client.render.builders.impl.BlurBuilder;
import dev.simplevisuals.client.render.builders.impl.TextBuilder;
import dev.simplevisuals.client.render.builders.states.QuadColorState;
import dev.simplevisuals.client.render.builders.states.QuadRadiusState;
import dev.simplevisuals.client.render.builders.states.SizeState;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.resource.language.I18n;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AltManagerScreen extends Screen {

    private final Screen parent;
    private final List<String> alts = new ArrayList<>();
    private final List<String> filteredAlts = new ArrayList<>();
    private final List<MainMenu.AnimatedButton> buttons = new ArrayList<>();
    private String inputBuffer = "";
    private String searchBuffer = "";
    private int selectedIndex = -1;
	private int caretIndex = 0;
	private int selectionStart = -1; // -1 -> no selection
    private int searchCaretIndex = 0;
    private int searchSelectionStart = -1;

    private boolean inputFocused = false;
    private boolean searchFocused = false;
    
    private final Animation inputFocusAnimation = new Animation(220, 1.0, false, Easing.OUT_EXPO);
    private final Animation searchFocusAnimation = new Animation(220, 1.0, false, Easing.OUT_EXPO);
    private final Animation listHoverAnimation = new Animation(200, 1.0, false, Easing.OUT_EXPO);
    private int hoveredRowIndex = -1;

    // Scrolling for alts list
    private float listScroll = 0f;
    private float listScrollTarget = 0f;
    private float listMaxScroll = 0f;

        private record Layout(
            float panelX,
            float panelY,
            float panelW,
            float panelH,
            float listX,
            float listY,
            float listW,
            float listH,
            float inputX,
            float inputY,
            float inputW,
            float inputH,
            float addX,
            float addY,
            float deleteX,
            float deleteY,
            float backX,
            float backY,
            float halfButtonW,
            float buttonH,
            float buttonGap
        ) {}

        private Layout layout() {
        float padding = 16f;
        float gap = 8f;
        float buttonH = 24f;
        float inputH = 20f;
            float dividerYInset = 52f; // from panelY
            float listTopInset = dividerYInset + 10f; // divider + spacing

        float panelW = Math.min(400f, this.width - 40f);
        float panelH = Math.min(320f, this.height - 40f);
        float panelX = this.width / 2f - panelW / 2f;
        float panelY = this.height / 2f - panelH / 2f;

        float listX = panelX + padding;
        float listW = panelW - padding * 2f;

        float listY = panelY + listTopInset;
        float footerH = buttonH * 2f + gap; // add/delete row + back row
        float listBottom = panelY + panelH - padding - (inputH + 10f) - footerH - 10f;
        float listH = Math.max(64f, listBottom - listY);

        float inputX = listX;
        float inputW = listW;
        float inputY = listY + listH + 10f;

        float halfButtonW = (listW - gap) / 2f;
        float buttonsY = inputY + inputH + 10f;

        float addX = listX;
        float addY = buttonsY;
        float deleteX = listX + halfButtonW + gap;
        float deleteY = buttonsY;
        float backX = listX;
        float backY = buttonsY + buttonH + gap;

        return new Layout(
            panelX, panelY, panelW, panelH,
            listX, listY, listW, listH,
            inputX, inputY, inputW, inputH,
            addX, addY, deleteX, deleteY, backX, backY,
            halfButtonW, buttonH, gap
        );
        }


    public AltManagerScreen(Screen parent) {
        super(Text.literal(I18n.translate("simplevisuals.alt.title")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        alts.clear();
        alts.addAll(AltManager.getNicknames());
        filterAlts();
        buttons.clear();
		if (caretIndex > inputBuffer.length()) caretIndex = inputBuffer.length();
        if (searchCaretIndex > searchBuffer.length()) searchCaretIndex = searchBuffer.length();

        // Reset list scroll state on init
        listScroll = 0f;
        listScrollTarget = 0f;

        // Preselect last used nickname if exists
        String active = AltManager.getLastUsedNickname();
        if (active != null) {
            for (int i = 0; i < filteredAlts.size(); i++) {
                if (filteredAlts.get(i).equalsIgnoreCase(active)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        Layout l = layout();

        // Add (left)
        buttons.add(new MainMenu.AnimatedButton(l.addX, l.addY, l.halfButtonW, l.buttonH, I18n.translate("simplevisuals.alt.add"), b -> {
            if (!inputBuffer.isEmpty()) {
                if (AltManager.addNickname(inputBuffer)) {
                    inputBuffer = "";
                    caretIndex = 0;
                    selectionStart = -1;
                    init();
                }
            }
        }));

        // Delete (right)
        buttons.add(new MainMenu.AnimatedButton(l.deleteX, l.deleteY, l.halfButtonW, l.buttonH, I18n.translate("simplevisuals.alt.delete"), b -> {
            if (selectedIndex >= 0 && selectedIndex < filteredAlts.size()) {
                String target = filteredAlts.get(selectedIndex);
                if (AltManager.removeNickname(target)) {
                    selectedIndex = -1;
                    init();
                }
            }
        }));

        // Back (centered below)
        buttons.add(new MainMenu.AnimatedButton(l.backX, l.backY, l.listW, l.buttonH, I18n.translate("simplevisuals.alt.back"), b -> {
            this.client.setScreen(parent);
        }));
    }

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (Character.isISOControl(chr)) return false;
		insertText(String.valueOf(chr));
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean ctrl = hasControlDown();
		boolean shift = hasShiftDown();

		// Clipboard shortcuts
		if (ctrl && keyCode == GLFW.GLFW_KEY_V) { // Paste
			String clip = this.client != null && this.client.keyboard != null ? this.client.keyboard.getClipboard() : null;
			if (clip != null && !clip.isEmpty()) insertText(clip);
			return true;
		}
		if (ctrl && keyCode == GLFW.GLFW_KEY_C) { // Copy
			String sel = getSelectedText();
			if (sel != null && this.client != null && this.client.keyboard != null) this.client.keyboard.setClipboard(sel);
			return true;
		}
		if (ctrl && keyCode == GLFW.GLFW_KEY_X) { // Cut
			String sel = getSelectedText();
			if (sel != null && this.client != null && this.client.keyboard != null) {
				this.client.keyboard.setClipboard(sel);
				deleteSelection();
			}
			return true;
		}
		if (ctrl && keyCode == GLFW.GLFW_KEY_A) { // Select All
			selectionStart = 0;
			caretIndex = inputBuffer.length();
			return true;
		}

		// Navigation and deletion
		if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			if (!deleteSelection()) {
				if (caretIndex > 0 && !inputBuffer.isEmpty()) {
					inputBuffer = inputBuffer.substring(0, caretIndex - 1) + inputBuffer.substring(caretIndex);
					caretIndex--;
				}
			}
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_DELETE) {
			if (!deleteSelection()) {
				if (caretIndex < inputBuffer.length()) {
					inputBuffer = inputBuffer.substring(0, caretIndex) + inputBuffer.substring(caretIndex + 1);
				}
			}
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_LEFT) {
			moveCaret(-1, shift);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_RIGHT) {
			moveCaret(1, shift);
			return true;
		}

		// Keyboard shortcuts
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			if (!inputBuffer.isEmpty()) {
				if (AltManager.addNickname(inputBuffer)) {
					inputBuffer = "";
					caretIndex = 0;
					selectionStart = -1;
					init();
				}
			} else if (selectedIndex >= 0 && selectedIndex < filteredAlts.size()) {
				AltManager.applyNickname(filteredAlts.get(selectedIndex));
				init();
			}
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_DELETE && selectedIndex >= 0 && selectedIndex < filteredAlts.size()) {
            String target = filteredAlts.get(selectedIndex);
            if (AltManager.removeNickname(target)) {
                selectedIndex = -1;
                init();
            }
            return true;
        }

		if (keyCode == GLFW.GLFW_KEY_UP) {
			if (selectedIndex > 0) {
				selectedIndex--;
			} else if (filteredAlts.size() > 0) {
				selectedIndex = filteredAlts.size() - 1;
			}
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_DOWN) {
			if (selectedIndex < filteredAlts.size() - 1) {
				selectedIndex++;
			} else {
				selectedIndex = 0;
			}
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout l = layout();
        // list selection
        if (mouseX >= l.listX && mouseX <= l.listX + l.listW && mouseY >= l.listY && mouseY <= l.listY + l.listH) {
            int rowHeight = 18;
            int index = (int) (((mouseY - l.listY) + listScroll) / rowHeight);
			if (index >= 0 && index < filteredAlts.size()) {
				if (selectedIndex == index) {
					AltManager.applyNickname(filteredAlts.get(index));
					// refresh to update highlight color and selection
					init();
				} else {
					selectedIndex = index;
				}
			}
        }

		// input click -> set caret/selection start
        if (mouseX >= l.inputX && mouseX <= l.inputX + l.inputW && mouseY >= l.inputY && mouseY <= l.inputY + l.inputH) {
            int newIndex = getCaretIndexByMouse((float) mouseX, l.inputX);
			if (button == 0) { // left click
				if (hasShiftDown()) {
					if (selectionStart == -1) selectionStart = caretIndex;
					caretIndex = newIndex;
				} else {
					caretIndex = newIndex;
					selectionStart = -1;
				}
                inputFocused = true;
			}
			return true;
		}
        inputFocused = false;

        for (MainMenu.AnimatedButton btn : buttons) {
            if (btn.isMouseOver((int) mouseX, (int) mouseY)) {
                btn.onPress();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Layout l = layout();
        if (mouseX >= l.listX && mouseX <= l.listX + l.listW && mouseY >= l.listY && mouseY <= l.listY + l.listH) {
            float step = (float) (-vertical * 12f);
            listScrollTarget = clamp(listScrollTarget + step, 0f, listMaxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();
        Layout l = layout();

        // MainMenu-style background: dark gradient + subtle blur
        new RectangleBuilder()
                .size(new SizeState(this.width, this.height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(
                        new Color(10, 10, 15, 255),
                        new Color(10, 10, 15, 255),
                        new Color(40, 40, 50, 255),
                        new Color(40, 40, 50, 255)
                ))
                .build()
                .render(matrix, 0f, 0f, 0f);

        new BlurBuilder()
                .size(new SizeState(this.width, this.height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(new Color(15, 15, 20, 10)))
                .blurRadius(0f)
                .smoothness(30f)
                .build()
                .render(matrix, 0f, 0f, 0f);

        // Panel
        new RectangleBuilder()
            .size(new SizeState(l.panelW + 2f, l.panelH + 2f))
            .radius(new QuadRadiusState(16))
            .color(new QuadColorState(new Color(70, 90, 130, 70)))
            .smoothness(1.0f)
            .build()
            .render(matrix, l.panelX - 1f, l.panelY - 1f, 0f);

        new RectangleBuilder()
            .size(new SizeState(l.panelW, l.panelH))
            .radius(new QuadRadiusState(15))
            .color(new QuadColorState(
                new Color(18, 18, 24, 235),
                new Color(18, 18, 24, 235),
                new Color(28, 28, 36, 235),
                new Color(28, 28, 36, 235)
            ))
            .smoothness(1.0f)
            .build()
            .render(matrix, l.panelX, l.panelY, 0f);

        String title = I18n.translate("simplevisuals.alt.title");
        float titleY = l.panelY + 18f;
        float titleW = Fonts.BOLD.getWidth(title, 9f);
        float titleX = l.panelX + (l.panelW - titleW) / 2f;
        new TextBuilder()
                .font(Fonts.BOLD.font())
                .text(title)
                .size(9f)
                .color(new Color(220, 220, 240))
                .smoothness(0.5f)
                .build()
                .render(matrix, titleX, titleY, 0f);

        // Status display
        String status = String.format("%d/%d", AltManager.getAltCount(), AltManager.getMaxAlts());
        float statusX = l.panelX + l.panelW - Fonts.REGULAR.getWidth(status, 6f) - 16f;
        float statusY = titleY + 2f;
        new TextBuilder()
                .font(Fonts.REGULAR.font())
                .text(status)
                .size(6f)
                .color(new Color(180, 180, 200))
                .smoothness(0.5f)
                .build()
                .render(matrix, statusX, statusY, 0f);

        // Divider
        new RectangleBuilder()
            .size(new SizeState(l.panelW - 32f, 1f))
            .radius(new QuadRadiusState(0))
            .color(new QuadColorState(new Color(255, 255, 255, 18)))
            .smoothness(1.0f)
            .build()
            .render(matrix, l.panelX + 16f, l.panelY + 52f, 0f);

        inputFocusAnimation.update(inputFocused);
        float focusT = (float) inputFocusAnimation.getValue();

        // list background
        new RectangleBuilder()
            .size(new SizeState(l.listW, l.listH))
            .radius(new QuadRadiusState(10))
            .color(new QuadColorState(new Color(30, 30, 38, 200)))
            .smoothness(1.0f)
            .build()
            .render(matrix, l.listX, l.listY, 0f);

        // input field (border + fill)
        int inBorderA = (int) (55 + 85 * focusT);
        new RectangleBuilder()
            .size(new SizeState(l.inputW + 2f, l.inputH + 2f))
            .radius(new QuadRadiusState(10))
            .color(new QuadColorState(new Color(80, 110, 170, inBorderA)))
            .smoothness(1.0f)
            .build()
            .render(matrix, l.inputX - 1f, l.inputY - 1f, 0f);

        new RectangleBuilder()
            .size(new SizeState(l.inputW, l.inputH))
            .radius(new QuadRadiusState(9))
            .color(new QuadColorState(new Color(24, 24, 30, 255)))
            .smoothness(1.0f)
            .build()
            .render(matrix, l.inputX, l.inputY, 0f);

        // Placeholder text or input text
        if (inputBuffer.isEmpty() && !inputFocused) {
            String placeholder = I18n.translate("simplevisuals.alt.placeholder");
            new TextBuilder()
                    .font(Fonts.REGULAR.font())
                    .text(placeholder)
                    .size(7f)
                    .color(new Color(120, 120, 140))
                    .smoothness(0.5f)
                    .build()
                    .render(matrix, l.inputX + 6, l.inputY + 6, 0f);
        } else if (!inputBuffer.isEmpty()) {
            new TextBuilder()
                    .font(Fonts.REGULAR.font())
                    .text(inputBuffer)
                    .size(7f)
                    .color(new Color(230, 230, 240))
                    .smoothness(0.5f)
                    .build()
                    .render(matrix, l.inputX + 6, l.inputY + 6, 0f);
        }

        // selection highlight (if any)
        if (selectionStart != -1 && selectionStart != caretIndex && !inputBuffer.isEmpty()) {
            int a = Math.min(selectionStart, caretIndex);
            int b = Math.max(selectionStart, caretIndex);
                float ax = l.inputX + 6 + Fonts.REGULAR.getWidth(inputBuffer.substring(0, a), 7f);
                float bx = l.inputX + 6 + Fonts.REGULAR.getWidth(inputBuffer.substring(0, b), 7f);
            new RectangleBuilder()
                    .size(new SizeState(Math.max(1, bx - ax), Fonts.REGULAR.getHeight(7f)))
                    .radius(new QuadRadiusState(2))
                    .color(new QuadColorState(new Color(80, 120, 200, 120)))
                    .build()
                    .render(matrix, ax, l.inputY + 6, 0f);
        }
        // caret
        if (!inputBuffer.isEmpty() || selectionStart == -1) {
                float caretX = l.inputX + 6 + Fonts.REGULAR.getWidth(inputBuffer.substring(0, Math.min(caretIndex, inputBuffer.length())), 7f);
            new RectangleBuilder()
                    .size(new SizeState(1, Fonts.REGULAR.getHeight(7f)))
                    .radius(new QuadRadiusState(0))
                    .color(new QuadColorState(new Color(230, 230, 240, (int) (200 * (0.5f + 0.5f * (float)Math.sin(System.currentTimeMillis() / 200.0))))))
                    .build()
                    .render(matrix, caretX, l.inputY + 6, 0f);
        }

            int rowHeight = 18;

            float contentHeight = filteredAlts.size() * rowHeight;
            listMaxScroll = Math.max(0f, contentHeight - l.listH);
            listScrollTarget = clamp(listScrollTarget, 0f, listMaxScroll);

            // Smooth scrolling
            float scrollSmooth = 0.18f;
            listScroll += (listScrollTarget - listScroll) * scrollSmooth;
            listScroll = clamp(listScroll, 0f, listMaxScroll);

            hoveredRowIndex = -1;
    		if (mouseX >= l.listX && mouseX <= l.listX + l.listW && mouseY >= l.listY && mouseY <= l.listY + l.listH) {
    			hoveredRowIndex = (int) (((mouseY - l.listY) + listScroll) / rowHeight);
            }
            listHoverAnimation.update(hoveredRowIndex >= 0 && hoveredRowIndex < filteredAlts.size());
            float hoverT = listHoverAnimation.getValue();

            float visibleBottom = l.listY + l.listH;

        // Clip rendering to the list bounds (inset by 1px to align with rounded corners)
        int sx1 = Math.round(l.listX) + 1;
        int sy1 = Math.round(l.listY) + 1;
        int sx2 = Math.round(l.listX + l.listW) - 1;
        int sy2 = Math.round(l.listY + l.listH) - 1;
        drawContext.enableScissor(sx1, sy1, sx2, sy2);
        for (int i = 0; i < filteredAlts.size(); i++) {
            float rowY = l.listY + i * rowHeight - listScroll;
            if (rowY + rowHeight < l.listY) continue;
            if (rowY > visibleBottom) break;
            boolean sel = i == selectedIndex;
            if (sel) {
                new RectangleBuilder()
                        .size(new SizeState(l.listW - 8, rowHeight))
                        .radius(new QuadRadiusState(8))
                        .color(new QuadColorState(new Color(60, 60, 80, 120)))
                        .build()
                        .render(matrix, l.listX + 4, rowY, 0f);
            }
            // Hover highlight with fade
            if (i == hoveredRowIndex && !sel) {
                int ha = (int) (80 + 70 * hoverT);
                new RectangleBuilder()
                        .size(new SizeState(l.listW - 8, rowHeight))
                        .radius(new QuadRadiusState(8))
                        .color(new QuadColorState(new Color(50, 60, 90, ha)))
                        .build()
                        .render(matrix, l.listX + 4, rowY, 0f);
            }
            boolean isActive = AltManager.getLastUsedNickname() != null && filteredAlts.get(i).equalsIgnoreCase(AltManager.getLastUsedNickname());
            new TextBuilder()
                    .font(Fonts.REGULAR.font())
                    .text(filteredAlts.get(i))
                    .size(7f)
                    .color(isActive ? new Color(90, 140, 255) : new Color(230, 230, 240))
                    .smoothness(0.5f)
                    .build()
                    .render(matrix, l.listX + 8, rowY + 5, 0f);
        }
        drawContext.disableScissor();

        for (MainMenu.AnimatedButton btn : buttons) {
            btn.render(drawContext, mouseX, mouseY, delta, 1.0f);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    // ===== Input helpers =====
    private void moveCaret(int delta, boolean keepSelection) {
        int newIndex = Math.max(0, Math.min(inputBuffer.length(), caretIndex + delta));
        if (keepSelection) {
            if (selectionStart == -1) selectionStart = caretIndex;
            caretIndex = newIndex;
        } else {
            caretIndex = newIndex;
            selectionStart = -1;
        }
    }

    private void insertText(String text) {
        if (text == null || text.isEmpty()) return;
        // Filter to English-only ASCII allowed set: letters, digits, '_', '.', '-'
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'A' && c <= 'Z') ||
                (c >= 'a' && c <= 'z') ||
                (c >= '0' && c <= '9') ||
                c == '_' || c == '.' || c == '-') {
                sb.append(c);
            }
        }
        String filtered = sb.toString();
        if (filtered.isEmpty()) return;
        if (!deleteSelection()) {
            String left = inputBuffer.substring(0, caretIndex);
            String right = inputBuffer.substring(caretIndex);
            inputBuffer = left + filtered + right;
            caretIndex += filtered.length();
        }
    }

    private boolean deleteSelection() {
        if (selectionStart != -1 && selectionStart != caretIndex) {
            int a = Math.min(selectionStart, caretIndex);
            int b = Math.max(selectionStart, caretIndex);
            inputBuffer = inputBuffer.substring(0, a) + inputBuffer.substring(b);
            caretIndex = a;
            selectionStart = -1;
            return true;
        }
        return false;
    }

    private String getSelectedText() {
        if (selectionStart != -1 && selectionStart != caretIndex) {
            int a = Math.min(selectionStart, caretIndex);
            int b = Math.max(selectionStart, caretIndex);
            return inputBuffer.substring(a, b);
        }
        return null;
    }

    private int getCaretIndexByMouse(float mouseX, float inputX) {
        // Simple linear search based on measured text width
        int idx = 0;
        float target = mouseX - (inputX + 6);
        if (target <= 0) return 0;
        float lastWidth = 0f;
        for (int i = 1; i <= inputBuffer.length(); i++) {
            float w = Fonts.REGULAR.getWidth(inputBuffer.substring(0, i), 7f);
            if (w >= target) {
                // Choose nearest caret position between i-1 and i
                return (target - lastWidth) < (w - target) ? i - 1 : i;
            }
            lastWidth = w;
            idx = i;
        }
        return idx;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private void filterAlts() {
        filteredAlts.clear();
        if (searchBuffer.isEmpty()) {
            filteredAlts.addAll(alts);
        } else {
            String searchLower = searchBuffer.toLowerCase();
            for (String alt : alts) {
                if (alt.toLowerCase().contains(searchLower)) {
                    filteredAlts.add(alt);
                }
            }
        }
    }
}



