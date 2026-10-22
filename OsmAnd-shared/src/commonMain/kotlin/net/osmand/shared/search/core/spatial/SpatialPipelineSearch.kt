package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.NameIndexReader
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.search.core.HashQuadTree
import net.osmand.shared.search.core.HashSkipTileQuadTree
import net.osmand.shared.search.core.HashSkipTileQuadTreeJoiner
import net.osmand.shared.search.core.spatial.SpatialSearchContext.SpatialSearchStats
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtom
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtomXY
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import net.osmand.shared.util.JavaFormat
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.math.max
import kotlin.math.min

/**
 * The pipeline of the spatial search: the objects the words of the query found grouped by the words
 * each matched, the groups crossed with each other a step at a time, from the objects that match the
 * whole query alone to the combinations of several, until enough results are found.
 *
 * A copy of `SpatialPipelineSearch` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS.
 */
class SpatialPipelineSearch(ctx: SpatialSearchContext, tokens: List<SpatialSearchToken>) {

	private val ctx: SpatialPipelineContext = SpatialPipelineContext(
		if (tokens.size > SpatialPipelineObjectRes.MAX_SUPPORTED_TOKENS)
			tokens.subList(0, SpatialPipelineObjectRes.MAX_SUPPORTED_TOKENS) else tokens, ctx
	)

	class SpatialPipelineContext(tokens: List<SpatialSearchToken>, ctx: SpatialSearchContext) {
		internal val tokens: MutableList<SpatialSearchToken> = ArrayList(tokens)
		internal val searchContext: SpatialSearchContext = ctx
		internal val settings: SpatialTextSearchSettings = ctx.settings
		internal val stats: SpatialSearchStats = ctx.stats

		internal val metrics = PipelineMetrics()

		internal val objectsById = KTLongObjectMap<SpatialPipelineObjectRes>()
		// the objects in the order the words found them, where java walks objectsById in the order of its hashes
		internal val objects = ArrayList<SpatialPipelineObjectRes>()
		internal var initBuckets: List<SpatialObjectsBucket> = emptyList()

		internal var allBuckets: MutableList<SpatialObjectsBucket> = ArrayList()

		internal val results = ArrayList<SpatialSearchResultsList>()
		internal var overallResults = 0
		internal var nonCatResults = 0

		init {
			this.tokens.sortWith { o1, o2 -> o1.originalOrder.compareTo(o2.originalOrder) }
		}

		fun isCancelled(): Boolean = searchContext.isCancelled()
	}

