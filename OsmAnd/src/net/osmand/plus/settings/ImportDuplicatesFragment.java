package net.osmand.plus.settings;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.map.ITileSource;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SettingsHelper;
import net.osmand.plus.SettingsHelper.SettingsItem;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.view.ComplexButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.settings.ImportSettingsFragment.IMPORT_SETTINGS_TAG;


public class ImportDuplicatesFragment extends BaseOsmAndFragment implements View.OnClickListener {

	public static final String TAG = ImportDuplicatesFragment.class.getSimpleName();
	private OsmandApplication app;
	private RecyclerView list;
	private LinearLayout buttonsContainer;
	private NestedScrollView nestedScroll;
	private List<? super Object> duplicatesList;
	private List<SettingsItem> settingsItems;
	private File file;
	private boolean nightMode;
	private ProgressBar progressBar;
	private CollapsingToolbarLayout toolbarLayout;
	private TextView description;

	public static void showInstance(@NonNull FragmentManager fm, List<? super Object> duplicatesList,
									List<SettingsItem> settingsItems, File file) {
		ImportDuplicatesFragment fragment = new ImportDuplicatesFragment();
		fragment.setDuplicatesList(duplicatesList);
		fragment.setSettingsItems(settingsItems);
		fragment.setFile(file);
		fm.beginTransaction()
				.replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(IMPORT_SETTINGS_TAG)
				.commitAllowingStateLoss();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();
		if (settingsItems == null) {
			settingsItems = app.getSettingsHelper().getSelectedItems();
		}
		if (duplicatesList == null) {
			duplicatesList = app.getSettingsHelper().getDuplicatesItems();
		}
		if (file == null) {
			file = app.getSettingsHelper().getSettingsFile();
		}
		SettingsHelper.ImportAsyncTask importAsyncTask = app.getSettingsHelper().getImportTask();
		if (importAsyncTask != null) {
			importAsyncTask.setListener(getImportListener());
		} else if (app.getSettingsHelper().getImportedItems() != null) {
			FragmentManager fm = getFragmentManager();
			if (fm != null) {
				ImportCompleteFragment.showInstance(fm, app.getSettingsHelper().getImportedItems(), file.getName());
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(app, nightMode);
		View root = inflater.inflate(R.layout.fragment_import_duplicates, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		setupToolbar(toolbar);
		ComplexButton replaceAllBtn = root.findViewById(R.id.replace_all_btn);
		ComplexButton keepBothBtn = root.findViewById(R.id.keep_both_btn);
		buttonsContainer = root.findViewById(R.id.buttons_container);
		nestedScroll = root.findViewById(R.id.nested_scroll);
		description = root.findViewById(R.id.description);
		progressBar = root.findViewById(R.id.progress_bar);
		toolbarLayout = root.findViewById(R.id.toolbar_layout);
		keepBothBtn.setIcon(getPaintedContentIcon(R.drawable.ic_action_keep_both,
				nightMode
						? getResources().getColor(R.color.icon_color_active_dark)
						: getResources().getColor(R.color.icon_color_active_light))
		);
		replaceAllBtn.setIcon(getPaintedContentIcon(R.drawable.ic_action_replace,
				nightMode
						? getResources().getColor(R.color.active_buttons_and_links_text_dark)
						: getResources().getColor(R.color.active_buttons_and_links_text_light))
		);
		keepBothBtn.setOnClickListener(this);
		replaceAllBtn.setOnClickListener(this);
		list = root.findViewById(R.id.list);
		ViewTreeObserver treeObserver = buttonsContainer.getViewTreeObserver();
		treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (buttonsContainer != null) {
					ViewTreeObserver vts = buttonsContainer.getViewTreeObserver();
					int height = buttonsContainer.getMeasuredHeight();
					nestedScroll.setPadding(0, 0, 0, height);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						vts.removeOnGlobalLayoutListener(this);
					} else {
						vts.removeGlobalOnLayoutListener(this);
					}
				}
			}
		});
		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(app, root);
		}

		return root;
	}


	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (duplicatesList != null) {
			DuplicatesSettingsAdapter adapter = new DuplicatesSettingsAdapter(app, prepareDuplicates(duplicatesList), nightMode);
			list.setLayoutManager(new LinearLayoutManager(getMyApplication()));
			list.setAdapter(adapter);
		}
		if (app.getSettingsHelper().isImporting() && !app.getSettingsHelper().isCollectOnly()) {
			setupImportingUi();
		} else {
			toolbarLayout.setTitle(getString(R.string.import_duplicates_title));
		}
	}

	private List<Object> prepareDuplicates(List<? super Object> duplicatesList) {
		List<? super Object> duplicates = new ArrayList<>();
		List<ApplicationMode.ApplicationModeBean> profiles = new ArrayList<>();
		List<QuickAction> actions = new ArrayList<>();
		List<PoiUIFilter> filters = new ArrayList<>();
		List<ITileSource> tileSources = new ArrayList<>();
		List<File> renderFilesList = new ArrayList<>();
		List<File> routingFilesList = new ArrayList<>();
		List<AvoidRoadInfo> avoidRoads = new ArrayList<>();

		for (Object object : duplicatesList) {
			if (object instanceof ApplicationMode.ApplicationModeBean) {
				profiles.add((ApplicationMode.ApplicationModeBean) object);
			} else if (object instanceof QuickAction) {
				actions.add((QuickAction) object);
			} else if (object instanceof PoiUIFilter) {
				filters.add((PoiUIFilter) object);
			} else if (object instanceof ITileSource) {
				tileSources.add((ITileSource) object);
			} else if (object instanceof File) {
				File file = (File) object;
				if (file.getAbsolutePath().contains("files/rendering")) {
					renderFilesList.add(file);
				} else if (file.getAbsolutePath().contains("files/routing")) {
					routingFilesList.add(file);
				}
			} else if (object instanceof AvoidRoadInfo) {
				avoidRoads.add((AvoidRoadInfo) object);
			}
		}
		if (!profiles.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_profiles));
			duplicates.addAll(profiles);
		}
		if (!actions.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_quick_actions));
			duplicates.addAll(actions);
		}
		if (!filters.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_poi_types));
			duplicates.addAll(filters);
		}
		if (!tileSources.isEmpty()) {
			duplicates.add(getString(R.string.quick_action_map_source_title));
			duplicates.addAll(tileSources);
		}
		if (!routingFilesList.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_routing));
			duplicates.addAll(routingFilesList);
		}
		if (!renderFilesList.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_rendering_style));
			duplicates.addAll(renderFilesList);
		}
		if (!avoidRoads.isEmpty()) {
			duplicates.add(getString(R.string.avoid_road));
			duplicates.addAll(avoidRoads);
		}
		return duplicates;
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.keep_both_btn: {
				importItems(false);
				break;
			}
			case R.id.replace_all_btn: {
				importItems(true);
				break;
			}
		}
	}

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	private void dismissFragment() {
		FragmentManager fm = getFragmentManager();
		if (fm != null) {
			fm.popBackStack(IMPORT_SETTINGS_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	private void importItems(boolean shouldReplace) {
		if (settingsItems != null && file != null) {
			setupImportingUi();
			for (SettingsItem item : settingsItems) {
				item.setShouldReplace(shouldReplace);
			}
			app.getSettingsHelper().importSettings(file, settingsItems, "", 1, getImportListener());
		}
	}

	private void setupImportingUi() {
		toolbarLayout.setTitle(getString(R.string.shared_string_importing));
		description.setText(UiUtilities.createSpannableString(
				String.format(getString(R.string.importing_from), file.getName()),
				file.getName(),
				new StyleSpan(Typeface.BOLD)
		));
		progressBar.setVisibility(View.VISIBLE);
		list.setVisibility(View.GONE);
		buttonsContainer.setVisibility(View.GONE);
	}

	private SettingsHelper.SettingsImportListener getImportListener() {
		return new SettingsHelper.SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items) {
				if (succeed) {
					app.getRendererRegistry().updateExternalRenderers();
					AppInitializer.loadRoutingFiles(app, new AppInitializer.LoadRoutingFilesCallback() {
						@Override
						public void onRoutingFilesLoaded() {
						}
					});
					FragmentManager fm = getFragmentManager();
					if (fm != null) {
						ImportCompleteFragment.showInstance(fm, items, file.getName());
					}
				} else if (empty) {
					dismissFragment();
					app.showShortToastMessage(app.getString(R.string.file_import_error, file.getName(), app.getString(R.string.shared_string_unexpected_error)));
				}
			}
		};
	}

	private void setupToolbar(Toolbar toolbar) {
		toolbar.setTitle(R.string.import_duplicates_title);
		toolbar.setNavigationIcon(getPaintedContentIcon(R.drawable.ic_arrow_back,
				nightMode
						? getResources().getColor(R.color.active_buttons_and_links_text_dark)
						: getResources().getColor(R.color.active_buttons_and_links_text_light)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fm = getFragmentManager();
				if (fm != null) {
					fm.popBackStackImmediate();
				}
			}
		});
	}

	public void setDuplicatesList(List<? super Object> duplicatesList) {
		this.duplicatesList = duplicatesList;
	}

	public void setSettingsItems(List<SettingsItem> settingsItems) {
		this.settingsItems = settingsItems;
	}

	public void setFile(File file) {
		this.file = file;
	}
}
