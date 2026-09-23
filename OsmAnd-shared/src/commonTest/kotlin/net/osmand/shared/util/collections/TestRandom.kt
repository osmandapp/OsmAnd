package net.osmand.shared.util.collections

/**
 * Small deterministic PRNG shared by the collection tests and benchmarks.
 *
 * `kotlin.random.Random` is deterministic per seed too, but this xorshift keeps benchmark key
 * generation out of the measured cost and produces the exact same stream on every platform, so JVM,
 * Android and iOS numbers describe the same workload.
 */
class XorShiftRandom(seed: Long) {

	private var state: Long = if (seed == 0L) 0x2545F4914F6CDD1DL else seed

	fun nextLong(): Long {
		var x = state
		x = x xor (x shl 13)
		x = x xor (x ushr 7)
		x = x xor (x shl 17)
		state = x
		return x
	}

	/** Non negative value below [bound]. */
	fun nextLong(bound: Long): Long {
		require(bound > 0) { "bound must be positive" }
		return (nextLong() ushr 1) % bound
	}

	/** Non negative value below [bound]. */
	fun nextInt(bound: Int): Int = nextLong(bound.toLong()).toInt()
}
