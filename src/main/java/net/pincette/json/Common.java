package net.pincette.json;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.pincette.json.JsonUtil.objectValue;
import static net.pincette.util.Util.tryToGetRethrow;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import javax.json.JsonObject;
import javax.json.JsonValue;

class Common {
  private static final String RESOURCE = "resource:";

  private Common() {}

  static Reader reader(final File file) {
    return tryToGetRethrow(() -> reader(new FileInputStream(file))).orElse(null);
  }

  static Reader reader(final InputStream in) {
    return new InputStreamReader(in, UTF_8);
  }

  static Reader readerResource(final String resource) {
    return reader(Jslt.class.getResourceAsStream(resource));
  }

  static UnaryOperator<JsonObject> transformerObject(final UnaryOperator<JsonValue> transformer) {
    return json -> objectValue(transformer.apply(json)).orElse(null);
  }

  static Reader tryReader(final String script) {
    final Supplier<Reader> tryFile =
        () -> new File(script).exists() ? reader(new File(script)) : new StringReader(script);

    return script.startsWith(RESOURCE)
        ? readerResource(script.substring(RESOURCE.length()))
        : tryFile.get();
  }
}
