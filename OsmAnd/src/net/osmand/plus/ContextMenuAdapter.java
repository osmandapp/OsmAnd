package net.osmand.plus;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
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

import gnu.trove.list.array.TIntArrayList;

public class ContextMenuAdapter {
	private static final Log LOG = PlatformUtil.getLog(ContextMenuAdapter.class);

	private final Context ctx;
	@LayoutRes
	private int defaultLayoutId = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ?
			R.layout.list_menu_item : R.layout.list_menu_item_native;
	final TIntArrayList titleResList = new TIntArrayList();
	final TIntArrayList isCategory = new TIntArrayList();
	final ArrayList<OnContextMenuClick> checkListeners = new ArrayList<>();
	final ArrayList<OnIntegerValueChangedListener> integerListeners = new ArrayList<>();
	final TIntArrayList selectedList = new TIntArrayList();
	final TIntArrayList progressList = new TIntArrayList();
	final TIntArrayList loadingList = new TIntArrayList();
	final TIntArrayList layoutIds = new TIntArrayList();
	final TIntArrayList iconList = new TIntArrayList();
	final TIntArrayList lightIconList = new TIntArrayList();
	final TIntArrayList secondaryLightIconList = new TIntArrayList();
	final ArrayList<String> itemDescription = new ArrayList<>();
	private List<ApplicationMode> visibleModes = new ArrayList<>();
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

	// Related to whole adapter
	public int length() {
		return titleResList.size();
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
		String[] names = new String[titleResList.size()];
		for (int i = 0; i < titleResList.size(); i++) {
			names[i] = activity.getString(titleResList.get(i));
		}
		return new ContextMenuArrayAdapter(activity, layoutId, R.id.title, names, app, holoLight);
	}

	public int[] getTitleResources() {
		return titleResList.toArray();
	}

	// Item related
	@StringRes
	public int getTitleRes(int pos) {
		return titleResList.get(pos);
	}

	public OnContextMenuClick getClickAdapter(int i) {
		return checkListeners.get(i);
	}

	public OnIntegerValueChangedListener getIntegerLister(int i) {
		return integerListeners.get(i);
	}

	public String getItemDescr(int pos) {
		return itemDescription.get(pos);
	}

	public void setItemDescription(int pos, String str) {
		itemDescription.set(pos, str);
	}

	public int getSelection(int pos) {
		return selectedList.get(pos);
	}

	public int getProgress(int pos) {
		return progressList.get(pos);
	}

	public int getLoading(int pos) {
		return loadingList.get(pos);
	}

	public void setSelection(int pos, int s) {
		selectedList.set(pos, s);
	}

	public void setProgress(int pos, int s) {
		progressList.set(pos, s);
	}

	public Drawable getImage(OsmandApplication ctx, int pos, boolean light) {
		int lst = iconList.get(pos);
		if (lst != 0) {
			return ctx.getResources().getDrawable(lst);
		}
		int lstLight = lightIconList.get(pos);
		if (lstLight != 0) {
			return ctx.getIconsCache().getIcon(lstLight, light);
		}
		return null;
	}

	public Drawable getSecondaryImage(OsmandApplication ctx, int pos, boolean light) {
		@DrawableRes
		int secondaryDrawableId = secondaryLightIconList.get(pos);
		if (secondaryDrawableId != 0) {
			return ContextCompat.getDrawable(ctx, secondaryDrawableId);
		}
		return null;
	}

	public boolean isCategory(int pos) {
		return isCategory.get(pos) > 0;
	}

	public void removeItem(int pos) {
		titleResList.removeAt(pos);
		selectedList.removeAt(pos);
		progressList.removeAt(pos);
		iconList.removeAt(pos);
		lightIconList.removeAt(pos);
		secondaryLightIconList.removeAt(pos);
		checkListeners.remove(pos);
		integerListeners.remove(pos);
		isCategory.removeAt(pos);
		layoutIds.removeAt(pos);
		loadingList.removeAt(pos);
	}

	public int getLayoutId(int position) {
		int l = layoutIds.get(position);
		if (l != -1) {
			return l;
		}
		return defaultLayoutId;
	}

	public Item item(String name) {
		Item i = new Item();
		i.title = (name.hashCode() << 4) | titleResList.size();
		i.name = name;
		return i;
	}


	public Item item(@StringRes int resId) {
		Item i = new Item();
		i.title = resId;
		i.name = ctx.getString(resId);
		return i;
	}

	public class Item {
		@DrawableRes
		int icon = 0;
		@DrawableRes
		int secondaryIcon = 0;
		@DrawableRes
		int lightIcon = 0;
		@StringRes
		int title;
		String name;
		int selected = -1;
		int progress = -1;
		@LayoutRes
		int layout = -1;
		int loading = -1;
		boolean cat;
		int pos = -1;
		String description = "";
		private OnContextMenuClick checkBoxListener;
		private OnIntegerValueChangedListener integerListener;

