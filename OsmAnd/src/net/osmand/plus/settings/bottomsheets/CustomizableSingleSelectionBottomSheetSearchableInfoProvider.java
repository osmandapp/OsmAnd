package net.osmand.plus.settings.bottomsheets;

import net.osmand.plus.base.dialog.data.DisplayItem;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.common.Lists;

public class CustomizableSingleSelectionBottomSheetSearchableInfoProvider {

	public static String getSearchableInfo(final String title,
										   final String description,
										   final List<DisplayItem> displayItems) {
		return String.join(", ", title, description, asString(displayItems));
	}

	private static String asString(final List<DisplayItem> displayItems) {
		return displayItems
				.stream()
				.map(CustomizableSingleSelectionBottomSheetSearchableInfoProvider::asString)
				.collect(Collectors.joining(", "));
	}

	private static String asString(final DisplayItem displayItem) {
		return String.join(
				", ",
				concat(
						Optional.ofNullable(displayItem.getTitle()),
						Optional.ofNullable(displayItem.getDescription())));
	}

	private static List<CharSequence> concat(final Optional<CharSequence> dialogTitle,
											 final Optional<CharSequence> description) {
		return Lists.getPresentElements(List.of(dialogTitle, description));
	}
}
