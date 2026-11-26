# Gym Slut

Aplicativo Android **offline-first** em Kotlin e Jetpack Compose para planejamento de divisões de treino, cronograma semanal, catálogo de aparelhos/exercícios, execução na academia com timer de descanso e histórico de cargas.

## Funcionalidades do MVP (v1)

- **Catálogo de Aparelhos e Exercícios (PT-BR)**: banco local pré-populado com busca, filtros por grupo muscular/aparelho e CRUD de itens customizados.
- **Múltiplos Planos e Fichas**: crie várias divisões (ex.: `ABC`, `Upper/Lower`), mantenha 1 plano ativo e configure séries, repetições e tempo de descanso por exercício.
- **Cronograma Semanal + Tela Hoje**: vincule treinos ou dias de descanso de segunda a domingo, mantendo liberdade para iniciar qualquer outro treino do plano no dia.
- **Execução de Treino Ativo**: registro inline de carga (`kg`) e repetições por série com sugestão da última sessão concluída.
- **Timer de Descanso Automático**: inicia ao concluir cada série, com vibração, notificação ao terminar e controles de `-15s`, `+15s`, pausa e pular.
- **Histórico Objetivo**: registro das sessões concluídas e consulta rápida das últimas cargas por exercício.
- **Backup em JSON e Tema**: exportação/restauração atômica em JSON (`schemaVersion = 1`) e tema escuro padrão (com opção claro/sistema).

## Stack Técnica

- **Linguagem**: Kotlin
- **UI**: Jetpack Compose + Material 3 + Navigation Compose
- **Arquitetura**: MVVM + Repositories + Hilt
- **Persistência**: Room (KSP) + DataStore Preferences
- **Serialização**: `kotlinx.serialization`
- **Compatibilidade**: Android 8.0+ (`minSdk = 26`)

Para detalhes completos de arquitetura, modelo de dados e roadmap, consulte [docs/SPEC.md](docs/SPEC.md).

## Pré-requisitos Locais (CLI sem Android Studio)

- **JDK 21** (`javac 21`)
- **Android SDK Command Line Tools** (`ANDROID_HOME` apontando para o SDK com `platform-tools`, `platforms` e `build-tools`)
- **Gradle Wrapper** (`./gradlew`, incluído a partir da issue `#1`)

## Comandos de Build e Validação

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
```

## CI/CD e Releases

- **CI**: todo push/PR para `main` executa testes unitários, Android Lint, build Debug e verificação Release com R8 em [.github/workflows/android-ci.yml](.github/workflows/android-ci.yml).
- **CD**: tags SemVer (`vX.Y.Z`, ex.: `v1.0.0` ou `v1.0.0-rc.1`) disparam [.github/workflows/android-release.yml](.github/workflows/android-release.yml), gerando APK assinado com R8, checksum `.sha256` e GitHub Release automática.

Consulte o passo a passo de keystore, secrets e publicação em [docs/RELEASE.md](docs/RELEASE.md).

## Fluxo de Desenvolvimento por Issues

O backlog do MVP é gerenciado via issues atômicas no GitHub (`1` issue por branch `issue/<numero>-<slug>` e PR para `main`). Para atualizar o cache local de issues abertas em `./issues/`:

```bash
./scripts/sync-issues.sh
```
