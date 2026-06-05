package com.gaitdetector.presenters.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.R
import com.gaitdetector.data.local.data
import com.gaitdetector.data.local.dataListMockup
import com.gaitdetector.data.local.readinessLineMockup
import com.gaitdetector.presenters.main.components.CurrentReadinessBoxes
import com.gaitdetector.presenters.main.components.CurrentReadinessPercentage
import com.gaitdetector.presenters.main.components.CurrentReadinessText
import com.gaitdetector.presenters.main.components.EnergyExpenditurePlot
import com.gaitdetector.presenters.main.components.EnergyExpenditurePlotColorsAndMeanings
import com.gaitdetector.presenters.main.components.EnergyExpenditureText
import com.gaitdetector.presenters.main.components.PlotTitle
import com.gaitdetector.presenters.main.components.ReadinessCirclesRow
import com.gaitdetector.presenters.main.components.ReadinessPlot
import com.gaitdetector.ui.theme.Inter
import com.gaitdetector.ui.theme.TwentyFourHourChartBlueColor
import com.gaitdetector.ui.theme.sedentaryColor

var energyExpenditureList = mutableStateListOf(Pair(0, TwentyFourHourChartBlueColor))
var readinessLineList      = mutableStateListOf(100)

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .background(Color.Black)
            .padding(dimensionResource(R.dimen.box_horizontal_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CurrentReadinessText()
        CurrentReadinessBoxes(
            modifier = Modifier.fillMaxWidth(1f).padding(top = 15.dp)
        )
        CurrentReadinessPercentage()

        PlotTitle(title = stringResource(R.string.twenty_four_hour_trend_text))
        EnergyExpenditurePlot(
            dataListMockup,
            lineData = readinessLineMockup,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.twenty_four_hour_chart_horizontal_padding))
        )
        EnergyExpenditureText(modifier = Modifier.align(Alignment.Start).padding(start = 62.dp))
        EnergyExpenditurePlotColorsAndMeanings()

        PlotTitle(title = stringResource(R.string.four_week_trend_text), modifier = Modifier.padding(top = 13.dp))
        ReadinessPlot(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.four_week_trend_chart_horizontal_padding))
                .padding(top = 4.dp)
                .height(dimensionResource(R.dimen.four_week_trend_chart_height))
                .background(Color.Black),
            data = data,
        )
        ReadinessCirclesRow(modifier = Modifier.padding(start = 30.dp, end = 4.dp))

        Row(
            modifier              = Modifier.padding(start = 36.dp, end = 8.dp).fillMaxWidth().height(40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text       = stringResource(R.string.nine_divide_eleven_text),
                fontSize   = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.W400,
                fontFamily = Inter, color = Color.DarkGray,
            )
            Text(
                text       = stringResource(R.string.daily_readiness_text),
                fontSize   = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.W400,
                fontFamily = Inter, color = Color.White,
            )
            Row {
                Box(modifier = Modifier.padding(end = 5.dp).background(sedentaryColor, CircleShape).size(16.dp))
                Text(
                    text       = stringResource(R.string.high_text),
                    fontSize   = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.W400,
                    fontFamily = Inter, color = Color.White,
                )
                Spacer(modifier = Modifier.width(20.dp))
                Box(modifier = Modifier.padding(end = 5.dp).background(Color.White, CircleShape).size(16.dp))
                Text(
                    text       = stringResource(R.string.low_text),
                    fontSize   = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.W400,
                    fontFamily = Inter, color = Color.White,
                )
            }
            Text(
                text       = stringResource(R.string.ten_divide_eight_text),
                fontSize   = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.W400,
                fontFamily = Inter, color = Color.DarkGray,
            )
        }
    }
}
