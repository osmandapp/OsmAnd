package net.osmand.plus.views.layers.geometry;

public abstract class PublicTransportGeometryWayStyle extends GeometryWayStyle<PublicTransportGeometryWayContext> {

	public PublicTransportGeometryWayStyle(PublicTransportGeometryWayContext context) {
		super(context);
	}

	public PublicTransportGeometryWayStyle(PublicTransportGeometryWayContext context, Integer color) {
		super(context, color);
	}
}
