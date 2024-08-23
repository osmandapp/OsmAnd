package net.osmand.shared.util

import android.annotation.SuppressLint
import android.content.Context
import java.lang.ref.WeakReference
import java.util.Locale

actual object Localization {

	private var context: WeakReference<Context>? = null

	private val foundCache = mutableMapOf<String, Int>()
	private val notFoundCache = mutableMapOf<String, String>()

	fun initialize(context: Context) {
		this.context = WeakReference(context)
	}

	actual fun getStringId(key: String): Int {
		val ctx = context?.get() ?: return 0
		notFoundCache[key]?.let { return 0 }
		return resolveResourceId(ctx, key)
	}

	actual fun getString(key: String): String {
		val ctx = context?.get() ?: return toHumanReadable(key)

		notFoundCache[key]?.let { return it }

		val resId = resolveResourceId(ctx, key)
		if (resId == 0) {
			return notFoundCache[key] ?: key
		}
		return ctx.getString(resId)
	}

	actual fun getString(key: String, vararg args: Any): String {
		val ctx = context?.get() ?: return toHumanReadable(key)

		notFoundCache[key]?.let { return it }

		val resId = resolveResourceId(ctx, key)
		if (resId == 0) {
			return notFoundCache[key] ?: key
		}
		val formatString = ctx.getString(resId)
		return String.format(formatString, *args)
	}

	@SuppressLint("DiscouragedApi")
	private fun resolveResourceId(ctx: Context, key: String): Int {
		val resId = foundCache.getOrPut(key) {
			val id = ctx.resources.getIdentifier(key, "string", ctx.packageName)
			if (id == 0) {
				val humanReadable = toHumanReadable(key)
				notFoundCache[key] = humanReadable
				return@getOrPut 0 // Use 0 to indicate not found
			}
			id
		}
		return resId
	}

	private fun toHumanReadable(key: String) = key.replace('_', ' ')
		.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}