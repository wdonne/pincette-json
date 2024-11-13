package net.pincette.json;

import static net.pincette.json.Factory.a;
import static net.pincette.json.Factory.f;
import static net.pincette.json.Factory.o;
import static net.pincette.json.Factory.v;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestJsonUtil {
  @Test
  @DisplayName("merge")
  void merge() {
    assertEquals(
        o(
            f("test1", v("test1")),
            f("test2", v("test2")),
            f("test3", v(0)),
            f("test4", a(v(1))),
            f("test5", v(true)),
            f("test6", o())),
        JsonUtil.merge(
            o(f("test1", v("test1")), f("test2", v("test1"))),
            o(f("test2", v("test2")), f("test3", v(0))),
            o(f("test4", a(v(1))), f("test5", v(true)), f("test6", o()))));
  }
}
