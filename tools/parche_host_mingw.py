#!/usr/bin/env python3
"""
El host tool (rexglue) tambien compila con MinGW, sin MSVC.

    python tools/parche_host_mingw.py            aplicar
    python tools/parche_host_mingw.py --estado
    python tools/parche_host_mingw.py --revertir

Toca ficheros del SDK del motor nativo (via NFSMW_SDK, como parche_iso.py):

    include/rex/chrono/chrono.h         el shim de clock_time_conversion /
                                        clock_cast tambien en MinGW
    src/input/xinput/xinput_input_driver.cpp
                                        reinterpret_cast al guardar los
                                        punteros de GetProcAddress

No guarda .original: aplica y deshace por sustitucion de texto exacta, bloque a
bloque, como los demas parches de este proyecto.


POR QUE HACE FALTA
==================

1. chrono.h declara el primario clock_time_conversion solo en Apple y Android,
   cuyas libc++ no lo exponen. La libc++ que acompana a MinGW tampoco, asi que
   compilar rexglue en Windows con clang de MinGW (sin Visual Studio) falla con:

       error: explicit specialization of non-template struct
              'clock_time_conversion'

2. Clang moderno no deja asignar el FARPROC (puntero a funcion) de
   GetProcAddress a los void* donde el driver XInput los guarda; MSVC lo
   aceptaba. El reinterpret_cast es correcto y no cambia nada.

Con Visual Studio no cambia nada en los dos casos.
"""

import argparse
import os
import pathlib
import sys


# (ruta en el SDK, nombre, ancla, nuevo). Cada ancla aparece EXACTAMENTE 1 vez.
BLOQUES = [
    ("include/rex/chrono/chrono.h",
     "shim de clock_time_conversion tambien en MinGW",
     """#if defined(__APPLE__) || defined(__ANDROID__)
// Apple and Android libc++ versions used by the supported SDKs do not expose
// clock_time_conversion or clock_cast.""",
     """// PARCHE LOCAL - HOST-MINGW: la libc++ de MinGW tampoco expone
// clock_time_conversion ni clock_cast; sin esto, compilar rexglue en Windows
// sin Visual Studio falla. Con VS no cambia nada.
#if defined(__APPLE__) || defined(__ANDROID__) || defined(__MINGW32__)
// Apple, Android y MinGW: sus libc++ no exponen clock_time_conversion ni
// clock_cast."""),

    ("src/input/xinput/xinput_input_driver.cpp",
     "reinterpret_cast al guardar punteros de GetProcAddress",
     """  module_ = module;
  XInputGetCapabilities_ = xigc;
  XInputGetState_ = xigs;
  XInputGetStateEx_ = xigsEx;
  XInputGetKeystroke_ = xigk;
  XInputSetState_ = xiss;
  XInputEnable_ = xie;""",
     """  // PARCHE LOCAL - HOST-MINGW: Clang no convierte implicito entre void* y
  // puntero a funcion (MSVC si); el reinterpret_cast es lo correcto.
  module_ = module;
  XInputGetCapabilities_ = reinterpret_cast<void*>(xigc);
  XInputGetState_ = reinterpret_cast<void*>(xigs);
  XInputGetStateEx_ = reinterpret_cast<void*>(xigsEx);
  XInputGetKeystroke_ = reinterpret_cast<void*>(xigk);
  XInputSetState_ = reinterpret_cast<void*>(xiss);
  XInputEnable_ = reinterpret_cast<void*>(xie);"""),
]


def localizar_sdk():
    otro = os.environ.get("NFSMW_SDK")
    raiz = pathlib.Path(__file__).resolve().parent.parent
    candidatos = [pathlib.Path(otro)] if otro else [raiz.parent / "rexglue-sdk", raiz / "sdk"]
    for cand in candidatos:
        if (cand / "src" / "system" / "runtime.cpp").exists():
            return cand
    sys.exit("[ERROR] No encuentro src/system/runtime.cpp del SDK.\n"
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

    sdk = localizar_sdk()
    ficheros = {}
    for ruta, _, _, _ in BLOQUES:
        if ruta not in ficheros:
            f = sdk / ruta
            if not f.exists():
                sys.exit(f"[ERROR] No encuentro {f}")
            ficheros[ruta] = [f, *leer(f)]

    if args.estado:
        puestos = sum(1 for r, _, _, nuevo in BLOQUES if nuevo in ficheros[r][1])
        print(f"  host-mingw                {puestos} de {len(BLOQUES)} bloques aplicados")
        for ruta, nombre, _, nuevo in BLOQUES:
            print(f"      {'si' if nuevo in ficheros[ruta][1] else 'NO':>2}  {nombre}  ({ruta})")
        return 0

    if args.revertir:
        quitados = 0
        for ruta, nombre, ancla, nuevo in BLOQUES:
            txt = ficheros[ruta][1]
            if nuevo not in txt:
                continue
            if txt.count(nuevo) != 1:
                sys.exit(f"[ERROR] El bloque '{nombre}' aparece {txt.count(nuevo)} veces "
                         f"en {ruta}. No lo toco, quitalo tu.")
            ficheros[ruta][1] = txt.replace(nuevo, ancla)
            quitados += 1
        if not quitados:
            print("[ok] host-mingw: no habia nada puesto")
            return 0
        for f, txt, eol in ficheros.values():
            escribir(f, txt, eol)
        print(f"[ok] Quitados {quitados} bloques")
        return 0

    faltan = [b for b in BLOQUES if b[3] not in ficheros[b[0]][1]]
    if not faltan:
        print(f"[ok] host-mingw: los {len(BLOQUES)} bloques ya estaban")
        return 0

    # Se comprueban TODOS los anclajes antes de escribir nada: un parche a
    # medias entre varios ficheros es peor que uno que no entra.
    for ruta, nombre, ancla, _ in faltan:
        n = ficheros[ruta][1].count(ancla)
        if n != 1:
            sys.exit(f"[ERROR] El anclaje de '{nombre}' aparece {n} veces en {ruta}, "
                     f"esperaba 1. Puede que este SDK sea otro. No he tocado nada.")

    for ruta, nombre, ancla, nuevo in faltan:
        ficheros[ruta][1] = ficheros[ruta][1].replace(ancla, nuevo)
        print(f"[ok] Aplicado: {nombre}")

    for f, txt, eol in ficheros.values():
        escribir(f, txt, eol)
    return 0


if __name__ == "__main__":
    sys.exit(main())
