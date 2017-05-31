package net.osmand.plus.myplaces;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.RotatedTileBox.RotatedTileBoxBuilder;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.Route;
import net.osmand.plus.GPXUtilities.Track;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.dialogs.ConfigureMapMenu.AppearanceListItem;
import net.osmand.plus.dialogs.ConfigureMapMenu.GpxAppearanceAdapter;
import net.osmand.plus.dialogs.ConfigureMapMenu.GpxAppearanceAdapter.GpxAppearanceAdapterType;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.AsyncLoadingThread;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.Renderable;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.PagerSlidingTabStrip.CustomTabProvider;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.plus.views.controls.WrapContentHeightViewPager.ViewAtPositionInterface;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;

import static net.osmand.plus.dialogs.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;

public class TrackSegmentFragment extends OsmAndListFragment {

	public static final String ARG_TO_FILTER_SHORT_TRACKS = "ARG_TO_FILTER_SHORT_TRACKS";

	private OsmandApplication app;
	private SegmentGPXAdapter adapter;

	private GpxDisplayItemType[] filterTypes = { GpxSelectionHelper.GpxDisplayItemType.TRACK_SEGMENT };
	private List<String> options = new ArrayList<>();
	private List<Double> distanceSplit = new ArrayList<>();
	private TIntArrayList timeSplit = new TIntArrayList();
	private int selectedSplitInterval;
	private boolean updateEnable;
	private View headerView;
	private int trackColor;
	private int currentTrackColor;
	private Paint paint;
	private Bitmap selectedPoint;
	private LatLon selectedPointLatLon;
	private int defPointColor;
	private Paint paintIcon;
	private Bitmap pointSmall;

