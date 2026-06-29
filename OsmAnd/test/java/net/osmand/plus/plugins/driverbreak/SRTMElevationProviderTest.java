package net.osmand.plus.plugins.driverbreak;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Unit tests for {@link SRTMElevationProvider} tile naming and HGT reading.
 */
@RunWith(AndroidJUnit4.class)
public class SRTMElevationProviderTest {

	@Test
	public void hgtFilenamePositiveLatLon() {
		Assert.assertEquals("N61E009.hgt", SRTMElevationProvider.hgtFilename(61.5, 9.7));
	}

	@Test
	public void hgtFilenameNegativeLon() {
		Assert.assertEquals("N61W011.hgt", SRTMElevationProvider.hgtFilename(61.5, -10.3));
	}

	@Test
	public void hgtFilenameNegativeLat() {
		Assert.assertEquals("S34E018.hgt", SRTMElevationProvider.hgtFilename(-33.2, 18.4));
	}

	@Test
	public void hgtByteOffsetForReferencePoint() {
		long offset = SRTMElevationProvider.hgtByteOffset(61.500, 9.700);
		Assert.assertEquals((1800L * 3601L + 2520L) * 2L, offset);
	}

	@Test
	public void voidSentinelInSyntheticHgt() throws IOException {
		File temp = File.createTempFile("void", ".hgt");
		temp.deleteOnExit();
		long offset = SRTMElevationProvider.hgtByteOffset(61.500, 9.700);
		try (RandomAccessFile raf = new RandomAccessFile(temp, "rw")) {
			raf.setLength(offset + 2);
			raf.seek(offset);
			raf.writeByte(0x80);
			raf.writeByte(0x00);
		}
		int elevation = SRTMElevationProvider.readHgtElevation(temp, 9, 61, 61.5, 9.7);
		Assert.assertEquals(SRTMElevationProvider.VOID_ELEVATION, elevation);
	}

	@Test
	public void tileBoundaryUsesFloorIndex() {
		Assert.assertEquals("N62E009.hgt", SRTMElevationProvider.hgtFilename(62.0, 9.5));
	}
}
