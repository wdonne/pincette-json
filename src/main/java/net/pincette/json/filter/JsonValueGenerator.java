package net.pincette.json.filter;

import static javax.json.Json.createValue;
import static javax.json.JsonValue.FALSE;
import static javax.json.JsonValue.TRUE;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

/**
 * The <code>write</code> methods call <code>JsonValue</code> variants for scalar values, so only
 * those have to be overridden.
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class JsonValueGenerator implements JsonGenerator {
  public void close() {
    // Nothing to do.
  }

  public void flush() {
    // Nothing to do.
  }

  public JsonGenerator write(JsonValue value) {
    return this;
  }

  public JsonGenerator write(String value) {
    write(createValue(value));

    return this;
  }

  public JsonGenerator write(BigDecimal value) {
    write(createValue(value));

    return this;
  }

  public JsonGenerator write(BigInteger value) {
    write(createValue(value));

    return this;
  }

  public JsonGenerator write(int value) {
    write(createValue(value));

    return this;
  }

  public JsonGenerator write(long value) {
    write(createValue(value));

    return this;
  }

  public JsonGenerator write(double value) {
    write(createValue(value));

    return this;
  }

  public JsonGenerator write(boolean value) {
    write(value ? TRUE : FALSE);

    return this;
  }

  public JsonGenerator write(String name, JsonValue value) {
    return this;
  }

  public JsonGenerator write(String name, String value) {
    write(name, createValue(value));

    return this;
  }

  public JsonGenerator write(String name, BigInteger value) {
    write(name, createValue(value));

    return this;
  }

  public JsonGenerator write(String name, BigDecimal value) {
    write(name, createValue(value));

    return this;
  }

  public JsonGenerator write(String name, int value) {
    write(name, createValue(value));

    return this;
  }

  public JsonGenerator write(String name, long value) {
    write(name, createValue(value));

    return this;
  }

  public JsonGenerator write(String name, double value) {
    write(name, createValue(value));

    return this;
  }

  public JsonGenerator write(String name, boolean value) {
    write(name, value ? TRUE : FALSE);

    return this;
  }

  public JsonGenerator writeEnd() {
    return this;
  }

  public JsonGenerator writeKey(String name) {
    return this;
  }

  public JsonGenerator writeNull() {
    return this;
  }

  public JsonGenerator writeNull(String name) {
    return this;
  }

  public JsonGenerator writeStartArray() {
    return this;
  }

  public JsonGenerator writeStartArray(String name) {
    return this;
  }

  public JsonGenerator writeStartObject() {
    return this;
  }

  public JsonGenerator writeStartObject(String name) {
    return this;
  }
}
