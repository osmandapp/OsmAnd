package com.osmand.osm.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MinskTransReader {
	// Routes RouteNum; Authority; City; Transport; Operator; ValidityPeriods; SpecialDates;RouteTag;RouteType;Commercial;RouteName;Weekdays;RouteID;Entry;RouteStops;Datestart
	public static class TransportRoute {
		public String routeNum; // 0
		public String transport; // 3
		public String routeType; // 8
		public String routeName; // 10
		public String routeId; // 12
		public List<String> routeStops = new ArrayList<String>(); // 14
	}
	
	// ID;City;Area;Street;Name;Lng;Lat;Stops
	public static class TransportStop {
		public String stopId; // 0
		public double longitude; // 5
		public double latitude; // 6
		public String name ; //4
	}
	
	public static void main(String[] args) throws IOException {
		FileInputStream fis = new FileInputStream(new File("E:/routes.txt"));
		BufferedReader reader = new BufferedReader(new InputStreamReader(fis, Charset.forName("cp1251")));
		List<TransportRoute> routes = readRoutes(reader);
		
		fis = new FileInputStream(new File("E:/stops.txt"));
		reader = new BufferedReader(new InputStreamReader(fis, Charset.forName("cp1251")));
		List<TransportStop> stops = readStopes(reader);
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("E:/routes_out.txt"), Charset.forName("cp1251"));
		
		for(TransportStop r : stops){
			writer.write(r.stopId + " lat : " + r.latitude + " lon : " + r.longitude +" " + r.name + "\n");
		}
		for(TransportRoute r : routes){
			writer.write(r.routeId +"  " + r.routeNum +" " + r.routeType + " " + r.routeName+"\n");
		}
	}

	protected static List<TransportRoute> readRoutes(BufferedReader reader) throws IOException {
		String st = null;
		int line = 0;
		TransportRoute previous = null;
		List<TransportRoute> routes = new ArrayList<TransportRoute>(); 
		while((st = reader.readLine()) != null){
			if(line++ == 0){
				continue;
			}
			
			TransportRoute current = new TransportRoute();
			int stI=0;
			int endI = 0;
			int i=0;
			while ((endI = st.indexOf(';', stI)) != -1) {
				
				String newS = st.substring(stI, endI);
				if(i==0){
					if(newS.length() > 0){
						current.routeNum = newS;
					} else if(previous != null){
						current.routeNum = previous.routeNum;
					}
				} else if(i==3){
					if(newS.length() > 0){
						current.transport = newS;
					} else if(previous != null){
						current.transport = previous.transport;
					}
				} else if(i==8){
					if(newS.length() > 0){
						current.routeType = newS;
					} else if(previous != null){
						current.routeType  = previous.routeType ;
					}
				} else if(i==10){
					if(newS.length() > 0){
						current.routeName = newS;
					} else if(previous != null){
						current.routeName  = previous.routeName ;
					}		
				} else if(i==12){
					current.routeId = newS;
				} else if(i==14){
					String[] strings = newS.split(",");
					for(String s : strings){
						s = s.trim();
						if(s.length() > 0){
							current.routeStops.add(s);
						}
					}
				}
				stI = endI + 1;
				i++;
			}
			previous = current;
			routes.add(current);
		}
		return routes;
	}
	
	protected static List<TransportStop> readStopes(BufferedReader reader) throws IOException {
		String st = null;
		int line = 0;
		List<TransportStop> stopes = new ArrayList<TransportStop>(); 
		while((st = reader.readLine()) != null){
			if(line++ == 0){
				continue;
			}
			
			TransportStop current = new TransportStop();
			int stI=0;
			int endI = 0;
			int i=0;
			while ((endI = st.indexOf(';', stI)) != -1) {
				
				String newS = st.substring(stI, endI);
				if(i==0){
					current.stopId = newS.trim();
				} else if(i==4){
					current.name = newS;
				} else if(i==5){
					current.latitude = Double.parseDouble(newS)/1e5;
				} else if(i==6){
					current.longitude =  Double.parseDouble(newS)/1e5;
				}
				stI = endI + 1;
				i++;
			}
			stopes.add(current);
		}
		return stopes;
	}
	
}