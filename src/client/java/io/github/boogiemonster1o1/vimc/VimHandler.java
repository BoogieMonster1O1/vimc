package io.github.boogiemonster1o1.vimc;

import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

public class VimHandler implements Drawable, Widget, Element {
    private final int height;
    private final int width;
    private int x;
    private int y;
    private boolean isEditing = false;
    public VimMode mode = VimMode.COMMAND;
    String command = null;
    private int position = 0;
    SignText saved = null;

    public VimHandler(int height, int width, int x, int y) {
        this.height = height;
        this.width = width;
        this.x = x;
        this.y = y;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().translate(0, 0, 100);
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000);
        int color = isErrorCommand() ? 0xFFFF0000 : 0xFF000000;
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);
        if (this.mode == VimMode.EXECUTE) {
            int txtLength = textRenderer.getWidth(command.substring(0, position + 1));
            int fillLength = getFillLength(textRenderer);
            context.fill(this.getX() + 3 + txtLength, this.getY() + 2, this.getX() + 3 + txtLength + fillLength, this.getY() + 12, 0xFFAAAAAA);
        }
        switch (this.mode) {
            case INSERT -> context.drawTextWithShadow(textRenderer, "-- INSERT --", this.getX() + 3, this.getY() + 3, 0xFFFFFFFF);
            case REPLACE -> context.drawTextWithShadow(textRenderer, "-- REPLACE --", this.getX() + 3, this.getY() + 3, 0xFFFFFFFF);
            case COMMAND, EXECUTE -> context.drawTextWithShadow(textRenderer, this.command, this.getX() + 3, this.getY() + 3, 0xFFFFFFFF);
        }
    }

    public void save(SignText newText) {
        this.saved = newText;
    }

    public boolean isErrorCommand() {
        if (command == null) {
            return false;
        }

        return command.startsWith("Not");
    }

    public int getFillLength(TextRenderer renderer) {
        if ((position + 1) == command.length()) {
            return 5;
        } else {
            char character = command.charAt(position + 1);
            String characterString = String.valueOf(character);
            return renderer.getWidth(characterString);
        }
    }

    public void enterExecuteMode() {
        this.command = ":";
        this.mode = VimMode.EXECUTE;
        this.position = 0;
    }

    public void enterCommandMode() {
        resetCommand();
        this.mode = VimMode.COMMAND;
    }

    public void enterInsertMode() {
        resetCommand();
        this.mode = VimMode.INSERT;
    }

    public void enterReplaceMode() {
        resetCommand();
        this.mode = VimMode.REPLACE;
    }

    public void execute(AbstractSignEditScreen screen) {
        String command = this.command.substring(1);
        var action =  switch (command) {
            case "wq", "x" -> SimpleVimCommands.WQ;
            case "w" -> SimpleVimCommands.WRITE;
            case "q" -> SimpleVimCommands.QUIT;
            default -> {
                this.enterCommandMode();
                this.command = "Not an editor command: " + command;
                yield null;
            }
        };
	if (action != null) {
	    action.perform(screen, false); // TODO        
	}
    }

    public void resetCommand() {
        this.command = null;
        this.position = 0;
    }

    public void appendCommand(char ch) {
        if (position >= 0 && position < command.length()) {
            StringBuilder builder = new StringBuilder(command);
            builder.insert(position + 1, ch);
            command = builder.toString();
        } else {
            command = command + ch;
        }
        position++;
    }

    public void shiftPos(int amount) {
        this.position = MathHelper.clamp(this.position + amount, 0, this.command.length() - 1);
    }

    public void removeCommand() {
        if (position > 0) {
            StringBuilder builder = new StringBuilder(command);
            builder.deleteCharAt(position);
            command = builder.toString();
            position--;
        } else if (position == 0 && command.length() == 1) {
            enterCommandMode();
        }
    }

    @Override
    public void setFocused(boolean focused) {
        // NO-OP
    }

    public void setEditing() {
        this.isEditing = true;
    }

    @Override
    public boolean isFocused() {
        return this.isEditing;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void forEachChild(Consumer<ClickableWidget> consumer) {
    }

    @Override
    public ScreenRect getNavigationFocus() {
        return Widget.super.getNavigationFocus();
    }
}
