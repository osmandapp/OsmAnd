package net.osmand.plus.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.poi.RearrangePoiFiltersFragment;
import net.osmand.plus.settings.UiCustomizationRootFragment.ScreenType;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DASHBOARD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.FAVORITES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.POI_OVERLAY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.SHOW_CATEGORY_ID;

public class UiCustomizationFragment extends BaseOsmAndFragment {

	public static final String TAG = UiCustomizationFragment.class.getName();
	private static final String ITEM_TYPE_KEY = "item_type_key";
	private static String ITEMS_ORDER_KEY = "items_order_key";

	private HashMap<String, Integer> itemsOrder = new HashMap<>();

	private List<ListItem> items = new ArrayList<>();

	private OsmandApplication app;
	private boolean nightMode;
	private ScreenType type;
	private LayoutInflater mInflater;


	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(ITEM_TYPE_KEY, type);
	}

	public static UiCustomizationFragment showInstance(@NonNull FragmentManager fm, @NonNull ScreenType type) {
		UiCustomizationFragment fragment = new UiCustomizationFragment();
		fragment.setType(type);
		fm.beginTransaction()
				.replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(null)
				.commitAllowingStateLoss();
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			type = (ScreenType) savedInstanceState.getSerializable(ITEM_TYPE_KEY);
		}
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();
		mInflater = UiUtilities.getInflater(app, nightMode);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = mInflater.inflate(R.layout.edit_arrangement_list_fragment, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		TextView toolbarTitle = root.findViewById(R.id.toolbar_title);
		ImageButton toolbarButton = root.findViewById(R.id.close_button);
		RecyclerView recyclerView = root.findViewById(R.id.profiles_list);
		toolbar.setBackgroundColor(nightMode
				? getResources().getColor(R.color.list_background_color_dark)
				: getResources().getColor(R.color.list_background_color_light));
		toolbarTitle.setTextColor(nightMode
				? getResources().getColor(R.color.text_color_primary_dark)
				: getResources().getColor(R.color.list_background_color_dark));
		toolbarButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_arrow_back, getResources().getColor(R.color.text_color_secondary_light)));
		toolbarTitle.setText(type.titleRes);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showExitDialog();
			}
		});

		recyclerView.setLayoutManager(new LinearLayoutManager(app));

		UiItemAdapter adapter = new UiItemAdapter(getItems(), new UiItemsAdapterListener() {
			@Override
			public void onDragStarted(RecyclerView.ViewHolder holder) {

			}

			@Override
			public void onDragOrSwipeEnded(RecyclerView.ViewHolder holder) {

			}

			@Override
			public void onButtonClicked(int view) {

			}
		});

		recyclerView.setAdapter(adapter);


