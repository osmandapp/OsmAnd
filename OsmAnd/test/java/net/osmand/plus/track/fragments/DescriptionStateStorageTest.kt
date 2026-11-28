package net.osmand.plus.track.fragments

import android.os.Bundle
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.track.fragments.DescriptionStateStorage.Companion.MAX_LENGTH_IN_STATE
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DescriptionStateStorageTest {

	private val key = "description_state_storage_test"
	private val context = InstrumentationRegistry.getInstrumentation().targetContext
	private val dir = File(context.cacheDir, "description_state")

	@Before
	@After
	fun deleteTestFiles() {
		files().forEach { it.delete() }
	}

	@Test
	fun longTextIsRestoredByNewInstanceAndStateIsSmall() {
		val text = "Описание — 説明 🗺 <b>html</b>\n".repeat(60_000)
		val state = save(DescriptionStateStorage(context, key), text)

		assertTrue("state size " + parcelSize(state), parcelSize(state) < 1000)
		assertEquals(text, DescriptionStateStorage(context, key).restore(state))
	}

	@Test
	fun shortTextIsKeptInStateWithoutFile() {
		for (text in listOf("", "short", "a".repeat(MAX_LENGTH_IN_STATE))) {
			val state = save(DescriptionStateStorage(context, key), text)
			assertEquals(text, DescriptionStateStorage(context, key).restore(state))
		}
		assertEquals(0, files().size)
	}

	@Test
	fun lastTextIsRestoredWhenTextChangesBetweenSaves() {
		val storage = DescriptionStateStorage(context, key)
		save(storage, longText('a'))
		assertEquals(longText('b'), DescriptionStateStorage(context, key).restore(save(storage, longText('b'))))
		assertEquals("short", DescriptionStateStorage(context, key).restore(save(storage, "short")))
		assertEquals(longText('c'), DescriptionStateStorage(context, key).restore(save(storage, longText('c'))))
		assertEquals(1, files().size)
	}

	@Test
	fun textIsRestoredAgainAfterSecondSave() {
		val state = save(DescriptionStateStorage(context, key), longText('a'))
		val restored = DescriptionStateStorage(context, key)
		val text = restored.restore(state)
		assertEquals(longText('a'), DescriptionStateStorage(context, key).restore(save(restored, text!!)))
		assertEquals(1, files().size)
	}

	@Test
	fun clearDeletesFileAndRestoreReturnsNull() {
		val storage = DescriptionStateStorage(context, key)
		val state = save(storage, longText('a'))
		assertEquals(1, files().size)
		storage.clear()
		assertEquals(0, files().size)
		assertNull(DescriptionStateStorage(context, key).restore(state))
	}

	@Test
	fun restoreReturnsNullIfNothingWasSaved() {
		assertNull(DescriptionStateStorage(context, key).restore(Bundle()))
	}

	@Test
	fun newScreenDeletesAbandonedFileAndNeverReturnsTextOfAnotherScreen() {
		val abandonedState = save(DescriptionStateStorage(context, key), longText('a'))
		val state = save(DescriptionStateStorage(context, key), longText('b'))
		assertEquals(1, files().size)
		assertNull(DescriptionStateStorage(context, key).restore(abandonedState))
		assertEquals(longText('b'), DescriptionStateStorage(context, key).restore(state))
	}

	private fun longText(c: Char) = c.toString().repeat(MAX_LENGTH_IN_STATE + 1)

	private fun save(storage: DescriptionStateStorage, text: String) = Bundle().also { storage.save(it, text) }

	private fun files() = dir.listFiles { _, name -> name.startsWith(key) } ?: emptyArray()

	private fun parcelSize(bundle: Bundle): Int {
		val parcel = Parcel.obtain()
		try {
			parcel.writeBundle(bundle)
			return parcel.dataSize()
		} finally {
			parcel.recycle()
		}
	}
}
