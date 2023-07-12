package io.github.boogiemonster1o1.vimc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum SimpleVimCommands implements VimCommand {
	WQ {
		@Override
		public void perform(AbstractSignEditScreen screen, boolean force) {
			WRITE.perform(screen, force);
			QUIT.perform(screen, force);
		}
	},
	WRITE {
		@Override
		public void perform(AbstractSignEditScreen screen, boolean force) {
			screen.vimc$write();
			VimHandler handler = screen.vimc$getHandler();
			handler.saved = screen.vimc$getText();
			handler.enterCommandMode();
			String joined = Arrays.stream(handler.saved.getMessages(MinecraftClient.getInstance().shouldFilterText())).map(Text::getString).collect(Collectors.joining());
			handler.command = "4 L, " + (joined.getBytes().length + 4) + "B written";
		}
	},
	QUIT {
		@Override
		public void perform(AbstractSignEditScreen screen, boolean force) {
			VimHandler handler = screen.vimc$getHandler();
			if (!force && screen.vimc$getText() != handler.saved) {
				handler.enterCommandMode();
				handler.command = "E37: No write since last change (add ! to override)";
			} else {
				screen.vimc$setText(screen.vimc$getHandler().saved);
				screen.vimc$quit();
			}
		}
	}
}
