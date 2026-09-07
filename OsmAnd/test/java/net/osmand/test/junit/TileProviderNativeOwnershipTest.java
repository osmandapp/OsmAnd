package net.osmand.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.core.android.NativeCore;
import net.osmand.core.jni.MapTiledCollectionProvider;
import net.osmand.core.jni.interface_MapTiledCollectionProvider;
import net.osmand.data.DataTileManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.core.AudioNotesTileProvider;
import net.osmand.plus.views.layers.core.FavoritesTileProvider;
import net.osmand.plus.views.layers.core.LocationPointsTileProvider;
import net.osmand.plus.views.layers.core.OsmBugsTileProvider;
import net.osmand.plus.views.layers.core.POITileProvider;
import net.osmand.plus.views.layers.core.TilePointsProvider;
import net.osmand.plus.views.layers.core.TransportStopsTileProvider;
import net.osmand.plus.views.layers.core.WptPtTileProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Guards the ownership contract between a Java tile-symbols provider and the native renderer.
 *
 * <p>Every {@code *TileProvider} extends the SWIG director class
 * {@code interface_MapTiledCollectionProvider} and hands a native proxy of itself to
 * {@code MapRendererView.addSymbolsProvider()}. From that moment the renderer keeps the proxy in
 * its own state and calls back into the Java object from its worker threads, also for a while
 * after {@code removeSymbolsProvider()}, so the Java object has to stay alive for as long as the
 * native side holds the proxy.
 *
 * <p>The SWIG default (and {@code swigTakeOwnership()}) gives the opposite semantics: the director
 * keeps only a JNI <em>weak</em> global reference to the Java object, and the Java finalizer
 * deletes the native director. Once the map layer drops its field, the Java object becomes
 * collectable, the finalizer frees the director, and every later renderer callback dereferences
 * freed memory - the {@code MapTiledCollectionProvider} SIGSEGV and {@code __cxa_pure_virtual}
 * crashes.
 *
 * <p>The correct idiom, already used elsewhere in the code base (see
 * {@code NativeCore.load(interface_ICoreResourcesProvider, String)} and the per-point providers in
 * {@code TransportStopsTileProvider#getTilePoints}), is {@code instantiateProxy(true)} plus
 * {@code swigReleaseOwnership()}: native takes a strong global reference and destroys the director
 * once the last native reference to the proxy is gone.
 */
@RunWith(AndroidJUnit4.class)
public class TileProviderNativeOwnershipTest {

	private static final long GC_TIMEOUT_MS = 15_000;
	private static final String SENTINEL = "sentinel";

	private OsmandApplication app;

	private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
	private final Map<Reference<?>, String> refNames = new HashMap<>();

	@Before
	public void setup() {
		Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
		app = (OsmandApplication) targetContext.getApplicationContext();

		NativeCoreContext.init(app);
		assumeTrue("OsmAndCore native library is not available on this device",
				NativeCore.isAvailable());
	}

	/**
	 * Reproduces the crash: after a provider has been handed to the renderer, dropping the last
	 * Java reference to it must not make the Java object collectable.
	 */
	@Test
	public void javaProviderSurvivesGcWhileNativeHoldsProxy() throws Exception {
		List<Handle> handles = new ArrayList<>();
		for (Callable<Handle> provider : providers().values()) {
			handles.add(provider.call());
		}

		Set<String> collected = collectGarbage();

		List<String> lost = new ArrayList<>();
		for (Handle handle : handles) {
			if (collected.contains(handle.name)) {
				lost.add(handle.name);
			}
		}
		assertTrue("Java tile providers were garbage collected while the native renderer still"
				+ " holds a proxy to them - the renderer would call into freed memory: "
				+ lost, lost.isEmpty());

		// This is what a renderer worker thread does with the proxy it still holds.
		for (Handle handle : handles) {
			assertNotNull(handle.name, handle.nativeProxy.getMinZoom());
		}
	}

	/**
	 * The mirror image of the test above: once the native side releases the proxy, the Java object
	 * must become collectable again, otherwise the crash is merely traded for a leak.
	 */
	@Test
	public void javaProviderIsReleasedWhenNativeProxyIsDestroyed() {
		Handle handle = favoritesProvider();

		// The renderer released the provider (removeSymbolsProvider() plus collection of the Java
		// wrapper), so the last native reference to the proxy is gone.
		handle.nativeProxy.delete();

		assertTrue("Java tile provider is still pinned by native after the proxy was destroyed",
				collectGarbage().contains(handle.name));
	}

	private Map<String, Callable<Handle>> providers() {
		Map<String, Callable<Handle>> providers = new LinkedHashMap<>();
		providers.put("FavoritesTileProvider", this::favoritesProvider);
		providers.put("WptPtTileProvider", () -> {
			WptPtTileProvider provider = new WptPtTileProvider(app, 0, false, null, 1f);
			return handOverToNative("WptPtTileProvider", provider, provider.getProviderInstance());
		});
		providers.put("AudioNotesTileProvider", () -> {
			AudioNotesTileProvider provider = new AudioNotesTileProvider(app, 0, 1f);
			return handOverToNative("AudioNotesTileProvider", provider, provider.getProviderInstance());
		});
		providers.put("TransportStopsTileProvider", () -> {
			TransportStopsTileProvider provider = new TransportStopsTileProvider(app, null, 0, 1f);
			return handOverToNative("TransportStopsTileProvider", provider, provider.getProviderInstance());
		});
		providers.put("POITileProvider", () -> {
			POITileProvider provider = new POITileProvider(app, null, 0, false, null, 1f, 1f);
			return handOverToNative("POITileProvider", provider, provider.getProviderInstance());
		});
		providers.put("OsmBugsTileProvider", () -> {
			OsmBugsTileProvider provider = new OsmBugsTileProvider(app, null, 0, false, 0, 1f);
			return handOverToNative("OsmBugsTileProvider", provider, provider.getProviderInstance());
		});
		providers.put("TilePointsProvider", () -> {
			TilePointsProvider<?> provider = new TilePointsProvider<>(app, new DataTileManager<>(),
					0, false, null, 1f, 1f, 0, 22);
			return handOverToNative("TilePointsProvider", provider, provider.getProviderInstance());
		});
		providers.put("LocationPointsTileProvider", () -> {
			LocationPointsTileProvider provider = new LocationPointsTileProvider(0,
					Collections.emptyList(), Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
			return handOverToNative("LocationPointsTileProvider", provider, provider.getProviderInstance());
		});
		return providers;
	}

	private Handle favoritesProvider() {
		FavoritesTileProvider provider = new FavoritesTileProvider(app, 0, false, null, 1f);
		return handOverToNative("FavoritesTileProvider", provider, provider.getProviderInstance());
	}

	/**
	 * Registers the Java provider for garbage collection watching and then forgets it, exactly like
	 * a map layer that nulls its provider field after the renderer got the proxy.
	 *
	 * <p>Every provider is created inside its own lambda on purpose: this is a debuggable build,
	 * where ART keeps every local of a running method alive until the method returns.
	 */
	private Handle handOverToNative(String name, interface_MapTiledCollectionProvider provider,
	                                MapTiledCollectionProvider nativeProxy) {
		WeakReference<Object> ref = new WeakReference<>(provider, refQueue);
		refNames.put(ref, name);
		return new Handle(name, nativeProxy, ref);
	}

	/**
	 * Runs the garbage collector until it proves it reclaimed a weakly reachable object, and
	 * returns the names of the tile providers it collected along the way.
	 *
	 * <p>Reclamation is observed through the {@link ReferenceQueue} rather than through
	 * {@code WeakReference.get()}: under ART's concurrent copying collector a {@code get()} call
	 * marks the referent, so polling with {@code get()} can keep the object alive indefinitely.
	 */
	private Set<String> collectGarbage() {
		WeakReference<Object> sentinel = newSentinel(refQueue);
		refNames.put(sentinel, SENTINEL);

		Set<String> collected = new HashSet<>();
		long deadline = System.currentTimeMillis() + GC_TIMEOUT_MS;
		while (System.currentTimeMillis() < deadline) {
			allocateGarbage();
			System.gc();
			System.runFinalization();
			try {
				Reference<?> ref = refQueue.remove(200);
				while (ref != null) {
					collected.add(refNames.get(ref));
					ref = refQueue.poll();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			if (collected.remove(SENTINEL)) {
				return collected;
			}
		}
		fail("GC did not reclaim a weakly reachable object within " + GC_TIMEOUT_MS
				+ " ms, the test is inconclusive");
		return Collections.emptySet();
	}

	private static WeakReference<Object> newSentinel(ReferenceQueue<Object> queue) {
		return new WeakReference<>(new Object(), queue);
	}

	private static void allocateGarbage() {
		// Nudge ART into an actual collection instead of a no-op System.gc().
		assertEquals(512 * 1024, new byte[512 * 1024].length);
	}

	private static final class Handle {

		final String name;
		final MapTiledCollectionProvider nativeProxy;
		@SuppressWarnings("unused") // kept reachable so that it can be enqueued
		final WeakReference<Object> javaProvider;

		Handle(String name, MapTiledCollectionProvider nativeProxy,
		       WeakReference<Object> javaProvider) {
			this.name = name;
			this.nativeProxy = nativeProxy;
			this.javaProvider = javaProvider;
		}
	}
}