//		final ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(adapter));
//		touchHelper.attachToRecyclerView(recyclerView);

		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(app, root);
		}
		return root;
	}

	private List<ListItem> getItems() {


		List<ListItem> items = new ArrayList<>();
		items.add(new ListItem(AdapterItemType.DESCRIPTION, type));
		items.add(new ListItem(AdapterItemType.DIVIDER, 1));
		return items;
	}

	private class ListItem {
		AdapterItemType type;
		Object value;

		public ListItem(AdapterItemType type, Object value) {
			this.type = type;
			this.value = value;
		}
	}


	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
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

	private void dismissFragment() {
		FragmentManager fm = getFragmentManager();
		if (fm != null && !fm.isStateSaved()) {
			getFragmentManager().popBackStack();
		}
	}

	public void setType(ScreenType type) {
		this.type = type;
	}

	public interface UiItemsAdapterListener {

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragOrSwipeEnded(RecyclerView.ViewHolder holder);

		void onButtonClicked(int view);
	}

	private enum AdapterItemType {
		DESCRIPTION,
		UI_ITEM,
		MOVE_DIVIDER,
		DIVIDER,
		HEADER,
		BUTTON
	}

	private class UiItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
			implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

		private List<ListItem> items;
		private UiItemsAdapterListener listener;

		public UiItemAdapter(List<ListItem> items, UiItemsAdapterListener listener) {
			this.items = items;
			this.listener = listener;
		}

		@Override
		public int getItemViewType(int position) {
			ListItem listItem = items.get(position);
			return listItem.type.ordinal();
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			AdapterItemType itemType = AdapterItemType.values()[viewType];
			View view;
			switch (itemType) {
				case DESCRIPTION:
					view = mInflater.inflate(R.layout.list_item_description_with_image, parent, false);
					return new DescriptionHolder(view);
				case UI_ITEM:
					view = mInflater.inflate(R.layout.change_order_item, parent, false);
					return new ItemHolder(view);
				case MOVE_DIVIDER:
					view = mInflater.inflate(R.layout.list_item_move_header, parent, false);
					return new MoveDividerHolder(view);
				case DIVIDER:
					view = mInflater.inflate(R.layout.divider, parent, false);
					return new DividerHolder(view);
				case HEADER:
					view = mInflater.inflate(R.layout.preference_category_with_descr, parent, false);
					return new HeaderHolder(view);
				case BUTTON:
					view = mInflater.inflate(R.layout.preference_button, parent, false);
					return new ButtonHolder(view);
				default:
					throw new IllegalArgumentException("Unsupported view type");
			}
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			ListItem item = items.get(position);
			if (holder instanceof DescriptionHolder) {
				DescriptionHolder h = (DescriptionHolder) holder;
				ScreenType screenType = (ScreenType) item.value;
				h.description.setText(String.format(getString(R.string.reorder_or_hide_from), getString(screenType.titleRes)));
				h.image.setImageResource(nightMode ? screenType.imageNightRes : screenType.imageDayRes);
			} else if (holder instanceof ItemHolder) {
				ItemHolder h = (ItemHolder) holder;
			} else if (holder instanceof HeaderHolder) {
				HeaderHolder h = (HeaderHolder) holder;
			} else if (holder instanceof MoveDividerHolder) {
				MoveDividerHolder h = (MoveDividerHolder) holder;
			} else if (holder instanceof ButtonHolder) {
				ButtonHolder h = (ButtonHolder) holder;
			}
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		@Override
		public boolean onItemMove(int from, int to) {
			return false;
		}

		@Override
		public void onItemDismiss(RecyclerView.ViewHolder holder) {
			listener.onDragOrSwipeEnded(holder);
		}

		private class DescriptionHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {
			private ImageView image;
			private TextView description;

			public DescriptionHolder(@NonNull View itemView) {
				super(itemView);
				image = itemView.findViewById(R.id.image);
				description = itemView.findViewById(R.id.description);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class ItemHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {

			private TextView title;
			private TextView description;
			private ImageView icon;
			private ImageView actionIcon;
			private ImageView moveIcon;
			private View itemsContainer;

			public ItemHolder(@NonNull View itemView) {
				super(itemView);
				title = itemView.findViewById(R.id.title);
				actionIcon = itemView.findViewById(R.id.action_icon);
				icon = itemView.findViewById(R.id.icon);
				moveIcon = itemView.findViewById(R.id.move_icon);
				itemsContainer = itemView.findViewById(R.id.selectable_list_item);
			}

			@Override
			public boolean isMovingDisabled() {
//				int position = getAdapterPosition();
//				if (position != RecyclerView.NO_POSITION) {
//					RearrangePoiFiltersFragment.ListItem item = items.get(position);
//					if (item.value instanceof RearrangePoiFiltersFragment.PoiUIFilterDataObject) {
//						RearrangePoiFiltersFragment.PoiUIFilterDataObject pdo = (RearrangePoiFiltersFragment.PoiUIFilterDataObject) item.value;
//						return !pdo.isActive;
//					}
//				}
				return true;
			}
		}

		private class MoveDividerHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {

			public MoveDividerHolder(@NonNull View itemView) {
				super(itemView);
			}

			@Override
			public boolean isMovingDisabled() {
				return false;
			}
		}

		private class DividerHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {
			View divider;

			public DividerHolder(View itemView) {
				super(itemView);
				divider = itemView.findViewById(R.id.divider);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class HeaderHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {
			private TextView title;
			private TextView description;

			public HeaderHolder(@NonNull View itemView) {
				super(itemView);
				title = itemView.findViewById(android.R.id.title);
				description = itemView.findViewById(android.R.id.summary);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class ButtonHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {
			private View button;
			private ImageView icon;
			private TextView title;

			public ButtonHolder(@NonNull View itemView) {
				super(itemView);
				button = itemView;
				icon = itemView.findViewById(android.R.id.icon);
				title = itemView.findViewById(android.R.id.title);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}
	}

	private List<UiItemBase> getDrawerItems() {
		List<UiItemBase> list = new ArrayList<>();
		list.add(new UiItemBase(FAVORITES_ID, R.string.shared_string_favorites, R.string.shared_string_favorites, R.drawable.ic_action_fav_dark, 0));
		list.add(new UiItemBase(POI_OVERLAY_ID, R.string.layer_poi, R.string.layer_poi, R.drawable.ic_action_info_dark, 1));
		return list;
	}


	private class UiItemBase {
		private String id;
		private int titleRes;
		private int descrRes;
		private int iconRes;
		private int order;

		public UiItemBase(String id, int titleRes, int descrRes, int iconRes, int order) {
			this.id = id;
			this.titleRes = titleRes;
			this.descrRes = descrRes;
			this.iconRes = iconRes;
			this.order = order;
		}

		public String getId() {
			return id;
		}

		public int getTitleRes() {
			return titleRes;
		}

		public int getDescrRes() {
			return descrRes;
		}

		public int getIconRes() {
			return iconRes;
		}

		public int getOrder() {
			return order;
		}
	}
}
