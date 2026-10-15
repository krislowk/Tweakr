#!/usr/bin/env python3
"""Tweakr - a simple APK editor for Termux."""
import os
import subprocess
import sys

WORK = os.path.expanduser("~/Tweakr/work")
OUT = os.path.expanduser("~/storage/shared/Tweakr")
KEY = os.path.expanduser("~/Tweakr/tweakr.keystore")
PASS = "tweakr123"


def run(cmd):
    print("$", " ".join(cmd))
    return subprocess.run(cmd).returncode == 0


def decode(apk):
    name = os.path.splitext(os.path.basename(apk))[0]
    dest = os.path.join(WORK, name)
    os.makedirs(WORK, exist_ok=True)
    if run(["apktool", "d", "-f", apk, "-o", dest]):
        print(f"\nDecoded to: {dest}")
        print("Edit files there (smali/, res/, AndroidManifest.xml), then run:")
        print(f"  python tweakr.py build {name}")


def make_key():
    if os.path.exists(KEY):
        return True
    return run([
        "keytool", "-genkey", "-v", "-keystore", KEY,
        "-alias", "tweakr", "-keyalg", "RSA", "-keysize", "2048",
        "-validity", "10000", "-storepass", PASS, "-keypass", PASS,
        "-dname", "CN=Tweakr",
    ])


def build(name):
    src = os.path.join(WORK, name)
    if not os.path.isdir(src):
        print("Project not found:", src)
        return
    os.makedirs(OUT, exist_ok=True)
    unsigned = os.path.join(OUT, f"{name}-unsigned.apk")
    signed = os.path.join(OUT, f"{name}-tweaked.apk")
    if not run(["apktool", "b", src, "-o", unsigned]):
        print("Build failed.")
        return
    if not make_key():
        print("Keystore creation failed.")
        return
    if run([
        "apksigner", "sign", "--ks", KEY, "--ks-pass", f"pass:{PASS}",
        "--out", signed, unsigned,
    ]):
        os.remove(unsigned)
        print(f"\nDone: {signed}")


def info(apk):
    run(["aapt2", "dump", "badging", apk])


def usage():
    print("""Tweakr - APK editor

Usage:
  python tweakr.py decode <file.apk>   Unpack an APK for editing
  python tweakr.py build <name>        Rebuild and sign the edited project
  python tweakr.py info <file.apk>     Show package info
""")


def main():
    if len(sys.argv) < 3:
        usage()
        return
    cmd, arg = sys.argv[1], sys.argv[2]
    if cmd == "decode":
        decode(arg)
    elif cmd == "build":
        build(arg)
    elif cmd == "info":
        info(arg)
    else:
        usage()


if __name__ == "__main__":
    main()
