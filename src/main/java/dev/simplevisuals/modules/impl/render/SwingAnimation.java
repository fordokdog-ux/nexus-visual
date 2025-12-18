package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import dev.simplevisuals.modules.settings.impl.NumberSetting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SwingAnimation extends Module {

    private static final SwingAnimation INSTANCE = new SwingAnimation();

    // Settings
    private final BooleanSetting modeNormal = new BooleanSetting("mode.normal", true, () -> false);
    private final BooleanSetting modeFirst = new BooleanSetting("mode.first", false, () -> false);
    private final BooleanSetting modeSecond = new BooleanSetting("mode.second", false, () -> false);
    private final BooleanSetting modeThird = new BooleanSetting("mode.third", false, () -> false);
    private final BooleanSetting modeFourth = new BooleanSetting("mode.fourth", false, () -> false);
    private final BooleanSetting modeSixth = new BooleanSetting("mode.sixth", false, () -> false);
    private final BooleanSetting modeSeventh = new BooleanSetting("mode.seventh", false, () -> false);
    private final BooleanSetting modeCustom = new BooleanSetting("mode.custom", false, () -> false);

    private final ListSetting animationMode = new ListSetting(
            I18n.translate("setting.animationMode"), true,
            modeNormal, modeFirst, modeSecond, modeThird, modeFourth,
            modeSixth, modeSeventh, modeCustom
    );

    private final NumberSetting swingPower = new NumberSetting("setting.swingPower", 5.0f, 1.0f, 10.0f, 0.05f);
    private static final float scale = 0.5f;

    // Custom mode settings (visible only when Custom is selected)
    private final NumberSetting customTranslateX = new NumberSetting(
            "custom.translateX", 0.0f, -2.0f, 2.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customTranslateY = new NumberSetting(
            "custom.translateY", 0.0f, -2.0f, 2.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customTranslateZ = new NumberSetting(
            "custom.translateZ", 0.0f, -2.0f, 2.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customRotateX = new NumberSetting(
            "custom.rotateX", 0.0f, -360.0f, 360.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customRotateY = new NumberSetting(
            "custom.rotateY", 0.0f, -360.0f, 360.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customRotateZ = new NumberSetting(
            "custom.rotateZ", 0.0f, -360.0f, 360.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customScaleX = new NumberSetting(
            "custom.scaleX", 1.0f, 0.1f, 3.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customScaleY = new NumberSetting(
            "custom.scaleY", 1.0f, 0.1f, 3.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customScaleZ = new NumberSetting(
            "custom.scaleZ", 1.0f, 0.1f, 3.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    // Always-on curve and equip offset (settings removed by request)

    // Custom idle rotation (always rotate without hits/interactions)
    // Base direction angle (constant yaw)
    private final NumberSetting customBaseYaw = new NumberSetting(
            "custom.baseYaw", 0.0f, -180.0f, 180.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customBasePitch = new NumberSetting(
            "custom.basePitch", 0.0f, -180.0f, 180.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customBaseRoll = new NumberSetting(
            "custom.baseRoll", 0.0f, -180.0f, 180.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final BooleanSetting customIdleRotate = new BooleanSetting(
            "custom.idleRotate", false,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue()
    );
    private final NumberSetting customIdleRotateSpeedX = new NumberSetting(
            "custom.idleRotateSpeedX", 0.0f, -720.0f, 720.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue() && customIdleRotate.getValue()
    );
    private final NumberSetting customIdleRotateSpeedY = new NumberSetting(
            "custom.idleRotateSpeedY", 90.0f, -720.0f, 720.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue() && customIdleRotate.getValue()
    );
    private final NumberSetting customIdleRotateSpeedZ = new NumberSetting(
            "custom.idleRotateSpeedZ", 0.0f, -720.0f, 720.0f, 0.01f,
            () -> animationMode.getName("mode.custom") != null && animationMode.getName("mode.custom").getValue() && customIdleRotate.getValue()
    );


    public SwingAnimation() {
        super("SwingAnimation", Category.Render, I18n.translate("module.swinganimation.description"));
    }

    public void renderSwordAnimation(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
        // Ensure only one mode is active
        BooleanSetting activeMode = animationMode.getToggled().stream().findFirst().orElse(modeNormal);
        if (animationMode.getToggled().size() > 1) {
            // If multiple modes are toggled, disable all except the first
            BooleanSetting finalActiveMode = activeMode;
            animationMode.getValue().forEach(setting -> {
                if (setting != finalActiveMode) {
                    setting.setValue(false);
                }
            });
        } else if (animationMode.getToggled().isEmpty()) {
            // If no modes are toggled, enable Normal
            modeNormal.setValue(true);
            activeMode = modeNormal;
        }

        float power = swingPower.getValue().floatValue();
        float anim = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float scaleValue = scale;

        switch (activeMode.getName()) {
            case "mode.normal" -> {
                matrices.translate(0.56F, -0.52F, -0.72F);
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -60.0F * power / 5.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(g * -30.0F * power / 5.0f));
            }
            case "mode.first" -> {
                if (swingProgress > 0) {
                    float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    matrices.translate(0.56F, equipProgress * -0.2f - 0.5F, -0.7F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -85.0F * power / 5.0f));
                    matrices.translate(-0.1F, 0.28F, 0.2F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0F));
                } else {
                    float n = -0.4f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float m = 0.2f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI * 2);
                    float f1 = -0.2f * MathHelper.sin(swingProgress * (float) Math.PI);
                    matrices.translate(n, m, f1);
                    applyEquipOffset(matrices, arm, equipProgress);
                    applySwingOffset(matrices, arm, swingProgress);
                }
            }
            case "mode.second" -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f + 20f * g * power / 5.0f));
            }
            case "mode.third" -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-30f * (1f - g) - 30f ));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f));
            }
            case "mode.fourth" -> {
                float g = MathHelper.sin(swingProgress * (float) Math.PI);
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0.1F, -0.2F, -0.3F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30f * g * power / 5.0f - 36f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25f * g * power / 5.0f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(12f));
            }
            case "mode.sixth" -> {
                matrices.scale(scaleValue, scaleValue, scaleValue);
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15 * anim));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0 * anim));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-(power * 10) * anim));
            }
            case "mode.seventh" -> {
                matrices.scale(scaleValue+0.1f, scaleValue, scaleValue-0.1f);
                applyEquipOffset(matrices, arm, 0);
                matrices.translate(0.2f * anim, 0, -0.5f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * anim * power / 5.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-140 * anim * power / 5.0f));
            }
            case "mode.custom" -> {
                // Equip offset must always be applied in custom mode
                applyEquipOffset(matrices, arm, equipProgress);
                // Swing curve always enabled with multiplier = 1
                float curve = anim * 1.0f;
                // Base 3D rotation first so subsequent transforms follow this direction
                if (customBasePitch.getValue() != 0.0f) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(customBasePitch.getValue()));
                if (customBaseYaw.getValue()   != 0.0f) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(customBaseYaw.getValue()));
                if (customBaseRoll.getValue()  != 0.0f) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(customBaseRoll.getValue()));
                matrices.translate(
                        customTranslateX.getValue() * curve,
                        customTranslateY.getValue() * curve,
                        customTranslateZ.getValue() * curve
                );

                // Apply idle rotation independent from swing if enabled
                if (customIdleRotate.getValue()) {
                    float time = (System.currentTimeMillis() % 100000L) / 1000.0f; // seconds
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(time * customIdleRotateSpeedX.getValue()));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * customIdleRotateSpeedY.getValue()));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time * customIdleRotateSpeedZ.getValue()));
                }

                // Additional rotation from curve
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(customRotateX.getValue() * curve));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(customRotateY.getValue() * curve));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(customRotateZ.getValue() * curve));

                matrices.scale(
                        customScaleX.getValue(),
                        customScaleY.getValue(),
                        customScaleZ.getValue()
                );
            }
        }
    }

    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }

    // Getter for singleton instance
    public static SwingAnimation getInstance() {
        return INSTANCE;
    }
}