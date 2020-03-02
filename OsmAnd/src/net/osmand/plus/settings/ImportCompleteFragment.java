package net.osmand.plus.settings;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SettingsHelper.SettingsItem;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment;
import net.osmand.plus.profiles.AdditionalDataWrapper;
import net.osmand.plus.quickaction.QuickActionListFragment;
import net.osmand.plus.routepreparationmenu.AvoidRoadsBottomSheetDialogFragment;
import net.osmand.plus.search.QuickSearchDialogFragment;

import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.plus.settings.ImportSettingsFragment.IMPORT_SETTINGS_TAG;
import static net.osmand.plus.settings.ImportSettingsFragment.getSettingsToOperate;

public class ImportCompleteFragment extends BaseOsmAndFragment {
	public static final String TAG = ImportCompleteFragment.class.getSimpleName();
	private static final String FILE_NAME_KEY = "FILE_NAME_KEY";
	private OsmandApplication app;
	private RecyclerView recyclerView;
	private List<SettingsItem> settingsItems;
	private String fileName;
	private boolean nightMode;

	public static void showInstance(FragmentManager fm, @NonNull List<SettingsItem> settingsItems,
									@NonNull String fileName) {
		ImportCompleteFragment fragment = new ImportCompleteFragment();
		fragment.setSettingsItems(settingsItems);
		fragment.setFileName(fileName);
		fm.beginTransaction()
				.replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(IMPORT_SETTINGS_TAG)
				.commit();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			fileName = savedInstanceState.getString(FILE_NAME_KEY);
		}
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();
		if (settingsItems == null) {
			settingsItems = app.getSettingsHelper().getImportedItems();
			if (settingsItems == null) {
				dismissFragment();
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(app, nightMode);
		View root = inflater.inflate(R.layout.fragment_import_complete, container, false);
		TextView description = root.findViewById(R.id.description);
		TextView btnClose = root.findViewById(R.id.button_close);
		recyclerView = root.findViewById(R.id.list);
		description.setText(UiUtilities.createSpannableString(
				String.format(getString(R.string.import_complete_description), fileName),
				fileName,
				new StyleSpan(Typeface.BOLD)
		));
		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismissFragment();
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
		if (settingsItems != null) {
			ImportedSettingsItemsAdapter adapter = new ImportedSettingsItemsAdapter(
					app,
					getSettingsToOperate(settingsItems),
					nightMode,
					new ImportedSettingsItemsAdapter.OnItemClickListener() {
						@Override
						public void onItemClick(AdditionalDataWrapper.Type type) {
							navigateTo(type);
						}
					});
			recyclerView.setLayoutManager(new LinearLayoutManager(getMyApplication()));
			recyclerView.setAdapter(adapter);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(FILE_NAME_KEY, fileName);
	}

	public void dismissFragment() {
		FragmentManager fm = getFragmentManager();
		if (fm != null) {
			fm.popBackStack(IMPORT_SETTINGS_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
		app.getSettingsHelper().setImportedItems(null);
	}

	private void navigateTo(AdditionalDataWrapper.Type type) {
		FragmentManager fm = getFragmentManager();
		if (fm == null) {
			return;
		}
		dismissFragment();
		fm.popBackStack(DRAWER_SETTINGS_ID + ".new", FragmentManager.POP_BACK_STACK_INCLUSIVE);
		switch (type) {
			case CUSTOM_ROUTING:
			case PROFILE:
				BaseSettingsFragment.showInstance(
						requireActivity(),
						BaseSettingsFragment.SettingsScreenType.MAIN_SETTINGS
				);
				break;
			case QUICK_ACTIONS:
				fm.beginTransaction()
						.add(R.id.fragmentContainer, new QuickActionListFragment(), QuickActionListFragment.TAG)
						.addToBackStack(QuickActionListFragment.TAG).commit();
				break;
			case POI_TYPES:
				new QuickSearchDialogFragment().show(fm, QuickSearchDialogFragment.TAG);
				break;
			case MAP_SOURCES:
				Activity activity = requireActivity();
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
			default:
				break;
		}
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
}
