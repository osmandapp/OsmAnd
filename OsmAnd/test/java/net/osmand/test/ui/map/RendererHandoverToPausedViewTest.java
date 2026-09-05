package net.osmand.test.ui.map;

import static net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.test.common.AndroidTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reproduces the Android Auto crash "Swig::DirectorException: Attempt to invoke virtual method
 * 'void android.opengl.GLSurfaceView$GLThread.requestRender()' on a null object reference".
 * <p>
 * When the car session stops, NavigationSession.onStop() calls setupRenderingView(), which hands
 * the live renderer over to the map view of the phone activity and immediately updates the state
 * of the renderer (setAzimuth(0) in MapViewWithLayers.setupAtlasMapRendererView()). If the phone
 * activity is paused at that moment, setupRenderer() only creates the rendering view and leaves it
 * without a GL renderer until the activity resumes, while the handed over renderer is already able
 * to ask for a frame through IFrameUpdateRequestCallback. The callback then dereferences the
 * unstarted rendering view, and the exception escapes the SWIG director into native code, which
 * aborts the process.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class RendererHandoverToPausedViewTest extends AndroidTest {

	private static final long FRAME_TIMEOUT_MS = 60000;

	@Rule
	public ActivityScenarioRule<MapActivity> scenarioRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void frameRequestAfterHandoverToPausedView() throws Throwable {
		skipAppStartDialogs(app);

		MapRendererContext rendererContext = NativeCoreContext.getMapRendererContext();
		assertNotNull("OpenGL rendering is not in use", rendererContext);

		MapRendererView activeView = rendererContext.getMapRendererView();
		assertNotNull("Map renderer view is not set up", activeView);
		assertTrue("Renderer did not draw a frame", awaitFrame(activeView));
		float activeAzimuth = onMainSync(activeView::getAzimuth);

		// The activity was recreated while the car session was driving the map, so its map view has
		// no renderer of its own, and it is paused while the car session is running.
		AtlasMapRendererView pausedView = onMainSync(() -> {
			AtlasMapRendererView view = new AtlasMapRendererView(app);
			rendererContext.presetMapRendererOptions(view, false);
			view.handleOnPause();
			return view;
		});

		// NavigationSession.onStop() -> MapViewWithLayers.setupAtlasMapRendererView(): the renderer
		// is handed over to the paused view and its state is updated right away.
		onMainSync(() -> {
			pausedView.setupRenderer(app, 0, 0, activeView);
			// setAzimuth() has to change the state of the renderer, otherwise no frame is requested
			pausedView.setAzimuth(activeAzimuth + 30);
			return null;
		});

		// The handed over renderer keeps asking for frames from its own threads as well.
		Thread.sleep(3000);

		onMainSync(() -> {
			pausedView.stopRenderer();
			return null;
		});
	}

	private boolean awaitFrame(@androidx.annotation.NonNull MapRendererView view) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		MapRendererView.MapRendererViewListener listener = new MapRendererView.MapRendererViewListener() {
			@Override
			public void onUpdateFrame(MapRendererView mapRenderer) {
				latch.countDown();
			}

			@Override
			public void onFrameReady(MapRendererView mapRenderer) {
			}
		};
		view.addListener(listener);
		try {
			return latch.await(FRAME_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} finally {
			view.removeListener(listener);
		}
	}

	private <T> T onMainSync(@androidx.annotation.NonNull java.util.concurrent.Callable<T> callable) throws Throwable {
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Throwable> error = new AtomicReference<>();
		InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
			try {
				result.set(callable.call());
			} catch (Throwable t) {
				error.set(t);
			}
		});
		if (error.get() != null) {
			throw error.get();
		}
		return result.get();
	}
}
