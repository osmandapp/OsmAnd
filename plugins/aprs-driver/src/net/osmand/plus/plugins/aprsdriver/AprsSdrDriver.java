package net.osmand.plus.plugins.aprsdriver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.util.HashMap;

/**
 * Android USB Host API integration for RTL2832U family devices.
 */
public class AprsSdrDriver {

	private static final Log LOG = PlatformUtil.getLog(AprsSdrDriver.class);
	private static final String ACTION_USB_PERMISSION = "net.osmand.aprs.USB_PERMISSION";

	private static final int VID_RTL = 0x0BDA;
	private static final int[] PIDS = {0x2831, 0x2832, 0x2838};

	public interface DeviceListener {
		void onDeviceReady(@NonNull UsbDeviceConnection connection, @NonNull UsbEndpoint bulkIn);
		void onDeviceDenied(@NonNull UsbDevice device);
		void onNoDevice();
	}

	private final Context context;
	private final UsbManager usbManager;
	private DeviceListener listener;
	private BroadcastReceiver permissionReceiver;

	public AprsSdrDriver(@NonNull Context context) {
		this.context = context.getApplicationContext();
		this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
	}

	public void setListener(@Nullable DeviceListener listener) {
		this.listener = listener;
	}

	public static boolean isSupportedDevice(@NonNull UsbDevice device) {
		if (device.getVendorId() != VID_RTL) {
			return false;
		}
		int pid = device.getProductId();
		for (int supported : PIDS) {
			if (pid == supported) {
				return true;
			}
		}
		return false;
	}

	public void requestDeviceAccess() {
		UsbDevice device = findFirstSupported();
		if (device == null) {
			if (listener != null) {
				listener.onNoDevice();
			}
			return;
		}
		if (usbManager.hasPermission(device)) {
			openDevice(device);
			return;
		}
		registerPermissionReceiver(device);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0,
				new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
		usbManager.requestPermission(device, pi);
	}

	@Nullable
	private UsbDevice findFirstSupported() {
		HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
		for (UsbDevice d : devices.values()) {
			if (isSupportedDevice(d)) {
				return d;
			}
		}
		return null;
	}

	private void registerPermissionReceiver(@NonNull UsbDevice device) {
		if (permissionReceiver != null) {
			return;
		}
		permissionReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context ctx, Intent intent) {
				if (!ACTION_USB_PERMISSION.equals(intent.getAction())) {
					return;
				}
				UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (dev == null) {
					return;
				}
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					openDevice(dev);
				} else if (listener != null) {
					listener.onDeviceDenied(dev);
					LOG.warn("USB permission denied for RTL-SDR device");
				}
			}
		};
		context.registerReceiver(permissionReceiver, new IntentFilter(ACTION_USB_PERMISSION));
	}

	private void openDevice(@NonNull UsbDevice device) {
		UsbDeviceConnection conn = usbManager.openDevice(device);
		if (conn == null) {
			LOG.warn("Failed to open USB device");
			return;
		}
		UsbInterface intf = device.getInterface(0);
		if (!conn.claimInterface(intf, true)) {
			LOG.warn("Failed to claim USB interface");
			conn.close();
			return;
		}
		UsbEndpoint bulkIn = null;
		for (int i = 0; i < intf.getEndpointCount(); i++) {
			UsbEndpoint ep = intf.getEndpoint(i);
			if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
					&& ep.getDirection() == UsbConstants.USB_DIR_IN) {
				bulkIn = ep;
				break;
			}
		}
		if (bulkIn == null) {
			LOG.warn("No bulk IN endpoint found");
			conn.close();
			return;
		}
		if (listener != null) {
			listener.onDeviceReady(conn, bulkIn);
		}
	}

	public void shutdown() {
		if (permissionReceiver != null) {
			try {
				context.unregisterReceiver(permissionReceiver);
			} catch (Exception ignored) {
			}
			permissionReceiver = null;
		}
	}
}
