package net.pincette.json;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static net.pincette.json.JsonUtil.copy;
import static net.pincette.json.JsonUtil.createArrayBuilder;
import static net.pincette.json.JsonUtil.createObjectBuilder;
import static net.pincette.json.JsonUtil.createValue;
import static net.pincette.json.JsonUtil.getParentPath;
import static net.pincette.json.JsonUtil.isObject;
import static net.pincette.util.Util.getLastSegment;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;

/**
 * A transformation utility for JSON. It lets you set up a chain of transformers through which a
 * JSON structure is run. The changes performed by one element in the chain are visible to the next.
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class Transform {
  private Transform() {}

  /**
   * Returns a transformer that adds or replaces the value of the field designated by the
   * dot-separated <code>path</code> with <code>value</code>.
   *
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The transformer.
   * @since 1.0
   */
  public static Transformer addTransformer(final String path, final JsonValue value) {
    final String parent = getParentPath(path);

    return new Transformer(
        e -> e.path.equals(parent) && isObject(e.value),
        e ->
            getLastSegment(path, ".")
                .map(
                    s ->
                        new JsonEntry(
                            e.path,
                            createObjectBuilder(e.value.asJsonObject()).add(s, value).build())));
  }

  /**
   * Returns a transformer that adds or replaces the value of the field designated by the
   * dot-separated <code>path</code> with <code>value</code>.
   *
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The transformer.
   * @since 1.0
   */
  public static Transformer addTransformer(final String path, final Object value) {
    return addTransformer(path, createValue(value));
  }

  private static String getKey(final String path, final String originalKey) {
    return Optional.ofNullable(originalKey)
        .map(path::lastIndexOf)
        .filter(index -> index != -1)
        .map(index -> originalKey)
        .orElseGet(() -> JsonUtil.getKey(path));
  }

  private static String getPath(final String parent, final String key) {
    return (parent != null && !"".equals(parent) ? (parent + ".") : "") + key;
  }

  /**
   * Returns a transformer that does nothing.
   *
   * @return The transformer.
   * @since 1.0
   */
  public static Transformer nopTransformer() {
    return new Transformer(e -> false, Optional::of);
  }

  public static Transformer removeTransformer(final String path) {
    return new Transformer(e -> e.path.equals(path), e -> Optional.empty());
  }

  /**
   * Returns a transformer that replaces the value of the existing field designated by the
   * dot-separated <code>path</code> with <code>value</code>.
   *
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The transformer.
   * @since 1.0
   */
  public static Transformer setTransformer(final String path, final JsonValue value) {
    return new Transformer(
        e -> e.path.equals(path), e -> Optional.of(new JsonEntry(e.path, value)));
  }

  /**
   * Returns a transformer that replaces the value of the existing field designated by the
   * dot-separated <code>path</code> with <code>value</code>.
   *
   * @param path the dot-separated path.
   * @param value the new value.
   * @return The transformer.
   * @since 1.0
   */
  public static Transformer setTransformer(final String path, final Object value) {
    return setTransformer(path, createValue(value));
  }

  /**
   * Returns a new value where recursively entries that <code>match</code> are transformed by <code>
   * transformer</code>. If the latter is empty the entry is removed from the result.
   *
   * @param json the given JSON value.
   * @param transformer the applied transformer.
   * @return The new JSON value.
   * @since 1.0
   */
  public static JsonValue transform(final JsonValue json, final Transformer transformer) {
    return transform(json, null, transformer);
  }

  private static JsonValue transform(
      final JsonValue json, final String parent, final Transformer transformer) {
    return json instanceof JsonStructure
        ? transform((JsonStructure) json, parent, transformer)
        : json;
  }

  /**
   * Returns a new structure where recursively entries that <code>match</code> are transformed by
   * <code>transformer</code>. If the latter is empty the entry is removed from the result.
   *
   * @param json the given JSON structure.
   * @param transformer the applied transformer.
   * @return The new JSON structure.
   * @since 1.0
   */
  public static JsonStructure transform(final JsonStructure json, final Transformer transformer) {
    return transform(json, null, transformer);
  }

  private static JsonStructure transform(
      final JsonStructure json, final String parent, final Transformer transformer) {
    return json instanceof JsonArray
        ? transform((JsonArray) json, parent, transformer)
        : transform((JsonObject) json, parent, transformer);
  }

  /**
   * Returns a new array where entries of objects that <code>match</code> are transformed by <code>
   * transformer</code>. If the latter is empty the entry is removed from the result.
   *
   * @param array the given JSON array.
   * @param transformer the applied transformer.
   * @return The new JSON array.
   * @since 1.0
   */
  public static JsonArray transform(final JsonArray array, final Transformer transformer) {
    return transform(array, null, transformer);
  }

  private static JsonArray transform(
      final JsonArray array, final String parent, final Transformer transformer) {
    return array.stream()
        .filter(Objects::nonNull)
        .reduce(
            createArrayBuilder(),
            (b, v) -> b.add(transform(v, parent, transformer)),
            (b1, b2) -> b1)
        .build();
  }

  /**
   * Returns a new object where entries that <code>match</code> are transformed by <code>transformer
   * </code>. If the latter is empty the entry is removed from the result.
   *
   * @param obj the given JSON object.
   * @param transformer the applied transformer.
   * @return The new JSON object.
   * @since 1.0
   */
  public static JsonObject transform(final JsonObject obj, final Transformer transformer) {
    return transform(obj, null, transformer);
  }

  private static JsonObject transform(
      final JsonObject obj, final String parent, final Transformer transformer) {
    return concat(parent == null ? of("") : empty(), obj.keySet().stream())
        .reduce(
            createObjectBuilder(),
            (b, k) ->
                transformer
                    .run(
                        "".equals(k)
                            ? new JsonEntry("", obj)
                            : new JsonEntry(getPath(parent, k), obj.get(k)))
                    .map(
                        entry ->
                            new JsonEntry(
                                getPath(parent, getKey(entry.path, k)),
                                transform(
                                    entry.value,
                                    getPath(parent, getKey(entry.path, k)),
                                    transformer)))
                    .map(
                        entry ->
                            "".equals(k)
                                ? copy(entry.value.asJsonObject(), b)
                                : b.add(getKey(entry.path, k), entry.value))
                    .orElse(b),
            (b1, b2) -> b1)
        .build();
  }

  public static class JsonEntry {
    /** A dot-separated key path. */
    public final String path;

    /** A JSON value. */
    public final JsonValue value;

    /**
     * @param path a dot-separated key path.
     * @param value a JSON value.
     * @since 1.0
     */
    public JsonEntry(final String path, final JsonValue value) {
      this.path = path;
      this.value = value;
    }
  }

  public static class Transformer {
    public final Predicate<JsonEntry> match;
    public final Transformer next;
    public final Function<JsonEntry, Optional<JsonEntry>> transform;

    /**
     * If the transform function returns an empty <code>Optional</code> the entry is removed from
     * the result.
     *
     * @param match the function to test an entry. When <code>true</code> is returned the <code>
     *     transform</code> function will be executed.
     * @param transform the transform function.
     * @since 1.0
     */
    public Transformer(
        final Predicate<JsonEntry> match,
        final Function<JsonEntry, Optional<JsonEntry>> transform) {
      this.match = match;
      this.next = null;
      this.transform = transform;
    }

    private Transformer(final Transformer me, final Transformer next) {
      this.match = me.match;
      this.transform = me.transform;
      this.next = me.next != null ? new Transformer(me.next, next) : next;
    }

    public Optional<JsonEntry> run(final JsonEntry entry) {
      return runNext(!match.test(entry) ? Optional.of(entry) : transform.apply(entry));
    }

    private Optional<JsonEntry> runNext(final Optional<JsonEntry> entry) {
      return next != null ? entry.flatMap(next::run) : entry;
    }

    public Transformer thenApply(final Transformer transformer) {
      return new Transformer(this, transformer);
    }
  }
}
