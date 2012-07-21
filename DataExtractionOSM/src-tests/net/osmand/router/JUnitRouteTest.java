package net.osmand.router;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import net.osmand.Algoritms;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.RoutingConfiguration;
import net.osmand.swing.DataExtractionSettings;
import net.osmand.swing.NativeSwingRendering;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;


public class JUnitRouteTest  {

	static BinaryMapIndexReader[]  rs;
	private NativeSwingRendering lib;
	@Before
	public void setupFiles() throws IOException {
		if(rs != null){
			return;
		}
		lib = NativeSwingRendering.getDefaultFromSettings();
		BinaryRoutePlanner.PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = false;
		String obfdir = System.getenv("OBF_DIR");
		if(Algoritms.isEmpty(obfdir)){
			obfdir = DataExtractionSettings.getSettings().getBinaryFilesDir();
		}
		
		List<File> files = new ArrayList<File>();  
		for (File f : new File(obfdir).listFiles()) {
			if (f.getName().endsWith(".obf")) {
				files.add(f);
			}
		}
		rs = new BinaryMapIndexReader[files.size()];
		int it = 0;
		for (File f : files) {
			RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$ //$NON-NLS-2$
			rs[it++] = new BinaryMapIndexReader(raf, false);
		}
	}
	
	@Test
	public void runCZ() throws SAXException, IOException, ParserConfigurationException {
		RouterTestsSuite.test(lib, getClass().getResourceAsStream("cz.test.xml"), rs, RoutingConfiguration.getDefault());
	}
	
	@Test
	public void runNL() throws SAXException, IOException, ParserConfigurationException {
		RouterTestsSuite.test(lib, getClass().getResourceAsStream("nl.test.xml"), rs, RoutingConfiguration.getDefault());
	}

	@Test
	public void runNL2() throws SAXException, IOException, ParserConfigurationException {
		RouterTestsSuite.test(lib, getClass().getResourceAsStream("nl2.test.xml"), rs, RoutingConfiguration.getDefault());
	}
	
	@Test
	public void runNLLeid() throws SAXException, IOException, ParserConfigurationException {
		RouterTestsSuite.test(lib, getClass().getResourceAsStream("nl_leid.test.xml"), rs, RoutingConfiguration.getDefault());
	}
	
	@Test
	public void runNLLong() throws SAXException, IOException, ParserConfigurationException {
		RouterTestsSuite.test(lib, getClass().getResourceAsStream("nl_long.test.xml"), rs, RoutingConfiguration.getDefault());
	}
	
	
	
	@Test
	public void runBLR() throws SAXException, IOException, ParserConfigurationException {
		RouterTestsSuite.test(lib, getClass().getResourceAsStream("blr.test.xml"), rs, RoutingConfiguration.getDefault());
	}
	
}
