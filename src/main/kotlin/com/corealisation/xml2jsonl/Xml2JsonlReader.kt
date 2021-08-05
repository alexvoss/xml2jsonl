/*
 * Copyright (c) 2021 Alexander Voss (alex@corealisation.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.corealisation.xml2jsonl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants.*
import javax.xml.stream.XMLStreamReader


/**
 * Uses a Java StAX () parser to read XML data and re-construct individual
 * elements and their content into JSON objects. The reason to use StAX rather
 * than SAX is that it is a
 * [pull parser](https://docs.oracle.com/javase/tutorial/jaxp/stax/why.html),
 * which makes it easier to work with when processing data lazyly.
 *
 * The JSON objects are returned as a [Kotlin Flow](https://kotlinlang.org/docs/flow.html),
 * allowing them to be processed lazily in a manner similar to the Java Steam API.
 *
 * _Note_: XML parsing is not namespace-aware.
 *
 * @param[allTop] to be true if all children of the root element are to be processed (trure by default)
 * @param[procRoot] to be true if the root of the XML document is to be processed (false by default)
 * @param[tags] a list of the tag names of elements to process, matched anywhere in the document
 * @constructor Sets up the StAX event reader.
 * @author [Alex Voss](https://www.corealisation.com/alex)
 */
class Xml2JsonlReader(
    val allTop: Boolean = true,
    val procRoot: Boolean = false,
    val tags: List<String> = emptyList<String>(),
    inputStream: InputStream
)  {

    private val reader : XMLStreamReader
    private val mapper : ObjectMapper = ObjectMapper()

    private var atRoot : Boolean = true

    init {
        val xmlInputFactory : XMLInputFactory = XMLInputFactory.newInstance()
        reader = xmlInputFactory.createXMLStreamReader(inputStream)
    }

    /**
     * Return a [Flow] of [ObjectNode]s by calling [getNextObject] while the XML
     * parser produces new elements.
     */
    public fun getFlow() : Flow<ObjectNode> = flow {
        if(reader.hasNext()) {
            var json : ObjectNode? = getNextObject()
            if(json != null) {
                emit(json)
            }
        }
    }

    /**
     * Get the next [ObjectNode] to emit from the XML stream. Returns null if there are
     * no more data to be returned.
     */
    private fun getNextObject() : ObjectNode? {
        require(scanToNextElement() == START_ELEMENT) {
            "The document does not contain any elements."
        }
        if(this.atRoot) {
            this.atRoot = false
            if(this.procRoot) return convertRoot()
            if(scanToNextElement() != START_ELEMENT) return null
        }
        do {
            if(this.allTop || this.reader.localName in tags) {
                return convertElement()
            }
        } while(scanToNextElement() == START_ELEMENT)
        return null
    }

    /**
     * Ignore XML parsing events until a START_ELEMENT is encountered.
     * The cursor will be at the start of an element after this or, if there
     * is no (further) element in the document, will be at the end of the
     * document.
     */
    private fun scanToNextElement() : Int {
        var event = this.reader.next()
        while (event != START_ELEMENT) {
            event = this.reader.next()
        }
        return event
    }

    /**
     * Convert an XML element to a JSON [ObjectNode].
     */
    private fun convertElement(): ObjectNode? {

        val element : ObjectNode = this.mapper.createObjectNode()
        element.put("__t", this.reader.localName)
        element.put("__x", "")
        val childElements : ArrayNode = this.mapper.createArrayNode()
        element.replace("__c", childElements)

        addAttributes(element)
        addChildNodes(childElements)
        return element
    }

    /**
     * Convert the XML root element to a JSON [ObjectNode]. In contrast to
     * [convertElement], this does not convert child elements since this would
     * mean converting the whole XML document in one go.
     */
    private fun convertRoot(): ObjectNode {
        val root = this.mapper.createObjectNode()
        root.put("__t", this.reader.localName)
        addAttributes(root)
        return root
    }

    /**
     * Add any attributes of the current XML element to a given [ObjectNode].
     * This assumes that the XML cursor is on an element node.
     */
    private fun addAttributes(element: ObjectNode) {
        if (reader.attributeCount > 0) {
            val attributes = mapper.createObjectNode()
            for (i in 0 until reader.attributeCount) {
                attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i))
            }
            element.replace("__a", attributes)
        }
    }

    /**
     * Add all the child nodes to an element. This can be elements, which get added to
     * an array under "__c" or text nodes, which get accumulated and are stored under
     * "__x".
     */
    private fun addChildNodes(childElements : ArrayNode) {
        var event = this.reader.next()
        var text : StringBuffer = StringBuffer()
        while (event != END_ELEMENT) {
            when(event) {
                START_ELEMENT -> {
                    childElements.add(convertElement())
                }
                CHARACTERS -> {
                    text.append(reader.text)
                }
                else -> {
                    println("ignoring: ${event}")
                }
            }
            event = this.reader.next()
        }
    }
}