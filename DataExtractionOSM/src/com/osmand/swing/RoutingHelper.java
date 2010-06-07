package com.osmand.swing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.osmand.ExceptionHandler;
import com.osmand.osm.LatLon;
import com.osmand.osm.Way;

public class RoutingHelper {
	// for vector rendering we should extract from osm
	// 1. Ways (different kinds) with tag highway= ?,highway=stop ...
	// 2. Junction = roundabout
	// 3. barrier, traffic_calming=bump
	// 4. Save {name, ref} of way to unify it

	
	// + for future routing we should extract from osm
	// 1. oneway 
	// 2. max_speed
	// 3. toll
	// 4. traffic_signals
	// 5. max_heigtht, max_width, min_speed, ...
	// 6. incline ?



	public static List<Way> route(LatLon start, LatLon end){
		List<Way> res = new ArrayList<Way>();
		long time = System.currentTimeMillis();
		System.out.println("Route from " + start + " to " + end);
		if (start != null && end != null) {
			try {
				StringBuilder uri = new StringBuilder();
				uri.append("http://www.yournavigation.org/api/1.0/gosmore.php?format=kml");
				uri.append("&flat=").append(start.getLatitude());
				uri.append("&flon=").append(start.getLongitude());
				uri.append("&tlat=").append(end.getLatitude());
				uri.append("&tlon=").append(end.getLongitude());
				uri.append("&v=motorcar").append("&fast=1").append("&layer=mapnik");

				URL url = new URL(uri.toString());
				URLConnection connection = url.openConnection();
				StringBuilder content = new StringBuilder();
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				{
					String s = null;
					boolean fist = true;
					while ((s = reader.readLine()) != null) {
						if (fist) {
							fist = false;
							System.out.println(s);
						}
						content.append(s).append("\n");
					}
					System.out.println(content);
				}
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dom = factory.newDocumentBuilder();
				Document doc = dom.parse(new InputSource(new StringReader(content.toString())));
				NodeList list = doc.getElementsByTagName("coordinates");
				for(int i=0; i<list.getLength(); i++){
					Node item = list.item(i);
					String str = item.getTextContent();
					int st = 0;
					int next = 0;
					Way w = new Way(-1);
					while((next = str.indexOf('\n', st)) != -1){
						String coordinate = str.substring(st, next + 1);
						int s = coordinate.indexOf(',');
						if (s != -1) {
							try {
								double lon = Double.parseDouble(coordinate.substring(0, s));
								double lat = Double.parseDouble(coordinate.substring(s + 1));
								w.addNode(new com.osmand.osm.Node(lat, lon, -1));
							} catch (NumberFormatException e) {
							}
						}
						st = next + 1;
					}
					if(!w.getNodes().isEmpty()){
						res.add(w);
					}
					
				}
			} catch (IOException e) {
				ExceptionHandler.handle(e);
			} catch (ParserConfigurationException e) {
				ExceptionHandler.handle(e);
			} catch (SAXException e) {
				ExceptionHandler.handle(e);
			}
			System.out.println("Finding routes " + res.size() + " " + (System.currentTimeMillis() - time) + " ms");
		}
		return res;
	}
}
