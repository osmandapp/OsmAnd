package net.osmand.plus.wikipedia;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class WikipediaSettingsFragment extends BaseSettingsFragment {

	private static final String WIKIPEDIA_LANGUAGES = "wikipedia_languages";
	private static final String IMAGES_CACHE = "wikipedia_images_cache";

	private String imagesCacheSummary;

	@Override
	protected void setupPreferences() {
		setupLanguagesPref();
		setupShowImagesPref();
		setupImagesCachePref();
	}

	private void setupLanguagesPref() {
		WikipediaPlugin plugin = PluginsHelper.getPlugin(WikipediaPlugin.class);
		Preference languages = new Preference(requireContext());
		languages.setKey(WIKIPEDIA_LANGUAGES);
		languages.setLayoutResource(R.layout.preference_with_descr);
		languages.setTitle(R.string.shared_string_languages);
		languages.setSummary(plugin != null ? plugin.getLanguagesSummary(getSelectedAppMode()) : null);
		languages.setIcon(getActiveIcon(R.drawable.ic_action_map_language));
		languages.setPersistent(false);
		addOnPreferencesScreen(languages);
	}

	private void setupShowImagesPref() {
		WikiArticleShowImages[] values = WikiArticleShowImages.values();
		String[] entries = new String[values.length];
		Integer[] entryValues = new Integer[values.length];
		for (int i = 0; i < values.length; i++) {
			entries[i] = getString(values[i].name);
			entryValues[i] = values[i].ordinal();
		}
		ListPreferenceEx showImages = createListPreferenceEx(settings.WIKI_ARTICLE_SHOW_IMAGES.getId(),
				entries, entryValues, R.string.download_images, R.layout.preference_with_descr);
		showImages.setIcon(getActiveIcon(R.drawable.ic_action_photo));
		addOnPreferencesScreen(showImages);
	}

	private void setupImagesCachePref() {
		Preference imagesCache = new Preference(requireContext());
		imagesCache.setKey(IMAGES_CACHE);
		imagesCache.setLayoutResource(R.layout.preference_with_descr);
		imagesCache.setTitle(R.string.images_cache);
		imagesCache.setSummary(imagesCacheSummary);
		imagesCache.setIcon(getActiveIcon(R.drawable.ic_action_storage));
		imagesCache.setPersistent(false);
		addOnPreferencesScreen(imagesCache);
		updateImagesCacheSize(false);
	}

	// The disk cache reads its journal on first access, so size and clearing stay off the UI thread
	private void updateImagesCacheSize(boolean clear) {
		new WikiImagesCacheTask(app, clear, sizeBytes -> {
			imagesCacheSummary = sizeBytes > 0 ? AndroidUtils.formatSize(app, sizeBytes)
					: app.getString(R.string.ltr_or_rtl_combine_via_space, "0", "MB");
			Preference imagesCache = isAdded() ? findPreference(IMAGES_CACHE) : null;
			if (imagesCache != null) {
				imagesCache.setSummary(imagesCacheSummary);
			}
		}).execute();
	}

	private void showClearImagesCacheDialog() {
		Context ctx = UiUtilities.getThemedContext(requireContext(), isNightMode());
		new AlertDialog.Builder(ctx)
				.setTitle(R.string.images_cache)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_clear, (dialog, which) -> {
					new WebView(ctx).clearCache(true);
					updateImagesCacheSize(true);
				})
				.show();
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		View switchProfile = view.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			AndroidUiHelper.updateVisibility(switchProfile, true);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (WIKIPEDIA_LANGUAGES.equals(preference.getKey())) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				SelectWikiLanguagesBottomSheet.showInstance(mapActivity, getSelectedAppMode(), this);
			}
			return true;
		} else if (IMAGES_CACHE.equals(preference.getKey())) {
			showClearImagesCacheDialog();
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		updateAllSettings();
	}

	@Override
	public String getAnalyticsScreen() {
		return "wikipedia_settings";
	}
}
