package net.pincette.json;

import static java.util.Optional.ofNullable;
import static javax.json.JsonValue.NULL;
import static net.pincette.json.JsonUtil.createArrayBuilder;
import static net.pincette.json.JsonUtil.createObjectBuilder;
import static net.pincette.util.Array.append;

import java.util.function.Function;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * Traverses JSON objects and arrays returning new ones that may be transformed with the given
 * function. When this function returns a scalar value, traversal stops there. When the function
 * transforms an entire object or array, the result will be further traversed.
 *
 * @author Werner Donné
 * @since 2.1
 */
public class ForEach {
  private ForEach() {}

  /**
   * Traverses a JSON object with the given function. <code>JsonValue.NULL</code> values are not
   * kept.
   *
   * @param json the object.
   * @param transform the transformation function.
   * @return The new object. If the top transformation doesn't return an object, <code>null</code>
   *     is returned.
   */
  public static JsonObject forEach(
      final JsonObject json, final Function<Location, JsonValue> transform) {
    return forEach(json, transform, false);
  }

  /**
   * Traverses a JSON object with the given function.
   *
   * @param json the object.
   * @param transform the transformation function.
   * @param keepNull when set, returned <code>JsonValue.NULL</code> values are kept in objects and
   *     arrays.
   * @return The new object. If the top transformation doesn't return an object, <code>null</code>
   *     is returned.
   */
  public static JsonObject forEach(
      final JsonObject json,
      final Function<Location, JsonValue> transform,
      final boolean keepNull) {
    return ofNullable(forEach(new Location().withValue(json), transform, keepNull))
        .filter(JsonUtil::isObject)
        .map(JsonValue::asJsonObject)
        .orElse(null);
  }

  /**
   * Traverses a JSON array with the given function. <code>JsonValue.NULL</code> values are not
   * kept.
   *
   * @param json the array.
   * @param transform the transformation function.
   * @return The new object. If the top transformation doesn't return an array, <code>null</code> is
   *     returned.
   */
  public static JsonArray forEach(
      final JsonArray json, final Function<Location, JsonValue> transform) {
    return forEach(json, transform, false);
  }

  /**
   * Traverses a JSON array with the given function.
   *
   * @param json the array.
   * @param transform the transformation function.
   * @param keepNull when set, returned <code>JsonValue.NULL</code> values are kept in objects and
   *     arrays.
   * @return The new object. If the top transformation doesn't return an array, <code>null</code> is
   *     returned.
   */
  public static JsonArray forEach(
      final JsonArray json, final Function<Location, JsonValue> transform, boolean keepNull) {
    return ofNullable(forEach(new Location().withValue(json), transform, keepNull))
        .filter(JsonUtil::isArray)
        .map(JsonValue::asJsonArray)
        .orElse(null);
  }

  private static JsonValue forEach(
      final Location location,
      final Function<Location, JsonValue> transform,
      final boolean keepNull) {
    final JsonValue result = transform.apply(location);

    return switch (result.getValueType()) {
      case NUMBER, STRING, TRUE, FALSE, NULL -> result;
      case OBJECT -> forEach(location, result.asJsonObject(), transform, keepNull);
      case ARRAY -> forEach(location, result.asJsonArray(), transform, keepNull);
    };
  }

  private static JsonValue forEach(
      final Location location,
      final JsonObject json,
      final Function<Location, JsonValue> transform,
      final boolean keepNull) {
    return json.entrySet().stream()
        .reduce(
            createObjectBuilder(),
            (b, e) ->
                ofNullable(
                        forEach(
                            new Location()
                                .withField(e.getKey())
                                .withPath(append(location.path, e.getKey()))
                                .withValue(e.getValue()),
                            transform,
                            keepNull))
                    .filter(v -> keepNull || v != NULL)
                    .map(v -> b.add(e.getKey(), v))
                    .orElse(b),
            (b1, b2) -> b1)
        .build();
  }

  private static JsonValue forEach(
      final Location location,
      final JsonArray json,
      final Function<Location, JsonValue> transform,
      final boolean keepNull) {
    return json.stream()
        .reduce(
            createArrayBuilder(),
            (b, v) ->
                ofNullable(forEach(location.withValue(v).withIsArrayValue(), transform, keepNull))
                    .filter(t -> keepNull || t != NULL)
                    .map(b::add)
                    .orElse(b),
            (b1, b2) -> b1)
        .build();
  }

  public static class Location {

    /**
     * The field of the value within the parent object. If the value is within an array, it is the
     * field of the array within its parent object.
     */
    public final String field;

    /** Is set when this is a value in an array. */
    public final boolean isArrayValue;

    /**
     * The field path up to this value. If the value is within an array, it is the path up to the
     * array.
     */
    public final String[] path;

    /** The value that is visited. */
    public final JsonValue value;

    private Location() {
      this(null, new String[0], null, false);
    }

    private Location(
        final String field,
        final String[] path,
        final JsonValue value,
        final boolean isArrayValue) {
      this.field = field;
      this.path = path;
      this.value = value;
      this.isArrayValue = isArrayValue;
    }

    private Location withField(final String field) {
      return new Location(field, path, value, isArrayValue);
    }

    private Location withIsArrayValue() {
      return new Location(field, path, value, true);
    }

    private Location withPath(final String[] path) {
      return new Location(field, path, value, isArrayValue);
    }

    private Location withValue(final JsonValue value) {
      return new Location(field, path, value, isArrayValue);
    }
  }
}
