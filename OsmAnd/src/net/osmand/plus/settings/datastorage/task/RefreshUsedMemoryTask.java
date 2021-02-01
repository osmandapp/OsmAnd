package net.osmand.plus.settings.datastorage.task;

import android.os.AsyncTask;

import net.osmand.plus.settings.datastorage.DataStorageHelper.UpdateMemoryInfoUIAdapter;
import net.osmand.plus.settings.datastorage.item.DirectoryItem;
import net.osmand.plus.settings.datastorage.item.DirectoryItem.CheckingType;
import net.osmand.plus.settings.datastorage.item.MemoryItem;

import java.io.File;

import static net.osmand.plus.settings.datastorage.DataStorageFragment.UI_REFRESH_TIME_MS;

public class RefreshUsedMemoryTask extends AsyncTask<MemoryItem, Void, Void> {
	private UpdateMemoryInfoUIAdapter listener;
	private File rootDir;
	private MemoryItem otherMemory;
	private String[] directoriesToAvoid;
	private String[] prefixesToAvoid;
	private String taskKey;
	private long lastRefreshTime;

	public RefreshUsedMemoryTask(UpdateMemoryInfoUIAdapter listener, MemoryItem otherMemory, File rootDir, String[] directoriesToAvoid, String[] prefixesToAvoid, String taskKey) {
		this.listener = listener;
		this.otherMemory = otherMemory;
		this.rootDir = rootDir;
		this.directoriesToAvoid = directoriesToAvoid;
		this.prefixesToAvoid = prefixesToAvoid;
		this.taskKey = taskKey;
	}

	@Override
	protected Void doInBackground(MemoryItem... items) {
		lastRefreshTime = System.currentTimeMillis();
		if (rootDir.canRead()) {
			calculateMultiTypes(rootDir, items);
		}
		return null;
	}

	private void calculateMultiTypes(File rootDir, MemoryItem... items) {
		File[] subFiles = rootDir.listFiles();

		for (File file : subFiles) {
			if (isCancelled()) {
				break;
			}
			nextFile : {
				if (file.isDirectory()) {
					//check current directory should be avoid
					if (directoriesToAvoid != null) {
						for (String directoryToAvoid : directoriesToAvoid) {
							if (file.getAbsolutePath().equals(directoryToAvoid)) {
								break nextFile;
							}
						}
					}
					//check current directory matched items type
					for (MemoryItem item : items) {
						DirectoryItem[] directories = item.getDirectories();
						if (directories == null) {
							continue;
						}
						for (DirectoryItem dir : directories) {
							if (file.getAbsolutePath().equals(dir.getAbsolutePath())
									|| (file.getAbsolutePath().startsWith(dir.getAbsolutePath()))) {
								if (dir.isGoDeeper()) {
									calculateMultiTypes(file, items);
									break nextFile;
								} else if (dir.isSkipOther()) {
									break nextFile;
								}
							}
						}
					}
					//current directory did not match to any type
					otherMemory.addBytes(getDirectorySize(file));
				} else if (file.isFile()) {
					//check current file should be avoid
					if (prefixesToAvoid != null) {
						for (String prefixToAvoid : prefixesToAvoid) {
							if (file.getName().toLowerCase().startsWith(prefixToAvoid.toLowerCase())) {
								break nextFile;
							}
						}
					}
					//check current file matched items type
					for (MemoryItem item : items) {
						DirectoryItem[] directories = item.getDirectories();
						if (directories == null) {
							continue;
						}
						for (DirectoryItem dir : directories) {
							if (rootDir.getAbsolutePath().equals(dir.getAbsolutePath())
									|| (rootDir.getAbsolutePath().startsWith(dir.getAbsolutePath()) && dir.isGoDeeper())) {
								CheckingType checkingType = dir.getCheckingType();
								switch (checkingType) {
									case EXTENSIONS : {
										String[] extensions = item.getExtensions();
										if (extensions != null) {
											for (String extension : extensions) {
												if (file.getAbsolutePath().endsWith(extension)) {
													item.addBytes(file.length());
													break nextFile;
												}
											}
										} else {
											item.addBytes(file.length());
											break nextFile;
										}
										break ;
									}
									case PREFIX : {
										String[] prefixes = item.getPrefixes();
										if (prefixes != null) {
											for (String prefix : prefixes) {
												if (file.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
													item.addBytes(file.length());
													break nextFile;
												}
											}
										} else {
											item.addBytes(file.length());
											break nextFile;
										}
										break ;
									}
								}
								if (dir.isSkipOther()) {
									break nextFile;
								}
							}
						}
					}
					//current file did not match any type
					otherMemory.addBytes(file.length());
				}
			}
			refreshUI();
		}
	}

	private long getDirectorySize(File dir) {
		long bytes = 0;
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (isCancelled()) {
					break;
				}
				if (file.isDirectory()) {
					bytes += getDirectorySize(file);
				} else if (file.isFile()) {
					bytes += file.length();
				}
			}
		}
		return bytes;
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
		if (listener != null) {
			listener.onMemoryInfoUpdate();
		}
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		super.onPostExecute(aVoid);
		if (listener != null) {
			listener.onFinishUpdating(taskKey);
		}
	}

	private void refreshUI() {
		long currentTime = System.currentTimeMillis();
		if ((currentTime - lastRefreshTime) > UI_REFRESH_TIME_MS) {
			lastRefreshTime = currentTime;
			publishProgress();
		}
	}
}
