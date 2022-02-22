package net.osmand.plus.views.mapwidgets.configure.reorder;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ItemType;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ListItem;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.WidgetAdapterListener;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.ButtonViewHolder.ButtonUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder.WidgetUiInfo;
import net.osmand.plus.views.mapwidgets.configure.WidgetItem;
import net.osmand.plus.views.mapwidgets.WidgetsRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReorderWidgetsFragment extends BaseOsmAndFragment implements CopyAppModePrefsListener {

	public static final String TAG = ReorderWidgetsFragment.class.getSimpleName();

	private static final String APP_MODE_ATTR = "app_mode_key";

	private OsmandApplication app;
	private OsmandSettings settings;
	private ApplicationMode appMode;
	private boolean nightMode;

	private View view;
	private Toolbar toolbar;
	private RecyclerView rvContentList;

	private final DataHolder dataHolder = new DataHolder();
	private ReorderWidgetsAdapter adapter;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();

		if (savedInstanceState != null) {
			restoreData(savedInstanceState);
		} else {
			dataHolder.initOrders(appMode, false);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		nightMode = !settings.isLightContent();
		inflater = UiUtilities.getInflater(getContext(), nightMode);

		view = inflater.inflate(R.layout.fragment_reorder_widgets, container, false);
		AndroidUtils.addStatusBarPadding21v(requireContext(), view);

		toolbar = view.findViewById(R.id.toolbar);
		rvContentList = view.findViewById(R.id.content_list);

		setupToolbar();
		setupApplyButton();
		setupContent();

		return view;
	}

	private void setupToolbar() {
		WidgetsPanel selectedGroup = dataHolder.getSelectedPanel();
		String title = getString(selectedGroup.getTitleId());
		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		tvTitle.setText(title);

		String appModeName = appMode.toHumanString();
		TextView tvSubtitle = toolbar.findViewById(R.id.toolbar_subtitle);
		tvSubtitle.setText(appModeName);

		View backBtn = toolbar.findViewById(R.id.back_button);
		backBtn.setOnClickListener(view -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});

		View resetBtn = toolbar.findViewById(R.id.reset_button);
		resetBtn.setOnClickListener(v -> onResetChanges());

		View copyBtn = toolbar.findViewById(R.id.copy_button);
		copyBtn.setOnClickListener(v -> onCopyFromProfile());
	}

	private void setupContent() {
		rvContentList.setLayoutManager(new LinearLayoutManager(app));
		adapter = new ReorderWidgetsAdapter(app, dataHolder, nightMode);

		ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(rvContentList);

		adapter.setListener(new WidgetAdapterListener() {
			private int fromPosition;
			private int toPosition;

			@Override
			public void onDragStarted(ViewHolder holder) {
				fromPosition = holder.getAdapterPosition();
				touchHelper.startDrag(holder);
			}

			@Override
			public void onDragOrSwipeEnded(ViewHolder holder) {
				toPosition = holder.getAdapterPosition();
				if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
					adapter.notifyDataSetChanged();
				}
			}
		});
		rvContentList.setAdapter(adapter);
		updateItems();
	}

	private void setupApplyButton() {
		View applyBtn = view.findViewById(R.id.apply_button);
		UiUtilities.setupDialogButton(nightMode, applyBtn, DialogButtonType.PRIMARY, R.string.shared_string_apply);
		applyBtn.setOnClickListener(v -> onApplyChanges());
	}

	private void onResetChanges() {
		Context context = getContext();
		if (context != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(getString(R.string.reset_to_default));
			builder.setMessage(R.string.are_you_sure);
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
				dataHolder.initOrders(appMode, true);
				updateItems();
			});
			builder.show();
		}
	}

	private void onCopyFromProfile() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fm = activity.getSupportFragmentManager();
			SelectCopyAppModeBottomSheet.showInstance(fm, this, false, appMode);
		}
	}

	private void onApplyChanges() {

	}

	@Override
	public void copyAppModePrefs(ApplicationMode appMode) {

	}

	private void updateItems() {
		List<ListItem> items = new ArrayList<>();
		items.add(new ListItem(ItemType.CARD_DIVIDER, 0));
		items.add(new ListItem(ItemType.HEADER, getString(R.string.shared_string_visible_widgets)));
		items.addAll(createWidgetsList());
		items.add(new ListItem(ItemType.CARD_DIVIDER, 0));
		items.add(new ListItem(ItemType.HEADER, getString(R.string.shared_string_actions)));
		items.add(new ListItem(ItemType.BUTTON, new ButtonUiInfo(
				getString(R.string.reset_to_default),
				R.drawable.ic_action_reset,
				v -> onResetChanges()
		)));
		items.add(new ListItem(ItemType.BUTTON, new ButtonUiInfo(
				getString(R.string.copy_from_other_profile),
				R.drawable.ic_action_copy,
				v -> onCopyFromProfile()
		)));
		items.add(new ListItem(ItemType.CARD_BOTTOM_DIVIDER, 0));
		float bottomSpace = getResources().getDimensionPixelSize(R.dimen.bottom_space_height);
		items.add(new ListItem(ItemType.SPACE, bottomSpace));
		adapter.setItems(items);
	}

	private List<ListItem> createWidgetsList() {
		List<ListItem> list = new ArrayList<>();
		List<WidgetItem> widgets = WidgetsRegister.getSortedWidgets(appMode, dataHolder.getSelectedPanel());
		for (WidgetItem item : widgets) {
			addWidgetToList(list, item);
		}
		Collections.sort(list, (o1, o2) -> {
			int order1 = ((WidgetUiInfo) o1.value).order;
			int order2 = ((WidgetUiInfo) o2.value).order;
			return Integer.compare(order1, order2);
		});
		return list;
	}

	private void addWidgetToList(@NonNull List<ListItem> list,
	                             @NonNull WidgetItem w) {
		String id = w.getTitle();
		Map<String, Integer> orders = dataHolder.getOrders();
		Integer order = orders.get(id);
		if (order == null) {
			order = w.getPriority();
		}
		WidgetUiInfo wi = new WidgetUiInfo();
		wi.title = w.getTitle();
		wi.iconId = w.getIconId();
		wi.isActive = w.isActive();
		wi.order = order;
		list.add(new ListItem(ItemType.WIDGET, wi));
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(APP_MODE_ATTR, appMode.getStringKey());
		dataHolder.onSaveInstanceState(outState);
	}

	private void restoreData(@NonNull Bundle savedInstanceState) {
		String appModeKey = savedInstanceState.getString(APP_MODE_ATTR);
		appMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
		dataHolder.restoreData(savedInstanceState);
	}

	public void setSelectedGroup(WidgetsPanel selectedGroup) {
		dataHolder.setSelectedPanel(selectedGroup);
	}

	public void setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull WidgetsPanel selectedGroup,
	                                @NonNull ApplicationMode appMode) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ReorderWidgetsFragment fragment = new ReorderWidgetsFragment();
			fragment.setSelectedGroup(selectedGroup);
			fragment.setAppMode(appMode);
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

}
