package app.metroclock.data

data class PresetCity(
  val name: String,
  val country: String,
  val timeZoneId: String
)

val WorldCityPresets = listOf(
  PresetCity("London", "United Kingdom", "Europe/London"),
  PresetCity("New York", "United States", "America/New_York"),
  PresetCity("Tokyo", "Japan", "Asia/Tokyo"),
  PresetCity("Paris", "France", "Europe/Paris"),
  PresetCity("Sydney", "Australia", "Australia/Sydney"),
  PresetCity("Dubai", "United Arab Emirates", "Asia/Dubai"),
  PresetCity("Singapore", "Singapore", "Asia/Singapore"),
  PresetCity("Los Angeles", "United States", "America/Los_Angeles"),
  PresetCity("Chicago", "United States", "America/Chicago"),
  PresetCity("Hong Kong", "China", "Asia/Hong_Kong"),
  PresetCity("Berlin", "Germany", "Europe/Berlin"),
  PresetCity("Rome", "Italy", "Europe/Rome"),
  PresetCity("Toronto", "Canada", "America/Toronto"),
  PresetCity("Mumbai", "India", "Asia/Kolkata"),
  PresetCity("Cairo", "Egypt", "Africa/Cairo"),
  PresetCity("São Paulo", "Brazil", "America/Sao_Paulo"),
  PresetCity("Seoul", "South Korea", "Asia/Seoul"),
  PresetCity("Bangkok", "Thailand", "Asia/Bangkok"),
  PresetCity("Amsterdam", "Netherlands", "Europe/Amsterdam"),
  PresetCity("Madrid", "Spain", "Europe/Madrid"),
  PresetCity("San Francisco", "United States", "America/Los_Angeles"),
  PresetCity("Zurich", "Switzerland", "Europe/Zurich"),
  PresetCity("Stockholm", "Sweden", "Europe/Stockholm"),
  PresetCity("Mexico City", "Mexico", "America/Mexico_City"),
  PresetCity("Buenos Aires", "Argentina", "America/Argentina/Buenos_Aires"),
  PresetCity("Johannesburg", "South Africa", "Africa/Johannesburg"),
  PresetCity("Auckland", "New Zealand", "Pacific/Auckland"),
  PresetCity("Honolulu", "United States", "Pacific/Honolulu"),
  PresetCity("Beijing", "China", "Asia/Shanghai"),
  PresetCity("Istanbul", "Turkey", "Europe/Istanbul")
)
