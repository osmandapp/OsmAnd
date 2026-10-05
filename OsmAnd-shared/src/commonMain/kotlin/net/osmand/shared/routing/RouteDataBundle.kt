package net.osmand.shared.routing

import net.osmand.shared.util.StringBundle

/** A [StringBundle] that carries the route wide state a segment needs while it is read or written. */
class RouteDataBundle : StringBundle {

	private val resources: RouteDataResources

	constructor(resources: RouteDataResources) : super() {
		this.resources = resources
	}

	constructor(resources: RouteDataResources, bundle: StringBundle) : super(bundle) {
		this.resources = resources
	}

	override fun newInstance(): StringBundle = RouteDataBundle(resources)

	fun getResources(): RouteDataResources = resources
}
