package net.osmand.plus.settings.datastorage.item;

public class DirectoryItem {

	private final String absolutePath;
	private final boolean processInternalDirectories;
	private final CheckingType checkingType;
	private final boolean skipUnmatchedInDirectory;

	public enum CheckingType {
		EXTENSIONS,
		PREFIX
	}

	public DirectoryItem(String absolutePath,
	                     boolean processInternalDirectories,
	                     CheckingType checkingType,
	                     boolean skipUnmatchedInDirectory) {
		this.absolutePath = absolutePath;
		this.processInternalDirectories = processInternalDirectories;
		this.checkingType = checkingType;
		this.skipUnmatchedInDirectory = skipUnmatchedInDirectory;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public boolean shouldProcessInternalDirectories() {
		return processInternalDirectories;
	}

	public CheckingType getCheckingType() {
		return checkingType;
	}

	public boolean shouldSkipUnmatchedInDirectory() {
		return skipUnmatchedInDirectory;
	}
}
