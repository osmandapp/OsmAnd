package net.osmand.test.ui.poi;

import static net.osmand.IndexConstants.SETTINGS_DIR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.osm.MapPoiTypes;
import net.osmand.test.common.AndroidTest;
import net.osmand.util.Algorithms;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

@RunWith(AndroidJUnit4.class)
public class KPoiTypesTest extends AndroidTest {

	private static final String CATEGORY = "custom_test_category";
	private static final String CATEGORY_XML = "<poi_category name=\"" + CATEGORY + "\">"
			+ "<poi_type name=\"custom_test_type\" tag=\"custom_test\" value=\"yes\"/></poi_category>";

	private File customPoiTypes;

	@Before
	@Override
	public void setup() {
		super.setup();
		customPoiTypes = app.getAppPath(SETTINGS_DIR + "poi_types.xml");
		assumeFalse("a custom poi_types.xml is already in use", customPoiTypes.exists());
	}

	@After
	public void removeCustomPoiTypes() {
		if (customPoiTypes != null && customPoiTypes.delete()) {
			app.reInitPoiTypes();
		}
	}

	@Test
	public void bothRegistriesReadTheCustomPoiTypes() throws IOException {
		String xml;
		try (InputStream is = MapPoiTypes.class.getResourceAsStream("poi_types.xml")) {
			xml = Algorithms.readFromInputStream(is).toString();
		}
		customPoiTypes.getParentFile().mkdirs();
		try (Writer writer = new FileWriter(customPoiTypes)) {
			writer.write(xml.replace("</poi_types>", CATEGORY_XML + "</poi_types>"));
		}

		app.reInitPoiTypes();
		assertEquals(CATEGORY, app.getPoiTypes().getPoiCategoryByName(CATEGORY).getKeyName());
		assertEquals(CATEGORY, app.getKPoiTypes().getPoiCategoryByName(CATEGORY).getKeyName());

		assertTrue(customPoiTypes.delete());
		app.reInitPoiTypes();
		var javaCategory = app.getPoiTypes().getPoiCategoryByName(CATEGORY);
		var kotlinCategory = app.getKPoiTypes().getPoiCategoryByName(CATEGORY);
		assertTrue(javaCategory == null || !CATEGORY.equals(javaCategory.getKeyName()));
		assertTrue(kotlinCategory == null || !CATEGORY.equals(kotlinCategory.getKeyName()));
	}
}
