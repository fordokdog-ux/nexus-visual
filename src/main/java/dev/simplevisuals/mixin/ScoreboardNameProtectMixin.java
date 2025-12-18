package dev.simplevisuals.mixin;

import dev.simplevisuals.modules.impl.utility.NameProtect;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InGameHud.class)
public class ScoreboardNameProtectMixin {

    @ModifyArg(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"),
            index = 1, require = 0)
    private Text sv$replaceTextWithShadow(Text text) {
        return sv$replaceText(text);
    }

    @ModifyArg(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"),
            index = 1, require = 0)
    private Text sv$replaceTextNoShadow(Text text) {
        return sv$replaceText(text);
    }

    @ModifyArg(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"),
            index = 1, require = 0)
    private String sv$replaceStringWithShadow(String text) {
        return sv$replaceString(text);
    }

    @ModifyArg(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I"),
            index = 1, require = 0)
    private String sv$replaceStringNoShadow(String text) {
        return sv$replaceString(text);
    }

    private static Text sv$replaceText(Text text) {
        if (text == null) return null;
        NameProtect np = NameProtect.getInstance();
        if (np == null || !np.isToggled()) return text;

        String before = text.getString();
        String after = np.replaceNames(before);

        if (before.equals(after)) {
            MutableText copy = text.copy();
            for (int i = 0; i < copy.getSiblings().size(); i++) {
                copy.getSiblings().set(i, sv$replaceText(copy.getSiblings().get(i)));
            }
            return copy;
        }

        MutableText rebuilt = sv$fromLegacy(after, text.getStyle());
        for (Text sibling : text.getSiblings()) {
            rebuilt.append(sv$replaceText(sibling));
        }
        return rebuilt;
    }

    private static String sv$replaceString(String text) {
        if (text == null) return null;
        NameProtect np = NameProtect.getInstance();
        if (np == null || !np.isToggled()) return text;
        return np.replaceNames(text);
    }

    private static MutableText sv$fromLegacy(String input, Style baseStyle) {
        MutableText root = Text.literal("");
        Style current = baseStyle;
        StringBuilder segment = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char ch = input.charAt(i);
            if (ch == '\u00A7' && i + 1 < input.length()) {
                if (segment.length() > 0) {
                    root.append(Text.literal(segment.toString()).setStyle(current));
                    segment.setLength(0);
                }
                Formatting formatting = Formatting.byCode(input.charAt(i + 1));
                if (formatting != null) {
                    current = sv$applyFormatting(baseStyle, current, formatting);
                }
                i += 2;
                continue;
            }
            segment.append(ch);
            i++;
        }
        if (segment.length() > 0 || root.getSiblings().isEmpty()) {
            root.append(Text.literal(segment.toString()).setStyle(current));
        }
        return root;
    }

    private static Style sv$applyFormatting(Style base, Style current, Formatting formatting) {
        if (formatting == Formatting.RESET) {
            return base;
        }
        if (formatting.isColor()) {
            TextColor color = TextColor.fromFormatting(formatting);
            return color != null ? base.withColor(color) : base;
        }
        return switch (formatting) {
            case BOLD -> current.withBold(true);
            case ITALIC -> current.withItalic(true);
            case UNDERLINE -> current.withUnderline(true);
            case STRIKETHROUGH -> current.withStrikethrough(true);
            case OBFUSCATED -> current.withObfuscated(true);
            default -> current;
        };
    }
}
