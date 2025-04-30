package net.pincette.json.filter;

import static javax.json.JsonValue.FALSE;
import static javax.json.JsonValue.TRUE;
import static net.pincette.json.JsonUtil.createValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

/**
 * The <code>write</code> methods call <code>JsonValue</code> variants for scalar values, so only
 * those have to be overridden.
 *
 * @author Werner Donné
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
    return write(createValue(value));
  }

  public JsonGenerator write(BigDecimal value) {
    return write(createValue(value));
  }

  public JsonGenerator write(BigInteger value) {
    return write(createValue(value));
  }

  public JsonGenerator write(int value) {
    return write(createValue(value));
  }

  public JsonGenerator write(long value) {
    return write(createValue(value));
  }

  public JsonGenerator write(double value) {
    return write(createValue(value));
  }

  public JsonGenerator write(boolean value) {
    return write(value ? TRUE : FALSE);
  }

  public JsonGenerator write(String name, JsonValue value) {
    return this;
  }

  public JsonGenerator write(String name, String value) {
    return write(name, createValue(value));
  }

  public JsonGenerator write(String name, BigInteger value) {
    return write(name, createValue(value));
  }

  public JsonGenerator write(String name, BigDecimal value) {
    return write(name, createValue(value));
  }

  public JsonGenerator write(String name, int value) {
    return write(name, createValue(value));
  }

  public JsonGenerator write(String name, long value) {
    return write(name, createValue(value));
  }

  public JsonGenerator write(String name, double value) {
    return write(name, createValue(value));
  }

  public JsonGenerator write(String name, boolean value) {
    return write(name, value ? TRUE : FALSE);
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
