package com.luismalddonado.garagedoor.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Session
import androidx.car.app.Screen

class GarageDoorSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return GarageDoorScreen(carContext)
    }
}
