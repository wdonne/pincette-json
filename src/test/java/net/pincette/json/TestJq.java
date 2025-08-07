package net.pincette.json;

import static net.pincette.json.Factory.f;
import static net.pincette.json.Factory.o;
import static net.pincette.json.Factory.v;
import static net.pincette.json.Jq.MapModuleLoader.mapModuleLoader;
import static net.pincette.json.Jq.transformerObject;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Pair.pair;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import javax.json.JsonObject;
import net.pincette.json.Jq.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestJq {
  @Test
  @DisplayName("jq1")
  void jq1() {
    final JsonObject o = o(f("test", v("test")));

    assertEquals(o, transformerObject(new Context(new StringReader("."))).apply(o));
  }

  @Test
  @DisplayName("jq2")
  void jq2() {
    final JsonObject o = o(f("test", v("test")));

    assertEquals(
        o,
        transformerObject(
                new Context(new StringReader("{test: $test}"))
                    .withVariables(map(pair("test", v("test")))))
            .apply(o));
  }

  @Test
  @DisplayName("jq3")
  void jq3() {
    var t =
        transformerObject(
            new Context(new StringReader("import \"test.jq\" as test; {test: .test|test::inc}"))
                .withModuleLoader(mapModuleLoader(map(pair("test.jq", "def inc: . + 1;")))));

    assertEquals(o(f("test", v(1))), t.apply(o(f("test", v(0)))));
    assertEquals(o(f("test", v(1))), t.apply(o(f("test", v(0)))));
  }
}
