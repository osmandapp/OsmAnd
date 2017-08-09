package net.osmand.plus.measurementtool.command;

import java.util.Deque;
import java.util.LinkedList;

public class CommandManager {

	private final Deque<Command> undoCommands = new LinkedList<>();
	private final Deque<Command> redoCommands = new LinkedList<>();

	public boolean canUndo() {
		return undoCommands.size() > 0;
	}

	public boolean canRedo() {
		return redoCommands.size() > 0;
	}

	public void execute(Command command) {
		if (command.execute()) {
			undoCommands.push(command);
			redoCommands.clear();
		}
	}

	public void undo() {
		if (canUndo()) {
			Command command = undoCommands.pop();
			redoCommands.push(command);
			command.undo();
		}
	}

	public void redo() {
		if (canRedo()) {
			Command command = redoCommands.pop();
			undoCommands.push(command);
			command.redo();
		}
	}

	public void clear() {
		undoCommands.clear();
		redoCommands.clear();
	}
}