		private Item() {
		}

		public Item icon(@DrawableRes int icon) {
			this.icon = icon;
			return this;
		}


		public Item colorIcon(@DrawableRes int icon) {
			this.lightIcon = icon;
			return this;
		}

		public Item secondaryIconColor(@DrawableRes int icon) {
			this.secondaryIcon = icon;
			return this;
		}

		public Item position(int pos) {
			this.pos = pos;
			return this;
		}

		public Item selected(int selected) {
			this.selected = selected;
			return this;
		}

		public Item progress(int progress) {
			this.progress = progress;
			return this;
		}

		public Item loading(int loading) {
			this.loading = loading;
			return this;
		}

		public Item layout(@LayoutRes int l) {
			this.layout = l;
			return this;
		}

		public Item description(String descr) {
			this.description = descr;
			return this;
		}

		public Item listen(OnContextMenuClick l) {
			this.checkBoxListener = l;
			return this;
		}

		public Item listenInteger(OnIntegerValueChangedListener l) {
			this.integerListener = l;
			return this;
		}

		public void reg() {
			if (pos >= titleResList.size() || pos < 0) {
				pos = titleResList.size();
			}
			titleResList.insert(pos, title);
			itemDescription.add(pos, description);
			selectedList.insert(pos, selected);
			progressList.insert(pos, progress);
			loadingList.insert(pos, loading);
			layoutIds.insert(pos, layout);
			iconList.insert(pos, icon);
			lightIconList.insert(pos, lightIcon);
			secondaryLightIconList.insert(pos, secondaryIcon);
			checkListeners.add(pos, checkBoxListener);
			integerListeners.add(pos, integerListener);
			isCategory.insert(pos, cat ? 1 : 0);
		}

		public Item setCategory(boolean b) {
			cat = b;
			return this;
		}

		public Item name(String name) {
			this.name = name;
			return this;
		}

	}

	public class ContextMenuArrayAdapter extends ArrayAdapter<String> {
		private Activity activity;
		private OsmandApplication app;
		private boolean holoLight;
		private int layoutId;

		public ContextMenuArrayAdapter(Activity context, int resource, int textViewResourceId,
									   String[] names, OsmandApplication app, boolean holoLight) {
			super(context, resource, textViewResourceId, names);
			activity = context;
			this.app = app;
			this.holoLight = holoLight;
			layoutId = resource;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			// User super class to create the View
			Integer lid = getLayoutId(position);
			if (lid == R.layout.mode_toggles) {
				final Set<ApplicationMode> selected = new LinkedHashSet<>();
				return AppModeDialog.prepareAppModeDrawerView(activity, visibleModes, selected, allModes, true, new View.OnClickListener() {
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
				convertView = activity.getLayoutInflater().inflate(lid, parent, false);
//				AndroidUtils.setListItemBackground(ctx, convertView, !holoLight);
				convertView.setTag(lid);
			}
			TextView tv = (TextView) convertView.findViewById(R.id.title);
			if (!isCategory(position)) {
				AndroidUtils.setTextPrimaryColor(ctx, tv, !holoLight);
			}
			tv.setText(isCategory(position) ? getItem(position).toUpperCase() : getItem(position));

			if (layoutId == R.layout.simple_list_menu_item) {
				int color = activity.getResources()
						.getColor(holoLight ? R.color.icon_color : R.color.dashboard_subheader_text_dark);
				Drawable imageId = app.getIconsCache().getPaintedContentIcon(
						lightIconList.get(position), color);
				float density = activity.getResources().getDisplayMetrics().density;
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
			int secondaryLightDrawable = secondaryLightIconList.get(position);
			if (secondaryLightDrawable != 0) {
				int color = ContextCompat.getColor(ctx,
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
				if (selectedList.get(position) != -1) {
					ch.setOnCheckedChangeListener(null);
					ch.setVisibility(View.VISIBLE);
					ch.setChecked(selectedList.get(position) > 0);
					final ArrayAdapter<String> la = this;
					final OnCheckedChangeListener listener = new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							OnContextMenuClick ca = getClickAdapter(position);
							selectedList.set(position, isChecked ? 1 : 0);
							if (ca != null) {
								ca.onContextMenuClick(la, getTitleRes(position), position, isChecked);
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
				if (progressList.get(position) != -1) {
					seekBar.setProgress(getProgress(position));
					seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							OnIntegerValueChangedListener listener = getIntegerLister(position);
							progressList.set(position, progress);
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
				if (loadingList.get(position) == 1) {
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