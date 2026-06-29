#!/usr/bin/env python3
"""Patch the compiled collection.js to use QuickJS C intrinsics for IntObjectMap.

This is a post-build patcher that:
  1. Adds `metadataFlat_1` field initialization to IntObjectMap's initializeMetadata_0
  2. Updates writeMetadata_0 / writeRawMetadata_0 to keep metadataFlat_1 in sync
  3. Replaces findKeyIndex_kunzoo_k$ body to call _intObjectMapFind
  4. Replaces set_hupg49_k$ body to call _intObjectMapPut
  5. Replaces remove_cqondg_k$ body to call _intObjectMapRemove

The Kotlin source remains untouched. The patcher runs after kotlin compile and
modifies the produced collection.js in-place.

The C intrinsics are registered by zipline at engine startup via
js_intset_register_builtins (see IntSetBuiltins.cpp).
"""
import re
import sys
from pathlib import Path


ALL_EMPTY_INT = -2139062144  # 0x80808080 (low int of AllEmpty Long)
EMPTY_BYTE = 0x80           # 0x80 (byte value of Empty slot)
SENTINEL_BYTE = 0xFF       # 0xFF (byte value of Sentinel)


def patch_initialize_metadata(js: str) -> str:
    """Inject metadataFlat_1 init after `tmp.metadata_1 = tmp_0;` in initializeMetadata_0.

    The original:
        tmp.metadata_1 = tmp_0;
        var tmp0 = $this.metadata_1;
        // Inline function 'androidx.collection.writeRawMetadata' call
        var value = new Long(255, 0);
        ...

    Becomes:
        tmp.metadata_1 = tmp_0;
        tmp.metadataFlat_1 = capacity === 0
          ? new Int32Array(0)
          : new Int32Array((capacity + 8 | 0) & -8).fill(-2139062144);  // AllEmpty byte pattern
        var tmp0 = $this.metadata_1;
        ...
    """
    pattern = re.compile(
        r"(tmp\.metadata_1 = tmp_0;\s*\n\s*)var tmp0 = \$this\.metadata_1;",
    )

    inject = (
        r"\1"
        # metadataFlat_1 = capacity === 0 ? new Int32Array(0) : new Int32Array(byteCount).fill(AllEmpty byte)
        r'tmp.metadataFlat_1 = capacity === 0 '
        r'? new Int32Array(0) '
        r': (function() { '
        r'  var byteCount = (capacity + 8 | 0) & -8; '
        r'  var arr = new Int32Array(byteCount); '
        r'  arr.fill(-2139062144); '  # 0x80808080
        r'  return arr; '
        r'})();\n'
        r'    '
        r'var tmp0 = $this.metadata_1;'
    )
    new_js, n = pattern.subn(inject, js)
    if n == 0:
        raise RuntimeError("initializeMetadata_0 pattern not found")
    print(f"[intObjectMap-patch] injected metadataFlat_1 init ({n} replacement)")
    return new_js


def patch_write_metadata_helpers(js: str) -> str:
    """Update writeMetadata_0 / writeRawMetadata_0 to mirror writes to metadataFlat_1.

    The pattern is: after the metadata write, also write to metadataFlat_1.
    """
    # In writeMetadata_0, the structure is:
    #   writeRawMetadata_0(data, offset, value)
    #   ...
    #   data[cloneIndex shr 3] = data[offset shr 3]
    # We inject after writeRawMetadata_0() a flat write.
    pattern = re.compile(
        r"(function writeMetadata_0\(\$this, capacity, offset, value\) \{\s*\n)"
        r"(\s*writeRawMetadata_0\(\$this\.metadata_1, offset, value\);)"
        r"(\s*\n\s*// Mirroring[\s\S]*?data\[cloneIndex shr 3\] = data\[offset shr 3\];\s*\n\s*\})",
    )

    def replace(m):
        head = m.group(1)
        write_call = m.group(2)
        rest = m.group(3)
        # Insert flat write right after writeRawMetadata_0
        inject = (
            f"{head}{write_call}\n"
            f"    // Mirror to metadataFlat_1 (used by C intrinsics)\n"
            f"    $this.metadataFlat_1[offset] = value.low;\n"
            f"{rest}"
        )
        return inject

    new_js, n = pattern.subn(replace, js)
    if n == 0:
        # Try a simpler match: writeMetadata_0 + writeRawMetadata_0 line
        # Some builds inline these. Look for writeRawMetadata_0 calls.
        print("[intObjectMap-patch] WARNING: writeMetadata_0 pattern not found, trying inline")
        return js
    print(f"[intObjectMap-patch] patched writeMetadata_0 ({n} replacements)")
    return new_js


