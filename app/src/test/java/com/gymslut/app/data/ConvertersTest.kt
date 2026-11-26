package com.gymslut.app.data

import com.gymslut.app.data.local.Converters
import com.gymslut.app.data.local.entity.EquipmentCategory
import com.gymslut.app.data.local.entity.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun equipmentCategory_roundTripsAllEnumValues() {
        for (category in EquipmentCategory.entries) {
            val raw = converters.fromEquipmentCategory(category)
            assertEquals(category, converters.toEquipmentCategory(raw))
        }
    }

    @Test
    fun muscleGroup_roundTripsAllEnumValuesAndNull() {
        assertNull(converters.fromMuscleGroup(null))
        assertNull(converters.toMuscleGroup(null))

        for (muscle in MuscleGroup.entries) {
            val raw = converters.fromMuscleGroup(muscle)
            assertEquals(muscle, converters.toMuscleGroup(raw))
        }
    }
}
