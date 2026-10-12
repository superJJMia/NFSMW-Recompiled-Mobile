#!/usr/bin/env python3
"""
El host tool (rexglue) tambien compila con MinGW, sin MSVC.

    python tools/parche_host_mingw.py            aplicar
    python tools/parche_host_mingw.py --estado
    python tools/parche_host_mingw.py --revertir

Toca un fichero del SDK del motor nativo (via NFSMW_SDK, como parche_iso.py):

    include/rex/chrono/chrono.h   el shim de clock_time_conversion/clock_cast
                                  tambien en MinGW

No guarda .original: aplica y deshace por sustitucion de texto exacta, bloque a
bloque, como los demas parches de este proyecto.


POR QUE HACE FALTA
==================

chrono.h declara el primario clock_time_conversion solo en Apple y Android,
cuyas libc++ no lo exponen. La libc++ que acompana a MinGW tampoco, asi que
compilar rexglue en Windows con clang de MinGW (sin Visual Studio) falla con:

    error: explicit specialization of non-template struct
           'clock_time_conversion'

Con Visual Studio no cambia nada: la condicion solo anade MinGW.
"""

import argparse
import os
import pathlib
import sys


FICHERO = "include/rex/chrono/chrono.h"

ANCLA = """#if defined(__APPLE__) || defined(__ANDROID__)
// Apple and Android libc++ versions used by the supported SDKs do not expose
// clock_time_conversion or clock_cast."""

NUEVO = """// PARCHE LOCAL - HOST-MINGW: la libc++ de MinGW tampoco expone
// clock_time_conversion ni clock_cast; sin esto, compilar rexglue en Windows
// sin Visual Studio falla. Con VS no cambia nada.
#if defined(__APPLE__) || defined(__ANDROID__) || defined(__MINGW32__)
// Apple, Android y MinGW: sus libc++ no exponen clock_time_conversion ni
// clock_cast."""

BLOQUES = [
    ("shim de clock_time_conversion tambien en MinGW", ANCLA, NUEVO),
]


def localizar_sdk():
    otro = os.environ.get("NFSMW_SDK")
    raiz = pathlib.Path(__file__).resolve().parent.parent
    candidatos = [pathlib.Path(otro)] if otro else [raiz.parent / "rexglue-sdk", raiz / "sdk"]
    for cand in candidatos:
        if (cand / FICHERO).exists():
            return cand
    sys.exit(f"[ERROR] No encuentro {FICHERO} del SDK.\n"
             "        Se busca en NFSMW_SDK, o en ..\\rexglue-sdk y .\\sdk")


def leer(f):
    # newline="" para respetar los finales de linea que haya (LF o CRLF).
    with open(f, encoding="utf-8", newline="") as h:
        txt = h.read()
    eol = "\r\n" if "\r\n" in txt else "\n"
    return txt.replace("\r\n", "\n"), eol


def escribir(f, txt, eol):
    with open(f, "w", encoding="utf-8", newline="") as h:
        h.write(txt.replace("\n", eol))


def main():
    p = argparse.ArgumentParser(add_help=True)
    p.add_argument("--estado", action="store_true")
    p.add_argument("--revertir", action="store_true")
    args = p.parse_args()

    f = localizar_sdk() / FICHERO
    txt, eol = leer(f)

    if args.estado:
        puestos = sum(1 for _, _, nuevo in BLOQUES if nuevo in txt)
        print(f"  host-mingw                {puestos} de {len(BLOQUES)} bloques aplicados")
        for nombre, _, nuevo in BLOQUES:
            print(f"      {'si' if nuevo in txt else 'NO':>2}  {nombre}  ({FICHERO})")
        return 0

    if args.revertir:
        quitados = 0
        for nombre, ancla, nuevo in BLOQUES:
            if nuevo not in txt:
                continue
            if txt.count(nuevo) != 1:
                sys.exit(f"[ERROR] El bloque '{nombre}' aparece {txt.count(nuevo)} veces "
                         f"en {FICHERO}. No lo toco, quitalo tu.")
            txt = txt.replace(nuevo, ancla)
            quitados += 1
        if not quitados:
            print("[ok] host-mingw: no habia nada puesto")
            return 0
        escribir(f, txt, eol)
        print(f"[ok] Quitados {quitados} bloques")
        return 0

    faltan = [b for b in BLOQUES if b[2] not in txt]
    if not faltan:
        print(f"[ok] host-mingw: los {len(BLOQUES)} bloques ya estaban")
        return 0

    # Se comprueban TODOS los anclajes antes de escribir nada.
    for nombre, ancla, _ in faltan:
        n = txt.count(ancla)
        if n != 1:
            sys.exit(f"[ERROR] El anclaje de '{nombre}' aparece {n} veces en {FICHERO}, "
                     f"esperaba 1. Puede que este SDK sea otro. No he tocado nada.")

    for nombre, ancla, nuevo in faltan:
        txt = txt.replace(ancla, nuevo)
        print(f"[ok] Aplicado: {nombre}")

    escribir(f, txt, eol)
    return 0


if __name__ == "__main__":
    sys.exit(main())
