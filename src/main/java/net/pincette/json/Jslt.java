package net.pincette.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static net.pincette.json.Jackson.from;
import static net.pincette.json.Jackson.to;
import static net.pincette.util.Util.tryToGetRethrow;

import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import com.schibsted.spt.data.jslt.Parser;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.function.UnaryOperator;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * JSLT utilities.
 *
 * @author Werner Donn\u00e9
 * @since 1.1
 */
public class Jslt {
  private Jslt() {}

  /**
   * Returns a function that transforms JSON using a JSLT file.
   *
   * @param resource the resource in the classpath that contains the JSLT file.
   * @return The transformer function.
   * @since 1.1
   */
  public static UnaryOperator<JsonObject> transformer(final String resource) {
    return transformer(resource, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT file.
   *
   * @param resource the resource in the classpath that contains the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.1
   */
  public static UnaryOperator<JsonObject> transformer(
      final String resource, final Collection<Function> functions) {
    return transformer(Jslt.class.getResourceAsStream(resource), functions);
  }

  /**
   * Returns a function that transforms JSON using a JSLT file.
   *
   * @param file the JSLT file.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(final File file) {
    return transformer(file, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT file.
   *
   * @param file the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(
      final File file, final Collection<Function> functions) {
    return transformer(tryToGetRethrow(() -> new FileInputStream(file)).orElse(null), functions);
  }

  /**
   * Returns a function that transforms JSON using a JSLT file.
   *
   * @param in the JSLT input stream.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(final InputStream in) {
    return transformer(in, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT file.
   *
   * @param in the JSLT input stream.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(
      final InputStream in, final Collection<Function> functions) {
    return transformer(new InputStreamReader(in, UTF_8), functions);
  }

  /**
   * Returns a function that transforms JSON using a JSLT file.
   *
   * @param reader the JSLT reader.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(final Reader reader) {
    return transformer(reader, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT file.
   *
   * @param reader the JSLT reader.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(
      final Reader reader, final Collection<Function> functions) {
    final Parser parser = new Parser(reader);
    final Expression jslt =
        (functions != null ? parser.withFunctions(functions) : parser).compile();

    return json ->
        ofNullable(to(jslt.apply(from(json))))
            .filter(JsonUtil::isObject)
            .map(JsonValue::asJsonObject)
            .orElse(null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT file.
   *
   * @param jslt the JSLT script.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformerString(final String jslt) {
    return transformerString(jslt, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT file.
   *
   * @param jslt the JSLT script.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformerString(
      final String jslt, final Collection<Function> functions) {
    return transformer(new StringReader(jslt), functions);
  }
}
