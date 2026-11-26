# Gym Slut

[![Android CI](https://github.com/Wather17/gym-slut/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Wather17/gym-slut/actions/workflows/android-ci.yml)
[![Latest Release](https://img.shields.io/github/v/release/Wather17/gym-slut?label=release)](https://github.com/Wather17/gym-slut/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Aplicativo Android **offline-first** em Kotlin e Jetpack Compose para planejamento de divisões de treino, cronograma semanal, catálogo de aparelhos/exercícios, execução na academia com timer de descanso e histórico objetivo de cargas — 100% em PT-BR, sem login e sem anúncios.

---

## Download e Instalação (APK Assinado)

Baixe o APK oficial assinado diretamente na página de releases do GitHub:

- **[Baixar última versão (GitHub Releases)](https://github.com/Wather17/gym-slut/releases/latest)**

> **Atualizações preservam seus dados:** todas as releases oficiais usam a mesma chave de assinatura (`com.gymslut.app`), então basta instalar o novo APK por cima da versão anterior sem desinstalar. O build `debug` usa `com.gymslut.app.debug` (**Gym Slut (Debug)**) e pode coexistir separadamente no mesmo aparelho.

---

## Funcionalidades

- **Catálogo de Aparelhos e Exercícios (PT-BR)**: banco local pré-populado com **33 aparelhos** e **65 exercícios** (máquinas guiadas, polias, pesos livres, bancos e peso corporal), busca em tempo real, filtros por grupo muscular/aparelho e CRUD completo de itens customizados.
- **Múltiplos Planos e Fichas**: crie várias divisões (ex.: `ABC`, `Push/Pull/Legs`, `Upper/Lower`), mantenha 1 plano ativo e configure séries, repetições e tempo de descanso por exercício com reordenação.
- **Cronograma Semanal + Tela Hoje**: vincule treinos ou dias de descanso de segunda a domingo, mantendo liberdade total para iniciar qualquer outro treino do plano no dia.
- **Execução de Treino Ativo**: registro inline de carga (`kg`) e repetições por série com sugestão automática da última sessão concluída, séries extras e persistência imediata.
- **Timer de Descanso Automático**: inicia ao concluir cada série, com vibração, notificação ao finalizar e controles rápidos de `-15s`, `+15s`, pausar/retomar e pular.
- **Histórico Objetivo**: lista de sessões concluídas com detalhes por série e tela dedicada de evolução/últimas cargas por exercício (preservando snapshots históricos mesmo se um plano for apagado).
- **Tema e Backup em JSON**: tema escuro padrão (com alternância para claro ou seguir o sistema) e exportação/restauração atômica em JSON (`schemaVersion = 1`).

---

## Stack Técnica

- **Linguagem**: Kotlin (JVM 21)
- **UI**: Jetpack Compose + Material 3 + Navigation Compose + Adaptive Icon (inclui monocromático Android 13+)
- **Arquitetura**: MVVM + Repositories + Hilt (DI)
- **Persistência**: Room (`version = 1`, KSP, `exportSchema = true`) + DataStore Preferences
- **Serialização**: `kotlinx.serialization`
- **Qualidade**: JUnit 4 + Coroutines Test + Robolectric + JetBrains Kover (gate mínimo de **90%** de cobertura de linhas no CI)
- **Compatibilidade**: Android 8.0+ (`minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`)

---

## Documentação

| Documento | Descrição |
| :--- | :--- |
| [docs/USER_GUIDE.md](docs/USER_GUIDE.md) | Guia prático de uso do aplicativo no dia a dia |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Arquitetura implementada, pacotes, Room, seed versionado e estratégia de testes |
| [docs/RELEASE.md](docs/RELEASE.md) | Configuração de keystore, GitHub Secrets, tags SemVer e verificação de APK |
| [docs/SPEC.md](docs/SPEC.md) | Especificação original de produto e requisitos do MVP |

---

## Desenvolvimento Local (CLI sem Android Studio)

### Pré-requisitos
- **JDK 21** (`javac 21`)
- **Android SDK Command Line Tools** com `ANDROID_HOME` configurado (`platform-tools`, `platforms;android-36`, `build-tools;35.0.0`)

### Comandos principais

```bash
# Testes unitários (JUnit + Robolectric)
./gradlew testDebugUnitTest

# Cobertura de código com Kover (gate >= 90%) e relatório HTML
./gradlew koverVerifyDebug koverLogDebug koverHtmlReportDebug

# Android Lint
./gradlew lintDebug

# Builds Debug (com.gymslut.app.debug) e Release com R8 (com.gymslut.app)
./gradlew assembleDebug
./gradlew assembleRelease
```

---

## CI/CD e Fluxo de Issues

- **CI** ([android-ci.yml](.github/workflows/android-ci.yml)): executa `testDebugUnitTest`, `koverVerifyDebug`, `lintDebug`, `assembleDebug` e `assembleRelease` (R8) em todo push/PR para `main`.
- **CD** ([android-release.yml](.github/workflows/android-release.yml)): tags SemVer (`vX.Y.Z` ou `vX.Y.Z-rc.N`) geram APK assinado com R8, verificam a assinatura com `apksigner`, calculam `.sha256` e publicam a GitHub Release automaticamente.
- **Issues**: o backlog é gerenciado via issues atômicas no GitHub (`1` issue por branch `issue/<numero>-<slug>` e PR para `main`). Para sincronizar o cache local em `./issues/`:

```bash
./scripts/sync-issues.sh
```

---

## Licença

Distribuído sob a licença **MIT**. Consulte [LICENSE](LICENSE) para mais detalhes.
