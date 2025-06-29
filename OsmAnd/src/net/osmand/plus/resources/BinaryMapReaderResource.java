package net.osmand.plus.resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResourceType;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class BinaryMapReaderResource {

	private static final Log log = PlatformUtil.getLog(BinaryMapReaderResource.class);

	private BinaryMapIndexReader initialReader;
	private final File file;
	private final List<BinaryMapIndexReader> readers = new ArrayList<>(BinaryMapReaderResourceType.values().length);
	private boolean useForRouting;
	private boolean useForPublicTransport;

	public BinaryMapReaderResource(@NonNull File file, @NonNull BinaryMapIndexReader initialReader) {
		this.file = file;
		this.initialReader = initialReader;
		while (readers.size() < BinaryMapReaderResourceType.values().length) {
			readers.add(null);
		}
	}

	@Nullable
	public BinaryMapIndexReader getReader(BinaryMapReaderResourceType type) {
		BinaryMapIndexReader r = readers.get(type.ordinal());
		BinaryMapIndexReader initialReader = this.initialReader;
		if (r == null && initialReader != null) {
			try {
				RandomAccessFile raf = new RandomAccessFile(file, "r");
				r = new BinaryMapIndexReader(raf, initialReader);
				readers.set(type.ordinal(), r);
			} catch (Exception e) {
				log.error("Fail to initialize " + file.getName(), e);
			}
		}
		return r;
	}

	public String getFileName() {
		return file.getName();
	}

	public long getFileLastModified() {
		return file.lastModified();
	}

	// should not use methods to read from file!
	@Nullable
	public BinaryMapIndexReader getShallowReader() {
		return initialReader;
	}

	public void close() {
		close(initialReader);
		for (BinaryMapIndexReader reader : readers) {
			if (reader != null) {
				close(reader);
			}
		}
		initialReader = null;
	}

	public boolean isClosed() {
		return initialReader == null;
	}

	private void close(@NonNull BinaryMapIndexReader r) {
		try {
			r.close();
		} catch (Exception e) {
			log.error("Fail to close " + file.getName(), e);
		}
	}

	public void setUseForRouting(boolean useForRouting) {
		this.useForRouting = useForRouting;
	}

	public boolean isUseForRouting() {
		return useForRouting;
	}

	public boolean isUseForPublicTransport() {
		return useForPublicTransport;
	}

	public void setUseForPublicTransport(boolean useForPublicTransport) {
		this.useForPublicTransport = useForPublicTransport;
	}
}
