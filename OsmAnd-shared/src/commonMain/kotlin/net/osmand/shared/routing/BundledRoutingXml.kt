package net.osmand.shared.routing

import okio.Source

/**
 * The `routing.xml` that ships with the application, or null where there is none.
 *
 * The file itself has not moved with this class: OsmAnd-java's `collectRoutingResources` task copies
 * it out of the `resources` repository into `net/osmand/router/`, and that is still the path the jvm
 * and android builds package it at, so the lookup is the one [RoutingConfiguration] always did.
 *
 * The iOS implementation currently returns null. Callers can supply a configuration file or
 * source through [RoutingConfiguration.parseFromFile] or [RoutingConfiguration.parseFromSource].
 */
internal expect fun openBundledRoutingXml(): Source?
