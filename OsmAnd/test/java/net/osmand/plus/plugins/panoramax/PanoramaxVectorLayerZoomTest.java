package net.osmand.plus.plugins.panoramax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PanoramaxVectorLayerZoomTest {

	@Test
	public void sequencesStartAtZoom15() {
		TileSourceTemplate source = TileSourceManager.getPanoramaxVectorSource();
		assertEquals(15, source.getMinimumZoomSupported());
		assertFalse(PanoramaxVectorLayer.isVisibleAtZoom(13, source));
		assertFalse(PanoramaxVectorLayer.isVisibleAtZoom(14, source));
		assertTrue(PanoramaxVectorLayer.isVisibleAtZoom(15, source));
		assertTrue(PanoramaxVectorLayer.isVisibleAtZoom(16, source));
	}

	@Test
	public void pointsAndOverzoomStayVisibleUpToTheSourceMaximum() {
		TileSourceTemplate source = TileSourceManager.getPanoramaxVectorSource();
		assertTrue(PanoramaxVectorLayer.isVisibleAtZoom(PanoramaxVectorLayer.MIN_POINTS_ZOOM, source));
		assertTrue(PanoramaxVectorLayer.isVisibleAtZoom(source.getMaximumZoomSupported(), source));
		assertFalse(PanoramaxVectorLayer.isVisibleAtZoom(source.getMaximumZoomSupported() + 1, source));
	}

	@Test
	public void aWiderSourceDoesNotLowerTheServiceMinimum() {
		// the raster cache source starts at 13, but the vector service still starts at 15
		TileSourceTemplate wider = TileSourceManager.getPanoramaxCacheSource();
		assertEquals(13, wider.getMinimumZoomSupported());
		assertFalse(PanoramaxVectorLayer.isVisibleAtZoom(14, wider));
		assertTrue(PanoramaxVectorLayer.isVisibleAtZoom(15, wider));
	}
}
