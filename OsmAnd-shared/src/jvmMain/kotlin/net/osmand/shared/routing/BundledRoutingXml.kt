package net.osmand.shared.routing

import okio.Source
import okio.source

internal actual fun openBundledRoutingXml(): Source? =
	RoutingConfiguration::class.java.getResourceAsStream("/net/osmand/router/routing.xml")?.source()
