/*
 * Standalone test for IntSetBuiltins C intrinsics.
 * Also loads and runs Kotlin/JS collection tests via quickjs.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include "quickjs/quickjs.h"
#include "IntSetBuiltins.cpp"

extern "C" void js_intset_register_builtins(JSContext *ctx);

static char* read_file(const char *path, size_t *out_len) {
    FILE *f = fopen(path, "rb");
    if (!f) { printf("ERROR: Cannot open %s\n", path); return NULL; }
    struct stat st;
    if (fstat(fileno(f), &st) < 0) { fclose(f); return NULL; }
    char *buf = (char*)malloc(st.st_size + 1);
    if (!buf) { fclose(f); return NULL; }
    fread(buf, 1, st.st_size, f);
    buf[st.st_size] = 0;
    *out_len = st.st_size;
    fclose(f);
    return buf;
}

int main(int argc, char **argv) {
    JSRuntime *rt = JS_NewRuntime();
    JSContext *ctx = JS_NewContext(rt);

    // Register C intrinsics
    js_intset_register_builtins(ctx);

    // Verify all intrinsics exist
    JSValue global = JS_GetGlobalObject(ctx);
    const char *funcs[] = {"_intsetFind", "_intsetAdd", "_intsetRemove",
                           "_intObjectMapFind", "_intObjectMapPut", "_intObjectMapRemove",
                           "_intObjectMapFindAvailableSlot", "_scatterSetFind"};
    int passed = 0, failed = 0;
    printf("=== Intrinsic Registration Check ===\n");
    for (size_t i = 0; i < sizeof(funcs)/sizeof(funcs[0]); i++) {
        JSValue fn = JS_GetPropertyStr(ctx, global, funcs[i]);
        const char *status = JS_IsFunction(ctx, fn) ? "OK" : "MISSING";
        printf("  %s: %s\n", funcs[i], status);
        if (JS_IsFunction(ctx, fn)) passed++; else failed++;
        JS_FreeValue(ctx, fn);
    }
    JS_FreeValue(ctx, global);

    if (failed > 0) {
        printf("\nFAILED: %d intrinsics missing\n", failed);
        JS_FreeContext(ctx);
        JS_FreeRuntime(rt);
        return 1;
    }
    printf("All intrinsics registered OK\n\n");

    // Test 1: Basic intrinsic algorithm tests
    printf("=== Intrinsic Algorithm Tests ===\n");
    const char *algo_tests =
        "var results = [];\n"
        // _intsetFind on empty table -> -1
        "var mf = new Int32Array(16); for(var i=0;i<16;i++) mf[i]=0x80;\n"
        "var el = new Int32Array(16);\n"
        "var r = _intsetFind(mf, el, 16, 42, 42, 42);\n"
        "results.push({n:'_intsetFind(empty)=-1', p: r===-1});\n"
        // _intsetAdd
        "var c = new Int32Array(1), sd = new Int32Array(1);\n"
        "var ai = _intsetAdd(mf, el, 16, 42, 42, 42, c, sd);\n"
        "results.push({n:'_intsetAdd returns valid idx', p: ai>=0&&ai<16});\n"
        "results.push({n:'_intsetAdd created=1', p: c[0]===1});\n"
        "results.push({n:'el[ai]===42', p: el[ai]===42});\n"
        // _intsetFind on existing
        "var fi = _intsetFind(mf, el, 16, 42, 42, 42);\n"
        "results.push({n:'_intsetFind(existing) returns idx', p: fi===ai});\n"
        // _intsetRemove
        "var ri = _intsetRemove(mf, el, 16, 42, 42, 42);\n"
        "results.push({n:'_intsetRemove returns idx', p: ri===ai});\n"
        "results.push({n:'_intsetFind after remove =-1', p: _intsetFind(mf,el,16,42,42,42)===-1});\n"
        // _intObjectMapFind/Put/Remove
        "var m2 = new Int32Array(16); for(var i=0;i<16;i++) m2[i]=0x80;\n"
        "var ks = new Int32Array(16), vs = new Int32Array(16);\n"
        "var mi = _intObjectMapPut(m2, ks, 16, 100, 100, 100&127, c, sd);\n"
        "results.push({n:'_intObjectMapPut valid idx', p: mi>=0&&mi<16});\n"
        "results.push({n:'_intObjectMapFind finds key', p: _intObjectMapFind(m2,ks,16,100,100,100&127)===mi});\n"
        "results.push({n:'_intObjectMapRemove returns idx', p: _intObjectMapRemove(m2,ks,16,100,100,100&127)===mi});\n"
        "results.push({n:'_intObjectMapFind after rem=-1', p: _intObjectMapFind(m2,ks,16,100,100,100&127)===-1});\n"
        // _scatterSetFind
        "var m3 = new Int32Array(16); for(var i=0;i<16;i++) m3[i]=0x80;\n"
        "var els = new Array(16); els[0] = 'hello';\n"
        "var si = _scatterSetFind(m3, els, 16, 'hello', 0, 0);\n"
        "results.push({n:'_scatterSetFind finds elem', p: si>=0&&si<16});\n"
        // Multiple elements in same group (hash collision test)
        "var m4 = new Int32Array(16); for(var i=0;i<16;i++) m4[i]=0x80;\n"
        "var el4 = new Int32Array(16);\n"
        "var h1 = 7, h2 = 7&127;\n"  // hash that maps to same group
        "_intsetAdd(m4, el4, 16, 1, h1, h2, c, sd);\n"
        "var idx1 = _intsetFind(m4, el4, 16, 1, h1, h2);\n"
        "_intsetAdd(m4, el4, 16, 9, h1, h2, c, sd);\n"
        "var idx2 = _intsetFind(m4, el4, 16, 9, h1, h2);\n"
        "results.push({n:'find after 2 adds works', p: idx1>=0&&idx2>=0});\n"
        // FindAvailableSlot
        "var m5 = new Int32Array(16); for(var i=0;i<16;i++) m5[i]=0x80;\n"
        "var slot = _intObjectMapFindAvailableSlot(m5, 16, 0);\n"
        "results.push({n:'_intObjectMapFindAvailableSlot OK', p: slot>=0&&slot<16});\n"
        // Delete and re-find
        "var m6 = new Int32Array(16); for(var i=0;i<16;i++) m6[i]=0x80;\n"
        "var el6 = new Int32Array(16);\n"
        "var i6 = _intsetAdd(m6, el6, 16, 123, 123, 123, c, sd);\n"
        "_intsetRemove(m6, el6, 16, 123, 123, 123);\n"
        "var i6b = _intsetAdd(m6, el6, 16, 456, 456, 456, c, sd);\n"
        "results.push({n:'re-insert after delete works', p: i6b>=0&&i6b<16});\n"
        "results.push({n:'el6[re-inserted]=456', p: el6[i6b]===456});\n"
        "JSON.stringify(results);\n";

    JSValue js_result = JS_Eval(ctx, algo_tests, strlen(algo_tests), "<algo_test>", JS_EVAL_TYPE_GLOBAL);
    if (JS_IsException(js_result)) {
        JSValue exc = JS_GetException(ctx);
        const char *str = JS_ToCString(ctx, exc);
        printf("JS EXCEPTION: %s\n", str ? str : "(null)");
        JS_FreeCString(ctx, str);
        JS_FreeValue(ctx, exc);
    } else {
        const char *json_str = JS_ToCString(ctx, js_result);
        printf("Algorithm test results: %s\n", json_str ? json_str : "(null)");
        JS_FreeCString(ctx, json_str);
    }
    JS_FreeValue(ctx, js_result);

    // Test 2: Try loading Kotlin/JS collection module if available
    printf("\n=== Kotlin/JS Module Test ===\n");
    size_t js_len = 0;
    char *js_code = NULL;

    // Try to find the compiled Kotlin/JS module
    const char *paths[] = {
        "build/js/packages/collection/kotlin/collection.js",
        "../collection-1.5.0/build/js/packages/collection/kotlin/collection.js",
        NULL
    };

    for (int i = 0; paths[i]; i++) {
        js_code = read_file(paths[i], &js_len);
        if (js_code) {
            printf("Loading %s...\n", paths[i]);
            JSValue result = JS_Eval(ctx, js_code, js_len, paths[i], JS_EVAL_TYPE_GLOBAL);
            if (JS_IsException(result)) {
                printf("  FAILED to load\n");
                JSValue exc = JS_GetException(ctx);
                const char *str = JS_ToCString(ctx, exc);
                printf("  Exception: %s\n", str ? str : "(null)");
                JS_FreeCString(ctx, str);
                JS_FreeValue(ctx, exc);
            } else {
                printf("  Loaded OK\n");
                JS_FreeValue(ctx, result);
            }
            free(js_code);
            break;
        } else {
            printf("  Not found at %s\n", paths[i]);
        }
    }

    // Test 3: Run actual collection-like JS that uses the intrinsics
    printf("\n=== Full Collection Simulation Test ===\n");
    const char *full_test =
        "// Simulate what Kotlin/JS IntSet does\n"
        "var M_EMPTY = 0x80, M_DELETED = 0xFE, M_SENTINEL = 0xFF;\n"

        "function makeIntSet() {\n"
        "  var CAP = 16;\n"
        "  var metadata = new Int32Array(CAP);\n"
        "  for (var i = 0; i < CAP; i++) metadata[i] = M_EMPTY;\n"
        "  var elements = new Int32Array(CAP);\n"
        "  var size = 0;\n"
        "  return { metadata, elements, size, CAP };\n"
        "}\n"

        "function intSetAdd(set, elem) {\n"
        "  var hash = elem | 0;\n"
        "  var h2 = hash & 127;\n"
        "  var c = new Int32Array(1), sd = new Int32Array(1);\n"
        "  var idx = _intsetAdd(set.metadata, set.elements, set.CAP, elem, hash, h2, c, sd);\n"
        "  if (c[0] === 1) set.size++;\n"
        "  return idx;\n"
        "}\n"

        "function intSetContains(set, elem) {\n"
        "  var hash = elem | 0;\n"
        "  var h2 = hash & 127;\n"
        "  return _intsetFind(set.metadata, set.elements, set.CAP, elem, hash, h2) >= 0;\n"
        "}\n"

        "function intSetRemove(set, elem) {\n"
        "  var hash = elem | 0;\n"
        "  var h2 = hash & 127;\n"
        "  var idx = _intsetRemove(set.metadata, set.elements, set.CAP, elem, hash, h2);\n"
        "  if (idx >= 0) set.size--;\n"
        "  return idx;\n"
        "}\n"

        "var results = [];\n"
        "var set = makeIntSet();\n"
        "results.push({n:'empty set has size 0', p: set.size === 0});\n"
        "results.push({n:'empty set contains nothing', p: !intSetContains(set, 1)});\n"
        "intSetAdd(set, 1);\n"
        "results.push({n:'after add size=1', p: set.size === 1});\n"
        "results.push({n:'contains added elem', p: intSetContains(set, 1)});\n"
        "results.push({n:'does not contain missing', p: !intSetContains(set, 2)});\n"
        "intSetAdd(set, 2);\n"
        "results.push({n:'size=2 after 2 adds', p: set.size === 2});\n"
        "intSetRemove(set, 1);\n"
        "results.push({n:'size=1 after remove', p: set.size === 1});\n"
        "results.push({n:'removed elem gone', p: !intSetContains(set, 1)});\n"
        "results.push({n:'other elem still there', p: intSetContains(set, 2)});\n"
        "intSetRemove(set, 2);\n"
        "results.push({n:'size=0 after all removed', p: set.size === 0});\n"
        // Add many elements
        "for (var i = 0; i < 10; i++) intSetAdd(set, i * 3);\n"
        "results.push({n:'size=10 after 10 adds', p: set.size === 10});\n"
        "for (var i = 0; i < 10; i++) results.push({n:'contains elem '+(i*3), p: intSetContains(set, i*3)});\n"
        // Remove every other
        "for (var i = 0; i < 10; i++) if (i % 2 === 0) intSetRemove(set, i * 3);\n"
        "results.push({n:'size=5 after removing 5', p: set.size === 5});\n"
        "for (var i = 0; i < 10; i++) {\n"
        "  var shouldHave = (i % 2 !== 0);\n"
        "  results.push({n:'after remove, contains '+(i*3)+':'+shouldHave, p: intSetContains(set, i*3) === shouldHave});\n"
        "}\n"

        "JSON.stringify(results);\n";

    js_result = JS_Eval(ctx, full_test, strlen(full_test), "<full_test>", JS_EVAL_TYPE_GLOBAL);
    if (JS_IsException(js_result)) {
        JSValue exc = JS_GetException(ctx);
        const char *str = JS_ToCString(ctx, exc);
        printf("JS EXCEPTION: %s\n", str ? str : "(null)");
        JS_FreeCString(ctx, str);
        JS_FreeValue(ctx, exc);
    } else {
        const char *json_str = JS_ToCString(ctx, js_result);
        printf("Full collection test results: %s\n", json_str ? json_str : "(null)");
        JS_FreeCString(ctx, json_str);
    }
    JS_FreeValue(ctx, js_result);

    JS_FreeContext(ctx);
    JS_FreeRuntime(rt);

    printf("\n=== Test Complete ===\n");
    return 0;
}
