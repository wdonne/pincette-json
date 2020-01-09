package net.pincette.json.filter;

import java.util.function.Supplier;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

/**
 * Removes empty arrays from the stream unless it is the top array.
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class RemoveEmptyArraysGenerator extends JsonGeneratorFilter {
  private static final String ANONYMOUS = "anonymous";

  private boolean first = true;
  private String name;
  private boolean pending = false;

  private JsonGenerator flushPending(final Supplier<JsonGenerator> then) {
    if (name != null) {
      if (name.equals(ANONYMOUS)) {
        super.writeStartArray();
      } else {
        super.writeStartArray(name);
      }

      name = null;
      pending = false;
    }

    return then.get();
  }

  private boolean hasRealName() {
    return name != null && !name.equals(ANONYMOUS);
  }

  private JsonGenerator reset() {
    name = null;
    pending = false;

    return this;
  }

  private JsonGenerator resetName() {
    name = null;

    return this;
  }

  private JsonGenerator setPending() {
    pending = true;

    return this;
  }

  @Override
  public JsonGenerator write(final JsonValue value) {
    return !pending && hasRealName()
        ? resetName().write(name, value)
        : flushPending(() -> super.write(value));
  }

  @Override
  public JsonGenerator writeEnd() {
    return name != null && pending ? reset() : super.writeEnd();
  }

  @Override
  public JsonGenerator writeKey(final String name) {
    this.name = name;

    return this;
  }

  @Override
  public JsonGenerator writeNull() {
    return !pending && hasRealName() ? resetName().writeNull(name) : flushPending(super::writeNull);
  }

  @Override
  public JsonGenerator writeStartArray() {
    if (first) {
      first = false;

      return super.writeStartArray();
    }

    if (name == null) {
      name = ANONYMOUS;
    }

    return hasRealName() ? writeStartArray(name) : setPending();
  }

  @Override
  public JsonGenerator writeStartArray(final String name) {
    this.name = name;

    return setPending();
  }

  @Override
  public JsonGenerator writeStartObject() {
    first = false;

    return !pending && hasRealName()
        ? resetName().writeStartObject(name)
        : flushPending(super::writeStartObject);
  }
}
