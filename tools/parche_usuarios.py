#!/usr/bin/env python3
"""
El perfil unico (XUID fabricado) vale para los 4 usuarios, no solo para el 0.

    python tools/parche_usuarios.py            aplicar
    python tools/parche_usuarios.py --estado
    python tools/parche_usuarios.py --revertir

Toca un fichero del SDK del motor nativo (via NFSMW_SDK, como parche_iso.py):

    src/kernel/xam/xam_user.cpp   XamUserGetXUID / GetSigninState / GetSigninInfo /
                                  GetName / GetGamerTag / Read+WriteProfileSettings /
                                  CheckPrivilege / ContentRestriction* /
                                  GetMembershipTier / AreUsersFriends

No guarda .original: aplica y deshace por sustitucion de texto exacta, bloque a
bloque, como los demas parches de este proyecto.


POR QUE HACE FALTA
==================

Xbox 360 admite como maximo 4 mandos (XUSER_MAX_COUNT = 4) y el juego escucha
al mando que pulsa Start. El SDK solo finge sesion iniciada en el usuario 0:
cualquier otro mando recibe X_E_NO_SUCH_USER / estado 0, asi que el juego no
enumera sus partidas, pide crear perfil de nuevo y nunca avanza. En un movil
o visor donde el sistema expone varios gamepads a la vez (p. ej. los Touch del
Quest mas un mando Bluetooth), el mando que el jugador usa puede caer en el
usuario 1, 2 o 3 y el juego se queda parado antes del primer [guardado].

El XUID es unico y fabricado, asi que todos los usuarios comparten las mismas
partidas: no se duplica nada en disco. NFSMW es de un jugador; un juego que
contara usuarios logados para multijugador veria 4 logados.
"""

import argparse
import os
import pathlib
import sys


FICHERO = "src/kernel/xam/xam_user.cpp"

CABECERA = """  // PARCHE LOCAL - USUARIOS: el perfil unico (XUID fabricado) vale para los
  // 4 usuarios. Xbox 360 admite como maximo 4 mandos y el juego puede escuchar
  // cualquiera: asi el mando que pulse Start siempre ve sesion y partidas.
"""

# (nombre, ancla, nuevo). Cada ancla aparece EXACTAMENTE 1 vez en el fichero.
BLOQUES = [
    ("GetXUID vale para 0-3",
     """  if (user_index < 4) {
    if (user_index == 0) {
      const auto& user_profile = REX_KERNEL_STATE()->user_profile();
      auto type = user_profile->type() & type_mask;""",
     CABECERA +
     """  if (user_index < 4) {
    if (user_index < 4) {
      const auto& user_profile = REX_KERNEL_STATE()->user_profile();
      auto type = user_profile->type() & type_mask;"""),

    ("GetSigninState vale para 0-3",
     """  uint32_t signin_state = 0;
  if (user_index < 4) {
    if (user_index == 0) {
      const auto& user_profile = REX_KERNEL_STATE()->user_profile();
      signin_state = user_profile->signin_state();""",
     CABECERA +
     """  uint32_t signin_state = 0;
  if (user_index < 4) {
    if (user_index < 4) {
      const auto& user_profile = REX_KERNEL_STATE()->user_profile();
      signin_state = user_profile->signin_state();"""),

    ("GetSigninInfo vale para 0-3",
     """  std::memset(info, 0, sizeof(X_USER_SIGNIN_INFO));
  if (user_index) {
    return X_E_NO_SUCH_USER;
  }""",
     """  std::memset(info, 0, sizeof(X_USER_SIGNIN_INFO));
""" + CABECERA.rstrip("\n")),

    ("GetName vale para 0-3",
     """  if (user_index) {
    return X_E_NO_SUCH_USER;
  }

  const auto& user_profile = REX_KERNEL_STATE()->user_profile();
  const auto& user_name = user_profile->name();""",
     CABECERA.rstrip("\n") + """
  const auto& user_profile = REX_KERNEL_STATE()->user_profile();
  const auto& user_name = user_profile->name();"""),

    ("GetGamerTag vale para 0-3",
     """  if (user_index) {
    return X_E_NO_SUCH_USER;
  }

  if (!buffer || buffer_len < 16) {""",
     CABECERA.rstrip("\n") + """
  if (!buffer || buffer_len < 16) {"""),

    ("ReadProfileSettings vale para 0-3",
     """  if (!xuids && user_index) {
    // Only support user 0.""",
     CABECERA.rstrip("\n") + """
  if (!xuids && user_index >= 4) {
    // Only support users 0-3."""),

    ("WriteProfileSettings vale para 0-3",
     """  if (user_index) {
    // Only support user 0.""",
     CABECERA.rstrip("\n") + """
  if (user_index >= 4) {
    // Only support users 0-3."""),

    ("CheckPrivilege niega igual para 0-3",
     """    if (user_index) {
      return X_ERROR_NO_SUCH_USER;
    }""",
     """    // PARCHE LOCAL - USUARIOS: la negacion total vale para 0-3."""),

    ("ContentRestrictionGetFlags vale para 0-3",
     """  if (user_index) {
    return X_ERROR_NO_SUCH_USER;
  }

  // No restrictions?""",
     CABECERA.rstrip("\n") + """
  // No restrictions?"""),

    ("ContentRestrictionGetRating vale para 0-3",
     """  if (user_index) {
    return X_ERROR_NO_SUCH_USER;
  }

  // Some games have special case paths for 3F that differ from the failure""",
     CABECERA.rstrip("\n") + """
  // Some games have special case paths for 3F that differ from the failure"""),

    ("GetMembershipTier vale para 0-3",
     """  if (user_index) {
    return X_ERROR_NO_SUCH_USER;
  }
  return 6 /* 6 appears to be Gold */;""",
     CABECERA.rstrip("\n") + """
  return 6 /* 6 appears to be Gold */;"""),

    ("AreUsersFriends trata 0-3 como el 0",
     """    if (user_index == 0) {
      const auto& user_profile = REX_KERNEL_STATE()->user_profile();
      if (user_profile->signin_state() == 0) {""",
     """    // PARCHE LOCAL - USUARIOS: como el usuario 0 (misma sesion/perfil).
    if (user_index < 4) {
      const auto& user_profile = REX_KERNEL_STATE()->user_profile();
      if (user_profile->signin_state() == 0) {"""),
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
        print(f"  usuarios                   {puestos} de {len(BLOQUES)} bloques aplicados")
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
            print("[ok] usuarios: no habia nada puesto")
            return 0
        escribir(f, txt, eol)
        print(f"[ok] Quitados {quitados} bloques")
        return 0

    faltan = [b for b in BLOQUES if b[2] not in txt]
    if not faltan:
        print(f"[ok] usuarios: los {len(BLOQUES)} bloques ya estaban")
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
