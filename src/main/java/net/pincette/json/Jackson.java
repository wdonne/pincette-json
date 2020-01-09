package net.pincette.json;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createValue;
import static javax.json.JsonValue.FALSE;
import static javax.json.JsonValue.NULL;
import static javax.json.JsonValue.TRUE;
import static net.pincette.json.JsonUtil.asNumber;
import static net.pincette.json.JsonUtil.asString;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * Conversion functions between <code>javax.json.JsonValue</code> and <code>com.fasterxml.jackson
 * .databind.JsonNode</code>.
 *
 * @author Werner Donn\u00e9
 * @since 1.0
 */
public class Jackson {
  private Jackson() {}

  public static JsonNode from(final JsonValue json) {
    switch (json.getValueType()) {
      case ARRAY:
        return from(json.asJsonArray());
      case FALSE:
        return instance.booleanNode(false);
      case NULL:
        return instance.nullNode();
      case NUMBER:
        return instance.numberNode(asNumber(json).bigDecimalValue());
      case OBJECT:
        return from(json.asJsonObject());
      case STRING:
        return instance.textNode(asString(json).getString());
      case TRUE:
        return instance.booleanNode(true);
      default:
        return null;
    }
  }

  public static ObjectNode from(final JsonObject json) {
    return json.entrySet().stream()
        .map(e -> pair(e.getKey(), from(e.getValue())))
        .filter(pair -> pair.second != null)
        .reduce(
            instance.objectNode(),
            (node, pair) -> node.set(pair.first, pair.second),
            (n1, n2) -> n1);
  }

  public static ArrayNode from(final JsonArray json) {
    return json.stream()
        .map(Jackson::from)
        .filter(Objects::nonNull)
        .reduce(instance.arrayNode(), ArrayNode::add, (n1, n2) -> n1);
  }

  public static JsonValue to(final JsonNode json) {
    switch (json.getNodeType()) {
      case ARRAY:
        return to((ArrayNode) json);
      case BOOLEAN:
        return json.booleanValue() ? TRUE : FALSE;
      case NULL:
        return NULL;
      case NUMBER:
        return createValue(json.decimalValue());
      case OBJECT:
        return to((ObjectNode) json);
      case STRING:
        return createValue(json.textValue());
      default:
        return null;
    }
  }

  public static JsonObject to(final ObjectNode json) {
    return stream(json.fields())
        .map(e -> pair(e.getKey(), to(e.getValue())))
        .filter(pair -> pair.second != null)
        .reduce(
            createObjectBuilder(),
            (builder, pair) -> builder.add(pair.first, pair.second),
            (b1, b2) -> b1)
        .build();
  }

  public static JsonArray to(final ArrayNode json) {
    return stream(json.elements())
        .map(Jackson::to)
        .filter(Objects::nonNull)
        .reduce(createArrayBuilder(), JsonArrayBuilder::add, (b1, b2) -> b1)
        .build();
  }
}
