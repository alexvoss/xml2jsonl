# xml2jsonl

*Note*: If you are looking for the JavaScript version of this tool, [you can find it here](). This is a new attempt using Kotlin that I hope will address some of the limitations of the JavaScript implementation.

Xml2jsonl is a utility to convert large XML files into files with one JSON object per line, allowing the data to be filtered on the way.

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
* **I will need to check performance!**

An additional function this tool serves is to enable filtering of the
data, so that subsets of the elements can be created. The tool can
call a user-defined function for every object that is read.  This
function can filter the data to be written to the output or can
transform it by removing properties that are not required.

## Unique versus non-unique elements

In XML, elements can be repeated any number of times. A
straightforward translation into JSON is to store child nodes as
arrays be default. This is the approach taken by `xml2json`. The JSON
produced looks like this:

```javascript
{
  '__t': 'tagname', 
  '__a': {<attributes>},
  '__c': [<child_nodes],
  '__x': 'text content'
}
```

Each information-carrying part of an XML element is mapped to an
attribute in the object. The XML attributes are mapped to a JSON
object since attributes in XML cannot repeat. Child elements, however,
can and so the child nodes are mapped to an array. Also, note that tag
names and attribute names can collide, so the element's tag name, its
attribute and the tag names of its children need to be in separate
attributes.

Now, this representation is not terribly convenient to work with. One
solution would have been to create an alternative representation that
assumes non-repeating elements. However, there will be cases where *some*
of the elements are repeating and some are not.

As a consequence, *prettifying* the generated JSON is left to filters
that can be applied before data gets written out to disk. The
`SimplifyUniqueTransformer` class provides functionality to simplify
the format while checking that there are no clashes. This may work out
of the box for a given dataset or may need to be adapted.

## Limitations

The tool does not support XML documents that contain mixed content
models, sorry. It is not easy to represent a mixed content model in
JSON, though I bet it is not impossible.

Another limitation is that the ordering of elements is not preserved.
Depending on ordering is common in document-oriented uses of XML that
involve mixed content models but not in uses of XML to represent more
structured data.

## Memory requirements

If the data you are working with are large as well as numerous, you will need to allow the JVM to allocate enough heap space to accommodate the large in-flight data. This is the case, for example, for some of the Wikipedia pages that are large *and* have an extensive history. The combination of this can make the resulting data structures run to multiple GBs.

## Performance

The tool was originally written in JavaScript (see History below) but has been re-written in Kotlin partly to ensure that multi-core CPUs are effectively utilised.

## Usage

The basic usage of the tool is fairly straightforward. It reads input either from a given input file or from standard input. Likewise, it writes to standard output by default but this can be changed by passing a suitable filename on the command line. This means that `xml2jsonl` can work as a filter like so: `bzcat large_file.xml | xml2jsonl | bzip2 > output.jsonl`.

By default, `xml2jsonl` processes all child objects of the root object. The `--tags` argument can be used to specify the tag names of the elements that should be processed. A user-specified filter function can be provided with `--filter` to reduce the output to only the data needed and to transform the objects (see below).

```
--input <filename>        provides an input file to read from. The default is to 
                          read from standad input.
--output <filename>       provides an output file to write to. The default is to 
                          write to standard output.
--tags <element name(s)>  the tag name(s) of elements to be extracted from the 
                          XML document parsed. If none are provided then all
                          child elements of the root element are processed.
--filter <js file>        the name of a Javascript module to load via require; 
                          must export a filter() function.
--root                    process the root element as well (to create
                          a single JSON object)
```

## Implementing filters

**TODO**

## Testing

The code comes with a range of unit tests written with
[JUnit5](https://junit.org/junit5/) and [Mockito](https://site.mockito.org/). Run `./gradlew test` to run the unit tests and `./gradlew acceptance` to run acceptance tests. These are potentially longer running and work with more complex data.

The tests are run routinely on (Jenkins? GitHub Actions? **TODO**)

## History

This project started with the need to translate the Wikipedia full history data dumps into a reasonably friendly data 
format. The dump comes in several large (about 10GB unpacked) XML files. They do not contain mixed content model data, 
though, and are fairly repetitive. It therefore seemed to make sense to have a tool that can convert them to JSONL format, 
i.e. a single JSON object per line in a file.

The initial implementation of this was in JavaScript. The reason for this was that 
[wtf_wikipedia](https://github.com/spencermountain/wtf_wikipedia) is also written in JavaScript. As far as I can tell, 
it is the best library for parsing MediaWiki content. However, some limitations of this approach soon became apparent. 
Some of these, such as the runtime performance, were expected, and I had decided to live with them. However, it turned
out that the V8 JavaScript engine has a limit on the length of a string that caused conversion to JSON to tail for some 
page entries in the Wikipedia data. While considering the necessary re-design of the tool, I decided it would be better
to re-implement in Kotlin rather than to try to address the limitations of the JavaScript version.