	inner class SpatialObjectsBucket {
		val parent: SpatialObjectsBucket?
		val edge: SpatialObjectsBucket?
		val depth: Int
		var edgeIndex = -1
		// 1. virtual based on masks before materialization
		internal var potentialMasks: MasksGroupInfo? = null

		// 2. geometric actual
		var resTree: HashSkipTileQuadTree<SpatialPipelineObjectRes>? = null
		var resObjectsByMasks: KTLongObjectMap<MutableList<SpatialPipelineObjectRes>>? = null

		// Lazy init
		internal var children: MutableList<SpatialObjectsBucket>? = null

		constructor(ref: SpatialObjectsBucket) {
			this.parent = ref.parent
			this.edge = ref.edge
			this.depth = ref.depth
			this.edgeIndex = ref.edgeIndex
			this.potentialMasks = MasksGroupInfo(ref.parent != null)
		}

		constructor(edgeIndex: Int) {
			// edge itself
			this.parent = null
			this.edge = null
			this.depth = 1
			this.edgeIndex = edgeIndex
			this.potentialMasks = MasksGroupInfo(false)
		}

		constructor(parent: SpatialObjectsBucket, edge: SpatialObjectsBucket) {
			this.parent = parent
			this.edge = edge
			this.depth = parent.depth + 1
			this.potentialMasks = MasksGroupInfo(true)
			this.edgeIndex = edge.edgeIndex
		}

		fun isMaskComputed(): Boolean = resObjectsByMasks != null || potentialMasks != null

		fun isComputed(): Boolean = resObjectsByMasks != null

		fun ensureTreeBuilt(): HashSkipTileQuadTree<SpatialPipelineObjectRes>? {
			val byMasks = resObjectsByMasks
			if (resTree == null && byMasks != null && !byMasks.isEmpty()) {
				ctx.metrics.treeBuild.start()
				val tree = HashSkipTileQuadTree<SpatialPipelineObjectRes>()
				resTree = tree
				val masks = sortedKeys(byMasks)
				for (i in masks.indices) {
					val list = byMasks[masks[i]]
					if (list != null) {
						for (j in list.indices) {
							val obj = list[j]
							tree.addObject(obj, obj.bbox)
						}
					}
				}
				ctx.metrics.treeBuild.finish()
			}
			val tree = resTree
			if (tree != null && !tree.isEmpty() && !tree.isBuilt()) {
				ctx.metrics.tree++
				ctx.metrics.treeBuild.start()
				tree.build()
				ctx.metrics.treeBuild.finish()
			}
			return tree
		}

		fun isEmpty(): Boolean {
			val maskMaps = getMaskMaps()
			if (maskMaps != null) {
				return maskMaps.isEmpty()
			}
			return true
		}

		fun getMasksCount(): Int {
			val maskMaps = getMaskMaps()
			if (maskMaps != null) {
				return maskMaps.size()
			}
			return 0
		}

		fun hasFullCovered(totalTokens: Int): Boolean {
			val maskMaps = getMaskMaps()
			if (maskMaps != null) {
				val it = maskMaps.iterator()
				while (it.hasNext()) {
					it.advance()
					if (SpatialPipelineObjectRes.countCoveredTokens(it.key()) == totalTokens) {
						return true
					}
				}
			}
			return false
		}

		fun getMaskMaps(): KTLongObjectMap<*>? {
			val byMasks = resObjectsByMasks
			if (byMasks != null) {
				return byMasks
			}
			val masks = potentialMasks
			if (masks != null) {
				return masks.map
			}
			return null
		}

		fun markInitialComputed() {
			val masks = potentialMasks!!
			if (masks.onlyCounts) {
				throw IllegalStateException()
			}
			val byMasks = KTLongObjectMap<MutableList<SpatialPipelineObjectRes>>()
			this.resObjectsByMasks = byMasks
			val it = masks.map.iterator()
			while (it.hasNext()) {
				it.advance()
				byMasks.put(it.key(), it.value().objects!!)
			}
			this.potentialMasks = null
		}

		fun markComputed(
			tree: HashSkipTileQuadTree<SpatialPipelineObjectRes>?,
			objectsByMasks: KTLongObjectMap<MutableList<SpatialPipelineObjectRes>>
		) {
			this.resTree = tree
			this.resObjectsByMasks = objectsByMasks
			this.potentialMasks = null
		}

		fun addChild(child: SpatialObjectsBucket) {
			var children = this.children
			if (children == null) {
				children = ArrayList(4)
				this.children = children
			}
			children.add(child)
		}

		fun getChildren(): List<SpatialObjectsBucket> = children ?: emptyList()

		override fun toString(): String {
			val sb = StringBuilder("Bucket [$depth] {count=")
			sb.append(getMasksCount())
			var totalObjs = 0L
			val byMasks = resObjectsByMasks
			val potential = potentialMasks
			if (byMasks != null) {
				for (list in byMasks.values()) {
					totalObjs += list.size
				}
			} else if (potential != null) {
				for (list in potential.map.values()) {
					totalObjs += list.count
				}
			}
			sb.append(", objs=").append(totalObjs)
			sb.append(", masks=[")
			val masks = getMaskMaps()
			if (masks != null) {
				var first = true
				for (m in masks.keys()) {
					if (!first) {
						sb.append(", ")
					}
					sb.append(SpatialPipelineObjectRes.formatMaskTokens(m, null))
					first = false
				}
			}
			return sb.append("]}").toString()
		}
	}

	private fun computeGeoCross(b: SpatialObjectsBucket?, res: MutableList<SpatialPipelineObjectRes>?, targetTokens: Int) {
		if (b == null || b.isEmpty()) {
			return
		}
		if (b.isComputed()) {
			if (res != null) {
				collectCoverageResults(b, res, targetTokens)
			}
			return
		}
		val parent = b.parent!!
		if (!parent.isComputed()) {
			computeGeoCross(parent, null, 0)
		}
		val parentTree = parent.ensureTreeBuilt()
		val edgeTree = b.edge!!.ensureTreeBuilt()

		val resTree: HashSkipTileQuadTree<SpatialPipelineObjectRes>? = null
		val resObjectsByMasks = KTLongObjectMap<MutableList<SpatialPipelineObjectRes>>()
		if (parentTree != null && edgeTree != null && !parentTree.isEmpty() && !edgeTree.isEmpty()) {
			ctx.metrics.joins++
			ctx.metrics.join.start()
			val joiner = HashSkipTileQuadTreeJoiner(parentTree, edgeTree)
			ctx.metrics.joinCross += (parentTree.getSize() * edgeTree.getSize()).toLong()
			joiner.joinAllBuckets { e1, e2 ->
				ctx.metrics.pairsChecked++
				val obj1 = e1.obj
				val obj2 = e2.obj
				if (!SpatialPipelineObjectRes.allowed(obj1.mainMask, obj2.mainMask)) {
					return@joinAllBuckets
				}
				if (!SpatialPipelineObjectRes.extraCheck(obj1, obj2)) {
					return@joinAllBuckets // alternatives
				}
				ctx.metrics.pairsAccepted++
				val combinedMask = SpatialPipelineObjectRes.combine2BitMasks(obj1.mainMask, obj2.mainMask)
				val combinedObj = SpatialPipelineObjectRes(combinedMask, obj1, obj2)
				var list = resObjectsByMasks[combinedMask]
				if (list == null) {
					list = ArrayList()
					resObjectsByMasks.put(combinedMask, list)
				}
				list.add(combinedObj)
			}
			ctx.metrics.join.finish()
		}
		b.markComputed(resTree, resObjectsByMasks)
		if (b.getChildren().isNotEmpty()) {
			ctx.metrics.recalcMasks.start()
			for (ch in b.children!!) {
				computeMaskCross(ch)
			}
			ctx.metrics.recalcMasks.finish()
		}
		if (res != null) {
			collectCoverageResults(b, res, targetTokens)
		}
	}

