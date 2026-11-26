package com.gymslut.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Aparelho ou equipamento de academia disponível no catálogo local.
 */
@Entity(
    tableName = "equipment",
    indices = [Index(value = ["name"], unique = true)]
)
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: EquipmentCategory,
    val notes: String = "",
    val isCustom: Boolean = false
)
