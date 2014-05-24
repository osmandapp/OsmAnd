package net.osmand.plus.gpxedit;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Koen Rabaey
 */
public class GpxEditorStates {

	public static enum Mode {START, EDIT_POI, EDIT_TRACK, EDIT_ROUTE, DELETE}

	private Mode _mode = Mode.START;

	private int _current = -1;
	private List<GpxEditorModel> _states = new LinkedList<GpxEditorModel>();

	public GpxEditorStates() {
	}

	public Mode getMode() {
		return _mode;
	}

	public void setMode(Mode mode) {
		_mode = mode;
	}

	public boolean hasCurrent() {
		return _current >= 0;
	}

	public GpxEditorModel current() {
		return _current < 0 ? null : _states.get(_current);
	}

	public GpxEditorModel createNew() {
		final GpxEditorModel current = current();
		if (current == null) {
			return new GpxEditorModel();
		}
		return new GpxEditorModel(current);
	}

	public void clear() {
		_states.clear();
		_current = -1;
	}

	public GpxEditorModel push(GpxEditorModel state) {
		_states.add(++_current, state);
		while (_current < _states.size()-1) {
			_states.remove(_states.size()-1);
		}
		return current();
	}

	public GpxEditorModel undo() {
		if (canUndo()) {
			_current--;
			return current();
		}
		return null;
	}

	public GpxEditorModel redo() {
		if (canRedo()) {
			_current++;
			return current();
		}
		return null;
	}

	public boolean canRedo() {
		return _current < _states.size() - 1;
	}

	public boolean canUndo() {
		return _current >= 0;
	}
}
