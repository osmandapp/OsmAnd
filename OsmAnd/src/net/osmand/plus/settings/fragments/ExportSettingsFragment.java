package net.osmand.plus.settings.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.AndroidUtils;
import net.osmand.FileUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsHelper.SettingsExportListener;
import net.osmand.plus.settings.backend.backup.SettingsItem;
import net.osmand.plus.settings.fragments.ExportSettingsAdapter.OnItemSelectedListener;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;

public class ExportSettingsFragment extends BaseOsmAndFragment implements OnItemSelectedListener {

	public static final String TAG = ImportSettingsFragment.class.getSimpleName();
	public static final Log LOG = PlatformUtil.getLog(ImportSettingsFragment.class.getSimpleName());

	private static final String EXPORT_SETTINGS_TAG = "import_settings_tag";
	private static final String GLOBAL_EXPORT_KEY = "global_export_key";
	private static final String EXPORT_START_TIME_KEY = "export_start_time_key";
	private static final String EXPORTING_STARTED_KEY = "exporting_started_key";
	private static final String INCLUDE_ADDITIONAL_DATA_KEY = "include_additional_data_key";
	private static final String INCLUDE_GLOBAL_SETTINGS_KEY = "include_global_settings_key";
	private static final String PROGRESS_MAX_KEY = "progress_max_key";
	private static final String PROGRESS_VALUE_KEY = "progress_value_key";

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yy", Locale.US);

	private OsmandApplication app;
	private Map<ExportSettingsCategory, List<ExportDataObject>> dataList;

	private ProgressDialog progress;
	private ApplicationMode appMode;
	private SettingsExportListener exportListener;

	private View continueBtn;
	private View headerShadow;
	private View headerDivider;
	private View itemsSizeContainer;
	private View availableSpaceContainer;
	private TextViewEx selectedItemsSize;
	private TextViewEx availableSpaceDescr;
	private LinearLayout buttonsContainer;
	private ExpandableListView expandableList;
	private ExportSettingsAdapter adapter;

	private int progressMax;
	private int progressValue;

	private long exportStartTime;

	private boolean nightMode;
	private boolean globalExport;
	private boolean exportingStarted;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();
		if (savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
			globalExport = savedInstanceState.getBoolean(GLOBAL_EXPORT_KEY);
			exportingStarted = savedInstanceState.getBoolean(EXPORTING_STARTED_KEY);
			exportStartTime = savedInstanceState.getLong(EXPORT_START_TIME_KEY);
			progressMax = savedInstanceState.getInt(PROGRESS_MAX_KEY);
			progressValue = savedInstanceState.getInt(PROGRESS_VALUE_KEY);
		}
		dataList = app.getSettingsHelper().getAdditionalData(globalExport);

		requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				showExitDialog();
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View root = themedInflater.inflate(R.layout.fragment_import, container, false);
		AndroidUtils.addStatusBarPadding21v(app, root);

		selectedItemsSize = root.findViewById(R.id.file_size);
		itemsSizeContainer = root.findViewById(R.id.file_size_container);
		expandableList = root.findViewById(R.id.list);
		buttonsContainer = root.findViewById(R.id.buttons_container);

		Toolbar toolbar = root.findViewById(R.id.toolbar);
		setupToolbar(toolbar);
		ViewCompat.setNestedScrollingEnabled(expandableList, true);

		View header = themedInflater.inflate(R.layout.list_item_description_header, null);
		headerDivider = header.findViewById(R.id.divider);
		headerShadow = header.findViewById(R.id.card_bottom_divider);
		expandableList.addHeaderView(header);

		availableSpaceContainer = inflater.inflate(R.layout.enough_space_warning_card, null);
		availableSpaceDescr = availableSpaceContainer.findViewById(R.id.warning_descr);

