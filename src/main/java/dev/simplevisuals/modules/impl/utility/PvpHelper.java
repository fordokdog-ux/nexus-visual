package dev.simplevisuals.modules.impl.utility;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BindSetting;
import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.client.events.impl.EventTick;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.resource.language.I18n;

public class PvpHelper extends Module {

    private final BindSetting pearlBind = new BindSetting("setting.pearlBind", new Bind(GLFW.GLFW_KEY_P, false));

    private boolean pearlLatch = false;
    private boolean usingNow = false;
    private boolean forcedUseKey = false;

    public PvpHelper() {
        super("PvPHelper", Category.Utility, I18n.translate("module.pvphelper.description"));
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!isToggled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        long window = mc.getWindow().getHandle();

        // 1) Pearl: по нажатию — только взять в руку
        boolean pearlDown = isBindDown(window, pearlBind.getValue());
        if (pearlDown && !pearlLatch) {
            switchToItem(mc, Items.ENDER_PEARL);
            pearlLatch = true;
        } else if (!pearlDown) pearlLatch = false;

    }

    private static boolean isBindDown(long window, Bind bind) {
        if (bind.isMouse()) return GLFW.glfwGetMouseButton(window, bind.getKey()) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(window, bind.getKey()) == GLFW.GLFW_PRESS;
    }

    private void switchToItem(MinecraftClient mc, Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                mc.player.getInventory().selectedSlot = i;
                break;
            }
        }
    }

    private void handleHoldUse(long window, MinecraftClient mc, Bind bind, Item item) {
        boolean down = isBindDown(window, bind);
        if (down) {
            int slot = findInHotbar(mc, item);
            if (slot != -1) {
                mc.player.getInventory().selectedSlot = slot;
                // Для хилок: проверяем, что это нужное зелье
                // Если понадобится — можно отфильтровать по конкретным эффектам зелья через DataComponentTypes.POTION_CONTENTS

                // Эмулируем удержание ПКМ, чтобы шла ванильная анимация
                mc.options.useKey.setPressed(true);
                forcedUseKey = true;

                if (!usingNow && mc.player.getActiveItem().isEmpty()) {
                    usingNow = true;
                    mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
                }
            }
        } else {
            if (forcedUseKey) {
                mc.options.useKey.setPressed(false);
                forcedUseKey = false;
            }
            if (usingNow) {
                usingNow = false;
                mc.player.stopUsingItem();
            }
        }
    }

    private int findInHotbar(MinecraftClient mc, Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) return i;
        }
        return -1;
    }
}
