package net.pincette.json;

import static net.pincette.json.JsonUtil.getValue;
import static net.pincette.util.StreamUtil.last;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * Some utilities to analyse JSON patches.
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class Patch {
  private static final String ADD = "add";
  private static final String COPY = "copy";
  private static final String FROM = "from";
  private static final String MOVE = "move";
  private static final String OP = "op";
  private static final String PATH = "path";
  private static final String REMOVE = "remove";
  private static final String REPLACE = "replace";
  private static final Set<String> OPS =
      net.pincette.util.Collections.set(ADD, COPY, MOVE, REMOVE, REPLACE);
  private static final Set<String> SET_OPS =
      net.pincette.util.Collections.set(ADD, COPY, MOVE, REPLACE);
  private static final String VALUE = "/value";

  private Patch() {}

  /**
   * Returns whether or not the field indicated by <code>jsonPointer</code> was added.
   *
   * @param patch the patch that is applied.
   * @param original the JSON object to which the patch will be applied.
   * @param jsonPointer the location of the field in the original JSON object.
   * @return <code>true</code> when the field was added, <code>false</code> otherwise.
   * @since 1.0
   */
  public static boolean added(
      final JsonArray patch, final JsonObject original, final String jsonPointer) {
    return !getValue(original, jsonPointer).isPresent() && isSet(patch, jsonPointer);
  }

  /**
   * Returns whether or not the field indicated by <code>jsonPointer</code> was changed.
   *
   * @param patch the patch that is applied.
   * @param jsonPointer the location of the field in the original JSON object.
   * @return <code>true</code> when the field was changed, <code>false</code> otherwise.
   * @since 1.0
   */
  public static boolean changed(final JsonArray patch, final String jsonPointer) {
    return changes(patch, jsonPointer, false).findFirst().isPresent();
  }

  /**
   * Returns whether or not the field indicated by <code>jsonPointer</code> was changed between the
   * values <code>from</code> and <code>to</code>.
   *
   * @param patch the patch that is applied.
   * @param original the JSON object to which the patch will be applied.
   * @param jsonPointer the location of the field in the original JSON object.
   * @param from the original value.
   * @param to the new value.
   * @return <code>true</code> when the field was changed in the indicated way, <code>false</code>
   *     otherwise.
   * @since 1.0
   */
  public static boolean changed(
      final JsonArray patch,
      final JsonObject original,
      final String jsonPointer,
      final JsonValue from,
      final JsonValue to) {
    final JsonObject[] changes = changes(patch, jsonPointer, true).toArray(JsonObject[]::new);

    return isRemoveThenAdd(changes, original, from, to)
        || isReplace(changes, original, from, to)
        || isMove(changes, original, from, to);
  }

  /**
   * Returns whether or not the field indicated by <code>jsonPointer</code> was changed to the value
   * <code>to</code>.
   *
   * @param patch the patch that is applied.
   * @param original the JSON object to which the patch will be applied.
   * @param jsonPointer the location of the field in the original JSON object.
   * @param to the new value.
   * @return <code>true</code> when the field was changed in the indicated way, <code>false</code>
   *     otherwise.
   * @since 1.0
   */
  public static boolean changed(
      final JsonArray patch,
      final JsonObject original,
      final String jsonPointer,
      final JsonValue to) {
    final JsonObject[] changes = changes(patch, jsonPointer, true).toArray(JsonObject[]::new);

    return isRemoveThenAdd(changes, to) || isReplace(changes, to) || isMove(changes, original, to);
  }

  private static Stream<JsonObject> changes(final JsonArray patch) {
    return patch.stream()
        .filter(JsonUtil::isObject)
        .map(JsonValue::asJsonObject)
        .filter(json -> json.containsKey(OP));
  }

  private static Stream<JsonObject> changes(
      final JsonArray patch, final String jsonPointer, final boolean exact) {
    return changes(patch, jsonPointer, exact, OPS);
  }

  private static Stream<JsonObject> changes(
      final JsonArray patch, final String jsonPointer, final boolean exact, final Set<String> ops) {
    return changes(patch)
        .filter(json -> ops.contains(json.getString(OP)))
        .filter(json -> comparePaths(json.getString(PATH, ""), jsonPointer, exact));
  }

  private static boolean comparePaths(final String found, final String given, final boolean exact) {
    return exact ? found.equals(given) : found.startsWith(given);
  }

  private static boolean isMove(
      final JsonObject[] changes,
      final JsonObject original,
      final JsonValue from,
      final JsonValue to) {
    return changes.length == 1
        && changes[0].getString("op").equals("move")
        && isValue(changes[0], "path", original, from)
        && isValue(changes[0], "from", original, to);
  }

  private static boolean isMove(
      final JsonObject[] changes, final JsonObject original, final JsonValue to) {
    return changes.length == 1
        && changes[0].getString("op").equals("move")
        && isValue(changes[0], "from", original, to);
  }

  private static boolean isRemove(final JsonObject change, final String jsonPointer) {
    return Optional.of(change.getString(OP))
        .map(
            op ->
                (op.equals(REMOVE) && isSubject(change, PATH, jsonPointer))
                    || (op.equals(MOVE) && isSubject(change, FROM, jsonPointer)))
        .orElse(false);
  }

  private static boolean isRemoveThenAdd(
      final JsonObject[] changes,
      final JsonObject original,
      final JsonValue from,
      final JsonValue to) {
    return changes.length == 2
        && changes[0].getString(OP).equals(REMOVE)
        && changes[1].getString(OP).equals(ADD)
        && changes[1].getValue(VALUE).equals(to)
        && isValue(changes[0], PATH, original, from);
  }

  private static boolean isRemoveThenAdd(final JsonObject[] changes, final JsonValue to) {
    return changes.length == 2
        && changes[0].getString(OP).equals(REMOVE)
        && changes[1].getString(OP).equals(ADD)
        && changes[1].getValue(VALUE).equals(to);
  }

  private static boolean isReplace(
      final JsonObject[] changes,
      final JsonObject original,
      final JsonValue from,
      final JsonValue to) {
    return changes.length == 1
        && changes[0].getString(OP).equals(REPLACE)
        && changes[0].getValue(VALUE).equals(to)
        && isValue(changes[0], PATH, original, from);
  }

  private static boolean isReplace(final JsonObject[] changes, final JsonValue to) {
    return changes.length == 1
        && changes[0].getString(OP).equals(REPLACE)
        && changes[0].getValue(VALUE).equals(to);
  }

  /**
   * Returns whether or not the field indicated by <code>jsonPointer</code> was set to something.
   *
   * @param patch the patch that is applied.
   * @param jsonPointer the location of the field in the original JSON object.
   * @return <code>true</code> when the field was set, <code>false</code> otherwise.
   * @since 1.0
   */
  public static boolean isSet(final JsonArray patch, final String jsonPointer) {
    return changes(patch, jsonPointer, true, SET_OPS).findFirst().isPresent();
  }

  private static boolean isSet(final JsonObject change, final String jsonPointer) {
    return Optional.of(change.getString(OP))
        .map(op -> SET_OPS.contains(op) && isSubject(change, PATH, jsonPointer))
        .orElse(false);
  }

  private static boolean isSubject(
      final JsonObject change, final String attribute, final String jsonPointer) {
    return Optional.ofNullable(change.getString(attribute, ""))
        .map(value -> value.equals(jsonPointer))
        .orElse(false);
  }

  private static boolean isValue(
      final JsonObject change,
      final String attribute,
      final JsonObject original,
      final JsonValue value) {
    return getValue(original, change.getString(attribute)).filter(v -> v.equals(value)).isPresent();
  }

  /**
   * Returns whether or not the field indicated by <code>jsonPointer</code> was removed.
   *
   * @param patch the patch that is applied.
   * @param jsonPointer the location of the field in the original JSON object.
   * @return <code>true</code> when the field was removed, <code>false</code> otherwise.
   * @since 1.0
   */
  public static boolean removed(final JsonArray patch, final String jsonPointer) {
    return last(removedOrSet(patch, jsonPointer))
        .map(change -> isRemove(change, jsonPointer))
        .orElse(false);
  }

  private static Stream<JsonObject> removedOrSet(final JsonArray patch, final String jsonPointer) {
    return changes(patch)
        .filter(change -> isRemove(change, jsonPointer) || isSet(change, jsonPointer));
  }
}
