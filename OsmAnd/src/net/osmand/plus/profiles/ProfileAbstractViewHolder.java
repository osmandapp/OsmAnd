package net.osmand.plus.profiles;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.R;

public abstract class ProfileAbstractViewHolder extends RecyclerView.ViewHolder {
	TextView title, descr;
	CompoundButton compoundButton;
	ImageView icon, menuIcon;
	LinearLayout profileOptions;
	View dividerBottom;
	View dividerUp;
	
	public ProfileAbstractViewHolder(View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		descr = itemView.findViewById(R.id.description);
		compoundButton = itemView.findViewById(R.id.compound_button);
		icon = itemView.findViewById(R.id.icon);
		profileOptions = itemView.findViewById(R.id.profile_settings);
		dividerBottom = itemView.findViewById(R.id.divider_bottom);
		dividerUp = itemView.findViewById(R.id.divider_up);
		menuIcon = itemView.findViewById(R.id.menu_image);
	}
}
