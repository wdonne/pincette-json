package net.pincette.json.filter;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

/**
 * Removes empty objects from the stream unless it is the top object.
 *
 * @author Werner Donné
 * @since 1.0
 */
public class RemoveEmptyObjectsGenerator extends JsonGeneratorFilter {
  private static final String ANONYMOUS = "anonymous";

  private boolean first = true;
  private String name;
  private boolean pending = false;

  private void flushPending() {
    if (name != null) {
      if (name.equals(ANONYMOUS)) {
        super.writeStartObject();
      } else {
        super.writeStartObject(name);
      }

      name = null;
      pending = false;
    }
  }

  private boolean hasRealName() {
    return name != null && !name.equals(ANONYMOUS);
  }

  private JsonGenerator reset() {
    name = null;
    pending = false;

    return this;
  }

  private JsonGenerator setPending() {
    pending = true;

    return this;
  }

  @Override
  public JsonGenerator write(JsonValue value) {
    return !pending && hasRealName() ? write(name, value) : super.write(value);
  }

  @Override
  public JsonGenerator write(final String name, final JsonValue value) {
    flushPending();

    return super.write(name, value);
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
    return !pending && hasRealName() ? writeNull(name) : super.writeNull();
  }

  @Override
  public JsonGenerator writeNull(final String name) {
    flushPending();

    return super.writeNull(name);
  }

  @Override
  public JsonGenerator writeStartArray() {
    first = false;

    return !pending && hasRealName() ? writeStartArray(name) : super.writeStartArray();
  }

  @Override
  public JsonGenerator writeStartArray(final String name) {
    flushPending();

    return super.writeStartArray(name);
  }

  @Override
  public JsonGenerator writeStartObject() {
    if (first) {
      first = false;

      return super.writeStartObject();
    }

    if (name == null) {
      name = ANONYMOUS;
    }

    return hasRealName() ? writeStartObject(name) : setPending();
  }

  @Override
  public JsonGenerator writeStartObject(final String name) {
    flushPending();
    this.name = name;

    return setPending();
  }
}
