package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.quickaction.QuickActionListFragment;
import net.osmand.plus.routepreparationmenu.AvoidRoadsBottomSheetDialogFragment;
import net.osmand.plus.search.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItem;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;

import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.plus.settings.fragments.BaseSettingsListFragment.SETTINGS_LIST_TAG;

public class ImportCompleteFragment extends BaseOsmAndFragment {
	public static final String TAG = ImportCompleteFragment.class.getSimpleName();
	private OsmandApplication app;
	private RecyclerView recyclerView;
	private List<SettingsItem> settingsItems;
	private String fileName;
	private boolean needRestart;
	private boolean nightMode;

	public static void showInstance(FragmentManager fm, @NonNull List<SettingsItem> settingsItems,
									@NonNull String fileName, boolean needRestart) {
		ImportCompleteFragment fragment = new ImportCompleteFragment();
		fragment.setSettingsItems(settingsItems);
		fragment.setFileName(fileName);
		fragment.setRetainInstance(true);
		fragment.setNeedRestart(needRestart);
		fm.beginTransaction()
				.replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(SETTINGS_LIST_TAG)
				.commitAllowingStateLoss();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();
		requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				dismissFragment();
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(app, nightMode);
		View root = inflater.inflate(R.layout.fragment_import_complete, container, false);
		TextView description = root.findViewById(R.id.description);
		TextView btnClose = root.findViewById(R.id.button_close);
		final ViewGroup buttonContainer = root.findViewById(R.id.button_container);
		recyclerView = root.findViewById(R.id.list);
		description.setText(UiUtilities.createSpannableString(
				String.format(getString(R.string.import_complete_description), fileName),
				new StyleSpan(Typeface.BOLD), fileName
		));
		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismissFragment();
			}
		});
		if (needRestart) {
			description.append("\n\n");
			description.append(app.getString(R.string.app_restart_required));
			setupRestartButton(root);
		}
		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(app, root);
		}
		ViewTreeObserver treeObserver = buttonContainer.getViewTreeObserver();
		treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				ViewTreeObserver vts = buttonContainer.getViewTreeObserver();
				int height = buttonContainer.getMeasuredHeight();
				recyclerView.setPadding(0, 0, 0, height);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					vts.removeOnGlobalLayoutListener(this);
				} else {
					vts.removeGlobalOnLayoutListener(this);
				}
			}
		});
		return root;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (settingsItems != null) {
			ImportedSettingsItemsAdapter adapter = new ImportedSettingsItemsAdapter(
					app,
					SettingsHelper.getSettingsToOperate(settingsItems, true),
					nightMode,
					new ImportedSettingsItemsAdapter.OnItemClickListener() {
						@Override
						public void onItemClick(ExportSettingsType type) {
							navigateTo(type);
						}
					});
			recyclerView.setLayoutManager(new LinearLayoutManager(getMyApplication()));
			recyclerView.setAdapter(adapter);
		}
	}

	public void dismissFragment() {
		FragmentManager fm = getFragmentManager();
		if (fm != null && !fm.isStateSaved()) {
			fm.popBackStack(SETTINGS_LIST_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	private void navigateTo(ExportSettingsType type) {
		FragmentManager fm = getFragmentManager();
		Activity activity = requireActivity();
		if (fm == null || fm.isStateSaved()) {
			return;
		}
		dismissFragment();
		fm.popBackStack(DRAWER_SETTINGS_ID + ".new", FragmentManager.POP_BACK_STACK_INCLUSIVE);
		switch (type) {
			case GLOBAL:
			case PROFILE:
			case CUSTOM_ROUTING:
			case ONLINE_ROUTING_ENGINES:
				BaseSettingsFragment.showInstance(requireActivity(), SettingsScreenType.MAIN_SETTINGS);
				break;
			case QUICK_ACTIONS:
				fm.beginTransaction()
						.add(R.id.fragmentContainer, new QuickActionListFragment(), QuickActionListFragment.TAG)
						.addToBackStack(QuickActionListFragment.TAG).commit();
				break;
			case POI_TYPES:
				if (activity instanceof MapActivity) {
					QuickSearchDialogFragment.showInstance(
							(MapActivity) activity,
							"",
							null,
							QuickSearchDialogFragment.QuickSearchType.REGULAR,
							QuickSearchDialogFragment.QuickSearchTab.CATEGORIES,
							null
					);
				}
				break;
			case MAP_SOURCES:
				if (activity instanceof MapActivity) {
					((MapActivity) activity).getDashboard()
							.setDashboardVisibility(
									true,
									DashboardOnMap.DashboardType.CONFIGURE_MAP,
									null
							);
				}
				break;
			case CUSTOM_RENDER_STYLE:
				new SelectMapStyleBottomSheetDialogFragment().show(fm, SelectMapStyleBottomSheetDialogFragment.TAG);
				break;
			case AVOID_ROADS:
				new AvoidRoadsBottomSheetDialogFragment().show(fm, AvoidRoadsBottomSheetDialogFragment.TAG);
				break;
			case TRACKS:
			case OSM_NOTES:
			case OSM_EDITS:
			case FAVORITES:
			case MULTIMEDIA_NOTES:
				int tabId = getFavoritesTabId(type);
				openFavouritesActivity(activity, tabId);
				break;
			case SEARCH_HISTORY:
				if (activity instanceof MapActivity) {
					QuickSearchDialogFragment.showInstance(
							(MapActivity) activity,
							"",
							null,
							QuickSearchDialogFragment.QuickSearchType.REGULAR,
							QuickSearchDialogFragment.QuickSearchTab.HISTORY,
							null
					);
				}
				break;
			default:
				break;
		}
	}

	private void openFavouritesActivity(Activity activity, int tabType) {
		OsmAndAppCustomization appCustomization = app.getAppCustomization();
		Intent favoritesActivity = new Intent(activity, appCustomization.getFavoritesActivity());
		favoritesActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		app.getSettings().FAVORITES_TAB.set(tabType);
		startActivity(favoritesActivity);
	}

	private int getFavoritesTabId(ExportSettingsType type) {
		switch (type) {
			case OSM_NOTES:
			case OSM_EDITS:
				return OsmEditingPlugin.OSM_EDIT_TAB;
			case MULTIMEDIA_NOTES:
				return AudioVideoNotesPlugin.NOTES_TAB;
			case TRACKS:
				return FavoritesActivity.GPX_TAB;
			case FAVORITES:
			default:
				return FavoritesActivity.FAV_TAB;
		}
	}

	private void setupRestartButton(View root) {
		View buttonsDivider = root.findViewById(R.id.buttons_divider);
		View buttonContainer = root.findViewById(R.id.button_restart_container);
		AndroidUiHelper.setVisibility(View.VISIBLE, buttonsDivider, buttonContainer);

		TextView btnRestart = root.findViewById(R.id.button_restart);
		btnRestart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity instanceof MapActivity) {
					MapActivity.doRestart(activity);
				} else {
					android.os.Process.killProcess(android.os.Process.myPid());
				}
			}
		});
	}

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	public void setSettingsItems(List<SettingsItem> settingsItems) {
		this.settingsItems = settingsItems;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setNeedRestart(boolean needRestart) {
		this.needRestart = needRestart;
	}
}
