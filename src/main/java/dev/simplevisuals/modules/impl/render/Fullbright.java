package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import dev.simplevisuals.client.events.impl.EventTick;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;

import java.lang.reflect.Field;

public class Fullbright extends Module {

    private final NumberSetting brightness = new NumberSetting("Яркость", 16.0f, 1.0f, 32.0f, 0.5f);
    private Double prevGamma = null;

    public Fullbright() {
        super("Fullbright", Category.Render, I18n.translate("module.fullbright.description"));
        getSettings().add(brightness);
    }

    public float getBrightness() {
        return brightness.getValue().floatValue();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        try {
            if (prevGamma == null) prevGamma = getGammaValue();
            applyGamma();
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDisable() {
        try {
            if (prevGamma != null) setGammaValue(prevGamma);
        } catch (Throwable ignored) {}
        prevGamma = null;
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!isToggled()) return;
        try {
            applyGamma();
        } catch (Throwable ignored) {}
    }

    private void applyGamma() throws Exception {
        setGammaValue((double) getBrightness());
    }

    private static Object getGammaOption() throws Exception {
        Object opts = mc.options;
        if (opts == null) throw new IllegalStateException("options_null");

        try {
            return opts.getClass().getMethod("getGamma").invoke(opts);
        } catch (Throwable ignored) {}

        try {
            Field f = opts.getClass().getDeclaredField("gamma");
            f.setAccessible(true);
            return f.get(opts);
        } catch (Throwable t) {
            throw new IllegalStateException("gamma_option_missing", t);
        }
    }

    private static double getGammaValue() throws Exception {
        Object opt = getGammaOption();
        if (opt == null) return 1.0;

        try {
            Object v = opt.getClass().getMethod("getValue").invoke(opt);
            if (v instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {}

        try {
            Object v = opt.getClass().getMethod("get").invoke(opt);
            if (v instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {}

        return 1.0;
    }

    private static void setGammaValue(double value) throws Exception {
        Object opt = getGammaOption();
        if (opt == null) return;

        try {
            opt.getClass().getMethod("setValue", Object.class).invoke(opt, value);
            return;
        } catch (Throwable ignored) {}

        try {
            opt.getClass().getMethod("setValue", Double.class).invoke(opt, value);
            return;
        } catch (Throwable ignored) {}

        try {
            opt.getClass().getMethod("setValue", double.class).invoke(opt, value);
        } catch (Throwable ignored) {}
    }
}