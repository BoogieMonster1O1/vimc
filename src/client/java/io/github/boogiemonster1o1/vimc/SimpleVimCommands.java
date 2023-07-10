package io.github.boogiemonster1o1.vimc;

import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;

public enum SimpleVimCommands implements VimCommand {
    WQ {
        @Override
        public void perform(AbstractSignEditScreen screen) {
            WRITE.perform(screen);
            QUIT.perform(screen);
        }
    },
    WRITE {
        @Override
        public void perform(AbstractSignEditScreen screen) {
            SignEditAccess signEditAccess = (SignEditAccess) screen;
            signEditAccess.vimc$write();
        }
    },
    QUIT {
        @Override
        public void perform(AbstractSignEditScreen screen) {
            SignEditAccess signEditAccess = (SignEditAccess) screen;
            signEditAccess.vimc$quit();
        }
    };
}