	private fun collectCoverageResults(b: SpatialObjectsBucket?, res: MutableList<SpatialPipelineObjectRes>, targetTokens: Int) {
		val byMasks = b?.resObjectsByMasks
		if (byMasks == null || byMasks.isEmpty()) {
			return
		}
		val masks = sortedKeys(byMasks)
		for (i in masks.indices) {
			val mask = masks[i]
			if (SpatialPipelineObjectRes.countCoveredTokens(mask) == targetTokens) {
				val list = byMasks[mask]
				if (list != null && list.isNotEmpty()) {
					res.addAll(list)
				}
			}
		}
	}

	internal class MasksGroupInfo(val onlyCounts: Boolean) {
		val map = KTLongObjectMap<MaskGroupInfo>()

		fun totalCounts(): Long {
			val it = map.iterator()
			var t = 0L
			while (it.hasNext()) {
				it.advance()
				t += it.value().count
			}
			return t
		}

		fun add(combinedMask: Long, sz: Long) {
			var info = map[combinedMask]
			if (info == null) {
				info = MaskGroupInfo(combinedMask, 0)
				map.put(combinedMask, info)
			}
			info.count += sz
		}

		fun add(combinedMask: Long, obj: SpatialPipelineObjectRes?) {
			var info = map[combinedMask]
			if ((obj == null) != onlyCounts) {
				throw IllegalStateException()
			}
			if (info == null) {
				info = MaskGroupInfo(combinedMask, 0)
				if (!onlyCounts) {
					info.objects = ArrayList()
				}
				map.put(combinedMask, info)
			}
			info.count++
			if (obj != null) {
				info.objects!!.add(obj)
			}
		}
	}

	internal class MaskGroupInfo(mask: Long, count: Int) {
		var count: Long = count.toLong()
		var objects: MutableList<SpatialPipelineObjectRes>? = null
	}

	private fun splitBucketIntoSmallBuckets(toSplit: SpatialObjectsBucket, outputList: MutableList<SpatialObjectsBucket>) {
		if (toSplit.isEmpty()) {
			return
		}
		val totalTokens = ctx.tokens.size
		val splitByFreq = toSplit.parent == null
		var split = false
		val pMasks = toSplit.potentialMasks
		if (!toSplit.isComputed()) {
			pMasks!!
			if (toSplit.hasFullCovered(totalTokens) && pMasks.map.size() > 1) {
				split = true
			} else if (ctx.settings.PIPELINE_MAX_VIRTUAL_MASKS < pMasks.map.size()) {
				split = true
			} else if (splitByFreq && ctx.settings.PIPELINE_FREQUENT_OBJECTS_THRESHOLD < pMasks.totalCounts()) {
				split = true
			}
		}
		if (!split || toSplit.parent != null) {
			finalizeAndAddBucket(toSplit, outputList)
			return
		}
		pMasks!!
		var fullCoveredBucket = SpatialObjectsBucket(toSplit)
		var combinedRareBucket = SpatialObjectsBucket(toSplit)
		val masks = sortedKeys(pMasks.map)
		for (i in masks.indices) {
			val mask = masks[i]
			val isFullCovered = (SpatialPipelineObjectRes.countCoveredTokens(mask) == totalTokens)
			val single = splitByFreq &&
					pMasks.map[mask]!!.count > ctx.settings.PIPELINE_FREQUENT_OBJECTS_THRESHOLD
			if (single) {
				val bucket = SpatialObjectsBucket(toSplit)
				bucket.potentialMasks!!.map.put(mask, pMasks.map[mask]!!)
				finalizeAndAddBucket(bucket, outputList)
				continue
			}

			val bucket = if (isFullCovered) fullCoveredBucket else combinedRareBucket
			bucket.potentialMasks!!.map.put(mask, pMasks.map[mask]!!)
			var close = false
			if (bucket.getMasksCount() >= ctx.settings.PIPELINE_MAX_VIRTUAL_MASKS) {
				close = true
			} else if (splitByFreq &&
				bucket.potentialMasks!!.totalCounts() >= ctx.settings.PIPELINE_FREQUENT_OBJECTS_THRESHOLD
			) {
				close = true
			}
			if (close) {
				finalizeAndAddBucket(bucket, outputList)
				if (isFullCovered) {
					fullCoveredBucket = SpatialObjectsBucket(toSplit)
				} else {
					combinedRareBucket = SpatialObjectsBucket(toSplit)
				}
			}
		}
		finalizeAndAddBucket(fullCoveredBucket, outputList)
		finalizeAndAddBucket(combinedRareBucket, outputList)
	}

