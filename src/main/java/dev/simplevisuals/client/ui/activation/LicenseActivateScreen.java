package dev.simplevisuals.client.ui.activation;

import com.google.gson.JsonObject;
import dev.simplevisuals.NexusVisual;
import dev.simplevisuals.util.licensing.LicenseApi;
import dev.simplevisuals.util.licensing.LicenseManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LicenseActivateScreen extends Screen {

    private static final String SAVED_KEY_FILENAME = "saved_key.txt";

    private final Screen parent;

    private TextFieldWidget keyField;
    private ButtonWidget activateBtn;
    private ButtonWidget backBtn;

    private String statusLine = "";
    private boolean busy = false;

    public LicenseActivateScreen(Screen parent) {
        super(Text.literal("Активация"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = this.width;
        int h = this.height;

        int boxW = Math.min(320, w - 40);
        int x = (w - boxW) / 2;
        int y = h / 2 - 30;

        keyField = new TextFieldWidget(this.textRenderer, x, y, boxW, 20, Text.literal("Ключ"));
        keyField.setMaxLength(128);
        keyField.setPlaceholder(Text.literal("Вставь ключ активации"));
        
        // Автоподстановка сохранённого ключа
        String savedKey = loadSavedKey();
        if (savedKey != null && !savedKey.isBlank()) {
            keyField.setText(savedKey);
        }
        
        addDrawableChild(keyField);

        int btnW = (boxW - 6) / 2;
        activateBtn = ButtonWidget.builder(Text.literal("Активировать"), btn -> onActivate())
                .dimensions(x, y + 28, btnW, 20)
                .build();
        addDrawableChild(activateBtn);

        backBtn = ButtonWidget.builder(Text.literal("Назад"), btn -> onBack())
                .dimensions(x + btnW + 6, y + 28, btnW, 20)
                .build();
        addDrawableChild(backBtn);

        setInitialFocus(keyField);
    }

    private void setBusy(boolean value) {
        busy = value;
        if (activateBtn != null) activateBtn.active = !value;
        if (backBtn != null) backBtn.active = !value;
        if (keyField != null) keyField.setEditable(!value);
    }

    private void onBack() {
        if (busy) return;
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void onActivate() {
        if (busy) return;

        String code = keyField.getText();
        if (code == null || code.trim().isEmpty()) {
            statusLine = "Введи ключ";
            return;
        }

        String uuid = LicenseManager.getCurrentSessionUuidString();
        if (uuid == null || uuid.isBlank()) {
            statusLine = "UUID не найден (зайди в мир/аккаунт)";
            return;
        }

        String hwid = LicenseManager.getHwidSafe();
        setBusy(true);
        statusLine = "Проверяем ключ...";

        new Thread(() -> {
            try {
                JsonObject resp = LicenseApi.redeem(LicenseManager.getServerUrl(), code, uuid, hwid);

                JsonObject lic = resp.has("license") ? resp.getAsJsonObject("license") : resp;
                String payload = lic.has("payload") ? lic.get("payload").getAsString() : null;
                String signature = lic.has("signature") ? lic.get("signature").getAsString() : null;
                if (payload == null || payload.isBlank() || signature == null || signature.isBlank()) {
                    throw new IllegalStateException("bad_license_format");
                }

                String licenseJson = "{\n" +
                        "  \"payload\": " + quote(payload) + ",\n" +
                        "  \"signature\": " + quote(signature) + "\n" +
                        "}\n";

                if (LicenseManager.getLicenseFile() == null) {
                    throw new IllegalStateException("license_file_null");
                }

                Files.createDirectories(LicenseManager.getLicenseFile().toPath().getParent());
                Files.writeString(LicenseManager.getLicenseFile().toPath(), licenseJson, StandardCharsets.UTF_8);

                // Сохраняем ключ для будущей повторной активации
                saveKey(code);

                LicenseManager.refresh();

                MinecraftClient.getInstance().execute(() -> {
                    setBusy(false);
                    if (LicenseManager.isLicensed()) {
                        statusLine = "Готово: лицензия активирована";
                        MinecraftClient.getInstance().setScreen(parent);
                    } else {
                        statusLine = "Не активировалось: " + LicenseManager.getReason();
                    }
                });
            } catch (Throwable t) {
                MinecraftClient.getInstance().execute(() -> {
                    setBusy(false);
                    String msg = null;
                    try {
                        msg = t.getMessage();
                    } catch (Throwable ignored) {}
                    if (msg == null || msg.isBlank()) msg = t.getClass().getSimpleName();
                    statusLine = "Ошибка: " + msg;
                });
            }
        }, "NV-LicenseActivate").start();
    }

    private static String quote(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Сохраняет ключ активации в файл для повторного использования
     */
    private static void saveKey(String key) {
        try {
            if (key == null || key.isBlank()) return;
            File licFile = LicenseManager.getLicenseFile();
            if (licFile == null) return;
            Path keyPath = licFile.toPath().getParent().resolve(SAVED_KEY_FILENAME);
            Files.createDirectories(keyPath.getParent());
            Files.writeString(keyPath, key.trim(), StandardCharsets.UTF_8);
        } catch (Throwable ignored) {}
    }

    /**
     * Загружает ранее сохранённый ключ активации
     */
    private static String loadSavedKey() {
        try {
            File licFile = LicenseManager.getLicenseFile();
            if (licFile == null) return null;
            Path keyPath = licFile.toPath().getParent().resolve(SAVED_KEY_FILENAME);
            if (!Files.exists(keyPath)) return null;
            String key = Files.readString(keyPath, StandardCharsets.UTF_8);
            return key == null ? null : key.trim();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter
            onActivate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int w = this.width;
        int y = this.height / 2 - 70;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Nexus Visual — Активация"), w / 2, y, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Введи ключ, нажми Активировать"), w / 2, y + 12, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);

        if (statusLine != null && !statusLine.isBlank()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(statusLine), w / 2, this.height / 2 + 60, 0xFFFFFF);
        }

        if (!busy) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("license.json: " + (LicenseManager.getLicenseFile() == null ? "unknown" : LicenseManager.getLicenseFile().getAbsolutePath())),
                    w / 2,
                    this.height - 16,
                    0x777777
            );

            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("server: " + LicenseManager.getServerUrl()),
                    w / 2,
                    this.height - 28,
                    0x777777
            );
        }
    }
}
