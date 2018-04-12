package net.osmand.plus.measurementtool.command;

interface Command {

	boolean execute();

	void undo();

	void redo();
}
