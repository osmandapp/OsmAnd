package net.osmand.plus;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
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

	private final Context ctx;
	@LayoutRes
	private int defaultLayoutId = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ?
			R.layout.list_menu_item : R.layout.list_menu_item_native;
	List<ContextMenuItem> items = new ArrayList<>();
	private ConfigureMapMenu.OnClickListener changeAppModeListener = null;
	//neded to detect whether user opened all modes or not
	private BooleanResult allModes = new BooleanResult();

	public ContextMenuAdapter(Context ctx, boolean allModes) {
		this.ctx = ctx;
		this.allModes.setResult(allModes);
	}

	public ContextMenuAdapter(Context ctx) {
		this.ctx = ctx;
	}

	public int length() {
		return items.size();
	}

	public int getElementId(int position) {
		return items.get(position).getTitleId();
	}

	public OnContextMenuClick getClickAdapter(int position) {
		return items.get(position).getCheckBoxListener();
	}

	public OnIntegerValueChangedListener getIntegerLister(int position) {
		return items.get(position).getIntegerListener();
	}

	public String getItemName(int position) {
		return items.get(position).getTitle();
	}

	public String getItemDescr(int position) {
		return items.get(position).getDescription();
	}

	public Boolean getSelection(int position) {
		return items.get(position).getSelected();
	}

	public int getProgress(int position) {
		return items.get(position).getProgress();
	}

	public int getLoading(int position) {
		return items.get(position).isLoading() ? 1 : 0;
	}

	public Drawable getImage(OsmandApplication ctx, int position, boolean light) {
		@DrawableRes
		int lst = items.get(position).getIcon();
		if (lst != -1) {
			return ctx.getResources().getDrawable(lst);
		}
		@DrawableRes
		int lstLight = items.get(position).getLightIcon();
		if (lstLight != -1) {
			return ctx.getIconsCache().getIcon(lstLight, light);
		}
		return null;
	}

	public Drawable getSecondaryImage(OsmandApplication ctx, int position, boolean light) {
		@DrawableRes
		int secondaryDrawableId = items.get(position).getSecondaryLightIcon();
		if (secondaryDrawableId != -1) {
			return ctx.getIconsCache().getIcon(secondaryDrawableId, light);
		}
		return null;
	}

	public int getBackgroundColor(Context ctx, boolean holoLight) {
		if (holoLight) {
			return ctx.getResources().getColor(R.color.bg_color_light);
		} else {
			return ctx.getResources().getColor(R.color.bg_color_dark);
		}
	}

	public boolean isCategory(int pos) {
		return items.get(pos).isCategory();
	}

	public int getLayoutId(int position) {
		int l = items.get(position).getLayout();
		if (l != -1) {
			return l;
		}
		return defaultLayoutId;
	}

	public void setItemName(int position, String str) {
		items.get(position).setTitle(str);
	}

	public void setItemDescription(int position, String str) {
		items.get(position).setDescription(str);
	}

	public void setSelection(int position, boolean s) {
		items.get(position).setSelected(s);
	}

	public void setProgress(int position, int progress) {
		items.get(position).setProgress(progress);
	}

	// Adapter related
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

	public void removeItem(int pos) {
		items.remove(pos);
	}

	public void setDefaultLayoutId(int defaultLayoutId) {
		this.defaultLayoutId = defaultLayoutId;
	}


	public void setChangeAppModeListener(ConfigureMapMenu.OnClickListener changeAppModeListener) {
		this.changeAppModeListener = changeAppModeListener;
	}


	public ArrayAdapter<?> createListAdapter(final Activity activity, final boolean holoLight) {
		final int layoutId = defaultLayoutId;
		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		return new ContextMenuArrayAdapter(activity, layoutId, R.id.title,
				items.toArray(new ContextMenuItem[items.size()]), app, holoLight);
	}

	public class ContextMenuArrayAdapter extends ArrayAdapter<ContextMenuItem> {
		private OsmandApplication app;
		private boolean holoLight;
		private int layoutId;

		public ContextMenuArrayAdapter(Activity context, int resource, int textViewResourceId,
									   ContextMenuItem[] objects, OsmandApplication app, boolean holoLight) {
			super(context, resource, textViewResourceId, objects);
			this.app = app;
			this.holoLight = holoLight;
			layoutId = resource;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			// User super class to create the View
			final ContextMenuItem item = getItem(position);
			Integer lid = getLayoutId(position);
			if (lid == R.layout.mode_toggles) {
				final Set<ApplicationMode> selected = new LinkedHashSet<ApplicationMode>();
				return AppModeDialog.prepareAppModeDrawerView((Activity) getContext(),
						selected, allModes, true, new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (selected.size() > 0) {
							app.getSettings().APPLICATION_MODE.set(selected.iterator().next());
							notifyDataSetChanged();
						}
						if (changeAppModeListener != null) {
							changeAppModeListener.onClick(allModes.getResult());
						}
					}
				});
			}
			if (convertView == null || (!lid.equals(convertView.getTag()))) {
				convertView = LayoutInflater.from(getContext()).inflate(lid, parent, false);
//				AndroidUtils.setListItemBackground(ctx, convertView, !holoLight);
				convertView.setTag(lid);
			}
			TextView tv = (TextView) convertView.findViewById(R.id.title);
			if (!isCategory(position)) {
				AndroidUtils.setTextPrimaryColor(getContext(), tv, !holoLight);
			}
			tv.setText(isCategory(position) ? getItemName(position).toUpperCase() : getItemName(position));

			if (layoutId == R.layout.simple_list_menu_item) {
				int color = ContextCompat.getColor(getContext(),
						holoLight ? R.color.icon_color : R.color.dashboard_subheader_text_dark);
				Drawable imageId = ContextCompat.getDrawable(getContext(), item.getLightIcon());
				DrawableCompat.setTint(imageId, color);
				float density = getContext().getResources().getDisplayMetrics().density;
				int paddingInPixels = (int) (24 * density);
				int drawableSizeInPixels = (int) (24 * density); // 32
				imageId.setBounds(0, 0, drawableSizeInPixels, drawableSizeInPixels);
				tv.setCompoundDrawables(imageId, null, null, null);
				tv.setCompoundDrawablePadding(paddingInPixels);
			} else {
				Drawable drawable = getImage(app, position, holoLight);
				if (drawable != null) {

					((ImageView) convertView.findViewById(R.id.icon)).setImageDrawable(drawable);
					convertView.findViewById(R.id.icon).setVisibility(View.VISIBLE);
				} else if (convertView.findViewById(R.id.icon) != null) {
					convertView.findViewById(R.id.icon).setVisibility(View.GONE);
				}
			}
			@DrawableRes
			int secondaryLightDrawable = item.getSecondaryLightIcon();
			if (secondaryLightDrawable != -1) {
				int color = ContextCompat.getColor(getContext(),
						holoLight ? R.color.icon_color : R.color.dashboard_subheader_text_dark);
				Drawable drawable = getSecondaryImage(app, position, holoLight);
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

			if (isCategory(position)) {
				tv.setTypeface(Typeface.DEFAULT_BOLD);
			} else {
				tv.setTypeface(null);
			}

			if (convertView.findViewById(R.id.toggle_item) != null) {
				final CompoundButton ch = (CompoundButton) convertView.findViewById(R.id.toggle_item);
				if (item.getSelected() != null) {
					ch.setOnCheckedChangeListener(null);
					ch.setVisibility(View.VISIBLE);
					ch.setChecked(item.getSelected());
					final ArrayAdapter<ContextMenuItem> la = this;
					final OnCheckedChangeListener listener = new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							OnContextMenuClick ca = getClickAdapter(position);
							item.setSelected(isChecked);
							if (ca != null) {
								ca.onContextMenuClick(la, getElementId(position), position, isChecked);
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
				if (item.getProgress() != -1) {
					seekBar.setProgress(getProgress(position));
					seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							OnIntegerValueChangedListener listener = getIntegerLister(position);
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

			String itemDescr = getItemDescr(position);
			if (convertView.findViewById(R.id.description) != null) {
				((TextView) convertView.findViewById(R.id.description)).setText(itemDescr);
			}
			return convertView;
		}
	}

	public interface OnContextMenuClick {
		//boolean return type needed to desribe if drawer needed to be close or not
		boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked);
	}

	public interface OnIntegerValueChangedListener {
		boolean onIntegerValueChangedListener(int newValue);
	}

	public static abstract class OnRowItemClick implements OnContextMenuClick {

		public OnRowItemClick() {
		}

		//boolean return type needed to desribe if drawer needed to be close or not
		public boolean onRowItemClick(ArrayAdapter<?> adapter, View view, int itemId, int pos) {
			CompoundButton btn = (CompoundButton) view.findViewById(R.id.toggle_item);
			if (btn != null && btn.getVisibility() == View.VISIBLE) {
				btn.setChecked(!btn.isChecked());
				return false;
			} else {
				return onContextMenuClick(adapter, itemId, pos, false);
			}
		}
	}

	public class BooleanResult {
		private boolean result = false;

		public void setResult(boolean value) {
			result = value;
		}

		public boolean getResult() {
			return result;
		}
	}

}