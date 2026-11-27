package net.osmand.shared.obd

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Adapter initialization against scripted ELM327 adapters (issue #27240).
 *
 * The adapter is driven through [UnderlyingTransport]: every write is one command, every read
 * returns the next chunk of bytes, just like the Bluetooth stream returns whatever has arrived.
 */
class Obd2ConnectionInitTest {

	private class FakeElm327(
		/** Protocols (AT SP n) on which the car answers. */
		private val carProtocols: Set<Char> = setOf('6'),
		/** Whether "AT SP 0" auto-search finds the car's protocol. */
		private val autoSearchWorks: Boolean = true,
		/** Adapter from #27240: after SEARCHING... it only sends "00" lines to every OBD request. */
		private val answersZeros: Boolean = false
	) : UnderlyingTransport {
		val commands = mutableListOf<String>()
		private val chunks = ArrayDeque<String>()
		private var echo = true
		private var protocol = '0'
		private var searched = false

		override suspend fun write(bytes: ByteArray) {
			val command = bytes.decodeToString().trim()
			commands.add(command)
			val prefix = if (echo) "$command\r" else ""
			val key = command.replace(" ", "").uppercase()
			when {
				key == "ATZ" -> {
					echo = true
					protocol = '0'
					searched = false
					reply(prefix, "\r\rELM327 v1.5")
				}
				key == "ATE0" -> {
					echo = false
					reply(prefix, "OK")
				}
				key == "ATI" -> reply(prefix, "ELM327 v1.5")
				key == "ATDPN" -> reply(prefix, if (protocol == '0') "A${carProtocols.first()}" else "$protocol")
				key == "ATRV" -> reply(prefix, "12.6V")
				key.startsWith("ATSP") -> {
					protocol = key.last()
					searched = false
					reply(prefix, "OK")
				}
				key.startsWith("AT") -> reply(prefix, "OK")
				key.startsWith("01") -> obdRequest(prefix, key.substring(2, 4))
				else -> reply(prefix, "?")
			}
		}

		private fun obdRequest(prefix: String, pid: String) {
			val searching = protocol == '0' && !searched
			searched = true
			if (answersZeros) {
				chunks.add(prefix + (if (searching) "SEARCHING...\r" else ""))
				repeat(7) { chunks.add("00\r") }
				chunks.add("\r>")
				return
			}
			val reachable = if (protocol == '0') autoSearchWorks else protocol in carProtocols
			val head = prefix + if (searching) "SEARCHING...\r" else ""
			if (!reachable) {
				chunks.add(head + (if (protocol == '0') "UNABLE TO CONNECT" else "CAN ERROR") + "\r\r>")
				return
			}
			val data = when (pid) {
				"00" -> "BE3EB811"
				"20" -> "80000000"
				"0D" -> "2A"
				"0C" -> "1AF8"
				"05" -> "7B"
				else -> null
			}
			chunks.add(head + (if (data == null) "NO DATA" else "41$pid$data") + "\r\r>")
		}

		private fun reply(prefix: String, body: String) {
			chunks.add("$prefix$body\r\r>")
		}

		override suspend fun read(): String = chunks.removeFirstOrNull() ?: UnderlyingTransport.TIMEOUT
	}

	private fun connect(adapter: FakeElm327): Obd2Connection =
		Obd2Connection(adapter, OBDDispatcher(debug = true))

	private fun Obd2Connection.speed(): OBDResponse = runBlocking {
		run("010D", OBDCommand.OBD_SPEED_COMMAND)
	}

	@Test
	fun genuineAdapterInitializesAndReadsSpeed() = runBlocking {
		val adapter = FakeElm327()
		val connection = connect(adapter)
		assertTrue(connection.initialize())
		assertEquals(0x2A, connection.speed().result.single())
		assertTrue(adapter.commands.contains("0100"), "protocol must be checked with 0100: ${adapter.commands}")
	}

	@Test
	fun autoSearchFailsExplicitProtocolWorks() = runBlocking {
		val adapter = FakeElm327(carProtocols = setOf('5'), autoSearchWorks = false) // ISO 14230-4 KWP, as in #27240
		val connection = connect(adapter)
		assertTrue(connection.initialize(), "explicit protocols must be tried: ${adapter.commands}")
		assertFalse(connection.isFinished(), "a failed probe must not close the connection")
		assertEquals(0x2A, connection.speed().result.single())
		assertEquals("AT SP 5", adapter.commands.last { it.startsWith("AT SP") })
	}

	@Test
	fun adapterAnsweringOnlyZerosIsNotInitialized() = runBlocking {
		// #27240: init used to succeed and the widgets showed 0 / -40 / 0.00 V
		val adapter = FakeElm327(answersZeros = true)
		val connection = connect(adapter)
		assertFalse(connection.initialize(), "no protocol answers 41 00, commands: ${adapter.commands}")
		assertEquals("AT SP 0", adapter.commands.last { it.startsWith("AT SP") }, "auto search must be restored")
	}
}