	private fun finalizeAndAddBucket(bucket: SpatialObjectsBucket?, outputList: MutableList<SpatialObjectsBucket>) {
		if (bucket != null && !bucket.isEmpty()) {
			bucket.parent?.addChild(bucket)
			outputList.add(bucket)
		}
	}

	private val CROSS_INTERSECT_MAX = 50L // almost never intersect more

	private fun computeChildrenMasksCross(
		bucket: SpatialObjectsBucket, edges: List<SpatialObjectsBucket>,
		nextLevel: MutableList<SpatialObjectsBucket>
	) {
		bucket.getMaskMaps() ?: throw IllegalStateException()
		for (i in edges.indices) {
			if (i < bucket.edgeIndex) {
				continue
			}
			val edge = edges[i]
			if (edge.isEmpty()) {
				continue
			}
			val child = SpatialObjectsBucket(bucket, edge)
			computeMaskCross(child)
			splitBucketIntoSmallBuckets(child, nextLevel)
		}
	}

	private fun computeMaskCross(bucket: SpatialObjectsBucket) {
		if (bucket.isComputed()) {
			return
		}
		val potential = bucket.potentialMasks!!
		val masks = potential.map.size()
		potential.map.clear()
		val pIterator = bucket.parent!!.getMaskMaps()!!.iterator()
		while (pIterator.hasNext()) {
			pIterator.advance()
			val pMask = pIterator.key()
			val eIterator = bucket.edge!!.getMaskMaps()!!.iterator()
			while (eIterator.hasNext()) {
				eIterator.advance()
				val eMask = eIterator.key()
				ctx.metrics.maskPairsEval++
				if (!SpatialPipelineObjectRes.allowed(pMask, eMask)) {
					continue
				}
				ctx.metrics.maskPairsAllowed++
				val combinedMask = SpatialPipelineObjectRes.combine2BitMasks(pMask, eMask)
				val estSize1 = estInterSize(pIterator.value())
				val estSize2 = estInterSize(eIterator.value())
				val estCross = max(
					min(estSize1, CROSS_INTERSECT_MAX) * estSize2,
					min(estSize2, CROSS_INTERSECT_MAX) * estSize1
				)
				potential.add(combinedMask, estCross)
			}
		}
		ctx.metrics.maskDelta += (potential.map.size() - masks).toLong()
		val children = bucket.children
		if (children != null) {
			for (ch in children) {
				computeMaskCross(ch)
			}
		}
	}

	fun estInterSize(v: Any?): Long {
		if (v is List<*>) {
			return v.size.toLong()
		} else if (v is MaskGroupInfo) {
			return v.count
		}
		return 1
	}

	private fun disallowPoiType(atom: NameIndexAtom, token: SpatialSearchToken): Boolean {
		if (atom.isPOI() && !atom.isPOIRef()) {
			val poiTypes = atom.poiTypes ?: return true
			var match = false
			for (k in 0 until poiTypes.size()) {
				val pType = poiTypes[k]
				if (ctx.searchContext.poiSearch.getById(pType)!!.isPlace()) {
					match = true
					break
				}
			}
			if (!match) {
				return true
			}
		}
		if (atom.isBuilding()) {
			return true
		}
		return false
	}

	private fun prepareInitialBuckets(): SpatialPipelineContext {
		val totalTokens = ctx.tokens.size
		// a query repeating words: for every token the tokens with its word
		val copies = copiesOfWords()
		// combine & merge by tokens
		for (tokenIdx in 0 until totalTokens) {
			val token = ctx.tokens[tokenIdx]
			val lastDupToken = previousCopy(copies, tokenIdx)
			val deleted = token.deletedAtoms
			for (atom in token.atoms) {
				if (deleted.contains(atom.indexInToken.toLong())) {
					continue
				}
				val existing = ctx.objectsById[atom.id]
				val noPoiType = disallowPoiType(atom, token)
				if (existing != null) {
					// "Shabu Shabu": the name has the word twice, so the copy side by side is the same name, not "8 8 ave"
					val dupToken = if (lastDupToken == tokenIdx - 1 && nameRepeatsWord(atom, token.word)) tokenIdx else lastDupToken
					existing.mergeSame(
						totalTokens, atom, tokenIdx, noPoiType, dupToken, copies,
						if (copies == null) 0 else tokensOf(atom.id)
					)
				} else {
					val obj = SpatialPipelineObjectRes(totalTokens, atom, tokenIdx, noPoiType)
					ctx.objectsById.put(atom.id, obj)
					ctx.objects.add(obj)
				}
			}
		}

		// create initial bucket & split it
		val mainBucket = SpatialObjectsBucket(0)
		for (o in ctx.objects) {
			var obj = o
			val mask = obj.mainMask
			mainBucket.potentialMasks!!.add(mask, obj)
			obj.alignOtherVariants()
			while (obj.otherVariants != null) {
				obj = obj.otherVariants!!
				mainBucket.potentialMasks!!.add(obj.mainMask, obj)
			}
		}
		val buckets = ArrayList<SpatialObjectsBucket>()
		splitBucketIntoSmallBuckets(mainBucket, buckets)
		for (i in buckets.indices) {
			val bucket = buckets[i]
			// assign correct index
			bucket.edgeIndex = i
			bucket.markInitialComputed()
		}
		ctx.initBuckets = buckets
		return ctx
	}

