package com.osmand.data;

public class PostcodeBasedStreet extends Street {

	public PostcodeBasedStreet(City city, String postcode) {
		super(city, postcode);
	}
	
	@Override
	public Long getId() {
		return -1l;
	}

}
