package net.osmand.router;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

@RunWith(Parameterized.class)
public class RouteTestingTest {
	private TestEntry te;


	public RouteTestingTest(String name, TestEntry te) {
		this.te = te;
	}

	@BeforeClass
	public static void setUp() throws Exception {
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Iterable<Object[]> data() throws IOException {
		String fileName = "/test_routing.json";
		Reader reader = new InputStreamReader(RouteTestingTest.class.getResourceAsStream(fileName));
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		TestEntry[] testEntries = gson.fromJson(reader, TestEntry[].class);
		ArrayList<Object[]> arrayList = new ArrayList<>();
		for (TestEntry te : testEntries) {
			if (te.isIgnore()) {
				continue;
			}
			arrayList.add(new Object[]{te.getTestName(), te});
		}
		reader.close();
		return arrayList;

	}

	@Test
	public void testRouting() throws Exception {

	}
}
