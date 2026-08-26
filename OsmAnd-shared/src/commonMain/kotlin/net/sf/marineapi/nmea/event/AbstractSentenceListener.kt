/* 
 * AbstractSentenceListener.java
 * Copyright (C) 2014 Kimmo Tuukkanen
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
package net.sf.marineapi.nmea.event

import net.sf.marineapi.nmea.sentence.Sentence
import kotlin.reflect.KClass

/**
 *
 *
 * Abstract listener for NMEA-0183 sentences. Extend this class to create a
 * listener for a specific sentence type and register it in a [ ]. For listeners that need to handle
 * all incoming sentences, it is recommended to implement the [ ] interface.
 *
 *
 *
 * Recommended usage:
 *
 * <pre>
 * class MyListener extends AbstractSentenceListener&lt;GGASentence&gt;
</pre> *
 *
 *
 * Notice that more advanced use of generics and inheritance may require using
 * the [.AbstractSentenceListener] constructor. For example, the
 * following example won't work because of the generic types not being available
 * at runtime:
 *
 * <pre>
 * class MyListener&lt;A, B extends Sentence&gt; extends AbstractSentenceListener&lt;B&gt;
 * ...
 * MyListener&lt;String, GGASentence&gt; ml = new MyListener&lt;&gt;();
</pre> *
 *
 *
 * Methods of the [SentenceListener] interface implemented by this class
 * are empty, except for [.sentenceRead] which is final
 * and detects the incoming sentences and casts them in correct interface
 * before calling the [.sentenceRead] method. The other methods
 * may be overridden as needed.
 *
 * @author Kimmo Tuukkanen
 * @param <T> Sentence interface to be listened.
 * @see net.sf.marineapi.nmea.io.SentenceReader
</T> */
abstract class AbstractSentenceListener<T : Sentence?>(val sentenceClass: KClass<*>) : SentenceListener {
    /**
     *
     *
     * Invoked for all received sentences. Checks the type of each sentence
     * and invokes the [.sentenceRead] if it matches the
     * listener's generic type `T`.
     *
     *
     *
     * This method has been declared `final` to ensure the correct
     * filtering of sentences.
     *
     *
     */
    override fun sentenceRead(event: SentenceEvent) {
        val sentence = event.sentence
        if (sentenceClass.isInstance(sentence)) {
            @Suppress("UNCHECKED_CAST")
            sentenceRead(sentence as T)
        }
    }

    /**
     * Invoked when sentence of type `T` is received.
     *
     * @param sentence Sentence of type `T`
     */
    abstract fun sentenceRead(sentence: T?)

    override fun readingPaused() {}

    override fun readingStarted() {}

    override fun readingStopped() {}
}