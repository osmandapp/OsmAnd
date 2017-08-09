package net.osmand.plus.measurementtool.command;

public interface Command {

	boolean execute();

	void undo();

	void redo();
}
