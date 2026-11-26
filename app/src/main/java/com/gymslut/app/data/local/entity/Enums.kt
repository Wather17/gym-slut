package com.gymslut.app.data.local.entity

/**
 * Categorias de aparelhos e equipamentos de academia.
 */
enum class EquipmentCategory(val labelPtBr: String) {
    MACHINE("Máquina"),
    FREE_WEIGHT("Peso Livre"),
    CABLE("Polia / Cabo"),
    BODYWEIGHT("Peso Corporal"),
    BENCH_RACK("Banco / Rack"),
    OTHER("Outro")
}

/**
 * Grupos musculares utilizados no catálogo de exercícios.
 */
enum class MuscleGroup(val labelPtBr: String) {
    CHEST("Peito"),
    BACK("Costas"),
    SHOULDERS("Ombros"),
    BICEPS("Bíceps"),
    TRICEPS("Tríceps"),
    QUADS("Quadríceps"),
    HAMSTRINGS("Posterior de Coxa"),
    GLUTES("Glúteos"),
    CALVES("Panturrilha"),
    CORE("Abdômen / Core"),
    FULL_BODY("Corpo Inteiro")
}
