/*
 * AbstractAISMessageListener.java
 * Copyright (C) 2015 Kimmo Tuukkanen
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
package net.sf.marineapi.ais.event

import net.sf.marineapi.ais.message.AISMessage

import net.sf.marineapi.ais.parser.AISMessageFactory
import net.sf.marineapi.nmea.event.AbstractSentenceListener
import net.sf.marineapi.nmea.sentence.AISSentence




/**
 *
 *
 * Abstract listener for AIS messages. Extend this class to create a listener
 * for a specific AIS message type and register it in a
 * [net.sf.marineapi.nmea.io.SentenceReader].
 *
 *
 * To listen to all incoming AIS sentences, extend the [ ] using [AISSentence] as type. However, in this
 * case you also need to implement the message concatenation to parse messages
 * being delivered over multiple sentences.
 *
 *
 * This class is based on [AbstractSentenceListener] and thus it has the
 * same recommendations and limitations regarding the usage of generics and
 * inheritance.
 *
 *
 * @author Kimmo Tuukkanen
 * @param <T> AIS message type to be listened.
 * @see AbstractSentenceListener
 *
 * @see GenericTypeResolver
</T> */
abstract class AbstractAISMessageListener<T : AISMessage?>(val expectedMessageType: Int) : AbstractSentenceListener<AISSentence>(AISSentence::class) {
    private val queue: ArrayDeque<AISSentence> = ArrayDeque()
    private val factory: AISMessageFactory? = AISMessageFactory.instance



    /**
     *
     *
     * Invoked when [AISSentence] of any type is received. Pre-parses
     * the message to determine it's type and invokes the
     * [.onMessage] method when the type matches the generic
     * type `T`.
     *
     *
     * This method has been declared `final` to ensure the correct
     * handling of received sentences.
     */
    override fun sentenceRead(sentence: AISSentence?) {
        if (sentence!!.isFirstFragment()) {
            queue.clear()
        }
        queue.add(sentence)
        if (sentence.isLastFragment()) {
            val sentences = queue.toTypedArray()
            try {
                val message = factory!!.create(*sentences)
                if (message.messageType == expectedMessageType) {
                    onMessage(message as T)
                }
            } catch (iae: IllegalArgumentException) {
                // never mind incorrect order or unsupported message types
            }
        }
    }

    /**
     * Invoked when AIS message has been received.
     * @param msg AISMessage of type `T`
     */
    abstract fun onMessage(msg: T?)

    override fun readingPaused() {}

    override fun readingStarted() {}

    override fun readingStopped() {}
}