package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AvailableWorldCities
import com.example.data.WorldClockCity
import com.example.ui.animation.MetroTurnstileEntrance
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalMetroBackground
import com.example.ui.theme.LocalMetroDivider
import com.example.ui.theme.LocalMetroSubtextColor
import com.example.ui.theme.LocalMetroTextColor
import com.example.ui.theme.LocalMetroTileBg
import com.example.util.rememberMetroHaptic
import kotlinx.coroutines.delay

@Composable
fun WorldClockTab(
  cities: List<WorldClockCity>,
  onAddCity: (WorldClockCity) -> Unit,
  onRemoveCity: (WorldClockCity) -> Unit,
  modifier: Modifier = Modifier
) {
  var showAddDialog by remember { mutableStateOf(false) }
  val haptic = rememberMetroHaptic()

  // Live ticking state to refresh times every 10 seconds
  var ticker by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(10000L)
      ticker = System.currentTimeMillis()
    }
  }

  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val accentColor = LocalAccentColor.current
  val bgColor = LocalMetroBackground.current

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("world_clock_tab")
  ) {
    if (cities.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp, vertical = 32.dp)
      ) {
        Text(
          text = "no cities added",
          fontSize = 24.sp,
          fontWeight = FontWeight.Light,
          color = subtextColor
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
      ) {
        item {
          Spacer(modifier = Modifier.height(4.dp))
        }

        itemsIndexed(cities, key = { _, city -> city.id }) { index, city ->
          // Read ticker to ensure recomposition on update
          val _tick = ticker
          MetroTurnstileEntrance(delayMillis = (index * 30).coerceAtMost(250)) {
            WorldClockCityRow(
              city = city,
              onDelete = {
                haptic()
                onRemoveCity(city)
              }
            )
          }
        }

        item {
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, subtextColor.copy(alpha = 0.5f))
              .clickable {
                haptic()
                showAddDialog = true
              }
              .padding(vertical = 12.dp, horizontal = 16.dp)
              .testTag("btn_add_city"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add city",
              tint = textColor,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "add city",
              fontSize = 16.sp,
              fontWeight = FontWeight.Light,
              color = textColor,
              modifier = Modifier.padding(start = 8.dp)
            )
          }
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }

    if (showAddDialog) {
      AddCityDialog(
        currentCities = cities,
        onSelectCity = { selectedCity ->
          onAddCity(selectedCity)
          showAddDialog = false
        },
        onDismiss = { showAddDialog = false }
      )
    }
  }
}

@Composable
fun WorldClockCityRow(
  city: WorldClockCity,
  onDelete: () -> Unit
) {
  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val accentColor = LocalAccentColor.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("world_clock_city_${city.id}"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = city.getCurrentTime(),
          fontSize = 44.sp,
          fontWeight = FontWeight.Light,
          color = textColor,
          letterSpacing = (-0.5).sp
        )
        Text(
          text = city.getAmPm(),
          fontSize = 18.sp,
          fontWeight = FontWeight.Light,
          color = textColor.copy(alpha = 0.75f),
          modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp)
      ) {
        Icon(
          imageVector = if (city.isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
          contentDescription = if (city.isNight) "Night" else "Day",
          tint = if (city.isNight) accentColor else textColor.copy(alpha = 0.8f),
          modifier = Modifier.size(14.dp)
        )
        Text(
          text = city.cityName,
          fontSize = 18.sp,
          fontWeight = FontWeight.Normal,
          color = textColor,
          modifier = Modifier.padding(start = 6.dp)
        )
      }

      Text(
        text = "${city.country} • ${city.getTimeDifference()}",
        fontSize = 13.sp,
        fontWeight = FontWeight.Light,
        color = subtextColor,
        modifier = Modifier.padding(top = 1.dp)
      )
    }

    Box(
      modifier = Modifier
        .size(40.dp)
        .clickable(onClick = onDelete)
        .testTag("delete_city_${city.id}"),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = "Remove ${city.cityName}",
        tint = subtextColor,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
fun AddCityDialog(
  currentCities: List<WorldClockCity>,
  onSelectCity: (WorldClockCity) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val currentIds = remember(currentCities) { currentCities.map { it.id }.toSet() }
  val available = remember(searchQuery, currentIds) {
    AvailableWorldCities.filter {
      !currentIds.contains(it.id) &&
      (it.cityName.contains(searchQuery, ignoreCase = true) ||
       it.country.contains(searchQuery, ignoreCase = true))
    }
  }

  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val accentColor = LocalAccentColor.current
  val bgColor = LocalMetroBackground.current
  val tileBg = LocalMetroTileBg.current
  val dividerColor = LocalMetroDivider.current
  val haptic = rememberMetroHaptic()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(bgColor)
        .padding(horizontal = 24.dp, vertical = 32.dp)
        .testTag("dialog_add_world_city")
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "choose city",
            fontSize = 36.sp,
            fontWeight = FontWeight.Light,
            color = textColor
          )
          Box(
            modifier = Modifier
              .size(40.dp)
              .clickable {
                haptic()
                onDismiss()
              }
              .testTag("btn_close_add_city"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = textColor,
              modifier = Modifier.size(24.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Input
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(tileBg)
            .border(1.dp, dividerColor)
            .padding(horizontal = 12.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          if (searchQuery.isEmpty()) {
            Text(
              text = "Search city or country...",
              fontSize = 16.sp,
              color = subtextColor
            )
          }
          BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            textStyle = TextStyle(
              color = textColor,
              fontSize = 16.sp,
              fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(accentColor),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_search_city")
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          itemsIndexed(available, key = { _, city -> city.id }) { _, city ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, dividerColor)
                .clickable {
                  haptic()
                  onSelectCity(city)
                }
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .testTag("city_choice_${city.id}"),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = city.cityName,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Normal,
                  color = textColor
                )
                Text(
                  text = city.country,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Light,
                  color = subtextColor
                )
              }
              Text(
                text = city.getCurrentTime(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = accentColor
              )
            }
          }
        }
      }
    }
  }
}
