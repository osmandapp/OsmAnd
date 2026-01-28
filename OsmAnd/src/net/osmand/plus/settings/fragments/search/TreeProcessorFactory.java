package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;

import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeCreator;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.TreeCreatorDescription;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.TreeTransformerDescription;

public class TreeProcessorFactory implements de.KnollFrank.lib.settingssearch.db.preference.db.transformer.TreeProcessorFactory<Configuration> {

	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;

	public TreeProcessorFactory(final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
	}

	@Override
	public SearchablePreferenceScreenTreeCreator<Configuration> createTreeCreator(final TreeCreatorDescription<Configuration> treeCreatorDescription) {
		if (SearchDatabaseRebuilder.class.equals(treeCreatorDescription.treeCreator())) {
			return new SearchDatabaseRebuilder(tileSourceTemplatesProvider);
		}
		throw new IllegalArgumentException(treeCreatorDescription.toString());
	}

	@Override
	public SearchablePreferenceScreenTreeTransformer<Configuration> createTreeTransformer(final TreeTransformerDescription<Configuration> treeTransformerDescription) {
		if (PluginSettingsOfConfigureProfileFragmentAdapter.class.equals(treeTransformerDescription.treeTransformer())) {
			return new PluginSettingsOfConfigureProfileFragmentAdapter(tileSourceTemplatesProvider);
		} else if (SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter.class.equals(treeTransformerDescription.treeTransformer())) {
			return new SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter(
					SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter.getPreferenceFragment(treeTransformerDescription.params()),
					tileSourceTemplatesProvider);
		}
		throw new IllegalArgumentException(treeTransformerDescription.toString());
	}
}
