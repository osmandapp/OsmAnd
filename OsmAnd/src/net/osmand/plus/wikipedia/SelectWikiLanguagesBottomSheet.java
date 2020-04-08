package net.osmand.plus.wikipedia;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		if (savedInstanceState != null) {
			dismiss();
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setLanguageListEnable(!isGlobalWikiPoiEnabled);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		boolean nightMode = isNightMode(app);
		final int activeColorResId = AndroidUtils.resolveAttribute(app, R.attr.active_color_basic);
		final int profileColorResId = appMode.getIconColorInfo().getColor(nightMode);

		final int paddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		final int paddingHalf = app.getResources().getDimensionPixelSize(R.dimen.content_padding_half);

		items.add(new TitleItem(getString(R.string.shared_string_languages)));
		items.add(new LongDescriptionItem(getString(R.string.wikipedia_poi_languages_promo)));
		items.add(new DividerSpaceItem(app, paddingSmall));

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
		items.add(new DividerSpaceItem(app, paddingHalf));

		languageItems = new ArrayList<>();
		for (final WikiLanguageItem language : languages) {
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
		}
		items.addAll(languageItems);
	}

	private void initLanguagesData() {
		languages = new ArrayList<>();

		Bundle wikiPoiSettings = WikipediaPoiMenu.getWikiPoiSettings(app);
		List<String> enabledWikiPoiLocales = null;
		if (wikiPoiSettings != null) {
			isGlobalWikiPoiEnabled = wikiPoiSettings.getBoolean(GLOBAL_WIKI_POI_ENABLED_KEY);
			enabledWikiPoiLocales = wikiPoiSettings.getStringArrayList(ENABLED_WIKI_POI_LANGUAGES_KEY);
		}
		if (enabledWikiPoiLocales != null) {
			for (String locale : app.getPoiTypes().getAllAvailableWikiLocales()) {
				boolean checked = enabledWikiPoiLocales.contains(locale);
				languages.add(new WikiLanguageItem(locale, WikipediaPoiMenu.getTranslation(app, locale), checked));
			}
		} else {
			for (String locale : app.getPoiTypes().getAllAvailableWikiLocales()) {
				languages.add(new WikiLanguageItem(locale, WikipediaPoiMenu.getTranslation(app, locale), false));
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

		int color = AndroidUtils.getColorFromAttr(app, R.attr.divider_color_basic);
		int bgColor = UiUtilities.getColorWithAlpha(color, 0.5f);

		int bgResId = R.drawable.rectangle_rounded_right;
		Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(bgResId, bgColor);
		AndroidUtils.setBackground(buttonView, bgDrawable);

		int selectedModeColorId = appMode.getIconColorInfo().getColor(nightMode);
		UiUtilities.setupCompoundButton(nightMode, selectedModeColorId, cb);

		return buttonView;
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

		public WikiLanguageItem(String locale, String title, boolean checked) {
			this.locale = locale;
			this.title = title;
			this.checked = checked;
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

		public String getTitle() {
			return title;
		}

		@Override
		public int compareTo(WikiLanguageItem other) {
			return this.title.compareToIgnoreCase(other.title);
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
