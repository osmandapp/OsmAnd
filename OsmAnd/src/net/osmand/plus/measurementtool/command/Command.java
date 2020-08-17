package net.osmand.plus.measurementtool.command;

import androidx.annotation.NonNull;

interface Command {

	boolean execute();

	boolean update(@NonNull Command command);

	void undo();

	void redo();
}
