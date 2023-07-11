package io.github.boogiemonster1o1.vimc;

import net.minecraft.block.entity.SignText;

public interface SignEditAccess {
    VimHandler vimc$getHandler();

    void vimc$write();

    void vimc$quit();

    void vimc$setText(SignText text);

    SignText vimc$getText();
}
