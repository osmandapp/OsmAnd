package net.osmand.plus.helpers;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.car.app.CarToast;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.IllegalFormatException;

public class ToastHelper {

	private static final Log LOG = PlatformUtil.getLog(ToastHelper.class);

	public interface ToastDisplayHandler {
		void showSimpleToast(@NonNull String text, boolean isLong);

		void showCarToast(@NonNull String text, boolean isLong);

		void showSimpleToast(@StringRes int textId, boolean isLong, Object... args);

		void showCarToast(@StringRes int textId, boolean isLong, Object... args);
	}

	private final OsmandApplication app;
	private ToastDisplayHandler displayHandler;

	public ToastHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.displayHandler = getDefaultToastHandler();
	}

	public void setDisplayHandler(@Nullable ToastDisplayHandler handler) {
		this.displayHandler = handler != null ? handler : getDefaultToastHandler();
	}

	public void showToast(@Nullable String text, boolean isLong) {
		if (!Algorithms.isEmpty(text)) {
			app.runInUIThread(() -> {
				displayHandler.showSimpleToast(text, isLong);
				displayHandler.showCarToast(text, isLong);
			});
		}
	}

	public void showToast(@StringRes int textId, boolean isLong, Object... args) {
		if (textId > 0) {
			app.runInUIThread(() -> {
				displayHandler.showSimpleToast(textId, isLong, args);
				displayHandler.showCarToast(textId, isLong, args);
			});
		}
	}

	public void showSimpleToast(@Nullable String text, boolean isLong) {
		if (!Algorithms.isEmpty(text)) {
			app.runInUIThread(() -> displayHandler.showSimpleToast(text, isLong));
		}
	}

	public void showCarToast(@Nullable String text, boolean isLong) {
		if (!Algorithms.isEmpty(text)) {
			app.runInUIThread(() -> displayHandler.showCarToast(text, isLong));
		}
	}

	@NonNull
	private ToastDisplayHandler getDefaultToastHandler() {
		return new ToastDisplayHandler() {
			@Override
			public void showSimpleToast(@NonNull String text, boolean isLong) {
				Toast.makeText(getVisualContext(app), text, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
			}

			@Override
			public void showCarToast(@NonNull String text, boolean isLong) {
				NavigationSession navigationSession = app.getCarNavigationSession();
				if (navigationSession != null && navigationSession.hasStarted()) {
					int duration = isLong ? CarToast.LENGTH_LONG : CarToast.LENGTH_SHORT;
					CarToast.makeText(navigationSession.getCarContext(), text, duration).show();
				}
			}

			@Override
			public void showSimpleToast(int textId, boolean isLong, Object... args) {
				showSimpleToast(getString(textId, args), isLong);
			}

			@Override
			public void showCarToast(int textId, boolean isLong, Object... args) {
				showCarToast(getString(textId, args), isLong);
			}

			@NonNull
			private String getString(@StringRes int textId, Object... args) {
				try {
					return args == null || args.length == 0 ? app.getString(textId) : app.getString(textId, args);
				} catch (IllegalFormatException e) {
					// a translation with an unescaped '%' must not take the app down with it
					LOG.error("Malformed format string for resource " + textId, e);
					return app.getString(textId);
				}
			}

			@NonNull
			private static Context getVisualContext(@NonNull Context context) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
					DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
					if (displayManager != null) {
						Display defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
						if (defaultDisplay != null) {
							return context.createDisplayContext(defaultDisplay)
									.createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null);
						}
					}
				}
				return context;
			}
		};
	}
}