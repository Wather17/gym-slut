package com.gymslut.app.data.local

import androidx.room.TypeConverter
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.MuscleGroup

/**
 * Conversores de tipos enum para persistência no Room.
 */
class Converters {

    @TypeConverter
    fun fromEquipmentCategory(value: EquipmentCategory): String = value.name

    @TypeConverter
    fun toEquipmentCategory(raw: String): EquipmentCategory =
        EquipmentCategory.valueOf(raw)

    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup?): String? = value?.name

    @TypeConverter
    fun toMuscleGroup(raw: String?): MuscleGroup? =
        raw?.let { MuscleGroup.valueOf(it) }
}
