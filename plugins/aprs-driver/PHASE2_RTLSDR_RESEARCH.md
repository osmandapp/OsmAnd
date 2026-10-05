# Phase 2 — Android RTL-SDR Driver Research

## Available driver options

### 1. SDR Driver (marto/rtl_tcp_andro) — recommended external dependency

| Item | Detail |
|------|--------|
| Package | `marto.rtl_tcp_andro` |
| Play Store | https://play.google.com/store/apps/details?id=marto.rtl_tcp_andro |
| Source | https://github.com/signalwareltd/rtl_tcp_andro- (fork of martinmarinov/rtl_tcp_andro) |
| Protocol | Extended rtl_tcp over localhost TCP |
| Root required | No (Android 3.1+ USB Host API) |
| License | GPL2+ |

**Integration via intent (no bundled driver):**

```java
Intent intent = new Intent(Intent.ACTION_VIEW)
    .setData(Uri.parse("iqsrc://-f 144800000 -s 192000 -p 1234"));
startActivityForResult(intent, REQUEST_CODE);
// On RESULT_OK, connect TCP client to localhost:1234 using rtl_tcp protocol
```

The driver handles USB permission dialogs, device selection, and fd passing to native libusb.

**Alternative:** Bundle `librtlsdr` JNI (e.g. keesj/librtlsdr-android) — heavier build, full USB stack in-process.

**Recommendation for aprs-driver:** Use intent dependency on SDR Driver for production USB; implement direct USB Host API in `AprsSdrDriver` as optional path for integrated builds.

### 2. keesj/librtlsdr-android

| Item | Detail |
|------|--------|
| Source | https://github.com/keesj/librtlsdr-android |
| Approach | libusb-1.0 modified for Android fd-based device open |
| Build | JNI `.so` for arm64-v8a, armeabi-v7a, x86, x86_64 |
| No AAR published | Must compile from source |

### 3. Direct Android USB Host API (aprs-driver plan)

Implement in `AprsSdrDriver.java` without external app:
- `UsbManager.getDeviceList()` enumeration
- `UsbManager.requestPermission()` + `BroadcastReceiver` for `ACTION_USB_PERMISSION`
- Claim interface 0, locate bulk IN endpoint
- Bulk transfer loop -> `AprsSdrDsp`

Requires embedding RTL2832U control protocol (tuner init, sample rate, frequency) — substantial native code or port of osmocom rtl-sdr.

## USB VID/PID table

All listed families use **Realtek RTL2832U** USB interface:

| Device family | VID | PID(s) | Notes |
|---------------|-----|--------|-------|
| RTL-SDR Blog V3 | `0x0BDA` | `0x2838`, `0x2832` | R820T/R820T2 tuner detected at runtime |
| RTL-SDR Blog V4 | `0x0BDA` | `0x2838` | R828D tuner detected at runtime (same PID as V3) |
| Nooelec NESDR (Mini, SMArt, XTR, etc.) | `0x0BDA` | `0x2838`, `0x2832`, `0x2831` | Same chip, different tuner |
| Generic RTL2832U | `0x0BDA` | `0x2831`, `0x2832`, `0x2838` | Catch-all match on VID + any known PID |

Reference udev rules: keesj/librtlsdr-android `rtl-sdr.rules`:
```
ATTRS{idVendor}=="0bda", ATTRS{idProduct}=="2832"
ATTRS{idVendor}=="0bda", ATTRS{idProduct}=="2838"
```

V4 vs V3 cannot be distinguished by PID alone; tuner type read after open (R828D vs R820T2 register probe).

## USB permission flow

1. Register `BroadcastReceiver` for `android.hardware.usb.action.USB_DEVICE_ATTACHED` and custom action for `UsbManager.requestPermission()`
2. Filter intent by VID/PID in `device_filter.xml`
3. `PendingIntent.getBroadcast()` with `FLAG_IMMUTABLE`
4. `usbManager.requestPermission(device, pendingIntent)`
5. On grant: `usbManager.openDevice(device)`, `connection.claimInterface(intf, true)`
6. Find bulk IN endpoint (`UsbConstants.USB_DIR_IN | USB_ENDPOINT_XFER_BULK`)
7. Start `AprsSdrThread` bulk read loop
8. On deny: log, disable SDR plugin gracefully

## Gradle / native dependencies

| Approach | Dependency |
|----------|------------|
| External SDR Driver | None in Gradle; intent + TCP client only |
| Bundled librtlsdr | JNI `.so` in `jniLibs/`, NDK build of librtlsdr + libusb-andro |
| No published Maven AAR | No `implementation '...:rtlsdr:...'` exists |

For aprs-driver Phase 6:
- Primary: direct USB Host API + Java/Kotlin DSP (`AprsSdrDsp`)
- Fallback: rtl_tcp intent to external SDR Driver app for devices requiring their tuned native stack

## AndroidManifest requirements

```xml
<uses-feature android:name="android.hardware.usb.host" android:required="false"/>
<uses-permission android:name="android.permission.USB_PERMISSION"/>

<intent-filter>
    <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"/>
</intent-filter>
<meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
           android:resource="@xml/usb_device_filter"/>
```

`usb_device_filter.xml`:
```xml
<usb-device vendor-id="3034" product-id="10296"/>  <!-- 0x0BDA 0x2838 -->
<usb-device vendor-id="3034" product-id="10290"/>  <!-- 0x0BDA 0x2832 -->
<usb-device vendor-id="3034" product-id="10289"/>  <!-- 0x0BDA 0x2831 -->
```
