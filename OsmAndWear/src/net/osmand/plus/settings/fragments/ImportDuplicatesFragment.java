package net.osmand.plus.settings.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportType;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;
import net.osmand.view.ComplexButton;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public abstract class ImportDuplicatesFragment extends BaseOsmAndFragment {

	protected List<SettingsItem> settingsItems;
	protected List<? super Object> duplicatesList;

	protected RecyclerView list;
	protected TextView description;
	protected ProgressBar progressBar;
	protected LinearLayout buttonsContainer;
	protected NestedScrollView nestedScroll;
	protected CollapsingToolbarLayout toolbarLayout;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	public void setSettingsItems(List<SettingsItem> settingsItems) {
		this.settingsItems = settingsItems;
	}

	public void setDuplicatesList(List<? super Object> duplicatesList) {
		this.duplicatesList = duplicatesList;
	}

	protected abstract ImportType getImportTaskType();


	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View root = themedInflater.inflate(R.layout.fragment_import_duplicates, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		setupToolbar(toolbar);
		ComplexButton replaceAllBtn = root.findViewById(R.id.replace_all_btn);
		ComplexButton keepBothBtn = root.findViewById(R.id.keep_both_btn);
		buttonsContainer = root.findViewById(R.id.buttons_container);
		nestedScroll = root.findViewById(R.id.nested_scroll);
		description = root.findViewById(R.id.description);
		progressBar = root.findViewById(R.id.progress_bar);
		toolbarLayout = root.findViewById(R.id.toolbar_layout);
		keepBothBtn.setIcon(getPaintedContentIcon(R.drawable.ic_action_keep_both, nightMode
				? getColor(R.color.icon_color_active_dark)
				: getColor(R.color.icon_color_active_light))
		);
		replaceAllBtn.setIcon(getPaintedContentIcon(R.drawable.ic_action_replace,
				ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode))
		);
		keepBothBtn.setOnClickListener(v -> importItems(false));
		replaceAllBtn.setOnClickListener(v -> importItems(true));
		list = root.findViewById(R.id.list);
		ViewCompat.setNestedScrollingEnabled(list, false);
		ViewTreeObserver treeObserver = buttonsContainer.getViewTreeObserver();
		treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (buttonsContainer != null) {
					ViewTreeObserver vts = buttonsContainer.getViewTreeObserver();
					int height = buttonsContainer.getMeasuredHeight();
					nestedScroll.setPadding(0, 0, 0, height);
					vts.removeOnGlobalLayoutListener(this);
				}
			}
		});
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), root);

		return root;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (duplicatesList != null) {
			DuplicatesSettingsAdapter adapter = new DuplicatesSettingsAdapter(app, prepareDuplicates(duplicatesList), nightMode);
			list.setLayoutManager(new LinearLayoutManager(app));
			list.setAdapter(adapter);
		}
		if (getImportTaskType() == ImportType.IMPORT) {
			setupImportingUi();
		} else {
			toolbarLayout.setTitle(getString(R.string.import_duplicates_title));
		}
		toolbarLayout.setTitle(getString(R.string.import_duplicates_title));
	}

	protected void importItems(boolean shouldReplace) {
		if (settingsItems != null) {
			setupImportingUi();
			for (SettingsItem item : settingsItems) {
				item.setShouldReplace(shouldReplace);
			}
		}
	}

	protected void setupImportingUi() {
		list.setVisibility(View.GONE);
		progressBar.setVisibility(View.VISIBLE);
		buttonsContainer.setVisibility(View.GONE);
	}

	protected void setupToolbar(Toolbar toolbar) {
		toolbar.setTitle(R.string.import_duplicates_title);
		toolbar.setNavigationIcon(getPaintedContentIcon(
				AndroidUtils.getNavigationIconResId(app),
				ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> {
			FragmentManager fm = getFragmentManager();
			if (fm != null && !fm.isStateSaved()) {
				fm.popBackStackImmediate();
			}
		});
	}

	@NonNull
	protected List<Object> prepareDuplicates(List<? super Object> duplicatesList) {
		List<Object> duplicates = new ArrayList<>();
		Map<ExportType, List<Object>> dataMap = new EnumMap<>(ExportType.class);
		for (Object object : duplicatesList) {
			ExportType exportType = ExportType.findBy(app, object);
			if (exportType != null) {
				List<Object> data = dataMap.get(exportType);
				if (data == null) {
					data = new ArrayList<>();
					dataMap.put(exportType, data);
				}
				data.add(object);
			}
		}
		for (ExportType exportType : dataMap.keySet()) {
			List<Object> data = dataMap.get(exportType);
			if (!Algorithms.isEmpty(data)) {
				duplicates.add(exportType.getTitle(app));
				duplicates.addAll(data);
			}
		}
		return duplicates;
	}
}
