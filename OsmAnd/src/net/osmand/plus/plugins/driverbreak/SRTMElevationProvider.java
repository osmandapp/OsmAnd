/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import mil.nga.tiff.FieldType;
import mil.nga.tiff.FileDirectory;
import mil.nga.tiff.ImageWindow;
import mil.nga.tiff.Rasters;
import mil.nga.tiff.TIFFImage;
import mil.nga.tiff.TiffReader;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;

/**
 * Reads SRTM HGT and optional Copernicus GeoTIFF elevation tiles.
 * Port of driver_break_srtm.c elevation lookup.
 */
public class SRTMElevationProvider {

	private static final Log LOG = PlatformUtil.getLog(SRTMElevationProvider.class);

	/** SRTM void sentinel; driver_break_srtm.c uses -32768. */
	public static final int VOID_ELEVATION = Integer.MIN_VALUE;

	/** HGT tile dimension (3601 x 3601 pixels). */
	public static final int HGT_TILE_PIXELS = 3601;

	/** Arc-seconds per degree for 1 arc-second SRTM. */
	private static final int ARC_SECONDS_PER_DEGREE = 3600;

	private final File tileDirectory;
	private final Object ioLock = new Object();

	public SRTMElevationProvider(@NonNull OsmandApplication app) {
		File dir = app.getAppPath("driver_break/srtm_tiles");
		if (!dir.exists() && !dir.mkdirs()) {
			LOG.warn("DriverBreak: could not create SRTM tile directory");
		}
		this.tileDirectory = dir;
	}

	/**
	 * @param lat latitude in degrees
	 * @param lon longitude in degrees
	 * @return elevation in metres, or {@link #VOID_ELEVATION} when unknown
	 */
	public int getElevation(double lat, double lon) {
		int lonIdx = (int) Math.floor(lon);
		int latIdx = (int) Math.floor(lat);
		synchronized (ioLock) {
			File geotiff = new File(tileDirectory, geotiffFilename(lonIdx, latIdx));
			if (geotiff.isFile()) {
				int fromTiff = readGeotiffElevation(geotiff, lonIdx, latIdx, lat, lon);
				if (fromTiff != VOID_ELEVATION) {
					return fromTiff;
				}
			}
			File hgt = new File(tileDirectory, hgtFilename(lat, lon));
			if (!hgt.isFile()) {
				return VOID_ELEVATION;
			}
			return readHgtElevation(hgt, lonIdx, latIdx, lat, lon);
		}
	}

	@NonNull
	public File getTileDirectory() {
		return tileDirectory;
	}

	/**
	 * Returns true when an HGT or Copernicus GeoTIFF tile is cached locally.
	 */
	public boolean hasTile(int lonIdx, int latIdx) {
		synchronized (ioLock) {
			File geotiff = new File(tileDirectory, geotiffFilename(lonIdx, latIdx));
			if (geotiff.isFile()) {
				return true;
			}
			File hgt = new File(tileDirectory, hgtFilename(latIdx + 0.5, lonIdx + 0.5));
			return hgt.isFile();
		}
	}

	/** Counts cached HGT and GeoTIFF tiles in the local directory. */
	public int countCachedTiles() {
		synchronized (ioLock) {
			File[] files = tileDirectory.listFiles();
			if (files == null) {
				return 0;
			}
			int count = 0;
			for (File file : files) {
				String name = file.getName().toLowerCase(Locale.US);
				if (file.isFile() && (name.endsWith(".hgt") || name.endsWith(".tif"))) {
					count++;
				}
			}
			return count;
		}
	}

	/**
	 * Build HGT filename for a coordinate, e.g. N61E009.hgt.
	 * Port of srtm_get_tile_filename in driver_break_srtm.c.
	 */
	@NonNull
	public static String hgtFilename(double lat, double lon) {
		int lonIdx = (int) Math.floor(lon);
		int latIdx = (int) Math.floor(lat);
		char latDir = latIdx >= 0 ? 'N' : 'S';
		char lonDir = lonIdx >= 0 ? 'E' : 'W';
		return String.format(Locale.US, "%c%02d%c%03d.hgt",
				latDir, Math.abs(latIdx), lonDir, Math.abs(lonIdx));
	}

