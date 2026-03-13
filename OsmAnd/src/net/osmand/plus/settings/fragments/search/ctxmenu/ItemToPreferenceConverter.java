package net.osmand.plus.settings.fragments.search.ctxmenu;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.codepoetics.ambivalence.Either;

import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.List;

class ItemToPreferenceConverter {

	private final Context context;

	public ItemToPreferenceConverter(final Context context) {
		this.context = context;
	}

	public List<Either<SimpleItem<Preference>, CategoryItem<PreferenceCategory, Preference>>> asPreferences(final List<Either<SimpleItem<ContextMenuItem>, CategoryItem<ContextMenuItem, ContextMenuItem>>> items) {
		return items
				.stream()
				.map(this::asPreference)
				.toList();
	}

	private Either<SimpleItem<Preference>, CategoryItem<PreferenceCategory, Preference>> asPreference(final Either<SimpleItem<ContextMenuItem>, CategoryItem<ContextMenuItem, ContextMenuItem>> item) {
		return item.map(this::asPreference, this::asPreference);
	}

	private SimpleItem<Preference> asPreference(final SimpleItem<ContextMenuItem> simpleItem) {
		final Preference preference = new Preference(context);
		copyKeyTitleDescription(simpleItem.item(), preference);
		return new SimpleItem<>(preference);
	}

	private static void copyKeyTitleDescription(final ContextMenuItem src, final Preference dst) {
		dst.setKey(src.getId());
		dst.setTitle(src.getTitle());
		dst.setSummary(src.getDescription());
	}

	private CategoryItem<PreferenceCategory, Preference> asPreference(final CategoryItem<ContextMenuItem, ContextMenuItem> categoryItem) {
		final PreferenceCategory preferenceCategory = new PreferenceCategory(context);
		copyKeyTitleDescription(categoryItem.category(), preferenceCategory);
		return new CategoryItem<>(preferenceCategory, _asPreferences(categoryItem.children()));
	}

	private List<SimpleItem<Preference>> _asPreferences(final List<SimpleItem<ContextMenuItem>> simpleItems) {
		return simpleItems
				.stream()
				.map(this::asPreference)
				.toList();
	}
}
