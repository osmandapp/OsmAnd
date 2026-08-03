package net.osmand.plus.plugins.audionotes;

public enum NotesSortByMode {
	BY_TYPE,
	BY_DATE;

	public boolean isByType() {
		return this == BY_TYPE;
	}

	public boolean isByDate() {
		return this == BY_DATE;
	}
}