	/**
	 * @return for every token the tokens with the same word, null when the query repeats no word
	 */
	private fun copiesOfWords(): LongArray? {
		var copies: LongArray? = null
		for (i in ctx.tokens.indices) {
			for (j in ctx.tokens.indices) {
				if (i != j && ctx.tokens[i].word == ctx.tokens[j].word) {
					if (copies == null) {
						copies = LongArray(ctx.tokens.size)
					}
					copies[i] = copies[i] or (1L shl j)
				}
			}
		}
		return copies
	}

	// the tokens that found the object
	private fun tokensOf(objectId: Long): Long {
		var tokens = 0L
		for (i in ctx.tokens.indices) {
			val token = ctx.tokens[i]
			val atom = token.index[objectId]
			if (atom != null && !token.deletedAtoms.contains(atom.indexInToken.toLong())) {
				tokens = tokens or (1L shl i)
			}
		}
		return tokens
	}

	private fun nameRepeatsWord(atom: NameIndexAtom, word: String): Boolean {
		var count = 0
		for (w in atom.name.split(" ")) {
			if (w == word && ++count > 1) {
				return true
			}
		}
		return false
	}

	// the nearest token before with the same word, the token itself when there is none
	private fun previousCopy(copies: LongArray?, tokenIdx: Int): Int {
		val before = if (copies == null) 0L else copies[tokenIdx] and ((1L shl tokenIdx) - 1)
		return if (before == 0L) tokenIdx else 63 - before.countLeadingZeroBits()
	}

	private fun validateResultsAndFinish(preResults: List<SpatialPipelineObjectRes>, stage: Int, tokens: List<SpatialSearchToken>): Boolean {
		if (ctx.isCancelled()) {
			return true
		}
		if (preResults.isEmpty()) {
			return false
		}
		val time = nanoTime()
		var nonCategoryRes = 0
		val stageList = createResultList(tokens, limitByPrescore(limitSingleObjects(preResults)))
		stageList.loadObjectsAndCalcBuildings(ctx.searchContext)
		if (ctx.isCancelled()) {
			return true
		}
		val res = stageList.sortResults(ctx.searchContext, ctx.settings.DEDUPLICATE_RES)
		val tsize = tokens.size
		for (r in res) {
			if (!r.isPoiCategory() && r.surplusWords + r.matchedTokens() == tsize) {
				nonCategoryRes++
			}
		}
		if (res.isNotEmpty()) {
			ctx.results.add(stageList)
			ctx.overallResults += res.size
			ctx.nonCatResults += nonCategoryRes
		}
		if (ctx.stats.printLogs) {
			val compString = if (tokens.size == ctx.tokens.size) "complete" else ("partial-" + (ctx.tokens.size - tokens.size))
			println(
				"PIPELINE " + stage + " LOAD RESULTS (" + JavaFormat.fixed((nanoTime() - time) / 1e6, 1) + " ms): " +
						nonCategoryRes + " " + compString + " results."
			)
		}
		val stops = ctx.settings.MAX_PIPELINE_RES_TO_STOP
		if (stops.isNotEmpty() && nonCategoryRes >= stops[min(stops.size, stage + 1) - 1]) {
			return true
		}
		if (ctx.overallResults >= ctx.settings.MAX_PIPELINE_ANY_RES && ctx.settings.MAX_PIPELINE_ANY_RES > 0) {
			return true
		}
		return false
	}

