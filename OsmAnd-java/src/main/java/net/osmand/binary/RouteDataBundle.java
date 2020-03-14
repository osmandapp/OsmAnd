package net.osmand.binary;

import net.osmand.router.RouteDataResources;

public class RouteDataBundle extends StringBundle {

	private RouteDataResources resources;

	public RouteDataBundle(RouteDataResources resources) {
		this.resources = resources;
	}

	public RouteDataBundle(RouteDataResources resources, StringBundle bundle) {
		super(bundle.getMap());
		this.resources = resources;
	}

	@Override
	public StringBundle newInstance() {
		return new RouteDataBundle(resources);
	}

	public RouteDataResources getResources() {
		return resources;
	}
}