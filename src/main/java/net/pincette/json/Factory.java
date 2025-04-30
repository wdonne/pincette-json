package net.pincette.json;

import static java.util.Arrays.stream;
import static net.pincette.json.JsonUtil.createArrayBuilder;
import static net.pincette.json.JsonUtil.createObjectBuilder;
import static net.pincette.json.JsonUtil.createValue;
import static net.pincette.util.Pair.pair;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import net.pincette.util.Pair;

/**
 * A class to create fixed JSON structures in a very compact way.
 *
 * @author Werner Donné
 * @since 1.2
 */
public class Factory {
  private Factory() {}

  public static JsonArray a(final JsonValue... v) {
    return stream(v).reduce(createArrayBuilder(), JsonArrayBuilder::add, (b1, b2) -> b1).build();
  }

  public static Pair<String, JsonValue> f(final String name, final JsonValue value) {
    return pair(name, value);
  }

  @SafeVarargs
  public static JsonObject o(final Pair<String, JsonValue>... f) {
    return stream(f)
        .reduce(createObjectBuilder(), (b, p) -> b.add(p.first, p.second), (b1, b2) -> b1)
        .build();
  }

  public static JsonValue v(final Object v) {
    return createValue(v);
  }
}
