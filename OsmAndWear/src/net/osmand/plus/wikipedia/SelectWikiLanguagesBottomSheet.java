package net.osmand.plus.wikipedia;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SelectWikiLanguagesBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectWikiLanguagesBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private ApplicationMode appMode;
	private WikipediaPlugin wikiPlugin;

	private List<BottomSheetItemWithCompoundButton> languageItems;

	private ArrayList<WikiLanguageItem> languages;
	private boolean isGlobalWikiPoiEnabled;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		appMode = app.getSettings().getApplicationMode();
		wikiPlugin = PluginsHelper.getPlugin(WikipediaPlugin.class);
		initLanguagesData();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setLanguageListEnable(!isGlobalWikiPoiEnabled);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		int activeColorResId = ColorUtilities.getActiveColorId(nightMode);
		int profileColor = appMode.getProfileColor(nightMode);

		int contentPadding = app.getResources().getDimensionPixelSize(R.dimen.content_padding);
		int contentPaddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		int contentPaddingHalf = app.getResources().getDimensionPixelSize(R.dimen.content_padding_half);

		items.add(new TitleItem(getString(R.string.shared_string_languages)));
		items.add(new LongDescriptionItem(getString(R.string.some_articles_may_not_available_in_lang)));
		items.add(new DividerSpaceItem(app, contentPadding));
		items.add(new LongDescriptionItem(getString(R.string.select_wikipedia_article_langs)));
		items.add(new DividerSpaceItem(app, contentPaddingSmall));

		BottomSheetItemWithCompoundButton[] btnSelectAll = new BottomSheetItemWithCompoundButton[1];
		btnSelectAll[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(this.isGlobalWikiPoiEnabled)
				.setCompoundButtonColor(profileColor)
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
		for (WikiLanguageItem language : languages) {
			if (!categoryChanged && !language.isTopDefined()) {
				categoryChanged = true;
				DividerItem divider = new DividerItem(app);
				divider.setMargins(contentPadding, 0, 0, 0);
				items.add(divider);
			}
			BottomSheetItemWithCompoundButton[] languageItem = new BottomSheetItemWithCompoundButton[1];
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

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		return (MapActivity) activity;
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

		isGlobalWikiPoiEnabled = wikiPlugin.isShowAllLanguages();
		if (wikiPlugin.hasLanguagesFilter()) {
			List<String> enabledWikiPoiLocales = wikiPlugin.getLanguagesToShow();
			for (String locale : app.getPoiTypes().getAllAvailableWikiLocales()) {
				boolean checked = enabledWikiPoiLocales.contains(locale);
				boolean topDefined = preferredLocales.contains(locale) || checked;
				languages.add(new WikiLanguageItem(locale,
						wikiPlugin.getWikiLanguageTranslation(locale), checked, topDefined));
			}
		} else {
			isGlobalWikiPoiEnabled = true;
			for (String locale : app.getPoiTypes().getAllAvailableWikiLocales()) {
				boolean topDefined = preferredLocales.contains(locale);
				languages.add(new WikiLanguageItem(locale,
						wikiPlugin.getWikiLanguageTranslation(locale), false, topDefined));
			}
		}

		Collections.sort(languages);
	}

	private void setLanguageListEnable(boolean enable) {
		int textColorPrimaryId = ColorUtilities.getPrimaryTextColorId(nightMode);
		int disableColorId = nightMode ?
				R.color.text_color_secondary_dark :
				R.color.text_color_secondary_light;
		int profileColor = appMode.getProfileColor(nightMode);
		int disableColor = ContextCompat.getColor(app, disableColorId);
		for (BottomSheetItemWithCompoundButton item : languageItems) {
			item.getView().setEnabled(enable);
			item.setTitleColorId(enable ? textColorPrimaryId : disableColorId);
			CompoundButton cb = item.getCompoundButton();
			if (cb != null) {
				cb.setEnabled(enable);
				UiUtilities.setupCompoundButton(nightMode, enable ? profileColor : disableColor, cb);
			}
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		List<String> localesForSaving = new ArrayList<>();
		for (WikiLanguageItem language : languages) {
			if (language.isChecked()) {
				localesForSaving.add(language.getLocale());
			}
		}
		applyPreferenceWithSnackBar(localesForSaving, isGlobalWikiPoiEnabled);
		dismiss();
	}

	protected final void applyPreference(boolean applyToAllProfiles, List<String> localesForSaving, boolean global) {
		if (applyToAllProfiles) {
			for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
				wikiPlugin.setLanguagesToShow(mode, localesForSaving);
				wikiPlugin.setShowAllLanguages(mode, global);
			}
		} else {
			wikiPlugin.setLanguagesToShow(localesForSaving);
			wikiPlugin.setShowAllLanguages(global);
		}

		wikiPlugin.updateWikipediaState();
	}

	protected void applyPreferenceWithSnackBar(List<String> localesForSaving, boolean global) {
		applyPreference(false, localesForSaving, global);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			String modeName = appMode.toHumanString();
			String text = app.getString(R.string.changes_applied_to_profile, modeName);
			SpannableString message = UiUtilities.createSpannableString(text, Typeface.BOLD, modeName);
			Snackbar snackbar = Snackbar.make(mapActivity.getLayout(), message, Snackbar.LENGTH_LONG)
					.setAction(R.string.apply_to_all_profiles, new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							applyPreference(true, localesForSaving, global);
						}
					});
			UiUtilities.setupSnackbarVerticalLayout(snackbar);
			UiUtilities.setupSnackbar(snackbar, nightMode);
			snackbar.show();
		}
	}

	private View getCustomButtonView() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return null;
		}
		View buttonView = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.bottom_sheet_item_title_with_swith_56dp, null);
		CompoundButton cb = buttonView.findViewById(R.id.compound_button);

		int color = ColorUtilities.getDividerColor(app, nightMode);
		int bgColor = ColorUtilities.getColorWithAlpha(color, 0.5f);

		int bgResId = R.drawable.rectangle_rounded_right;
		Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(bgResId, bgColor);
		AndroidUtils.setBackground(buttonView, bgDrawable);

		int selectedModeColorId = appMode.getProfileColor(nightMode);
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

	private static class WikiLanguageItem implements Comparable<WikiLanguageItem> {
		private final String locale;
		private final String title;
		private boolean checked;
		private final boolean topDefined;

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
									boolean usedOnMap) {
		SelectWikiLanguagesBottomSheet fragment = new SelectWikiLanguagesBottomSheet();
		fragment.setUsedOnMap(usedOnMap);
		fragment.show(mapActivity.getSupportFragmentManager(), TAG);
	}
}
