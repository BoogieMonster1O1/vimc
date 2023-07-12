package io.github.boogiemonster1o1.vimc;

public enum VimMode {
	COMMAND(false),
	EXECUTE(false),
	INSERT(true),
	REPLACE(true);

	private final boolean input;

	VimMode(boolean input) {
		this.input = input;
	}

	public boolean isInput() {
		return input;
	}
}
