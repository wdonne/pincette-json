package net.pincette.json;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.String.join;
import static java.time.Instant.ofEpochMilli;
import static java.time.Instant.parse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createParserFactory;
import static javax.json.Json.createReader;
import static javax.json.Json.createWriterFactory;
import static javax.json.JsonValue.ValueType.FALSE;
import static javax.json.JsonValue.ValueType.TRUE;
import static javax.xml.stream.XMLOutputFactory.newInstance;
import static net.pincette.json.Transform.addTransformer;
import static net.pincette.json.Transform.setTransformer;
import static net.pincette.json.Transform.transform;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.autoClose;
import static net.pincette.util.Util.getLastSegment;
import static net.pincette.util.Util.getSegments;
import static net.pincette.util.Util.pathSearch;
import static net.pincette.util.Util.tryToDoWith;
import static net.pincette.util.Util.tryToDoWithRethrow;
import static net.pincette.util.Util.tryToGetSilent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.xml.stream.XMLEventWriter;
import net.pincette.function.SideEffect;
import net.pincette.util.Pair;
import net.pincette.xml.JsonEventReader;

/**
 * Some JSON-utilities.
 *
 * @author Werner Donn\u00e9
 */
public class JsonUtil {
  public static final Function<JsonObject, ?> EVALUATOR =
      value ->
          value.getValueType() == JsonValue.ValueType.NUMBER
              ? asNumber(value).longValue()
              : toString(value);

  private JsonUtil() {}

  public static JsonObject add(
      final JsonObject obj, final String name, final JsonArrayBuilder value) {
    return add(obj, builder -> builder.add(name, value));
  }

  public static JsonObject add(
      final JsonObject obj, final String name, final JsonObjectBuilder value) {
    return add(obj, builder -> builder.add(name, value));
  }

  /**
   * Returns a new object in which the value of the field designated by the dot-separated <code>path
   * </code> is added or replaced with <code>value</code>.
   *
   * @param obj the given JSON object.
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The new object.
   */
  public static JsonObject add(final JsonObject obj, final String path, final JsonValue value) {
    return transform(obj, addTransformer(path, value));
  }

  /**
   * Returns a new object in which the value of the field designated by the dot-separated <code>path
   * </code> is added or replaced with <code>value</code>.
   *
   * @param obj the given JSON object.
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The new object.
   */
  public static JsonObject add(final JsonObject obj, final String path, final Object value) {
    return add(obj, path, createValue(value));
  }

  public static JsonObject add(final JsonObject obj, final UnaryOperator<JsonObjectBuilder> add) {
    return add.apply(copy(obj, createObjectBuilder())).build();
  }

  public static JsonObject add(final JsonObject obj1, final JsonObject obj2) {
    return add(obj1, builder -> copy(obj2, builder));
  }

  public static JsonObject add(final JsonObject obj, final Map<String, ?> fields) {
    return add(obj, builder -> add(builder, fields));
  }

  public static JsonObjectBuilder add(
      final JsonObjectBuilder builder, final Map<String, ?> fields) {
    return fields.entrySet().stream()
        .reduce(builder, (b, e) -> addJsonField(b, e.getKey(), e.getValue()), (b1, b2) -> b1);
  }

  public static JsonObjectBuilder add(final JsonObjectBuilder builder, final JsonObject obj) {
    return copy(obj, builder);
  }

  public static JsonArrayBuilder add(final JsonArrayBuilder builder, final JsonArray array) {
    return array.stream().reduce(builder, JsonArrayBuilder::add, (b1, b2) -> b1);
  }

  /**
   * Returns a copy of the object where the values of the fields in <code>values</code> are
   * replaced. The first entry of a pair is the name and the second entry is the new value.
   *
   * @param obj the given JSON object.
   * @param values the list of pairs, where the first entry is the name and the second entry is the
   *     new value.
   * @return The new object.
   */
  public static JsonObject add(final JsonObject obj, Collection<Pair<String, Object>> values) {
    return values.stream()
        .reduce(
            copy(obj, createObjectBuilder()),
            (b, p) -> addJsonField(b, p.first, p.second),
            (b1, b2) -> b1)
        .build();
  }

