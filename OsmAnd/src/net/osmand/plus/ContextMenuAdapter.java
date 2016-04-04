package net.osmand.plus;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.dialogs.ConfigureMapMenu;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ContextMenuAdapter {
	private static final Log LOG = PlatformUtil.getLog(ContextMenuAdapter.class);

	@LayoutRes
	private int DEFAULT_LAYOUT_ID = R.layout.list_menu_item_native;
	List<ContextMenuItem> items = new ArrayList<>();
	private ConfigureMapMenu.OnClickListener changeAppModeListener = null;

	public int length() {
		return items.size();
	}

	public String[] getItemNames() {
		String[] itemNames = new String[items.size()];
		for (int i = 0; i < items.size(); i++) {
			itemNames[i] = items.get(i).getTitle();
		}
		return itemNames;
	}

	public void addItem(ContextMenuItem item) {
		items.add(item);
	}

	public ContextMenuItem getItem(int position) {
		return items.get(position);
	}

	public void removeItem(int position) {
		items.remove(position);
	}

	public void setDefaultLayoutId(int defaultLayoutId) {
		this.DEFAULT_LAYOUT_ID = defaultLayoutId;
	}


	public void setChangeAppModeListener(ConfigureMapMenu.OnClickListener changeAppModeListener) {
		this.changeAppModeListener = changeAppModeListener;
	}


	public ArrayAdapter<ContextMenuItem> createListAdapter(final Activity activity, final boolean holoLight) {
		final int layoutId = DEFAULT_LAYOUT_ID;
		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		return new ContextMenuArrayAdapter(activity, layoutId, R.id.title,
				items.toArray(new ContextMenuItem[items.size()]), app, holoLight, changeAppModeListener);
	}

	public class ContextMenuArrayAdapter extends ArrayAdapter<ContextMenuItem> {
		private OsmandApplication app;
		private boolean holoLight;
		@LayoutRes
		private int layoutId;
		private final ConfigureMapMenu.OnClickListener changeAppModeListener;

		public ContextMenuArrayAdapter(Activity context,
									   @LayoutRes
									   int layoutRes,
									   @IdRes
									   int textViewResourceId,
									   ContextMenuItem[] objects,
									   OsmandApplication app,
									   boolean holoLight,
									   ConfigureMapMenu.OnClickListener changeAppModeListener) {
			super(context, layoutRes, textViewResourceId, objects);
			this.app = app;
			this.holoLight = holoLight;
			layoutId = layoutRes;
			this.changeAppModeListener = changeAppModeListener;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			// User super class to create the View
			final ContextMenuItem item = getItem(position);
			int layoutId = item.getLayout();
			layoutId = layoutId != ContextMenuItem.INVALID_ID ? layoutId : DEFAULT_LAYOUT_ID;
			if (layoutId == R.layout.mode_toggles) {
				final Set<ApplicationMode> selected = new LinkedHashSet<>();
				return AppModeDialog.prepareAppModeDrawerView((Activity) getContext(),
						selected, true, new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								if (selected.size() > 0) {
									app.getSettings().APPLICATION_MODE.set(selected.iterator().next());
									notifyDataSetChanged();
								}
								if (changeAppModeListener != null) {
									changeAppModeListener.onClick();
								}
							}
						});
			}
			if (convertView == null || !(convertView.getTag() instanceof Integer)
					|| (layoutId != (Integer) convertView.getTag())) {
				convertView = LayoutInflater.from(getContext()).inflate(layoutId, parent, false);
				convertView.setTag(layoutId);
			}
			TextView tv = (TextView) convertView.findViewById(R.id.title);

			if (item.isCategory()) {
				tv.setTypeface(Typeface.DEFAULT_BOLD);
			} else {
				AndroidUtils.setTextPrimaryColor(getContext(), tv, !holoLight);
				tv.setTypeface(null);
			}
			tv.setText(item.isCategory() ? item.getTitle().toUpperCase() : item.getTitle());

			if (this.layoutId == R.layout.simple_list_menu_item) {
				int color = ContextCompat.getColor(getContext(),
						holoLight ? R.color.icon_color : R.color.dashboard_subheader_text_dark);
				Drawable drawable = ContextCompat.getDrawable(getContext(), item.getIcon());
				Drawable imageId = DrawableCompat.wrap(drawable);
				imageId.mutate();
				DrawableCompat.setTint(imageId, color);
				float density = getContext().getResources().getDisplayMetrics().density;
				int paddingInPixels = (int) (24 * density);
				int drawableSizeInPixels = (int) (24 * density); // 32
				imageId.setBounds(0, 0, drawableSizeInPixels, drawableSizeInPixels);
				tv.setCompoundDrawables(imageId, null, null, null);
				tv.setCompoundDrawablePadding(paddingInPixels);
			} else {
				if (item.getIcon() != ContextMenuItem.INVALID_ID) {
					Drawable drawable = ContextCompat.getDrawable(getContext(), item.getIcon());
					drawable = DrawableCompat.wrap(drawable);
					drawable.mutate();
					DrawableCompat.setTint(drawable, item.getThemedColor(getContext()));
					((AppCompatImageView) convertView.findViewById(R.id.icon)).setImageDrawable(drawable);
					convertView.findViewById(R.id.icon).setVisibility(View.VISIBLE);
				} else if (convertView.findViewById(R.id.icon) != null) {
					convertView.findViewById(R.id.icon).setVisibility(View.GONE);
				}
			}
			@DrawableRes
			int secondaryDrawable = item.getSecondaryIcon();
			if (secondaryDrawable != ContextMenuItem.INVALID_ID) {
				int color = ContextCompat.getColor(getContext(),
						holoLight ? R.color.icon_color : R.color.dashboard_subheader_text_dark);
				Drawable drawable = ContextCompat.getDrawable(getContext(), item.getSecondaryIcon());
				drawable = DrawableCompat.wrap(drawable);
				drawable.mutate();
				DrawableCompat.setTint(drawable, color);
				ImageView imageView = (ImageView) convertView.findViewById(R.id.secondary_icon);
				imageView.setImageDrawable(drawable);
				imageView.setVisibility(View.VISIBLE);
			} else {
				ImageView imageView = (ImageView) convertView.findViewById(R.id.secondary_icon);
				if (imageView != null) {
					imageView.setVisibility(View.GONE);
				}
			}

			if (convertView.findViewById(R.id.toggle_item) != null && !item.isCategory()) {
				final CompoundButton ch = (CompoundButton) convertView.findViewById(R.id.toggle_item);
				if (item.getSelected() != null) {
					ch.setOnCheckedChangeListener(null);
					ch.setVisibility(View.VISIBLE);
					ch.setChecked(item.getSelected());
					final ArrayAdapter<ContextMenuItem> la = this;
					final OnCheckedChangeListener listener = new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							ItemClickListener ca = item.getItemClickListener();
							item.setSelected(isChecked);
							if (ca != null) {
								ca.onContextMenuClick(la, item.getTitleId(), position, isChecked);
							}
						}
					};
					ch.setOnCheckedChangeListener(listener);
					ch.setVisibility(View.VISIBLE);
				} else if (ch != null) {
					ch.setVisibility(View.GONE);
				}
			}

			if (convertView.findViewById(R.id.seekbar) != null) {
				SeekBar seekBar = (SeekBar) convertView.findViewById(R.id.seekbar);
				if (item.getProgress() != ContextMenuItem.INVALID_ID) {
					seekBar.setProgress(item.getProgress());
					seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							OnIntegerValueChangedListener listener = item.getIntegerListener();
							item.setProgress(progress);
							if (listener != null && fromUser) {
								listener.onIntegerValueChangedListener(progress);
							}
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					});
					seekBar.setVisibility(View.VISIBLE);
				} else if (seekBar != null) {
					seekBar.setVisibility(View.GONE);
				}
			}

			if (convertView.findViewById(R.id.ProgressBar) != null) {
				ProgressBar bar = (ProgressBar) convertView.findViewById(R.id.ProgressBar);
				if (item.isLoading()) {
					bar.setVisibility(View.VISIBLE);
				} else {
					bar.setVisibility(View.INVISIBLE);
				}
			}

			View descriptionTextView = convertView.findViewById(R.id.description);
			if (descriptionTextView != null) {
				String itemDescr = item.getDescription();
				if (itemDescr != null) {
					((TextView) descriptionTextView).setText(itemDescr);
					descriptionTextView.setVisibility(View.VISIBLE);
				} else {
					descriptionTextView.setVisibility(View.GONE);
				}
			}

			View dividerView = convertView.findViewById(R.id.divider);
			if (dividerView != null) {
				if (getCount() - 1 == position || getItem(position + 1).isCategory()) {
					dividerView.setVisibility(View.GONE);
				} else {
					dividerView.setVisibility(View.VISIBLE);
				}
			}
			return convertView;
		}

		@Override
		public boolean isEnabled(int position) {
			return !getItem(position).isCategory();
		}
	}

	public interface ItemClickListener {
		//boolean return type needed to desribe if drawer needed to be close or not
		boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked);
	}

	public interface OnIntegerValueChangedListener {
		boolean onIntegerValueChangedListener(int newValue);
	}

	public static abstract class OnRowItemClick implements ItemClickListener {

		//boolean return type needed to describe if drawer needed to be close or not
		public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
			CompoundButton btn = (CompoundButton) view.findViewById(R.id.toggle_item);
			if (btn != null && btn.getVisibility() == View.VISIBLE) {
				btn.setChecked(!btn.isChecked());
				return false;
			} else {
				return onContextMenuClick(adapter, itemId, position, false);
			}
		}
	}
}