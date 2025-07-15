package net.pincette.json;

import static net.pincette.json.Factory.f;
import static net.pincette.json.Factory.o;
import static net.pincette.json.Factory.v;
import static net.pincette.json.Jslt.transformerObject;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Pair.pair;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import javax.json.JsonObject;
import net.pincette.json.Jslt.Context;
import net.pincette.json.Jslt.MapResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestJslt {
  @Test
  @DisplayName("jslt1")
  void jslt1() {
    final JsonObject o = o(f("test", v("test")));

    assertEquals(o, transformerObject(new Context(new StringReader("{ * : . }"))).apply(o));
  }

  @Test
  @DisplayName("jslt2")
  void jslt2() {
    final JsonObject o = o(f("test", v("test")));

    assertEquals(
        o,
        transformerObject(
                new Context(new StringReader("{ \"test\" : $test }"))
                    .withVariables(map(pair("test", v("test")))))
            .apply(o));
  }

  @Test
  @DisplayName("jslt3")
  void jslt3() {
    final JsonObject o = o(f("test", v("test")));

    assertEquals(
        o,
        transformerObject(
                new Context(
                        new StringReader(
                            "import \"test.jslt\" as test\n{ \"test\" : test:test() }"))
                    .withResolver(new MapResolver(map(pair("test.jslt", "def test() \"test\"")))))
            .apply(o));
  }
}
