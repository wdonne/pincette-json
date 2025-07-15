package net.pincette.json;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static javax.json.JsonValue.NULL;
import static net.pincette.json.Jackson.from;
import static net.pincette.json.Jackson.to;
import static net.pincette.json.JsonUtil.createArrayBuilder;
import static net.pincette.util.Or.tryWith;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.thisptr.jackson.jq.Scope.newChildScope;
import static net.thisptr.jackson.jq.Scope.newEmptyScope;
import static net.thisptr.jackson.jq.Version.LATEST;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import net.pincette.util.Cases;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;
import net.thisptr.jackson.jq.module.SimpleModule;
import net.thisptr.jackson.jq.module.loaders.BuiltinModuleLoader;
import net.thisptr.jackson.jq.module.loaders.ChainedModuleLoader;

/**
 * JQ utilities.
 *
 * @author Werner Donné
 * @since 2.3.0
 */
public class Jq {
  private static final Scope rootScope = rootScope();

  private Jq() {}

  private static String read(final Reader reader) {
    return new BufferedReader(reader).lines().collect(joining("\n"));
  }

  public static Reader reader(final File file) {
    return Common.reader(file);
  }

  public static Reader reader(final InputStream in) {
    return Common.reader(in);
  }

  public static Reader readerResource(final String resource) {
    return Common.readerResource(resource);
  }

  private static JsonValue result(final List<JsonNode> result) {
    return Cases.<List<JsonNode>, JsonValue>withValue(result)
        .or(List::isEmpty, r -> NULL)
        .or(r -> r.size() == 1, r -> to(r.get(0)))
        .get()
        .orElseGet(
            () ->
                result.stream()
                    .map(Jackson::to)
                    .reduce(createArrayBuilder(), JsonArrayBuilder::add, (b1, b2) -> b1)
                    .build());
  }

  private static Scope rootScope() {
    final Scope scope = newEmptyScope();

    BuiltinFunctionLoader.getInstance().loadFunctions(LATEST, scope);
    scope.setModuleLoader(BuiltinModuleLoader.getInstance());

    return scope;
  }

  /**
   * Returns a function that transforms JSON using a JQ script.
   *
   * @param context The context of the call.
   * @return The transformer function.
   * @since 2.3.0
   */
  public static UnaryOperator<JsonObject> transformerObject(final Context context) {
    return Common.transformerObject(transformerValue(context));
  }

  /**
   * Returns a function that transforms JSON using a JQ script.
   *
   * @param context The context of the call.
   * @return The transformer function.
   * @since 2.3.0
   */
  public static UnaryOperator<JsonValue> transformerValue(final Context context) {
    final Scope scope = newChildScope(rootScope);

    if (context.extensions != null) {
      context.extensions.accept(scope);
    }

    if (context.variables != null) {
      context.variables.forEach((k, v) -> scope.setValue(k, from(v)));
    }

    if (context.moduleLoader != null) {
      scope.setModuleLoader(
          new ChainedModuleLoader(scope.getModuleLoader(), context.moduleLoader.apply(scope)));
    }

    final Expression expression =
        tryToGetRethrow(() -> ExpressionParser.compile(read(context.reader), LATEST)).orElse(null);

    return json -> {
      final List<JsonNode> result = new ArrayList<>();

      tryToDoRethrow(() -> expression.apply(scope, from(json), result::add));

      return result(result);
    };
  }

  /**
   * Returns a reader for a JQ script.
   *
   * @param jq the JQ script. If it starts with "resource:" the script is loaded as a class path
   *     resource. If it denotes an existing file, it will be loaded from that file. Otherwise, it
   *     is interpreted as a JQ script.
   * @return The transformer function.
   * @since 2.3.0
   */
  public static Reader tryReader(final String jq) {
    return Common.tryReader(jq);
  }

  /**
   * The context for generating JQ transformer functions.
   *
   * @since 2.3.0
   */
  public static class Context {
    final Consumer<Scope> extensions;
    final Function<Scope, ModuleLoader> moduleLoader;
    final Reader reader;
    final Map<String, JsonValue> variables;

    public Context(final Reader reader) {
      this(reader, null, null, null);
    }

    private Context(
        final Reader reader,
        final Consumer<Scope> extensions,
        final Map<String, JsonValue> variables,
        final Function<Scope, ModuleLoader> moduleLoader) {
      this.reader = reader;
      this.extensions = extensions;
      this.variables = variables;
      this.moduleLoader = moduleLoader;
    }

    /**
     * Allows the scope in which the functions run to be manipulated.
     *
     * @param extensions the extensions function.
     * @return The context.
     * @since 2.3.0
     */
    public Context withExtensions(final Consumer<Scope> extensions) {
      return new Context(reader, extensions, variables, moduleLoader);
    }

    /**
     * Sets the module loader, which is chained with the built-in module loader.
     *
     * @param moduleLoader the module loader.
     * @return The context.
     * @since 2.3.0
     */
    public Context withModuleLoader(final Function<Scope, ModuleLoader> moduleLoader) {
      return new Context(reader, extensions, variables, moduleLoader);
    }

    /**
     * Sets additional variables that can be addressed with <code>$name</code>.
     *
     * @param variables the variables.
     * @return The context.
     * @since 2.3.0
     */
    public Context withVariables(final Map<String, JsonValue> variables) {
      return new Context(reader, extensions, variables, moduleLoader);
    }
  }

  /**
   * Loads imported modules from a map.
   *
   * @since 2.3.0
   */
  public static class MapModuleLoader implements ModuleLoader {
    private final Map<String, Module> loaded = new HashMap<>();
    private final Map<String, String> map;
    private final Scope scope;

    private MapModuleLoader(final Scope scope, final Map<String, String> map) {
      this.scope = scope;
      this.map = map;
    }

    /**
     * Creates the module loader with a map.
     *
     * @param map the keys are relative paths and the values are JQ strings.
     */
    public static Function<Scope, ModuleLoader> mapModuleLoader(final Map<String, String> map) {
      return scope -> new MapModuleLoader(scope, map);
    }

    public JsonNode loadData(final Module caller, final String path, final JsonNode metadata) {
      return null;
    }

    public Module loadModule(final Module caller, final String path, final JsonNode metadata) {
      return tryWith(() -> loaded.get(path))
          .or(
              () ->
                  ofNullable(map.get(path))
                      .map(
                          s -> {
                            final Scope childScope = newChildScope(scope);
                            final SimpleModule module = new SimpleModule();

                            childScope.setCurrentModule(module);

                            // Copied from FileSystemModuleLoader. There should be a public API.
                            tryToDoRethrow(
                                () ->
                                    ExpressionParser.compile(s + " null", LATEST)
                                        .apply(
                                            childScope,
                                            NullNode.getInstance(),
                                            null,
                                            (o, p) -> {},
                                            false));
                            module.addAllFunctions(childScope.getLocalFunctions());
                            loaded.put(path, module);

                            return module;
                          }))
          .get()
          .orElse(null);
    }
  }
}
