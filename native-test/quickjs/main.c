#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <time.h>
#include "quickjs.h"
#include "quickjs-libc.h"

extern "C" void js_intset_register_builtins(JSContext *ctx);

static int eval_buf(JSContext *ctx, const void *buf, int buf_len,
                    const char *filename, int eval_flags)
{
    JSValue val;
    int ret;

    if ((eval_flags & JS_EVAL_TYPE_MASK) == JS_EVAL_TYPE_MODULE) {
        val = JS_Eval(ctx, buf, buf_len, filename,
                      eval_flags | JS_EVAL_FLAG_COMPILE_ONLY);
        if (!JS_IsException(val)) {
            js_module_set_import_meta(ctx, val, TRUE, TRUE);
            val = JS_EvalFunction(ctx, val);
        }
        val = js_std_await(ctx, val);
    } else {
        val = JS_Eval(ctx, buf, buf_len, filename, eval_flags);
    }
    if (JS_IsException(val)) {
        js_std_dump_error(ctx);
        ret = -1;
    } else {
        ret = 0;
    }
    JS_FreeValue(ctx, val);
    return ret;
}

static int eval_file(JSContext *ctx, const char *filename, int module, int strict)
{
    uint8_t *buf;
    int ret, eval_flags;
    size_t buf_len;

    buf = js_load_file(ctx, &buf_len, filename);
    if (!buf) {
        perror(filename);
        return -1;
    }

    if (module < 0) {
        module = (has_suffix(filename, ".mjs") ||
                  JS_DetectModule((const char *)buf, buf_len));
    }
    if (module) {
        eval_flags = JS_EVAL_TYPE_MODULE;
    } else {
        eval_flags = JS_EVAL_TYPE_GLOBAL;
        if (strict)
            eval_flags |= JS_EVAL_FLAG_STRICT;
    }

    ret = eval_buf(ctx, buf, buf_len, filename, eval_flags);
    js_free(ctx, buf);
    return ret;
}

int main(int argc, char **argv)
{
    JSRuntime *rt;
    JSContext *ctx;
    int ret, i;
    int empty_run = 0;
    int optind;

    for (i = 0; i < argc; i++) {
        if (!strcmp(argv[i], "-h") || !strcmp(argv[i], "--help")) {
            printf("QuickJS version %s\n", CONFIG_VERSION);
            printf("Usage: qjs [options] [file]\n");
            exit(0);
        }
        if (!strcmp(argv[i], "-q") || !strcmp(argv[i], "--quit")) {
            empty_run = 1;
        }
    }

    for (optind = 1; optind < argc; optind++) {
        if (argv[optind][0] != '-' || argv[optind][1] == '\0')
            break;
    }

    rt = JS_NewRuntime();
    ctx = JS_NewContext(rt);

    js_intset_register_builtins(ctx);

    js_std_init_handlers(rt);
    js_std_add_helpers(ctx, argc - optind, argv + optind);

    /* make 'std' and 'os' visible */
    {
        const char *str = "import * as std from 'std';\n"
                         "import * as os from 'os';\n"
                         "globalThis.std = std;\n"
                         "globalThis.os = os;\n"
                         "globalThis.setTimeout = os.setTimeout;\n"
                         "globalThis.clearTimeout = os.clearTimeout;\n";
        eval_buf(ctx, str, strlen(str), "<input>", JS_EVAL_TYPE_MODULE);
    }

    if (!empty_run) {
        for(i = 0; i < optind && i < argc; i++) {
            if (eval_file(ctx, argv[i], -1, 0) != 0)
                goto fail;
        }

        /* Run pending jobs without entering full event loop */
        while (JS_IsJobPending(rt)) {
            int err = JS_ExecutePendingJob(rt, &ctx);
            if (err < 0) {
                js_std_dump_error(ctx);
                break;
            }
            if (err == 0)
                break;
        }
    }

    js_std_free_handlers(rt);
    JS_FreeContext(ctx);
    JS_FreeRuntime(rt);
    return 0;
 fail:
    js_std_free_handlers(rt);
    JS_FreeContext(ctx);
    JS_FreeRuntime(rt);
    return 1;
}
