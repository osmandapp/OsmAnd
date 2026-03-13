package net.osmand.plus.settings.fragments.search.ctxmenu;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.codepoetics.ambivalence.Either;

import java.util.List;

class PreferencesToScreenAdder {

	private final PreferenceScreen screen;

	public PreferencesToScreenAdder(final PreferenceScreen screen) {
		this.screen = screen;
	}

	public void addPreferencesToScreen(final List<Either<SimpleItem<Preference>, CategoryItem<PreferenceCategory, Preference>>> preferences) {
		preferences.forEach(this::addPreferenceToScreen);
	}

	private void addPreferenceToScreen(final Either<SimpleItem<Preference>, CategoryItem<PreferenceCategory, Preference>> preference) {
		preference.forEither(
				simpleItem -> addSimpleItemToGroup(simpleItem, screen),
				this::addCategoryItemToScreen);
	}

	private void addSimpleItemToGroup(final SimpleItem<Preference> simpleItem,
									  final PreferenceGroup group) {
		group.addPreference(simpleItem.item());
	}

	private void addCategoryItemToScreen(final CategoryItem<PreferenceCategory, Preference> categoryItem) {
		screen.addPreference(categoryItem.category());
		addSimpleItemsToGroup(categoryItem.children(), categoryItem.category());
	}

	private void addSimpleItemsToGroup(final List<SimpleItem<Preference>> simpleItems,
									   final PreferenceGroup group) {
		simpleItems.forEach(simpleItem -> addSimpleItemToGroup(simpleItem, group));
	}
}
