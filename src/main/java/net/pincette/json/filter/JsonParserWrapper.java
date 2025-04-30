package net.pincette.json.filter;

import static javax.json.stream.JsonParser.Event.START_ARRAY;
import static net.pincette.json.filter.Util.valueStream;

import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;

/**
 * This class implements the default methods in the <code>JsonParser</code> interface and delegates
 * all event operations.
 *
 * @author Werner Donné
 * @since 1.0
 */
public class JsonParserWrapper implements JsonParser {
  private final JsonParser delegate;
  private Event event;

  public JsonParserWrapper(final JsonParser delegate) {
    this.delegate = delegate;
  }

  public void close() {
    delegate.close();
  }

  @Override
  public JsonArray getArray() {
    return Util.getArray(this);
  }

  @Override
  public Stream<JsonValue> getArrayStream() {
    if (event != START_ARRAY) {
      throw new IllegalStateException(
          "In state " + event.toString() + " instead of " + START_ARRAY.toString());
    }

    return valueStream(this);
  }

  public BigDecimal getBigDecimal() {
    return delegate.getBigDecimal();
  }

  public int getInt() {
    return delegate.getInt();
  }

  public JsonLocation getLocation() {
    return delegate.getLocation();
  }

  public long getLong() {
    return delegate.getLong();
  }

  @Override
  public JsonObject getObject() {
    return Util.getObject(this);
  }

  @Override
  public Stream<Entry<String, JsonValue>> getObjectStream() {
    return getObject().entrySet().stream();
  }

  public String getString() {
    return delegate.getString();
  }

  @Override
  public JsonValue getValue() {
    return Util.getValue(event, this);
  }

  @Override
  public Stream<JsonValue> getValueStream() {
    return valueStream(this);
  }

  public boolean hasNext() {
    return delegate.hasNext();
  }

  public boolean isIntegralNumber() {
    return delegate.isIntegralNumber();
  }

  public Event next() {
    event = delegate.next();

    return event;
  }

  @Override
  public void skipArray() {
    getArray();
  }

  @Override
  public void skipObject() {
    getObject();
  }
}
