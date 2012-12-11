package net.osmand.map;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.osmand.map.OsmandRegionInfo.OsmAndRegionInfo;
import net.osmand.map.OsmandRegionInfo.OsmAndRegions;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RegionsRegistryConverter {
	
	public static List<RegionCountry> parseRegions() throws IllegalStateException {
		InputStream is = RegionsRegistryConverter.class.getResourceAsStream("countries.xml");
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			RegionsHandler h = new RegionsHandler(parser);
			parser.parse(new InputSource(is), h);
			return h.getCountries();
		} catch (SAXException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}
	
	
	private static class RegionsHandler extends DefaultHandler {
		
		private SAXParser parser;
		private String continentName;
		private RegionCountry current;
		private RegionCountry currentRegion;
		private List<RegionCountry> countries = new ArrayList<RegionCountry>();
		private StringBuilder txt = new StringBuilder();

		public RegionsHandler(SAXParser parser) {
			this.parser = parser;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			String tagName = parser.isNamespaceAware() ? localName : qName;
			if(tagName.equals("continent")) {
				continentName = attributes.getValue("name");
			} else if(tagName.equals("country")) {
				RegionCountry c = new RegionCountry();
				c.continentName = continentName;
				c.name = attributes.getValue("name");
				current = c;
				countries.add(c);
			} else if(tagName.equals("tiles")) {
				txt.setLength(0);
			} else if(tagName.equals("region")) {
				RegionCountry c = new RegionCountry();
				c.name = attributes.getValue("name");
				currentRegion = c;
				current.addSubregion(c);
			}
		}
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			txt.append(ch, start, length);
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			String tagName = parser.isNamespaceAware() ? localName : qName;
			if(tagName.equals("region")) {
				currentRegion = null;
			} else if(tagName.equals("tiles")) {
				String[] s = txt.toString().split("( |;)");
				RegionCountry a = currentRegion == null ? current : currentRegion;
				for(int i =0; i < s.length; i+=2) {
					a.add(Integer.parseInt(s[i]), Integer.parseInt(s[i+1]));
				}
			}
		}
		
		public List<RegionCountry> getCountries() {
			return countries;
		}
	}
	
	
	
	public static void main(String[] args) throws IOException {
		List<RegionCountry> countries = parseRegions();
		OsmAndRegions.Builder regions= OsmAndRegions.newBuilder();
		for(RegionCountry c : countries){
			regions.addRegions(c.convert());
		}
		
		String filePath = "../OsmAnd-java/src/net/osmand/map/"+RegionRegistry.fileName;
		long t = -System.currentTimeMillis();
		FileOutputStream out = new FileOutputStream(filePath);
		OsmAndRegionInfo.newBuilder().setRegionInfo(regions)
		.build().writeTo(out);
		out.close();
		InputStream in = RegionRegistry.class.getResourceAsStream(RegionRegistry.fileName);
		OsmAndRegionInfo regInfo = OsmAndRegionInfo.newBuilder().mergeFrom(in).build();
		t += System.currentTimeMillis();
		for(int j = 0; j < regInfo.getRegionInfo().getRegionsCount(); j++) {
			RegionCountry.construct(regInfo.getRegionInfo().getRegions(j));
		}
		System.out.println("Read countries " + regInfo.getRegionInfo().getRegionsCount() +  " " + countries.size() );
		System.out.println("Timing " + t);
		
		

	}
	
}
