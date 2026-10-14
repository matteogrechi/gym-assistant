package it.matteogrechi.gymassystant.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import it.matteogrechi.gymassystant.feature.GymApp

// GYM-001/022/027: one native activity, platform Back/locales and no web/network runtime.
class MainActivity : AppCompatActivity() {
    private val model: GymViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GymApp(model) }
    }
    override fun onStop() {
        model.checkpoint()
        super.onStop()
    }
}
