package net.osmand.plus.mapcontextmenu.other;

import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidRoadsRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.GpxLocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.InterruptMusicRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameterGroup;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.MuteSoundRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.OtherSettingsRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.VoiceGuidanceRoutingParameter;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.CtxMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.router.GeneralRouter;

import java.io.File;
import java.util.List;

public class RoutePreferencesMenu {

	private final OsmandSettings settings;
	private final OsmandApplication app;
	private final MapActivity mapActivity;
	private final RoutingHelper routingHelper;
	private final RoutingOptionsHelper routingOptionsHelper;
	private ArrayAdapter<LocalRoutingParameter> listAdapter;

	public static final String MORE_VALUE = "MORE_VALUE";

	public RoutePreferencesMenu(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		app = mapActivity.getMyApplication();
		routingHelper = mapActivity.getRoutingHelper();
		routingOptionsHelper = app.getRoutingOptionsHelper();
		settings = app.getSettings();
	}

	private void doSelectVoiceGuidance() {
		routingOptionsHelper.selectVoiceGuidance(mapActivity, new CallbackWithObject<String>() {
			@Override
			public boolean processResult(String result) {
				routingOptionsHelper.applyVoiceProvider(mapActivity, result, false);
				updateParameters();
				return true;
			}
		}, routingHelper.getAppMode());
	}

