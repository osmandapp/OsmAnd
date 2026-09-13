package net.osmand.shared.routing

import okio.Source

/**
 * The `routing.xml` that ships with the application, or null where there is none.
 *
 * The file itself has not moved with this class: OsmAnd-java's `collectRoutingResources` task copies
 * it out of the `resources` repository into `net/osmand/router/`, and that is still the path the jvm
 * and android builds package it at, so the lookup is the one [RoutingConfiguration] always did.
 *
 * iOS has no classpath and does not route through this class - its C++ core reads its own copy of
 * the file - so there is nothing to open there.
 */
internal expect fun openBundledRoutingXml(): Source?
