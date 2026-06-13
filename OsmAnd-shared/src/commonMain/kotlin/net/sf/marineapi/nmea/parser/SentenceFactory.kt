/*
 * SentenceFactory.java
 * Copyright (C) 2010 Kimmo Tuukkanen
 *
 * This file is part of Java Marine API.
 * <http://ktuukkan.github.io/marine-api/>
 *
 * Java Marine API is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Java Marine API is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java Marine API. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.marineapi.nmea.parser

import net.sf.marineapi.nmea.sentence.Sentence
import net.sf.marineapi.nmea.sentence.SentenceId
import net.sf.marineapi.nmea.sentence.TalkerId
import co.touchlab.stately.collections.ConcurrentMutableMap

/**
 * Factory for creating sentence parsers.
 *
 *
 * Custom parsers may be implemented and registered in the factory at runtime
 * by following these steps:
 *
 *  1. Define a sentence interface by extending the [Sentence] interface
 * (e.g. `com.acme.XYZSentence`).
 *  1. Implement the interface in a class that extends [SentenceParser],
 * (e.g. `com.acme.XYZParser`).
 *  1. Use the protected getters and setters in `SentenceParser` to
 * read and write sentence data.
 *  1. Add a constructor in `XYZParser` with `String`
 * parameter, i.e. the sentence to be parsed. Pass this parameter to
 * [SentenceParser] with expected sentence
 * type (e.g. `"XYZ"`).
 *  1. Add another constructor with [TalkerId] parameter. Pass this
 * parameter to [SentenceParser]
 * with sentence type and the expected number of data fields.
 *  1. Register `XYZParser` in `SentenceFactory` by using
 * the [.registerParser] method.
 *  1. Use [SentenceFactory.createParser] or
 * [SentenceFactory.createParser] to obtain an instance
 * of your parser. In addition, [net.sf.marineapi.nmea.io.SentenceReader]
 * will now dispatch instances of `XYZSentence` when "XYZ" sentences
 * are read from the data source.
 *
 *
 *
 * Notice that there is no need to compile the whole library and the added
 * parser source code may be located in your own codebase. Additionally, it is
 * also possible to override any existing parsers of the library as needed.
 *
 *
 * @author Kimmo Tuukkanen
 * @author Gunnar Hillert
 */
class SentenceFactory private constructor() {
    /**
     * Constructor.
     */
    init {
        reset()
    }

    /**
     * Creates a parser for specified NMEA 0183 sentence String. The parser
     * implementation is selected from registered parsers according to sentence
     * type. The returned instance must be cast in to correct sentence
     * interface, for which the type should first be checked by using the
     * [Sentence.getSentenceId] method.
     *
     * @param nmea NMEA 0183 sentence String
     * @return Sentence parser instance for specified sentence
     * @throws IllegalArgumentException If there is no parser registered for the
     * given sentence type
     * @throws IllegalStateException If parser is found, but it does not
     * implement expected constructors or is otherwise unusable.
     */
    fun createParser(nmea: String): Sentence? {
        val sid = SentenceId.parseStr(nmea)
        return createParserImpl(sid, nmea)
    }

    /**
     * Creates a parser for specified talker and sentence type. The returned
     * instance needs to be cast to corresponding sentence interface.
     *
     * @param talker Sentence talker id
     * @param type Sentence type
     * @return Sentence parser of requested type.
     * @throws IllegalArgumentException If talker id is null or if there is no
     * parser registered for given sentence type.
     * @throws IllegalStateException If parser instantiation fails.
     */
    fun createParser(talker: TalkerId?, type: SentenceId): Sentence? {
        return createParser(talker, type.toString())
    }

    /**
     * Creates a parser for specified talker and sentence type. This method is
     * mainly intended to be used when custom parsers have been registered in
     * the factory. The returned instance needs to be cast to corresponding
     * sentence interface.
     *
     * @param talker Talker ID to use in parser
     * @param type Type of the parser to create
     * @return Sentence parser for requested type
     * @throws IllegalArgumentException If talker id is null or if there is no
     * parser registered for given sentence type.
     * @throws IllegalStateException If parser is found, but it does not
     * implement expected constructors or is otherwise unusable.
     */
    fun createParser(talker: TalkerId?, type: String): Sentence? {
        requireNotNull(talker) { "TalkerId cannot be null" }
        return createParserImpl(type, talker)
    }

    /**
     * Tells if the factory is able to create parser for specified sentence
     * type. All [SentenceId] enum values should result returning
     * `true` at all times.
     *
     * @param type Sentence type id, e.g. "GLL" or "GGA".
     * @return true if type is supported, otherwise false.
     */
    fun hasParser(type: String): Boolean {
        return stringConstructors.containsKey(type)
    }

    /**
     * Returns a list of currently parseable sentence types.
     *
     * @return List of sentence ids
     */
    fun listParsers(): List<String> {
        val keys: Set<String> = stringConstructors.keys
        return listOf(*keys.toTypedArray())
    }

    /**
     * Registers a sentence parser to the factory. After registration,
     * [.createParser] method can be used to obtain instances of
     * registered parser.
     *
     * @param type Sentence type id, e.g. "GGA" or "GLL".
     * @param createNmea Lambda for instantiating the parser with a String
     * @param createTalker Lambda for instantiating the parser with a TalkerId
     */
    fun registerParser(
        type: String,
        createNmea: (String) -> SentenceParser,
        createTalker: (TalkerId?) -> SentenceParser
    ) {
        stringConstructors[type] = createNmea
        talkerConstructors[type] = createTalker
    }

    /**
     * Unregisters a parser class, regardless of sentence type(s) it is
     * registered for.
     *
     * @param type Parser type implementation for `type`.
     * @see .registerParser
     */
    fun unregisterParser(type: String) {
        stringConstructors.remove(type)
        talkerConstructors.remove(type)
    }

    /**
     * Creates a new parser instance with specified parameters.
     *
     * @param sid Sentence/parser type ID, e.g. "GGA" or "GLL"
     * @param param Object to pass as parameter to parser constructor
     * @return Sentence parser
     */
    private fun createParserImpl(sid: String, param: Any): Sentence? {
        if (!hasParser(sid)) {
            val msg = "Parser for type '$sid' not found"
            throw UnsupportedSentenceException(msg)
        }
        
        return if (param is String) {
            stringConstructors[sid]!!.invoke(param)
        } else if (param is TalkerId) {
            talkerConstructors[sid]!!.invoke(param)
        } else {
            throw IllegalStateException("Unsupported param type")
        }
    }

    /**
     * Resets the factory in it's initial state, i.e. restores and removes all
     * parsers the have been either removed or added.
     *
     */
    fun reset() {
        stringConstructors.clear()
        talkerConstructors.clear()
        registerParser("VDM", { s -> VDMParser(s) }, { t -> VDMParser(t) })
        registerParser("VDO", { s -> VDOParser(s) }, { t -> VDOParser(t) })
    }

    companion object {
        // map that holds registered sentence types and parser builders
        private val stringConstructors: ConcurrentMutableMap<String, (String) -> SentenceParser> = ConcurrentMutableMap()
        private val talkerConstructors: ConcurrentMutableMap<String, (TalkerId?) -> SentenceParser> = ConcurrentMutableMap()

        /**
         * Returns the singleton instance of `SentenceFactory`.
         *
         * @return SentenceFactory instance
         */
        // singleton factory instance
        val instance = SentenceFactory()
    }
}