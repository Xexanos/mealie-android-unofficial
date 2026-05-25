package dev.xexanos.mealie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dev.xexanos.mealie.core.ui.theme.MealieTheme
import dev.xexanos.mealie.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MealieTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(navController = rememberNavController())
                }
            }
        }
    }
}
