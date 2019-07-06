package net.osmand.plus.profiles;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.profiles.ProfileMenuAdapter.ProfileViewHolder;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;


public class ProfileMenuAdapter extends RecyclerView.Adapter<ProfileViewHolder> {

	private static final Log LOG = PlatformUtil.getLog(ProfileMenuAdapter.class);

	private List<Object> items = new ArrayList<>();
	private Set<ApplicationMode> selectedItems;
	@Nullable
	private ProfileMenuAdapterListener listener;
	private final OsmandApplication app;
	@ColorRes
	private int selectedIconColorRes;
	private boolean bottomButton;
	private String bottomButtonText;
	private static final String BUTTON_ITEM = "button_item";

	public ProfileMenuAdapter(List<ApplicationMode> items, Set<ApplicationMode> selectedItems,
		OsmandApplication app, String bottomButtonText) {
		this.items.addAll(items);
		if (bottomButton) {
			this.items.add(BUTTON_ITEM);
		}
		this.app = app;
		this.selectedItems = selectedItems;
		this.bottomButton = !Algorithms.isEmpty(bottomButtonText);
		this.bottomButtonText = bottomButtonText;
		selectedIconColorRes = isNightMode(app)
			? R.color.active_color_primary_dark
			: R.color.active_color_primary_light;
	}

	public List<Object> getItems() {
		return items;
	}

	public void setListener(@Nullable ProfileMenuAdapterListener listener) {
		this.listener = listener;
	}

	public void updateItemsList(List<ApplicationMode> newList, Set<ApplicationMode> selectedItems) {
		this.items.clear();
		this.selectedItems.clear();
		this.items.addAll(newList);
		if (bottomButton) {
			items.add(BUTTON_ITEM);
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
		View itemView =
				LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_list_item, parent, false);
		return new ProfileViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull final ProfileViewHolder holder, int position) {
		Object obj = items.get(position);
		if (obj instanceof ApplicationMode) {
			holder.divider.setVisibility(View.VISIBLE);
			holder.icon.setVisibility(View.VISIBLE);
			holder.descr.setVisibility(View.VISIBLE);
			holder.switcher.setVisibility(View.VISIBLE);
			holder.menuIcon.setVisibility(View.VISIBLE);
			final ApplicationMode item = (ApplicationMode) obj;
			if (bottomButton) {
				holder.divider.setBackgroundColor(isNightMode(app)
					? app.getResources().getColor(R.color.divider_color_dark)
					: app.getResources().getColor(R.color.divider_color_light));
			}
			holder.title.setText(item.toHumanString(app));
			if (item.isCustomProfile()) {
				holder.descr.setText(String.format(app.getString(R.string.profile_type_descr_string),
					Algorithms.capitalizeFirstLetterAndLowercase(item.getParent().toHumanString(app))));
			} else {
				holder.descr.setText(R.string.profile_type_base_string);
			}

			holder.title.setTextColor(app.getResources().getColor(isNightMode(app)
				? R.color.text_color_primary_dark
				: R.color.text_color_primary_light));

			holder.initSwitcher = true;
			holder.switcher.setChecked(selectedItems.contains(item));
			holder.initSwitcher = false;
			updateViewHolder(holder, item);
		} else {
			final String title = (String) obj;
			if (title.equals(BUTTON_ITEM)) {
				holder.divider.setVisibility(View.INVISIBLE);
			}
			holder.icon.setVisibility(View.INVISIBLE);
			holder.descr.setVisibility(View.GONE);
			holder.switcher.setVisibility(View.GONE);
			holder.menuIcon.setVisibility(View.GONE);
			holder.title.setTextColor(app.getResources().getColor(
				isNightMode(app)
				? R.color.active_color_primary_dark
				: R.color.active_color_primary_light));
			holder.title.setText(bottomButtonText);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	private void updateViewHolder(ProfileViewHolder holder, ApplicationMode mode) {
		int iconRes = mode.getIconRes();
		if (iconRes == 0 || iconRes == -1) {
			iconRes = R.drawable.ic_action_world_globe;
		}
		selectedIconColorRes = mode.getIconColorInfo().getColor(isNightMode(app));
		if (selectedItems.contains(mode)) {
			holder.icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, mode.getIconColorInfo().getColor(isNightMode(app))));
		} else {
			holder.icon.setImageDrawable(app.getUIUtilities().getIcon(iconRes, R.color.profile_icon_color_inactive));
		}
	}

	private static boolean isNightMode(OsmandApplication ctx) {
		return !ctx.getSettings().isLightContent();
	}

	class ProfileViewHolder extends RecyclerView.ViewHolder {

		TextView title, descr;
		SwitchCompat switcher;
		ImageView icon, menuIcon;
		LinearLayout profileOptions;
		View divider;

		boolean initSwitcher;

		ProfileViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			descr = itemView.findViewById(R.id.description);
			switcher = itemView.findViewById(R.id.compound_button);
			icon = itemView.findViewById(R.id.icon);
			profileOptions = itemView.findViewById(R.id.profile_settings);
			divider = itemView.findViewById(R.id.divider_bottom);
			menuIcon = itemView.findViewById(R.id.menu_image);

			profileOptions.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					int pos = getAdapterPosition();
					if (pos != RecyclerView.NO_POSITION && listener != null) {
						Object o = items.get(pos);
						if (o instanceof ApplicationMode) {
							listener.onProfilePressed((ApplicationMode) o);
						} else {
							listener.onButtonPressed();
						}
					}
				}
			});
			switcher.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					int pos = getAdapterPosition();
					if (pos != RecyclerView.NO_POSITION && listener != null && !initSwitcher) {
						Object o = items.get(pos);
						if (o instanceof ApplicationMode) {
							final ApplicationMode item = (ApplicationMode) o;
							if (isChecked) {
								selectedItems.add(item);
							} else {
								selectedItems.remove(item);
							}
							updateViewHolder(ProfileViewHolder.this, item);
							listener.onProfileSelected(item, isChecked);
						}
					}
				}
			});
		}
	}

	public interface ProfileMenuAdapterListener {

		void onProfileSelected(ApplicationMode item, boolean selected);

		void onProfilePressed(ApplicationMode item);

		void onButtonPressed();
	}
}
