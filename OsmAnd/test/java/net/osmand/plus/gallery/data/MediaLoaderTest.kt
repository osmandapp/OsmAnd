package net.osmand.plus.gallery.data

import net.osmand.data.LatLon
import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.shared.media.domain.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MediaLoaderTest {

	private class Holder(override val isComplete: Boolean) : MediaHolder {
		override fun getItems(): List<MediaItem> = emptyList()
	}

	private class CountingDelegate(val results: ArrayDeque<MediaHolder>) : MediaLoadDelegate {
		var loads = 0
		var online = true

		override fun canRefresh() = online

		override fun load(
			key: GalleryKey, onStarted: () -> Unit, onResult: (MediaHolder) -> Unit, onError: () -> Unit
		) {
			loads++
			onResult(results.removeFirst())
		}
	}

	private class Recorder : MediaLoadListener {
		val loaded = mutableListOf<MediaHolder>()

		override fun onLoaded(key: GalleryKey, holder: MediaHolder) {
			loaded.add(holder)
		}
	}

	private val key = GalleryKey.Location(LatLon(48.85, 2.34), emptyMap())
	private lateinit var repository: GalleryRepository
	private lateinit var loader: MediaLoader

	@Before
	fun setUp() {
		repository = GalleryRepository(MediaLoadStateRegistry())
		loader = MediaLoader(repository)
	}

	private fun register(vararg results: MediaHolder): CountingDelegate {
		val delegate = CountingDelegate(ArrayDeque(results.toList()))
		loader.registerDelegate(GalleryKey.Location::class, delegate)
		return delegate
	}

	@Test
	fun completeResultIsServedFromTheCacheWithoutAnotherLoad() {
		val delegate = register(Holder(isComplete = true))
		val first = Recorder()
		val second = Recorder()

		loader.load(key, first)
		loader.load(key, second)

		assertEquals(1, delegate.loads)
		assertSame(first.loaded.single(), second.loaded.single())
	}

	@Test
	fun incompleteResultIsShownButLoadedAgainOnTheNextRequest() {
		val partial = Holder(isComplete = false)
		val full = Holder(isComplete = true)
		val delegate = register(partial, full)
		val first = Recorder()
		val second = Recorder()

		loader.load(key, first)
		assertSame(partial, first.loaded.single())

		loader.load(key, second)
		assertEquals(2, delegate.loads)
		assertSame(full, second.loaded.single())
	}

	@Test
	fun theCompleteResultReplacesTheIncompleteOneInTheRepository() {
		val partial = Holder(isComplete = false)
		val full = Holder(isComplete = true)
		register(partial, full)

		loader.load(key, Recorder())
		assertSame(partial, repository.get(key))
		loader.load(key, Recorder())
		assertSame(full, repository.get(key))
		assertTrue(repository.get(key)!!.isComplete)
	}

	@Test
	fun anIncompleteResultStaysIncompleteUntilALoadSucceeds() {
		val delegate = register(Holder(false), Holder(false), Holder(true))

		repeat(3) { loader.load(key, Recorder()) }
		assertEquals(3, delegate.loads)
		assertTrue(repository.get(key)!!.isComplete)

		loader.load(key, Recorder())
		assertEquals(3, delegate.loads)
		assertFalse(repository.get(key) == null)
	}

	@Test
	fun anIncompleteResultIsKeptOfflineInsteadOfBeingLoadedAgain() {
		val partial = Holder(isComplete = false)
		val delegate = register(partial, Holder(isComplete = false))
		loader.load(key, Recorder())

		delegate.online = false
		val offline = Recorder()
		loader.load(key, offline)

		assertEquals(1, delegate.loads)
		assertSame(partial, offline.loaded.single())
		assertSame(partial, repository.get(key))
	}

	@Test
	fun anIncompleteResultIsLoadedAgainWhenTheConnectionReturns() {
		val full = Holder(isComplete = true)
		val delegate = register(Holder(isComplete = false), full)
		loader.load(key, Recorder())
		delegate.online = false
		loader.load(key, Recorder())

		delegate.online = true
		val online = Recorder()
		loader.load(key, online)

		assertEquals(2, delegate.loads)
		assertSame(full, online.loaded.single())
	}

	@Test
	fun aFirstLoadOfflineAsksTheDelegateOnceAndRetriesOnline() {
		val delegate = register(Holder(isComplete = false), Holder(isComplete = true))
		delegate.online = false

		loader.load(key, Recorder())
		loader.load(key, Recorder())
		assertEquals(1, delegate.loads)

		delegate.online = true
		loader.load(key, Recorder())
		assertEquals(2, delegate.loads)
		assertTrue(repository.get(key)!!.isComplete)
	}

	@Test
	fun reloadIgnoresTheCachedResultEvenOffline() {
		val fromDisk = Holder(isComplete = false)
		val delegate = register(Holder(isComplete = false), fromDisk)
		loader.load(key, Recorder())
		delegate.online = false

		val reloaded = Recorder()
		loader.reload(key, reloaded)

		assertEquals(2, delegate.loads)
		assertSame(fromDisk, reloaded.loaded.single())
	}
}
