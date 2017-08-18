package net.osmand.plus.measurementtool.command;

import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.Deque;
import java.util.LinkedList;

public class MeasurementCommandManager {

	private final Deque<MeasurementModeCommand> undoCommands = new LinkedList<>();
	private final Deque<MeasurementModeCommand> redoCommands = new LinkedList<>();

	public boolean canUndo() {
		return undoCommands.size() > 0;
	}

	public boolean canRedo() {
		return redoCommands.size() > 0;
	}

	public boolean execute(MeasurementModeCommand command) {
		if (command.execute()) {
			undoCommands.push(command);
			redoCommands.clear();
			return true;
		}
		return false;
	}

	public void undo() {
		if (canUndo()) {
			MeasurementModeCommand command = undoCommands.pop();
			redoCommands.push(command);
			command.undo();
		}
	}

	public void redo() {
		if (canRedo()) {
			MeasurementModeCommand command = redoCommands.pop();
			undoCommands.push(command);
			command.redo();
		}
	}

	public void resetMeasurementLayer(MeasurementToolLayer layer) {
		for (MeasurementModeCommand command : undoCommands) {
			command.setMeasurementLayer(layer);
		}
		for (MeasurementModeCommand command : redoCommands) {
			command.setMeasurementLayer(layer);
		}
	}
}
