package net.osmand.plus.plugins.astro.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import net.osmand.plus.plugins.astro.views.StarView
import net.osmand.shared.util.LoggerFactory
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.tan

class StarMapCameraHelper(
	private val fragment: Fragment,
	private val cameraTextureView: TextureView,
	private val starView: StarView,
	private val cameraButton: ImageButton,
	private val transparencySlider: SeekBar,
	private val sliderContainer: View,
	private val resetFovButton: View,
	private val onCameraStateChanged: (Boolean) -> Unit
) {

	var isCameraOverlayEnabled = false
		private set

	var calculatedFov = 60.0 // Default fallback
		private set

	private var cameraDevice: CameraDevice? = null
	private var captureSession: CameraCaptureSession? = null
	private var previewSize: Size? = null
	private var baseTransformMatrix: Matrix? = null

	companion object {
		const val PERMISSION_REQUEST_CAMERA = 1001
		private val log = LoggerFactory.getLogger("StarMapCameraHelper")
	}

	init {
		updateCameraButtonState()

		cameraButton.setOnClickListener {
			toggleCameraOverlay()
		}

		transparencySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				cameraTextureView.alpha = progress / 100f
			}
			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		resetFovButton.setOnClickListener {
			starView.setViewAngle(calculatedFov)
			Toast.makeText(fragment.context, "FOV reset to ${String.format("%.1f", calculatedFov)}°", Toast.LENGTH_SHORT).show()
		}

		// Calculate FOV initially if camera permission is already granted (best effort)
		val context = fragment.requireContext()
		if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
			calculatedFov = calculateSensorFov()
		}
	}

	fun onResume() {
		if (isCameraOverlayEnabled) {
			openCamera()
		}
	}

	fun onPause() {
		closeCamera()
	}

	fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
		if (requestCode == PERMISSION_REQUEST_CAMERA) {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				toggleCameraOverlay() // Retry enabling
			} else {
				Toast.makeText(fragment.context, "Camera permission required for overlay", Toast.LENGTH_SHORT).show()
			}
		}
	}

	fun updateCameraZoom(fov: Double) {
		if (!isCameraOverlayEnabled || baseTransformMatrix == null || cameraTextureView.width == 0) return

		// scale = tan(baseFov/2) / tan(targetFov/2)
		// calculatedFov is our base effective FOV at 1x scale
		val baseRad = Math.toRadians(calculatedFov / 2.0)
		val targetRad = Math.toRadians(fov / 2.0)

		val scale = (tan(baseRad) / tan(targetRad)).toFloat()

		val matrix = Matrix(baseTransformMatrix)
		val centerX = cameraTextureView.width / 2f
		val centerY = cameraTextureView.height / 2f

		matrix.postScale(scale, scale, centerX, centerY)
		cameraTextureView.setTransform(matrix)
	}

	private fun toggleCameraOverlay() {
		val context = fragment.requireContext()
		if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			fragment.requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
			return
		}

		isCameraOverlayEnabled = !isCameraOverlayEnabled
		if (isCameraOverlayEnabled) {
			// Initial best guess, will be refined in configureTransform
			calculatedFov = calculateSensorFov()
			openCamera()
			cameraTextureView.visibility = View.VISIBLE
			sliderContainer.visibility = View.VISIBLE
			resetFovButton.visibility = View.VISIBLE
		} else {
			closeCamera()
			cameraTextureView.visibility = View.GONE
			sliderContainer.visibility = View.GONE
			resetFovButton.visibility = View.GONE
		}
		updateCameraButtonState()
		onCameraStateChanged(isCameraOverlayEnabled)
	}

	private fun updateCameraButtonState() {
		if (isCameraOverlayEnabled) {
			cameraButton.setColorFilter(Color.BLUE)
		} else {
			cameraButton.setColorFilter("#5f6e7c".toColorInt())
		}
	}

	private fun openCamera() {
		val context = fragment.requireContext()
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			return
		}

		if (cameraTextureView.isAvailable) {
			startCameraSession()
		} else {
			cameraTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
				@RequiresPermission(Manifest.permission.CAMERA)
				override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
					startCameraSession()
				}
				override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
					configureTransform(width, height)
				}
				override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
				override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
			}
		}
	}

	@RequiresPermission(Manifest.permission.CAMERA)
	private fun startCameraSession() {
		val manager = fragment.requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
		try {
			// Find back camera
			var cameraId: String? = null
			for (id in manager.cameraIdList) {
				val characteristics = manager.getCameraCharacteristics(id)
				if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
					cameraId = id

					// Calculate optimal preview size
					val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
					if (map != null) {
						previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
							cameraTextureView.width, cameraTextureView.height)
						// Apply transform immediately based on selected size
						configureTransform(cameraTextureView.width, cameraTextureView.height)
					}
					break
				}
			}

			if (cameraId == null) return

			manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
				override fun onOpened(camera: CameraDevice) {
					cameraDevice = camera
					createCaptureSession()
				}
				override fun onDisconnected(camera: CameraDevice) {
					camera.close()
					cameraDevice = null
				}
				override fun onError(camera: CameraDevice, error: Int) {
					camera.close()
					cameraDevice = null
				}
			}, null)
		} catch (e: Exception) {
			log.error("Failed to open camera", e)
		}
	}

	private fun chooseOptimalSize(choices: Array<Size>, viewWidth: Int, viewHeight: Int): Size {
		// Prefer sizes that match the aspect ratio of the TextureView to minimize cropping
		val targetRatio = if (viewWidth < viewHeight) {
			viewHeight.toFloat() / viewWidth
		} else {
			viewWidth.toFloat() / viewHeight
		}

		val tolerance = 0.1f
		val matchAspect = choices.filter {
			val ratio = if (it.height > 0) it.width.toFloat() / it.height else 0f
			abs(ratio - targetRatio) < tolerance
		}
		val candidates = matchAspect.ifEmpty { choices.toList() }

		// Pick largest available size within candidates to ensure quality
		return candidates.maxByOrNull { it.width * it.height } ?: choices[0]
	}

	private fun configureTransform(viewWidth: Int, viewHeight: Int) {
		val activity = fragment.activity ?: return
		if (null == previewSize || null == cameraTextureView) {
			return
		}
		val rotation = activity.windowManager.defaultDisplay.rotation
		val matrix = Matrix()
		val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
		val bufferRect =
			RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
		val centerX = viewRect.centerX()
		val centerY = viewRect.centerY()

		var scaleX = 1f
		var scaleY = 1f

		val sensorInfo = getSensorInfo()
		val fovW = sensorInfo?.fovWidth ?: 60.0
		val sensorRatio = sensorInfo?.aspectRatio ?: (4.0/3.0)
		// FOV Height: tan(Ah/2) = tan(Aw/2) / ratio
		val fovH = Math.toDegrees(2 * atan(tan(Math.toRadians(fovW / 2.0)) / sensorRatio))

		var baseFovForX = fovW

		if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
			// Landscape: Screen X aligns with Sensor Width
			baseFovForX = fovW

			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
			val scale = max(
				viewHeight.toFloat() / previewSize!!.height,
				viewWidth.toFloat() / previewSize!!.width
			)
			matrix.postScale(scale, scale, centerX, centerY)
			matrix.postRotate(90f * (rotation - 2), centerX, centerY)

			scaleX = (previewSize!!.width * scale) / viewWidth
			scaleY = (previewSize!!.height * scale) / viewHeight
		} else if (Surface.ROTATION_180 == rotation) {
			// Upside Down Portrait: Screen X aligns with Sensor Height
			baseFovForX = fovH

			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
			val scale = max(
				viewHeight.toFloat() / previewSize!!.height,
				viewWidth.toFloat() / previewSize!!.width
			)
			matrix.postScale(scale, scale, centerX, centerY)
			matrix.postRotate(180f, centerX, centerY)

			scaleX = (previewSize!!.height * scale) / viewWidth
			scaleY = (previewSize!!.width * scale) / viewHeight
		} else if (Surface.ROTATION_0 == rotation) {
			// Portrait: Screen X aligns with Sensor Height
			baseFovForX = fovH

			// Portrait mode: Camera sensor is usually landscape, so we need to rotate 90 degrees
			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
			val scale = max(
				viewHeight.toFloat() / previewSize!!.height,
				viewWidth.toFloat() / previewSize!!.width
			)
			matrix.postScale(scale, scale, centerX, centerY)

			// Update scales for FOV Refinement
			// effectively: how much did we zoom in relative to the full preview image fitting the screen?
			// The preview image is rotated. Height becomes Width.
			scaleX = (previewSize!!.height * scale) / viewWidth
			scaleY = (previewSize!!.width * scale) / viewHeight
		}
		baseTransformMatrix = Matrix(matrix)
		cameraTextureView.setTransform(matrix)

		// Update FOV based on Aspect Ratio Crop
		updateEffectiveFov(scaleX, baseFovForX)
	}

	private fun createCaptureSession() {
		try {
			val texture = cameraTextureView.surfaceTexture!!
			if (previewSize != null) {
				texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
			} else {
				texture.setDefaultBufferSize(cameraTextureView.width, cameraTextureView.height)
			}
			val surface = Surface(texture)

			val builder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
			builder?.addTarget(surface)

			cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
				override fun onConfigured(session: CameraCaptureSession) {
					captureSession = session
					builder?.build()?.let {
						session.setRepeatingRequest(it, null, null)
					}
				}
				override fun onConfigureFailed(session: CameraCaptureSession) {}
			}, null)
		} catch (e: Exception) {
			log.error("Failed to create capture session", e)
		}
	}

	private fun closeCamera() {
		captureSession?.close()
		captureSession = null
		cameraDevice?.close()
		cameraDevice = null
	}

	fun calculateCameraFov(): Double {
		return calculateSensorFov()
	}

	private fun updateEffectiveFov(scaleX: Float, baseFov: Double) {
		val halfFovRad = Math.toRadians(baseFov / 2.0)
		val tanHalfFov = tan(halfFovRad)
		val effectiveRad = 2 * atan(tanHalfFov / scaleX)
		calculatedFov = Math.toDegrees(effectiveRad)

		fragment.activity?.runOnUiThread {
			if (isCameraOverlayEnabled) {
				starView.setViewAngle(calculatedFov)
			}
		}
	}

	private data class SensorInfo(val fovWidth: Double, val aspectRatio: Double)

	private fun getSensorInfo(): SensorInfo? {
		val manager = fragment.requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
		try {
			for (id in manager.cameraIdList) {
				val characteristics = manager.getCameraCharacteristics(id)
				if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
					val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
					val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

					if (sensorSize != null && focalLengths != null && focalLengths.isNotEmpty()) {
						val w = sensorSize.width
						val h = sensorSize.height
						val f = focalLengths[0]
						val fovRad = 2 * atan(w / (2 * f))
						val fovW = Math.toDegrees(fovRad.toDouble())
						return SensorInfo(fovW, w.toDouble() / h)
					}
				}
			}
		} catch (e: Exception) {
			log.error("Failed to calculate camera FOV", e)
		}
		return null
	}

	private fun calculateSensorFov(): Double {
		return getSensorInfo()?.fovWidth ?: 60.0
	}
}