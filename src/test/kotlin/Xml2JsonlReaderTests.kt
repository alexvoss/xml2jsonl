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

import com.corealisation.xml2jsonl.Xml2JsonlReader
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.kotest.assertions.fail
import io.kotest.core.spec.style.BehaviorSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import java.io.ByteArrayInputStream

class Xml2JsonlReaderTests : BehaviorSpec({

    val doc1 = """<?xml version='1.0'?>
<root len='2'>
    <a><b></b></a>
    <b attr='b'><c>Hi!</c><a></a></b>
</root>"""

    Given("doc1") {

        When("procroot argument is false") {
            Then("should ignore root element") {
                val reader = Xml2JsonlReader(
                    procRoot = false,
                    inputStream = ByteArrayInputStream(doc1.toByteArray())
                )
                val jsonObjects = reader.getFlow().toList()
                for(json in jsonObjects) {
                    if(json.get(":t")?.asText().equals("root")) {
                        fail("root element was not ingnored.")
                    }
                }
            }
        }

        When("procroot argument is true") {
            Then("should process root element") {
                val reader = Xml2JsonlReader(
                    procRoot = true,
                    inputStream = ByteArrayInputStream(doc1.toByteArray())
                )
                val json = reader.getFlow().first()
                assert(json.has(":t"))
                assert(json.get(":t").asText().equals("root"))
            }
        }

        When("element is child of root and allTop is true") {
            Then("json is emitted") {
                val reader = Xml2JsonlReader(
                    procRoot = false,
                    allTop = true,
                    inputStream = ByteArrayInputStream(doc1.toByteArray())
                )
                val json = reader.getFlow().first()
                assert(json.has(":t"))
                assert(json.get(":t").asText().equals("a"))
            }
        }

        When("element specified in tags") {
            Then("element is included") {
                val reader = Xml2JsonlReader(
                    allTop = false,
                    procRoot = false,
                    tags = listOf("c"),
                    inputStream = ByteArrayInputStream(doc1.toByteArray())
                )
                val json = reader.getFlow().first()
                println(json.toPrettyString())
                assert(json.has(":t"))
                assert(json.get(":t").asText().equals("c"))
            }
        }

        When("element has child elements") {
            Then("child elements included in ':c' attribute") {
                Then("json is emitted") {
                    val reader = Xml2JsonlReader(
                        procRoot = false,
                        allTop = true,
                        inputStream = ByteArrayInputStream(doc1.toByteArray())
                    )
                    val json = reader.getFlow().first()
                    assert(json.has(":c"))
                    assert(json.get(":c") is ArrayNode)
                    val childElements : ArrayNode = json.get(":c") as ArrayNode
                    assert(childElements.get(0) is ObjectNode)
                    assert((childElements.get(0) as ObjectNode).get(":t")?.asText().equals("b"))
                }
            }
        }

        When("using simplifyFlow()") {
            Then("child elements are moved from ':c'") {
                val reader = Xml2JsonlReader(
                    procRoot = false,
                    allTop = true,
                    inputStream = ByteArrayInputStream(doc1.toByteArray())
                )
                val json = reader.getSimplifiedFlow().first()
                println(json)
                assert(json.has("b"))
            }
        }
    }

})
