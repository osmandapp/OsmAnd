package net.osmand.plus.wikipedia;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
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
import net.osmand.plus.helpers.LanguageUtilities;

import java.util.ArrayList;
import java.util.List;

public class SelectWikiLanguagesBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectWikiLanguagesBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private ApplicationMode appMode;
	private boolean allLanguages = false;
	private ArrayList<WikiLanguageItem> languages;
	private CallbackWithObject<Boolean> callback;
	private List<BottomSheetItemWithCompoundButton> languageItems;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		initLanguagesData();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		boolean nightMode = isNightMode(app);
		final int textColorPrimary = nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
		final int activeColorResId = AndroidUtils.resolveAttribute(app, R.attr.active_color_basic);
		final int profileColorResId = getAppMode().getIconColorInfo().getColor(nightMode);
		final int disableTextColor = nightMode ?
				R.color.active_buttons_and_links_text_disabled_dark :
				R.color.active_buttons_and_links_text_disabled_light;

		final int paddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		final int paddingHalf = app.getResources().getDimensionPixelSize(R.dimen.content_padding_half);

		items.add(new TitleItem(getString(R.string.shared_string_languages)));
		items.add(new LongDescriptionItem(getString(R.string.wikipedia_languages_promo)));
		items.add(new DividerSpaceItem(app, paddingSmall));

		final BottomSheetItemWithCompoundButton[] selectAll = new BottomSheetItemWithCompoundButton[1];
		selectAll[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(this.allLanguages)
				.setCompoundButtonColorId(profileColorResId)
				.setTitle(getString(R.string.shared_string_all_languages))
				.setTitleColorId(activeColorResId)
				.setCustomView(getCustomButtonView())
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						allLanguages = !allLanguages;
						selectAll[0].setChecked(allLanguages);
						setLanguagesEnable(!allLanguages);
					}
				})
				.create();
		items.add(selectAll[0]);
		items.add(new DividerSpaceItem(app, paddingHalf));

		languageItems = new ArrayList<>();
		for (final WikiLanguageItem language : languages) {
			final BottomSheetItemWithCompoundButton[] languageItem = new BottomSheetItemWithCompoundButton[1];
					languageItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(language.isChecked())
					.setCompoundButtonColorId(allLanguages ? disableTextColor : profileColorResId)
					.setTitle(language.getTitle())
					.setTitleColorId(allLanguages ? disableTextColor : textColorPrimary)
					.setLayoutId(R.layout.bottom_sheet_item_title_with_checkbox)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							boolean newValue = !languageItem[0].isChecked();
							language.setChecked(newValue);
						}
					})
					.create();
			languageItems.add(languageItem[0]);
			language.setLanguageListener(new OnCheckLanguageListener() {
				@Override
				public void onCheckLanguage(boolean check) {
					languageItem[0].setChecked(check);
				}
			});
		}
		items.addAll(languageItems);
	}

	private void initLanguagesData() {
		languages = new ArrayList<>();
		allLanguages = app.getSettings().ENABLE_ALL_WIKI_LANGUAGES.getModeValue(appMode);

		List<LanguageUtilities.Language> supportedLanguages = LanguageUtilities.getSupportedLanguages(app);
		List<String> activatedLocales = getChosenWikiLanguages(app);

		for (LanguageUtilities.Language language : supportedLanguages) {
			String locale = language.getLocale();
			String title = language.getTranslation();
			boolean checked = false;
			if (activatedLocales != null) {
				checked = activatedLocales.contains(locale);
			}
			languages.add(new WikiLanguageItem(locale, title, checked));
		}
	}

	private void setLanguagesEnable(boolean enable) {
		boolean nightMode = isNightMode(app);
		int textColorPrimary = nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
		int disableTextColor = nightMode ? R.color.active_buttons_and_links_text_disabled_dark : R.color.active_buttons_and_links_text_disabled_light;
		int profileColorResId = getAppMode().getIconColorInfo().getColor(nightMode);
		for (BottomSheetItemWithCompoundButton item : languageItems) {
			item.getView().setEnabled(enable);
			item.setTitleColorId(enable ? textColorPrimary : disableTextColor);
			CompoundButton cb = item.getCompoundButton();
			if (cb != null) {
				cb.setEnabled(enable);
				UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(app, enable ? profileColorResId : disableTextColor), cb);
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
		OsmandSettings settings = app.getSettings();
		ApplicationMode appMode = getAppMode();
		List<String> languagesList = new ArrayList<>();
		for (WikiLanguageItem language : languages) {
			if (language.isChecked()) {
				languagesList.add(language.getLanguage());
			}
		}
		settings.ENABLED_WIKI_LANGUAGES.setStringsList(languagesList);
		settings.ENABLE_ALL_WIKI_LANGUAGES.setModeValue(appMode, allLanguages);
		if (callback != null) {
			callback.processResult(true);
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

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
		} else {
			int bgResId = R.drawable.rectangle_rounded_right;
			Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(bgResId, bgColor);
			AndroidUtils.setBackground(buttonView, bgDrawable);
		}

		int selectedModeColorId = getAppMode().getIconColorInfo().getColor(nightMode);
		UiUtilities.setupCompoundButton(nightMode, selectedModeColorId, cb);

		return buttonView;
	}

	public void setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public ApplicationMode getAppMode() {
		return appMode;
	}

	public void setCallback(CallbackWithObject<Boolean> callback) {
		this.callback = callback;
	}

	private class WikiLanguageItem {
		private String language;
		private String title;
		private boolean checked;
		private OnCheckLanguageListener languageListener;

		public WikiLanguageItem(String language, String title, boolean checked) {
			this.language = language;
			this.title = title;
			this.checked = checked;
		}

		public String getLanguage() {
			return language;
		}

		public boolean isChecked() {
			return checked;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
			if (languageListener != null) {
				languageListener.onCheckLanguage(checked);
			}
		}

		public void setLanguageListener(OnCheckLanguageListener languageListener) {
			this.languageListener = languageListener;
		}

		public String getTitle() {
			return title;
		}
	}

	private interface OnCheckLanguageListener {
		void onCheckLanguage(boolean check);
	}

	public static List<String> getChosenWikiLanguages(OsmandApplication app) {
		return app.getSettings().ENABLED_WIKI_LANGUAGES.getStringsList();
	}

	public static void showInstance(@NonNull MapActivity mapActivity,
	                                @NonNull ApplicationMode appMode,
	                                boolean usedOnMap,
	                                CallbackWithObject<Boolean> callback) {
		SelectWikiLanguagesBottomSheet fragment = new SelectWikiLanguagesBottomSheet();
		fragment.setAppMode(appMode);
		fragment.setUsedOnMap(usedOnMap);
		fragment.setCallback(callback);
		fragment.show(mapActivity.getSupportFragmentManager(), SelectWikiLanguagesBottomSheet.TAG);
	}
}