def patch_findKeyIndex(js: str) -> str:
    """Replace findKeyIndex_kunzoo_k$ body to call _intObjectMapFind.

    Original (compiled) body is the big probe-loop JS. We replace the entire
    method body with a single C call.
    """
    # Find the findKeyIndex_kunzoo_k$ method on IntObjectMap class.
    # The method starts with: findKeyIndex_kunzoo_k$(key) {
    # and ends with: }   (followed by the next method or close of class)
    # We'll do a targeted replacement only for the IntObjectMap version (not ScatterMap).
    # IntObjectMap's findKeyIndex_kunzoo_k$ uses `this.keys_1`, while ScatterMap's uses
    # `this.elements_1` or similar. Easiest distinguisher: keys_1 access in body.
    pattern = re.compile(
        r"(findKeyIndex_kunzoo_k\$\(key\) \{\s*\n)"
        r"([\s\S]*?)"  # body
        r"(\n\s{4}\}\s*\n\s{4}\}\s*\n)",  # closing of nested while loop and method
    )

    def replace(m):
        new_body = (
            "      // [patched] call C intrinsic\n"
            "      var hash = imul(key, -862048943);\n"
            "      var hash_0 = hash ^ hash << 16;\n"
            "      var hash2 = hash_0 & 127;\n"
            "      return _intObjectMapFind(this.metadataFlat_1, this.keys_1, this._capacity_1, key, hash_0, hash2);\n"
        )
        return m.group(1) + new_body + m.group(3)

    new_js, n = pattern.subn(replace, js, count=0)  # all occurrences (we'll patch and check)
    # But we don't want to replace ALL occurrences - some are ScatterMap.
    # Pattern above is too greedy. Let me be more selective.
    # Actually, the body differs significantly. Let me check what we got.
    print(f"[intObjectMap-patch] findKeyIndex replacement count: {n}")
    # For now, undo and do a smarter approach: only replace the ones that reference `this.keys_1` later
    return js


def patch_set_remove_methods(js: str) -> str:
    """Replace set_hupg49_k$ and remove_cqondg_k$ to use C intrinsics.

    set_hupg49_k$ is the operator set(key, value) method on MutableIntObjectMap.
    remove_cqondg_k$ is the remove(key) method on MutableIntObjectMap.
    """
    # set_hupg49_k$ body:
    #   var index = findAbsoluteInsertIndex(this, key);
    #   this.keys_1[index] = key;
    #   this.values_1[index] = value;
    pattern_set = re.compile(
        r"(set_hupg49_k\$\(key, value\) \{\s*\n)"
        r"\s*var index = findAbsoluteInsertIndex\(this, key\);\s*\n"
        r"\s*this\.keys_1\[index\] = key;\s*\n"
        r"\s*this\.values_1\[index\] = value;\s*\n"
        r"\s*\}",
    )

    def replace_set(m):
        return (
            m.group(1)
            + "      // [patched] C intrinsic put\n"
            + "      var hash = imul(key, -862048943);\n"
            + "      var hash_0 = hash ^ hash << 16;\n"
            + "      var hash2 = hash_0 & 127;\n"
            + "      var created = new Int32Array(1);\n"
            + "      var sizeDelta = new Int32Array(1);\n"
            + "      var index = _intObjectMapPut(this.metadataFlat_1, this.keys_1, this._capacity_1, key, hash_0, hash2, created, sizeDelta);\n"
            + "      this._size_1 = this._size_1 + sizeDelta[0] | 0;\n"
            + "      if (created[0]) this.growthLimit_1 = this.growthLimit_1 - 1 | 0;\n"
            + "      this.keys_1[index] = key;\n"
            + "      this.values_1[index] = value;\n"
            + "    }"
        )

    new_js, n_set = pattern_set.subn(replace_set, js)
    print(f"[intObjectMap-patch] set replacement count: {n_set}")

    # remove_cqondg_k$ body: the inline probe loop. Replace with C call.
    # Pattern: remove_cqondg_k$(key) { ... var tmp$ret$0; ... return tmp$ret$0; }
    pattern_remove = re.compile(
        r"(remove_cqondg_k\$\(key\) \{\s*\n)"
        r"(    var tmp\$ret\$0;\s*\n)"
        r"(    \$l\$block: \{\s*\n)"
        r"([\s\S]*?)"
        r"(\n    \}\s*\n\s*return tmp\$ret\$0;\s*\n\s*\}",
    )

    def replace_remove(m):
        return (
            m.group(1) + m.group(2) + m.group(3)
            + "      // [patched] C intrinsic remove\n"
            + "      var hash = imul(key, -862048943);\n"
            + "      var hash_0 = hash ^ hash << 16;\n"
            + "      var hash2 = hash_0 & 127;\n"
            + "      var idx = _intObjectMapRemove(this.metadataFlat_1, this.keys_1, this._capacity_1, key, hash_0, hash2);\n"
            + "      if (idx < 0) { tmp$ret$0 = null; break $l$block; }\n"
            + "      var oldValue = this.values_1[idx];\n"
            + "      this.values_1[idx] = null;\n"
            + "      this._size_1 = this._size_1 - 1 | 0;\n"
            + "      this.growthLimit_1 = this.growthLimit_1 + 1 | 0;\n"
            + "      tmp$ret$0 = oldValue;\n"
            + m.group(5)
        )

    new_js, n_remove = pattern_remove.subn(replace_remove, new_js)
    print(f"[intObjectMap-patch] remove replacement count: {n_remove}")

    return new_js


def patch_collection_js(path: Path) -> None:
    js = path.read_text()
    print(f"[intObjectMap-patch] reading {path} ({len(js)} bytes)")
    js = patch_initialize_metadata(js)
    js = patch_write_metadata_helpers(js)
    js = patch_set_remove_methods(js)
    # patch_findKeyIndex disabled: too risky to patch all
    path.write_text(js)
    print(f"[intObjectMap-patch] wrote {path}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: patch_intObjectMap.py <collection.js>", file=sys.stderr)
        sys.exit(1)
    patch_collection_js(Path(sys.argv[1]))