package it.matteogrechi.gymassystant.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.matteogrechi.gymassystant.R

// GYM-007/013/022/025/027: one centered offline brand, without unusable navigation.
@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
        Box(Modifier.size(288.dp).semantics { contentDescription = "gym assystant" }) {
            Image(painterResource(R.drawable.gym_launch_brand), null, Modifier.fillMaxSize())
            Text(stringResource(R.string.loading), color = GymColors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp))
        }
    }
}
