package net.pincette.json.filter;

import static net.pincette.json.JsonUtil.createArrayBuilder;
import static net.pincette.json.JsonUtil.createObjectBuilder;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

/**
 * This filter accumulates objects and arrays and writes them to the next filter element with the
 * <code>JsonValue</code> variants of the <code>write</code> method.
 *
 * @author Werner Donné
 * @since 1.0
 */
public class AccumulatingGeneratorFilter extends JsonGeneratorFilter {
  private final Deque<String> stack = new ArrayDeque<>();
  private Object builder;

  private JsonValue build() {
    return builder instanceof JsonObjectBuilder jsonObjectBuilder
        ? jsonObjectBuilder.build()
        : ((JsonArrayBuilder) builder).build();
  }

  @Override
  public JsonGenerator writeEnd() {
    final String name = stack.pop();

    if (stack.isEmpty()) {
      removeAccumulator();
      if ("".equals(name)) {
        super.write(build());
      } else {
        super.write(name, build());
      }
    } else {
      super.writeEnd();
    }

    return this;
  }

  @Override
  public JsonGenerator writeStartArray() {
    return writeStartArray("");
  }

  @Override
  public JsonGenerator writeStartArray(final String name) {
    if (stack.isEmpty()) {
      builder = createArrayBuilder();
      insertAccumulator(new JsonBuilderGenerator((JsonArrayBuilder) builder));
    } else {
      if ("".equals(name)) {
        super.writeStartArray();
      } else {
        super.writeStartArray(name);
      }
    }

    stack.push(name);

    return this;
  }

  @Override
  public JsonGenerator writeStartObject() {
    return writeStartObject("");
  }

  @Override
  public JsonGenerator writeStartObject(final String name) {
    if (stack.isEmpty()) {
      builder = createObjectBuilder();
      insertAccumulator(new JsonBuilderGenerator((JsonObjectBuilder) builder));
    } else {
      if ("".equals(name)) {
        super.writeStartObject();
      } else {
        super.writeStartObject(name);
      }
    }

    stack.push(name);

    return this;
  }
}
