package net.pincette.json.filter;

import static net.pincette.json.JsonUtil.createArrayBuilder;
import static net.pincette.json.JsonUtil.createObjectBuilder;
import static net.pincette.util.Pair.pair;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Consumer;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import net.pincette.util.Pair;

/**
 * Accumulates a JSON stream in a given JSON builder.
 *
 * @author Werner Donné
 * @since 1.0
 */
public class JsonBuilderGenerator extends JsonValueGenerator {
  private final Deque<Pair<String, Object>> builders = new ArrayDeque<>();
  private Object builder;
  private String lastName;

  public JsonBuilderGenerator() {}

  public JsonBuilderGenerator(final JsonObjectBuilder builder) {
    builders.push(pair(null, builder));
  }

  public JsonBuilderGenerator(final JsonArrayBuilder builder) {
    builders.push(pair(null, builder));
  }

  private static void add(final Object parent, final Pair<String, Object> builder) {
    if (parent instanceof JsonObjectBuilder jsonObjectBuilder) {
      if (builder.first != null) {
        jsonObjectBuilder.add(builder.first, build(builder.second));
      }
    } else {
      ((JsonArrayBuilder) parent).add(build(builder.second));
    }
  }

  private static JsonStructure build(final Object builder) {
    return builder instanceof JsonObjectBuilder jsonObjectBuilder
        ? jsonObjectBuilder.build()
        : ((JsonArrayBuilder) builder).build();
  }

  private Optional<JsonObjectBuilder> asObjectBuilder() {
    return Optional.ofNullable(builders.peek()).map(p -> (JsonObjectBuilder) p.second);
  }

  private Optional<JsonArrayBuilder> asArrayBuilder() {
    return Optional.ofNullable(builders.peek()).map(p -> (JsonArrayBuilder) p.second);
  }

  /**
   * Use this method only when no builder was passed through a constructor, because in that case
   * everything is added to that builder.
   *
   * @return The created JSON structure.
   * @since 1.0
   */
  public JsonStructure build() {
    if (builder == null) {
      throw new IllegalStateException("Object or array is not complete");
    }

    return build(builder);
  }

  private void checkNoLastName() {
    if (lastName != null) {
      throw new JsonException("writeKey was called without following value");
    }
  }

  @Override
  public JsonGenerator write(final JsonValue value) {
    writeAnonymous(
        name -> asObjectBuilder().ifPresent(b -> b.add(name, value)),
        () -> asArrayBuilder().ifPresent(b -> b.add(value)));

    return this;
  }

  @Override
  public JsonGenerator write(final String name, final JsonValue value) {
    checkNoLastName();
    asObjectBuilder().ifPresent(b -> b.add(name, value));

    return this;
  }

  private void writeAnonymous(final Consumer<String> objectBuilder, final Runnable arrayBuilder) {
    if (lastName != null) {
      objectBuilder.accept(lastName);
      lastName = null;
    } else {
      arrayBuilder.run();
    }
  }

  @Override
  public JsonGenerator writeEnd() {
    final Pair<String, Object> bldr = builders.pop();

    Optional.ofNullable(builders.peek()).map(p -> p.second).ifPresent(b -> add(b, bldr));

    if (builders.isEmpty()) {
      builder = bldr.second;
    }

    return this;
  }

  @Override
  public JsonGenerator writeKey(final String name) {
    lastName = name;

    return this;
  }

  @Override
  public JsonGenerator writeNull() {
    return write(JsonValue.NULL);
  }

  @Override
  public JsonGenerator writeNull(final String name) {
    return write(name, JsonValue.NULL);
  }

  @Override
  public JsonGenerator writeStartArray() {
    builders.push(pair(lastName, createArrayBuilder()));
    lastName = null;

    return this;
  }

  @Override
  public JsonGenerator writeStartArray(final String name) {
    checkNoLastName();
    builders.push(pair(name, createArrayBuilder()));

    return this;
  }

  @Override
  public JsonGenerator writeStartObject() {
    builders.push(pair(lastName, createObjectBuilder()));
    lastName = null;

    return this;
  }

  @Override
  public JsonGenerator writeStartObject(final String name) {
    checkNoLastName();
    builders.push(pair(name, createObjectBuilder()));

    return this;
  }
}
