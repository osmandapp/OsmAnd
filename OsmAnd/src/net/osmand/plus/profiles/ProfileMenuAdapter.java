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
import java.util.List;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.profiles.ProfileMenuAdapter.ProfileViewHolder;
import net.osmand.plus.profiles.SettingsProfileActivity.ProfileItem;

public class ProfileMenuAdapter extends RecyclerView.Adapter<ProfileViewHolder> {

	private List<ProfileItem> items;
	OsmandApplication app;

	public ProfileMenuAdapter(List<ProfileItem> items, OsmandApplication app) {
		this.items = items;
		this.app = app;
	}

	public List<ProfileItem> getItems() {
		return items;
	}

	public void addItem(ProfileItem profileItem) {
		items.add(profileItem);
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
	public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
		ProfileItem item = items.get(position);
		holder.title.setText(item.getTitle());
		holder.descr.setText(item.getDescr());
		Drawable drawable = app.getUIUtilities().getThemedIcon(item.getIconRes());
		holder.icon.setImageDrawable(drawable);
		holder.aSwitch.setChecked(item.getState());
		holder.aSwitch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//todo change profile state;
			}
		});
		holder.profileOptions.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//todo open profile settings;
			}
		});
	}

	@Override
	public int getItemCount() {
		return items.size();
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
}
