package net.pincette.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static net.pincette.json.Jackson.from;
import static net.pincette.json.Jackson.to;
import static net.pincette.util.Collections.union;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.tryToGetRethrow;

import com.fasterxml.jackson.databind.JsonNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import com.schibsted.spt.data.jslt.Parser;
import com.schibsted.spt.data.jslt.ResourceResolver;
import com.schibsted.spt.data.jslt.impl.ClasspathResourceResolver;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import javax.json.JsonObject;
import javax.json.JsonValue;
import net.pincette.json.JsltCustom.CustomFunction;

/**
 * JSLT utilities.
 *
 * @author Werner Donn\u00e9
 * @since 1.1
 */
public class Jslt {
  private static final String RESOURCE = "resource:";
  private static final Set<CustomFunction> customFunctions = new HashSet<>();

  private Jslt() {}

  private static java.util.function.Function<JsonValue, JsonObject> asObject() {
    return json ->
        ofNullable(json).filter(JsonUtil::isObject).map(JsonValue::asJsonObject).orElse(null);
  }

  private static java.util.function.Function<JsonObject, JsonValue> asValue() {
    return json -> json;
  }

  public static Reader reader(final File file) {
    return tryToGetRethrow(() -> reader(new FileInputStream(file))).orElse(null);
  }

  public static Reader reader(final InputStream in) {
    return new InputStreamReader(in, UTF_8);
  }

  public static Reader readerResource(final String resource) {
    return reader(Jslt.class.getResourceAsStream(resource));
  }

  private static Set<CustomFunction> toCustom(final Collection<Function> functions) {
    return functions.stream().map(CustomFunction::new).collect(toSet());
  }

  private static Collection<Function> toFunction(final Set<CustomFunction> functions) {
    return functions.stream().map(Function.class::cast).collect(toList());
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param context The context of the call.
   * @return The transformer function.
   * @since 1.4
   */
  @SuppressWarnings("java:S4276") // Not compatible.
  public static UnaryOperator<JsonObject> transformerObject(final Context context) {
    final java.util.function.Function<JsonObject, JsonObject> transformer =
        asObject().compose(transformerValue(context)).compose(asValue());

    return transformer::apply;
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param context The context of the call.
   * @return The transformer function.
   * @since 1.4
   */
  public static UnaryOperator<JsonValue> transformerValue(final Context context) {
    final Parser parser = new Parser(context.reader);
    final Expression jslt =
        parser
            .withFunctions(
                toFunction(
                    union(
                        customFunctions,
                        context.functions != null ? toCustom(context.functions) : emptySet())))
            .withResourceResolver(
                context.resolver != null ? context.resolver : new ClasspathResourceResolver())
            .compile();
    final Map<String, JsonNode> vars = variables(context.variables);

    return json -> ofNullable(to(jslt.apply(vars, from(json)))).orElse(null);
  }

  /**
   * Returns a reader for a JSLT script.
   *
   * @param jslt the JSLT script. If it starts with "resource:" the script is loaded as a class path
   *     resource. If it denotes an existing file it will be loaded from that file. Otherwise it is
   *     interpreted as a JSLT script.
   * @return The transformer function.
   * @since 1.4
   */
  public static Reader tryReader(final String jslt) {
    final Supplier<Reader> tryFile =
        () -> new File(jslt).exists() ? reader(new File(jslt)) : new StringReader(jslt);

    return jslt.startsWith(RESOURCE)
        ? readerResource(jslt.substring(RESOURCE.length()))
        : tryFile.get();
  }

  private static Map<String, JsonNode> variables(final Map<String, JsonValue> variables) {
    return ofNullable(variables)
        .map(
            v ->
                v.entrySet().stream()
                    .map(e -> pair(e.getKey(), from(e.getValue())))
                    .filter(pair -> pair.second != null)
                    .collect(toMap(pair -> pair.first, pair -> pair.second)))
        .orElseGet(Collections::emptyMap);
  }

  /**
   * Resolves imported modules from a map.
   *
   * @since 1.3.5
   */
  public static class MapResolver implements ResourceResolver {
    private final Map<String, String> map;

    /**
     * Creates the resolver with a map.
     *
     * @param map the keys are relative paths and the values are JSLT strings.
     */
    public MapResolver(final Map<String, String> map) {
      this.map = map;
    }

    public Reader resolve(final String jslt) {
      return ofNullable(map.get(jslt)).map(StringReader::new).orElse(null);
    }
  }

  /**
   * The context for generating JSLT transformer functions.
   *
   * @since 1.4
   */
  public static class Context {
    final Collection<Function> functions;
    final Reader reader;
    final ResourceResolver resolver;
    final Map<String, JsonValue> variables;

    public Context(final Reader reader) {
      this(reader, null, null, null);
    }

    private Context(
        final Reader reader,
        final Collection<Function> functions,
        final Map<String, JsonValue> variables,
        final ResourceResolver resolver) {
      this.reader = reader;
      this.functions = functions;
      this.variables = variables != null ? variables : emptyMap();
      this.resolver = resolver;
    }

    /**
     * Sets additional custom functions.
     *
     * @param functions the collection of functions.
     * @return The context.
     * @since 1.4
     */
    public Context withFunctions(final Collection<Function> functions) {
      return new Context(reader, functions, variables, resolver);
    }

    /**
     * Sets the resolver.
     *
     * @param resolver the resolver.
     * @return The context.
     * @since 1.4
     */
    public Context withResolver(final ResourceResolver resolver) {
      return new Context(reader, functions, variables, resolver);
    }

    /**
     * Sets additional variables that can be address with <code>$$name</code>.
     *
     * @param variables the variables.
     * @return The context.
     * @since 1.4
     */
    public Context withVariables(final Map<String, JsonValue> variables) {
      return new Context(reader, functions, variables, resolver);
    }
  }
}
