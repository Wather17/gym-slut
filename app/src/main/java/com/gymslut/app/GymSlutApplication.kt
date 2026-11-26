package com.gymslut.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Classe de aplicação raiz responsável por inicializar o grafo Hilt do Gym Slut.
 */
@HiltAndroidApp
class GymSlutApplication : Application()
