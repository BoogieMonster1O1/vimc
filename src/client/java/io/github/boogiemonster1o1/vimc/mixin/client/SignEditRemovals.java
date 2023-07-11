package io.github.boogiemonster1o1.vimc.mixin.client;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(AbstractSignEditScreen.class)
public class SignEditRemovals extends Screen {
    protected SignEditRemovals(Text title) {
        super(title);
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/AbstractSignEditScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"), method = "init")
    public Element removeDoneButton(AbstractSignEditScreen instance, Element element) {
        return element; // NO-OP
    }

    @Redirect(method = "setCurrentRowMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;setText(Lnet/minecraft/block/entity/SignText;Z)Z"))
    public boolean unsaveText(SignBlockEntity instance, SignText text, boolean front) {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Redirect(method = "renderSignText", slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 0)), at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"))
    public int removeCursor(DrawContext instance, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        return 0;
    }

    @Redirect(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    public void removeCursor2ElectricBoogaloo(DrawContext instance, int x1, int y1, int x2, int y2, int color) {
    }
}
