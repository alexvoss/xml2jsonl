# xml2jsonl

*Note*: If you are looking for the JavaScript version of this tool, [you can find it here](). 
This is a new attempt using Kotlin that I hope will address some of the limitations of the 
JavaScript implementation.

Xml2jsonl is a utility to convert large XML files into a flow of JSON objects, 
to be written out to files as one JSON object per line. It can be run as a 
command-line utility or can be used as a library with the user writing their owen
[Kotlin Flow](https://kotlinlang.org/docs/flow.html) to filter and transform the data.  

This can be useful when working with large datasets that come as one
big XML file but contain repeated elements that are of interest. There
are plenty of examples of datasets published as very large XML
documents. Notorious examples are the [Wikipedia data
dumps](https://dumps.wikimedia.org/backup-index.html) or the
[Stack Exchange data
dumps](https://archive.org/details/stackexchange).

Converting to JSONL files before trying to work with such files has the following advantages:

* JSON is less verbose than XML, leading to a reduction of file sizes, at least for uncompressed
  versions but quite possibly also for compressed files.
* Because not all the data is stored in a single object, document-oriented parsers can be used to
  read the data back into memory, one object at a time. This makes writing code for analysing the
  data much, much simpler to write.
* JSON parsers are available in many languages out of the box while XML parsers usually come in the
  form of libraries that need to be made available as dependencies.
* There are a whole range of tools that work well with JSON, not least many NoSQL databases  
  and search engines such as ElasticSearch.

## Unique versus non-unique elements

In XML, elements can be repeated any number of times. A
straightforward translation into JSON is to store child nodes as
arrays be default, attributes as objects and text nodes concatenated 
as a text element. This is the approach taken by `xml2json`. The JSON
produced looks like this:

```javascript
{
  ':t': 'tagname', 
  ':a': {<attributes>},
  ':c': [<child_nodes],
  ':x': 'text content'
}
```

Each information-carrying part of an XML element is mapped to an
attribute in the object. The XML attributes are mapped to a JSON
object since attributes in XML cannot repeat. Child elements, however,
can and so the child nodes are mapped to an array. Also, note that tag
names and attribute names can collide, so the element's tag name, its
attribute and the tag names of its children need to be in separate
attributes. The names used for these start with a colon to ensure this
does not clash with valid XML tag and attribute names.

### Simplifying

Now, this representation is not terribly convenient to work with. One
solution would have been to create an alternative representation that makes
assumptions about the XML format but this would have rendered the tool less
generic. 

As a consequence, *prettifying* the generated JSON is left to filters
that can be applied before data gets written out to disk. The `-s` option
of the commandline tool turns this behavior on. This may work out
of the box for a given dataset or may need to be adapted.

## Limitations

The tool does not support XML documents that contain mixed content
models, sorry. It is not easy to represent a mixed content model in
JSON, though I bet it is not impossible.

Another limitation is not attempt is made to preserve the ordering of 
elements. Depending on ordering is common in document-oriented uses of 
XML that involve mixed content models but not in uses of XML to represent 
more structured data.

## Memory requirements

If the data you are working with are large as well as numerous, you will 
need to allow the JVM to allocate enough heap space to accommodate the 
large in-flight data. This is the case, for example, for some of the Wikipedia 
pages that are large *and* have an extensive history. The combination of this 
can make the resulting data structures run to multiple GBs.

The Java virtual machine allocates heap memory in proportion to the available
RAM, so if you have plenty of that you may not need to do anything. If you find
you are short of memory in relation to the data, use the `-Xmx` option of the 
JVM to increase the maximum heap size.

# XML Parser Configuration

XML parsers limit the number of entities they process before throwing an exception.
Presumably this is to prevent DDOS attacks but this can be a problem when processing
large datasets. The [solution](https://stackoverflow.com/questions/42991043/error-xml-sax-saxparseexception-while-parsing-a-xml-file-using-wikixmlj) is to run the JVM for `xml2json` with 
the following options:

```-DentityExpansionLimit=2147480000 -DtotalEntitySizeLimit=2147480000 -Djdk.xml.totalEntitySizeLimit=2147480000 -Xmx16g```

## Performance

The tool was originally written in JavaScript (see History below) but has been 
re-written in Kotlin partly to ensure that multi-core CPUs are effectively utilised.

## Usage

The basic usage of the tool is fairly straightforward. It reads input either from a 
given input file or from standard input. Likewise, it writes to standard output by 
default but this can be changed by passing a suitable filename on the command line. 
This means that `xml2jsonl` can work as a filter like so: 

```bzcat large_file.xml | xml2jsonl | bzip2 > output.jsonl```

By default, `xml2jsonl` processes all child objects of the root object. The `--tags` 
argument can be used to specify the tag names of the elements that should be processed. 
See `xml2jsonl -h` for more options.

## Implementing transformation and filtering

`xml2jsonl` is written in Kotlin and uses [Kotlin Flows](https://kotlinlang.org/docs/flow.html), 
which are similar to [Java's Streams](https://www.oracle.com/technical-resources/articles/java/ma14-java-se-8-streams.html).
Implementing transformations and filtering is therefore pretty straightforward. Use  
[main.kt](alexvoss/xml2jsonl/blob/main/src/main/kotlin/main.kt) as a starting point or
any of the scripts in the [examples](/alexvoss/xml2jsonl/tree/main/examples).

## Testing

The code comes with a range of unit tests written with [Kotest](https://kotest.io/). To run in 
IntelliJ, install the [Kotest Plugin](https://kotest.io/docs/intellij/intellij-plugin.html).
To run in Gradle, run `./gradlew test` to run the unit tests and `./gradlew acceptance` to 
run acceptance tests. These are potentially longer running and work with more complex data.

The tests are run routinely on (Jenkins? GitHub Actions? **TODO**)

## History

This project started with the need to translate the Wikipedia full history data dumps into 
a reasonably friendly data format. The dump comes in several large (about 10GB unpacked) XML 
files. They do not contain mixed content model data, though, and are fairly repetitive. It 
therefore seemed to make sense to have a tool that can convert them to JSONL format, 
i.e. a single JSON object per line in a file.

The initial implementation of this was in JavaScript. The reason for this was that 
[wtf_wikipedia](https://github.com/spencermountain/wtf_wikipedia) is also written in 
JavaScript. As far as I can tell, it is the best library for parsing MediaWiki content. 
However, some limitations of this approach soon became apparent. Some of these, such as 
the runtime performance, were expected, and I had decided to live with them. However, 
it turned out that the V8 JavaScript engine has a limit on the length of a string that 
caused conversion to JSON to tail for some page entries in the Wikipedia data. While 
considering the necessary re-design of the tool, I decided it would be better to 
re-implement in Kotlin rather than to try to address the limitations of the JavaScript 
version.