	private ImageView imageView;
	private RotatedTileBox rotatedTileBox;
	private Bitmap mapBitmap;
	private Bitmap mapTrackBitmap;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.app = getMyApplication();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(getResources().getColor(
				getMyApplication().getSettings().isLightContent() ? R.color.ctx_menu_info_view_bg_light
						: R.color.ctx_menu_info_view_bg_dark));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.header_layout).setVisibility(View.GONE);

		ListView listView = (ListView) view.findViewById(android.R.id.list);
		listView.setDivider(null);
		listView.setDividerHeight(0);

		TextView tv = new TextView(getActivity());
		tv.setText(R.string.none_selected_gpx);
		tv.setTextSize(24);
		listView.setEmptyView(tv);

		paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(AndroidUtils.dpToPx(app, 4f));
		defPointColor = ContextCompat.getColor(app, R.color.gpx_color_point);
		paintIcon = new Paint();
		pointSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_white_shield_small);
		selectedPoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_default_location);

		adapter = new SegmentGPXAdapter(new ArrayList<GpxDisplayItem>());
		headerView = getActivity().getLayoutInflater().inflate(R.layout.gpx_item_list_header, null, false);
		listView.addHeaderView(headerView);
		listView.addFooterView(getActivity().getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false));
		updateHeader();
		setListAdapter(adapter);
		return view;
	}

	public TrackActivity getMyActivity() {
		return (TrackActivity) getActivity();
	}

	public ArrayAdapter<?> getAdapter() {
		return adapter;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		getMyActivity().getClearToolbar(false);
		if (getGpx() != null && getGpx().path != null && !getGpx().showCurrentTrack) {
			MenuItem item = menu.add(R.string.shared_string_share).setIcon(R.drawable.ic_action_gshare_dark)
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							final Uri fileUri = Uri.fromFile(new File(getGpx().path));
							final Intent sendIntent = new Intent(Intent.ACTION_SEND);
							sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
							sendIntent.setType("application/gpx+xml");
							startActivity(sendIntent);
							return true;
						}
					});
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
		if (getGpx() != null && getGpx().showCurrentTrack) {
			MenuItem item = menu.add(R.string.shared_string_refresh).setIcon(R.drawable.ic_action_refresh_dark)
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							if (updateEnable) {
								updateContent();
								adapter.notifyDataSetChanged();
							}
							return true;
						}
					});
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}

	private GPXFile getGpx() {
		return getMyActivity().getGpx();
	}

	private GpxDataItem getGpxDataItem() {
		return getMyActivity().getGpxDataItem();
	}

	private void startHandler() {
		Handler updateCurrentRecordingTrack = new Handler();
		updateCurrentRecordingTrack.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (updateEnable) {
					updateContent();
					adapter.notifyDataSetChanged();
					startHandler();
				}
			}
		}, 2000);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateContent();
		updateEnable = true;
		if (getGpx() != null && getGpx().showCurrentTrack) {
			//startHandler();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		updateEnable = false;
	}

	private void updateHeader() {
		imageView = (ImageView) headerView.findViewById(R.id.imageView);
		final View splitColorView = headerView.findViewById(R.id.split_color_view);
		final View divider = headerView.findViewById(R.id.divider);
		final View splitIntervalView = headerView.findViewById(R.id.split_interval_view);
		final View colorView = headerView.findViewById(R.id.color_view);
		final SwitchCompat vis = (SwitchCompat) headerView.findViewById(R.id.showOnMapToggle);
		final ProgressBar progressBar = (ProgressBar) headerView.findViewById(R.id.mapLoadProgress);
		boolean selected = getGpx() != null &&
				((getGpx().showCurrentTrack && app.getSelectedGpxHelper().getSelectedCurrentRecordingTrack() != null) ||
						(getGpx().path != null && app.getSelectedGpxHelper().getSelectedFileByPath(getGpx().path) != null));
		vis.setChecked(selected);
		vis.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				app.getSelectedGpxHelper().selectGpxFile(getGpx(), vis.isChecked(), false);
				updateColorView(colorView);
			}
		});
		updateColorView(colorView);
		colorView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final ListPopupWindow popup = new ListPopupWindow(getActivity());
				popup.setAnchorView(colorView);
				popup.setContentWidth(AndroidUtils.dpToPx(app, 200f));
				popup.setModal(true);
				popup.setDropDownGravity(Gravity.RIGHT | Gravity.TOP);
				popup.setVerticalOffset(AndroidUtils.dpToPx(app, -48f));
				popup.setHorizontalOffset(AndroidUtils.dpToPx(app, -6f));
				final GpxAppearanceAdapter gpxApprAdapter = new GpxAppearanceAdapter(getActivity(),
						getGpx().getColor(0), GpxAppearanceAdapterType.TRACK_COLOR);
				popup.setAdapter(gpxApprAdapter);
				popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						AppearanceListItem item = gpxApprAdapter.getItem(position);
						if (item != null) {
							if (item.getAttrName() == CURRENT_TRACK_COLOR_ATTR) {
								int clr = item.getColor();
								if (vis.isChecked()) {
									SelectedGpxFile sf = app.getSelectedGpxHelper().selectGpxFile(getGpx(), vis.isChecked(), false);
									if (clr != 0 && sf.getModifiableGpxFile() != null) {
										sf.getModifiableGpxFile().setColor(clr);
										if (getGpxDataItem() != null) {
											app.getGpxDatabase().updateColor(getGpxDataItem(), clr);
										}
									}
								} else if (getGpxDataItem() != null) {
									app.getGpxDatabase().updateColor(getGpxDataItem(), clr);
								}
								if (getGpx().showCurrentTrack) {
									app.getSettings().CURRENT_TRACK_COLOR.set(clr);
								}
								refreshTrackBitmap();
							}
						}
						popup.dismiss();
						updateColorView(colorView);
					}
				});
				popup.show();
			}
		});

		boolean hasPath = getGpx() != null && getGpx().showCurrentTrack;
		if (rotatedTileBox == null || mapBitmap == null || mapTrackBitmap == null) {
			QuadRect rect = getRect();
			if (rect.left != 0 && rect.top != 0) {
				hasPath = getGpx() != null && (getGpx().tracks.size() > 0 || getGpx().routes.size() > 0);
				progressBar.setVisibility(View.VISIBLE);

				double clat = rect.bottom / 2 + rect.top / 2;
				double clon = rect.left / 2 + rect.right / 2;
				WindowManager mgr = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
				DisplayMetrics dm = new DisplayMetrics();
				mgr.getDefaultDisplay().getMetrics(dm);
				RotatedTileBoxBuilder boxBuilder = new RotatedTileBoxBuilder()
						.setLocation(clat, clon)
						.setZoom(15)
						.density(dm.density)
						.setPixelDimensions(dm.widthPixels, AndroidUtils.dpToPx(app, 152f), 0.5f, 0.5f);

				rotatedTileBox = boxBuilder.build();
				while (rotatedTileBox.getZoom() < 17 && rotatedTileBox.containsLatLon(rect.top, rect.left) && rotatedTileBox.containsLatLon(rect.bottom, rect.right)) {
					rotatedTileBox.setZoom(rotatedTileBox.getZoom() + 1);
				}
				while (rotatedTileBox.getZoom() >= 7 && (!rotatedTileBox.containsLatLon(rect.top, rect.left) || !rotatedTileBox.containsLatLon(rect.bottom, rect.right))) {
					rotatedTileBox.setZoom(rotatedTileBox.getZoom() - 1);
				}

				final DrawSettings drawSettings = new DrawSettings(!app.getSettings().isLightContent(), true);
				final ResourceManager resourceManager = app.getResourceManager();
				final MapRenderRepositories renderer = resourceManager.getRenderer();
				if (resourceManager.updateRenderedMapNeeded(rotatedTileBox, drawSettings)) {
					resourceManager.updateRendererMap(rotatedTileBox, new AsyncLoadingThread.OnMapLoadedListener() {
						@Override
						public void onMapLoaded(boolean interrupted) {
							app.runInUIThread(new Runnable() {
								@Override
								public void run() {
									if (updateEnable) {
										mapBitmap = renderer.getBitmap();
										if (mapBitmap != null) {
											progressBar.setVisibility(View.GONE);
											refreshTrackBitmap();
										}
									}
								}
							});
						}
					});
				}

				imageView.setVisibility(View.VISIBLE);
			} else {
				imageView.setVisibility(View.GONE);
			}
		} else {
			refreshTrackBitmap();
		}

		if (hasPath) {
			if (getGpx() != null && !getGpx().showCurrentTrack && adapter.getCount() > 0) {
				prepareSplitIntervalAdapterData();
				setupSplitIntervalView(splitIntervalView);
				updateSplitIntervalView(splitIntervalView);
				splitIntervalView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						final ListPopupWindow popup = new ListPopupWindow(getActivity());
						popup.setAnchorView(splitIntervalView);
						popup.setContentWidth(AndroidUtils.dpToPx(app, 200f));
						popup.setModal(true);
						popup.setDropDownGravity(Gravity.RIGHT | Gravity.TOP);
						popup.setVerticalOffset(AndroidUtils.dpToPx(app, -48f));
						popup.setHorizontalOffset(AndroidUtils.dpToPx(app, -6f));
						popup.setAdapter(new ArrayAdapter<>(getMyActivity(),
								R.layout.popup_list_text_item, options));
						popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

							@Override
							public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
								selectedSplitInterval = position;
								SelectedGpxFile sf = app.getSelectedGpxHelper().selectGpxFile(getGpx(), vis.isChecked(), false);
								final List<GpxDisplayGroup> groups = getDisplayGroups();
								if (groups.size() > 0) {
									updateSplit(groups, vis.isChecked() ? sf : null);
								}
								popup.dismiss();
								updateSplitIntervalView(splitIntervalView);
							}
						});
						popup.show();
					}
				});
				splitIntervalView.setVisibility(View.VISIBLE);
			} else {
				splitIntervalView.setVisibility(View.GONE);
			}
			splitColorView.setVisibility(View.VISIBLE);
			divider.setVisibility(View.VISIBLE);
		} else {
			splitColorView.setVisibility(View.GONE);
			divider.setVisibility(View.GONE);
		}
	}

	private void refreshTrackBitmap() {
		currentTrackColor = app.getSettings().CURRENT_TRACK_COLOR.get();
		if (mapBitmap != null) {
			SelectedGpxFile sf;
			if (getGpx().showCurrentTrack) {
				sf = app.getSavingTrackHelper().getCurrentTrack();
			} else {
				sf = new SelectedGpxFile();
				sf.setGpxFile(getGpx());
			}
			Bitmap bmp = mapBitmap.copy(mapBitmap.getConfig(), true);
			Canvas canvas = new Canvas(bmp);
			drawTrack(canvas, rotatedTileBox, sf);
			drawPoints(canvas, rotatedTileBox, sf);
			mapTrackBitmap = bmp;
			Bitmap selectedPointBitmap = drawSelectedPoint();
			if (selectedPointBitmap != null) {
				imageView.setImageDrawable(new BitmapDrawable(app.getResources(), selectedPointBitmap));
			} else {
				imageView.setImageDrawable(new BitmapDrawable(app.getResources(), mapTrackBitmap));
			}
		}
	}

	private void drawTrack(Canvas canvas, RotatedTileBox tileBox, SelectedGpxFile g) {
		GpxDataItem gpxDataItem = null;
		if (!g.isShowCurrentTrack()) {
			gpxDataItem = getGpxDataItem();
		}
		List<TrkSegment> segments = g.getPointsToDisplay();
		for (TrkSegment ts : segments) {
			int color = gpxDataItem != null ? gpxDataItem.getColor() : 0;
			if (g.isShowCurrentTrack()) {
				color = currentTrackColor;
			}
			if (color == 0) {
				color = ts.getColor(trackColor);
			}
			if (ts.renders.isEmpty()                // only do once (CODE HERE NEEDS TO BE UI INSTEAD)
					&& !ts.points.isEmpty()) {        // hmmm. 0-point tracks happen, but.... how?
				if (g.isShowCurrentTrack()) {
					ts.renders.add(new Renderable.CurrentTrack(ts.points));
				} else {
					ts.renders.add(new Renderable.StandardTrack(ts.points, 17.2));
				}
			}
			paint.setColor(color == 0 ? trackColor : color);
			ts.drawRenderers(tileBox.getZoom(), paint, canvas, tileBox);
		}
	}

	private void drawPoints(Canvas canvas, RotatedTileBox tileBox, SelectedGpxFile g) {
		List<WptPt> pts = g.getGpxFile().points;
		@ColorInt
		int fileColor = g.getColor() == 0 ? defPointColor : g.getColor();
		for (WptPt o : pts) {
			float x = tileBox.getPixXFromLatLon(o.lat, o.lon);
			float y = tileBox.getPixYFromLatLon(o.lat, o.lon);

			int pointColor = o.getColor(fileColor);
			paintIcon.setColorFilter(new PorterDuffColorFilter(pointColor, PorterDuff.Mode.MULTIPLY));
			canvas.drawBitmap(pointSmall, x - pointSmall.getWidth() / 2, y - pointSmall.getHeight() / 2, paintIcon);
		}
	}

	private Bitmap drawSelectedPoint() {
		if (mapTrackBitmap != null && rotatedTileBox != null && selectedPointLatLon != null) {
			float x = rotatedTileBox.getPixXFromLatLon(selectedPointLatLon.getLatitude(), selectedPointLatLon.getLongitude());
			float y = rotatedTileBox.getPixYFromLatLon(selectedPointLatLon.getLatitude(), selectedPointLatLon.getLongitude());
			paintIcon.setColorFilter(null);
			Bitmap bmp = mapTrackBitmap.copy(mapTrackBitmap.getConfig(), true);
			Canvas canvas = new Canvas(bmp);
			canvas.drawBitmap(selectedPoint, x - selectedPoint.getWidth() / 2, y - selectedPoint.getHeight() / 2, paintIcon);
			return bmp;
		} else {
			return null;
		}
	}

	private QuadRect getRect() {
		double left = 0, right = 0;
		double top = 0, bottom = 0;
		if (getGpx() != null) {
			for (Track track : getGpx().tracks) {
				for (TrkSegment segment : track.segments) {
					for (WptPt p : segment.points) {
						if (left == 0 && right == 0) {
							left = p.getLongitude();
							right = p.getLongitude();
							top = p.getLatitude();
							bottom = p.getLatitude();
						} else {
							left = Math.min(left, p.getLongitude());
							right = Math.max(right, p.getLongitude());
							top = Math.max(top, p.getLatitude());
							bottom = Math.min(bottom, p.getLatitude());
						}
					}
				}
			}
			for (WptPt p : getGpx().points) {
				if (left == 0 && right == 0) {
					left = p.getLongitude();
					right = p.getLongitude();
					top = p.getLatitude();
					bottom = p.getLatitude();
				} else {
					left = Math.min(left, p.getLongitude());
					right = Math.max(right, p.getLongitude());
					top = Math.max(top, p.getLatitude());
					bottom = Math.min(bottom, p.getLatitude());
				}
			}
			for (Route route : getGpx().routes) {
				for (WptPt p : route.points) {
					if (left == 0 && right == 0) {
						left = p.getLongitude();
						right = p.getLongitude();
						top = p.getLatitude();
						bottom = p.getLatitude();
					} else {
						left = Math.min(left, p.getLongitude());
						right = Math.max(right, p.getLongitude());
						top = Math.max(top, p.getLatitude());
						bottom = Math.min(bottom, p.getLatitude());
					}
				}
			}
		}
		return new QuadRect(left, top, right, bottom);
	}

	private List<GpxDisplayGroup> getOriginalGroups() {
		return filterGroups(false);
	}

	private List<GpxDisplayGroup> getDisplayGroups() {
		return filterGroups(true);
	}

	private void setupSplitIntervalView(View view) {
		final TextView title = (TextView) view.findViewById(R.id.split_interval_title);
		final TextView text = (TextView) view.findViewById(R.id.split_interval_text);
		final ImageView img = (ImageView) view.findViewById(R.id.split_interval_arrow);
		int colorId;
		final List<GpxDisplayGroup> groups = getDisplayGroups();
		if (groups.size() > 0) {
			colorId = app.getSettings().isLightContent() ?
					R.color.primary_text_light : R.color.primary_text_dark;
		} else {
			colorId = app.getSettings().isLightContent() ?
					R.color.secondary_text_light : R.color.secondary_text_dark;
		}
		int color = app.getResources().getColor(colorId);
		title.setTextColor(color);
		text.setTextColor(color);
		img.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_action_arrow_drop_down, colorId));
	}

	private void updateSplitIntervalView(View view) {
		final TextView text = (TextView) view.findViewById(R.id.split_interval_text);
		if (selectedSplitInterval == 0) {
			text.setText(getString(R.string.shared_string_none));
		} else {
			text.setText(options.get(selectedSplitInterval));
		}
	}

	private void updateColorView(View colorView) {
		final ImageView colorImageView = (ImageView) colorView.findViewById(R.id.colorImage);
		int color = getGpxDataItem() != null ? getGpxDataItem().getColor() : 0;
		if (color == 0 && getGpx() != null) {
			if (getGpx().showCurrentTrack) {
				color = app.getSettings().CURRENT_TRACK_COLOR.get();
			} else {
				color = getGpx().getColor(0);
			}
		}
		if (color == 0) {
			final RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
			final OsmandSettings.CommonPreference<String> prefColor
					= app.getSettings().getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR);
			color = GpxAppearanceAdapter.parseTrackColor(renderer, prefColor.get());
		}
		if (color == 0) {
			colorImageView.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_circle));
		} else {
			colorImageView.setImageDrawable(app.getIconsCache().getPaintedIcon(R.drawable.ic_action_circle, color));
		}
		trackColor = color;
	}

	private boolean isArgumentTrue(@NonNull String arg) {
		return getArguments() != null && getArguments().getBoolean(arg);
	}

	protected boolean hasFilterType(GpxDisplayItemType filterType) {
		for (GpxDisplayItemType type : filterTypes) {
			if (type == filterType) {
				return true;
			}
		}
		return false;
	}

	private List<GpxDisplayGroup> filterGroups(boolean useDisplayGroups) {
		List<GpxDisplayGroup> result = getMyActivity().getGpxFile(useDisplayGroups);
		List<GpxDisplayGroup> groups = new ArrayList<>();
		for (GpxDisplayGroup group : result) {
			boolean add = hasFilterType(group.getType());
			if (isArgumentTrue(ARG_TO_FILTER_SHORT_TRACKS)) {
				Iterator<GpxDisplayItem> item = group.getModifiableList().iterator();
				while (item.hasNext()) {
					GpxDisplayItem it2 = item.next();
					if (it2.analysis != null && it2.analysis.totalDistance < 100) {
						item.remove();
					}
				}
				if (group.getModifiableList().isEmpty()) {
					add = false;
				}
			}
			if (add) {
				groups.add(group);
			}

		}
		return groups;
	}

	public void updateContent() {
		adapter.clear();
		List<GpxDisplayGroup> groups = getOriginalGroups();
		adapter.setNotifyOnChange(false);
		for (GpxDisplayItem i : flatten(groups)) {
			adapter.add(i);
		}
		adapter.setNotifyOnChange(true);
		adapter.notifyDataSetChanged();
		updateHeader();
	}

	protected List<GpxDisplayItem> flatten(List<GpxDisplayGroup> groups) {
		ArrayList<GpxDisplayItem> list = new ArrayList<>();
		for(GpxDisplayGroup g : groups) {
			list.addAll(g.getModifiableList());
		}
		return list;
	}

	private void prepareSplitIntervalAdapterData() {
		final List<GpxDisplayGroup> groups = getDisplayGroups();

		options.add(app.getString(R.string.shared_string_none));
		distanceSplit.add(-1d);
		timeSplit.add(-1);
		addOptionSplit(30, true, groups); // 50 feet, 20 yards, 20
		// m
		addOptionSplit(60, true, groups); // 100 feet, 50 yards,
		// 50 m
		addOptionSplit(150, true, groups); // 200 feet, 100 yards,
		// 100 m
		addOptionSplit(300, true, groups); // 500 feet, 200 yards,
		// 200 m
		addOptionSplit(600, true, groups); // 1000 feet, 500 yards,
		// 500 m
		addOptionSplit(1500, true, groups); // 2000 feet, 1000 yards, 1 km
		addOptionSplit(3000, true, groups); // 1 mi, 2 km
		addOptionSplit(6000, true, groups); // 2 mi, 5 km
		addOptionSplit(15000, true, groups); // 5 mi, 10 km

		addOptionSplit(15, false, groups);
		addOptionSplit(30, false, groups);
		addOptionSplit(60, false, groups);
		addOptionSplit(120, false, groups);
		addOptionSplit(150, false, groups);
		addOptionSplit(300, false, groups);
		addOptionSplit(600, false, groups);
		addOptionSplit(900, false, groups);
	}

	private void updateSplit(List<GpxDisplayGroup> groups, SelectedGpxFile sf) {
		new SplitTrackAsyncTask(sf, groups).execute((Void) null);
	}

	private void addOptionSplit(int value, boolean distance, List<GpxDisplayGroup> model) {
		if (distance) {
			double dvalue = OsmAndFormatter.calculateRoundedDist(value, app);
			options.add(OsmAndFormatter.getFormattedDistance((float) dvalue, app));
			distanceSplit.add(dvalue);
			timeSplit.add(-1);
			if (Math.abs(model.get(0).getSplitDistance() - dvalue) < 1) {
				selectedSplitInterval = distanceSplit.size() - 1;
			}
		} else {
			if (value < 60) {
				options.add(value + " " + app.getString(R.string.int_seconds));
			} else if (value % 60 == 0) {
				options.add((value / 60) + " " + app.getString(R.string.int_min));
			} else {
				options.add((value / 60f) + " " + app.getString(R.string.int_min));
			}
			distanceSplit.add(-1d);
			timeSplit.add(value);
			if (model.get(0).getSplitTime() == value) {
				selectedSplitInterval = distanceSplit.size() - 1;
			}
		}
	}

	private class SegmentGPXAdapter extends ArrayAdapter<GpxDisplayItem> {

		SegmentGPXAdapter(List<GpxDisplayItem> items) {
			super(getActivity(), R.layout.gpx_list_item_tab_content, items);
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			View row = convertView;
			PagerSlidingTabStrip tabLayout;
			WrapContentHeightViewPager pager;
			boolean create = false;
			if (row == null) {
				LayoutInflater inflater = getMyActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.gpx_list_item_tab_content, parent, false);

				boolean light = app.getSettings().isLightContent();
				tabLayout = (PagerSlidingTabStrip) row.findViewById(R.id.sliding_tabs);
				tabLayout.setTabBackground(R.color.color_transparent);
				tabLayout.setIndicatorColorResource(light ? R.color.color_dialog_buttons_light : R.color.color_dialog_buttons_dark);
				tabLayout.setIndicatorBgColorResource(light ? R.color.dashboard_divider_light : R.color.dashboard_divider_dark);
				tabLayout.setIndicatorHeight(AndroidUtils.dpToPx(app, 1f));
				tabLayout.setTextColor(tabLayout.getIndicatorColor());
				tabLayout.setTextSize(AndroidUtils.spToPx(app, 12f));
				tabLayout.setShouldExpand(true);
				tabLayout.setTabSelectionType(PagerSlidingTabStrip.TabSelectionType.SOLID_COLOR);
				pager = (WrapContentHeightViewPager) row.findViewById(R.id.pager);
				pager.setSwipeable(false);
				pager.setOffscreenPageLimit(2);
				create = true;
			} else {
				tabLayout = (PagerSlidingTabStrip) row.findViewById(R.id.sliding_tabs);
				pager = (WrapContentHeightViewPager) row.findViewById(R.id.pager);
			}
			GpxDisplayItem item = getItem(position);
			if (item != null) {
				pager.setAdapter(new GPXItemPagerAdapter(tabLayout, item));
				if (create) {
					tabLayout.setViewPager(pager);
				} else {
					tabLayout.notifyDataSetChanged(true);
				}
			}
			return row;
		}
	}

	private enum GPXTabItemType {
		GPX_TAB_ITEM_GENERAL,
		GPX_TAB_ITEM_ALTITUDE,
		GPX_TAB_ITEM_SPEED
	}

	private class GPXItemPagerAdapter extends PagerAdapter implements CustomTabProvider, ViewAtPositionInterface {

		protected SparseArray<View> views = new SparseArray<>();
		private PagerSlidingTabStrip tabs;
		private GpxDisplayItem gpxItem;
		private GPXTabItemType[] tabTypes;
		private String[] titles;
		private Map<GPXTabItemType, List<ILineDataSet>> dataSetsMap = new HashMap<>();
		private TrkSegment segment;

		GPXItemPagerAdapter(PagerSlidingTabStrip tabs, GpxDisplayItem gpxItem) {
			super();
			this.tabs = tabs;
			this.gpxItem = gpxItem;
			fetchTabTypes();
		}

		private void fetchTabTypes() {

			List<GPXTabItemType> tabTypeList = new ArrayList<>();
			tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
			if (gpxItem != null && gpxItem.analysis != null) {
				if (gpxItem.analysis.hasElevationData) {
					tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_ALTITUDE);
				}
				if (gpxItem.analysis.isSpeedSpecified()) {
					tabTypeList.add(GPXTabItemType.GPX_TAB_ITEM_SPEED);
				}
			}
			tabTypes = tabTypeList.toArray(new GPXTabItemType[tabTypeList.size()]);

			Context context = tabs.getContext();
			titles = new String[tabTypes.length];
			for (int i = 0; i < titles.length; i++) {
				switch (tabTypes[i]) {
					case GPX_TAB_ITEM_GENERAL:
						titles[i] = context.getString(R.string.shared_string_overview);
						break;
					case GPX_TAB_ITEM_ALTITUDE:
						titles[i] = context.getString(R.string.altitude);
						break;
					case GPX_TAB_ITEM_SPEED:
						titles[i] = context.getString(R.string.map_widget_speed);
						break;
				}
			}
		}

		private List<ILineDataSet> getDataSets(GPXTabItemType tabType, LineChart chart) {
			List<ILineDataSet> dataSets = dataSetsMap.get(tabType);
			if (dataSets == null && chart != null) {
				dataSets = new ArrayList<>();
				GPXTrackAnalysis analysis = gpxItem.analysis;
				switch (tabType) {
					case GPX_TAB_ITEM_GENERAL: {
						OrderedLineDataSet speedDataSet = null;
						OrderedLineDataSet elevationDataSet = null;
						if (analysis.hasSpeedData) {
							speedDataSet = GpxUiHelper.createGPXSpeedDataSet(app, chart,
									analysis, GPXDataSetAxisType.DISTANCE, true, true);
						}
						if (analysis.hasElevationData) {
							elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, chart,
									analysis, GPXDataSetAxisType.DISTANCE, false, true);
						}
						if (speedDataSet != null) {
							dataSets.add(speedDataSet);
							if (elevationDataSet != null) {
								dataSets.add(elevationDataSet.getPriority() < speedDataSet.getPriority()
										? 1 : 0, elevationDataSet);
							}
						} else if (elevationDataSet != null) {
							dataSets.add(elevationDataSet);
						}
						dataSetsMap.put(GPXTabItemType.GPX_TAB_ITEM_GENERAL, dataSets);
						break;
					}
					case GPX_TAB_ITEM_ALTITUDE: {
						OrderedLineDataSet elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, chart,
								analysis, GPXDataSetAxisType.DISTANCE, false, true);
						if (elevationDataSet != null) {
							dataSets.add(elevationDataSet);
						}
						if (analysis.hasElevationData) {
							OrderedLineDataSet slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, chart,
									analysis, GPXDataSetAxisType.DISTANCE, elevationDataSet.getValues(), true, true);
							if (slopeDataSet != null) {
								dataSets.add(slopeDataSet);
							}
						}
						dataSetsMap.put(GPXTabItemType.GPX_TAB_ITEM_ALTITUDE, dataSets);
						break;
					}
					case GPX_TAB_ITEM_SPEED: {
						OrderedLineDataSet speedDataSet = GpxUiHelper.createGPXSpeedDataSet(app, chart,
								analysis, GPXDataSetAxisType.DISTANCE, false, true);
						if (speedDataSet != null) {
							dataSets.add(speedDataSet);
						}
						dataSetsMap.put(GPXTabItemType.GPX_TAB_ITEM_SPEED, dataSets);
						break;
					}
				}
			}
			return dataSets;
		}

		private TrkSegment getTrackSegment(LineChart chart) {
			if (segment == null) {
				List<ILineDataSet> ds = chart.getLineData().getDataSets();
				if (ds != null && ds.size() > 0) {
					for (GPXUtilities.Track t : gpxItem.group.getGpx().tracks) {
						for (TrkSegment s : t.segments) {
							if (s.points.size() > 0 && s.points.get(0).equals(gpxItem.analysis.locationStart)) {
								segment = s;
								break;
							}
						}
						if (segment != null) {
							break;
						}
					}
				}
			}
			return segment;
		}

		private WptPt getPoint(LineChart chart, float pos) {
			WptPt wpt = null;
			List<ILineDataSet> ds = chart.getLineData().getDataSets();
			if (ds != null && ds.size() > 0) {
				TrkSegment segment = getTrackSegment(chart);
				OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
				if (gpxItem.chartAxisType == GPXDataSetAxisType.TIME) {
					float time = pos * 1000;
					for (WptPt p : segment.points) {
						if (p.time - gpxItem.analysis.startTime >= time) {
							wpt = p;
							break;
						}
					}
				} else {
					float distance = pos * dataSet.getDivX();
					for (WptPt p : segment.points) {
						if (p.distance >= distance) {
							wpt = p;
							break;
						}
					}
				}
			}
			return wpt;
		}

		@Override
		public int getCount() {
			return tabTypes.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titles[position];
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {

			GPXTabItemType tabType = tabTypes[position];
			View view = null;
			switch (tabType) {
				case GPX_TAB_ITEM_GENERAL:
					view = getActivity().getLayoutInflater().inflate(R.layout.gpx_item_general, container, false);
					break;
				case GPX_TAB_ITEM_ALTITUDE:
					view = getActivity().getLayoutInflater().inflate(R.layout.gpx_item_altitude, container, false);
					break;
				case GPX_TAB_ITEM_SPEED:
					view = getActivity().getLayoutInflater().inflate(R.layout.gpx_item_speed, container, false);
					break;
			}

			if (view != null) {
				if (gpxItem != null) {
					GPXTrackAnalysis analysis = gpxItem.analysis;
					final LineChart chart = (LineChart) view.findViewById(R.id.chart);
					chart.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							getListView().requestDisallowInterceptTouchEvent(true);
							return false;
						}
					});
					chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
						@Override
						public void onValueSelected(Entry e, Highlight h) {
							WptPt wpt = getPoint(chart, h.getX());
							if (wpt != null) {
								selectedPointLatLon = new LatLon(wpt.lat, wpt.lon);
								Bitmap bmp = drawSelectedPoint();
								imageView.setImageDrawable(new BitmapDrawable(app.getResources(), bmp));
							}
						}

						@Override
						public void onNothingSelected() {

						}
					});
					final View finalView = view;
					chart.setOnChartGestureListener(new OnChartGestureListener() {

						float highlightDrawX = -1;

						@Override
						public void onChartGestureStart(MotionEvent me, ChartGesture lastPerformedGesture) {
							if (chart.getHighlighted() != null && chart.getHighlighted().length > 0) {
								highlightDrawX = chart.getHighlighted()[0].getDrawX();
							} else {
								highlightDrawX = -1;
							}
						}

						@Override
						public void onChartGestureEnd(MotionEvent me, ChartGesture lastPerformedGesture) {
							gpxItem.chartMatrix = new Matrix(chart.getViewPortHandler().getMatrixTouch());
							Highlight[] highlights = chart.getHighlighted();
							if (highlights != null && highlights.length > 0) {
								gpxItem.chartHighlightPos = highlights[0].getX();
							} else {
								gpxItem.chartHighlightPos = -1;
							}
							for (int i = 0; i < getCount(); i++) {
								View v = getViewAtPosition(i);
								if (v != finalView) {
									updateChart(i);
								}
							}
						}

						@Override
						public void onChartLongPressed(MotionEvent me) {
						}

						@Override
						public void onChartDoubleTapped(MotionEvent me) {
						}

						@Override
						public void onChartSingleTapped(MotionEvent me) {
						}

						@Override
						public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
						}

						@Override
						public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
						}

						@Override
						public void onChartTranslate(MotionEvent me, float dX, float dY) {
							if (highlightDrawX != -1) {
								Highlight h = chart.getHighlightByTouchPoint(highlightDrawX, 0f);
								if (h != null) {
									chart.highlightValue(h);
									WptPt wpt = getPoint(chart, h.getX());
									if (wpt != null) {
										selectedPointLatLon = new LatLon(wpt.lat, wpt.lon);
										Bitmap bmp = drawSelectedPoint();
										imageView.setImageDrawable(new BitmapDrawable(app.getResources(), bmp));
									}
								}
							}
						}
					});

					IconsCache ic = app.getIconsCache();
					switch (tabType) {
						case GPX_TAB_ITEM_GENERAL:
							if (analysis != null) {
								if (analysis.hasElevationData || analysis.hasSpeedData) {
									GpxUiHelper.setupGPXChart(app, chart, 4);
									chart.setData(new LineData(getDataSets(GPXTabItemType.GPX_TAB_ITEM_GENERAL, chart)));
									updateChart(chart);
									chart.setVisibility(View.VISIBLE);
								} else {
									chart.setVisibility(View.GONE);
								}

								((ImageView) view.findViewById(R.id.distance_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_polygom_dark));
								((ImageView) view.findViewById(R.id.duration_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time_span));
								((ImageView) view.findViewById(R.id.start_time_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time_start));
								((ImageView) view.findViewById(R.id.end_time_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time_end));

								((TextView) view.findViewById(R.id.distance_text))
										.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
								((TextView) view.findViewById(R.id.duration_text))
										.setText(Algorithms.formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled()));

								if (analysis.timeSpan > 0) {
									DateFormat tf = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
									DateFormat df = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);

									Date start = new Date(analysis.startTime);
									((TextView) view.findViewById(R.id.start_time_text)).setText(tf.format(start));
									((TextView) view.findViewById(R.id.start_date_text)).setText(df.format(start));
									Date end = new Date(analysis.endTime);
									((TextView) view.findViewById(R.id.end_time_text)).setText(tf.format(end));
									((TextView) view.findViewById(R.id.end_date_text)).setText(df.format(end));
								} else {
									view.findViewById(R.id.list_divider).setVisibility(View.GONE);
									view.findViewById(R.id.start_end_time).setVisibility(View.GONE);
								}
							} else {
								chart.setVisibility(View.GONE);
								view.findViewById(R.id.distance_time_span).setVisibility(View.GONE);
								view.findViewById(R.id.list_divider).setVisibility(View.GONE);
								view.findViewById(R.id.start_end_time).setVisibility(View.GONE);
							}
							view.findViewById(R.id.details_view).setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									openDetails(GPXTabItemType.GPX_TAB_ITEM_GENERAL);
								}
							});

							break;
						case GPX_TAB_ITEM_ALTITUDE:
							if (analysis != null) {
								if (analysis.hasElevationData) {
									GpxUiHelper.setupGPXChart(app, chart, 4);
									chart.setData(new LineData(getDataSets(GPXTabItemType.GPX_TAB_ITEM_ALTITUDE, chart)));
									updateChart(chart);
									chart.setVisibility(View.VISIBLE);
								} else {
									chart.setVisibility(View.GONE);
								}
								((ImageView) view.findViewById(R.id.average_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_altitude_average));
								((ImageView) view.findViewById(R.id.range_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_altitude_average));
								((ImageView) view.findViewById(R.id.ascent_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_altitude_ascent));
								((ImageView) view.findViewById(R.id.descent_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_altitude_descent));

								String min = OsmAndFormatter.getFormattedAlt(analysis.minElevation, app);
								String max = OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app);
								String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
								String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);

								((TextView) view.findViewById(R.id.average_text))
										.setText(OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app));
								((TextView) view.findViewById(R.id.range_text)).setText(min + " - " + max);
								((TextView) view.findViewById(R.id.ascent_text)).setText(asc);
								((TextView) view.findViewById(R.id.descent_text)).setText(desc);

							} else {
								chart.setVisibility(View.GONE);
								view.findViewById(R.id.average_range).setVisibility(View.GONE);
								view.findViewById(R.id.list_divider).setVisibility(View.GONE);
								view.findViewById(R.id.ascent_descent).setVisibility(View.GONE);
							}
							view.findViewById(R.id.details_view).setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									openDetails(GPXTabItemType.GPX_TAB_ITEM_ALTITUDE);
								}
							});

							break;
						case GPX_TAB_ITEM_SPEED:
							if (analysis != null && analysis.isSpeedSpecified()) {
								if (analysis.hasSpeedData) {
									GpxUiHelper.setupGPXChart(app, chart, 4);
									chart.setData(new LineData(getDataSets(GPXTabItemType.GPX_TAB_ITEM_SPEED, chart)));
									updateChart(chart);
									chart.setVisibility(View.VISIBLE);
								} else {
									chart.setVisibility(View.GONE);
								}
								((ImageView) view.findViewById(R.id.average_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_speed));
								((ImageView) view.findViewById(R.id.max_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_max_speed));
								((ImageView) view.findViewById(R.id.time_moving_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_time_span));
								((ImageView) view.findViewById(R.id.distance_icon))
										.setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_polygom_dark));

								String avg = OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app);
								String max = OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app);

								((TextView) view.findViewById(R.id.average_text)).setText(avg);
								((TextView) view.findViewById(R.id.max_text)).setText(max);
								((TextView) view.findViewById(R.id.time_moving_text))
										.setText(Algorithms.formatDuration((int) (analysis.timeMoving / 1000), app.accessibilityEnabled()));
								((TextView) view.findViewById(R.id.distance_text))
										.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving, app));

							} else {
								chart.setVisibility(View.GONE);
								view.findViewById(R.id.average_max).setVisibility(View.GONE);
								view.findViewById(R.id.list_divider).setVisibility(View.GONE);
								view.findViewById(R.id.time_distance).setVisibility(View.GONE);
							}
							view.findViewById(R.id.details_view).setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									openDetails(GPXTabItemType.GPX_TAB_ITEM_SPEED);
								}
							});
							break;
					}
				}
			}

			container.addView(view, 0);
			views.put(position, view);
			return view;
		}

		@Override
		public void destroyItem(ViewGroup collection, int position, Object view) {
			views.remove(position);
			collection.removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public View getCustomTabView(ViewGroup parent, int position) {
			View tab = getActivity().getLayoutInflater().inflate(R.layout.gpx_tab, parent, false);
			tab.setTag(tabTypes[position].name());
			deselect(tab);
			return tab;
		}

		private int getImageId(GPXTabItemType tabType) {
			int imageId;
			switch (tabType) {
				case GPX_TAB_ITEM_GENERAL:
					imageId = R.drawable.ic_action_polygom_dark;
					break;
				case GPX_TAB_ITEM_ALTITUDE:
					imageId = R.drawable.ic_action_altitude_average;
					break;
				case GPX_TAB_ITEM_SPEED:
					imageId = R.drawable.ic_action_speed;
					break;
				default:
					imageId = R.drawable.ic_action_folder_stroke;
			}
			return imageId;
		}

		@Override
		public void select(View tab) {
			GPXTabItemType tabType = GPXTabItemType.valueOf((String)tab.getTag());
			ImageView img = (ImageView) tab.findViewById(R.id.tab_image);
			int imageId = getImageId(tabType);
			switch (tabs.getTabSelectionType()) {
				case ALPHA:
					ViewCompat.setAlpha(img, tabs.getTabTextSelectedAlpha());
					break;
				case SOLID_COLOR:
					img.setImageDrawable(app.getIconsCache().getPaintedIcon(imageId, tabs.getTextColor()));
					break;
			}
		}

		@Override
		public void deselect(View tab) {
			GPXTabItemType tabType = GPXTabItemType.valueOf((String)tab.getTag());
			ImageView img = (ImageView) tab.findViewById(R.id.tab_image);
			int imageId = getImageId(tabType);
			switch (tabs.getTabSelectionType()) {
				case ALPHA:
					ViewCompat.setAlpha(img, tabs.getTabTextAlpha());
					break;
				case SOLID_COLOR:
					img.setImageDrawable(app.getIconsCache().getPaintedIcon(imageId, tabs.getTabInactiveTextColor()));
					break;
			}
		}

		@Override
		public View getViewAtPosition(int position) {
			return views.get(position);
		}

		void updateChart(int position) {
			View view = getViewAtPosition(position);
			if (view != null) {
				updateChart((LineChart) view.findViewById(R.id.chart));
			}
		}

		void updateChart(LineChart chart) {
			if (chart != null && !chart.isEmpty()) {
				if (gpxItem.chartMatrix != null) {
					chart.getViewPortHandler().refresh(new Matrix(gpxItem.chartMatrix), chart, true);
				}
				if (gpxItem.chartHighlightPos != -1) {
					chart.highlightValue(gpxItem.chartHighlightPos, 0);
				} else {
					chart.highlightValue(null);
				}
			}
		}

		void openDetails(GPXTabItemType tabType) {
			LatLon location = null;
			WptPt wpt = null;
			gpxItem.chartTypes = null;
			List<ILineDataSet> ds = getDataSets(tabType, null);
			if (ds != null && ds.size() > 0) {
				gpxItem.chartTypes = new GPXDataSetType[ds.size()];
				for (int i = 0; i < ds.size(); i++) {
					OrderedLineDataSet orderedDataSet = (OrderedLineDataSet) ds.get(i);
					gpxItem.chartTypes[i] = orderedDataSet.getDataSetType();
				}
				if (gpxItem.chartHighlightPos != -1) {
					TrkSegment segment = null;
					for (Track t : gpxItem.group.getGpx().tracks) {
						for (TrkSegment s : t.segments) {
							if (s.points.size() > 0 && s.points.get(0).equals(gpxItem.analysis.locationStart)) {
								segment = s;
								break;
							}
						}
						if (segment != null) {
							break;
						}
					}
					if (segment != null) {
						OrderedLineDataSet dataSet = (OrderedLineDataSet) ds.get(0);
						float distance = gpxItem.chartHighlightPos * dataSet.getDivX();
						for (WptPt p : segment.points) {
							if (p.distance >= distance) {
								wpt = p;
								break;
							}
						}
						if (wpt != null) {
							location = new LatLon(wpt.lat, wpt.lon);
						}
					}
				}
			}
			if (location == null) {
				location = new LatLon(gpxItem.locationStart.lat, gpxItem.locationStart.lon);
			}
			if (wpt != null) {
				gpxItem.locationOnMap = wpt;
			} else {
				gpxItem.locationOnMap = gpxItem.locationStart;
			}

			if (gpxItem.group.getGpx() != null) {
				gpxItem.wasHidden = app.getSelectedGpxHelper().getSelectedFileByPath(getGpx().path) == null;
				app.getSelectedGpxHelper().setGpxFileToDisplay(gpxItem.group.getGpx());
			}
			final OsmandSettings settings = app.getSettings();
			settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
					settings.getLastKnownMapZoom(),
					new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
					false,
					gpxItem);

			MapActivity.launchMapActivityMoveToTop(getActivity());
		}
	}

	private class SplitTrackAsyncTask extends AsyncTask<Void, Void, Void> {
		@Nullable
		private final SelectedGpxFile mSelectedGpxFile;
		@NonNull private final TrackSegmentFragment mFragment;
		@NonNull private final TrackActivity mActivity;

		private final List<GpxDisplayGroup> groups;

		SplitTrackAsyncTask(@Nullable SelectedGpxFile selectedGpxFile, List<GpxDisplayGroup> groups) {
			mSelectedGpxFile = selectedGpxFile;
			mFragment = TrackSegmentFragment.this;
			mActivity = getMyActivity();
			this.groups = groups;
		}

		protected void onPostExecute(Void result) {
			if (mSelectedGpxFile != null) {
				mSelectedGpxFile.setDisplayGroups(getDisplayGroups());
			}
			if (mFragment.isVisible()) {
				//mFragment.updateContent();
			}
			if (!mActivity.isFinishing()) {
				mActivity.setProgressBarIndeterminateVisibility(false);
			}
		}

		protected void onPreExecute() {
			mActivity.setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected Void doInBackground(Void... params) {
			for (GpxDisplayGroup model : groups) {
				OsmandApplication application = mActivity.getMyApplication();
				if (selectedSplitInterval == 0) {
					model.noSplit(application);
				} else if (distanceSplit.get(selectedSplitInterval) > 0) {
					model.splitByDistance(application, distanceSplit.get(selectedSplitInterval));
				} else if (timeSplit.get(selectedSplitInterval) > 0) {
					model.splitByTime(application, timeSplit.get(selectedSplitInterval));
				}
			}

			return null;
		}
	}
}
