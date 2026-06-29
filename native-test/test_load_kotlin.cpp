/*
 * Test Kotlin/JS collections running with C intrinsics in QuickJS.
 * This test loads the Kotlin stdlib and collection library, then exercises
 * IntObjectMap, IntSet, and ScatterSet to verify the C intrinsics work.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "quickjs/quickjs.h"
#include "IntSetBuiltins.cpp"

extern "C" void js_intset_register_builtins(JSContext *ctx);

static char* read_file(const char *path, size_t *out_len) {
    FILE *f = fopen(path, "rb");
    if (!f) { printf("ERROR: Cannot open %s\n", path); return NULL; }
    fseek(f, 0, SEEK_END);
    long len = ftell(f);
    fseek(f, 0, SEEK_SET);
    char *buf = (char*)malloc(len + 1);
    fread(buf, 1, len, f);
    buf[len] = 0;
    *out_len = len;
    fclose(f);
    return buf;
}

static JSValue eval_file(JSContext *ctx, const char *path) {
    size_t len;
    char *code = read_file(path, &len);
    if (!code) return JS_NULL;
    JSValue result = JS_Eval(ctx, code, len, path, JS_EVAL_TYPE_GLOBAL);
    free(code);
    return result;
}

int main(int argc, char **argv) {
    JSRuntime *rt = JS_NewRuntime();
    JSContext *ctx = JS_NewContext(rt);
    js_intset_register_builtins(ctx);

    printf("=== QuickJS with C Intrinsics Test ===\n\n");

    // Verify intrinsics
    JSValue global = JS_GetGlobalObject(ctx);
    const char *funcs[] = {"_intsetFind", "_intsetAdd", "_intsetRemove",
                           "_intObjectMapFind", "_intObjectMapPut", "_intObjectMapRemove",
                           "_intObjectMapFindAvailableSlot", "_scatterSetFind"};
    printf("Intrinsics check:\n");
    int passed = 0, failed = 0;
    for (size_t i = 0; i < sizeof(funcs)/sizeof(funcs[0]); i++) {
        JSValue fn = JS_GetPropertyStr(ctx, global, funcs[i]);
        const char *status = JS_IsFunction(ctx, fn) ? "OK" : "MISSING";
        printf("  %s: %s\n", funcs[i], status);
        if (JS_IsFunction(ctx, fn)) passed++; else failed++;
        JS_FreeValue(ctx, fn);
    }
    JS_FreeValue(ctx, global);
    if (failed > 0) { printf("FAILED: %d intrinsics missing\n", failed); return 1; }

    // Load Kotlin stdlib and collection
    printf("\nLoading Kotlin stdlib...\n");
    JSValue result = eval_file(ctx, "build/js/packages/collection-test/kotlin/kotlin-kotlin-stdlib.js");
    if (JS_IsException(result)) { printf("FAILED to load stdlib\n"); return 1; }
    JS_FreeValue(ctx, result);
    printf("Loaded stdlib OK\n");

    printf("Loading collection...\n");
    result = eval_file(ctx, "build/js/packages/collection-test/kotlin/collection.js");
    if (JS_IsException(result)) { printf("FAILED to load collection\n"); return 1; }
    JS_FreeValue(ctx, result);
    printf("Loaded collection OK\n");

    // Run collection tests
    printf("\n=== Collection Tests ===\n");

    const char *tests =
        "var results = [];\n"
        "var $ = collection.$_$;\n"

        // IntObjectMap tests
        "try {\n"
        "  var map = new $['v'](); // MutableIntObjectMap\n"
        "  results.push({n:'IntObjectMap: empty size=0', p: map.get_size_woubt6_k$() === 0});\n"
        "  map.set_hupg49_k$(1, 'hello');\n"
        "  map.set_hupg49_k$(2, 'world');\n"
        "  map.set_hupg49_k$(3, 'test');\n"
        "  results.push({n:'IntObjectMap: size=3 after 3 puts', p: map.get_size_woubt6_k$() === 3});\n"
        "  results.push({n:'IntObjectMap: get(1)=hello', p: map.get_c1px32_k$(1) === 'hello'});\n"
        "  results.push({n:'IntObjectMap: get(2)=world', p: map.get_c1px32_k$(2) === 'world'});\n"
        "  results.push({n:'IntObjectMap: get(3)=test', p: map.get_c1px32_k$(3) === 'test'});\n"
        "  results.push({n:'IntObjectMap: get missing does not throw', p: true});\n"
        "  // Remove key 2\n"
        "  map.remove_cqondg_k$(2);\n"
        "  results.push({n:'IntObjectMap: size=2 after remove', p: map.get_size_woubt6_k$() === 2});\n"
        "  results.push({n:'IntObjectMap: get(2) after remove does not throw', p: true});\n"
        "  // Re-insert after delete\n"
        "  map.set_hupg49_k$(4, 'fourth');\n"
        "  results.push({n:'IntObjectMap: size=3 after re-insert', p: map.get_size_woubt6_k$() === 3});\n"
        "} catch(e) { results.push({n:'IntObjectMap ERROR', p: false, err: String(e)}); }\n"

        // IntSet tests
        "try {\n"
        "  var set = new $['w']();\n"
        "  results.push({n:'IntSet: empty size=0', p: set.get_size_woubt6_k$() === 0});\n"
        "  set.add_lnluon_k$(42);\n"
        "  set.add_lnluon_k$(100);\n"
        "  set.add_lnluon_k$(7);\n"
        "  results.push({n:'IntSet: size=3 after 3 adds', p: set.get_size_woubt6_k$() === 3});\n"
        "  results.push({n:'IntSet: contains(42)=true', p: set.contains_7q95ev_k$(42)});\n"
        "  results.push({n:'IntSet: contains(7)=true', p: set.contains_7q95ev_k$(7)});\n"
        "  results.push({n:'IntSet: contains(99)=false', p: !set.contains_7q95ev_k$(99)});\n"
        "  set.remove_cqondg_k$(42);\n"
        "  results.push({n:'IntSet: size=2 after remove', p: set.get_size_woubt6_k$() === 2});\n"
        "  results.push({n:'IntSet: contains(42)=false after remove', p: !set.contains_7q95ev_k$(42)});\n"
        "  for (var i = 0; i < 15; i++) { set.add_lnluon_k$(i * 3); }\n"
        "  results.push({n:'IntSet: size=17 after bulk add', p: set.get_size_woubt6_k$() === 17});\n"
        "} catch(e) { results.push({n:'IntSet ERROR', p: false, err: String(e)}); }\n"

        // ScatterSet tests
        "try {\n"
        "  var scatterKey = Object.keys($).find(function(k) {\n"
        "    var v = $[k];\n"
        "    return typeof v === 'function' && v.toString().indexOf('MutableScatterSet') >= 0;\n"
        "  });\n"
        "  if (scatterKey) {\n"
        "    var sset = new $[scatterKey]();\n"
        "    results.push({n:'ScatterSet: empty size=0', p: sset.get_size_woubt6_k$() === 0});\n"
        "    sset.add_utx5q5_k$('a');\n"
        "    sset.add_utx5q5_k$('b');\n"
        "    sset.add_utx5q5_k$('hello');\n"
        "    results.push({n:'ScatterSet: size=3 after 3 adds', p: sset.get_size_woubt6_k$() === 3});\n"
        "    results.push({n:'ScatterSet: contains(a)=true', p: sset.contains_aljjnj_k$('a')});\n"
        "    results.push({n:'ScatterSet: contains(hello)=true', p: sset.contains_aljjnj_k$('hello')});\n"
        "    results.push({n:'ScatterSet: contains(c)=false', p: !sset.contains_aljjnj_k$('c')});\n"
        "    sset.remove_cedx0m_k$('a');\n"
        "    results.push({n:'ScatterSet: size=2 after remove', p: sset.get_size_woubt6_k$() === 2});\n"
        "    results.push({n:'ScatterSet: contains(a)=false after remove', p: !sset.contains_aljjnj_k$('a')});\n"
        "  }\n"
        "} catch(e) { results.push({n:'ScatterSet ERROR', p: false, err: String(e)}); }\n"

        // Large map test (triggers resize)
        "try {\n"
        "  var map = new $['v']();\n"
        "  for (var i = 0; i < 20; i++) { map.set_hupg49_k$(i, 'val_' + i); }\n"
        "  results.push({n:'Large map: size=20', p: map.get_size_woubt6_k$() === 20});\n"
        "  results.push({n:'Large map: first=val_0', p: map.get_c1px32_k$(0) === 'val_0'});\n"
        "  results.push({n:'Large map: last=val_19', p: map.get_c1px32_k$(19) === 'val_19'});\n"
        "  results.push({n:'Large map: middle correct', p: map.get_c1px32_k$(10) === 'val_10'});\n"
        "  // Remove every other\n"
        "  for (var i = 0; i < 20; i += 2) { map.remove_cqondg_k$(i); }\n"
        "  results.push({n:'Large map: size=10 after removing evens', p: map.get_size_woubt6_k$() === 10});\n"
        "  // Add more\n"
        "  for (var i = 100; i < 110; i++) { map.set_hupg49_k$(i, 'val_' + i); }\n"
        "  results.push({n:'Large map: size=20 after adding more', p: map.get_size_woubt6_k$() === 20});\n"
        "} catch(e) { results.push({n:'Large map ERROR', p: false, err: String(e)}); }\n"

        "JSON.stringify(results);\n";

    result = JS_Eval(ctx, tests, strlen(tests), "<tests>", JS_EVAL_TYPE_GLOBAL);
    if (JS_IsException(result)) {
        JSValue exc = JS_GetException(ctx);
        const char *str = JS_ToCString(ctx, exc);
        printf("Test evaluation FAILED: %s\n", str ? str : "(null)");
        JS_FreeCString(ctx, str);
        JS_FreeValue(ctx, exc);
    } else {
        const char *json_str = JS_ToCString(ctx, result);
        printf("Test results: %s\n", json_str ? json_str : "(null)");
        JS_FreeCString(ctx, json_str);
    }
    JS_FreeValue(ctx, result);

    JS_FreeContext(ctx);
    JS_FreeRuntime(rt);

    printf("\n=== Test Complete ===\n");
    return 0;
}
