package net.osmand.plus.settings;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
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
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment;
import net.osmand.plus.profiles.AdditionalDataWrapper;
import net.osmand.plus.quickaction.QuickActionListFragment;
import net.osmand.plus.routepreparationmenu.AvoidRoadsBottomSheetDialogFragment;
import net.osmand.plus.search.QuickSearchDialogFragment;

import java.util.List;

import static net.osmand.plus.settings.ImportSettingsFragment.IMPORT_SETTINGS_TAG;
import static net.osmand.plus.settings.ImportSettingsFragment.getSettingsToOperate;

public class ImportCompleteFragment extends BaseOsmAndFragment {
	public static final String TAG = ImportCompleteFragment.class.getSimpleName();
	private static final String FILE_NAME_KEY = "FILE_NAME_KEY";
	private List<SettingsItem> settingsItems;
	private String fileName;
	private OsmandApplication app;
	private boolean nightMode;
	private RecyclerView recyclerView;

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
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(app, nightMode);
		View root = inflater.inflate(R.layout.fragment_import_complete, container, false);
		TextView description = root.findViewById(R.id.description);
		description.setText(UiUtilities.createSpannableString(
				String.format(getString(R.string.import_complete_description), fileName),
				fileName,
				new StyleSpan(Typeface.BOLD)
		));

		TextView btnClose = root.findViewById(R.id.button_close);
		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismissFragment();
			}
		});
		recyclerView = root.findViewById(R.id.list);
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
			getFragmentManager().popBackStack(IMPORT_SETTINGS_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	private void navigateTo(AdditionalDataWrapper.Type type) {
		FragmentManager fm = getFragmentManager();
		if (fm == null) {
			return;
		}
		Fragment fragment;
		String TAG;
		switch (type) {
			case PROFILE:
				BaseSettingsFragment.showInstance(
						getActivity(),
						BaseSettingsFragment.SettingsScreenType.MAIN_SETTINGS
				);
				break;
			case QUICK_ACTIONS:
				fm.beginTransaction()
						.add(R.id.fragmentContainer, new QuickActionListFragment(), QuickActionListFragment.TAG)
						.addToBackStack(QuickActionListFragment.TAG).commitAllowingStateLoss();
				break;
			case POI_TYPES:
				QuickSearchDialogFragment quickSearchDialogFragment = new QuickSearchDialogFragment();
				quickSearchDialogFragment.show(fm, QuickSearchDialogFragment.TAG);
				break;
			case MAP_SOURCES:

				break;
			case CUSTOM_RENDER_STYLE:

				fragment = new SelectMapStyleBottomSheetDialogFragment();
				TAG = SelectMapStyleBottomSheetDialogFragment.TAG;
				break;
			case CUSTOM_ROUTING:

				return;
			case AVOID_ROADS:
				fragment = new AvoidRoadsBottomSheetDialogFragment();
				TAG = AvoidRoadsBottomSheetDialogFragment.TAG;
				break;
			default:
				return;
		}
		dismissFragment();
		app.getSettingsHelper().setImportedItems(null);
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
