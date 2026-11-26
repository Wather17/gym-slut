# Especificação Técnica e de Produto — Gym Slut (MVP v1)

## 1. Visão Geral e Objetivos do MVP v1

**Gym Slut** é um aplicativo Android offline-first em Português (PT-BR) voltado para planejamento de treinos de musculação, controle de cronograma semanal, execução rápida na academia e histórico objetivo de cargas.

### Objetivos do MVP v1
1. **Catálogo de aparelhos e exercícios**: banco local pré-populado em PT-BR com aparelhos/equipamentos e exercícios vinculados, permitindo busca, filtros e CRUD completo de itens customizados.
2. **Planos de treino e fichas**: suporte a múltiplos planos salvos com exatamente 1 plano ativo por vez, contendo treinos (A, B, C...) e prescrição minimalista por exercício.
3. **Cronograma semanal com flexibilidade**: associação fixa de dias da semana (`Seg`–`Dom`) a um treino do plano ativo ou `Descanso`, permitindo iniciar qualquer outro treino do plano na tela **Hoje** para compensar faltas.
4. **Execução ágil na academia**: registro de carga (`kg`) e repetições reais série a série, pré-preenchidos com os valores da sessão concluída mais recente daquele exercício, além de timer de descanso automático por exercício com vibração, notificação e controles rápidos.
5. **Histórico de sessões e cargas**: listagem cronológica de sessões concluídas, detalhe da sessão e histórico de cargas por exercício usando snapshots imutáveis.
6. **Persistência offline e backup portátil**: armazenamento 100% local em Room/SQLite com exportação e restauração atômica em arquivo JSON versionado (`schemaVersion = 1`) e seleção de tema (`Escuro` padrão, `Claro`, `Sistema`).

### Não-Escopo explícito do MVP v1
- Login, cadastro de conta, servidor ou sincronização em nuvem (Firebase/Supabase/API remota).
- Gráficos de evolução, estimativa de 1RM ou badges de recorde pessoal (PR).
- Tipos avançados de série na prescrição da ficha (aquecimento, drop-set, bi-set, falha) ou notas técnicas na prescrição.
- Cronograma por ciclo rotativo (`A -> B -> C` independente do dia da semana) na v1.
- Integrações com wearables, Health Connect ou importação de terceiros.

---

## 2. Decisões de Produto

- **Identidade e Idioma**: nome do app `Gym Slut`, `applicationId = "com.gymslut.app"`, pacote raiz `com.gymslut.app`, interface 100% em PT-BR.
- **Prescrição Minimalista**: cada exercício adicionado a um treino da ficha define apenas `targetSets` (número fixo de séries >= 1), `targetReps` (número fixo de repetições >= 1) e `restSeconds` (descanso padrão >= 0). Durante a execução, cada série registra `weightKg` e `reps` reais, permitindo adicionar ou remover séries extras.
- **Múltiplos Planos com 1 Ativo**: o usuário pode manter várias divisões salvas (ex.: `ABC`, `Upper/Lower`) e alternar o plano ativo em transação atômica sem perder as fichas antigas.
- **Cronograma Seg–Dom + Liberdade na Tela Hoje**: cada plano mapeia os 7 dias da semana (`1 = Segunda` a `7 = Domingo` segundo ISO-8601) para um `WorkoutTemplate` ou `Descanso` (`null`). Na tela **Hoje**, o treino agendado para o dia atual aparece em destaque, mas o usuário pode iniciar qualquer outro treino do plano ativo.
- **Sugestão da Última Sessão**: ao iniciar um treino, cada série recebe como sugestão `weightKg` e `reps` do último registro concluído daquele `exerciseId` (ou `0.0 kg` e `targetReps` quando não houver histórico prévio).
- **Timer de Descanso Automático**: ao marcar uma série como concluída (e `restSeconds > 0`), o timer inicia automaticamente com o tempo do exercício, usa timestamp alvo em relógio de parede (mantendo contagem precisa se o app for para segundo plano), oferece controles `-15s`, `+15s`, pausar/retomar e pular, e dispara vibração + notificação ao zerar.
- **Imutabilidade do Histórico**: `workout_sessions` e `session_sets` gravam snapshots de nome do treino, nome do exercício e nome do aparelho para que renomeações ou exclusões futuras no catálogo/fichas não corrompam o histórico.

