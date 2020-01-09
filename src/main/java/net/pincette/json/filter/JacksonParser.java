package net.pincette.json.filter;

import static com.fasterxml.jackson.core.JsonToken.NOT_AVAILABLE;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;

import com.fasterxml.jackson.core.JsonToken;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;

/**
 * A JSON parser that gets everything from a Jackson parser.
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class JacksonParser implements JsonParser {
  private final com.fasterxml.jackson.core.JsonParser parser;
  private Event event;
  private JsonToken token;

  public JacksonParser(final com.fasterxml.jackson.core.JsonParser parser) {
    this.parser = parser;
  }

  public void close() {
    tryToDoRethrow(parser::close);
  }

  public BigDecimal getBigDecimal() {
    return tryToGetRethrow(parser::getDecimalValue).orElse(null);
  }

  public int getInt() {
    return tryToGetRethrow(parser::getIntValue).orElse(-1);
  }

  public JsonLocation getLocation() {
    return null;
  }

  public long getLong() {
    return tryToGetRethrow(parser::getLongValue).orElse(-1L);
  }

  public String getString() {
    return tryToGetRethrow(parser::getText).orElse(null);
  }

  public boolean hasNext() {
    return event != null || (event = nextEvent()) != null;
  }

  public boolean isIntegralNumber() {
    return event != null && token == VALUE_NUMBER_INT;
  }

  public Event next() {
    final Event result = event != null ? event : nextEvent();

    event = null;

    return result;
  }

  private Event nextEvent() {
    token = tryToGetRethrow(parser::nextToken).orElse(NOT_AVAILABLE);

    switch (token) {
      case END_ARRAY:
        return Event.END_ARRAY;
      case END_OBJECT:
        return Event.END_OBJECT;
      case FIELD_NAME:
        return Event.KEY_NAME;
      case NOT_AVAILABLE:
        return null;
      case VALUE_NULL:
        return Event.VALUE_NULL;
      case VALUE_TRUE:
        return Event.VALUE_TRUE;
      case START_ARRAY:
        return Event.START_ARRAY;
      case VALUE_FALSE:
        return Event.VALUE_FALSE;
      case START_OBJECT:
        return Event.START_OBJECT;
      case VALUE_STRING:
        return Event.VALUE_STRING;
      case VALUE_NUMBER_INT:
      case VALUE_NUMBER_FLOAT:
        return Event.VALUE_NUMBER;
      default:
        throw new NoSuchElementException();
    }
  }
}
