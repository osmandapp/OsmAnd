package net.osmand.plus.utils;

import android.content.Context;
import android.os.StatFs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.PlatformUtil;
import net.osmand.plus.BuildConfig;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.wikivoyage.WikivoyageUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class PicassoUtils {
	private static final Log LOG = PlatformUtil.getLog(PicassoUtils.class);

	private static final String PICASSO_CACHE = "picasso-cache";
	private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
	private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
	private static PicassoUtils INSTANCE;

	private final Cache diskCache;
	private final LruCache memoryCache;

	private final Map<String, Boolean> cached = new HashMap<>();

	private PicassoUtils(@NonNull OsmandApplication app) {
		File cacheDir = createDefaultCacheDir(app);

		diskCache = new Cache(cacheDir, calculateDiskCacheSize(cacheDir));
		memoryCache = new LruCache(app);

		OkHttpClient okHttpClient = new OkHttpClient.Builder().cache(diskCache).build();
		Picasso picasso = new Picasso.Builder(app)
				.downloader(new OkHttp3Downloader(okHttpClient))
				.memoryCache(memoryCache)
				.build();

		try {
			Picasso.setSingletonInstance(picasso);
		} catch (IllegalStateException e) {
			LOG.error(e);
		}
	}

	public OkHttpClient getUnsafeOkHttpClient() {
		try {
			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(X509Certificate[] chain, String authType) {
						}

						@Override
						public void checkServerTrusted(X509Certificate[] chain, String authType) {
						}

						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return new X509Certificate[0];
						}
					}
			};
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new SecureRandom());
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
			return new OkHttpClient.Builder()
					.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
					.hostnameVerifier((hostname, session) -> true)
					.cache(diskCache)
					.build();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized static PicassoUtils getPicasso(@NonNull OsmandApplication app) {
		if (INSTANCE == null) {
			INSTANCE = new PicassoUtils(app);
		}
		return INSTANCE;
	}

	public void clearAllPicassoCache() {
		if (memoryCache != null) {
			memoryCache.clear();
		}
		if (diskCache != null) {
			try {
				diskCache.evictAll();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		cached.clear();
	}

	public Boolean isURLLoaded(@NonNull String key) {
		return cached.get(key);
	}

	public void setResultLoaded(@NonNull String key, boolean val) {
		cached.put(key, val);
	}

	public void clearCachedMap() {
		cached.clear();
	}

	public long getDiskCacheSizeBytes() throws IOException {
		return diskCache.size();
	}

	private static File createDefaultCacheDir(Context context) {
		File cache = new File(context.getApplicationContext().getCacheDir(), PICASSO_CACHE);
		if (!cache.exists()) {
			//noinspection ResultOfMethodCallIgnored
			cache.mkdirs();
		}
		return cache;
	}

	private static long calculateDiskCacheSize(File dir) {
		long size = MIN_DISK_CACHE_SIZE;

		try {
			StatFs statFs = new StatFs(dir.getAbsolutePath());
			long available = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
			// Target 2% of the total space.
			size = available / 50;
		} catch (IllegalArgumentException e) {
			LOG.error(e);
		}

		// Bound inside min/max size for disk cache.
		return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
	}

	public static boolean isImageUrl(@Nullable String url) {
		if (!Algorithms.isEmpty(url)) {
			String lowerCaseUrl = url.toLowerCase();
			if (lowerCaseUrl.contains(".jpg")
					|| lowerCaseUrl.contains(".jpeg")
					|| lowerCaseUrl.contains(".png")
					|| lowerCaseUrl.contains(".bmp")
					|| lowerCaseUrl.contains(".webp")) {
				return true;
			}
		}
		return false;
	}

	public static void setupImageViewByUrl(@NonNull OsmandApplication app, @Nullable AppCompatImageView imageView,
	                                       @Nullable String imageUrl, boolean useWikivoyageNetworkPolicy) {
		if (imageView == null || imageUrl == null || !isImageUrl(imageUrl)) {
			LOG.error("Invalid setupImageByUrl() call");
			return;
		}
		PicassoUtils picasso = PicassoUtils.getPicasso(app);
		RequestCreator rc = Picasso.get().load(imageUrl);
		if (useWikivoyageNetworkPolicy) {
			WikivoyageUtils.setupNetworkPolicy(app.getSettings(), rc);
		}
		rc.into(imageView, new Callback() {
			@Override
			public void onSuccess() {
				picasso.setResultLoaded(imageUrl, true);
				AndroidUiHelper.updateVisibility(imageView, true);
			}
			@Override
			public void onError(Exception e) {
				picasso.setResultLoaded(imageUrl, false);
			}
		});
	}
}
