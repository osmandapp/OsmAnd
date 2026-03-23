package net.osmand.shared.util

/**
 * LLM: ChatGPT 5.2
 * GeoPointParserSimpleS2
 * Minimal S2 CellId -> lat/lon (cell center) implementation.
 */

class KGeoPointParserSimpleS2 private constructor() {

    // ---- public API ----

    class CellId(private val id: Long) {

        companion object {

            /** "0x3f8d..." -> CellId (unsigned 64-bit stored in signed long). */
            fun fromHex(hex: String): CellId {
                val h = if (hex.startsWith("0x") || hex.startsWith("0X")) hex.substring(2) else hex
                return CellId(h.toULong(16).toLong())
            }

            /**
             * "0xAAA:0xBBB" or "AAA:BBB" -> uses the FIRST part as S2 CellId.
             */
            fun fromFtid(ftid: String): CellId {
                var first = ftid
                val colon = ftid.indexOf(':')
                if (colon >= 0) {
                    first = ftid.substring(0, colon)
                }
                return fromHex(first.trim())
            }
        }

        /** Basic structural validation (same idea as S2CellId::is_valid). */
        fun isValid(): Boolean {
            return face() < NUM_FACES && ((lowestOnBit() and 0x1555555555555555L) != 0L)
        }

        /**
         * Returns cell center as [latDeg, lonDeg].
         * This is the center of the S2 cell, NOT necessarily the POI coordinate.
         */
        fun toLatLon(): DoubleArray {
            val p = toPointRaw().normalize()
            val lat = kotlin.math.atan2(p.z, kotlin.math.sqrt(p.x * p.x + p.y * p.y))
            val lon = kotlin.math.atan2(p.y, p.x)
            return doubleArrayOf(lat * RAD_TO_DEG, lon * RAD_TO_DEG)
        }

        // ---- internals ----

        private fun face(): Int = (id ushr POS_BITS).toInt()

        private fun isLeaf(): Boolean = ((id.toInt()) and 1) != 0

        private fun lowestOnBit(): Long = id and -id

        private fun toPointRaw(): Vec3 {
            val i = MutableInt(0)
            val j = MutableInt(0)
            val face = toFaceIJ(i, j)

            // This mirrors the "cell center" handling used by S2 for non-leaf cells.
            val delta = if (isLeaf()) 1 else if (((i.v xor ((id.toInt()) ushr 2)) and 1) != 0) 2 else 0

            val si = (i.v shl 1) + delta - MAX_SIZE
            val ti = (j.v shl 1) + delta - MAX_SIZE

            return faceSiTiToXyz(face, si, ti)
        }

        private fun toFaceIJ(pi: MutableInt, pj: MutableInt): Int {
            val face = face()
            var bits = face and SWAP_MASK
            for (k in 7 downTo 0) {
                bits = getBits1(pi, pj, k, bits)
            }
            return face
        }

        private fun getBits1(i: MutableInt, j: MutableInt, k: Int, bits0: Int): Int {
            val nbits = if (k == 7) (MAX_LEVEL - 7 * LOOKUP_BITS) else LOOKUP_BITS

            var bits = bits0
            bits += (((id ushr (k * 2 * LOOKUP_BITS + 1)).toInt() and ((1 shl (2 * nbits)) - 1))) shl 2
            bits = LOOKUP_IJ[bits]

            i.v += ((bits shr (LOOKUP_BITS + 2)) shl (k * LOOKUP_BITS))
            j.v += (((bits shr 2) and ((1 shl LOOKUP_BITS) - 1)) shl (k * LOOKUP_BITS))

            return bits and (SWAP_MASK or INVERT_MASK)
        }
    }

    // ---- minimal vector math ----

    private class Vec3(val x: Double, val y: Double, val z: Double) {
        fun normalize(): Vec3 {
            val n = kotlin.math.sqrt(x * x + y * y + z * z)
            return Vec3(x / n, y / n, z / n)
        }
    }

    private class MutableInt(var v: Int)

    companion object {

        // ---- S2 projection helpers ----

        // Quadratic projection used by S2.
        private fun stToUV(s: Double): Double {
            if (s >= 0) return (1.0 / 3.0) * ((1 + s) * (1 + s) - 1)
            return (1.0 / 3.0) * (1 - (1 - s) * (1 - s))
        }

        private fun faceUvToXyz(face: Int, u: Double, v: Double): Vec3 {
            return when (face) {
                0 -> Vec3(1.0, u, v)
                1 -> Vec3(-u, 1.0, v)
                2 -> Vec3(-u, -v, 1.0)
                3 -> Vec3(-1.0, -v, -u)
                4 -> Vec3(v, -1.0, -u)
                else -> Vec3(v, u, -1.0)
            }
        }

        private fun faceSiTiToXyz(face: Int, si: Int, ti: Int): Vec3 {
            val kScale = 1.0 / MAX_SIZE
            val u = stToUV(kScale * si)
            val v = stToUV(kScale * ti)
            return faceUvToXyz(face, u, v)
        }

        // ---- Hilbert lookup tables (same concept as S2) ----

        private const val MAX_LEVEL = 30
        private const val POS_BITS = 2 * MAX_LEVEL + 1 // 61
        private const val NUM_FACES = 6
        private const val MAX_SIZE = 1 shl MAX_LEVEL

        private const val LOOKUP_BITS = 4
        private const val SWAP_MASK = 0x01
        private const val INVERT_MASK = 0x02

        private val LOOKUP_IJ = IntArray(1 shl (2 * LOOKUP_BITS + 2))

        private val POS_TO_ORIENTATION = intArrayOf(SWAP_MASK, 0, 0, INVERT_MASK or SWAP_MASK)

        private val POS_TO_IJ = arrayOf(
            intArrayOf(0, 1, 3, 2),
            intArrayOf(0, 2, 3, 1),
            intArrayOf(3, 2, 0, 1),
            intArrayOf(3, 1, 0, 2)
        )

        private fun posToOrientation(position: Int): Int = POS_TO_ORIENTATION[position and 3]
        private fun posToIJ(orientation: Int, position: Int): Int = POS_TO_IJ[orientation and 3][position and 3]

        init {
            initLookupCell(0, 0, 0, 0, 0, 0)
            initLookupCell(0, 0, 0, SWAP_MASK, 0, SWAP_MASK)
            initLookupCell(0, 0, 0, INVERT_MASK, 0, INVERT_MASK)
            initLookupCell(0, 0, 0, SWAP_MASK or INVERT_MASK, 0, SWAP_MASK or INVERT_MASK)
        }

        private fun initLookupCell(
            levelValue: Int,
            iValue: Int,
            jValue: Int,
            origOrientation: Int,
            Value: Int,
            orientation: Int
        ) {
            var level = levelValue
            var i = iValue
            var j = jValue
            var pos = Value

            if (level == LOOKUP_BITS) {
                val ij = (i shl LOOKUP_BITS) + j
                LOOKUP_IJ[(pos shl 2) + origOrientation] = (ij shl 2) + orientation
            } else {
                level++
                i = i shl 1
                j = j shl 1
                pos = pos shl 2
                for (subPos in 0 until 4) {
                    val ij = posToIJ(orientation, subPos)
                    val orientationMask = posToOrientation(subPos)
                    initLookupCell(
                        level,
                        i + (ij ushr 1),
                        j + (ij and 1),
                        origOrientation,
                        pos + subPos,
                        orientation xor orientationMask
                    )
                }
            }
        }

        private const val RAD_TO_DEG = 180.0 / kotlin.math.PI
    }
}