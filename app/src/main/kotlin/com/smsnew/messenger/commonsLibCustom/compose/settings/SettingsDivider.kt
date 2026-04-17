package com.smsnew.messenger.commonsLibCustom.compose.settings

import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.smsnew.messenger.commonsLibCustom.compose.extensions.MyDevices
import com.smsnew.messenger.commonsLibCustom.compose.theme.AppThemeSurface

@Composable
fun SettingsHorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.background, //divider_grey,
    thickness: Dp = DividerDefaults.Thickness,
) {
    HorizontalDivider(modifier = modifier, color = color, thickness = thickness)
}


@Composable
@MyDevices
private fun SettingsHorizontalDividerPreview() {
    AppThemeSurface {
        SettingsHorizontalDivider()
    }
}
