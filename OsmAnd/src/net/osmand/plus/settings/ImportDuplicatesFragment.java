package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SettingsHelper;
import net.osmand.plus.SettingsHelper.SettingsItem;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.view.ComplexButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class ImportDuplicatesFragment extends BaseOsmAndDialogFragment implements View.OnClickListener {

	public static final String TAG = ImportSettingsFragment.class.getSimpleName();
	private OsmandApplication app;
	private RecyclerView list;
	private List<? super Object> duplicatesList;
	private List<SettingsItem> settingsItems;
	private DuplicatesSettingsAdapter adapter;
	private File file;

	public static void showInstance(@NonNull FragmentManager fm, List<? super Object> duplicatesList,
									List<SettingsItem> settingsItems, File file) {
		ImportDuplicatesFragment fragment = new ImportDuplicatesFragment();
		fragment.setDuplicatesList(duplicatesList);
		fragment.setSettingsItems(settingsItems);
		fragment.setFile(file);
		fragment.show(fm, TAG);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_import_duplicates, container, false);
		setupToolbar((Toolbar) root.findViewById(R.id.toolbar));
		ComplexButton replaceAllBtn = root.findViewById(R.id.replace_all_btn);
		ComplexButton keepBothBtn = root.findViewById(R.id.keep_both_btn);
		keepBothBtn.setIcon(getPaintedContentIcon(R.drawable.ic_action_keep_both,
				getSettings().isLightContent()
						? getResources().getColor(R.color.icon_color_active_light)
						: getResources().getColor(R.color.icon_color_active_dark))
		);
		replaceAllBtn.setIcon(getPaintedContentIcon(R.drawable.ic_action_replace,
				getSettings().isLightContent()
						? getResources().getColor(R.color.active_buttons_and_links_text_light)
						: getResources().getColor(R.color.active_buttons_and_links_text_dark))
		);
		keepBothBtn.setOnClickListener(this);
		replaceAllBtn.setOnClickListener(this);
		list = root.findViewById(R.id.list);

		return root;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		adapter = new DuplicatesSettingsAdapter(getMyApplication(), prepareDuplicates(), getSettings().isLightContent());
		list.setLayoutManager(new LinearLayoutManager(getMyApplication()));
		list.setAdapter(adapter);
	}

	private List<Object> prepareDuplicates() {
		List<Object> duplicates = new ArrayList<>();
		List<ApplicationMode> profiles = new ArrayList<>();
		List<QuickAction> actions = new ArrayList<>();

		for (Object object : duplicatesList) {
			if (object instanceof ApplicationMode) {
				profiles.add((ApplicationMode) object);
			} else if (object instanceof QuickAction) {
				actions.add((QuickAction) object);
			}
		}

		if (!profiles.isEmpty()) {
			duplicates.add(getString(R.string.shared_sting_profiles));
			duplicates.addAll(profiles);
		}

		if (!actions.isEmpty()) {
			duplicates.add(getString(R.string.shared_sting_quick_actions));
			duplicates.addAll(actions);
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

	private void importItems(boolean shouldReplace) {
		for (SettingsItem item : settingsItems) {
			item.setShouldReplace(shouldReplace);
		}
		app.getSettingsHelper().importSettings(file, settingsItems, "", 1, new SettingsHelper.SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, boolean empty, @NonNull List<SettingsHelper.SettingsItem> items) {
				if (succeed) {
					app.showShortToastMessage(app.getString(R.string.file_imported_successfully, file.getName()));
				} else if (empty) {
					app.showShortToastMessage(app.getString(R.string.file_import_error, file.getName(), app.getString(R.string.shared_string_unexpected_error)));
				}
			}
		});
		dismiss();
	}

	private void setupToolbar(Toolbar toolbar) {
		toolbar.setNavigationIcon(getPaintedContentIcon(R.drawable.ic_arrow_back,
				getSettings().isLightContent()
						? getResources().getColor(R.color.active_buttons_and_links_text_light)
						: getResources().getColor(R.color.active_buttons_and_links_text_dark)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
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
