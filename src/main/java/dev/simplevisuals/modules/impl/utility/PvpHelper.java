package dev.simplevisuals.modules.impl.utility;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BindSetting;
import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.client.events.impl.EventTick;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.resource.language.I18n;
import dev.simplevisuals.client.util.InventoryUtil;

public class PvpHelper extends Module {

    private final BindSetting pearlBind = new BindSetting("setting.pearlBind", new Bind(GLFW.GLFW_KEY_P, false));
    private final BindSetting elytraBind = new BindSetting("setting.elytraBind", new Bind(GLFW.GLFW_KEY_Y, false));

    private boolean pearlLatch = false;
    private boolean elytraLatch = false;
    private boolean usingNow = false;
    private boolean forcedUseKey = false;

    public PvpHelper() {
        super("PvPHelper", Category.Utility, I18n.translate("module.pvphelper.description"));

        // Expose binds in ClickGUI + persist them in configs
        getSettings().add(pearlBind);
        getSettings().add(elytraBind);
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

        // 2) Elytra: toggle equip (elytra <-> chestplate) using normal inventory clicks
        boolean elytraDown = isBindDown(window, elytraBind.getValue());
        if (elytraDown && !elytraLatch) {
            toggleElytraChestSwapIfOpen(mc);
            elytraLatch = true;
        } else if (!elytraDown) elytraLatch = false;

    }

    private void toggleElytraChestSwapIfOpen(MinecraftClient mc) {
        // "Не как чит": не делаем действий в закрытом инвентаре.
        if (mc.player == null || mc.interactionManager == null) return;
        if (!(mc.currentScreen instanceof HandledScreen<?>)) return;
        if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) return;

        var h = mc.player.playerScreenHandler;
        int chestSlotId = 6; // vanilla PlayerScreenHandler layout
        if (chestSlotId < 0 || chestSlotId >= h.slots.size()) return;

        ItemStack chest = h.getSlot(chestSlotId).getStack();
        boolean wearingElytra = chest != null && !chest.isEmpty() && chest.getItem() == Items.ELYTRA;

        if (!wearingElytra) {
            // Equip elytra only from hotbar (как было)
            int hotbarIndex = InventoryUtil.findHotbar(Items.ELYTRA);
            if (hotbarIndex == -1) return;
            int sourceSlotId = InventoryUtil.indexToSlot(hotbarIndex);
            if (sourceSlotId < 0 || sourceSlotId >= h.slots.size()) return;

            if (chest == null || chest.isEmpty()) {
                mc.interactionManager.clickSlot(h.syncId, sourceSlotId, 0, SlotActionType.QUICK_MOVE, mc.player);
            } else {
                swapSlots(mc, h.syncId, sourceSlotId, chestSlotId);
            }
            return;
        }

        // Wearing elytra -> swap back to best chestplate from anywhere
        int chestplateIndex = findBestChestplateIndex();
        if (chestplateIndex == -1) return;
        int sourceSlotId = InventoryUtil.indexToSlot(chestplateIndex);
        if (sourceSlotId < 0 || sourceSlotId >= h.slots.size()) return;
        swapSlots(mc, h.syncId, sourceSlotId, chestSlotId);
    }

    private int findBestChestplateIndex() {
        // Scan player inventory indices 0..35 (hotbar + main)
        int bestIndex = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i <= 35; i++) {
            if (mc.player == null) return -1;
            ItemStack st = mc.player.getInventory().getStack(i);
            if (st == null || st.isEmpty()) continue;
            Item item = st.getItem();
            if (item == Items.ELYTRA) continue;
            if (!isChestplateItem(item)) continue;

            int score = chestplateScore(item);
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private static boolean isChestplateItem(Item item) {
        return item == Items.NETHERITE_CHESTPLATE
                || item == Items.DIAMOND_CHESTPLATE
                || item == Items.IRON_CHESTPLATE
                || item == Items.CHAINMAIL_CHESTPLATE
                || item == Items.GOLDEN_CHESTPLATE
                || item == Items.LEATHER_CHESTPLATE;
    }

    private static int chestplateScore(Item item) {
        // Prefer stronger chestplates when swapping back.
        if (item == Items.NETHERITE_CHESTPLATE) return 600;
        if (item == Items.DIAMOND_CHESTPLATE) return 500;
        if (item == Items.IRON_CHESTPLATE) return 400;
        if (item == Items.CHAINMAIL_CHESTPLATE) return 300;
        if (item == Items.GOLDEN_CHESTPLATE) return 200;
        if (item == Items.LEATHER_CHESTPLATE) return 100;
        return 10;
    }

    private static void swapSlots(MinecraftClient mc, int syncId, int slotA, int slotB) {
        mc.interactionManager.clickSlot(syncId, slotA, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, slotB, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, slotA, 0, SlotActionType.PICKUP, mc.player);
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
