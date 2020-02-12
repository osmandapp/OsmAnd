package net.osmand.plus.settings;

public class DataStorageMemoryItem {
	public final static int EXTENSIONS = 0;
	public final static int PREFIX = 1;
	
	private String key;
	private String[] extensions;
	private String[] prefixes;
	private Directory[] directories;
	private long usedMemoryBytes;

	private DataStorageMemoryItem(String key, String[] extensions, String[] prefixes, long usedMemoryBytes, Directory[] directories) {
		this.key = key;
		this.extensions = extensions;
		this.prefixes = prefixes;
		this.usedMemoryBytes = usedMemoryBytes;
		this.directories = directories;
	}

	public String getKey() {
		return key;
	}

	public long getUsedMemoryBytes() {
		return usedMemoryBytes;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public static DataStorageMemoryItemBuilder builder() {
		return new DataStorageMemoryItemBuilder();
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String[] getPrefixes() {
		return prefixes;
	}

	public Directory[] getDirectories() {
		return directories;
	}

	public void addBytes(long bytes) {
		this.usedMemoryBytes += bytes;
	}

	public static class DataStorageMemoryItemBuilder {
		private String key;
		private String[] extensions;
		private String[] prefixes;
		private Directory[] directories;
		private long usedMemoryBytes;

		public DataStorageMemoryItemBuilder setKey(String key) {
			this.key = key;
			return this;
		}

		public DataStorageMemoryItemBuilder setExtensions(String ... extensions) {
			this.extensions = extensions;
			return this;
		}

		public DataStorageMemoryItemBuilder setPrefixes(String ... prefixes) {
			this.prefixes = prefixes;
			return this;
		}
		
		public DataStorageMemoryItemBuilder setDirectories(Directory ... directories) {
			this.directories = directories;
			return this;
		}
		
		public DataStorageMemoryItemBuilder setUsedMemoryBytes(long usedMemoryBytes) {
			this.usedMemoryBytes = usedMemoryBytes;
			return this;
		}
		
		public DataStorageMemoryItem createItem() {
			return new DataStorageMemoryItem(key, extensions, prefixes, usedMemoryBytes, directories);
		}
	}
	
	public static class Directory {
		private String absolutePath;
		private boolean goDeeper;
		private int checkingType;
		private boolean skipOther;

		public Directory(String absolutePath, boolean goDeeper, int checkingType, boolean skipOther) {
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

		public int getCheckingType() {
			return checkingType;
		}

		public boolean isSkipOther() {
			return skipOther;
		}
	}
}
