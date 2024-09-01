package net.osmand.plus.settings.fragments;

import net.osmand.plus.settings.backend.ApplicationMode;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.MaskSubgraph;

import java.util.Set;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.PreferenceEdge;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;

class GraphMinimizer {

	public static Graph<PreferenceScreenWithHost, PreferenceEdge> minimizeGraph(final Graph<PreferenceScreenWithHost, PreferenceEdge> preferenceScreenGraph) {
		return getSettingsSubGraph(getGraphWithoutApplicationModes(preferenceScreenGraph));
	}

	private static Graph<PreferenceScreenWithHost, PreferenceEdge> getGraphWithoutApplicationModes(final Graph<PreferenceScreenWithHost, PreferenceEdge> graph) {
		final Set<String> keysOfApplicationModes = getKeysOfApplicationModes();
		return new MaskSubgraph<>(
				graph,
				preferenceScreenWithHost -> false,
				preferenceEdge -> keysOfApplicationModes.contains(preferenceEdge.preference.getKey()));
	}

	private static Set<String> getKeysOfApplicationModes() {
		return ApplicationMode
				.allPossibleValues()
				.stream()
				.map(ApplicationMode::getStringKey)
				.collect(Collectors.toSet());
	}

	private static Graph<PreferenceScreenWithHost, PreferenceEdge> getSettingsSubGraph(final Graph<PreferenceScreenWithHost, PreferenceEdge> graph) {
		return new AsSubgraph<>(
				graph,
				new ConnectivityInspector<>(graph).connectedSetOf(getSettingsScreen(graph)));
	}

	private static PreferenceScreenWithHost getSettingsScreen(final Graph<PreferenceScreenWithHost, PreferenceEdge> graph) {
		return graph
				.vertexSet()
				.stream()
				.filter(preferenceScreenWithHost -> "Settings".equals(preferenceScreenWithHost.preferenceScreen.toString()))
				.findFirst()
				.get();
	}
}
