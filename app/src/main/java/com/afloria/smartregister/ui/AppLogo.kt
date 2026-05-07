package com.afloria.smartregister.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afloria.smartregister.R
import com.afloria.smartregister.ui.theme.SmartRegisterTheme

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    // Simplest way: just show the PNG as it is
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.smart_register_icon),
            contentDescription = "Smart Register Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Preview(showBackground = true, widthDp = 512, heightDp = 512)
@Composable
fun AppLogoPreview() {
    SmartRegisterTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppLogo(modifier = Modifier.size(512.dp))
        }
    }
}
