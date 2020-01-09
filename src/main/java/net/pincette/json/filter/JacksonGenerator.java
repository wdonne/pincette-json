package net.pincette.json.filter;

import static javax.json.JsonValue.ValueType.ARRAY;
import static javax.json.JsonValue.ValueType.OBJECT;
import static net.pincette.json.JsonUtil.asNumber;
import static net.pincette.json.JsonUtil.asString;
import static net.pincette.util.Util.tryToDoRethrow;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonGenerator;

/**
 * A JSON generator that forwards everything to a Jackson generator.
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class JacksonGenerator implements JsonGenerator {
  private final com.fasterxml.jackson.core.JsonGenerator generator;
  private Deque<ValueType> stack = new ArrayDeque<>();

  public JacksonGenerator(final com.fasterxml.jackson.core.JsonGenerator generator) {
    this.generator = generator;
  }

  public void close() {
    tryToDoRethrow(generator::close);
  }

  public void flush() {
    tryToDoRethrow(generator::flush);
  }

  public JsonGenerator write(final String name, final JsonValue value) {
    writeKey(name);
    write(value);

    return this;
  }

  public JsonGenerator write(final String name, final String value) {
    tryToDoRethrow(() -> generator.writeStringField(name, value));

    return this;
  }

  public JsonGenerator write(final String name, final BigInteger value) {
    writeKey(name);
    write(value);

    return this;
  }

  public JsonGenerator write(final String name, final BigDecimal value) {
    tryToDoRethrow(() -> generator.writeNumberField(name, value));

    return this;
  }

  public JsonGenerator write(final String name, final int value) {
    tryToDoRethrow(() -> generator.writeNumberField(name, value));

    return this;
  }

  public JsonGenerator write(final String name, final long value) {
    tryToDoRethrow(() -> generator.writeNumberField(name, value));

    return this;
  }

  public JsonGenerator write(final String name, final double value) {
    tryToDoRethrow(() -> generator.writeNumberField(name, value));

    return this;
  }

  public JsonGenerator write(final String name, final boolean value) {
    tryToDoRethrow(() -> generator.writeBooleanField(name, value));

    return this;
  }

  public JsonGenerator write(final JsonValue value) {
    switch (value.getValueType()) {
      case ARRAY:
        return write(value.asJsonArray());
      case FALSE:
        return write(false);
      case OBJECT:
        return write(value.asJsonObject());
      case NULL:
        return writeNull();
      case NUMBER:
        return write(asNumber(value));
      case STRING:
        return write(asString(value).getString());
      case TRUE:
        return write(true);
      default:
        return this;
    }
  }

  public JsonGenerator write(final String value) {
    tryToDoRethrow(() -> generator.writeString(value));

    return this;
  }

  public JsonGenerator write(final BigDecimal value) {
    tryToDoRethrow(() -> generator.writeNumber(value));

    return this;
  }

  public JsonGenerator write(final BigInteger value) {
    tryToDoRethrow(() -> generator.writeNumber(value));

    return this;
  }

  public JsonGenerator write(final int value) {
    tryToDoRethrow(() -> generator.writeNumber(value));

    return this;
  }

  public JsonGenerator write(final long value) {
    tryToDoRethrow(() -> generator.writeNumber(value));

    return this;
  }

  public JsonGenerator write(final double value) {
    return this;
  }

  public JsonGenerator write(final boolean value) {
    tryToDoRethrow(() -> generator.writeBoolean(value));

    return this;
  }

  private JsonGenerator write(final JsonNumber value) {
    return value.isIntegral() ? write(value.bigIntegerValue()) : write(value.bigDecimalValue());
  }

  private JsonGenerator write(final JsonArray value) {
    writeStartArray();
    value.forEach(this::write);
    writeEnd();

    return this;
  }

  private JsonGenerator write(final JsonObject value) {
    writeStartObject();
    value.forEach(this::write);
    writeEnd();

    return this;
  }

  public JsonGenerator writeEnd() {
    switch (stack.pop()) {
      case ARRAY:
        tryToDoRethrow(generator::writeEndArray);
        return this;
      case OBJECT:
        tryToDoRethrow(generator::writeEndObject);
        return this;
      default:
        return this;
    }
  }

  public JsonGenerator writeKey(final String name) {
    tryToDoRethrow(() -> generator.writeFieldName(name));

    return this;
  }

  public JsonGenerator writeNull(final String name) {
    tryToDoRethrow(() -> generator.writeNullField(name));

    return this;
  }

  public JsonGenerator writeNull() {
    tryToDoRethrow(generator::writeNull);

    return this;
  }

  public JsonGenerator writeStartArray() {
    tryToDoRethrow(generator::writeStartArray);
    stack.push(ARRAY);

    return this;
  }

  public JsonGenerator writeStartArray(final String name) {
    writeKey(name);
    writeStartArray();

    return this;
  }

  public JsonGenerator writeStartObject() {
    tryToDoRethrow(generator::writeStartObject);
    stack.push(OBJECT);

    return this;
  }

  public JsonGenerator writeStartObject(final String name) {
    writeKey(name);
    writeStartObject();

    return this;
  }
}
