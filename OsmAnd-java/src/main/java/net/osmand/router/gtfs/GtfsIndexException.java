package net.osmand.router.gtfs;

import java.io.IOException;

/** The index file is missing parts, was written by another format version, or fails its checksum: re-import the feed. */
public class GtfsIndexException extends IOException {

	public GtfsIndexException(String message) {
		super(message);
	}
}