---

## 3. Stack Técnica e Requisitos de Build

- **Linguagem**: Kotlin (JVM 21 / Kotlin Gradle Plugin).
- **UI**: Jetpack Compose com Material 3 (tema escuro como padrão, com opção `Escuro`, `Claro` e `Sistema` via DataStore Preferences) e Navigation Compose.
- **Arquitetura**: MVVM unidirecional (`StateFlow` de UI + eventos) com camada de repositórios.
- **Injeção de Dependência**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`).
- **Persistência Local**: Room + KSP (`GymSlutDatabase`) e DataStore Preferences.
- **Serialização**: `kotlinx.serialization` (`kotlinx-serialization-json`) para backup/restauração em JSON (`schemaVersion = 1`).
- **Compatibilidade Android**: `minSdk = 26` (Android 8.0+), `compileSdk` / `targetSdk` alinhados ao SDK instalado (`36`/`37` conforme plataforma estável configurada).
- **Tooling CLI**: Gradle Wrapper (`./gradlew`), OpenJDK 21 (`javac 21`) e Android SDK Command Line Tools (`sdkmanager`, `adb`), sem dependência de Android Studio.

---

## 4. Arquitetura e Estrutura de Pacotes (`com.gymslut.app`)

```text
com.gymslut.app
├── GymSlutApplication.kt
├── MainActivity.kt
├── core
│   ├── designsystem        # GymSlutTheme, Color, Typography, componentes comuns
│   ├── navigation          # Rotas tipadas, BottomNavBar (4 abas) e NavHost
│   ├── preferences         # ThemeMode (DARK, LIGHT, SYSTEM) e PreferencesRepository (DataStore)
│   └── notification        # Canal e helper de notificação + vibração do timer
├── data
│   ├── local
│   │   ├── GymSlutDatabase.kt
│   │   ├── Converters.kt
│   │   ├── CatalogSeedData.kt
│   │   ├── entity/         # 8 Room entities + enums de domínio (MuscleGroup, EquipmentCategory)
│   │   └── dao/            # EquipmentDao, ExerciseDao, PlanDao, ScheduleDao, SessionDao, BackupDao
│   ├── model/              # Modelos de domínio, projeções/relações e BackupPayload
│   └── repository/         # Implementações de repositório + BackupManager
├── di
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
└── feature
    ├── today               # Tela Hoje (aba 1)
    ├── plans               # Planos, detalhe do plano, editor de treino e cronograma (aba 2)
    ├── catalog             # Catálogo de aparelhos e exercícios com filtros e CRUD (aba 3)
    ├── workout             # Execução de treino ativo e controller do timer de descanso
    ├── history             # Histórico de sessões, detalhe da sessão e histórico por exercício (aba 4)
    └── settings            # Configurações de tema e exportar/importar backup JSON
