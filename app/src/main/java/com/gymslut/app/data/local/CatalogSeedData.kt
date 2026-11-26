package com.gymslut.app.data.local

import android.database.sqlite.SQLiteConstraintException
import com.gymslut.app.data.local.dao.EquipmentDao
import com.gymslut.app.data.local.dao.ExerciseDao
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.EquipmentEntity
import com.gymslut.app.data.local.entity.ExerciseEntity
import com.gymslut.app.data.local.entity.MuscleGroup

/**
 * Catálogo inicial pré-populado em PT-BR com aparelhos e exercícios comuns de academia.
 */
object CatalogSeedData {

    const val SEED_VERSION: Int = 2

    data class SeedEquipment(
        val name: String,
        val category: EquipmentCategory,
        val notes: String = "",
        val introducedInVersion: Int = 1
    )

    data class SeedExercise(
        val name: String,
        val primaryMuscle: MuscleGroup,
        val secondaryMuscle: MuscleGroup? = null,
        val equipmentName: String,
        val notes: String = "",
        val introducedInVersion: Int = 1
    )

    val defaultEquipment: List<SeedEquipment> = listOf(
        SeedEquipment("Supino Reto", EquipmentCategory.BENCH_RACK),
        SeedEquipment("Supino Inclinado", EquipmentCategory.BENCH_RACK),
        SeedEquipment("Supino Máquina", EquipmentCategory.MACHINE, "Pegadores na linha do meio do peito", introducedInVersion = 2),
        SeedEquipment("Supino Inclinado Máquina", EquipmentCategory.MACHINE, introducedInVersion = 2),
        SeedEquipment("Polia Alta", EquipmentCategory.CABLE),
        SeedEquipment("Polia Baixa", EquipmentCategory.CABLE),
        SeedEquipment("Cross Over", EquipmentCategory.CABLE),
        SeedEquipment("Leg Press 45°", EquipmentCategory.MACHINE),
        SeedEquipment("Leg Press Horizontal", EquipmentCategory.MACHINE, "Não tire a lombar do banco e não trave o joelho", introducedInVersion = 2),
        SeedEquipment("Cadeira Extensora", EquipmentCategory.MACHINE),
        SeedEquipment("Mesa Flexora", EquipmentCategory.MACHINE),
        SeedEquipment("Cadeira Flexora", EquipmentCategory.MACHINE),
        SeedEquipment("Hack Machine", EquipmentCategory.MACHINE),
        SeedEquipment("Smith Machine", EquipmentCategory.MACHINE),
        SeedEquipment("Peck Deck", EquipmentCategory.MACHINE),
        SeedEquipment("Crucifixo Inverso Máquina", EquipmentCategory.MACHINE, "Banco ajustado com braços na linha dos ombros", introducedInVersion = 2),
        SeedEquipment("Desenvolvimento Máquina", EquipmentCategory.MACHINE, "Lombar apoiada no encosto do início ao fim", introducedInVersion = 2),
        SeedEquipment("Elevação Lateral Máquina", EquipmentCategory.MACHINE, "Cotovelos apoiados, subindo até a linha do ombro", introducedInVersion = 2),
        SeedEquipment("Remada Máquina", EquipmentCategory.MACHINE, "Apoie o peito e puxe juntando as escápulas atrás", introducedInVersion = 2),
        SeedEquipment("Tríceps Máquina", EquipmentCategory.MACHINE, introducedInVersion = 2),
        SeedEquipment("Cadeira Abdutora", EquipmentCategory.MACHINE),
        SeedEquipment("Cadeira Adutora", EquipmentCategory.MACHINE),
        SeedEquipment("Panturrilha em Pé", EquipmentCategory.MACHINE),
        SeedEquipment("Abdominal Máquina", EquipmentCategory.MACHINE, "Curve o tronco pelo abdômen, não pelo pescoço", introducedInVersion = 2),
        SeedEquipment("Esteira", EquipmentCategory.MACHINE, "Cardio leve/moderado (caminhada firme ou trote leve)", introducedInVersion = 2),
        SeedEquipment("Barra Reta", EquipmentCategory.FREE_WEIGHT),
        SeedEquipment("Barra W", EquipmentCategory.FREE_WEIGHT),
        SeedEquipment("Halteres", EquipmentCategory.FREE_WEIGHT),
        SeedEquipment("Banco Regulável", EquipmentCategory.BENCH_RACK),
        SeedEquipment("Banco Scott", EquipmentCategory.BENCH_RACK),
        SeedEquipment("Barra Fixa", EquipmentCategory.BODYWEIGHT),
        SeedEquipment("Paralelas", EquipmentCategory.BODYWEIGHT),
        SeedEquipment("Peso Corporal", EquipmentCategory.BODYWEIGHT)
    )

