package net.pincette.json.filter;

import java.util.Optional;
import javax.json.JsonException;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

/**
 * A filter for <code>JsonGenerators</code>. You can use it as follows:
 *
 * <p>{@code new JsonGeneratorFilter() .thenApply(new JsonGeneratorFilter()) ... thenApply(new
 * JsonGenerator())}
 *
 * <p>The last one may be a plain <code>JsonGenerator</code>. The result of the entire expression is
 * the first filter.
 *
 * <p>The value writers in this class call the variants with the <code>JsonValue</code> type.
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class JsonGeneratorFilter extends JsonValueGenerator implements JsonGenerator {
  private JsonGenerator next;
  private JsonGenerator saved;

  public void close() {
    Optional.ofNullable(next).ifPresent(JsonGenerator::close);
  }

  public void flush() {
    Optional.ofNullable(next).ifPresent(JsonGenerator::flush);
  }

  /**
   * Causes all writes to go to <code>accumulator</code> instead of the next element in the filter
   * chain.
   *
   * @param accumulator the given accumulator.
   * @since 1.0
   */
  protected void insertAccumulator(final JsonGenerator accumulator) {
    saved = next;
    next = accumulator;
  }

  /**
   * A filter element may insert filters of its own, which causes all writes to first through them
   * before going to the original next filter element.
   *
   * @param filter the filter that will be inserted.
   * @return This filter element.
   * @since 1.0
   */
  protected JsonGeneratorFilter insertFilter(final JsonGeneratorFilter filter) {
    filter.next = this.next;
    this.next = filter;

    return this;
  }

  /**
   * Stops all writes to go to the inserted accumulator. This will throw an exception if no
   * accumulator was inserted.
   *
   * @since 1.0
   */
  protected void removeAccumulator() {
    if (saved == null) {
      throw new JsonException("No accumulator was inserted");
    }

    next = saved;
    saved = null;
  }

  /**
   * Appends a generator to a filter chain.
   *
   * @param next the next filter element or generator.
   * @return The filter chain.
   * @since 1.0
   */
  public JsonGeneratorFilter thenApply(final JsonGenerator next) {
    if (this.next == null) {
      this.next = next;
    } else {
      if (this.next instanceof JsonGeneratorFilter) {
        ((JsonGeneratorFilter) this.next).thenApply(next);
      } else {
        throw new JsonException("Unsupported operation");
      }
    }

    return this;
  }

  public JsonGenerator write(final JsonValue value) {
    if (next != null) {
      next.write(value);
    }

    return this;
  }

  public JsonGenerator write(final String name, final JsonValue value) {
    if (next != null) {
      next.write(name, value);
    }

    return this;
  }

  public JsonGenerator writeEnd() {
    if (next != null) {
      next.writeEnd();
    }

    return this;
  }

  public JsonGenerator writeKey(final String name) {
    if (next != null) {
      next.writeKey(name);
    }

    return this;
  }

  public JsonGenerator writeNull() {
    if (next != null) {
      next.writeNull();
    }

    return this;
  }

  public JsonGenerator writeNull(final String name) {
    if (next != null) {
      next.writeNull(name);
    }

    return this;
  }

  public JsonGenerator writeStartArray() {
    if (next != null) {
      next.writeStartArray();
    }

    return this;
  }

  public JsonGenerator writeStartArray(final String name) {
    if (next != null) {
      next.writeStartArray(name);
    }

    return this;
  }

  public JsonGenerator writeStartObject() {
    if (next != null) {
      next.writeStartObject();
    }

    return this;
  }

  public JsonGenerator writeStartObject(final String name) {
    if (next != null) {
      next.writeStartObject(name);
    }

    return this;
  }
}
