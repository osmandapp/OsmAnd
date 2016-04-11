package net.osmand.plus;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.AppCompatImageView;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

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


	public ArrayAdapter<ContextMenuItem> createListAdapter(final Activity activity, final boolean lightTheme) {
		final int layoutId = DEFAULT_LAYOUT_ID;
		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		return new ContextMenuArrayAdapter(activity, layoutId, R.id.title,
				items.toArray(new ContextMenuItem[items.size()]), app, lightTheme, changeAppModeListener);
	}

	public class ContextMenuArrayAdapter extends ArrayAdapter<ContextMenuItem> {
		private OsmandApplication app;
		private boolean lightTheme;
		@LayoutRes
		private int layoutId;
		private final ConfigureMapMenu.OnClickListener changeAppModeListener;
		private final IconsCache mIconsCache;

		public ContextMenuArrayAdapter(Activity context,
									   @LayoutRes int layoutRes,
									   @IdRes int textViewResourceId,
									   ContextMenuItem[] objects,
									   OsmandApplication app,
									   boolean lightTheme,
									   ConfigureMapMenu.OnClickListener changeAppModeListener) {
			super(context, layoutRes, textViewResourceId, objects);
			this.app = app;
			this.lightTheme = lightTheme;
			this.layoutId = layoutRes;
			this.changeAppModeListener = changeAppModeListener;
			mIconsCache = app.getIconsCache();
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
				int themeRes = lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
				convertView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), layoutId, null);
				convertView.setTag(layoutId);
			}
			TextView tv = (TextView) convertView.findViewById(R.id.title);
			tv.setText(item.getTitle());

			if (this.layoutId == R.layout.simple_list_menu_item) {
				@ColorRes
				int color = lightTheme ? R.color.icon_color : R.color.dashboard_subheader_text_dark;
				Drawable drawable = mIconsCache.getIcon(item.getIcon(), color);
				float density = getContext().getResources().getDisplayMetrics().density;
				int paddingInPixels = (int) (24 * density);
				int drawableSizeInPixels = (int) (24 * density); // 32
				drawable.setBounds(0, 0, drawableSizeInPixels, drawableSizeInPixels);
				tv.setCompoundDrawables(drawable, null, null, null);
				tv.setCompoundDrawablePadding(paddingInPixels);
			} else {
				if (item.getIcon() != ContextMenuItem.INVALID_ID) {
					int colorRes = item.getColorRes();
					if (colorRes == ContextMenuItem.INVALID_ID) {
						colorRes = lightTheme ? R.color.icon_color : R.color.color_white;
					}
					Drawable drawable = mIconsCache.getIcon(item.getIcon(), colorRes);
					((AppCompatImageView) convertView.findViewById(R.id.icon)).setImageDrawable(drawable);
					convertView.findViewById(R.id.icon).setVisibility(View.VISIBLE);
				} else if (convertView.findViewById(R.id.icon) != null) {
					convertView.findViewById(R.id.icon).setVisibility(View.GONE);
				}
			}
			@DrawableRes
			int secondaryDrawable = item.getSecondaryIcon();
			if (secondaryDrawable != ContextMenuItem.INVALID_ID) {
				@ColorRes
				int colorRes = lightTheme ? R.color.icon_color_light : R.color.dialog_inactive_text_color_dark;
				Drawable drawable = mIconsCache.getIcon(item.getSecondaryIcon(), colorRes);
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
				if (getCount() - 1 == position || getItem(position + 1).isCategory()
						|| item.shouldHideDivider()) {
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