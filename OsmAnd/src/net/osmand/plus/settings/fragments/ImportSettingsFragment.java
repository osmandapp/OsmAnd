package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.PlatformUtil;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.download.ReloadIndexesTask;
import net.osmand.plus.download.ReloadIndexesTask.ReloadIndexesListener;
import net.osmand.plus.settings.backend.backup.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportAsyncTask;
import net.osmand.plus.settings.backend.backup.SettingsItem;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

public class ImportSettingsFragment extends BaseSettingsListFragment {

	public static final String TAG = ImportSettingsFragment.class.getSimpleName();
	public static final Log LOG = PlatformUtil.getLog(ImportSettingsFragment.class.getSimpleName());

	private static final String DUPLICATES_START_TIME_KEY = "duplicates_start_time";
	private static final long MIN_DELAY_TIME_MS = 500;

	private File file;
	private SettingsHelper settingsHelper;
	private List<SettingsItem> settingsItems;

	private TextView description;
	private ProgressBar progressBar;
	private LinearLayout buttonsContainer;
	private CollapsingToolbarLayout toolbarLayout;

	private long duplicateStartTime;

	public static void showInstance(@NonNull FragmentManager fm, @NonNull List<SettingsItem> settingsItems, @NonNull File file) {
		ImportSettingsFragment fragment = new ImportSettingsFragment();
		fragment.setSettingsItems(settingsItems);
		fragment.setFile(file);
		fm.beginTransaction().
				replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(SETTINGS_LIST_TAG)
				.commitAllowingStateLoss();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			duplicateStartTime = savedInstanceState.getLong(DUPLICATES_START_TIME_KEY);
		}
		exportMode = false;
		settingsHelper = app.getSettingsHelper();

		ImportAsyncTask importTask = settingsHelper.getImportTask();
		if (importTask != null) {
			if (settingsItems == null) {
				settingsItems = importTask.getItems();
			}
			if (file == null) {
				file = importTask.getFile();
			}
			List<Object> duplicates = importTask.getDuplicates();
			List<SettingsItem> selectedItems = importTask.getSelectedItems();
			if (duplicates == null) {
				importTask.setDuplicatesListener(getDuplicatesListener());
			} else if (duplicates.isEmpty()) {
				if (selectedItems != null && file != null) {
					settingsHelper.importSettings(file, selectedItems, "", 1, getImportListener());
				}
			}
		}
		if (settingsItems != null) {
			dataList = SettingsHelper.getSettingsToOperateByCategory(settingsItems, false);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		toolbarLayout = view.findViewById(R.id.toolbar_layout);
		buttonsContainer = view.findViewById(R.id.buttons_container);
		progressBar = view.findViewById(R.id.progress_bar);

		description = header.findViewById(R.id.description);
		description.setText(R.string.select_data_to_import);

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(DUPLICATES_START_TIME_KEY, duplicateStartTime);
	}

	@Override
	protected void onContinueButtonClickAction() {
		if (adapter.getData().isEmpty()) {
			app.showShortToastMessage(getString(R.string.shared_string_nothing_selected));
		} else {
			importItems();
		}
	}

	private void updateUi(int toolbarTitleRes, int descriptionRes) {
		if (file != null) {
			String fileName = file.getName();
			toolbarLayout.setTitle(getString(toolbarTitleRes));
			description.setText(UiUtilities.createSpannableString(
					String.format(getString(descriptionRes), fileName),
					new StyleSpan(Typeface.BOLD), fileName
			));
			buttonsContainer.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			adapter.clearSettingsList();
		}
	}

	private void importItems() {
		List<SettingsItem> selectedItems = settingsHelper.prepareSettingsItems(adapter.getData(), settingsItems, false);
		if (file != null && settingsItems != null) {
			duplicateStartTime = System.currentTimeMillis();
			settingsHelper.checkDuplicates(file, settingsItems, selectedItems, getDuplicatesListener());
		}
		updateUi(R.string.shared_string_preparing, R.string.checking_for_duplicate_description);
	}

	public SettingsHelper.SettingsImportListener getImportListener() {
		return new SettingsHelper.SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {
				if (succeed) {
					app.getRendererRegistry().updateExternalRenderers();
					AppInitializer.loadRoutingFiles(app, null);
					reloadIndexes(items);
					AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
					if (plugin != null) {
						plugin.indexingFiles(null, true, true);
					}
					FragmentManager fm = getFragmentManager();
					if (fm != null && file != null) {
						ImportCompleteFragment.showInstance(fm, items, file.getName(), needRestart);
					}
				}
			}
		};
	}

	private void reloadIndexes(@NonNull List<SettingsItem> items) {
		for (SettingsItem item : items) {
			if (item instanceof FileSettingsItem && ((FileSettingsItem) item).getSubtype().isMap()) {
				Activity activity = getActivity();
				if (activity instanceof MapActivity) {
					final WeakReference<MapActivity> mapActivityRef = new WeakReference<>((MapActivity) activity);
					ReloadIndexesListener listener = new ReloadIndexesListener() {
						@Override
						public void reloadIndexesStarted() {

						}

						@Override
						public void reloadIndexesFinished(List<String> warnings) {
							MapActivity mapActivity = mapActivityRef.get();
							if (mapActivity != null) {
								mapActivity.refreshMap();
							}
						}
					};
					ReloadIndexesTask reloadIndexesTask = new ReloadIndexesTask(app, listener);
					reloadIndexesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				break;
			}
		}
	}

	private SettingsHelper.CheckDuplicatesListener getDuplicatesListener() {
		return new SettingsHelper.CheckDuplicatesListener() {
			@Override
			public void onDuplicatesChecked(@NonNull final List<Object> duplicates, final List<SettingsItem> items) {
				long spentTime = System.currentTimeMillis() - duplicateStartTime;
				if (spentTime < MIN_DELAY_TIME_MS) {
					long delay = MIN_DELAY_TIME_MS - spentTime;
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							processDuplicates(duplicates, items);
						}
					}, delay);
				} else {
					processDuplicates(duplicates, items);
				}
			}
		};
	}

	private void processDuplicates(List<Object> duplicates, List<SettingsItem> items) {
		FragmentManager fm = getFragmentManager();
		if (file != null) {
			if (duplicates.isEmpty()) {
				if (isAdded()) {
					updateUi(R.string.shared_string_importing, R.string.importing_from);
				}
				settingsHelper.importSettings(file, items, "", 1, getImportListener());
			} else if (fm != null && !isStateSaved()) {
				ImportDuplicatesFragment.showInstance(fm, duplicates, items, file, this);
			}
		}
	}

	public void setSettingsItems(List<SettingsItem> settingsItems) {
		this.settingsItems = settingsItems;
	}

	public void setFile(File file) {
		this.file = file;
	}
}
