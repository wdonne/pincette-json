package net.pincette.json.filter;

import javax.json.stream.JsonGenerator;

/**
 * With this filter the write sequence to the next filter element will be <code>writeStartArray()
 * </code>, a number of <code>write(JsonValue)</code> calls and finally <code>writeEnd()</code>
 *
 * @author Werner Donné
 * @since 1.0
 */
public class ArrayStreamingGeneratorFilter extends JsonGeneratorFilter {
  private boolean started;

  @Override
  public JsonGenerator writeStartArray() {
    super.writeStartArray();

    if (!started) {
      started = true;
      insertFilter(new AccumulatingGeneratorFilter());
    }

    return this;
  }
}
