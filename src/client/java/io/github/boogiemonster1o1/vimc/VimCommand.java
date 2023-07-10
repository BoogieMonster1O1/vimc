package io.github.boogiemonster1o1.vimc;

import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;

public interface VimCommand {
    void perform(AbstractSignEditScreen screen);
}
