import com.corealisation.xml2jsonl.Xml2JsonlReader
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

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

class Main : CliktCommand() {

    private val allTop by option(
        "-a", "--allTop",
        help = "include all child elements of the root element"
    ).flag(default=false)

    private val procRoot by option(
        "-r", "--procRoot",
        help = "process the root element as well, creating an additional JSON object"
    ).flag(default=false)

    private val simplify by option(
        "-s", "--simplify",
        help="simplify the JSON objects to make them easier to work with"
    ).flag(default=false)

    private val input by option(
        "-i", "--input",
        help = "provides an input file to read from. The default is to read from standard input."
    )

    private val output by option(
        "-o", "--output",
        help = "provides an output file to write to. The default is to write to standard output."
    )

    private val tags by option(
        "-t", "--tags",
        help = "the tag name(s) of elements to be extracted from the XML document parsed."
    ).multiple()

    override fun run() {
        val inputStream: InputStream = if (input == null) {
            System.`in`
        } else {
            Files.newInputStream(Path.of(input))
        }

        val outputStream: OutputStream = if (output == null) {
            System.`out`
        } else {
            Files.newOutputStream(Path.of(output))
        }

        runBlocking {
            val reader: Xml2JsonlReader = Xml2JsonlReader(
                allTop = allTop,
                procRoot = procRoot,
                tags = tags,
                inputStream = inputStream
            )

            val flow = if(simplify) reader.getSimplifiedFlow() else reader.getFlow()

            flow.collect { json ->
                println(json.toPrettyString())
            }
        }
    }
}

fun main(args: Array<String>) = Main().main(args)
