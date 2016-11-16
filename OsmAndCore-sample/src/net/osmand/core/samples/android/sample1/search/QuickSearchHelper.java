package net.osmand.core.samples.android.sample1.search;

import net.osmand.IndexConstants;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuickSearchHelper {
	private SampleApplication app;
	private SearchUICore core;
	private SearchResultCollection resultCollection;

	public QuickSearchHelper(SampleApplication app) {
		this.app = app;
		core = new SearchUICore(app.getPoiTypes(), SampleApplication.LANGUAGE, SampleApplication.TRANSLITERATE);
	}

	public SearchUICore getCore() {
		return core;
	}

	public SearchResultCollection getResultCollection() {
		return resultCollection;
	}

	public void setResultCollection(SearchResultCollection resultCollection) {
		this.resultCollection = resultCollection;
	}

	public void initSearchUICore() {
		setRepositoriesForSearchUICore(app);
		core.setPoiTypes(app.getPoiTypes());
		core.init();
	}

	public void setRepositoriesForSearchUICore(final SampleApplication app) {
		ArrayList<File> files = new ArrayList<File>();
		File appPath = app.getAppPath(null);
		collectFiles(appPath, IndexConstants.BINARY_MAP_INDEX_EXT, files);
		collectFiles(app.getAppPath(IndexConstants.WIKI_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);

		List<BinaryMapIndexReader> readers = new ArrayList<>();
		for (File f : files) {
			try {
				RandomAccessFile mf = new RandomAccessFile(f.getPath(), "r");
				BinaryMapIndexReader reader = new BinaryMapIndexReader(mf, f);
				readers.add(reader);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		core.getSearchSettings().setOfflineIndexes(readers);
	}

	private List<File> collectFiles(File dir, String ext, List<File> files) {
		if (dir.exists() && dir.canRead()) {
			File[] lf = dir.listFiles();
			if (lf == null || lf.length == 0) {
				return files;
			}
			for (File f : lf) {
				if (f.getName().endsWith(ext)) {
					files.add(f);
				}
			}
		}
		return files;
	}
}
