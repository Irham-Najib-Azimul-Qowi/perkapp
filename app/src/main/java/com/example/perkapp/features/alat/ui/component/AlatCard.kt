package com.example.perkapp.features.alat.ui.component

import android.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perkapp.features.alat.data.local.AlatEntity

@Composable
fun AlatCard(
    alat: AlatEntity,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal =  16.dp, vertical =  4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation =  2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
             Text(
                text = alat.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Kategori: ${alat.category}")
            Row{
                Text(text = "Stok: ${alat.available_qty}/${alat.total_qty}")
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Kondisi: ${alat.condition}")
            }
        }
    }
}