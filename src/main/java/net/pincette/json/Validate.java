package net.pincette.json;

import static java.util.stream.Collectors.toSet;
import static net.pincette.json.JsonUtil.createArrayBuilder;
import static net.pincette.json.JsonUtil.createObjectBuilder;
import static net.pincette.json.JsonUtil.emptyObject;
import static net.pincette.util.Collections.difference;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.allPaths;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import net.pincette.util.Pair;

/**
 * A validation utility for JSON.
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class Validate {
  public static final String ERROR = "_error";
  public static final String MESSAGE = "message";
  public static final String VALUE = "value";

  private Validate() {}

  /**
   * Creates a JSON object with the field "value" set to the original <code>value</code>, the
   * "message" field set to <code>message</code> and the field "_error" set to <code>true</code>.
   *
   * @param value the original value.
   * @param message the error message.
   * @return The error object.
   * @since 1.0
   */
  public static JsonObject createErrorObject(final JsonValue value, final String message) {
    return Optional.of(createObjectBuilder())
        .map(builder -> builder.add(ERROR, true))
        .map(builder -> builder.add(MESSAGE, message))
        .map(builder -> value != null ? builder.add(VALUE, value) : builder)
        .map(JsonObjectBuilder::build)
        .orElse(emptyObject());
  }

  private static Stream<String> getFieldVariants(final String field) {
    return allPaths(field, ".");
  }

  private static Set<String> getMandatoryKeys(final Set<String> all, final String parent) {
    return parent == null
        ? all.stream().filter(key -> key.indexOf('.') == -1).collect(toSet())
        : all.stream()
            .filter(key -> key.startsWith(parent + "."))
            .map(key -> key.substring(parent.length() + 1))
            .filter(key -> key.indexOf('.') == -1)
            .collect(toSet());
  }

  private static String getMessage(final Map<String, String> messages, final String field) {
    return getFieldVariants(field)
        .filter(messages::containsKey)
        .map(messages::get)
        .findFirst()
        .orElse("Error");
  }

  private static Validator getValidator(
      final Map<String, Validator> validators, final String field) {
    return getFieldVariants(field)
        .filter(validators::containsKey)
        .map(validators::get)
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns <code>true</code> if <code>obj</code> contains an entry with the name _error and value
   * <code>true</code>.
   *
   * @param obj the given JSON object.
   * @return Whether the object contains errors or not.
   * @since 1.0
   */
  public static boolean hasErrors(final JsonObject obj) {
    return obj.get(ERROR) != null && obj.getBoolean(ERROR, false);
  }

  /**
   * Returns <code>true</code> if any object in <code>array</code> contains an entry with the name
   * _error and value <code>true</code>.
   *
   * @param array the given JSON array.
   * @return Whether the array contains errors or not.
   * @since 1.0
   */
  public static boolean hasErrors(final JsonArray array) {
    return array.stream()
        .anyMatch(value -> value instanceof JsonObject && hasErrors((JsonObject) value));
  }

  public static boolean hasErrors(final JsonStructure json) {
    return (json instanceof JsonObject && hasErrors((JsonObject) json))
        || (json instanceof JsonArray && hasErrors((JsonArray) json));
  }

  public static ValidationResult isArray(final ValidationContext context) {
    return new ValidationResult(JsonUtil.isArray(context.value), null);
  }

  public static ValidationResult isBoolean(final ValidationContext context) {
    return new ValidationResult(JsonUtil.isBoolean(context.value), null);
  }

  public static ValidationResult isDate(final ValidationContext context) {
    return new ValidationResult(JsonUtil.isDate(context.value), null);
  }

  public static ValidationResult isEmail(final ValidationContext context) {
    return new ValidationResult(JsonUtil.isEmail(context.value), null);
  }

  public static ValidationResult isInstant(final ValidationContext context) {
    return new ValidationResult(JsonUtil.isInstant(context.value), null);
  }

  public static ValidationResult isNumber(final ValidationContext context) {
    return new ValidationResult(JsonUtil.isNumber(context.value), null);
  }

  public static ValidationResult isObject(final ValidationContext context) {
    return new ValidationResult(JsonUtil.isObject(context.value), null);
  }

  public static ValidationResult isString(final ValidationContext context) {
    return new ValidationResult(JsonUtil.isString(context.value), null);
  }

  public static ValidationResult isUri(final ValidationContext context) {
    return new ValidationResult(JsonUtil.isUri(context.value), null);
  }

  /**
   * The method validates <code>value</code>. If there are no errors <code>value</code> is returned
   * unchanged. Otherwise the fields for which there is an error are replaced with an object
   * containing the field "value" with the original value, the field "message" with an error message
   * and the field "_error", which is set to <code>true</code>. All ancestor objects will also have
   * the field _error set to <code>true</code>.
   *
   * @param value the object or array that is to be validated.
   * @param context the context information which is passed to each validator.
   * @param validators maps dot-separated fields to validator functions.
   * @param messages maps dot-separated fields to error messages.
   * @param mandatory the set of dot-separated fields that are must appear in <code>value</code>.
   *     When a field has several segments this only says something about the lowest level. For
   *     example, when the field "a.b" is present that "b" must be present when "a" is, but "a" as
   *     such doesn't have to be present. If that is also required then the field "a" should also be
   *     in the set.
   * @param missingMessage a general error message for missing fields.
   * @return An annotated copy of <code>value</code> when there is at least one error, just <code>
   *     value</code> otherwise.
   * @since 1.0
   */
  public static JsonStructure validate(
      final JsonStructure value,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    return (JsonStructure)
        validate(null, value, context, validators, messages, mandatory, missingMessage).first;
  }

  public static JsonObject validate(
      final JsonObject obj,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    return (JsonObject)
        validate(null, obj, context, validators, messages, mandatory, missingMessage).first;
  }

  public static JsonArray validate(
      final JsonArray array,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    return validate(null, array, context, validators, messages, mandatory, missingMessage).first;
  }

  private static Pair<? extends JsonValue, Boolean> validate(
      final String field,
      final JsonValue value,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    final Function<JsonValue, Pair<? extends JsonValue, Boolean>> ifArrayOr =
        v ->
            v instanceof JsonArray
                ? validate(
                    field,
                    v.asJsonArray(),
                    context,
                    validators,
                    messages,
                    mandatory,
                    missingMessage)
                : pair(v, false);

    return value instanceof JsonObject
        ? validate(
            field, value.asJsonObject(), context, validators, messages, mandatory, missingMessage)
        : ifArrayOr.apply(value);
  }

  private static Pair<JsonValue, Boolean> validate(
      final String parent,
      final JsonObject obj,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    final JsonObjectBuilder builder = createObjectBuilder();
    final Set<String> found = new HashSet<>();
    boolean errors =
        obj.keySet().stream()
            .map(
                key -> {
                  final String field = parent != null ? (parent + "." + key) : key;
                  final JsonValue value = obj.get(key);
                  final ValidationResult result =
                      Optional.ofNullable(getValidator(validators, field))
                          .map(validator -> validator.apply(context.with(field).with(value)))
                          .orElse(new ValidationResult(true, null));
                  final Supplier<String> message =
                      () -> result.message != null ? result.message : getMessage(messages, field);
                  final Pair<? extends JsonValue, Boolean> entry =
                      result.status
                          ? validate(
                              field,
                              value,
                              context,
                              validators,
                              messages,
                              mandatory,
                              missingMessage)
                          : pair(createErrorObject(value, message.get()), true);

                  found.add(key);
                  builder.add(key, entry.first);

                  return entry.second;
                })
            .reduce(false, (e1, e2) -> e1 || e2);

    errors |=
        difference(getMandatoryKeys(mandatory, parent), found).stream()
            .map(
                key -> {
                  builder.add(key, createErrorObject(null, missingMessage));

                  return true;
                })
            .reduce(false, (e1, e2) -> e1 || e2);

    if (errors) {
      builder.add(ERROR, true);
    }

    return pair(builder.build(), errors);
  }

  private static Pair<JsonArray, Boolean> validate(
      final String parent,
      final JsonArray array,
      final ValidationContext context,
      final Map<String, Validator> validators,
      final Map<String, String> messages,
      final Set<String> mandatory,
      final String missingMessage) {
    final JsonArrayBuilder builder = createArrayBuilder();
    final boolean errors =
        array.stream()
            .map(
                value -> {
                  final Pair<? extends JsonValue, Boolean> entry =
                      validate(
                          parent, value, context, validators, messages, mandatory, missingMessage);

                  builder.add(entry.first);

                  return entry.second;
                })
            .reduce(false, (e1, e2) -> e1 || e2);

    return pair(builder.build(), errors);
  }

  public interface Validator extends Function<ValidationContext, ValidationResult> {}

  public static class ValidationContext {
    public final String field;
    public final JsonStructure newJson;
    public final JsonStructure oldJson;
    public final JsonValue value;

    public ValidationContext(final JsonStructure oldJson, final JsonStructure newJson) {
      this(oldJson, newJson, null, null);
    }

    private ValidationContext(
        final JsonStructure oldJson,
        final JsonStructure newJson,
        final String field,
        final JsonValue value) {
      this.oldJson = oldJson;
      this.newJson = newJson;
      this.field = field;
      this.value = value;
    }

    private ValidationContext with(final String field) {
      return new ValidationContext(oldJson, newJson, field, value);
    }

    private ValidationContext with(final JsonValue value) {
      return new ValidationContext(oldJson, newJson, field, value);
    }
  }

  public static class ValidationResult {
    public final String message;
    public final boolean status;

    public ValidationResult(final boolean status) {
      this(status, null);
    }

    public ValidationResult(final boolean status, final String message) {
      this.status = status;
      this.message = message;
    }
  }
}
