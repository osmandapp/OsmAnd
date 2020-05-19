package net.osmand.plus.wikipedia;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static net.osmand.plus.wikipedia.WikipediaPoiMenu.ENABLED_WIKI_POI_LANGUAGES_KEY;
import static net.osmand.plus.wikipedia.WikipediaPoiMenu.GLOBAL_WIKI_POI_ENABLED_KEY;

public class SelectWikiLanguagesBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectWikiLanguagesBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private ApplicationMode appMode;
	private OsmandSettings settings;

	private List<BottomSheetItemWithCompoundButton> languageItems;

	private ArrayList<WikiLanguageItem> languages;
	private CallbackWithObject<Boolean> languageChangedCallback;
	private boolean isGlobalWikiPoiEnabled = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		settings = app.getSettings();
		initLanguagesData();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setLanguageListEnable(!isGlobalWikiPoiEnabled);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		final int profileColorResId = appMode.getIconColorInfo().getColor(nightMode);

		final int contentPadding = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
		final int contentPaddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		final int contentPaddingHalf = app.getResources().getDimensionPixelSize(R.dimen.content_padding_half);

		items.add(new TitleItem(getString(R.string.shared_string_languages)));
		items.add(new LongDescriptionItem(getString(R.string.some_articles_may_not_available_in_lang)));
		items.add(new DividerSpaceItem(app, contentPadding));
		items.add(new LongDescriptionItem(getString(R.string.select_wikipedia_article_langs)));
		items.add(new DividerSpaceItem(app, contentPaddingSmall));

		final BottomSheetItemWithCompoundButton[] btnSelectAll = new BottomSheetItemWithCompoundButton[1];
		btnSelectAll[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(this.isGlobalWikiPoiEnabled)
				.setCompoundButtonColorId(profileColorResId)
				.setTitle(getString(R.string.shared_string_all_languages))
				.setTitleColorId(activeColorResId)
				.setCustomView(getCustomButtonView())
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						isGlobalWikiPoiEnabled = !isGlobalWikiPoiEnabled;
						btnSelectAll[0].setChecked(isGlobalWikiPoiEnabled);
						setLanguageListEnable(!isGlobalWikiPoiEnabled);
					}
				})
				.create();
		items.add(btnSelectAll[0]);
		items.add(new DividerSpaceItem(app, contentPaddingHalf));

		languageItems = new ArrayList<>();
		boolean categoryChanged = false;
		for (final WikiLanguageItem language : languages) {
			if (!categoryChanged && !language.isTopDefined()) {
				categoryChanged = true;
				DividerItem divider = new DividerItem(app);
				divider.setMargins(contentPadding, 0, 0, 0);
				items.add(divider);
			}
			final BottomSheetItemWithCompoundButton[] languageItem = new BottomSheetItemWithCompoundButton[1];
			languageItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(language.isChecked())
					.setTitle(language.getTitle())
					.setLayoutId(R.layout.bottom_sheet_item_title_with_checkbox)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							boolean newValue = !languageItem[0].isChecked();
							languageItem[0].setChecked(newValue);
							language.setChecked(newValue);
						}
					})
					.create();
			languageItems.add(languageItem[0]);
			items.add(languageItem[0]);
		}
	}

	private void initLanguagesData() {
		languages = new ArrayList<>();

		Set<String> preferredLocales = new HashSet<>();
		LocaleListCompat locales = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration());
		for (int i = 0; i < locales.size(); i++) {
			preferredLocales.add(locales.get(i).getLanguage());
		}
		preferredLocales.add(app.getLanguage());
		preferredLocales.add(Locale.getDefault().getLanguage());

		Bundle wikiPoiSettings = WikipediaPoiMenu.getWikiPoiSettings(app);
		List<String> enabledWikiPoiLocales = null;
		if (wikiPoiSettings != null) {
			isGlobalWikiPoiEnabled = wikiPoiSettings.getBoolean(GLOBAL_WIKI_POI_ENABLED_KEY);
			enabledWikiPoiLocales = wikiPoiSettings.getStringArrayList(ENABLED_WIKI_POI_LANGUAGES_KEY);
		}
		if (enabledWikiPoiLocales != null) {
			for (String locale : app.getPoiTypes().getAllAvailableWikiLocales()) {
				boolean checked = enabledWikiPoiLocales.contains(locale);
				boolean topDefined = preferredLocales.contains(locale) || checked;
				languages.add(new WikiLanguageItem(locale, WikipediaPoiMenu.getTranslation(app, locale), checked, topDefined));
			}
		} else {
			for (String locale : app.getPoiTypes().getAllAvailableWikiLocales()) {
				boolean topDefined = preferredLocales.contains(locale);
				languages.add(new WikiLanguageItem(locale, WikipediaPoiMenu.getTranslation(app, locale), false, topDefined));
			}
		}

		Collections.sort(languages);
	}

	private void setLanguageListEnable(boolean enable) {
		int textColorPrimaryId = nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
		int disableColorId = nightMode ? R.color.active_buttons_and_links_text_disabled_dark : R.color.active_buttons_and_links_text_disabled_light;
		int profileColorId = appMode.getIconColorInfo().getColor(nightMode);
		for (BottomSheetItemWithCompoundButton item : languageItems) {
			item.getView().setEnabled(enable);
			item.setTitleColorId(enable ? textColorPrimaryId : disableColorId);
			CompoundButton cb = item.getCompoundButton();
			if (cb != null) {
				cb.setEnabled(enable);
				UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app, enable ? profileColorId : disableColorId), cb);
			}
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		super.onRightBottomButtonClick();
		List<String> localesForSaving = new ArrayList<>();
		for (WikiLanguageItem language : languages) {
			if (language.isChecked()) {
				localesForSaving.add(language.getLocale());
			}
		}
		settings.WIKIPEDIA_POI_ENABLED_LANGUAGES.setStringsListForProfile(appMode, localesForSaving);
		settings.GLOBAL_WIKIPEDIA_POI_ENABLED.setModeValue(appMode, isGlobalWikiPoiEnabled);
		if (languageChangedCallback != null) {
			languageChangedCallback.processResult(true);
		}
		dismiss();
	}

	private View getCustomButtonView() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return null;
		}
		View buttonView = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.bottom_sheet_item_title_with_swith_56dp, null);
		CompoundButton cb = buttonView.findViewById(R.id.compound_button);

		int color = nightMode ? R.color.divider_color_dark : R.color.divider_color_light;
		int bgColor = UiUtilities.getColorWithAlpha(ContextCompat.getColor(app, color), 0.5f);

		int bgResId = R.drawable.rectangle_rounded_right;
		Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(bgResId, bgColor);
		AndroidUtils.setBackground(buttonView, bgDrawable);

		int selectedModeColorId = appMode.getIconColorInfo().getColor(nightMode);
		UiUtilities.setupCompoundButton(nightMode, selectedModeColorId, cb);

		return buttonView;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (requireActivity().isChangingConfigurations()) {
			dismiss();
		}
	}

	public void setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public void setLanguageChangedCallback(CallbackWithObject<Boolean> languageChangedCallback) {
		this.languageChangedCallback = languageChangedCallback;
	}

	private class WikiLanguageItem implements Comparable<WikiLanguageItem> {
		private String locale;
		private String title;
		private boolean checked;
		private boolean topDefined;

		public WikiLanguageItem(String locale, String title, boolean checked, boolean topDefined) {
			this.locale = locale;
			this.title = title;
			this.checked = checked;
			this.topDefined = topDefined;
		}

		public String getLocale() {
			return locale;
		}

		public boolean isChecked() {
			return checked;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
		}

		public boolean isTopDefined() {
			return topDefined;
		}

		public String getTitle() {
			return title;
		}

		@Override
		public int compareTo(WikiLanguageItem other) {
			int result = other.topDefined ? (!this.topDefined ? 1 : 0) : (this.topDefined ? -1 : 0);
			return result != 0 ? result : this.title.compareToIgnoreCase(other.title);
		}
	}

	public static void showInstance(@NonNull MapActivity mapActivity,
	                                @NonNull ApplicationMode appMode,
	                                boolean usedOnMap,
	                                CallbackWithObject<Boolean> callback) {
		SelectWikiLanguagesBottomSheet fragment = new SelectWikiLanguagesBottomSheet();
		fragment.setAppMode(appMode);
		fragment.setUsedOnMap(usedOnMap);
		fragment.setLanguageChangedCallback(callback);
		fragment.show(mapActivity.getSupportFragmentManager(), SelectWikiLanguagesBottomSheet.TAG);
	}
}
