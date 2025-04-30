package net.pincette.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static net.pincette.util.Util.tryToGetRethrow;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.json.JsonStructure;

/**
 * Parses a stream of unrelated JSON objects and arrays that follow one another on an input stream.
 *
 * @author Werner Donné
 * @since 1.3
 * @see net.pincette.util.StreamUtil#stream
 */
public class JsonStructureIterator implements Iterator<JsonStructure> {
  final Reader reader;
  JsonStructure next;

  public JsonStructureIterator(final InputStream in) {
    this.reader = new BufferedReader(new InputStreamReader(in, UTF_8));
  }

  private static Optional<String> readStructure(final Reader reader) {
    final StringBuilder builder = new StringBuilder(1024);
    final Deque<Integer> stack = new ArrayDeque<>();
    int c;

    while ((c = tryToGetRethrow(reader::read).orElse(-1)) != -1) {
      builder.append((char) c);

      if (c == '{' || c == '[') {
        stack.push(c);
      } else if (c == '}' || c == ']') {
        stack.pop();

        if (stack.isEmpty()) {
          return Optional.of(builder.toString());
        }
      }
    }

    return empty();
  }

  public boolean hasNext() {
    next = readStructure(reader).flatMap(JsonUtil::from).orElse(null);

    return next != null;
  }

  public JsonStructure next() {
    if (next == null) {
      throw new NoSuchElementException();
    }

    return next;
  }
}
