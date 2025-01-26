package net.osmand.plus.settings.bottomsheets;

import net.osmand.plus.base.dialog.data.DisplayItem;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.common.Optionals;

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
		return Optionals
				.streamOfPresentElements(
						Optional.ofNullable(displayItem.getTitle()),
						Optional.ofNullable(displayItem.getDescription()))
				.collect(Collectors.joining(", "));
	}
}
