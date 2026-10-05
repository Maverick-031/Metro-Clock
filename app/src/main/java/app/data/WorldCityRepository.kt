package app.metroclock.data

import kotlinx.coroutines.flow.Flow

class WorldCityRepository(private val dao: WorldCityDao) {
  val allCities: Flow<List<WorldCityEntity>> = dao.getAllCities()

  suspend fun insertCity(city: WorldCityEntity): Long = dao.insertCity(city)

  suspend fun deleteCity(city: WorldCityEntity) = dao.deleteCity(city)

  suspend fun deleteCityById(id: Long) = dao.deleteCityById(id)
}
