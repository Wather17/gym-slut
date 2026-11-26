package com.gymslut.app.data.local

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

    data class SeedEquipment(
        val name: String,
        val category: EquipmentCategory,
        val notes: String = ""
    )

    data class SeedExercise(
        val name: String,
        val primaryMuscle: MuscleGroup,
        val secondaryMuscle: MuscleGroup? = null,
        val equipmentName: String,
        val notes: String = ""
    )

    val defaultEquipment: List<SeedEquipment> = listOf(
        SeedEquipment("Supino Reto", EquipmentCategory.BENCH_RACK),
        SeedEquipment("Supino Inclinado", EquipmentCategory.BENCH_RACK),
        SeedEquipment("Polia Alta", EquipmentCategory.CABLE),
        SeedEquipment("Polia Baixa", EquipmentCategory.CABLE),
        SeedEquipment("Cross Over", EquipmentCategory.CABLE),
        SeedEquipment("Leg Press 45°", EquipmentCategory.MACHINE),
        SeedEquipment("Cadeira Extensora", EquipmentCategory.MACHINE),
        SeedEquipment("Mesa Flexora", EquipmentCategory.MACHINE),
        SeedEquipment("Cadeira Flexora", EquipmentCategory.MACHINE),
        SeedEquipment("Hack Machine", EquipmentCategory.MACHINE),
        SeedEquipment("Smith Machine", EquipmentCategory.MACHINE),
        SeedEquipment("Peck Deck", EquipmentCategory.MACHINE),
        SeedEquipment("Cadeira Abdutora", EquipmentCategory.MACHINE),
        SeedEquipment("Cadeira Adutora", EquipmentCategory.MACHINE),
        SeedEquipment("Panturrilha em Pé", EquipmentCategory.MACHINE),
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
        SeedExercise("Remada Baixa Sentada", MuscleGroup.BACK, MuscleGroup.BICEPS, "Polia Baixa"),
        SeedExercise("Remada Curvada com Barra", MuscleGroup.BACK, MuscleGroup.BICEPS, "Barra Reta"),
        SeedExercise("Remada Unilateral (Serrote)", MuscleGroup.BACK, MuscleGroup.BICEPS, "Halteres"),
        SeedExercise("Pull-down com Corda", MuscleGroup.BACK, null, "Polia Alta"),
        SeedExercise("Barra Fixa Pronada", MuscleGroup.BACK, MuscleGroup.BICEPS, "Barra Fixa"),
        SeedExercise("Levantamento Terra", MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, "Barra Reta"),

        // Ombros
        SeedExercise("Desenvolvimento com Halteres", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, "Halteres"),
        SeedExercise("Desenvolvimento Militar com Barra", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, "Barra Reta"),
        SeedExercise("Desenvolvimento no Smith", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS, "Smith Machine"),
        SeedExercise("Elevação Lateral com Halteres", MuscleGroup.SHOULDERS, null, "Halteres"),
        SeedExercise("Elevação Lateral na Polia", MuscleGroup.SHOULDERS, null, "Polia Baixa"),
        SeedExercise("Elevação Frontal com Halteres", MuscleGroup.SHOULDERS, null, "Halteres"),
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
        SeedExercise("Abdominal Supra no Solo", MuscleGroup.CORE, null, "Peso Corporal"),
        SeedExercise("Abdominal na Polia Alta", MuscleGroup.CORE, null, "Polia Alta"),
        SeedExercise("Prancha Isométrica", MuscleGroup.CORE, null, "Peso Corporal"),
        SeedExercise("Burpee", MuscleGroup.FULL_BODY, null, "Peso Corporal")
    )

    /**
     * Popula aparelhos e exercícios iniciais de forma idempotente (seguro para múltiplas execuções).
     */
    suspend fun seedIfNeeded(
        equipmentDao: EquipmentDao,
        exerciseDao: ExerciseDao
    ) {
        val equipmentIdsByName = mutableMapOf<String, Long>()

        equipmentDao.getAll().forEach { existing ->
            equipmentIdsByName[existing.name.trim().lowercase()] = existing.id
        }

        for (item in defaultEquipment) {
            val key = item.name.trim().lowercase()
            if (!equipmentIdsByName.containsKey(key)) {
                val id = equipmentDao.insert(
                    EquipmentEntity(
                        name = item.name,
                        category = item.category,
                        notes = item.notes,
                        isCustom = false
                    )
                )
                equipmentIdsByName[key] = id
            }
        }

        val existingExerciseKeys = exerciseDao.getAll()
            .map { "${it.name.trim().lowercase()}|${it.equipmentId}" }
            .toMutableSet()

        for (exercise in defaultExercises) {
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