	/**
	 * A one-word query of a popular category ("restaurant" finds 180K POIs): a POI found by its category only and
	 * without rating scores by its distance alone, so only the nearest of them are read. Everything else is read:
	 * a POI named by the word, a rated one, a street, a city.
	 */
	private fun limitSingleObjects(preResults: List<SpatialPipelineObjectRes>): List<SpatialPipelineObjectRes> {
		val limit = ctx.settings.LIMIT_READ_SINGLE_OBJECTS
		val l = ctx.searchContext.location
		if (limit <= 0 || preResults.size <= limit || ctx.tokens.size != 1 || ctx.searchContext.ranking == null
			|| l == null
		) {
			return preResults
		}
		val dist = DoubleArray(preResults.size)
		var byCategory = 0
		for (i in preResults.indices) {
			val r = preResults[i]
			val a = if (r.refs1 == null && r.refs2 == null) r.atoms[0] else null
			if (a != null && a.isPOI() && a.elo <= 0 && a.name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)) {
				dist[i] = KMapUtils.getDistance(
					l, KMapUtils.get31LatitudeY(a.coords.y16 shl 15),
					KMapUtils.get31LongitudeX(a.coords.x16 shl 15)
				)
				byCategory++
			} else {
				dist[i] = -1.0
			}
		}
		if (byCategory <= limit) {
			return preResults
		}
		val sorted = dist.copyOf()
		sorted.sort()
		val max = sorted[preResults.size - byCategory + limit - 1]
		val res = ArrayList<SpatialPipelineObjectRes>()
		for (i in preResults.indices) {
			if (dist[i] <= max) {
				res.add(preResults[i])
			}
		}
		return res
	}

	/**
	 * Only the single objects (a street with a house number to check counts as one) with the best prescore are read.
	 * Categories, combinations of several objects and pre-results with a token matched by nothing are always read:
	 * their rank depends on the objects.
	 */
	private fun limitByPrescore(preResults: List<SpatialPipelineObjectRes>): List<SpatialPipelineObjectRes> {
		val limit = ctx.settings.LIMIT_READ_OBJECTS
		val ranking = ctx.searchContext.ranking
		val l = ctx.searchContext.location
		if (limit <= 0 || preResults.size <= limit || ranking == null || l == null) {
			return preResults
		}
		val res = ArrayList<SpatialPipelineObjectRes>()
		val single = ArrayList<SpatialPipelineObjectRes>()
		for (r in preResults) {
			if (r.mainAtom.isPoiCategory() || r.refs2 != null || !oneObjectForAllTokens(r)) {
				res.add(r)
			} else {
				single.add(r)
			}
		}
		if (single.size <= limit) {
			return preResults
		}
		var queryIsKind = false
		var number = false
		for (t in ctx.tokens) {
			queryIsKind = queryIsKind or t.hasPoiCategoryKeys()
			number = number or t.word.any { it.isDigit() }
		}
		val score = DoubleArray(single.size)
		for (i in single.indices) {
			score[i] = ranking.prescore(
				single[i], l, ctx.searchContext.poiSearch, ctx.settings.MIN_ELO_RATING,
				queryIsKind, number
			)
		}
		val order = (0 until single.size).sortedWith { a, b -> score[b].compareTo(score[a]) }
		for (i in 0 until limit) {
			res.add(single[order[i]])
		}
		return res
	}

	private fun createResultList(tokens: List<SpatialSearchToken>, r: List<SpatialPipelineObjectRes>): SpatialSearchResultsList {
		val singleResults = SpatialSearchResultsList(ctx.searchContext, tokens)
		val atoms = ArrayList<NameIndexAtom>()
		for (res in r) {
			// actual size doesn't matter any more
			val z = 15
			val tileId = HashQuadTree.encodeTileId(z, res.bbox[0] shr (31 - z), res.bbox[1] shr (31 - z))

			for (refs in 0 until (if (res.refs2 != null) 2 else 1)) {
				atoms.clear()
				for (i in res.atoms.indices) {
					var atom = res.atoms[i]
					if (atom == null && res.refs1 != null && refs == 0) {
						atom = res.refs1!![i]
					} else if (atom == null && res.refs2 != null && refs == 1) {
						atom = res.refs2!![i]
					}
					if (atom != null) {
						var skip = false
						if (atom.isPoiCategory() && res.distinctObjects() > 1 && i < tokens.size) {
							// skip incomplete (not efficient to do by masks)
							skip = tokens[i].incomplete
							//|| tokens.get(i).hasPoiCategoryKeys(); // incorrect 'brand street'
							if (ctx.searchContext.poiSearch.getById(atom.id.toInt())!!.isPlace()) {
								skip = true
							}
						}
						if (!skip) {
							atoms.add(atom)
						}
					}
				}
				if (atoms.size == tokens.size) {
					singleResults.tileIds.add(tileId)
					singleResults.linearResults.addAll(atoms)
				}
			}
		}
		return singleResults
	}

	fun runPipeline(): List<SpatialSearchResultsList> {
		val tokensSize = ctx.tokens.size
		ctx.metrics.prepareTimer.start()
		prepareInitialBuckets()
		ctx.metrics.logPrepareTime(ctx)

		var depth = 0
		depth = runSearch(tokensSize, depth, ctx.initBuckets)

		if (ctx.nonCatResults <= MIN_RESULTS_ENLARGE && tokensSize > 1 && !ctx.isCancelled()) {
			if (ctx.stats.printLogs) {
				println("PIPELINE Enlarge bboxes on stage $depth")
			}
			ctx.allBuckets = ArrayList(ctx.initBuckets) // 1st stage is skipped
			val currentLevel = ctx.initBuckets
			enlargeBboxes(currentLevel)
			depth = runSearch(tokensSize, depth, currentLevel)
		}
		if (ctx.nonCatResults <= MIN_RESULTS_PARTIAL && tokensSize > 1 && !ctx.isCancelled()) {
			if (ctx.stats.printLogs) {
				println("PIPELINE LOOKUP $depth partial results")
			}
			calculatePartialCoverage(depth)
		}
		return ctx.results
	}

	private fun enlargeBboxes(currentLevel: List<SpatialObjectsBucket>) {
		for (b in currentLevel) {
			b.children = null
			b.resTree = null
			for (l in b.resObjectsByMasks!!.values()) {
				for (r in l) {
					if (r.mainAtom.isGeoArea()) {
						val v = ctx.settings.evalEnlargeBoundary(
							ctx.settings.ENLARGE_BOUNDARIES,
							NameIndexAtomXY.distanceInM(r.bbox)
						)
						if (v > 0) {
							NameIndexAtomXY.enlargeBbox(v, r.bbox)
						}
					}
				}
			}
		}
	}

	private fun runSearch(tokensSize: Int, startDepth: Int, startLevel: List<SpatialObjectsBucket>): Int {
		var depth = startDepth
		var currentLevel = startLevel
		while (depth < ctx.settings.PIPELINE_MAX_STEPS && currentLevel.isNotEmpty()) {
			ctx.metrics.resetDepth()
			if (depth > 0) {
				ctx.metrics.masksComputeTimer.start()
				val nextLevel = ArrayList<SpatialObjectsBucket>()
				for (b in currentLevel) {
					computeChildrenMasksCross(b, ctx.initBuckets, nextLevel)
				}
				ctx.metrics.logMasksCross(ctx, currentLevel, nextLevel, depth)
				currentLevel = nextLevel
			}
			ctx.allBuckets.addAll(currentLevel)
			ctx.metrics.geoComputeTimer.start()
			val res = ArrayList<SpatialPipelineObjectRes>()
			var fullCoverageCount = 0
			for (b in currentLevel) {
				if (b.hasFullCovered(tokensSize) || b.parent == null) {
					fullCoverageCount++
					computeGeoCross(b, res, tokensSize)
				}
			}
			ctx.metrics.logGeoCross(ctx, currentLevel, depth, fullCoverageCount, res.size)
			if (validateResultsAndFinish(res, depth, ctx.tokens)) {
				return depth
			}
			depth++
		}
		return depth
	}

	private fun calculatePartialCoverage(startDepth: Int): List<SpatialSearchResultsList> {
		var depth = startDepth
		val tokensSize = ctx.tokens.size
		// collect full but without refs
		val fullWithoutRefs = HashMap<Int, MutableList<SpatialPipelineObjectRes>>()
		val fullResults = ArrayList<SpatialPipelineObjectRes>()
		for (b in ctx.allBuckets) {
			if (b.hasFullCovered(tokensSize)) {
				collectCoverageResults(b, fullResults, tokensSize)
			}
		}
		for (fullResult in fullResults) {
			val noRefsMask = fullResult.maskWithoutRefs()
			if (noRefsMask != fullResult.mainMask) {
				val covered = SpatialPipelineObjectRes.countCoveredTokens(noRefsMask)
				fullResult.mainMask = noRefsMask
				fullResult.refs1 = null
				fullResult.refs2 = null
				fullWithoutRefs.getOrPut(covered) { ArrayList() }.add(fullResult)
			}
		}

		// calculate partial
		var targetTokens = tokensSize - 1
		while (targetTokens >= 1) {
			val partialRes = ArrayList<SpatialPipelineObjectRes>()
			ctx.metrics.geoComputeTimer.start()
			var proc = 0
			val currentLevel = ArrayList<SpatialObjectsBucket>()
			for (b in ctx.allBuckets) {
				if (b.hasFullCovered(targetTokens)) {
					proc++
					currentLevel.add(b)
					computeGeoCross(b, partialRes, targetTokens)
				}
			}
			ctx.metrics.logGeoCross(ctx, currentLevel, depth, proc, partialRes.size)
			val withoutRefs = fullWithoutRefs[targetTokens]
			if (withoutRefs != null) {
				partialRes.addAll(withoutRefs)
			}
			if (partialRes.isNotEmpty()) {
				val groupByTokens = KTLongObjectMap<MutableList<SpatialPipelineObjectRes>>()
				for (r in partialRes) {
					val mask = r.maskOnlyByTokens()
					groupByTokens.getOrPut(mask) { ArrayList() }.add(r)
				}
				var ex = false
				for (mask in sortedKeys(groupByTokens)) {
					val res = groupByTokens[mask]!!
					val lst = ArrayList<SpatialSearchToken>()
					for (i in 0 until tokensSize) {
						if (SpatialPipelineObjectRes.getTokenState(mask, i) != 0L) {
							lst.add(ctx.tokens[i])
						}
					}

					ex = ex or validateResultsAndFinish(res, depth, lst)
				}
				if (ex) {
					return ctx.results
				}
			}
			targetTokens--
			depth++
		}
		return ctx.results
	}

	internal class PipelineMetrics {
		var doTiming = true
		val prepareTimer = Timer()
		val masksComputeTimer = Timer()
		var maskPairsEval = 0L
		var maskPairsAllowed = 0L
		var maskDelta = 0L
		val recalcMasks = Timer()

		val geoComputeTimer = Timer()

		val treeBuild = Timer()
		var tree = 0
		val join = Timer()
		var joins = 0
		var joinCross = 0L

		var pairsChecked = 0L
		var pairsAccepted = 0L

		fun countMasks(list: List<SpatialPipelineSearch.SpatialObjectsBucket>): Int {
			var sum = 0
			for (b in list) {
				sum += b.getMasksCount()
			}
			return sum
		}

		fun logGeoCross(
			ctx: SpatialPipelineContext, buckets: List<SpatialPipelineSearch.SpatialObjectsBucket>, level: Int, fcCount: Int,
			resCount: Int
		) {
			geoComputeTimer.finish()
			if (ctx.stats.printLogs) {
				val s = "Tree Built " + g(tree.toLong()) + " (" + f(treeBuild.ms()) + " ms) | Joins " + g(joins.toLong()) +
						" (" + f(join.ms()) + " ms) | Cross objs " + g(joinCross / 1024) + "K, check " + g(pairsChecked) +
						", accept " + g(pairsAccepted) + " | Prune (" + f(recalcMasks.ms()) + " ms) " + g(maskDelta) +
						" del masks: " + g(maskPairsEval) + " eval -> " + g(maskPairsAllowed) + " accept "
				println(
					"PIPELINE " + level + " GEO CROSS (" + f(geoComputeTimer.ms()) + " ms) - " + g(resCount.toLong()) +
							" RESULTS | " + g(buckets.size.toLong()) + " (" + g(fcCount.toLong()) + " full) Buckets | " + s
				)
			}
			maskPairsEval = 0
			maskPairsAllowed = 0
			maskDelta = 0
		}

		fun logMasksCross(
			ctx: SpatialPipelineContext, currentLevel: List<SpatialPipelineSearch.SpatialObjectsBucket>,
			nextLevel: List<SpatialPipelineSearch.SpatialObjectsBucket>, stage: Int
		) {
			ctx.metrics.masksComputeTimer.finish()
			if (ctx.stats.printLogs) {
				println(
					"PIPELINE " + stage + " MASKS CROSS (" + f(masksComputeTimer.ms()) + " ms) | " +
							g(currentLevel.size.toLong()) + " -> " + g(nextLevel.size.toLong()) + " Buckets | " +
							g(countMasks(currentLevel).toLong()) + " -> " + g(countMasks(nextLevel).toLong()) +
							" Masks | Masks Cross " + g(maskPairsEval) + " eval -> " + g(maskPairsAllowed) + " accept"
				)
			}
			maskPairsEval = 0
			maskPairsAllowed = 0
			maskDelta = 0
		}

		fun logPrepareTime(ctx: SpatialPipelineContext) {
			prepareTimer.finish()
			if (ctx.stats.printLogs) {
				SpatialPipelineStats.printTree(ctx)
				println(
					"PIPELINE 0 PREPARE (" + f(prepareTimer.ms()) + " ms) " + g(ctx.initBuckets.size.toLong()) +
							" Buckets | " + g(countMasks(ctx.initBuckets).toLong()) + " masks | " +
							g(ctx.objectsById.size().toLong()) + " objects "
				)
			}
		}

		fun resetDepth() {
			treeBuild.reset()
			joins = 0
			tree = 0
			join.reset()
			recalcMasks.reset()

			geoComputeTimer.reset()
			masksComputeTimer.reset()
			joinCross = 0
			pairsChecked = 0
			pairsAccepted = 0
			maskDelta = 0
			maskPairsEval = 0
			maskPairsAllowed = 0
		}

		private fun f(ms: Double): String = JavaFormat.fixed(ms, 1)

		private fun g(value: Long): String = JavaFormat.grouped(value)

		inner class Timer {
			var time = 0L

			fun start() {
				if (doTiming) {
					time -= nanoTime()
				}
			}

			fun finish() {
				if (doTiming) {
					time += nanoTime()
				}
			}

			fun reset() {
				time = 0
			}

			fun ms(): Double = time / 1e6
		}
	}

	companion object {
		var MIN_RESULTS_ENLARGE = 0
		var MIN_RESULTS_PARTIAL = 0 // for testing now

		// where the order of the masks makes the results: the order of their values, not of their hashes as in java
		private fun sortedKeys(map: KTLongObjectMap<*>): LongArray {
			val keys = map.keys()
			keys.sort()
			return keys
		}

		private fun oneObjectForAllTokens(r: SpatialPipelineObjectRes): Boolean {
			val id = r.mainAtom.id
			for (i in r.atoms.indices) {
				val atom = r.atoms[i]
				// a token matched by a reference (a house number) is read with the object
				if (if (atom == null) r.refs1 == null || r.refs1!![i] == null else atom.id != id) {
					return false
				}
			}
			return true
		}
	}
}
