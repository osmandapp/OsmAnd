package net.osmand.plus.profiles;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.profiles.ProfileMenuAdapter.ProfileViewHolder;
import net.sf.junidecode.App;


public class ProfileMenuAdapter extends RecyclerView.Adapter<ProfileViewHolder> {

	private List<ApplicationMode> items;
	private Set<ApplicationMode> selectedItems;
	private ProfileListener listener = null;
	private final OsmandApplication app;

	public ProfileMenuAdapter(List<ApplicationMode> items, Set<ApplicationMode> selectedItems, OsmandApplication app, ProfileListener listener) {
		this.items = items;
		this.listener = listener;
		this.app = app;
		this.selectedItems = selectedItems;
	}

	public List<ApplicationMode> getItems() {
		return items;
	}

	public void addItem(ApplicationMode profileItem) {
		items.add(profileItem);
		notifyDataSetChanged();
	}

	public void updateItemsList(List<ApplicationMode> newList) {
		items.clear();
		items.addAll(newList);
		notifyDataSetChanged();
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
		final ApplicationMode item = items.get(position);
		if (item.getParent() != null) {
			holder.descr.setText(String.format("Type: %s", item.getParent().getStringKey()));
		} else {
			holder.title.setText(app.getResources().getString(item.getStringResource()));
			holder.descr.setText(String.format("Type: %s", item.getStringKey()));
		}

		holder.title.setTextColor(app.getResources().getColor(isNightMode(app) ? R.color.main_font_dark : R.color.main_font_light));
		holder.icon.setImageDrawable(app.getUIUtilities().getIcon(item.getSmallIconDark(), isNightMode(app) ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light));
		holder.aSwitch.setChecked(selectedItems.contains(item));
		holder.aSwitch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.changeProfileStatus(item, holder.aSwitch.isChecked());
			}
		});
		holder.profileOptions.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.editProfile(item);
			}
		});
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
		ImageView icon, profileOptions;

		ProfileViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.profile_title);
			descr = itemView.findViewById(R.id.profile_descr);
			aSwitch = itemView.findViewById(R.id.profile_switch);
			icon = itemView.findViewById(R.id.profile_icon);
			profileOptions = itemView.findViewById(R.id.profile_settings);
		}
	}

	public interface ProfileListener {
		void changeProfileStatus(ApplicationMode item, boolean isSelected);
		void editProfile(ApplicationMode item);
	}
}

