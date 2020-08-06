package net.pincette.json;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.round;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.json.JsonValue.NULL;
import static net.pincette.json.Jackson.from;
import static net.pincette.json.Jackson.to;
import static net.pincette.json.JsonUtil.add;
import static net.pincette.json.JsonUtil.asString;
import static net.pincette.json.JsonUtil.createArrayBuilder;
import static net.pincette.json.JsonUtil.createValue;
import static net.pincette.json.JsonUtil.getValue;
import static net.pincette.json.JsonUtil.isObject;
import static net.pincette.json.JsonUtil.isString;
import static net.pincette.json.JsonUtil.string;
import static net.pincette.json.JsonUtil.toDotSeparated;
import static net.pincette.util.Collections.list;
import static net.pincette.util.Collections.union;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Triple.triple;
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
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import net.pincette.function.SideEffect;

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

  /**
   * Creates a collection of all custom functions except <code>trace</code>.
   *
   * @return The collection of functions.
   * @since 1.3.6
   */
  public static Collection<Function> customFunctions() {
    return list(getPointer(), parseIsoInstant(), pointer(), setPointer(), uuid());
  }

  /**
   * Wraps a lambda in a JSLT function.
   *
   * @param name the name of the function.
   * @param minArguments the minimum number of arguments.
   * @param maxArguments the maximum number of arguments.
   * @param function the lambda.
   * @return The JSLT function.
   * @since 1.3.1
   */
  public static Function function(
      final String name,
      final int minArguments,
      final int maxArguments,
      final java.util.function.Function<JsonArray, JsonValue> function) {
    return new CustomFunction(
        name,
        minArguments,
        maxArguments,
        (input, arguments) ->
            from(
                function.apply(
                    stream(arguments)
                        .reduce(createArrayBuilder(), (b, v) -> b.add(to(v)), (b1, b2) -> b1)
                        .build())));
  }

  /**
   * This custom function is called "get-pointer". Its first two mandatory arguments are a JSON
   * object and a JSON pointer. The optional third argument is a fallback value that is returned
   * when the value doesn't exist. The default fallback value is <code>null</code>.
   *
   * @return The generated function.
   * @since 1.3.6
   */
  public static Function getPointer() {
    return function(
        "get-pointer",
        2,
        3,
        array ->
            Optional.of(array)
                .map(a -> pair(a.get(0), a.get(1)))
                .filter(pair -> isObject(pair.first) && isString(pair.second))
                .flatMap(
                    pair -> getValue(pair.first.asJsonObject(), asString(pair.second).getString()))
                .orElseGet(() -> array.size() == 3 ? array.get(2) : NULL));
  }

  /**
   * This custom function is called "parse-iso-instant". It uses <code>java.time.Instant.parse
   * </code> to parse its only argument and returns the epoch seconds value.
   *
   * @return The generated function.
   * @since 1.3.2
   */
  public static Function parseIsoInstant() {
    return function(
        "parse-iso-instant",
        1,
        1,
        array ->
            Optional.of(array.get(0))
                .filter(JsonUtil::isInstant)
                .map(JsonUtil::asInstant)
                .map(Instant::toEpochMilli)
                .map(v -> round(v / 1000.0))
                .map(JsonUtil::createValue)
                .orElse(NULL));
  }

  /**
   * This custom function is called "pointer". It composes a JSON pointer with all of its string
   * arguments, of which there should be at least one.
   *
   * @return The generated function.
   * @since 1.3.6
   */
  public static Function pointer() {
    return function(
        "pointer",
        1,
        MAX_VALUE,
        array ->
            createValue(
                "/"
                    + array.stream()
                        .filter(JsonUtil::isString)
                        .map(JsonUtil::asString)
                        .map(JsonString::getString)
                        .collect(joining("/"))));
  }

  /**
   * Adds custom functions globally. Later registrations override earlier ones based on the function
   * names. All created transformers will have access to all registered functions. Functions passed
   * to transformers will override registered ones.
   *
   * @param functions the given custom functions.
   * @since 1.3.1
   */
  public static void registerCustomFunctions(final Collection<Function> functions) {
    functions.forEach(f -> customFunctions.add(new CustomFunction(f)));
  }

  /**
   * This custom function is called "set-pointer". It requires three arguments: a JSON object, a
   * JSON pointer and a value. The value will be set at the pointer location. The new object is
   * returned.
   *
   * @return The generated function.
   * @since 1.3.6
   */
  public static Function setPointer() {
    return function(
        "set-pointer",
        3,
        3,
        array ->
            Optional.of(array)
                .map(a -> triple(a.get(0), a.get(1), a.get(2)))
                .filter(triple -> isObject(triple.first) && isString(triple.second))
                .map(
                    triple ->
                        (JsonValue)
                            add(
                                triple.first.asJsonObject(),
                                toDotSeparated(asString(triple.second).getString()),
                                triple.third))
                .orElse(NULL));
  }

  private static Set<CustomFunction> toCustom(final Collection<Function> functions) {
    return functions.stream().map(CustomFunction::new).collect(toSet());
  }

  private static Collection<Function> toFunction(final Set<CustomFunction> functions) {
    return functions.stream().map(f -> (Function) f).collect(toList());
  }

  /**
   * This custom function, which is called "trace" in JSLT, accepts one argument. It sends it to
   * <code>logger</code> with level <code>INFO</code> and then returns it.
   *
   * @param logger the given logger.
   * @return The generated function.
   * @since 1.3.1
   */
  public static Function trace(final Logger logger) {
    return function(
        "trace",
        1,
        1,
        array ->
            SideEffect.<JsonValue>run(() -> logger.info(string(array.get(0))))
                .andThenGet(() -> array.get(0)));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param resource the resource in the classpath that contains the JSLT file.
   * @return The transformer function.
   * @since 1.1
   */
  public static UnaryOperator<JsonObject> transformer(final String resource) {
    return transformer(resource, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param resource the resource in the classpath that contains the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.1
   */
  public static UnaryOperator<JsonObject> transformer(
      final String resource, final Collection<Function> functions) {
    return transformer(resource, functions, emptyMap());
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param resource the resource in the classpath that contains the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.3
   */
  public static UnaryOperator<JsonObject> transformer(
      final String resource,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables) {
    return transformer(resource, functions, variables, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param resource the resource in the classpath that contains the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @param resolver a resolver for imported modules. It may be <code>null</code> in which case the
   *     classpath resolver is used.
   * @return The transformer function.
   * @since 1.3.5
   */
  public static UnaryOperator<JsonObject> transformer(
      final String resource,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    return transformer(Jslt.class.getResourceAsStream(resource), functions, variables, resolver);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param file the JSLT file.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(final File file) {
    return transformer(file, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param file the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(
      final File file, final Collection<Function> functions) {
    return transformer(file, functions, emptyMap());
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param file the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.3
   */
  public static UnaryOperator<JsonObject> transformer(
      final File file,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables) {
    return transformer(file, functions, variables, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param file the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @param resolver a resolver for imported modules. It may be <code>null</code> in which case the
   *     classpath resolver is used.
   * @return The transformer function.
   * @since 1.3.5
   */
  public static UnaryOperator<JsonObject> transformer(
      final File file,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    return transformer(
        tryToGetRethrow(() -> new FileInputStream(file)).orElse(null),
        functions,
        variables,
        resolver);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param in the JSLT input stream.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(final InputStream in) {
    return transformer(in, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param in the JSLT input stream.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(
      final InputStream in, final Collection<Function> functions) {
    return transformer(in, functions, emptyMap());
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param in the JSLT input stream.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.3
   */
  public static UnaryOperator<JsonObject> transformer(
      final InputStream in,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables) {
    return transformer(in, functions, variables, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param in the JSLT input stream.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @param resolver a resolver for imported modules. It may be <code>null</code> in which case the
   *     classpath resolver is used.
   * @return The transformer function.
   * @since 1.3.5
   */
  public static UnaryOperator<JsonObject> transformer(
      final InputStream in,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    return transformer(new InputStreamReader(in, UTF_8), functions, variables, resolver);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param reader the JSLT reader.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(final Reader reader) {
    return transformer(reader, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param reader the JSLT reader.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformer(
      final Reader reader, final Collection<Function> functions) {
    return transformer(reader, functions, emptyMap());
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param reader the JSLT reader.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.3
   */
  public static UnaryOperator<JsonObject> transformer(
      final Reader reader,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables) {
    return transformer(reader, functions, variables, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param reader the JSLT reader.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @param resolver a resolver for imported modules. It may be <code>null</code> in which case the
   *     classpath resolver is used.
   * @return The transformer function.
   * @since 1.3.5
   */
  public static UnaryOperator<JsonObject> transformer(
      final Reader reader,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    final Parser parser = new Parser(reader);
    final Expression jslt =
        parser
            .withFunctions(
                toFunction(
                    union(customFunctions, functions != null ? toCustom(functions) : emptySet())))
            .withResourceResolver(resolver != null ? resolver : new ClasspathResourceResolver())
            .compile();
    final Map<String, JsonNode> vars = variables(variables);

    return json ->
        ofNullable(to(jslt.apply(vars, from(json))))
            .filter(JsonUtil::isObject)
            .map(JsonValue::asJsonObject)
            .orElse(null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param jslt the JSLT script.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformerString(final String jslt) {
    return transformerString(jslt, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param jslt the JSLT script.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   */
  public static UnaryOperator<JsonObject> transformerString(
      final String jslt, final Collection<Function> functions) {
    return transformerString(jslt, functions, emptyMap());
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param jslt the JSLT script.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.3.1
   */
  public static UnaryOperator<JsonObject> transformerString(
      final String jslt,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables) {
    return transformerString(jslt, functions, variables, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param jslt the JSLT script.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @param resolver a resolver for imported modules. It may be <code>null</code> in which case the
   *     classpath resolver is used.
   * @return The transformer function.
   * @since 1.3.5
   */
  public static UnaryOperator<JsonObject> transformerString(
      final String jslt,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    return transformer(new StringReader(jslt), functions, variables, resolver);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param jslt the JSLT script. If it starts with "resource:" the script is loaded as a class path
   *     resource. If it denotes an existing file it will be loaded from that file. Otherwise it is
   *     interpreted as a JSLT script.
   * @return The transformer function.
   * @since 1.3.4
   */
  public static UnaryOperator<JsonObject> tryTransformer(final String jslt) {
    return tryTransformer(jslt, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param jslt the JSLT script. If it starts with "resource:" the script is loaded as a class path
   *     resource. If it denotes an existing file it will be loaded from that file. Otherwise it is
   *     interpreted as a JSLT script.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.3.4
   */
  public static UnaryOperator<JsonObject> tryTransformer(
      final String jslt, final Collection<Function> functions) {
    return tryTransformer(jslt, functions, emptyMap());
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param jslt the JSLT script. If it starts with "resource:" the script is loaded as a class path
   *     resource. If it denotes an existing file it will be loaded from that file. Otherwise it is
   *     interpreted as a JSLT script.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.3.4
   */
  public static UnaryOperator<JsonObject> tryTransformer(
      final String jslt,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables) {
    return tryTransformer(jslt, functions, variables, null);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param jslt the JSLT script. If it starts with "resource:" the script is loaded as a class path
   *     resource. If it denotes an existing file it will be loaded from that file. Otherwise it is
   *     interpreted as a JSLT script.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @param resolver a resolver for imported modules. It may be <code>null</code> in which case the
   *     classpath resolver is used.
   * @return The transformer function.
   * @since 1.3.5
   */
  public static UnaryOperator<JsonObject> tryTransformer(
      final String jslt,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    final Supplier<UnaryOperator<JsonObject>> tryFile =
        () ->
            new File(jslt).exists()
                ? transformer(new File(jslt), functions, variables, resolver)
                : transformerString(jslt, functions, variables, resolver);

    return jslt.startsWith(RESOURCE)
        ? transformer(jslt.substring(RESOURCE.length()), functions, variables, resolver)
        : tryFile.get();
  }

  /**
   * This custom function is called "uuid". It generates a UUID.
   *
   * @return The generated function.
   * @since 1.3.6
   */
  public static Function uuid() {
    return function("uuid", 0, 0, array -> createValue(randomUUID().toString()));
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

  private static class CustomFunction implements Function {
    private final BiFunction<JsonNode, JsonNode[], JsonNode> function;
    private final int minArguments;
    private final int maxArguments;
    private final String name;

    private CustomFunction(
        final String name,
        final int minArguments,
        final int maxArguments,
        final BiFunction<JsonNode, JsonNode[], JsonNode> function) {
      this.name = name;
      this.minArguments = minArguments;
      this.maxArguments = maxArguments;
      this.function = function;
    }

    private CustomFunction(final Function delegate) {
      this.name = delegate.getName();
      this.minArguments = delegate.getMinArguments();
      this.maxArguments = delegate.getMaxArguments();
      this.function = delegate::call;
    }

    public JsonNode call(final JsonNode input, final JsonNode[] arguments) {
      return function.apply(input, arguments);
    }

    @Override
    public boolean equals(final Object other) {
      return other instanceof CustomFunction && ((CustomFunction) other).name.equals(name);
    }

    public String getName() {
      return name;
    }

    public int getMinArguments() {
      return minArguments;
    }

    public int getMaxArguments() {
      return maxArguments;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
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
}
