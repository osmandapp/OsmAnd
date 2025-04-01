package net.osmand.plus.widgets.alert;

import net.osmand.plus.settings.fragments.search.Collectors;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.common.Lists;

public record SelectionDialogFragmentData(List<String> keys,
										  List<CharSequence> items,
										  Optional<boolean[]> checkedItems,
										  int selectedItemIndex) {

	public SelectionDialogFragmentData {
		if (keys.size() != items.size()) {
			throw new IllegalArgumentException("keys and items must have the same size");
		}
		if (checkedItems.isPresent() && checkedItems.orElseThrow().length != keys.size()) {
			throw new IllegalArgumentException("checkedItems must have the same size as keys");
		}
	}

	public Map<String, CharSequence> orderedItemByKey() {
		return Lists
				.zip(keys(), items())
				.stream()
				.collect(
						Collectors.toOrderedMap(
								keyItemPair -> keyItemPair.first,
								keyItemPair -> keyItemPair.second));

	}
}
