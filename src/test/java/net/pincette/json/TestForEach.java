package net.pincette.json;

import static javax.json.JsonValue.NULL;
import static net.pincette.json.Factory.a;
import static net.pincette.json.Factory.f;
import static net.pincette.json.Factory.o;
import static net.pincette.json.Factory.v;
import static net.pincette.json.JsonUtil.createValue;
import static net.pincette.json.JsonUtil.intValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestForEach {
  @Test
  @DisplayName("forEach1")
  void forEach1() {
    assertEquals(
        o(
            f("f1", v(2)),
            f("f2", o(f("f1", v(2)), f("f2", o(f("f", v(2)))))),
            f("f3", a(v(2), o(f("f1", v(2)), f("f2", a(v(2)))), a(o(f("f", v(2))), v(2))))),
        ForEach.forEach(
            o(
                f("f1", v(1)),
                f("f2", o(f("f1", v(1)), f("f2", o(f("f", v(1)))), f("f3", NULL))),
                f("f3", a(v(1), o(f("f1", v(1)), f("f2", a(v(1)))), a(o(f("f", v(1))), v(1))))),
            location ->
                intValue(location.value).map(v -> createValue(v + 1)).orElse(location.value)));
  }

  @Test
  @DisplayName("forEach2")
  void forEach2() {
    assertEquals(
        o(
            f("f1", v(2)),
            f("f2", o(f("f1", v(2)), f("f2", o(f("f", v(2)))), f("f3", NULL))),
            f("f3", a(v(2), o(f("f1", v(2)), f("f2", a(v(2)))), a(o(f("f", v(2))), v(2))))),
        ForEach.forEach(
            o(
                f("f1", v(1)),
                f("f2", o(f("f1", v(1)), f("f2", o(f("f", v(1)))), f("f3", NULL))),
                f("f3", a(v(1), o(f("f1", v(1)), f("f2", a(v(1)))), a(o(f("f", v(1))), v(1))))),
            location ->
                intValue(location.value).map(v -> createValue(v + 1)).orElse(location.value),
            true));
  }
}
