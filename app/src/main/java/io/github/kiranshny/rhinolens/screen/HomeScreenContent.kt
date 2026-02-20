package io.github.kiranshny.rhinolens.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = "New App MOFO",
            color = Color.White
        )
        Button(
            onClick = {}
        ) {
            Text(text = "Start Scanninh")
        }
        Text(
            text = "New App bitch",
            color = Color.White
        )
        Button(
            onClick = {}
        ) {
            Text(text = "PAst scans bih")
        }
    }
}

@Composable
@Preview
fun HomeScreenContentPreview() {
    HomeScreenContent()
}