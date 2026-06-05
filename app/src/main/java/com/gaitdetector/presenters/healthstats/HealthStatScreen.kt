package com.gaitdetector.presenters.healthstats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.R
import com.gaitdetector.presenters.healthstats.components.HealthDataInputRow
import com.gaitdetector.presenters.healthstats.components.MeasuredMetricRow

@Composable
fun HealthStatScreen(modifier: Modifier = Modifier) {
    var gender        by rememberSaveable { mutableStateOf("") }
    var birthdate     by rememberSaveable { mutableStateOf("") }
    var height        by rememberSaveable { mutableStateOf("") }
    var weight        by rememberSaveable { mutableStateOf("") }
    var activityLevel by rememberSaveable { mutableStateOf("") }
    var vo2Max        by rememberSaveable { mutableStateOf("") }
    var restingBp     by rememberSaveable { mutableStateOf("") }

    Column(
        modifier            = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.user_health_data_entry_text), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        HealthDataInputRow(stringResource(R.string.gender_text),         gender,        false) { gender = it }
        HealthDataInputRow(stringResource(R.string.birthdate_text),      birthdate,     false) { birthdate = it }
        HealthDataInputRow(stringResource(R.string.height_text),         height,        true)  { height = it }
        HealthDataInputRow(stringResource(R.string.weight_text),         weight,        true)  { weight = it }
        HealthDataInputRow(stringResource(R.string.activity_level_text), activityLevel, true)  { activityLevel = it }
        HealthDataInputRow(stringResource(R.string.vo2_max_text),        vo2Max,        true)  { vo2Max = it }
        HealthDataInputRow(stringResource(R.string.resting_bp_text),     restingBp,     true)  { restingBp = it }

        Spacer(modifier = Modifier.height(55.dp))
        Text(text = stringResource(R.string.measured_health_metrics_text), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(27.dp))

        MeasuredMetricRow(
            label    = stringResource(R.string.resting_or_max_hr_text),
            value    = stringResource(R.string.fifty_five_text),
            unit     = stringResource(R.string.bpm_text),
            modifier = Modifier.padding(horizontal = 30.dp),
        )
        MeasuredMetricRow(
            label    = stringResource(R.string.cardiovascular_health_text),
            value    = stringResource(R.string.eighty_five_text),
            modifier = Modifier.padding(horizontal = 30.dp),
        )
        MeasuredMetricRow(
            label    = stringResource(R.string.endurance_level_text),
            value    = stringResource(R.string.eighty_five_text),
            modifier = Modifier.padding(horizontal = 30.dp),
        )
    }
}
