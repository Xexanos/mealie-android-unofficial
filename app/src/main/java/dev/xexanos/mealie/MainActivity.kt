package dev.xexanos.mealie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import dev.xexanos.mealie.core.ui.theme.MealieTheme
import dev.xexanos.mealie.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MealieTheme {
                AppNavGraph(navController = rememberNavController())
            }
        }
    }
}
