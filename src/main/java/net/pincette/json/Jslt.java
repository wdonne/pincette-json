package net.pincette.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.pincette.json.Jackson.from;
import static net.pincette.json.Jackson.to;

import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import com.schibsted.spt.data.jslt.Parser;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.function.UnaryOperator;
import javax.json.JsonObject;

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
    final Parser parser =
        new Parser(new InputStreamReader(Jslt.class.getResourceAsStream(resource), UTF_8));
    final Expression jslt =
        (functions != null ? parser.withFunctions(functions) : parser).compile();

    return json -> to(jslt.apply(from(json))).asJsonObject();
  }
}