		continueBtn = root.findViewById(R.id.continue_button);
		UiUtilities.setupDialogButton(nightMode, continueBtn, DialogButtonType.PRIMARY, getString(R.string.shared_string_continue));
		continueBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				prepareFile();
			}
		});

		ViewTreeObserver treeObserver = buttonsContainer.getViewTreeObserver();
		treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (buttonsContainer != null) {
					ViewTreeObserver vts = buttonsContainer.getViewTreeObserver();
					int height = buttonsContainer.getMeasuredHeight();
					expandableList.setPadding(0, 0, 0, height);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						vts.removeOnGlobalLayoutListener(this);
					} else {
						vts.removeGlobalOnLayoutListener(this);
					}
				}
			}
		});

		adapter = new ExportSettingsAdapter(app, this, nightMode);
		adapter.updateSettingsList(dataList);
		expandableList.setAdapter(adapter);

		CollapsingToolbarLayout toolbarLayout = root.findViewById(R.id.toolbar_layout);
		toolbarLayout.setTitle(getString(R.string.shared_string_export));
		TextView description = header.findViewById(R.id.description);
		description.setText(R.string.select_data_to_export);
		updateAvailableSpace();

		return root;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(GLOBAL_EXPORT_KEY, globalExport);
		outState.putBoolean(EXPORTING_STARTED_KEY, exportingStarted);
		outState.putLong(EXPORT_START_TIME_KEY, exportStartTime);
		outState.putString(APP_MODE_KEY, appMode.getStringKey());
		if (progress != null) {
			outState.putInt(PROGRESS_MAX_KEY, progress.getMax());
			outState.putInt(PROGRESS_VALUE_KEY, progress.getProgress());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		checkExportingFile();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (exportingStarted) {
			File file = getExportFile();
			app.getSettingsHelper().updateExportListener(file, null);
		}
	}

	private void dismissFragment() {
		FragmentManager fm = getFragmentManager();
		if (fm != null && !fm.isStateSaved()) {
			getFragmentManager().popBackStack(EXPORT_SETTINGS_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public void showExitDialog() {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), nightMode);
		AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
		dismissDialog.setTitle(getString(R.string.shared_string_dismiss));
		dismissDialog.setMessage(getString(R.string.exit_without_saving));
		dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
		dismissDialog.setPositiveButton(R.string.shared_string_exit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismissFragment();
			}
		});
		dismissDialog.show();
	}

	private void setupToolbar(Toolbar toolbar) {
		int color = ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_text_dark : R.color.active_buttons_and_links_text_light);
		toolbar.setNavigationIcon(getPaintedContentIcon(R.drawable.ic_action_close, color));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showExitDialog();
			}
		});
	}

	private void updateAvailableSpace() {
		long calculatedSize = ExportSettingsAdapter.calculateItemsSize(adapter.getData());
		if (calculatedSize != 0) {
			selectedItemsSize.setText(AndroidUtils.formatSize(app, calculatedSize));

			File dir = app.getAppPath("").getParentFile();
			long availableSizeBytes = AndroidUtils.getAvailableSpace(dir);
			if (calculatedSize > availableSizeBytes) {
				String availableSize = AndroidUtils.formatSize(app, availableSizeBytes);
				availableSpaceDescr.setText(getString(R.string.export_not_enough_space_descr, availableSize));
				updateWarningHeaderVisibility(true);
				continueBtn.setEnabled(false);
			} else {
				updateWarningHeaderVisibility(false);
				continueBtn.setEnabled(adapter.hasSelectedData());
			}
			itemsSizeContainer.setVisibility(View.VISIBLE);
		} else {
			updateWarningHeaderVisibility(false);
			itemsSizeContainer.setVisibility(View.INVISIBLE);
			continueBtn.setEnabled(adapter.hasSelectedData());
		}
	}

	private void updateWarningHeaderVisibility(boolean visible) {
		if (visible) {
			if (expandableList.getHeaderViewsCount() < 2) {
				expandableList.addHeaderView(availableSpaceContainer);
			}
			AndroidUiHelper.updateVisibility(headerShadow, false);
			AndroidUiHelper.updateVisibility(headerDivider, true);
		} else {
			expandableList.removeHeaderView(availableSpaceContainer);
			AndroidUiHelper.updateVisibility(headerShadow, true);
			AndroidUiHelper.updateVisibility(headerDivider, false);
		}
	}

	@Override
	public void onCategorySelected(ExportSettingsCategory type, boolean selected) {
		updateAvailableSpace();
	}

	@Override
	public void onTypeSelected(ExportSettingsType type, boolean selected) {
		updateAvailableSpace();
	}

	private void prepareFile() {
		if (app != null) {
			exportingStarted = true;
			exportStartTime = System.currentTimeMillis();
			showExportProgressDialog();
			File tempDir = FileUtils.getTempDir(app);
			String fileName = getFileName();
			List<SettingsItem> items = app.getSettingsHelper().prepareAdditionalSettingsItems(adapter.getData());
			progress.setMax(getMaxProgress(items));
			app.getSettingsHelper().exportSettings(tempDir, fileName, getSettingsExportListener(), items, true);
		}
	}

	private int getMaxProgress(List<SettingsItem> items) {
		long maxProgress = 0;
		for (SettingsItem item : items) {
			if (item instanceof FileSettingsItem) {
				maxProgress += ((FileSettingsItem) item).getSize();
			}
		}
		return (int) maxProgress / (1 << 20);
	}

	private String getFileName() {
		if (globalExport) {
			if (exportStartTime == 0) {
				exportStartTime = System.currentTimeMillis();
			}
			return "Export_" + DATE_FORMAT.format(new Date(exportStartTime));
		} else {
			return appMode.toHumanString();
		}
	}

	private void showExportProgressDialog() {
		Context context = getContext();
		if (context == null) {
			return;
		}
		if (progress != null) {
			progress.dismiss();
		}
		progress = new ProgressDialog(context);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setCancelable(true);
		progress.setTitle(app.getString(R.string.shared_string_export));
		progress.setMessage(app.getString(R.string.shared_string_preparing));
		progress.setButton(DialogInterface.BUTTON_NEGATIVE, app.getString(R.string.shared_string_cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				cancelExport();
			}
		});
		progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancelExport();
			}
		});
		progress.show();
	}

	private void cancelExport() {
		app.getSettingsHelper().cancelExportForFile(getExportFile());
		progress.dismiss();
		dismissFragment();
	}

	private SettingsExportListener getSettingsExportListener() {
		if (exportListener == null) {
			exportListener = new SettingsExportListener() {

				@Override
				public void onSettingsExportFinished(@NonNull File file, boolean succeed) {
					dismissExportProgressDialog();
					exportingStarted = false;
					if (succeed) {
						shareProfile(file);
					} else {
						app.showToastMessage(R.string.export_profile_failed);
					}
				}

				@Override
				public void onSettingsExportProgressUpdate(int value) {
					progress.setProgress(value);
				}
			};
		}
		return exportListener;
	}

	private void checkExportingFile() {
		if (exportingStarted) {
			File file = getExportFile();
			boolean fileExporting = app.getSettingsHelper().isFileExporting(file);
			if (fileExporting) {
				showExportProgressDialog();
				progress.setMax(progressMax);
				progress.setProgress(progressValue);
				app.getSettingsHelper().updateExportListener(file, getSettingsExportListener());
			} else if (file.exists()) {
				dismissExportProgressDialog();
				shareProfile(file);
			}
		}
	}

	private void dismissExportProgressDialog() {
		FragmentActivity activity = getActivity();
		if (progress != null && activity != null && AndroidUtils.isActivityNotDestroyed(activity)) {
			progress.dismiss();
		}
	}

	private File getExportFile() {
		File tempDir = FileUtils.getTempDir(app);
		String fileName = getFileName();
		return new File(tempDir, fileName + IndexConstants.OSMAND_SETTINGS_FILE_EXT);
	}

	private void shareProfile(@NonNull File file) {
		try {
			final Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
			sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(getMyApplication(), file));
			sendIntent.setType("*/*");
			sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(sendIntent);
			dismissFragment();
		} catch (Exception e) {
			Toast.makeText(requireContext(), R.string.export_profile_failed, Toast.LENGTH_SHORT).show();
			LOG.error("Share profile error", e);
		}
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, @NonNull ApplicationMode appMode, boolean globalExport) {
		try {
			ExportSettingsFragment fragment = new ExportSettingsFragment();
			fragment.appMode = appMode;
			fragment.globalExport = globalExport;
			fragmentManager.beginTransaction().
					replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(EXPORT_SETTINGS_TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}