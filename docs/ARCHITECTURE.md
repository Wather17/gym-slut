# Arquitetura — Gym Slut

Este documento descreve a arquitetura implementada no **Gym Slut** (Android offline-first em Kotlin + Jetpack Compose).

---

## 1. Visão Geral

O aplicativo segue **MVVM unidirecional (UDF)** com camada de repositórios sobre **Room** e **DataStore Preferences**, injetados via **Hilt**:

```text
Jetpack Compose Screens (UI)
        │  observa StateFlow<UiState> / envia eventos
        ▼
   ViewModels (@HiltViewModel)
        │  chama casos de uso / operações suspend e Flow
        ▼
  Repositories (Catalog, Plan, Session, Backup, Preferences)
        │
        ├──► Room Database (gym_slut.db — DAOs + Entities)
        └──► DataStore Preferences (preferências de tema)
```

---

## 2. Estrutura de Pacotes (`com.gymslut.app`)

```text
com.gymslut.app
├── core
│   ├── designsystem   # Tema Material 3 (escuro padrão, claro, tipografia e cores)
│   ├── navigation     # Rotas e NavHost da navegação principal e fluxos secundários
│   ├── notification   # Notificação e vibração do timer de descanso
│   ├── preferences    # ThemeMode e DataStorePreferencesRepository
│   └── time           # CurrentDateProvider e rótulos de dias da semana (Seg–Dom)
├── data
│   ├── local
│   │   ├── dao        # EquipmentDao, ExerciseDao, PlanDao, ScheduleDao, SessionDao, BackupDao
│   │   ├── entity     # 8 entidades Room + Enums (EquipmentCategory, MuscleGroup)
│   │   ├── CatalogSeedData.kt   # Seed idempotente versionado (SEED_VERSION = 2)
│   │   ├── Converters.kt        # TypeConverters para enums
│   │   └── GymSlutDatabase.kt   # @Database(version = 1, exportSchema = true)
│   ├── model          # DTOs de leitura, exceções de domínio e BackupPayload (JSON)
│   └── repository     # Implementações Room*Repository e BackupManager
├── di                 # DatabaseModule, RepositoryModule, AppModule (Hilt)
└── feature
    ├── today          # Tela Hoje (treino agendado do dia + acesso rápido a outros treinos)
    ├── plans          # Planos, fichas de treino, editor de ficha e cronograma semanal
    ├── catalog        # Catálogo de aparelhos e exercícios com busca, filtros e CRUD
    ├── workout        # Execução de treino ativo, sugestão de carga e RestTimerController
    ├── history        # Histórico de sessões concluídas, detalhes e evolução por exercício
    └── settings       # Preferência de tema e exportação/restauração de backup JSON
```

---

## 3. Persistência e Integridade de Dados

### Banco Room (`gym_slut.db`, `version = 1`)
- **Entidades:** `EquipmentEntity`, `ExerciseEntity`, `TrainingPlanEntity`, `WorkoutTemplateEntity`, `WorkoutExerciseEntity`, `WeeklyScheduleEntity`, `WorkoutSessionEntity` e `SessionSetEntity`.
- **Snapshots históricos:** `WorkoutSessionEntity` e `SessionSetEntity` armazenam snapshots de nome do treino, exercício e aparelho no momento da execução, preservando o histórico mesmo se planos ou fichas forem renomeados ou removidos.
- **Regras de integridade:** exclusão de aparelhos ou exercícios em uso lança `ResourceInUseException`; nomes duplicados (case-insensitive) lançam `DuplicateResourceException`.
- **Schemas exportados:** KSP exporta os JSONs de schema em `app/schemas/` para rastreamento seguro de futuras migrações.

### Seed Versionado em Upgrades
- `CatalogSeedData.seedIfNeeded(...)` é **idempotente** (verifica itens existentes por nome normalizado).
- Em `DatabaseModule`, o callback `RoomDatabase.Callback.onOpen` compara `catalog_seed_version` salvo em `SharedPreferences` com `CatalogSeedData.SEED_VERSION`.
- Em instalações limpas ou ao atualizar de uma versão anterior com novo `SEED_VERSION`, apenas os aparelhos e exercícios ausentes são inseridos, executando uma única vez por versão sem re-inserir itens que o usuário tenha removido depois.

### Backup e Restauração JSON (`schemaVersion = 1`)
- Serializado com `kotlinx.serialization` (`BackupPayload`).
- A restauração valida `schemaVersion`, integridade referencial de chaves estrangeiras e substitui os dados em uma **transação atômica** no Room (`BackupDao`), evitando estado parcial em caso de arquivo corrompido ou incompatível.

---

## 4. Builds e Identidade

| Build Type | `applicationId` | Nome na Launcher | R8 / Shrink |
| :--- | :--- | :--- | :--- |
| `debug` | `com.gymslut.app.debug` | `Gym Slut (Debug)` | Desativado |
| `release` | `com.gymslut.app` | `Gym Slut` | Ativado (`isMinifyEnabled = true`, `isShrinkResources = true`) |

O `namespace` permanece `com.gymslut.app` em ambos os builds, permitindo coexistência de `debug` e `release` no mesmo dispositivo sem alterar pacotes Kotlin ou regras de ProGuard.

---

## 5. Estratégia de Testes e Qualidade

- **Testes unitários e de integração local:** JUnit 4, `kotlinx-coroutines-test` e **Robolectric** (`@Config(sdk = [35])`) testando DAOs/Room em memória, repositórios, ViewModels, `BackupManager`, `RestTimerController`, notificações e DataStore.
- **Cobertura de código (Kover):** plugin JetBrains Kover configurado em `app/build.gradle.kts` com gate obrigatório `minBound(90)` para cobertura de linhas (excluindo classes geradas pelo Android/Hilt/Room/Compose), validado automaticamente no CI (`koverVerifyDebug`).
- **Lint e R8:** `lintDebug` e `assembleRelease` (com R8) rodam em todo PR para detectar regressões de recursos, regras de ProGuard e compatibilidade.
