package net.pincette.json;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.round;
import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
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
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Triple.triple;
import static net.pincette.util.Util.tryToGetRethrow;

import com.fasterxml.jackson.databind.JsonNode;
import com.schibsted.spt.data.jslt.Function;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import javax.json.JsonArray;
import javax.json.JsonString;
import javax.json.JsonValue;
import net.pincette.function.SideEffect;

/**
 * Customs functions for JSLT.
 *
 * @author Werner Donn\u00e9
 * @since 1.4
 */
public class JsltCustom {
  private JsltCustom() {}

  /**
   * This custom function is called "base64-decode". It performs Base64-decoding on its argument.
   *
   * @return The generated function.
   * @since 1.5
   */
  public static Function base64Decode() {
    return function(
        "base64-decode",
        1,
        1,
        array ->
            createValue(
                new String(
                    getDecoder().decode(asString(array.get(0)).getString().getBytes(US_ASCII)),
                    UTF_8)));
  }

  /**
   * This custom function is called "base64-encode". It performs Base64-encoding on its argument.
   *
   * @return The generated function.
   * @since 1.5
   */
  public static Function base64Encode() {
    return function(
        "base64-encode",
        1,
        1,
        array ->
            createValue(
                new String(
                    getEncoder().encode(asString(array.get(0)).getString().getBytes(UTF_8)),
                    US_ASCII)));
  }

  /**
   * Creates a collection of all custom functions except <code>trace</code>.
   *
   * @return The collection of functions.
   * @since 1.4
   */
  public static Collection<Function> customFunctions() {
    return list(
        base64Decode(),
        base64Encode(),
        getPointer(),
        parseIsoInstant(),
        pointer(),
        setPointer(),
        uriDecode(),
        uriEncode(),
        uuid());
  }

  /**
   * Wraps a lambda in a JSLT function.
   *
   * @param name the name of the function.
   * @param minArguments the minimum number of arguments.
   * @param maxArguments the maximum number of arguments.
   * @param function the lambda.
   * @return The JSLT function.
   * @since 1.4
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
   * @since 1.4
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
   * @since 1.4
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
   * @since 1.4
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
   * This custom function is called "set-pointer". It requires three arguments: a JSON object, a
   * JSON pointer and a value. The value will be set at the pointer location. The new object is
   * returned.
   *
   * @return The generated function.
   * @since 1.4
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

  /**
   * This custom function, which is called "trace" in JSLT, accepts one argument. It sends it to
   * <code>logger</code> with level <code>INFO</code> and then returns it.
   *
   * @param logger the given logger.
   * @return The generated function.
   * @since 1.4
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
   * This custom function is called "uri-decode". It performs URI-decoding on its argument.
   *
   * @return The generated function.
   * @since 1.4
   */
  public static Function uriDecode() {
    return function(
        "uri-decode",
        1,
        1,
        array ->
            createValue(
                tryToGetRethrow(() -> decode(asString(array.get(0)).getString(), "UTF-8"))
                    .orElse(null)));
  }

  /**
   * This custom function is called "uri-encode". It performs URI-encoding on its argument.
   *
   * @return The generated function.
   * @since 1.4
   */
  public static Function uriEncode() {
    return function(
        "uri-encode",
        1,
        1,
        array ->
            createValue(
                tryToGetRethrow(() -> encode(asString(array.get(0)).getString(), "UTF-8"))
                    .orElse(null)));
  }

  /**
   * This custom function is called "uuid". It generates a UUID.
   *
   * @return The generated function.
   * @since 1.4
   */
  public static Function uuid() {
    return function("uuid", 0, 0, array -> createValue(randomUUID().toString()));
  }

  static class CustomFunction implements Function {
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

    CustomFunction(final Function delegate) {
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
}
