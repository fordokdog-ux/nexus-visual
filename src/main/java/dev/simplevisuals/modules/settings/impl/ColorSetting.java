package dev.simplevisuals.modules.settings.impl;

import dev.simplevisuals.modules.settings.Setting;
import java.awt.*;
import java.util.function.Supplier;

/**
 * Настройка цвета (RGB) с кэшированием
 */
public class ColorSetting extends Setting<Integer> {
    private Integer cachedValue;// Хранение родительского модуля для возможного использования
    private Runnable onAction; // Хранение действия для onAction
    private Runnable onSetVisible; // Хранение действия для onSetVisible

    public ColorSetting(String name) {
        super(name, Color.RED.getRGB()); // По умолчанию красный цвет
        this.cachedValue = Color.RED.getRGB();
    }

    public ColorSetting(String name, Integer value) {
        super(name, value);
        this.cachedValue = value;
    }

    public ColorSetting set(Integer value) {
        super.setValue(value); // Вызов метода setValue из Setting
        this.cachedValue = value;
        if (onAction != null) {
            onAction.run(); // Выполнение зарегистрированного действия
        }
        return this; // Поддержка цепочки вызовов
    }

    @Override
    public void setValue(Integer value) {
        Integer before = null;
        try {
            before = super.getValue();
        } catch (Throwable ignored) {}

        super.setValue(value);

        Integer after = null;
        try {
            after = super.getValue();
        } catch (Throwable ignored) {}

        this.cachedValue = after;
        if (onAction != null) {
            // Запускаем действие только если изменение реально применилось
            if (before == null || after == null || !before.equals(after)) {
                onAction.run();
            }
        }
    }

    // Исправление: использование сеттера setVisible вместо прямого доступа к полю visible
    @Override
    public void setVisible(Supplier<Boolean> visible) {
        super.setVisible(visible); // Вызов публичного сеттера из Setting
        if (onSetVisible != null) {
            onSetVisible.run(); // Исправлено: onVisible → onSetVisible
        }
    }

    // Реализация onAction, так как метода нет в Setting
    public ColorSetting onAction(Runnable action) {
        this.onAction = action; // Сохранение действия
        return this; // Поддержка цепочки вызовов
    }

    // Реализация onSetVisible, так как метода нет в Setting
    public ColorSetting onSetVisible(Runnable action) {
        this.onSetVisible = action; // Сохранение действия
        return this; // Поддержка цепочки вызовов
    }

    @Override
    public Integer getValue() {
        if (cachedValue == null) {
            cachedValue = super.getValue();
        }
        return cachedValue;
    }

    @Override
    public void reset() {
        super.reset();
        cachedValue = super.getValue();
        if (onAction != null) {
            onAction.run();
        }
    }

    /**
     * Установка цвета с использованием java.awt.Color
     */
    public void setColor(Color color) {
        set(color.getRGB());
    }

    /**
     * Получение цвета как java.awt.Color
     */
    public Color getColor() {
        return new Color(getValue(), true);
    }
}