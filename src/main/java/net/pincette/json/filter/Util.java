package net.pincette.json.filter;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static javax.json.JsonValue.FALSE;
import static javax.json.JsonValue.NULL;
import static javax.json.JsonValue.TRUE;
import static javax.json.stream.JsonParser.Event.END_ARRAY;
import static javax.json.stream.JsonParser.Event.END_OBJECT;
import static javax.json.stream.JsonParser.Event.START_ARRAY;
import static javax.json.stream.JsonParser.Event.START_OBJECT;
import static net.pincette.json.JsonUtil.createValue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import net.pincette.json.JsonUtil;

/**
 * Some JSON streaming and filter utilities.
 *
 * @author Werner Donn\u00e9
 */
public class Util {
  private Util() {}

  /**
   * Adds all events from <code>parser</code> to <code>generator</code>.
   *
   * @param parser the given parser.
   * @param generator the given generator.
   * @return The given generator.
   */
  public static JsonGenerator add(final JsonParser parser, final JsonGenerator generator) {
    while (parser.hasNext()) {
      writeEvent(parser.next(), parser, generator);
    }

    return generator;
  }

  public static JsonGenerator addArray(final JsonParser parser, final JsonGenerator generator) {
    return addArray(parser, generator, null);
  }

  /**
   * Reads an array from <code>parser</code> and adds its events to <code>generator</code>. The
   * parser must be in the state <code>START_ARRAY</code>.
   *
   * @param parser the given parser.
   * @param generator the given generator.
   * @param name the name of the array. It may be <code>null</code>.
   * @return The given generator.
   */
  public static JsonGenerator addArray(
      final JsonParser parser, final JsonGenerator generator, final String name) {
    return addStructure(
        parser,
        generator,
        name,
        START_ARRAY,
        END_ARRAY,
        generator::writeStartArray,
        generator::writeStartArray);
  }

  public static JsonGenerator addObject(final JsonParser parser, final JsonGenerator generator) {
    return addObject(parser, generator, null);
  }

  /**
   * Reads an object from <code>parser</code> and adds its events to <code>generator</code>. The
   * parser must be in the state <code>START_OBJECT</code>.
   *
   * @param parser the given parser.
   * @param generator the given generator.
   * @param name the name of the object. It may be <code>null</code>.
   * @return The given generator.
   */
  public static JsonGenerator addObject(
      final JsonParser parser, final JsonGenerator generator, final String name) {
    return addStructure(
        parser,
        generator,
        name,
        START_OBJECT,
        END_OBJECT,
        generator::writeStartObject,
        generator::writeStartObject);
  }

  private static JsonGenerator addStructure(
      final JsonParser parser,
      final JsonGenerator generator,
      final String name,
      final Event startEvent,
      final Event endEvent,
      final Runnable start,
      final Consumer<String> startName) {
    final Deque<Event> stack = new ArrayDeque<>();

    stack.push(startEvent);

    if (name != null) {
      startName.accept(name);
    } else {
      start.run();
    }

    while (parser.hasNext()) {
      final Event e = parser.next();

      writeEvent(e, parser, generator);

      if (e.equals(startEvent)) {
        stack.push(e);
      } else if (e.equals(endEvent)) {
        stack.pop();

        if (stack.isEmpty()) {
          return generator;
        }
      }
    }

    throw new IllegalStateException("Expecting end of object or array");
  }

  /**
   * Reads one array from <code>parser</code>, which must be in the state <code>START_ARRAY</code>.
   *
   * @param parser the given parser.
   * @return The read array.
   */
  public static JsonArray getArray(final JsonParser parser) {
    return Optional.of(new JsonBuilderGenerator())
        .map(generator -> addArray(parser, generator))
        .map(generator -> ((JsonBuilderGenerator) generator).build())
        .filter(JsonUtil::isArray)
        .map(JsonValue::asJsonArray)
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Reads one object from <code>parser</code>, which must be in the state <code>START_OBJECT</code>
   * .
   *
   * @param parser the given parser.
   * @return The read object.
   */
  public static JsonObject getObject(final JsonParser parser) {
    return Optional.of(new JsonBuilderGenerator())
        .map(generator -> addObject(parser, generator))
        .map(generator -> ((JsonBuilderGenerator) generator).build())
        .filter(JsonUtil::isObject)
        .map(JsonValue::asJsonObject)
        .orElseThrow(IllegalStateException::new);
  }

  public static JsonValue getValue(final Event e, final JsonParser parser) {
    switch (e) {
      case VALUE_NULL:
        return NULL;
      case VALUE_STRING:
        return createValue(parser.getString());
      case START_ARRAY:
        return getArray(parser);
      case START_OBJECT:
        return getObject(parser);
      case VALUE_TRUE:
        return TRUE;
      case VALUE_FALSE:
        return FALSE;
      case VALUE_NUMBER:
        return createValue(parser.getBigDecimal());
      default:
        return null;
    }
  }

  /**
   * Produces a stream from the <code>parser</code>. If the parser offers an object then the stream
   * consists of one element. If it offers an array the stream consists of the elements in the
   * array. The stream will keep only one element in memory at the time.
   *
   * @param parser the given parser.
   * @return The stream of values.
   */
  public static Stream<JsonValue> stream(final JsonParser parser) {
    final Function<Event, Stream<JsonValue>> contents =
        event -> event == START_OBJECT ? of(getObject(parser)) : valueStream(parser);

    return !parser.hasNext() ? empty() : contents.apply(parser.next());
  }

  /**
   * Returns a stream of values in an array. The state must be such that a value will start after
   * it.
   *
   * @param parser the given parser.
   * @return The value stream.
   */
  public static Stream<JsonValue> valueStream(final JsonParser parser) {
    return net.pincette.util.StreamUtil.stream(
        new Iterator<JsonValue>() {
          Event nextEvent;

          private Event getNext() {
            return nextEvent != null && nextEvent != END_ARRAY ? nextEvent : null;
          }

          @Override
          public boolean hasNext() {
            nextEvent = parser.hasNext() ? parser.next() : null;

            return getNext() != null;
          }

          @Override
          public JsonValue next() {
            return Optional.ofNullable(getNext())
                .map(e -> getValue(e, parser))
                .orElseThrow(NoSuchElementException::new);
          }
        });
  }

  /**
   * Write one event from <code>parser</code> to <code>generator</code>.
   *
   * @param e the given event.
   * @param parser the given parser.
   * @param generator the given generator.
   * @return The given generator.
   */
  public static JsonGenerator writeEvent(
      final Event e, final JsonParser parser, final JsonGenerator generator) {
    switch (e) {
      case END_ARRAY:
      case END_OBJECT:
        generator.writeEnd();
        break;
      case KEY_NAME:
        generator.writeKey(parser.getString());
        break;
      case START_ARRAY:
        generator.writeStartArray();
        break;
      case START_OBJECT:
        generator.writeStartObject();
        break;
      case VALUE_FALSE:
        generator.write(false);
        break;
      case VALUE_NULL:
        generator.writeNull();
        break;
      case VALUE_NUMBER:
        generator.write(parser.getBigDecimal());
        break;
      case VALUE_STRING:
        generator.write(parser.getString());
        break;
      case VALUE_TRUE:
        generator.write(true);
        break;
    }

    return generator;
  }
}
