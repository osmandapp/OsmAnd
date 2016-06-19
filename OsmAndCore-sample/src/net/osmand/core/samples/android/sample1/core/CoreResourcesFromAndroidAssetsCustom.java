package net.osmand.core.samples.android.sample1.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import net.osmand.core.jni.BoolPtr;
import net.osmand.core.jni.SWIGTYPE_p_QByteArray;
import net.osmand.core.jni.SWIGTYPE_p_bool;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.interface_ICoreResourcesProvider;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.util.Algorithms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// This class provides reverse mapping from 'embed-resources.list' to files&folders scheme used by OsmAndCore_android.aar package
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class CoreResourcesFromAndroidAssetsCustom extends interface_ICoreResourcesProvider {
	private static final String TAG = "CoreResFromAndAssets";
	private static final String NATIVE_TAG = "CoreResFromAndAssets";

	private final Context _context;
	private String _bundleFilename;
	private final HashMap<String, ResourceEntry> _resources = new HashMap<String, ResourceEntry>();

	private final class ResourceData {
		public File path;
		public long offset;
		public long size;
	}

	private final class ResourceEntry {
		public ResourceData defaultVariant;
		public TreeMap<Float, ResourceData> variantsByDisplayDensityFactor;
	}

	private CoreResourcesFromAndroidAssetsCustom(final Context context) {
		_context = context;
	}

	private boolean load() {
		final AssetManager assetManager = _context.getResources().getAssets();

		PackageInfo packageInfo = null;
		try {
			packageInfo = _context.getPackageManager().getPackageInfo(_context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Failed to get own package info", e);
			return false;
		}
		_bundleFilename = packageInfo.applicationInfo.sourceDir;
		Log.i(TAG, "Located own package at '" + _bundleFilename + "'");

		// Load the index
		final List<String> resourcesInBundle = new LinkedList<String>();
		try {
			final InputStream resourcesIndexStream = assetManager.open("OsmAndCore_ResourcesBundle.index",
					AssetManager.ACCESS_BUFFER);

			final BufferedReader resourcesIndexBufferedReader = new BufferedReader(new InputStreamReader(
					resourcesIndexStream));
			String resourceInBundle;
			while ((resourceInBundle = resourcesIndexBufferedReader.readLine()) != null)
				resourcesInBundle.add(resourceInBundle);
		} catch (IOException e) {
			Log.e(TAG, "Failed to read bundle index", e);
			return false;
		}
		Log.i(TAG, "Application contains " + resourcesInBundle.size() + " resources");

		// Parse resources index
		final Pattern resourceNameWithQualifiersRegExp = Pattern.compile("(?:\\[(.*)\\]/)(.*)");
		for (String resourceInBundle : resourcesInBundle) {
			// Process resource name
			String pureResourceName = resourceInBundle;
			String[] qualifiers = null;
			final Matcher resourceNameComponentsMatcher = resourceNameWithQualifiersRegExp.matcher(resourceInBundle);
			if (resourceNameComponentsMatcher.matches()) {
				qualifiers = resourceNameComponentsMatcher.group(1).split(";");
				pureResourceName = resourceNameComponentsMatcher.group(2);
			}

			// Get location of this resource
			final String path = "OsmAndCore_ResourcesBundle/" + resourceInBundle + (resourceInBundle.endsWith(".png") ? "" : ".qz");
			final File extractedPath = ((SampleApplication) _context.getApplicationContext()).getAppPath(path);
			final ResourceData resourceData = new ResourceData();
			if (!extractedPath.exists()) {
				try {
					final AssetFileDescriptor resourceFd = assetManager.openFd(path);
					long declaredSize = resourceFd.getDeclaredLength();
					resourceData.size = resourceFd.getLength();
					resourceData.offset = resourceFd.getStartOffset();
					resourceData.path = new File(_bundleFilename);
					resourceFd.close();
					if (declaredSize != resourceData.size) {
						Log.e(NATIVE_TAG, "Declared size does not match size for '" + resourceInBundle + "'");
						continue;
					}
				} catch (FileNotFoundException e) {
					try {
						final File containgDir = extractedPath.getParentFile();
						if (containgDir != null && !containgDir.exists())
							containgDir.mkdirs();
						extractedPath.createNewFile();

						final InputStream resourceStream = assetManager.open(path, AssetManager.ACCESS_STREAMING);
						final FileOutputStream fileStream = new FileOutputStream(extractedPath);
						Algorithms.streamCopy(resourceStream, fileStream);
						Algorithms.closeStream(fileStream);
						Algorithms.closeStream(resourceStream);
					} catch (IOException e2) {
						if (extractedPath.exists())
							extractedPath.delete();

						Log.e(NATIVE_TAG, "Failed to extract '" + resourceInBundle + "'", e2);
						continue;
					}
					resourceData.offset = 0;
					resourceData.path = extractedPath;
					resourceData.size = resourceData.path.length();
				} catch (IOException e) {
					Log.e(NATIVE_TAG, "Failed to locate '" + resourceInBundle + "'", e);
					continue;
				}
			} else {
				resourceData.offset = 0;
				resourceData.path = extractedPath;
				resourceData.size = resourceData.path.length();
			}

			// Get resource entry for this resource
			ResourceEntry resourceEntry = _resources.get(pureResourceName);
			if (resourceEntry == null) {
				resourceEntry = new ResourceEntry();
				_resources.put(pureResourceName, resourceEntry);
			}
			if (qualifiers == null) {
				resourceEntry.defaultVariant = resourceData;
			} else {
				for (String qualifier : qualifiers) {
					final String[] qualifierComponents = qualifier.trim().split("=");

					if (qualifierComponents.length == 2 && qualifierComponents[0].equals("ddf")) {
						float ddfValue;
						try {
							ddfValue = Float.parseFloat(qualifierComponents[1]);
						} catch (NumberFormatException e) {
							Log.e(TAG, "Unsupported value '" + qualifierComponents[1] + "' for DDF qualifier", e);
							continue;
						}

						if (resourceEntry.variantsByDisplayDensityFactor == null)
							resourceEntry.variantsByDisplayDensityFactor = new TreeMap<Float, ResourceData>();
						resourceEntry.variantsByDisplayDensityFactor.put(ddfValue, resourceData);
					} else {
						Log.w(TAG, "Unsupported qualifier '" + qualifier.trim() + "'");
					}
				}
			}
		}

		return true;
	}

	@Override
	public SWIGTYPE_p_QByteArray getResource(String name, float displayDensityFactor, SWIGTYPE_p_bool ok_) {
		final BoolPtr ok = BoolPtr.frompointer(ok_);

		ResourceData resourceData = getResourceData(name, displayDensityFactor);
		if (resourceData == null) {
			Log.w(TAG, "Requested resource [ddf=" + displayDensityFactor + "]'" + name + "' was not found");
			if (ok != null)
				ok.assign(false);
			return SwigUtilities.emptyQByteArray();
		}
		final String dataPath = resourceData.path.getAbsolutePath();

		final SWIGTYPE_p_QByteArray data;
		if (resourceData.offset == 0 && resourceData.size == resourceData.path.length()) {
			if (!name.endsWith(".png")) {
				data = SwigUtilities.qDecompress(SwigUtilities.readEntireFile(dataPath));
			} else {
				data = SwigUtilities.readEntireFile(dataPath);
			}
		} else {
			if (!name.endsWith(".png")) {
				data = SwigUtilities.qDecompress(SwigUtilities.readPartOfFile(dataPath,
						resourceData.offset, resourceData.size));
			} else {
				data = SwigUtilities.readPartOfFile(dataPath,
						resourceData.offset, resourceData.size);
			}
		}

		if (ok != null)
			ok.assign(true);
		return data;
	}

	@Override
	public SWIGTYPE_p_QByteArray getResource(String name, SWIGTYPE_p_bool ok_) {
		final BoolPtr ok = BoolPtr.frompointer(ok_);

		final ResourceEntry resourceEntry = _resources.get(name);
		if (resourceEntry == null) {
			Log.w(TAG, "Requested resource '" + name + "' was not found");
			if (ok != null)
				ok.assign(false);
			return SwigUtilities.emptyQByteArray();
		}

		if (resourceEntry.defaultVariant == null) {
			Log.w(TAG, "Requested resource '" + name + "' was not found");
			if (ok != null)
				ok.assign(false);
			return SwigUtilities.emptyQByteArray();
		}
		System.out.println(resourceEntry.defaultVariant.path.getAbsolutePath());
		final SWIGTYPE_p_QByteArray data;
		final String dataPath = resourceEntry.defaultVariant.path.getAbsolutePath();
		if (resourceEntry.defaultVariant.offset == 0
				&& resourceEntry.defaultVariant.size == resourceEntry.defaultVariant.path.length()) {
			if (!name.endsWith(".png")) {
				data = SwigUtilities.qDecompress(SwigUtilities.readEntireFile(dataPath));
			} else {
				data = SwigUtilities.readEntireFile(dataPath);
			}
		} else {
			if (!name.endsWith(".png")) {
				data = SwigUtilities.qDecompress(SwigUtilities.readPartOfFile(dataPath,
						resourceEntry.defaultVariant.offset, resourceEntry.defaultVariant.size));
			} else {
				data = SwigUtilities.readPartOfFile(dataPath,
						resourceEntry.defaultVariant.offset, resourceEntry.defaultVariant.size);
			}
		}

		if (ok != null)
			ok.assign(true);
		return data;
	}

	@Override
	public boolean containsResource(String name, float displayDensityFactor) {
		final ResourceEntry resourceEntry = _resources.get(name);
		if (resourceEntry == null || resourceEntry.variantsByDisplayDensityFactor == null)
			return false;

		// If there's variant for any DDF, it will be used
		return true;
	}

	@Override
	public boolean containsResource(String name) {
		final ResourceEntry resourceEntry = _resources.get(name);
		if (resourceEntry == null)
			return false;

		if (resourceEntry.defaultVariant == null)
			return false;

		return true;
	}

	public ResourceData getResourceData(String name, float displayDensityFactor) {
		final ResourceEntry resourceEntry = _resources.get(name);
		if (resourceEntry == null || resourceEntry.variantsByDisplayDensityFactor == null) {
			return null;
		}

		Map.Entry<Float, ResourceData> resourceDataEntry = resourceEntry.variantsByDisplayDensityFactor
				.ceilingEntry(displayDensityFactor);
		if (resourceDataEntry == null)
			resourceDataEntry = resourceEntry.variantsByDisplayDensityFactor.lastEntry();
		ResourceData resourceData = resourceDataEntry.getValue();
		Log.d(TAG, "Using ddf=" + resourceDataEntry.getKey() + " while looking for " + displayDensityFactor + " of '"
				+ name + "'");
		System.out.println(resourceData.path.getAbsolutePath());
		return resourceData;
	}

	public Drawable getIcon(String name, float displayDensityFactor) {
		ResourceData resourceData = getResourceData(name, displayDensityFactor);
		if (resourceData != null) {
			final String dataPath = resourceData.path.getAbsolutePath();
			if (resourceData.offset == 0 && resourceData.size == resourceData.path.length()) {
				return BitmapDrawable.createFromPath(dataPath);
			} else {
				try {
					byte[] array = new byte[(int)resourceData.size];
					FileInputStream fis = new FileInputStream(dataPath);
					fis.skip((int)resourceData.offset);
					fis.read(array, 0, (int)resourceData.size);
					fis.close();
					Bitmap bitmap = BitmapFactory.decodeByteArray(array, 0, (int)resourceData.size);
					return new BitmapDrawable(_context.getResources(), bitmap);

				} catch (IOException e) {
					Log.d(TAG, "Cannot read file: " + dataPath);
				}
			}
		}
		return null;
	}

	public static CoreResourcesFromAndroidAssetsCustom loadFromCurrentApplication(final Context context) {
		final CoreResourcesFromAndroidAssetsCustom bundle = new CoreResourcesFromAndroidAssetsCustom(context);

		if (!bundle.load())
			return null;

		return bundle;
	}
}