    val defaultExercises: List<SeedExercise> = listOf(
        // Peito
        SeedExercise("Supino Reto com Barra", MuscleGroup.CHEST, MuscleGroup.TRICEPS, "Supino Reto"),
        SeedExercise("Supino Inclinado com Barra", MuscleGroup.CHEST, MuscleGroup.SHOULDERS, "Supino Inclinado"),
        SeedExercise("Supino Máquina", MuscleGroup.CHEST, MuscleGroup.TRICEPS, "Supino Máquina", introducedInVersion = 2),
        SeedExercise("Supino Inclinado Máquina", MuscleGroup.CHEST, MuscleGroup.SHOULDERS, "Supino Inclinado Máquina", introducedInVersion = 2),
        SeedExercise("Supino Reto com Halteres", MuscleGroup.CHEST, MuscleGroup.TRICEPS, "Halteres"),
        SeedExercise("Supino Inclinado com Halteres", MuscleGroup.CHEST, MuscleGroup.SHOULDERS, "Banco Regulável"),
        SeedExercise("Crucifixo com Halteres", MuscleGroup.CHEST, null, "Banco Regulável"),
        SeedExercise("Crucifixo no Cross Over", MuscleGroup.CHEST, null, "Cross Over"),
        SeedExercise("Voador (Peck Deck)", MuscleGroup.CHEST, null, "Peck Deck"),
        SeedExercise("Flexão de Braço", MuscleGroup.CHEST, MuscleGroup.TRICEPS, "Peso Corporal"),
        SeedExercise("Mergulho para Peito", MuscleGroup.CHEST, MuscleGroup.TRICEPS, "Paralelas"),

        // Costas
        SeedExercise("Puxada Frontal Aberta", MuscleGroup.BACK, MuscleGroup.BICEPS, "Polia Alta"),
        SeedExercise("Puxada Triângulo", MuscleGroup.BACK, MuscleGroup.BICEPS, "Polia Alta"),
        SeedExercise("Remada Máquina", MuscleGroup.BACK, MuscleGroup.BICEPS, "Remada Máquina", introducedInVersion = 2),
        SeedExercise("Remada Baixa Sentada", MuscleGroup.BACK, MuscleGroup.BICEPS, "Polia Baixa"),
        SeedExercise("Remada Curvada com Barra", MuscleGroup.BACK, MuscleGroup.BICEPS, "Barra Reta"),
        SeedExercise("Remada Unilateral (Serrote)", MuscleGroup.BACK, MuscleGroup.BICEPS, "Halteres"),
        SeedExercise("Pull-down com Corda", MuscleGroup.BACK, null, "Polia Alta"),
        SeedExercise("Barra Fixa Pronada", MuscleGroup.BACK, MuscleGroup.BICEPS, "Barra Fixa"),
        SeedExercise("Levantamento Terra", MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, "Barra Reta"),

        // Ombros
        SeedExercise("Desenvolvimento Máquina", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, "Desenvolvimento Máquina", introducedInVersion = 2),
        SeedExercise("Desenvolvimento com Halteres", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, "Halteres"),
        SeedExercise("Desenvolvimento Militar com Barra", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, "Barra Reta"),
        SeedExercise("Desenvolvimento no Smith", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, "Smith Machine"),
        SeedExercise("Elevação Lateral Máquina", MuscleGroup.SHOULDERS, null, "Elevação Lateral Máquina", introducedInVersion = 2),
        SeedExercise("Elevação Lateral com Halteres", MuscleGroup.SHOULDERS, null, "Halteres"),
        SeedExercise("Elevação Lateral na Polia", MuscleGroup.SHOULDERS, null, "Polia Baixa"),
        SeedExercise("Elevação Frontal com Halteres", MuscleGroup.SHOULDERS, null, "Halteres"),
        SeedExercise("Crucifixo Inverso Máquina", MuscleGroup.SHOULDERS, MuscleGroup.BACK, "Crucifixo Inverso Máquina", introducedInVersion = 2),
        SeedExercise("Crucifixo Inverso no Cross", MuscleGroup.SHOULDERS, MuscleGroup.BACK, "Cross Over"),
        SeedExercise("Encolhimento com Halteres", MuscleGroup.SHOULDERS, null, "Halteres"),

        // Bíceps
        SeedExercise("Rosca Direta com Barra Reta", MuscleGroup.BICEPS, null, "Barra Reta"),
        SeedExercise("Rosca Direta com Barra W", MuscleGroup.BICEPS, null, "Barra W"),
        SeedExercise("Rosca Alternada com Halteres", MuscleGroup.BICEPS, null, "Halteres"),
        SeedExercise("Rosca Martelo", MuscleGroup.BICEPS, null, "Halteres"),
        SeedExercise("Rosca Scott", MuscleGroup.BICEPS, null, "Banco Scott"),
        SeedExercise("Rosca na Polia Baixa", MuscleGroup.BICEPS, null, "Polia Baixa"),

        // Tríceps
        SeedExercise("Tríceps Máquina", MuscleGroup.TRICEPS, null, "Tríceps Máquina", introducedInVersion = 2),
        SeedExercise("Tríceps Pulley (Barra)", MuscleGroup.TRICEPS, null, "Polia Alta"),
        SeedExercise("Tríceps Corda na Polia", MuscleGroup.TRICEPS, null, "Polia Alta"),
        SeedExercise("Tríceps Testa com Barra W", MuscleGroup.TRICEPS, null, "Barra W"),
        SeedExercise("Tríceps Francês com Halter", MuscleGroup.TRICEPS, null, "Halteres"),
        SeedExercise("Mergulho nas Paralelas", MuscleGroup.TRICEPS, MuscleGroup.CHEST, "Paralelas"),

        // Quadríceps
        SeedExercise("Agachamento Livre", MuscleGroup.QUADS, MuscleGroup.GLUTES, "Barra Reta"),
        SeedExercise("Agachamento no Smith", MuscleGroup.QUADS, MuscleGroup.GLUTES, "Smith Machine"),
        SeedExercise("Hack Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES, "Hack Machine"),
        SeedExercise("Leg Press 45°", MuscleGroup.QUADS, MuscleGroup.GLUTES, "Leg Press 45°"),
        SeedExercise("Leg Press Horizontal", MuscleGroup.QUADS, MuscleGroup.GLUTES, "Leg Press Horizontal", introducedInVersion = 2),
        SeedExercise("Cadeira Extensora", MuscleGroup.QUADS, null, "Cadeira Extensora"),
        SeedExercise("Afundo com Halteres", MuscleGroup.QUADS, MuscleGroup.GLUTES, "Halteres"),

        // Posterior de Coxa
        SeedExercise("Mesa Flexora", MuscleGroup.HAMSTRINGS, null, "Mesa Flexora"),
        SeedExercise("Cadeira Flexora", MuscleGroup.HAMSTRINGS, null, "Cadeira Flexora"),
        SeedExercise("Stiff com Barra", MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, "Barra Reta"),
        SeedExercise("Stiff com Halteres", MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, "Halteres"),

        // Glúteos
        SeedExercise("Elevação Pélvica com Barra", MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, "Barra Reta"),
        SeedExercise("Cadeira Abdutora", MuscleGroup.GLUTES, null, "Cadeira Abdutora"),
        SeedExercise("Cadeira Adutora", MuscleGroup.QUADS, MuscleGroup.GLUTES, "Cadeira Adutora"),

        // Panturrilha
        SeedExercise("Panturrilha em Pé na Máquina", MuscleGroup.CALVES, null, "Panturrilha em Pé"),
        SeedExercise("Panturrilha no Leg Press", MuscleGroup.CALVES, null, "Leg Press 45°"),

        // Abdômen / Core e Corpo Inteiro
        SeedExercise("Abdominal Máquina", MuscleGroup.CORE, null, "Abdominal Máquina", introducedInVersion = 2),
        SeedExercise("Abdominal Supra no Solo", MuscleGroup.CORE, null, "Peso Corporal"),
        SeedExercise("Abdominal na Polia Alta", MuscleGroup.CORE, null, "Polia Alta"),
        SeedExercise("Prancha Isométrica", MuscleGroup.CORE, null, "Peso Corporal"),
        SeedExercise("Caminhada / Trote na Esteira", MuscleGroup.FULL_BODY, null, "Esteira", introducedInVersion = 2),
        SeedExercise("Burpee", MuscleGroup.FULL_BODY, null, "Peso Corporal")
    )

    /**
     * Popula aparelhos e exercícios de forma incremental e idempotente.
     *
     * Em upgrades a partir de [fromVersion], insere apenas itens introduzidos em versões posteriores,
     * evitando ressuscitar itens padrão removidos ou duplicar itens renomeados pelo usuário.
     */
    suspend fun seedIfNeeded(
        equipmentDao: EquipmentDao,
        exerciseDao: ExerciseDao,
        fromVersion: Int = 0
    ) {
        val existingEquipment = equipmentDao.getAll()
        val effectiveFromVersion = if (fromVersion == 0 && existingEquipment.isNotEmpty()) {
            1
        } else {
            fromVersion
        }
        if (effectiveFromVersion >= SEED_VERSION) return

        val equipmentIdsByName = mutableMapOf<String, Long>()
        existingEquipment.forEach { existing ->
            equipmentIdsByName[existing.name.trim().lowercase()] = existing.id
        }

        for (item in defaultEquipment) {
            if (item.introducedInVersion <= effectiveFromVersion) continue
            val key = item.name.trim().lowercase()
            if (!equipmentIdsByName.containsKey(key)) {
                val id = try {
                    equipmentDao.insert(
                        EquipmentEntity(
                            name = item.name,
                            category = item.category,
                            notes = item.notes,
                            isCustom = false
                        )
                    )
                } catch (_: SQLiteConstraintException) {
                    equipmentDao.getByNameIgnoreCase(item.name)?.id ?: continue
                }
                equipmentIdsByName[key] = id
            }
        }

        val existingExerciseKeys = exerciseDao.getAll()
            .map { "${it.name.trim().lowercase()}|${it.equipmentId}" }
            .toMutableSet()

        for (exercise in defaultExercises) {
            if (exercise.introducedInVersion <= effectiveFromVersion) continue
            val equipmentId = equipmentIdsByName[exercise.equipmentName.trim().lowercase()] ?: continue
            val exerciseKey = "${exercise.name.trim().lowercase()}|$equipmentId"
            if (existingExerciseKeys.add(exerciseKey)) {
                exerciseDao.insert(
                    ExerciseEntity(
                        name = exercise.name,
                        primaryMuscle = exercise.primaryMuscle,
                        secondaryMuscle = exercise.secondaryMuscle,
                        equipmentId = equipmentId,
                        notes = exercise.notes,
                        isCustom = false
                    )
                )
            }
        }
    }
}
