package net.osmand.plus.profiles;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import org.apache.commons.logging.Log;

public class SelectIconBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil.getLog(SelectIconBottomSheetDialogFragment.class);

	private List<IconResWithDescr> icons;
	private IconIdListener listener;
	private IconIdListener listListener;
	private RecyclerView recyclerView;
	private IconIdAdapter adapter;

	public void setIconIdListener(IconIdListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		icons = getProfileIcons();

		Bundle args = getArguments();
		if (args != null) {
			int selectedIcon = args.getInt("selectedIcon", -1);
			if(selectedIcon != -1) {
				for (IconResWithDescr icon : icons) {
					if (icon.resId == selectedIcon) {
						icon.setSelected(true);
					}
				}
			}
		}

		final int themeRes = getMyApplication().getSettings().isLightContent()
			? R.style.OsmandLightTheme
			: R.style.OsmandDarkTheme;

		View view = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
			R.layout.bottom_sheet_select_type_fragment, null);

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		listListener = new IconIdListener() {
			@Override
			public void selectedIconId(int iconRes) {
				listener.selectedIconId(iconRes);
				dismiss();
			}
		};
		recyclerView = view.findViewById(R.id.menu_list_view);
		adapter = new IconIdAdapter(icons, isNightMode(getMyApplication()), listListener);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);

		Button cancelBtn = view.findViewById(R.id.cancel_selection);
		cancelBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		return view;
	}

	private static boolean isNightMode(OsmandApplication ctx) {
		return !ctx.getSettings().isLightContent();
	}

	class IconIdAdapter extends RecyclerView.Adapter<SelectIconBottomSheetDialogFragment.ItemViewHolder> {
		private final List<IconResWithDescr> items;
		private final boolean isNightMode;
		private IconIdListener listener;
		private int previousSelection;

		private IconIdAdapter(@NonNull List<IconResWithDescr> objects,
			@NonNull boolean isNightMode, IconIdListener listener) {
			this.items = objects;
			this.isNightMode = isNightMode;
			this.listener = listener;
		}

		@NonNull
		@Override
		public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(
				R.layout.bottom_sheet_item_with_radio_btn, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {
			final int pos = position;
			final IconResWithDescr item = items.get(position);
			holder.title.setText(item.getTitleId());
			holder.icon.setImageDrawable(getIcon(item.getResId(), isNightMode
				? R.color.active_buttons_and_links_dark
				: R.color.active_buttons_and_links_light));
			if(item.isSelected()) {
				holder.radioButton.setChecked(true);
				previousSelection = position;
			} else {
				holder.radioButton.setChecked(false);
			}
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.selectedIconId(item.resId);
					holder.radioButton.setChecked(true);
					items.get(pos).setSelected(true);
					items.get(previousSelection).setSelected(false);
					notifyItemChanged(previousSelection);
					previousSelection = pos;
				}
			});
		}

		@Override
		public int getItemCount() {
			return items.size();
		}
	}

	class ItemViewHolder extends RecyclerView.ViewHolder {
		TextView title;
		RadioButton radioButton;
		ImageView icon;

		public ItemViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			radioButton = itemView.findViewById(R.id.compound_button);
			icon = itemView.findViewById(R.id.icon);
		}
	}

	interface IconIdListener {
		void selectedIconId(int iconRes);
	}

	public class IconResWithDescr {
		private int resId;
		private int titleId;
		private boolean isSelected;

		public IconResWithDescr(int resId, int titleId, boolean isSelected) {
			this.resId = resId;
			this.titleId = titleId;
			this.isSelected = isSelected;
		}

		public int getResId() {
			return resId;
		}

		public int getTitleId() {
			return titleId;
		}

		public boolean isSelected() {
			return isSelected;
		}

		public void setSelected(boolean selected) {
			isSelected = selected;
		}
	}

	private List<IconResWithDescr> getProfileIcons() {
		List<IconResWithDescr> icons = new ArrayList<>();
		icons.add(new IconResWithDescr(R.drawable.ic_action_car_dark, R.string.rendering_value_car_name,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_bicycle_dark, R.string.rendering_value_bicycle_name,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_pedestrian_dark, R.string.rendering_value_pedestrian_name,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_bus_dark, R.string.app_mode_bus,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_sail_boat_dark, R.string.app_mode_boat,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_aircraft, R.string.app_mode_aircraft,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_truck_dark, R.string.app_mode_truck,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_motorcycle_dark, R.string.app_mode_motorcycle,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_trekking_dark, R.string.app_mode_hiking,false));
		return icons;
	}

}
