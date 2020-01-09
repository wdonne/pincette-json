package net.pincette.json.filter;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

/**
 * This filter flushes every write down stream, which can be interesting for debugging purposes.
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class FlushingGenerator extends JsonGeneratorFilter {
  @Override
  public JsonGenerator write(JsonValue value) {
    super.write(value);
    super.flush();

    return this;
  }

  @Override
  public JsonGenerator write(String name, JsonValue value) {
    super.write(name, value);
    super.flush();

    return this;
  }

  @Override
  public JsonGenerator writeEnd() {
    super.writeEnd();
    super.flush();

    return this;
  }

  @Override
  public JsonGenerator writeNull() {
    super.writeNull();
    super.flush();

    return this;
  }

  @Override
  public JsonGenerator writeStartArray() {
    super.writeStartArray();
    super.flush();

    return this;
  }

  @Override
  public JsonGenerator writeStartArray(String name) {
    super.writeStartArray(name);
    super.flush();

    return this;
  }

  @Override
  public JsonGenerator writeStartObject() {
    super.writeStartObject();
    super.flush();

    return this;
  }

  @Override
  public JsonGenerator writeStartObject(String name) {
    super.writeStartObject(name);
    super.flush();

    return this;
  }
}
