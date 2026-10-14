# NFSMW Recompiled — Android / Meta Quest

Aplicativo que roda **Need for Speed™: Most Wanted (2005)**, do Xbox 360, em
celulares Android e no **Meta Quest 3S**.

É uma **recompilação nativa** (não é emulador): o `default.xex` do Xbox 360 é
traduzido para C++ e compilado direto para ARM64, rodando como código nativo.
O app funciona com a sua **`.iso`** ou com uma **pasta com o `.xex` já
extraído**.

> **Você precisa da sua própria cópia do jogo.** Este repositório não contém
> dados do jogo, nem o `default.xex`, nem o código gerado, nem binários
> compilados.

## Versões do jogo compatíveis

Cada APK é compilado a partir de um `default.xex` específico e só roda com a
edição correspondente. Os APKs publicados aqui são da **PAL España**, que
também roda:

- **PAL España** (espanhol)
- **PAL Brasil / PT-BR** — usa a mesma compilação/tabela da PAL España

As edições PAL Alemanha, França e Itália são a mesma compilação da espanhola
(reconhecidas, mas não testadas). USA e Japão são outras compilações e exigem
um APK próprio, que não é publicado aqui.

## O que este fork traz

- **Launcher em português do Brasil**, sempre em paisagem: tela inicial simples
  (escolher o jogo + **JOGAR**) e o resto das opções em uma tela de
  **Configurações** separada.
- **Fix do save** — controles que se apresentam como vários perfis (família
  PS4/PS5) espalhavam os saves por slots diferentes; agora todos usam o mesmo
  login e os saves caem sempre na mesma pasta.
- **Fonte dupla** — a **`.iso`** no lugar, ou uma **pasta com o `.xex` extraído**.
- **Edição PT-BR** do disco reconhecida.
- Correções de crash da interface.
- **(só na versão com gamepad)** **gamepad emulado pelos controles Touch do
  Meta Quest 3S**.

## Como instalar

Requisitos: Android 10 ou superior (64 bits, Snapdragon) **ou** Meta Quest
3S/3/2/Pro.

1. **Instale o APK** — envie o arquivo para o celular/Quest e abra para
   instalar. Pelo PC: `adb install -r NFSMW-....apk`
2. **Copie o jogo** para o armazenamento **interno** do aparelho — a **`.iso`**
   ou a pasta com o **`.xex`** extraído. (Uma `.iso` tem cerca de 7,8 GB; um
   microSD FAT32 não aceita arquivos acima de 4 GB.)
3. Abra o app, toque em **ESCOLHER ISO…** (ou **ESCOLHER PASTA…**), **navegue
   até a pasta** onde está o jogo e **selecione** o arquivo (ou a pasta).
4. Toque em **JOGAR**.

No jogo, use os controles de toque na tela ou conecte um controle (gamepad).

## As duas versões do release

| Arquivo | O que é |
|---|---|
| `NFSMW-v0.4.1-pal-ptbr.apk` | Versão padrão: **fix do save**. Roda em Android e no Quest. |
| `NFSMW-v0.4.1-pal-ptbr-quest-gamepad.apk` | **Fix do save + gamepad emulado** pelos controles do Meta Quest 3S. |

> ⚠️ **A versão com gamepad não pode ser instalada junto do app que ela
> personifica.** Para o Quest ativar o modo gamepad, o app adota o identificador
> `com.xbox.pwa` (um dos nomes que o sistema já aceita para esse modo). Se você
> tem o app oficial que usa esse mesmo identificador, um substitui o outro —
> instale apenas um.

## Créditos

Obrigado a todo mundo cujo trabalho este projeto usa.

**O projeto original**

- [victorgbd/NFSMW-Recompiled-Mobile](https://github.com/victorgbd/NFSMW-Recompiled-Mobile) —
  o fork upstream de onde este repositório parte (port Android, motor nativo e
  as ferramentas de edição já prontas)
- [madelrandel-blip/NFSMW-Recompiled](https://github.com/madelrandel-blip/NFSMW-Recompiled) —
  a recompilação na qual os dois forks se baseiam

**A base**

- [ReXGlue SDK](https://github.com/rexglue/rexglue-sdk) — o runtime
- [XenonRecomp](https://github.com/hedge-dev/XenonRecomp) — a abordagem de
  recompilação estática
- [Xenia](https://xenia.jp/) — a emulação de kernel e GPU da qual o ReXGlue vem
- [hells-gate-recomp-android](https://github.com/deivid22srk/hells-gate-recomp-android)
  por deivid22srk — o patch Android do ReXGlue SDK (sem licença declarada, por
  isso não é incluído aqui: é baixado na hora do build)
- [libadrenotools](https://github.com/bylaws/libadrenotools) por Billy Laws —
  drivers Turnip em GPUs Adreno
- [Mesa / Turnip](https://docs.mesa3d.org/drivers/freedreno.html) — o driver
  Vulkan Adreno de código aberto
- [codepdbh/nfsmw-android](https://github.com/codepdbh/nfsmw-android) — o port
  Android do nfsmw-nx; o motor nativo opcional é compilado contra a árvore dele
- [StevensND/nfsmw-nx](https://github.com/StevensND/nfsmw-nx) — o port de
  Nintendo Switch deste mesmo projeto
- As bibliotecas que o SDK traz, cada uma sob sua licença: FFmpeg (áudio XMA),
  SDL3, glslang, SPIRV-Tools, Dear ImGui, spdlog, fmt, xxHash, Vulkan Memory
  Allocator e toml++, entre outras

## Aviso legal

A recompilação estática de um jogo que você possui, para uso próprio, está no
mesmo terreno da emulação: o código traduzido deriva de um binário que você
comprou.

**Nunca** devem ser distribuídos: o `default.xex` (ou qualquer arquivo do jogo),
o C++ que o gerador produz a partir dele, ou builds do jogo como um download
completo. Os APKs publicados aqui são uma conveniência para sideload nos seus
próprios aparelhos, a partir de um disco que você possui — sem a sua cópia a
edição PAL España/PT-BR, eles não rodam nada.

Este projeto está sob a **GNU General Public License v3.0** — veja
[LICENSE](LICENSE). O ReXGlue SDK é BSD 3-Clause, um trabalho à parte com seus
próprios termos.

Need for Speed e Most Wanted são marcas da Electronic Arts Inc. Este projeto
não é afiliado, endossado nem conectado à Electronic Arts de nenhuma forma.
