package com.gaitdetector.presenters.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.R
import com.gaitdetector.ui.theme.Inter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text       = stringResource(R.string.top_bar_title_text),
                style      = MaterialTheme.typography.displayLarge,
                color      = Color.White,
                fontSize   = 24.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.W700,
                textAlign  = TextAlign.Center,
                fontFamily = Inter,
            )
        },
        navigationIcon = {
            Image(
                painter            = painterResource(R.drawable.app_logo),
                contentDescription = stringResource(R.string.app_logo_image_content_description_text),
                modifier           = Modifier
                    .padding(start = 16.dp)
                    .size(width = 64.dp, height = 35.dp),
                contentScale = ContentScale.Crop,
            )
        },
        actions = {
            Image(
                painter            = painterResource(R.drawable.ic_fenix),
                contentDescription = stringResource(R.string.app_logo_image_content_description_text),
                modifier           = Modifier
                    .padding(end = 16.dp)
                    .size(width = 61.dp, height = 21.dp),
                contentScale = ContentScale.Crop,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor    = Color.Black,
            titleContentColor = Color.White,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
