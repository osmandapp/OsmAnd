package net.osmand.plus.settings.datastorage.item;

public class DirectoryItem {

	private final String absolutePath;
	private final boolean goDeeper;
	private final CheckingType checkingType;
	private final boolean skipOther;

	public enum CheckingType {
		EXTENSIONS,
		PREFIX
	}

	public DirectoryItem(String absolutePath,
	                     boolean goDeeper,
	                     CheckingType checkingType,
	                     boolean skipOther) {
		this.absolutePath = absolutePath;
		this.goDeeper = goDeeper;
		this.checkingType = checkingType;
		this.skipOther = skipOther;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public boolean isGoDeeper() {
		return goDeeper;
	}

	public CheckingType getCheckingType() {
		return checkingType;
	}

	public boolean isSkipOther() {
		return skipOther;
	}
}
