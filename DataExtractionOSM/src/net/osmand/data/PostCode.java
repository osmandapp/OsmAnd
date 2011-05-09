package net.osmand.data;

import java.text.Collator;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class PostCode extends MapObject {
	private Map<String, Street> streets = new TreeMap<String, Street>(Collator.getInstance());
	
	public PostCode(String name){
		setName(name);
		setEnName(name);
		setId(-1L);
	}

	public boolean isEmptyWithStreets(){
		return streets.isEmpty();
	}
	
	public Street getStreet(String name){
		return streets.get(name);
	}
	
	public Collection<Street> getStreets() {
		return streets.values();
	}
	
	public void removeAllStreets()
	{
		streets.clear();
	}
	
	public Street registerStreet(Street street, boolean useEnglishNames){
		String name = street.getName(useEnglishNames);
		streets.put(name, street);
		return street;
	}

}
