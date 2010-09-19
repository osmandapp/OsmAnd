package net.osmand.data;

import java.text.Collator;
import java.util.Comparator;

public class CityComparator implements Comparator<City>{
	private final boolean en;
	Collator collator = Collator.getInstance();
	public CityComparator(boolean en){
		this.en = en;
	}
	
	
	
	@Override
	public int compare(City o1, City o2) {
		if(en){
			return collator.compare(o1.getEnName(), o2.getEnName());
		} else {
			return collator.compare(o1.getName(), o2.getName());
		}
	}
}