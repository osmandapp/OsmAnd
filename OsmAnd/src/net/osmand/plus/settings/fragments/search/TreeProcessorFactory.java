package net.osmand.plus.settings.fragments.search;

import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeCreator;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.TreeCreatorDescription;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.TreeTransformerDescription;

public class TreeProcessorFactory implements de.KnollFrank.lib.settingssearch.db.preference.db.transformer.TreeProcessorFactory<Configuration> {

	@Override
	public SearchablePreferenceScreenTreeCreator<Configuration> createTreeCreator(final TreeCreatorDescription<Configuration> treeCreatorDescription) {
		throw new IllegalArgumentException(treeCreatorDescription.toString());
	}

	@Override
	public SearchablePreferenceScreenTreeTransformer<Configuration> createTreeTransformer(final TreeTransformerDescription<Configuration> treeTransformerDescription) {
		throw new IllegalArgumentException(treeTransformerDescription.toString());
	}
}