```

---

## 5. Modelo de Dados Room

Todas as entidades ficam em `com.gymslut.app.data.local.entity`.

### Enums persistidos via `Converters`
- `EquipmentCategory`: `MACHINE`, `FREE_WEIGHT`, `CABLE`, `BODYWEIGHT`, `BENCH_RACK`, `OTHER`
- `MuscleGroup`: `CHEST`, `BACK`, `SHOULDERS`, `BICEPS`, `TRICEPS`, `QUADS`, `HAMSTRINGS`, `GLUTES`, `CALVES`, `CORE`, `FULL_BODY`

### Entidades (8 tabelas)

1. **`equipment`** (`EquipmentEntity`)
   - `id: Long` (PK, autoGenerate)
   - `name: String` (índice `unique = true`)
   - `category: EquipmentCategory`
   - `notes: String` (default `""`)
   - `isCustom: Boolean` (default `false`)

2. **`exercises`** (`ExerciseEntity`)
   - `id: Long` (PK, autoGenerate)
   - `name: String`
   - `primaryMuscle: MuscleGroup`
   - `secondaryMuscle: MuscleGroup?`
   - `equipmentId: Long` (FK -> `equipment.id`, `ON DELETE RESTRICT`, indexado)
   - `notes: String` (default `""`)
   - `isCustom: Boolean` (default `false`)

3. **`training_plans`** (`TrainingPlanEntity`)
   - `id: Long` (PK, autoGenerate)
   - `name: String`
   - `isActive: Boolean` (default `false`)
   - `createdAtEpochMs: Long`
   - *Regra de negócio*: ao ativar um plano, a operação em transação desativa todos os demais e ativa apenas o escolhido.

4. **`workout_templates`** (`WorkoutTemplateEntity`)
   - `id: Long` (PK, autoGenerate)
   - `planId: Long` (FK -> `training_plans.id`, `ON DELETE CASCADE`, indexado)
   - `name: String` (ex.: `"Treino A — Peito e Tríceps"`)
   - `orderIndex: Int`

5. **`workout_exercises`** (`WorkoutExerciseEntity`)
   - `id: Long` (PK, autoGenerate)
   - `workoutTemplateId: Long` (FK -> `workout_templates.id`, `ON DELETE CASCADE`, indexado)
   - `exerciseId: Long` (FK -> `exercises.id`, `ON DELETE RESTRICT`, indexado)
   - `orderIndex: Int`
   - `targetSets: Int` (>= 1)
   - `targetReps: Int` (>= 1)
   - `restSeconds: Int` (>= 0)

6. **`weekly_schedule`** (`WeeklyScheduleEntity`)
   - Chave primária composta: `(planId, dayOfWeek)` (`dayOfWeek` entre `1` e `7`, ISO-8601 Segunda=1 .. Domingo=7)
   - `planId: Long` (FK -> `training_plans.id`, `ON DELETE CASCADE`)
   - `dayOfWeek: Int`
   - `workoutTemplateId: Long?` (FK -> `workout_templates.id`, `ON DELETE SET NULL`, indexado; `null` representa `Descanso`)

7. **`workout_sessions`** (`WorkoutSessionEntity`)
   - `id: Long` (PK, autoGenerate)
   - `planId: Long?` (FK -> `training_plans.id`, `ON DELETE SET NULL`, indexado)
   - `workoutTemplateId: Long?` (FK -> `workout_templates.id`, `ON DELETE SET NULL`, indexado)
   - `workoutNameSnapshot: String`
   - `startedAtEpochMs: Long`
   - `finishedAtEpochMs: Long?` (`null` enquanto a sessão está em andamento)

8. **`session_sets`** (`SessionSetEntity`)
   - `id: Long` (PK, autoGenerate)
   - `sessionId: Long` (FK -> `workout_sessions.id`, `ON DELETE CASCADE`, indexado)
   - `exerciseId: Long?` (FK -> `exercises.id`, `ON DELETE SET NULL`, indexado)
   - `exerciseNameSnapshot: String`
   - `equipmentNameSnapshot: String`
   - `exerciseOrderIndex: Int`
   - `setNumber: Int`
   - `weightKg: Double`
   - `reps: Int`
   - `completed: Boolean`
   - `completedAtEpochMs: Long?`

---

## 6. Navegação e Fluxos de Tela

### Barra de Navegação Inferior (4 abas principais)
1. **`Hoje`** (`TodayScreen`):
   - Cabeçalho com nome do plano ativo e ícone de engrenagem para `Settings`.
   - Faixa de 7 dias (`Seg`–`Dom`) destacando o dia atual.
   - Banner de retomada caso exista uma `WorkoutSession` em andamento (`finishedAtEpochMs == null`).
   - Card principal do treino agendado para hoje (ou estado de `Descanso`) com lista resumida dos exercícios (`séries x reps`) e botão **Iniciar treino**.
   - Seletor secundário **Escolher outro treino** listando todos os treinos do plano ativo para compensar faltas.
   - Estado vazio orientando a criar/ativar um plano quando não houver plano ativo.
2. **`Planos`** (`PlansScreen`):
   - Lista de planos salvos com indicação do plano ativo, ações para criar, renomear, excluir e ativar.
   - `PlanDetailScreen`: gerencia treinos (`WorkoutTemplate`) do plano (criar, renomear, reordenar, excluir) e o cronograma semanal (`Seg`–`Dom` -> treino ou `Descanso`).
   - `WorkoutEditorScreen`: seleciona exercícios do catálogo e configura `targetSets`, `targetReps`, `restSeconds` e ordem na ficha.
3. **`Catálogo`** (`CatalogScreen`):
   - Segmentos **Exercícios** e **Aparelhos**.
   - Busca por texto e filtros por `MuscleGroup` e `Equipment`.
   - CRUD de aparelhos e exercícios customizados com proteção amigável contra exclusão de itens referenciados.
4. **`Histórico`** (`HistoryScreen`):
   - Lista reversa cronológica das sessões concluídas (`finishedAtEpochMs != null`) com data, nome do treino, duração, séries concluídas e volume total (`kg`).
   - `SessionDetailScreen`: detalhe completo dos exercícios e séries executadas.
   - `ExerciseHistoryScreen`: histórico de datas, séries, cargas e repetições para um exercício específico.

### Rotas Secundárias (fora da barra inferior)
- **`ActiveWorkoutScreen`**: execução do treino ativo (edição inline de `weightKg` e `reps`, marcação de série concluída, adicionar/remover série extra, barra persistente do timer de descanso com `-15s`, `+15s`, pausar/retomar e pular, finalizar ou descartar sessão).
- **`SettingsScreen`**: seleção de tema (`Escuro` padrão, `Claro`, `Sistema`) e exportação/importação de backup JSON via Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`).

