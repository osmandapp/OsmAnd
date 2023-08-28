package net.osmand.plus.profiles;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class EditProfilesFragment extends BaseOsmAndFragment {

	private static final String DELETED_APP_MODES_KEY = "deleted_app_modes_key";
	private static final String APP_MODES_ORDER_KEY = "app_modes_order_key";

	private final List<Object> items = new ArrayList<>();
	private HashMap<String, Integer> appModesOrders = new HashMap<>();
	private ArrayList<String> deletedModesKeys = new ArrayList<>();

	private EditProfilesAdapter adapter;

	private boolean wasDrawerDisabled;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		if (savedInstanceState != null && savedInstanceState.containsKey(APP_MODES_ORDER_KEY) && savedInstanceState.containsKey(DELETED_APP_MODES_KEY)) {
			appModesOrders = (HashMap<String, Integer>) AndroidUtils.getSerializable(savedInstanceState, APP_MODES_ORDER_KEY, HashMap.class);
			deletedModesKeys = savedInstanceState.getStringArrayList(DELETED_APP_MODES_KEY);
		} else {
			for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
				appModesOrders.put(mode.getStringKey(), mode.getOrder());
			}
		}
		View mainView = themedInflater.inflate(R.layout.edit_arrangement_list_fragment, container, false);

		AppBarLayout appbar = mainView.findViewById(R.id.appbar);
		View toolbar = themedInflater.inflate(R.layout.global_preference_toolbar, container, false);
		appbar.addView(toolbar);

		ImageButton closeButton = mainView.findViewById(R.id.close_button);
		closeButton.setImageResource(R.drawable.ic_action_remove_dark);
		closeButton.setOnClickListener(v -> {
			FragmentActivity fragmentActivity = getActivity();
			if (fragmentActivity != null) {
				fragmentActivity.onBackPressed();
			}
		});

		TextView toolbarTitle = mainView.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.edit_profiles);

		RecyclerView recyclerView = mainView.findViewById(R.id.profiles_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

		adapter = new EditProfilesAdapter(app);
		updateItems();

		ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(adapter));

		touchHelper.attachToRecyclerView(recyclerView);
		adapter.setAdapterListener(new ProfilesAdapterListener() {

			private int fromPosition;
			private int toPosition;

			@Override
			public void onDragStarted(RecyclerView.ViewHolder holder) {
				fromPosition = holder.getAdapterPosition();
				touchHelper.startDrag(holder);
			}

			@Override
			public void onDragOrSwipeEnded(RecyclerView.ViewHolder holder) {
				toPosition = holder.getAdapterPosition();
				if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
					adapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onButtonClicked(int pos) {
				Object item = adapter.getItem(pos);
				if (item instanceof EditProfileDataObject) {
					EditProfileDataObject profileDataObject = (EditProfileDataObject) item;
					profileDataObject.toggleDeleted();
					if (profileDataObject.deleted) {
						deletedModesKeys.add(profileDataObject.getStringKey());
					} else {
						deletedModesKeys.remove(profileDataObject.getStringKey());
					}
					updateItems();
				}
			}
		});

		recyclerView.setAdapter(adapter);

		DialogButton cancelButton = mainView.findViewById(R.id.dismiss_button);
		cancelButton.setButtonType(DialogButtonType.SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_cancel);
		cancelButton.setOnClickListener(v -> {
			FragmentActivity fragmentActivity = getActivity();
			if (fragmentActivity != null) {
				fragmentActivity.onBackPressed();
			}
		});

		mainView.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);

		DialogButton applyButton = mainView.findViewById(R.id.right_bottom_button);
		applyButton.setButtonType(DialogButtonType.PRIMARY);
		applyButton.setTitleId(R.string.shared_string_apply);
		applyButton.setVisibility(View.VISIBLE);
		applyButton.setOnClickListener(v -> {
			MapActivity mapActivity = (MapActivity) getActivity();
			if (mapActivity != null) {
				OsmandApplication app = mapActivity.getMyApplication();

				if (!deletedModesKeys.isEmpty()) {
					List<ApplicationMode> deletedModes = new ArrayList<>();
					for (String modeKey : deletedModesKeys) {
						ApplicationMode mode = ApplicationMode.valueOfStringKey(modeKey, null);
						if (mode != null) {
							deletedModes.add(mode);
						}
					}
					ApplicationMode.deleteCustomModes(deletedModes, app);
				}
				for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
					String modeKey = mode.getStringKey();
					Integer order = appModesOrders.get(modeKey);
					if (order == null) {
						order = mode.getOrder();
					}
					mode.setOrder(order);
				}
				ApplicationMode.reorderAppModes();
				mapActivity.onBackPressed();
			}
		});

		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), mainView);
		}

		return mainView;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(APP_MODES_ORDER_KEY, appModesOrders);
		outState.putStringArrayList(DELETED_APP_MODES_KEY, deletedModesKeys);
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
		}
	}

	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !wasDrawerDisabled) {
			mapActivity.enableDrawer();
		}
	}

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	public List<EditProfileDataObject> getProfiles(boolean deleted) {
		List<EditProfileDataObject> profiles = new ArrayList<>();
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			String modeKey = mode.getStringKey();
			if (deleted && deletedModesKeys.contains(modeKey) || !deleted && !deletedModesKeys.contains(modeKey)) {
				Integer order = appModesOrders.get(modeKey);
				if (order == null) {
					order = mode.getOrder();
				}
				profiles.add(new EditProfileDataObject(modeKey, mode.toHumanString(), ProfileDataUtils.getAppModeDescription(getContext(), mode),
						mode.getIconRes(), false, mode.isCustomProfile(), deleted, mode.getProfileColor(false), mode.getProfileColor(true), order));
			}
		}
		Collections.sort(profiles, new Comparator<EditProfileDataObject>() {
			@Override
			public int compare(EditProfileDataObject o1, EditProfileDataObject o2) {
				return (o1.order < o2.order) ? -1 : ((o1.order == o2.order) ? 0 : 1);
			}
		});

		return profiles;
	}

	private void updateItems() {
		List<EditProfileDataObject> activeObjects = getProfiles(false);
		List<EditProfileDataObject> deletedObjects = getProfiles(true);

		items.clear();
		items.add(getString(R.string.edit_profiles_descr));
		items.addAll(activeObjects);
		items.add(new SpannableString(getString(R.string.shared_string_deleted)));
		items.add(getString(R.string.delete_profiles_descr));
		items.addAll(deletedObjects);

		adapter.setItems(items);
	}

	public class EditProfileDataObject extends ProfileDataObject {

		private int order;
		private boolean deleted;
		private final boolean customProfile;

		EditProfileDataObject(String stringKey, String name, String descr, int iconRes, boolean isSelected,
							  boolean customProfile, boolean deleted, @ColorInt int iconColorLight, @ColorInt int iconColorDark, int order) {
			super(name, descr, stringKey, iconRes, isSelected, iconColorLight, iconColorDark);
			this.customProfile = customProfile;
			this.deleted = deleted;
			this.order = order;
		}

		public boolean isDeleted() {
			return deleted;
		}

		public boolean isCustomProfile() {
			return customProfile;
		}

		public int getOrder() {
			return order;
		}

		public void setOrder(int order) {
			this.order = order;
		}

		public void toggleDeleted() {
			this.deleted = !deleted;
		}
	}

	private class EditProfilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
			implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

		private static final int INFO_TYPE = 0;
		private static final int PROFILE_EDIT_TYPE = 1;
		private static final int CATEGORY_TYPE = 3;

		private final OsmandApplication app;
		private final UiUtilities uiUtilities;

		private List<Object> items = new ArrayList<>();
		private ProfilesAdapterListener listener;

		private final boolean nightMode;

		EditProfilesAdapter(OsmandApplication app) {
			setHasStableIds(true);
			this.app = app;
			uiUtilities = app.getUIUtilities();
			nightMode = !app.getSettings().isLightContent();
		}

		public void setItems(List<Object> items) {
			this.items = items;
			notifyDataSetChanged();
		}

		public void setAdapterListener(ProfilesAdapterListener listener) {
			this.listener = listener;
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
			LayoutInflater inflater = UiUtilities.getInflater(viewGroup.getContext(), nightMode);
			if (viewType == INFO_TYPE) {
				View itemView = inflater.inflate(R.layout.bottom_sheet_item_description_long, viewGroup, false);
				return new InfoViewHolder(itemView);
			} else if (viewType == PROFILE_EDIT_TYPE) {
				View itemView = inflater.inflate(R.layout.profile_edit_list_item, viewGroup, false);
				return new ProfileViewHolder(itemView);
			} else if (viewType == CATEGORY_TYPE) {
				View itemView = inflater.inflate(R.layout.simple_category_item, viewGroup, false);
				return new CategoryViewHolder(itemView);
			} else {
				throw new IllegalArgumentException("Unsupported view type");
			}
		}

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
			if (holder instanceof ProfileViewHolder) {
				ProfileViewHolder profileViewHolder = (ProfileViewHolder) holder;
				EditProfileDataObject mode = (EditProfileDataObject) items.get(pos);

				profileViewHolder.title.setText(mode.getName());
				profileViewHolder.description.setText(mode.getDescription());

				int iconRes = mode.getIconRes();
				if (iconRes == 0 || iconRes == -1) {
					iconRes = R.drawable.ic_action_world_globe;
				}
				int colorNoAlpha = mode.getIconColor(nightMode);

				profileViewHolder.icon.setImageDrawable(uiUtilities.getPaintedIcon(iconRes, colorNoAlpha));

				//set up cell color
				Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, colorNoAlpha, 0.3f);
				AndroidUtils.setBackground(profileViewHolder.itemsContainer, drawable);

				profileViewHolder.actionIcon.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int pos = holder.getAdapterPosition();
						if (mode.isCustomProfile() && pos != RecyclerView.NO_POSITION) {
							listener.onButtonClicked(pos);
						}
					}
				});
				profileViewHolder.moveButton.setVisibility(mode.isDeleted() ? View.GONE : View.VISIBLE);
				if (!mode.isDeleted()) {
					int removeIconColor = mode.isCustomProfile() ? R.color.color_osm_edit_delete : R.color.icon_color_default_light;
					profileViewHolder.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_remove, removeIconColor));
					profileViewHolder.moveButton.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View view, MotionEvent event) {
							if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
								listener.onDragStarted(holder);
							}
							return false;
						}
					});
				} else {
					profileViewHolder.actionIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_undo, R.color.color_osm_edit_create));
				}
			} else if (holder instanceof InfoViewHolder) {
				InfoViewHolder infoViewHolder = (InfoViewHolder) holder;
				String text = (String) items.get(pos);
				infoViewHolder.description.setText(text);
			} else if (holder instanceof CategoryViewHolder) {
				CategoryViewHolder infoViewHolder = (CategoryViewHolder) holder;
				SpannableString text = (SpannableString) items.get(pos);
				infoViewHolder.title.setText(text);
			}
		}

		@Override
		public int getItemViewType(int position) {
			Object item = items.get(position);
			if (item instanceof EditProfileDataObject) {
				return PROFILE_EDIT_TYPE;
			} else if (item instanceof String) {
				return INFO_TYPE;
			} else if (item instanceof SpannableString) {
				return CATEGORY_TYPE;
			} else {
				throw new IllegalArgumentException("Unsupported view type");
			}
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		@Override
		public boolean onItemMove(int from, int to) {
			Object itemFrom = getItem(from);
			Object itemTo = getItem(to);
			if (itemFrom instanceof EditProfileDataObject && itemTo instanceof EditProfileDataObject) {
				EditProfileDataObject profileFrom = (EditProfileDataObject) itemFrom;
				EditProfileDataObject profileTo = (EditProfileDataObject) itemTo;

				int orderFrom = profileFrom.getOrder();
				int orderTo = profileTo.getOrder();

				profileFrom.setOrder(orderTo);
				profileTo.setOrder(orderFrom);
				appModesOrders.put(profileFrom.getStringKey(), orderTo);
				appModesOrders.put(profileTo.getStringKey(), orderFrom);

				Collections.swap(items, from, to);
				notifyItemMoved(from, to);
				return true;
			}
			return false;
		}

		@Override
		public long getItemId(int position) {
			Object item = items.get(position);
			if (item instanceof EditProfileDataObject) {
				return ((EditProfileDataObject) item).getStringKey().hashCode();
			}
			return item.hashCode();
		}

		public Object getItem(int position) {
			return items.get(position);
		}

		@Override
		public void onItemDismiss(@NonNull ViewHolder holder) {
			listener.onDragOrSwipeEnded(holder);
		}

		private class ProfileViewHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {

			TextView title;
			TextView description;
			ImageView icon;
			ImageButton actionIcon;
			ImageView moveIcon;
			View itemsContainer;
			FrameLayout moveButton;

			ProfileViewHolder(View itemView) {
				super(itemView);
				title = itemView.findViewById(R.id.title);
				description = itemView.findViewById(R.id.description);
				actionIcon = itemView.findViewById(R.id.action_icon);
				icon = itemView.findViewById(R.id.icon);
				moveIcon = itemView.findViewById(R.id.move_icon);
				moveButton = itemView.findViewById(R.id.move_button);
				itemsContainer = itemView.findViewById(R.id.selectable_list_item);
			}

			@Override
			public boolean isMovingDisabled() {
				int position = getAdapterPosition();
				if (position != RecyclerView.NO_POSITION) {
					Object item = items.get(position);
					if (item instanceof EditProfileDataObject) {
						return ((EditProfileDataObject) item).isDeleted();
					}
				}
				return false;
			}
		}

		private class InfoViewHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {

			private final TextView description;

			InfoViewHolder(View itemView) {
				super(itemView);
				description = itemView.findViewById(R.id.description);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}

		private class CategoryViewHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {

			private final TextView title;

			CategoryViewHolder(View itemView) {
				super(itemView);
				title = itemView.findViewById(R.id.title);
			}

			@Override
			public boolean isMovingDisabled() {
				return true;
			}
		}
	}

	public interface ProfilesAdapterListener {

		void onDragStarted(RecyclerView.ViewHolder holder);

		void onDragOrSwipeEnded(RecyclerView.ViewHolder holder);

		void onButtonClicked(int view);
	}
}