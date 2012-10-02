package net.osmand.translator.test;

import java.io.IOException;

import com.google.devtools.j2cpp.CppGenerationTest;

public class TranslatorTest extends CppGenerationTest {
	
	
//	public void testSimple() throws IOException{
//		cppTranslateSourceFile(getResourceAstring("MapUtils_1.java"), "MapUtils_1");
//		printHeaderAndSource("MapUtils_1");
//	}
	
	public void addDependency(String pack, String fileName) throws IOException {
		String s = getResourceAstring(pack+"/"+fileName);
		 s = s.replace("package "+pack.replace('_', '.')+";","");
		addSourceFile(s, fileName);
	}
	
	public void testMapUtils() throws IOException{
		addDependency("net_osmand_osm", "Way.java");
		addDependency("net_osmand_osm", "LatLon.java");
		addDependency("net_osmand_osm", "Way.java");
		addDependency("net_osmand_osm", "Node.java");
		addDependency("net_osmand_osm", "Entity.java");
		addDependency("net_osmand_osm", "Relation.java");
		cppTranslateSourceFile(getResourceAstring("MapUtils_2.java"), "MapUtils");
		printHeaderAndSource("MapUtils");
	}

	

}
