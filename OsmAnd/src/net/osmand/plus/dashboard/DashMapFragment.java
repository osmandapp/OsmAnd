package net.osmand.plus.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.ImageView;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.R;
import net.osmand.plus.activities.MainMenuActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.ResourceManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by Denis on
 * 24.11.2014.
 */
public class DashMapFragment extends DashBaseFragment implements IMapDownloaderCallback {

	private static final int CARD_INTERVAL_UPDATE = 60*1000;//4*60*60*1000;

	public static final String TAG = "DASH_MAP_FRAGMENT";
	
	private Paint paintBmp;

	@Override
	public void onDestroy() {
		super.onDestroy();
		getMyApplication().getResourceManager().getMapTileDownloader().removeDownloaderCallback(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getMyApplication().getResourceManager().getMapTileDownloader().addDownloaderCallback(this);
		paintBmp = new Paint();
		paintBmp.setAntiAlias(true);
		paintBmp.setFilterBitmap(true);
		paintBmp.setDither(true);
	}

	protected void startMapActivity() {
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_map_fragment, container, false);

		view.findViewById(R.id.map_image).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startMapActivity();
			}
		});

		return view;
	}

	private void setMapImage(final Bitmap image) {
		final View view = getView();
		if (view != null && image != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final ImageView mapView = (ImageView) view.findViewById(R.id.map_image);
					mapView.setImageDrawable(new Drawable(){

						@Override
						public void draw(Canvas canvas) {
//							int h = mapView.getHeight();
//							int w = mapView.getWidth();
							canvas.drawBitmap(image, 0, 0, paintBmp);
						}

						@Override
						public void setAlpha(int alpha) {
						}

						@Override
						public void setColorFilter(ColorFilter cf) {
						}

						@Override
						public int getOpacity() {
							return 0;
						}
						
					});
				}
			});

		}
	}


	@Override
	public void onResume() {
		super.onResume();
        if (!getMyApplication().isApplicationInitializing()) {
			updateMapImage();
		}

    }

	@Override
	public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.findViewById(R.id.map_image).setVisibility(View.GONE);
		if (getMyApplication().isApplicationInitializing()) {
			getMyApplication().checkApplicationIsBeingInitialized(getActivity(), (TextView) view.findViewById(R.id.ProgressMessage),
					(ProgressBar) view.findViewById(R.id.ProgressBar), new Runnable() {
						@Override
						public void run() {
							applicationInitialized(view);
						}
					});
		} else {
			applicationInitialized(view);
		}
	}

	private void applicationInitialized(View view) {
		updateMapImage();
		view.findViewById(R.id.loading).setVisibility(View.GONE);
		MainMenuActivity mainMenuActivity = ((MainMenuActivity) getActivity());
		if (mainMenuActivity != null) {
			if (System.currentTimeMillis() - getMyApplication().getSettings().LAST_UPDATES_CARD_REFRESH.get()
					> CARD_INTERVAL_UPDATE && getMyApplication().getSettings().isInternetConnectionAvailable(true)) {
				getMyApplication().getSettings().LAST_UPDATES_CARD_REFRESH.set(System.currentTimeMillis());
				mainMenuActivity.updateDownloads();
			}
			view.findViewById(R.id.map_image).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void tileDownloaded(DownloadRequest request) {
		if (request != null && !request.error && request.fileToSave != null) {
			ResourceManager mgr = getMyApplication().getResourceManager();
			mgr.tileDownloaded(request);
		}
		setMapImage(getMyApplication().getResourceManager().getRenderer().getBitmap());
	}

	@SuppressWarnings("deprecation")
	private void updateMapImage() {
		MapRenderRepositories repositories = getMyApplication().getResourceManager().getRenderer();
		if(repositories.getBitmap() != null) {
			setMapImage(repositories.getBitmap());
		} else {

			LatLon lm = getMyApplication().getSettings().getLastKnownMapLocation();
			int zm = getMyApplication().getSettings().getLastKnownMapZoom();

			WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			int height = display.getHeight(); // (int) getActivity().getResources().getDimension(R.dimen.dashMapHeight);
			int width = display.getWidth();

			WindowManager mgr = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			mgr.getDefaultDisplay().getMetrics(dm);
			RotatedTileBox currentViewport = new RotatedTileBox.RotatedTileBoxBuilder().setZoom(zm)
					.setLocation(lm.getLatitude(), lm.getLongitude()).setPixelDimensions(width, height).build();
			currentViewport.setDensity(dm.density);
			currentViewport.setMapDensity(getSettingsMapDensity(dm.density));
			repositories.loadMap(currentViewport, getMyApplication().getResourceManager().getMapTileDownloader()
					.getDownloaderCallbacks());
		}
	}
	
	public double getSettingsMapDensity(double density) {
		return (getMyApplication().getSettings().MAP_DENSITY.get()) * Math.max(1, density);
	}
}
