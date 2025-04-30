package net.pincette.json;

import static com.fasterxml.jackson.dataformat.yaml.YAMLFactory.builder;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.LITERAL_BLOCK_STYLE;
import static java.util.Arrays.stream;
import static javax.json.stream.JsonParser.Event.START_OBJECT;
import static net.pincette.json.JsonUtil.createReader;
import static net.pincette.json.JsonUtil.createWriter;
import static net.pincette.util.Util.tryToDoWithRethrow;
import static net.pincette.util.Util.tryToGetWithRethrow;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Optional;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.stream.JsonParser;
import net.pincette.json.filter.JacksonGenerator;
import net.pincette.json.filter.JacksonParser;
import net.pincette.json.filter.JsonParserWrapper;

/**
 * Convenience methods to read and write YAML or JSON for situations where both are supported.
 *
 * @author Werner Donné
 * @since 1.6
 */
public class JsonOrYaml {
  private static final YAMLFactory factory = builder().build().enable(LITERAL_BLOCK_STYLE);

  private JsonOrYaml() {}

  private static JsonStructure get(final JsonParser parser) {
    return parser.next() == START_OBJECT ? parser.getObject() : parser.getArray();
  }

  private static Optional<File> getFile(final String filename) {
    return Optional.of(new File(filename))
        .filter(File::exists)
        .or(() -> getFile(filename, new String[] {"yml", "yaml", "json"}));
  }

  private static Optional<File> getFile(final String filename, final String[] extensions) {
    return stream(extensions)
        .map(extension -> new File(filename + "." + extension))
        .filter(File::exists)
        .findFirst();
  }

  /**
   * Reads JSON or YAML. If the file doesn't exist, the extensions ".yml", ".yaml" and ".json" are
   * tried.
   *
   * @param filename the given filename.
   * @return The contents of the file as a JSON structure.
   */
  public static Optional<JsonStructure> read(final String filename) {
    return getFile(filename).flatMap(JsonOrYaml::read);
  }

  /**
   * Reads JSON or YAML, depending on the extension of the filename. If it is ".yml" or ".yaml" the
   * file will be read as YAML and as JSON otherwise.
   *
   * @param file the given file.
   * @return The contents of the file as a JSON structure.
   */
  public static Optional<JsonStructure> read(final File file) {
    return tryToGetWithRethrow(
        () -> new FileReader(file),
        reader ->
            file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")
                ? readYaml(reader).orElse(null)
                : readJson(reader).orElse(null));
  }

  public static Optional<JsonStructure> readJson(final Reader reader) {
    return tryToGetWithRethrow(() -> createReader(reader), JsonReader::read);
  }

  public static Optional<JsonStructure> readYaml(final Reader reader) {
    return tryToGetWithRethrow(
        () -> new JsonParserWrapper(new JacksonParser(factory.createParser(reader))),
        JsonOrYaml::get);
  }

  public static void writeJson(final JsonStructure json, final Writer writer) {
    tryToDoWithRethrow(() -> createWriter(writer), w -> w.write(json));
  }

  public static void writeYaml(final JsonStructure json, final Writer writer) {
    tryToDoWithRethrow(
        () -> new JacksonGenerator(factory.createGenerator(writer)),
        generator -> generator.write(json));
  }
}