  /**
   * Executes <code>add</code> if <code>test</code> returns <code>true</code>.
   *
   * @param builder the builder
   * @param test the test function
   * @param add the add function
   * @return The builder.
   */
  public static JsonObjectBuilder addIf(
      final JsonObjectBuilder builder,
      final BooleanSupplier test,
      final UnaryOperator<JsonObjectBuilder> add) {
    return test.getAsBoolean() ? add.apply(builder) : builder;
  }

  public static JsonObjectBuilder addJsonField(
      final JsonObjectBuilder builder, final String name, final Object value) {
    return builder.add(name, createValue(value));
  }

  public static JsonArrayBuilder addJsonField(final JsonArrayBuilder builder, final Object value) {
    return builder.add(createValue(value));
  }

  public static JsonArray asArray(final JsonValue value) {
    if (value.getValueType() != JsonValue.ValueType.ARRAY) {
      throw new JsonException("Not an array");
    }

    return (JsonArray) value;
  }

  public static double asDouble(final JsonValue value) {
    return asNumber(value).doubleValue();
  }

  public static Instant asInstant(final JsonValue value) {
    return parse(asString(value).getString());
  }

  public static int asInt(final JsonValue value) {
    return asNumber(value).intValueExact();
  }

  public static long asLong(final JsonValue value) {
    return asNumber(value).longValueExact();
  }

  public static JsonNumber asNumber(final JsonValue value) {
    if (value.getValueType() != JsonValue.ValueType.NUMBER) {
      throw new JsonException("Not a number");
    }

    return (JsonNumber) value;
  }

  public static JsonObject asObject(final JsonValue value) {
    if (value.getValueType() != JsonValue.ValueType.OBJECT) {
      throw new JsonException("Not an object");
    }

    return (JsonObject) value;
  }

  public static JsonString asString(final JsonValue value) {
    if (value.getValueType() != JsonValue.ValueType.STRING) {
      throw new JsonException("Not a string");
    }

    return (JsonString) value;
  }

  private static Object convertNumber(final JsonNumber number) {
    return number.isIntegral() ? (Object) number.longValue() : (Object) number.doubleValue();
  }

  public static JsonObjectBuilder copy(final JsonObject obj, final JsonObjectBuilder builder) {
    return copy(obj, builder, key -> true);
  }

  public static JsonObjectBuilder copy(
      final JsonObject obj, final JsonObjectBuilder builder, final Predicate<String> retain) {
    return copy(obj, builder, (key, o) -> retain.test(key));
  }

  public static JsonObjectBuilder copy(
      final JsonObject obj,
      final JsonObjectBuilder builder,
      final BiPredicate<String, JsonObject> retain) {
    return obj.keySet().stream()
        .filter(key -> retain.test(key, obj))
        .reduce(builder, (b, key) -> b.add(key, obj.get(key)), (b1, b2) -> b1);
  }

  public static JsonArrayBuilder copy(
      final JsonArray array, final JsonArrayBuilder builder, final Predicate<JsonValue> retain) {
    return array.stream().filter(retain).reduce(builder, JsonArrayBuilder::add, (b1, b2) -> b1);
  }

  public static JsonValue createValue(final Object value) {
    if (value == null) {
      return JsonValue.NULL;
    }

    if (value instanceof Boolean) {
      return ((boolean) value) ? JsonValue.TRUE : JsonValue.FALSE;
    }

    if (value instanceof Integer) {
      return javax.json.Json.createValue((int) value);
    }

    if (value instanceof Long) {
      return javax.json.Json.createValue((long) value);
    }

    if (value instanceof BigInteger) {
      return javax.json.Json.createValue((BigInteger) value);
    }

    if (value instanceof BigDecimal) {
      return javax.json.Json.createValue((BigDecimal) value);
    }

    if (value instanceof Double || value instanceof Float) {
      return javax.json.Json.createValue(((Number) value).doubleValue());
    }

    if (value instanceof Date) {
      return javax.json.Json.createValue(ofEpochMilli(((Date) value).getTime()).toString());
    }

    if (value instanceof Map) {
      return from((Map) value);
    }

    if (value instanceof Stream) {
      return from((Stream) value);
    }

    if (value instanceof List) {
      return from((List) value);
    }

    return javax.json.Json.createValue(value.toString());
  }

