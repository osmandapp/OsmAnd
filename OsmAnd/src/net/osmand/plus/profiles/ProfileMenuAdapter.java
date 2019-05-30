package net.osmand.plus.profiles;

import android.content.Intent;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.profiles.ProfileMenuAdapter.ProfileViewHolder;
import net.osmand.util.Algorithms;


public class ProfileMenuAdapter extends RecyclerView.Adapter<ProfileViewHolder> {

	private List<Object> items = new ArrayList<>();
	private Set<ApplicationMode> selectedItems;
	private ProfileListener listener;
	private final OsmandApplication app;
	@ColorRes
	private int selectedIconColorRes;
	private boolean isBottomSheet = false;
	private static final String MANAGE_BTN = "manage_button";

	public ProfileMenuAdapter(List<ApplicationMode> items, Set<ApplicationMode> selectedItems,
		OsmandApplication app, ProfileListener listener) {
		this.items.addAll(items);
		this.listener = listener;
		this.app = app;
		this.selectedItems = selectedItems;
		selectedIconColorRes = isNightMode(app)
			? R.color.active_buttons_and_links_dark
			: R.color.active_buttons_and_links_light;
	}

	public ProfileMenuAdapter(List<ApplicationMode> items, Set<ApplicationMode> selectedItems,
		OsmandApplication app, ProfileListener listener, boolean isBottomSheet) {
		this.items.addAll(items);
		this.items.add(MANAGE_BTN);
		this.listener = listener;
		this.app = app;
		this.selectedItems = selectedItems;
		this.isBottomSheet = isBottomSheet;
		selectedIconColorRes = isNightMode(app)
			? R.color.active_buttons_and_links_dark
			: R.color.active_buttons_and_links_light;
	}

	public List<Object> getItems() {
		return items;
	}

	public void addItem(ApplicationMode profileItem) {
		items.add(profileItem);
		notifyDataSetChanged();
	}

	public void setListener(ProfileListener listener) {
		this.listener = listener;
	}

	public void updateItemsList(List<ApplicationMode> newList, Set<ApplicationMode> selectedItems) {
		items.clear();
		this.selectedItems.clear();
		items.addAll(newList);
		if (isBottomSheet) {
			items.add(MANAGE_BTN);
		}
		this.selectedItems.addAll(selectedItems);
		notifyDataSetChanged();
	}

	@Override
	public int getItemViewType(int position) {
		return super.getItemViewType(position);
	}

	@NonNull
	@Override
	public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View itemView = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.profile_list_item, parent, false);
		return new ProfileViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull final ProfileViewHolder holder, int position) {
		Object obj = items.get(position);

		if (obj instanceof ApplicationMode) {
			final ApplicationMode item = (ApplicationMode) obj;
			if (isBottomSheet) {
				holder.divider.setBackgroundColor(isNightMode(app)
					? app.getResources().getColor(R.color.divider_dark)
					: app.getResources().getColor(R.color.divider_light));
			}

			if (item.getParent() != null) {
				holder.title.setText(item.getUserProfileName());
				holder.descr.setText(String.format(app.getString(R.string.profile_type_descr_string),
					Algorithms.capitalizeFirstLetterAndLowercase(
						item.getParent().getStringKey().replace("_", " "))));
			} else {
				holder.title.setText(app.getResources().getString(item.getStringResource()));
				holder.descr.setText(R.string.profile_type_base_string);
			}

			holder.title.setTextColor(app.getResources().getColor(isNightMode(app)
				? R.color.main_font_dark
				: R.color.main_font_light));

			int iconRes = item.getParent() == null
				? item.getSmallIconDark()
				: ApplicationMode.getIconResFromName(app, item.getIconName(), app.getPackageName());

			if (iconRes == 0 || iconRes == -1) {
				iconRes = R.drawable.ic_action_world_globe;
			}

			final int ficon = iconRes;

			if (selectedItems.contains(item)) {
				holder.aSwitch.setChecked(true);
				holder.icon
					.setImageDrawable(app.getUIUtilities().getIcon(iconRes, selectedIconColorRes));
			} else {
				holder.aSwitch.setChecked(false);
				holder.icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, R.color.icon_color));
			}

			holder.aSwitch.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.changeProfileStatus(item, holder.aSwitch.isChecked());
					if (selectedItems.contains(item)) {
						holder.icon.setImageDrawable(app.getUIUtilities()
							.getIcon(ficon, selectedIconColorRes));
					} else {
						holder.icon.setImageDrawable(
							app.getUIUtilities().getIcon(ficon, R.color.icon_color));
					}
				}
			});
			holder.profileOptions.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.editProfile(item);
				}
			});
		} else {
			final String title = (String) obj;
			if (title.equals("manage_button"))
			holder.divider.setVisibility(View.INVISIBLE);
			holder.icon.setVisibility(View.INVISIBLE);
			holder.descr.setVisibility(View.GONE);
			holder.aSwitch.setVisibility(View.GONE);
			holder.menuIcon.setVisibility(View.GONE);
			holder.title.setTextColor(app.getResources().getColor(selectedIconColorRes));
			holder.title.setText(R.string.shared_string_manage);
			holder.profileOptions.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					app.startActivity(new Intent(app, SettingsProfileActivity.class));
				}
			});
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	private static boolean isNightMode(OsmandApplication ctx) {
		return !ctx.getSettings().isLightContent();
	}

	class ProfileViewHolder extends RecyclerView.ViewHolder {

		TextView title, descr;
		SwitchCompat aSwitch;
		ImageView icon, menuIcon;
		LinearLayout profileOptions;
		View divider;

		ProfileViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			descr = itemView.findViewById(R.id.description);
			aSwitch = itemView.findViewById(R.id.compound_button);
			icon = itemView.findViewById(R.id.icon);
			profileOptions = itemView.findViewById(R.id.profile_settings);
			divider = itemView.findViewById(R.id.divider_bottom);
			menuIcon = itemView.findViewById(R.id.menu_image);
		}
	}

	public interface ProfileListener {

		void changeProfileStatus(ApplicationMode item, boolean isSelected);

		void editProfile(ApplicationMode item);
	}
}