	/**
	 * Compute byte offset for an HGT sample. Used by unit tests and reader.
	 */
	public static long hgtByteOffset(double lat, double lon) {
		int lonIdx = (int) Math.floor(lon);
		int latIdx = (int) Math.floor(lat);
		int row = (int) Math.round((latIdx + 1 - lat) * ARC_SECONDS_PER_DEGREE);
		int col = (int) Math.round((lon - lonIdx) * ARC_SECONDS_PER_DEGREE);
		return ((long) row * HGT_TILE_PIXELS + col) * 2L;
	}

	@NonNull
	static String geotiffFilename(int lonIdx, int latIdx) {
		char latDir = latIdx >= 0 ? 'N' : 'S';
		char lonDir = lonIdx >= 0 ? 'E' : 'W';
		return String.format(Locale.US, "Copernicus_DSM_COG_10_%c%02d_00_%c%03d_00_DEM.tif",
				latDir, Math.abs(latIdx), lonDir, Math.abs(lonIdx));
	}

	static int readHgtElevation(@NonNull File filepath, int lonIdx, int latIdx, double lat, double lon) {
		long offset = hgtByteOffset(lat, lon);
		try (RandomAccessFile raf = new RandomAccessFile(filepath, "r")) {
			if (offset < 0 || offset + 1 >= raf.length()) {
				return VOID_ELEVATION;
			}
			raf.seek(offset);
			int hi = raf.readUnsignedByte();
			int lo = raf.readUnsignedByte();
			short value = (short) ((hi << 8) | lo);
			if (value == -32768) {
				return VOID_ELEVATION;
			}
			return value;
		} catch (IOException e) {
			LOG.error("DriverBreak: HGT read failed for " + filepath.getName(), e);
			return VOID_ELEVATION;
		}
	}

	private static int readGeotiffElevation(@NonNull File filepath, int lonIdx, int latIdx,
			double lat, double lon) {
		try {
			TIFFImage image = TiffReader.readTiff(filepath);
			FileDirectory directory = image.getFileDirectory();
			if (directory == null) {
				return VOID_ELEVATION;
			}
			Number widthNum = directory.getImageWidth();
			Number heightNum = directory.getImageHeight();
			if (widthNum == null || heightNum == null) {
				return VOID_ELEVATION;
			}
			int width = widthNum.intValue();
			int height = heightNum.intValue();
			if (width <= 0 || height <= 0) {
				return VOID_ELEVATION;
			}
			double tileMinLon = lonIdx;
			double tileMaxLat = latIdx + 1.0;
			int x = (int) ((lon - tileMinLon) * width);
			int y = (int) ((tileMaxLat - lat) * height);
			if (x >= width) {
				x = width - 1;
			}
			if (y >= height) {
				y = height - 1;
			}
			if (x < 0 || y < 0) {
				return VOID_ELEVATION;
			}
			ImageWindow window = new ImageWindow(x, y);
			Rasters rasters = directory.readRasters(window);
			if (rasters == null) {
				return VOID_ELEVATION;
			}
			Number sample = rasters.getFirstPixelSample(0, 0);
			return sampleToElevationM(sample, directory);
		} catch (IOException e) {
			LOG.error("DriverBreak: GeoTIFF read failed for " + filepath.getName(), e);
			return VOID_ELEVATION;
		} catch (RuntimeException e) {
			LOG.error("DriverBreak: GeoTIFF parse failed for " + filepath.getName(), e);
			return VOID_ELEVATION;
		}
	}

	private static int sampleToElevationM(@Nullable Number sample, @NonNull FileDirectory directory) {
		if (sample == null) {
			return VOID_ELEVATION;
		}
		FieldType fieldType = directory.getFieldTypeForSample(0);
		if (fieldType == FieldType.FLOAT) {
			float value = sample.floatValue();
			if (value < -500.0f || value > 9000.0f) {
				return VOID_ELEVATION;
			}
			return Math.round(value);
		}
		int value = sample.intValue();
		if (value == -32768) {
			return VOID_ELEVATION;
		}
		return value;
	}
}