---

## 7. Schema de Backup JSON (`schemaVersion = 1`)

Estrutura serializável via `kotlinx.serialization`:

```kotlin
@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAtEpochMs: Long,
    val equipment: List<EquipmentBackupDto>,
    val exercises: List<ExerciseBackupDto>,
    val trainingPlans: List<TrainingPlanBackupDto>,
    val workoutTemplates: List<WorkoutTemplateBackupDto>,
    val workoutExercises: List<WorkoutExerciseBackupDto>,
    val weeklySchedule: List<WeeklyScheduleBackupDto>,
    val workoutSessions: List<WorkoutSessionBackupDto>,
    val sessionSets: List<SessionSetBackupDto>
)
```

### Regras de Exportação e Restauração
- **Exportação**: lê todas as 8 tabelas em transação de leitura consistente e grava JSON formatado no URI selecionado pelo usuário.
- **Importação**:
  1. Faz parse do JSON e valida `schemaVersion == 1`.
  2. Valida integridade referencial antes de tocar no banco (FKs entre exercícios -> aparelhos, templates -> planos, workout_exercises -> templates/exercícios, weekly_schedule -> planos/templates e `dayOfWeek in 1..7`, session_sets -> sessões, e garantia de no máximo 1 plano ativo).
  3. Executa substituição atômica em uma única transação Room (`@Transaction`): limpa as tabelas em ordem segura e insere todos os registros do backup.
  4. Se qualquer validação falhar, aborta sem modificar os dados existentes e exibe mensagem de erro clara em PT-BR.

---

## 8. Roadmap do MVP (Backlog no GitHub)

| Ordem | Issue | Título resumido | Prioridade | Dependências |
| :---: | :---: | :--- | :---: | :--- |
| 1 | `#1` | Scaffold inicial do projeto Android (Kotlin, Compose, Room, Hilt) e CI | `critical` | Nenhuma |
| 2 | `#2` | Camada de dados Room, entidades, DAOs e seed inicial do catálogo em PT-BR | `critical` | `#1` |
| 3 | `#3` | Catálogo de aparelhos e exercícios (listagem, filtros e CRUD customizado) | `high` | `#2` |
| 4 | `#4` | Gerenciamento de planos de treino, fichas (treinos A/B/C) e prescrição minimalista | `high` | `#2`, `#3` |
| 5 | `#5` | Cronograma semanal (Seg–Dom) e tela Hoje com liberdade de escolha | `high` | `#4` |
| 6 | `#6` | Execução de treino ativo com sugestão da última carga e registro de séries | `high` | `#5` |
| 7 | `#7` | Timer de descanso automático com vibração, notificação e controles rápidos | `high` | `#6` |
| 8 | `#8` | Histórico de sessões concluídas e últimas cargas por exercício | `medium` | `#6` |
| 9 | `#9` | Configurações (tema claro/escuro/sistema) e backup/restauração em JSON | `medium` | `#2`, `#8` |
