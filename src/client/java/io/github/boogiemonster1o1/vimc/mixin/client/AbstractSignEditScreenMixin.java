package io.github.boogiemonster1o1.vimc.mixin.client;

import io.github.boogiemonster1o1.vimc.SignEditAccess;
import io.github.boogiemonster1o1.vimc.VimHandler;
import io.github.boogiemonster1o1.vimc.VimMode;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.stream.IntStream;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen implements SignEditAccess {
	@Shadow @Final private SignBlockEntity blockEntity;
	@Shadow @Final private boolean front;
	@Shadow @Final private String[] messages;
	@Shadow protected abstract void finishEditing();
	@Shadow private SignText text;
	@Shadow private int currentRow;
	@Shadow private SelectionManager selectionManager;
	@Unique private VimHandler handler;
	@Unique public int lastKeyPress = 0;
	@Unique public int cursorAt = 0;

	protected AbstractSignEditScreenMixin(Text title) {
		super(title);
	}

	@Override
	public VimHandler vimc$getHandler() {
		return this.handler;
	}

	@Inject(at = @At("HEAD"), method = "init")
	public void initHandler(CallbackInfo ci) {
		this.handler = new VimHandler(14, 140, 0, 0);
		this.handler.save(this.text);
	}

	@Inject(method = "renderSignText", at = @At("TAIL"))
	public void setStatusBarPos(DrawContext context, CallbackInfo ci) {
		handler.setX(this.width / 2 - 70);
		handler.setY(this.height / 4 + 74);
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/DiffuseLighting;enableGuiDepthLighting()V"))
	public void renderStatusBar(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		context.getMatrices().push();
		this.handler.render(context, mouseX, mouseY, delta);
		context.getMatrices().pop();
	}

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

	@Override
	public void vimc$setText(SignText text) {
		this.text = text;
		Text[] texts = this.text.getMessages(false);
		String[] messages = IntStream.range(0, 4).mapToObj(i -> texts[i].getString()).toArray(String[]::new);
		System.arraycopy(messages,0, this.messages, 0, 4);
	}

	@Override
	public SignText vimc$getText() {
		return this.text;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	public void setSelection(CallbackInfo ci) {
		String text = Optional.ofNullable(this.messages[this.currentRow]).orElse("");
		int maxCursorPos = MathHelper.clamp(this.cursorAt, 0, text.length());
		this.selectionManager.setSelection(maxCursorPos, maxCursorPos);
	}

	@Inject(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I", ordinal = 0))
	public void renderCursor(DrawContext context, CallbackInfo ci) {
		if (this.handler.mode != VimMode.EXECUTE) {
			String text = Optional.ofNullable(this.messages[this.currentRow]).orElse("");
			int textStart = -this.textRenderer.getWidth(text) / 2;
			int maxCursorPos = MathHelper.clamp(this.cursorAt, 0, text.length());
			int xStart = textStart + this.textRenderer.getWidth(text.substring(0, maxCursorPos));
			int height = this.blockEntity.getTextLineHeight();
			int fillWidth = maxCursorPos == text.length() ? 5 : this.textRenderer.getWidth(text.substring(maxCursorPos, maxCursorPos + 1));
			int yStart = this.currentRow * height - 20;
			context.getMatrices().push();
			context.getMatrices().translate(0, 0, -1);
			context.fill(xStart, yStart, xStart + fillWidth, yStart + height, 0xFFAAAAAA);
			context.getMatrices().pop();
		}
	}

	@Inject(method = "keyPressed", at = @At("RETURN"))
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
			switch (keyCode) {
				case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
					var action = this.handler.execute();
					if (action != null) {
						action.perform((AbstractSignEditScreen) (Object) this);
					}
					cir.setReturnValue(true);
					return;
				}
				case GLFW.GLFW_KEY_BACKSPACE -> {
					handler.removeCommand();
					cir.setReturnValue(true);
					return;
				}
				case GLFW.GLFW_KEY_LEFT -> handler.shiftPos(-1);
				case GLFW.GLFW_KEY_RIGHT -> handler.shiftPos(1);
			}
			cir.setReturnValue(true);
			return;
		} else {
			switch (keyCode) {
				case GLFW.GLFW_KEY_BACKSPACE -> {
					if (this.handler.mode == VimMode.INSERT) {
						this.cursorAt = Math.max(0, this.cursorAt - 1);
					}
				}
				case GLFW.GLFW_KEY_LEFT -> {
					if (this.cursorAt > 0) {
						this.cursorAt--;
					} else if (this.currentRow > 0) {
						this.currentRow--;
						this.cursorAt = 0;
					}
					cir.setReturnValue(true);
					return;
				}
				case GLFW.GLFW_KEY_RIGHT -> {
					String message = this.messages[this.currentRow];
					if (this.cursorAt < message.length()) {
						this.cursorAt++;
					} else if (this.currentRow < 3) {
						this.currentRow++;
						this.cursorAt = this.messages[this.currentRow].length();
					}
					cir.setReturnValue(true);
					return;
				}
				case GLFW.GLFW_KEY_UP -> {
					if (this.currentRow > 0) {
						this.currentRow--;
					}
					cir.setReturnValue(true);
					return;
				}
				case GLFW.GLFW_KEY_DOWN -> {
					if (this.currentRow < this.messages.length - 1) {
						this.currentRow++;
					}
					cir.setReturnValue(true);
					return;
				}
			}
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
		} if (handler.mode == VimMode.COMMAND) {
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
				if (this.currentRow != 3 && Optional.ofNullable(this.messages[3]).map(String::isEmpty).orElse(true)) {
					// TODO
				}
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

	@Inject(method = "charTyped", at = @At("TAIL"))
	public void afterCharType(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (handler.mode == VimMode.INSERT) {
			this.cursorAt = Math.min(this.messages[this.currentRow].length(), this.cursorAt + 1);
		}
	}
}
