package net.pincette.json;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.max;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.fill;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static net.pincette.json.Jackson.from;
import static net.pincette.json.Jackson.to;
import static net.pincette.util.Collections.list;
import static net.pincette.util.Collections.union;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.rangeExclusive;
import static net.pincette.util.StreamUtil.zip;
import static net.pincette.util.Util.tryToGetRethrow;

import com.fasterxml.jackson.databind.JsonNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import com.schibsted.spt.data.jslt.JsltException;
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
import java.util.logging.Logger;
import javax.json.JsonArray;
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

  /**
   * Creates a collection of all custom functions except <code>trace</code>.
   *
   * @return The collection of functions.
   * @since 1.3.6
   * @deprecated Use the JsltCustom version.
   */
  @Deprecated(since = "1.4")
  public static Collection<Function> customFunctions() {
    return list(
        getPointer(), parseIsoInstant(), pointer(), setPointer(), uriDecode(), uriEncode(), uuid());
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
   * @deprecated Use the JsltCustom version.
   */
  @Deprecated(since = "1.4")
  public static Function function(
      final String name,
      final int minArguments,
      final int maxArguments,
      final java.util.function.Function<JsonArray, JsonValue> function) {
    return JsltCustom.function(name, minArguments, maxArguments, function);
  }

  /**
   * This custom function is called "get-pointer". Its first two mandatory arguments are a JSON
   * object and a JSON pointer. The optional third argument is a fallback value that is returned
   * when the value doesn't exist. The default fallback value is <code>null</code>.
   *
   * @return The generated function.
   * @since 1.3.6
   * @deprecated Use the JsltCustom version.
   */
  @Deprecated(since = "1.4")
  public static Function getPointer() {
    return JsltCustom.getPointer();
  }

  private static String numberLines(final String s) {
    return zip(stream(s.split("\\n")).sequential(), rangeExclusive(1, MAX_VALUE))
        .map(pair -> rightAlign(valueOf(pair.second), 4) + " " + pair.first)
        .collect(joining("\n"));
  }

  /**
   * This custom function is called "parse-iso-instant". It uses <code>java.time.Instant.parse
   * </code> to parse its only argument and returns the epoch seconds value.
   *
   * @return The generated function.
   * @since 1.3.2
   * @deprecated Use the JsltCustom version.
   */
  @Deprecated(since = "1.4")
  public static Function parseIsoInstant() {
    return JsltCustom.parseIsoInstant();
  }

  /**
   * This custom function is called "pointer". It composes a JSON pointer with all of its string
   * arguments, of which there should be at least one.
   *
   * @return The generated function.
   * @since 1.3.6
   * @deprecated Use the JsltCustom version.
   */
  @Deprecated(since = "1.4")
  public static Function pointer() {
    return JsltCustom.pointer();
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

  /**
   * Adds custom functions globally. Later registrations override earlier ones based on the function
   * names. All created transformers will have access to all registered functions. Functions passed
   * to transformers will override registered ones.
   *
   * @param functions the given custom functions.
   * @since 1.3.1
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static void registerCustomFunctions(final Collection<Function> functions) {
    functions.forEach(f -> customFunctions.add(new JsltCustom.CustomFunction(f)));
  }

  private static String rightAlign(final String s, final int size) {
    final char[] padding = new char[max(size - s.length(), 0)];

    fill(padding, ' ');

    return new String(padding) + s;
  }

  /**
   * This custom function is called "set-pointer". It requires three arguments: a JSON object, a
   * JSON pointer and a value. The value will be set at the pointer location. The new object is
   * returned.
   *
   * @return The generated function.
   * @since 1.3.6
   * @deprecated Use the JsltCustom version.
   */
  @Deprecated(since = "1.4")
  public static Function setPointer() {
    return JsltCustom.setPointer();
  }

  private static Set<CustomFunction> toCustom(final Collection<Function> functions) {
    return functions.stream().map(CustomFunction::new).collect(toSet());
  }

  private static Collection<Function> toFunction(final Set<CustomFunction> functions) {
    return functions.stream().map(Function.class::cast).collect(toList());
  }

  /**
   * This custom function, which is called "trace" in JSLT, accepts one argument. It sends it to
   * <code>logger</code> with level <code>INFO</code> and then returns it.
   *
   * @param logger the given logger.
   * @return The generated function.
   * @since 1.3.1
   * @deprecated Use the JsltCustom version.
   */
  @Deprecated(since = "1.4")
  public static Function trace(final Logger logger) {
    return JsltCustom.trace(logger);
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param resource the resource in the classpath that contains the JSLT file.
   * @return The transformer function.
   * @since 1.1
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(final String resource) {
    return transformerObject(new Context(readerResource(resource)));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param resource the resource in the classpath that contains the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.1
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final String resource, final Collection<Function> functions) {
    return transformerObject(new Context(readerResource(resource)).withFunctions(functions));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param resource the resource in the classpath that contains the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.3
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final String resource,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables) {
    return transformerObject(
        new Context(readerResource(resource)).withFunctions(functions).withVariables(variables));
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
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final String resource,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    return transformerObject(
        new Context(readerResource(resource))
            .withFunctions(functions)
            .withVariables(variables)
            .withResolver(resolver));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param file the JSLT file.
   * @return The transformer function.
   * @since 1.2.2
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(final File file) {
    return transformerObject(new Context(reader(file)));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param file the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final File file, final Collection<Function> functions) {
    return transformerObject(new Context(reader(file)).withFunctions(functions));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param file the JSLT file.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.3
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final File file,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables) {
    return transformerObject(
        new Context(reader(file)).withFunctions(functions).withVariables(variables));
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
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final File file,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    return transformerObject(
        new Context(reader(file))
            .withFunctions(functions)
            .withVariables(variables)
            .withResolver(resolver));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param in the JSLT input stream.
   * @return The transformer function.
   * @since 1.2.2
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(final InputStream in) {
    return transformerObject(new Context(reader(in)));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param in the JSLT input stream.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final InputStream in, final Collection<Function> functions) {
    return transformerObject(new Context(reader(in)).withFunctions(functions));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param in the JSLT input stream.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.3
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final InputStream in,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables) {
    return transformerObject(
        new Context(reader(in)).withFunctions(functions).withVariables(variables));
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
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final InputStream in,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    return transformerObject(
        new Context(reader(in))
            .withFunctions(functions)
            .withVariables(variables)
            .withResolver(resolver));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param reader the JSLT reader.
   * @return The transformer function.
   * @since 1.2.2
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(final Reader reader) {
    return transformerObject(new Context(reader));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param reader the JSLT reader.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.2
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final Reader reader, final Collection<Function> functions) {
    return transformerObject(new Context(reader).withFunctions(functions));
  }

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param reader the JSLT reader.
   * @param functions a collection of custom functions. It may be <code>null</code>.
   * @param variables pre-set variables. It may be <code>null</code>.
   * @return The transformer function.
   * @since 1.2.3
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final Reader reader,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables) {
    return transformerObject(new Context(reader).withFunctions(functions).withVariables(variables));
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
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformer(
      final Reader reader,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    return transformerObject(
        new Context(reader)
            .withFunctions(functions)
            .withVariables(variables)
            .withResolver(resolver));
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
   * @param jslt the JSLT script.
   * @return The transformer function.
   * @since 1.2.2
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
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
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
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
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
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
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> transformerString(
      final String jslt,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    try {
      return transformer(new StringReader(jslt), functions, variables, resolver);
    } catch (JsltException e) {
      throw new JsltException(numberLines(jslt) + "\n" + e.getMessage());
    }
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

  /**
   * Returns a function that transforms JSON using a JSLT script.
   *
   * @param jslt the JSLT script. If it starts with "resource:" the script is loaded as a class path
   *     resource. If it denotes an existing file it will be loaded from that file. Otherwise it is
   *     interpreted as a JSLT script.
   * @return The transformer function.
   * @since 1.3.4
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
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
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
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
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
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
   * @deprecated Use the variant with the context.
   */
  @Deprecated(since = "1.4")
  public static UnaryOperator<JsonObject> tryTransformer(
      final String jslt,
      final Collection<Function> functions,
      final Map<String, JsonValue> variables,
      final ResourceResolver resolver) {
    return transformerObject(
        new Context(tryReader(jslt))
            .withFunctions(functions)
            .withVariables(variables)
            .withResolver(resolver));
  }

  /**
   * This custom function is called "uri-decode". It performs URI-decode on its argument.
   *
   * @return The generated function.
   * @since 1.3.13
   * @deprecated Use the JsltCustom version.
   */
  @Deprecated(since = "1.4")
  public static Function uriDecode() {
    return JsltCustom.uriDecode();
  }

  /**
   * This custom function is called "uri-encode". It performs URI-encoding on its argument.
   *
   * @return The generated function.
   * @since 1.3.13
   * @deprecated Use the JsltCustom version.
   */
  @Deprecated(since = "1.4")
  public static Function uriEncode() {
    return JsltCustom.uriEncode();
  }

  /**
   * This custom function is called "uuid". It generates a UUID.
   *
   * @return The generated function.
   * @since 1.3.6
   * @deprecated Use the JsltCustom version.
   */
  @Deprecated(since = "1.4")
  public static Function uuid() {
    return JsltCustom.uuid();
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
