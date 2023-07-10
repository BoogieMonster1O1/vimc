package io.github.boogiemonster1o1.vimc.mixin.client;

import io.github.boogiemonster1o1.vimc.SignEditAccess;
import io.github.boogiemonster1o1.vimc.VimMode;
import io.github.boogiemonster1o1.vimc.VimHandler;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen implements SignEditAccess {
	@Shadow @Final private SignBlockEntity blockEntity;
	@Shadow @Final private boolean front;
	@Shadow @Final private String[] messages;

	@Shadow protected abstract void finishEditing();

	@Shadow private SignText text;
	@Unique
	private VimHandler handler;

	protected AbstractSignEditScreenMixin(Text title) {
		super(title);
	}

	@Inject(at = @At("HEAD"), method = "init")
	public void initStatusBar(CallbackInfo ci) {
		this.handler = new VimHandler(14, 140, 0, 0);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/AbstractSignEditScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"), method = "init")
	public Element removeDoneButton(AbstractSignEditScreen instance, Element element) {
		return element; // NO-OP
	}

	@Redirect(method = "setCurrentRowMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;setText(Lnet/minecraft/block/entity/SignText;Z)Z"))
	public boolean unsaveText(SignBlockEntity instance, SignText text, boolean front) {
		return false;
	}

	@Inject(method = "renderSignText", at = @At("TAIL"))
	public void setStatusBarPos(DrawContext context, CallbackInfo ci) {
		var x = this.width / 2 - 70;
		var y = this.height / 4 + 74;
		handler.setX(x);
		handler.setY(y);
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/DiffuseLighting;enableGuiDepthLighting()V"))
	public void renderStatusBar(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		context.getMatrices().push();
		context.getMatrices().translate(0, 0, 100);
		this.handler.render(context, mouseX, mouseY, delta);
		context.getMatrices().pop();
	}

	@Unique
	public int lastKeyPress = 0;

	@Override
	public void vimc$write() {
		var networkHandler = MinecraftClient.getInstance().getNetworkHandler();
		if (networkHandler != null) {
			networkHandler.sendPacket(new UpdateSignC2SPacket(this.blockEntity.getPos(), this.front, this.messages[0], this.messages[1], this.messages[2], this.messages[3]));
			this.blockEntity.setText(this.text, this.front);
		}
	}

	@Override
	public void vimc$quit() {
		this.finishEditing();
	}

	@Redirect(method = "renderSignText", slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 0)), at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"))
	public int interceptBlinker(DrawContext instance, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
		if (this.handler.mode == VimMode.INSERT) {
			return instance.drawText(textRenderer, text, x, y, color, shadow);
		}
		return 0;
	}

	@Inject(method = "keyPressed", at = @At("TAIL"))
	public void setLastKeyPress(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		this.lastKeyPress = keyCode;
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	public void interceptKeypress(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			if (handler.mode.isInput() || lastKeyPress == keyCode) {
				handler.enterCommandMode();
				cir.setReturnValue(true);
				return;
			}
		}
		if (handler.mode == VimMode.EXECUTE) {
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				var action = this.handler.execute();
				if (action != null) {
					action.perform((AbstractSignEditScreen) (Object) this);
				}
				cir.setReturnValue(true);
				return;
			} else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				handler.removeCommand();
				cir.setReturnValue(true);
				return;
			} else if (keyCode == GLFW.GLFW_KEY_LEFT) {
				handler.shiftPos(-1);
			} else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
				handler.shiftPos(1);
			}
			cir.setReturnValue(true);
			return;
		}
		if (handler.mode == VimMode.COMMAND) {
			cir.setReturnValue(true);
			return;
		}
	}

	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	public void interceptCharType(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (handler.mode == VimMode.EXECUTE) {
			handler.appendCommand(chr);
			cir.setReturnValue(true);
			return;
		}
		if (handler.mode == VimMode.COMMAND) {
			if (chr == ':') {
				handler.enterExecuteMode();
				cir.setReturnValue(true);
				return;
			}
			if (chr == 'i') {
				handler.enterInsertMode();
				cir.setReturnValue(true);
				return;
			}
			if (chr == 'o') {
				handler.enterInsertMode();
				// TODO
				cir.setReturnValue(true);
				return;
			}
			if (chr == 'r') {
				handler.enterReplaceMode();
				cir.setReturnValue(true);
				return;
			}
			cir.setReturnValue(true);
			return;
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
