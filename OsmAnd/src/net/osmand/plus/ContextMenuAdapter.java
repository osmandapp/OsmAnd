package net.osmand.plus;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatImageView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.activities.HelpActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.dialogs.HelpArticleDialogFragment;
import net.osmand.plus.mapillary.MapillaryAutoCompleteAdapter;

import org.apache.commons.logging.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ContextMenuAdapter {
	private static final Log LOG = PlatformUtil.getLog(ContextMenuAdapter.class);

	@LayoutRes
	private int DEFAULT_LAYOUT_ID = R.layout.list_menu_item_native;
	List<ContextMenuItem> items = new ArrayList<>();
	private ConfigureMapMenu.OnClickListener changeAppModeListener = null;
	private MapActivity mapActivity;

	public ContextMenuAdapter() {

	}

	public ContextMenuAdapter(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

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
		try {
			items.add(item.getPos(), item);
		} catch (IndexOutOfBoundsException ex) {
			items.add(item);
		}
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
		public boolean isEnabled(int position) {
			final ContextMenuItem item = getItem(position);
			if (item != null) {
				return !item.isCategory() && item.isClickable() && item.getLayout() != R.layout.drawer_divider;
			}
			return true;
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
			if (item.getMinHeight() > 0) {
				convertView.setMinimumHeight(item.getMinHeight());
			}
			if (layoutId == R.layout.help_to_improve_item) {
				TextView feedbackButton = (TextView) convertView.findViewById(R.id.feedbackButton);
				Drawable pollIcon = app.getIconsCache().getThemedIcon(R.drawable.ic_action_big_poll);
				feedbackButton.setCompoundDrawablesWithIntrinsicBounds(null, pollIcon, null, null);
				feedbackButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						HelpArticleDialogFragment
								.instantiateWithUrl(HelpActivity.OSMAND_POLL_HTML, app.getString(R.string.feedback))
								.show(((FragmentActivity) getContext()).getSupportFragmentManager(), null);
					}
				});
				TextView contactUsButton = (TextView) convertView.findViewById(R.id.contactUsButton);
				Drawable contactUsIcon =
						app.getIconsCache().getThemedIcon(R.drawable.ic_action_big_feedback);
				contactUsButton.setCompoundDrawablesWithIntrinsicBounds(null, contactUsIcon, null,
						null);
				final String email = app.getString(R.string.support_email);
				contactUsButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(Intent.ACTION_SENDTO);
						intent.setData(Uri.parse("mailto:")); // only email apps should handle this
						intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
						if (intent.resolveActivity(app.getPackageManager()) != null) {
							getContext().startActivity(intent);
						}
					}
				});
				return convertView;
			}

			TextView tv = (TextView) convertView.findViewById(R.id.title);
			if (tv != null) {
				tv.setText(item.getTitle());
			}

			if (this.layoutId == R.layout.simple_list_menu_item) {
				@ColorRes
				int color = lightTheme ? R.color.icon_color : R.color.dashboard_subheader_text_dark;
				Drawable drawable = item.getIcon() != ContextMenuItem.INVALID_ID
						? mIconsCache.getIcon(item.getIcon(), color) : null;
				if (drawable != null && tv != null) {
					float density = getContext().getResources().getDisplayMetrics().density;
					int paddingInPixels = (int) (24 * density);
					int drawableSizeInPixels = (int) (24 * density); // 32
					drawable.setBounds(0, 0, drawableSizeInPixels, drawableSizeInPixels);
					tv.setCompoundDrawables(drawable, null, null, null);
					tv.setCompoundDrawablePadding(paddingInPixels);
				}
			} else {
				if (item.getIcon() != ContextMenuItem.INVALID_ID) {
					int colorRes = item.getColorRes();
					if (colorRes == ContextMenuItem.INVALID_ID) {
						if (!item.shouldSkipPainting()) {
							colorRes = lightTheme ? R.color.icon_color : R.color.color_white;
						} else {
							colorRes = 0;
						}
					}
					final Drawable drawable = mIconsCache.getIcon(item.getIcon(), colorRes);
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
				int colorRes;
				if (secondaryDrawable == R.drawable.ic_action_additional_option) {
					colorRes = lightTheme ? R.color.icon_color_light : R.color.dialog_inactive_text_color_dark;
				} else {
					colorRes = lightTheme ? R.color.icon_color : R.color.color_white;
				}
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

			View progressBar = convertView.findViewById(R.id.ProgressBar);
			if (progressBar != null) {
				ProgressBar bar = (ProgressBar) progressBar;
				if (item.isLoading()) {
					int progress = item.getProgress();
					if (progress == ContextMenuItem.INVALID_ID) {
						bar.setIndeterminate(true);
					} else {
						bar.setIndeterminate(false);
						bar.setProgress(progress);
					}
					bar.setVisibility(View.VISIBLE);
				} else {
					bar.setVisibility(View.GONE);
				}
			}

			View descriptionTextView = convertView.findViewById(R.id.description);
			if (descriptionTextView != null) {
				String itemDescr = item.getDescription();
				if (itemDescr != null && (progressBar == null || !item.isLoading())) {
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

			final View autoCompleteTextView = convertView.findViewById(R.id.auto_complete_text_view);
			if (autoCompleteTextView != null) {
				final AutoCompleteTextView textView = (AutoCompleteTextView) autoCompleteTextView;
				textView.setAdapter(new MapillaryAutoCompleteAdapter(getContext(), R.layout.auto_complete_suggestion, app));

				String selectedUsername = app.getSettings().MAPILLARY_FILTER_USERNAME.get();
				if (!selectedUsername.equals("") && app.getSettings().USE_MAPILLARY_FILTER.get()) {
					textView.setText(selectedUsername);
					textView.setSelection(selectedUsername.length());
				}

				textView.addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

					}

					@Override
					public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        ((View)textView.getParent().getParent()).findViewById(R.id.warning_linear_layout).setVisibility(View.GONE);
					}

					@Override
					public void afterTextChanged(Editable editable) {

					}
				});

				ImageView imageView = (ImageView) ((View) textView.getParent().getParent()).findViewById(R.id.warning_image_view);
				imageView.setImageDrawable(mIconsCache.getPaintedIcon(R.drawable.ic_small_warning,
						app.getResources().getColor(R.color.color_warning)));
			}

			final View dateFromEditText = convertView.findViewById(R.id.date_from_edit_text);
			if (dateFromEditText != null) {
				final EditText dateFrom = (EditText) dateFromEditText;
				final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
				final OsmandSettings settings = app.getSettings();

				final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						Calendar from = Calendar.getInstance();
						from.set(Calendar.YEAR, year);
						from.set(Calendar.MONTH, monthOfYear);
						from.set(Calendar.DAY_OF_MONTH, dayOfMonth);
						dateFrom.setText(dateFormat.format(from.getTime()));
						settings.MAPILLARY_FILTER_FROM_DATE.set(from.getTimeInMillis());
					}
				};

				dateFrom.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Calendar now = Calendar.getInstance();
						new DatePickerDialog(mapActivity, date,
								now.get(Calendar.YEAR),
								now.get(Calendar.MONTH),
								now.get(Calendar.DAY_OF_MONTH)).show();
					}
				});

				if (settings.USE_MAPILLARY_FILTER.get()) {
					long from = settings.MAPILLARY_FILTER_FROM_DATE.get();
					if (from != 0) {
                        dateFrom.setText(dateFormat.format(new Date(from)));
                    }
				}
				dateFrom.setCompoundDrawablesWithIntrinsicBounds(null, null,
						mIconsCache.getThemedIcon(R.drawable.ic_action_arrow_drop_down), null);
			}

			final View dateToEditText = convertView.findViewById(R.id.date_to_edit_text);
			if (dateToEditText != null) {
				final EditText dateTo = (EditText) dateToEditText;
				final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
				final OsmandSettings settings = app.getSettings();

				final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						Calendar to = Calendar.getInstance();
						to.set(Calendar.YEAR, year);
						to.set(Calendar.MONTH, monthOfYear);
						to.set(Calendar.DAY_OF_MONTH, dayOfMonth);
						dateTo.setText(dateFormat.format(to.getTime()));
						settings.MAPILLARY_FILTER_TO_DATE.set(to.getTimeInMillis());
					}
				};

				dateTo.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Calendar now = Calendar.getInstance();
						new DatePickerDialog(mapActivity, date,
								now.get(Calendar.YEAR),
								now.get(Calendar.MONTH),
								now.get(Calendar.DAY_OF_MONTH)).show();
					}
				});

				if (settings.USE_MAPILLARY_FILTER.get()) {
					long to = settings.MAPILLARY_FILTER_TO_DATE.get();
					if (to != 0) {
                        dateTo.setText(dateFormat.format(new Date(to)));
                    }
				}
				dateTo.setCompoundDrawablesWithIntrinsicBounds(null, null,
						mIconsCache.getThemedIcon(R.drawable.ic_action_arrow_drop_down), null);
			}

			final View applyBtn = convertView.findViewById(R.id.button_apply);
			if (applyBtn != null) {
				final Button apply = (Button) applyBtn;
				apply.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						View list = (View) view.getParent().getParent().getParent();
						String username = ((AutoCompleteTextView) list.findViewById(R.id.auto_complete_text_view)).getText().toString();
						String dateFrom = ((EditText) list.findViewById(R.id.date_from_edit_text)).getText().toString();
						String dateTo = ((EditText) list.findViewById(R.id.date_to_edit_text)).getText().toString();
						OsmandSettings settings = app.getSettings();

						if (!settings.MAPILLARY_FILTER_USERNAME.get().equals("") || !dateFrom.equals("") || !dateTo.equals("")) {
							settings.USE_MAPILLARY_FILTER.set(true);
						}
						if (!username.equals("") && settings.MAPILLARY_FILTER_USERNAME.get().equals("")) {
							list.findViewById(R.id.warning_linear_layout).setVisibility(View.VISIBLE);
						}
						if (dateFrom.equals("")) {
							settings.MAPILLARY_FILTER_FROM_DATE.set(0L);
						}
						if (dateTo.equals("")) {
							settings.MAPILLARY_FILTER_TO_DATE.set(0L);
						}
					}
				});
			}

			final View clearBtn = convertView.findViewById(R.id.button_clear);
			if (clearBtn != null) {
				final Button clear = (Button) clearBtn;
				clear.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						View list = (View) view.getParent().getParent().getParent();
						OsmandSettings settings = app.getSettings();

						((AutoCompleteTextView) list.findViewById(R.id.auto_complete_text_view)).setText("");
						((EditText) list.findViewById(R.id.date_from_edit_text)).setText("");
						((EditText) list.findViewById(R.id.date_to_edit_text)).setText("");

						settings.USE_MAPILLARY_FILTER.set(false);
						settings.MAPILLARY_FILTER_USER_KEY.set("");
						settings.MAPILLARY_FILTER_USERNAME.set("");
						settings.MAPILLARY_FILTER_FROM_DATE.set(0L);
						settings.MAPILLARY_FILTER_TO_DATE.set(0L);
					}
				});
			}

			if (item.isCategory()) {
				convertView.setFocusable(false);
				convertView.setClickable(false);
			}

			if (!item.isClickable()) {
				convertView.setFocusable(false);
				convertView.setClickable(false);
			}

			return convertView;
		}
	}

	public interface ItemClickListener {
		//boolean return type needed to desribe if drawer needed to be close or not
		boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter,
								   int itemId,
								   int position,
								   boolean isChecked);
	}

	public interface ProgressListener {
		boolean onProgressChanged(Object progressObject,
								  int progress,
								  ArrayAdapter<ContextMenuItem> adapter,
								  int itemId,
								  int position);
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