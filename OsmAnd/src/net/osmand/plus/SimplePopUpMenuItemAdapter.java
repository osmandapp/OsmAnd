package net.osmand.plus;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class SimplePopUpMenuItemAdapter
		extends ArrayAdapter<SimplePopUpMenuItemAdapter.SimplePopUpMenuItem> {

	private List<SimplePopUpMenuItem> items;

	public SimplePopUpMenuItemAdapter(@NonNull Context context, int resource,
	                                  List<SimplePopUpMenuItem> items) {
		super(context, resource);
		this.items = items;
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(getContext());
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.popup_menu_item, null);
		}
		SimplePopUpMenuItem item = getItem(position);
		if (item != null) {
			TextView tvTitle = convertView.findViewById(R.id.title);
			tvTitle.setText(item.title);
			ImageView ivIcon = convertView.findViewById(R.id.icon);
			Drawable icon = item.icon;
			if (icon != null) {
				ivIcon.setImageDrawable(icon);
			} else {
				ivIcon.setVisibility(View.GONE);
			}
		}
		return convertView;
	}

	@Nullable
	@Override
	public SimplePopUpMenuItem getItem(int position) {
		return items.get(position);
	}

	public static class SimplePopUpMenuItem {
		private CharSequence title;
		private Drawable icon;

		public SimplePopUpMenuItem(CharSequence title, Drawable icon) {
			this.title = title;
			this.icon = icon;
		}

		public CharSequence getTitle() {
			return title;
		}

		public Drawable getIcon() {
			return icon;
		}
	}
}
