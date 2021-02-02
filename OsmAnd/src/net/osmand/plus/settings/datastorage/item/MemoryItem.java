package net.osmand.plus.settings.datastorage.item;

public class MemoryItem {

	private String key;
	private final String[] extensions;
	private final String[] prefixes;
	private final DirectoryItem[] directories;
	private long usedMemoryBytes;

	private MemoryItem(String key,
	                   String[] extensions,
	                   String[] prefixes,
	                   long usedMemoryBytes,
	                   DirectoryItem[] directories) {
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

	public DirectoryItem[] getDirectories() {
		return directories;
	}

	public void addBytes(long bytes) {
		this.usedMemoryBytes += bytes;
	}

	public static class DataStorageMemoryItemBuilder {
		private String key;
		private String[] extensions;
		private String[] prefixes;
		private DirectoryItem[] directories;
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
		
		public DataStorageMemoryItemBuilder setDirectories(DirectoryItem... directories) {
			this.directories = directories;
			return this;
		}
		
		public DataStorageMemoryItemBuilder setUsedMemoryBytes(long usedMemoryBytes) {
			this.usedMemoryBytes = usedMemoryBytes;
			return this;
		}
		
		public MemoryItem createItem() {
			return new MemoryItem(key, extensions, prefixes, usedMemoryBytes, directories);
		}
	}
}