  public static JsonArray emptyArray() {
    return createArrayBuilder().build();
  }

  public static JsonObject emptyObject() {
    return createObjectBuilder().build();
  }

  public static Object evaluate(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.NUMBER
        ? (Object) asNumber(value).longValue()
        : toString(value);
  }

  public static JsonObject from(final Map<String, ?> fields) {
    return add(createObjectBuilder(), fields).build();
  }

  public static JsonArray from(final List<?> values) {
    return from(values.stream());
  }

  public static JsonArray from(final Stream<?> values) {
    return values.reduce(createArrayBuilder(), JsonUtil::addJsonField, (b1, b2) -> b1).build();
  }

  public static Optional<JsonStructure> from(final String json) {
    return tryToGetSilent(() -> createReader(new StringReader(json)).read());
  }

  public static Optional<JsonArray> getArray(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer).filter(JsonUtil::isArray).map(JsonValue::asJsonArray);
  }

  /**
   * Returns the value for <code>field</code>, which may be dot-separated.
   *
   * @param obj the given JSON object.
   * @param field the query field.
   * @return The optional result value.
   */
  public static Optional<JsonValue> get(final JsonObject obj, final String field) {
    return pathSearch(obj, field.split("\\."));
  }

  public static Optional<Boolean> getBoolean(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer)
        .filter(v -> v.getValueType() == TRUE || v.getValueType() == FALSE)
        .map(v -> v.getValueType() == TRUE);
  }

  public static Optional<Instant> getInstant(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer).filter(JsonUtil::isInstant).map(JsonUtil::asInstant);
  }

  /**
   * Returns the last segment of a dot-separated path.
   *
   * @param path the given path.
   * @return The last segment.
   */
  public static String getKey(final String path) {
    return getLastSegment(path, "\\.").orElse(path);
  }

  public static Optional<Double> getNumber(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer)
        .filter(JsonUtil::isNumber)
        .map(JsonUtil::asNumber)
        .map(JsonNumber::doubleValue);
  }

  public static Stream<Double> getNumbers(final JsonObject json, final String array) {
    return getValues(json, array)
        .filter(JsonUtil::isNumber)
        .map(JsonUtil::asNumber)
        .map(JsonNumber::doubleValue);
  }

  public static Optional<JsonObject> getObject(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer).filter(JsonUtil::isObject).map(JsonValue::asJsonObject);
  }

  public static Stream<JsonObject> getObjects(final JsonObject json, final String array) {
    return getValues(json, array).filter(JsonUtil::isObject).map(JsonValue::asJsonObject);
  }

  /**
   * Returns the parent path of a dot-separated path, or the empty string if the path has only one
   * segment.
   *
   * @param path the given dot-separated path.
   * @return The dot-separated parent path.
   */
  public static String getParentPath(final String path) {
    return Optional.of(path.lastIndexOf('.'))
        .filter(i -> i != -1)
        .map(i -> path.substring(0, i))
        .orElse("");
  }

  public static Optional<String> getString(final JsonStructure json, final String jsonPointer) {
    return getValue(json, jsonPointer)
        .filter(JsonUtil::isString)
        .map(JsonUtil::asString)
        .map(JsonString::getString);
  }

  public static Stream<String> getStrings(final JsonObject json, final String array) {
    return getValues(json, array)
        .filter(JsonUtil::isString)
        .map(JsonUtil::asString)
        .map(JsonString::getString);
  }

  public static Optional<JsonValue> getValue(final JsonStructure json, final String jsonPointer) {
    return tryToGetSilent(() -> json.getValue(jsonPointer));
  }

  public static Stream<JsonValue> getValues(final JsonObject json, final String array) {
    return Optional.ofNullable(json.getJsonArray(array))
        .map(JsonArray::stream)
        .orElseGet(Stream::empty);
  }

  public static boolean isArray(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.ARRAY;
  }

  public static boolean isBoolean(final JsonValue value) {
    return value.getValueType() == TRUE || value.getValueType() == FALSE;
  }

  public static boolean isDate(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.STRING
        && net.pincette.util.Util.isDate(asString(value).getString());
  }

  public static boolean isDouble(final JsonValue value) {
    return isNumber(value)
        && Optional.of(asNumber(value).doubleValue())
            .filter(v -> v != NEGATIVE_INFINITY && v != POSITIVE_INFINITY)
            .isPresent();
  }

  public static boolean isEmail(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.STRING
        && net.pincette.util.Util.isEmail(asString(value).getString());
  }

  public static boolean isInstant(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.STRING
        && net.pincette.util.Util.isInstant(asString(value).getString());
  }

  public static boolean isInt(final JsonValue value) {
    return tryToGetSilent(() -> asNumber(value).intValueExact()).isPresent();
  }

  public static boolean isLong(final JsonValue value) {
    return tryToGetSilent(() -> asNumber(value).longValueExact()).isPresent();
  }

  public static boolean isNull(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.NULL;
  }

  public static boolean isNumber(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.NUMBER;
  }

  public static boolean isObject(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.OBJECT;
  }

  public static boolean isString(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.STRING;
  }

  public static boolean isUri(final JsonValue value) {
    return isUri(asString(value).getString());
  }

  public static boolean isUri(final String s) {
    return s.startsWith("/") || net.pincette.util.Util.isUri(s);
  }

  /**
   * Returns a stream of nested objects in document order.
   *
   * @param json the structure to navigate.
   * @return The stream of found objects.
   */
  public static Stream<JsonObject> nestedObjects(final JsonStructure json) {
    return isArray(json) ? nestedObjects(json.asJsonArray()) : nestedObjects(json.asJsonObject());
  }

  public static Stream<JsonObject> nestedObjects(final JsonObject json) {
    return nestedObjectsAndSelf(json.entrySet().stream().map(Map.Entry::getValue));
  }

  public static Stream<JsonObject> nestedObjects(final JsonArray json) {
    return nestedObjectsAndSelf(json.stream());
  }

  private static Stream<JsonObject> nestedObjectsAndSelf(final Stream<JsonValue> stream) {
    return stream
        .filter(j -> isObject(j) || isArray(j))
        .flatMap(
            j ->
                isObject(j)
                    ? concat(of(j.asJsonObject()), nestedObjects(j.asJsonObject()))
                    : nestedObjects(j.asJsonArray()));
  }

  public static JsonObject remove(final JsonObject obj, final Set<String> fields) {
    return remove(obj, fields::contains);
  }

  public static JsonObject remove(final JsonObject obj, final Predicate<String> pred) {
    return copy(obj, createObjectBuilder(), key -> !pred.test(key)).build();
  }

  public static JsonArray remove(final JsonArray array, final Predicate<JsonValue> pred) {
    return copy(array, createArrayBuilder(), value -> !pred.test(value)).build();
  }

  /**
   * Removes fields with a name that starts with an underscore.
   *
   * @param obj the given JSON object.
   * @return The new JSON object without the technical fields.
   */
  public static JsonObject removeTechnical(final JsonObject obj) {
    return copy(obj, createObjectBuilder(), key -> !key.startsWith("_")).build();
  }

  /**
   * Returns a new object in which the value of the existing field designated by the dot-separated
   * <code>path</code> is replaced with <code>value</code>.
   *
   * @param obj the given JSON object.
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The new object.
   */
  public static JsonObject set(final JsonObject obj, final String path, final JsonValue value) {
    return transform(obj, setTransformer(path, value));
  }

  /**
   * Returns a new object in which the value of the existing field designated by the dot-separated
   * <code>path</code> is replaced with <code>value</code>.
   *
   * @param obj the given JSON object.
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The new object.
   */
  public static JsonObject set(final JsonObject obj, final String path, final Object value) {
    return set(obj, path, createValue(value));
  }

  public static String string(final JsonValue json) {
    return string(json, false);
  }

  public static String string(final JsonValue json, final boolean pretty) {
    final Map<String, Object> config = new HashMap<>();
    final StringWriter writer = new StringWriter();

    if (pretty) {
      config.put(JsonGenerator.PRETTY_PRINTING, true);
    }

    tryToDoWith(() -> createWriterFactory(config).createWriter(writer), w -> w.write(json));

    return writer.toString();
  }

  /**
   * Transforms a JSON pointer in the form "/a/b/c" into a dot-separated field in the form "a.b.c".
   *
   * @param jsonPointer the given pointer.
   * @return The dot-separated field.
   */
  public static String toDotSeparated(final String jsonPointer) {
    return getSegments(jsonPointer, "/").collect(joining("."));
  }

  /**
   * Transforms a dot-separated field in the form "a.b.c" into a JSON pointer in the form "/a/b/c".
   *
   * @param dotSeparatedField the given field.
   * @return The JSON pointer.
   */
  public static String toJsonPointer(final String dotSeparatedField) {
    return "/" + join("/", dotSeparatedField.split("\\."));
  }

  /**
   * Converts <code>value</code> recursively to a Java value.
   *
   * @param value the given value.
   * @return The converted value.
   */
  public static Object toNative(final JsonValue value) {
    switch (value.getValueType()) {
      case ARRAY:
        return toNative(asArray(value));
      case FALSE:
        return false;
      case TRUE:
        return true;
      case NUMBER:
        return convertNumber(asNumber(value));
      case OBJECT:
        return toNative(asObject(value));
      case STRING:
        return asString(value).getString();
      case NULL:
        return null;
      default:
        return value;
    }
  }

  /**
   * Converts <code>array</code> recursively to a list with Java values.
   *
   * @param array the given array.
   * @return The generated list.
   */
  public static List<Object> toNative(final JsonArray array) {
    return array.stream().map(JsonUtil::toNative).collect(toList());
  }

  /**
   * Converts <code>object</code> recursively to a map with Java values. Null values in the object
   * will be omitted.
   *
   * @param object the given object.
   * @return The generated map.
   */
  public static Map<String, Object> toNative(final JsonObject object) {
    return object.entrySet().stream()
        .map(e -> pair(e.getKey(), toNative(e.getValue())))
        .filter(pair -> pair.second != null)
        .collect(toMap(pair -> pair.first, pair -> pair.second));
  }

  private static String toString(final JsonValue value) {
    return value.getValueType() == JsonValue.ValueType.STRING
        ? asString(value).getString()
        : value.toString();
  }

  public static InputStream transformToXML(final JsonStructure json) {
    return Optional.of(new ByteArrayOutputStream())
        .map(
            out ->
                SideEffect.<ByteArrayOutputStream>run(
                        () ->
                            tryToDoWithRethrow(
                                autoClose(
                                    () -> newInstance().createXMLEventWriter(out),
                                    XMLEventWriter::close),
                                writer ->
                                    writer.add(
                                        new JsonEventReader(
                                            json instanceof JsonObject
                                                ? createParserFactory(null)
                                                    .createParser((JsonObject) json)
                                                : createParserFactory(null)
                                                    .createParser((JsonArray) json)))))
                    .andThenGet(() -> out))
        .map(ByteArrayOutputStream::toByteArray)
        .map(ByteArrayInputStream::new)
        .orElse(null);
  }
}