	public OnItemClickListener getItemClickListener(ArrayAdapter<?> listAdapter) {
		return new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int item, long l) {
				Object obj = listAdapter.getItem(item);
				if (obj instanceof LocalRoutingParameterGroup) {
					LocalRoutingParameterGroup group = (LocalRoutingParameterGroup) obj;
					ContextMenuAdapter adapter = new ContextMenuAdapter(app);
					int i = 0;
					int selectedIndex = -1;
					for (LocalRoutingParameter p : group.getRoutingParameters()) {
						adapter.addItem(new ContextMenuItem(null)
								.setTitle(p.getText(mapActivity))
								.setSelected(false));
						if (p.isSelected(settings)) {
							selectedIndex = i;
						}
						i++;
					}
					if (selectedIndex == -1) {
						selectedIndex = 0;
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
					final int layout = R.layout.list_menu_item_native_singlechoice;

					List<String> names = CtxMenuUtils.getNames(adapter.getItems());
					ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(mapActivity, layout, R.id.text1, names) {
						@NonNull
						@Override
						public View getView(int position, View convertView, ViewGroup parent) {
							// User super class to create the View
							View v = convertView;
							if (v == null) {
								v = mapActivity.getLayoutInflater().inflate(layout, null);
							}
							ContextMenuItem item = adapter.getItem(position);
							TextView tv = v.findViewById(R.id.text1);
							tv.setText(item.getTitle());
							tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);

							return v;
						}
					};

					int[] selectedPosition = {selectedIndex};
					builder.setSingleChoiceItems(listAdapter, selectedIndex, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int position) {
							selectedPosition[0] = position;
						}
					});
					builder.setTitle(group.getText(mapActivity))
							.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {

									int position = selectedPosition[0];
									if (position >= 0 && position < group.getRoutingParameters().size()) {
										for (int i = 0; i < group.getRoutingParameters().size(); i++) {
											LocalRoutingParameter rp = group.getRoutingParameters().get(i);
											rp.setSelected(settings, i == position);
										}
										mapActivity.getRoutingHelper().onSettingsChanged(true);
										updateParameters();
									}
								}
							})
							.setNegativeButton(R.string.shared_string_cancel, null);

					builder.create().show();
				} else if (obj instanceof MuteSoundRoutingParameter) {
					CompoundButton btn = view.findViewById(R.id.toggle_item);
					btn.performClick();
				} else if (obj instanceof VoiceGuidanceRoutingParameter) {
					doSelectVoiceGuidance();
				} else if (obj instanceof InterruptMusicRoutingParameter) {
					CompoundButton btn = view.findViewById(R.id.toggle_item);
					btn.performClick();
				} else if (obj instanceof AvoidRoadsRoutingParameter) {
					routingOptionsHelper.selectRestrictedRoads(mapActivity);
				} else if (obj instanceof GpxLocalRoutingParameter) {
					showOptionsMenu(view.findViewById(R.id.description));
				} else {
					CheckBox ch = view.findViewById(R.id.toggle_item);
					if (ch != null) {
						ch.setChecked(!ch.isChecked());
					}
				}
			}
		};
	}

	public ArrayAdapter<LocalRoutingParameter> getRoutePreferencesDrawerAdapter(boolean nightMode) {

		listAdapter = new ArrayAdapter<LocalRoutingParameter>(mapActivity, R.layout.layers_list_activity_item, R.id.title,
				routingOptionsHelper.getAllRoutingParameters(routingHelper.getAppMode()/*settings.APPLICATION_MODE.get()*/)) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				LocalRoutingParameter parameter = getItem(position);
				if (parameter instanceof MuteSoundRoutingParameter) {
					View v = mapActivity.getLayoutInflater().inflate(R.layout.switch_select_list_item, null);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
					v.findViewById(R.id.description_text).setVisibility(View.GONE);
					v.findViewById(R.id.select_button).setVisibility(View.GONE);
					((ImageView) v.findViewById(R.id.icon))
							.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_volume_up, !nightMode));
					CompoundButton btn = v.findViewById(R.id.toggle_item);
					btn.setVisibility(View.VISIBLE);
					btn.setChecked(!routingHelper.getVoiceRouter().isMute());
					btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							routingOptionsHelper.switchSound();
						}
					});

					TextView tv = v.findViewById(R.id.header_text);
					AndroidUtils.setTextPrimaryColor(mapActivity, tv, nightMode);
					tv.setText(getString(R.string.shared_string_sound));
					return v;
				}
				if (parameter instanceof AvoidRoadsRoutingParameter) {
					View v = mapActivity.getLayoutInflater().inflate(R.layout.switch_select_list_item, null);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
					((ImageView) v.findViewById(R.id.icon))
							.setImageDrawable(app.getUIUtilities().getIcon(parameter.getActiveIconId(), !nightMode));
					v.findViewById(R.id.toggle_item).setVisibility(View.GONE);
					TextView btn = v.findViewById(R.id.select_button);
					btn.setTextColor(btn.getLinkTextColors());
					btn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							routingOptionsHelper.selectRestrictedRoads(mapActivity);
						}
					});

					TextView tv = v.findViewById(R.id.header_text);
					AndroidUtils.setTextPrimaryColor(mapActivity, tv, nightMode);
					tv.setText(getString(R.string.impassable_road));

					TextView tvDesc = v.findViewById(R.id.description_text);
					AndroidUtils.setTextSecondaryColor(mapActivity, tvDesc, nightMode);
					tvDesc.setText(getString(R.string.impassable_road_desc));

					return v;
				}
				if (parameter instanceof VoiceGuidanceRoutingParameter) {
					View v = mapActivity.getLayoutInflater().inflate(R.layout.switch_select_list_item, null);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
					v.findViewById(R.id.icon).setVisibility(View.GONE);
					v.findViewById(R.id.description_text).setVisibility(View.GONE);
					v.findViewById(R.id.toggle_item).setVisibility(View.GONE);
					TextView btn = v.findViewById(R.id.select_button);
					btn.setTextColor(btn.getLinkTextColors());
					String voiceProvider = settings.VOICE_PROVIDER.get();
					String voiceProviderStr;
					if (voiceProvider != null) {
						if (OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(voiceProvider)) {
							voiceProviderStr = getString(R.string.shared_string_do_not_use);
						} else {
							voiceProviderStr = FileNameTranslationHelper.getVoiceName(mapActivity, voiceProvider);
						}
						voiceProviderStr += voiceProvider.contains("tts") ? " TTS" : "";
					} else {
						voiceProviderStr = getString(R.string.shared_string_not_selected);
					}
					btn.setText(voiceProviderStr);
					btn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							doSelectVoiceGuidance();
						}
					});

					TextView tv = v.findViewById(R.id.header_text);
					AndroidUtils.setTextPrimaryColor(mapActivity, tv, nightMode);
					tv.setText(getString(R.string.voice_provider));

					return v;
				}
				if (parameter instanceof InterruptMusicRoutingParameter) {
					View v = mapActivity.getLayoutInflater().inflate(R.layout.switch_select_list_item, null);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
					v.findViewById(R.id.select_button).setVisibility(View.GONE);
					v.findViewById(R.id.icon).setVisibility(View.GONE);
					CompoundButton btn = v.findViewById(R.id.toggle_item);
					btn.setVisibility(View.VISIBLE);
					btn.setChecked(settings.INTERRUPT_MUSIC.get());
					btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							routingOptionsHelper.switchMusic();
						}
					});

					TextView tv = v.findViewById(R.id.header_text);
					AndroidUtils.setTextPrimaryColor(mapActivity, tv, nightMode);
					tv.setText(getString(R.string.interrupt_music));
					TextView tvDesc = v.findViewById(R.id.description_text);
					AndroidUtils.setTextSecondaryColor(mapActivity, tvDesc, nightMode);
					tvDesc.setText(getString(R.string.interrupt_music_descr));

					return v;
				}
				if (parameter instanceof GpxLocalRoutingParameter) {
					View v = mapActivity.getLayoutInflater().inflate(R.layout.plan_route_gpx, null);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
					AndroidUtils.setTextPrimaryColor(mapActivity, v.findViewById(R.id.title), nightMode);
					TextView gpxSpinner = v.findViewById(R.id.description);
					AndroidUtils.setTextPrimaryColor(mapActivity, gpxSpinner, nightMode);
					((ImageView) v.findViewById(R.id.dropDownIcon))
							.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_drop_down, !nightMode));
					updateSpinnerItems(gpxSpinner);
					return v;
				}
				if (parameter instanceof OtherSettingsRoutingParameter) {
					View v = mapActivity.getLayoutInflater().inflate(R.layout.layers_list_activity_item, null);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
					ImageView icon = v.findViewById(R.id.icon);
					icon.setImageDrawable(app.getUIUtilities().getIcon(parameter.getActiveIconId(), !nightMode));
					icon.setVisibility(View.VISIBLE);
					TextView titleView = v.findViewById(R.id.title);
					titleView.setText(R.string.routing_settings_2);
					AndroidUtils.setTextPrimaryColor(mapActivity, titleView, nightMode);
					v.findViewById(R.id.toggle_item).setVisibility(View.GONE);
					return v;
				}
				return inflateRoutingParameter(position);
			}

			private View inflateRoutingParameter(int position) {
				View v = mapActivity.getLayoutInflater().inflate(R.layout.layers_list_activity_item, null);
				AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
				TextView tv = v.findViewById(R.id.title);
				TextView desc = v.findViewById(R.id.description);
				CheckBox ch = v.findViewById(R.id.toggle_item);
				LocalRoutingParameter rp = getItem(position);
				AndroidUtils.setTextPrimaryColor(mapActivity, tv, nightMode);
				tv.setText(rp.getText(mapActivity));
				ch.setOnCheckedChangeListener(null);
				if (rp instanceof LocalRoutingParameterGroup) {
					LocalRoutingParameterGroup group = (LocalRoutingParameterGroup) rp;
					AndroidUtils.setTextPrimaryColor(mapActivity, desc, nightMode);
					LocalRoutingParameter selected = group.getSelected(settings);
					if (selected != null) {
						desc.setText(selected.getText(mapActivity));
						desc.setVisibility(View.VISIBLE);
					}
					ch.setVisibility(View.GONE);
				} else {
					if (rp.routingParameter != null && rp.routingParameter.getId().equals(GeneralRouter.USE_SHORTEST_WAY)) {
						// if short route settings - it should be inverse of fast_route_mode
						ch.setChecked(!settings.FAST_ROUTE_MODE.getModeValue(routingHelper.getAppMode()));
					} else {
						ch.setChecked(rp.isSelected(settings));
					}
					ch.setVisibility(View.VISIBLE);
					ch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							routingOptionsHelper.applyRoutingParameter(rp, isChecked);
						}
					});
				}
				return v;
			}
		};

		return listAdapter;
	}

	private void updateParameters() {
		//ApplicationMode am = settings.APPLICATION_MODE.get();
		ApplicationMode am = routingHelper.getAppMode();
		listAdapter.setNotifyOnChange(false);
		listAdapter.clear();
		for (LocalRoutingParameter r : routingOptionsHelper.getAllRoutingParameters(am)) {
			listAdapter.add(r);
		}
		listAdapter.notifyDataSetChanged();
	}

	protected void openGPXFileSelection(TextView gpxSpinner) {
		GpxUiHelper.selectGPXFile(mapActivity, false, false, new CallbackWithObject<GPXUtilities.GPXFile[]>() {

			@Override
			public boolean processResult(GPXUtilities.GPXFile[] result) {
				mapActivity.getMapActions().setGPXRouteParams(result[0]);
				app.getTargetPointsHelper().updateRouteAndRefresh(true);
				updateSpinnerItems(gpxSpinner);
				updateParameters();
				mapActivity.getRoutingHelper().onSettingsChanged(true);
				return true;
			}
		}, app.getDaynightHelper().isNightModeForMapControls());
	}

	private void updateSpinnerItems(TextView gpxSpinner) {
		GPXRouteParamsBuilder rp = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		gpxSpinner.setText(rp == null ? mapActivity.getString(R.string.shared_string_none) :
				new File(rp.getFile().path).getName());
	}

	private void showOptionsMenu(TextView gpxSpinner) {
		GPXRouteParamsBuilder rp = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		PopupMenu optionsMenu = new PopupMenu(gpxSpinner.getContext(), gpxSpinner);
		MenuItem item = optionsMenu.getMenu().add(
				mapActivity.getString(R.string.shared_string_none));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (mapActivity.getRoutingHelper().getCurrentGPXRoute() != null) {
					mapActivity.getRoutingHelper().setGpxParams(null);
					settings.FOLLOW_THE_GPX_ROUTE.set(null);
					mapActivity.getRoutingHelper().onSettingsChanged(true);
				}
				updateParameters();
				return true;
			}
		});
		item = optionsMenu.getMenu().add(mapActivity.getString(R.string.select_gpx));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				openGPXFileSelection(gpxSpinner);
				return true;
			}
		});
		if (rp != null) {
			item = optionsMenu.getMenu().add(new File(rp.getFile().path).getName());
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					// nothing to change
					return true;
				}
			});
		}
		optionsMenu.show();
	}

	private String getString(int id) {
		return mapActivity.getString(id);
	}
}
