package net.osmand.plus.plugins.topoprint.export

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGExternalFileResolver
import net.osmand.PlatformUtil
import org.apache.commons.logging.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.CRC32

/**
 * Rasterizes the map SVG to a print-resolution PNG via AndroidSVG.
 * The SVG stays in millimetre units; the target pixel size is imposed through
 * document width/height, so the viewBox mapping scales everything exactly.
 * CJK text falls back to the platform's system font chain.
 */
object SvgToPngRenderer {

	private val LOG: Log = PlatformUtil.getLog(SvgToPngRenderer::class.java)

	/** Resolves the map's basemap image reference relative to the SVG file. */
	private class SiblingImageResolver(private val baseDir: File?) : SVGExternalFileResolver() {
		override fun resolveImage(filename: String?): Bitmap? {
			if (filename.isNullOrBlank()) return null
			// Embedded basemap: the SVG carries the PNG as a base64 data URI.
			val marker = ";base64,"
			if (filename.startsWith("data:") && filename.contains(marker)) {
				val payload = filename.substringAfter(marker)
				val data = android.util.Base64.decode(payload, android.util.Base64.DEFAULT)
				return BitmapFactory.decodeByteArray(data, 0, data.size)
			}
			if (baseDir == null) return null
			val file = File(baseDir, filename)
			val bitmap = if (file.isFile) BitmapFactory.decodeFile(file.absolutePath) else null
			if (bitmap == null) {
				LOG.warn("basemap image not resolved: file=$file exists=${file.isFile}")
			}
			return bitmap
		}
	}

	/**
	 * Renders [svgFile] at [widthPx] x [heightPx] into [pngFile], stamping the
	 * physical print density ([dpi]) into the PNG so print tools keep the
	 * map at its paper size.
	 *
	 * @param cacheDir app-private cache directory for the intermediate
	 * compressed PNG — never the output dir, whose shared storage rejects
	 * unknown MIME types (".tmp") with EPERM
	 *
	 * @return true on success; throws on invalid input — the caller treats any
	 * failure as "SVG kept, PNG unavailable"
	 */
	fun renderToPng(svgFile: File, widthPx: Int, heightPx: Int, pngFile: File, dpi: Int,
	                cacheDir: File): Boolean {
		val svg = FileInputStream(svgFile).use { stream -> SVG.getFromInputStream(stream) }
		val resolver = SiblingImageResolver(svgFile.parentFile)
		SVG.registerExternalFileResolver(resolver)
		val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
		try {
			svg.setDocumentWidth("${widthPx}px")
			svg.setDocumentHeight("${heightPx}px")

			val canvas = Canvas(bitmap)
			// Paper is white; the SVG leaves the background transparent.
			canvas.drawColor(Color.WHITE)
			svg.renderToCanvas(canvas)

			if (dpi > 0) {
				// Compress to a temp file and splice the pHYs chunk while
				// streaming: holding the whole compressed PNG (and its array
				// copies) in the heap OOMs on 600 dpi maps — the same reason
				// embedBasemap() streams its base64. The temp file MUST live
				// in the app-private cache dir: shared-storage output dirs
				// reject unknown MIME types (".tmp") with EPERM.
				val tmpFile = File(cacheDir, pngFile.name + ".tmp")
				try {
					FileOutputStream(tmpFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
					FileInputStream(tmpFile).use { input ->
						val ihdrEnd = 8 + (4 + 4 + 13 + 4)   // signature + IHDR chunk
						val head = ByteArray(ihdrEnd)
						var off = 0
						while (off < ihdrEnd) {
							val n = input.read(head, off, ihdrEnd - off)
							if (n < 0) throw java.io.IOException("truncated PNG header")
							off += n
						}
						FileOutputStream(pngFile).use { out ->
							out.write(head)
							out.write(pngDpiChunk(dpi))
							input.copyTo(out)
						}
					}
				} finally {
					tmpFile.delete()
				}
			} else {
				FileOutputStream(pngFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
			}
			return true
		} finally {
			SVG.deregisterExternalFileResolver()
			bitmap.recycle()
		}
	}

	/**
	 * Builds the 21-byte pHYs chunk (pixels per metre on both axes). The
	 * caller splices it right after the mandatory IHDR chunk while streaming —
	 * the only structural change needed to declare the print density, since
	 * Android's PNG encoder writes no pHYs chunk and viewers would assume
	 * 72 dpi.
	 */
	private fun pngDpiChunk(dpi: Int): ByteArray {
		val ppm = Math.round(dpi / 0.0254f)
		val chunk = ByteArray(21)
		writeInt(chunk, 0, 9)                                   // data length
		System.arraycopy("pHYs".toByteArray(Charsets.US_ASCII), 0, chunk, 4, 4)
		writeInt(chunk, 8, ppm)
		writeInt(chunk, 12, ppm)
		chunk[16] = 1                                           // unit: metre
		val crc = CRC32().apply { update(chunk, 4, 13) }.value
		writeInt(chunk, 17, crc.toInt())
		return chunk
	}

	private fun writeInt(target: ByteArray, offset: Int, value: Int) {
		target[offset] = (value ushr 24).toByte()
		target[offset + 1] = (value ushr 16).toByte()
		target[offset + 2] = (value ushr 8).toByte()
		target[offset + 3] = value.toByte()
	}

	/**
	 * ARGB bitmap budget check shared by render and rasterization steps.
	 * Mirrors OffscreenMapRenderer.isBitmapSizeAllowed: on API 26+ bitmap
	 * pixels live in the native heap, so budget against the large heap and
	 * really available RAM instead of the plain memoryClass.
	 */
	fun fitsMemoryBudget(context: Context, widthPx: Int, heightPx: Int): Boolean {
		val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return true
		val bytes = widthPx.toLong() * heightPx * 4
		val heapBudget = am.largeMemoryClass * 1024L * 1024L * 7 / 10
		val info = ActivityManager.MemoryInfo()
		am.getMemoryInfo(info)
		val memBudget = (info.availMem * 0.5).toLong().coerceAtLeast(512L * 1024 * 1024)
		return bytes <= heapBudget && bytes <= memBudget
	}
}
