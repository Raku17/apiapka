package com.example.apkaapii

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.apkaapii.ui.theme.ApkaapiiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Data classes to hold the API response
data class WeatherResponse(val main: MainWeather, val weather: List<Weather>)
data class MainWeather(val temp: Double, val feels_like: Double)
data class Weather(val main: String, val description: String)

// Retrofit API service interface
interface WeatherService {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApkaapiiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherApp()
                }
            }
        }
    }
}

fun translateWeatherCondition(condition: String): String {
    return when (condition) {
        "Clear" -> "Bezchmurnie"
        "Clouds" -> "Zachmurzenie"
        "Rain" -> "Deszcz"
        "Snow" -> "Śnieg"
        "Drizzle" -> "Mżawka"
        "Thunderstorm" -> "Burza"
        "Mist" -> "Mgła"
        "Smoke" -> "Dym"
        "Haze" -> "Zamglenie"
        "Dust" -> "Pył"
        "Fog" -> "Mgła"
        "Sand" -> "Piasek"
        "Ash" -> "Popiół"
        "Squall" -> "Szkwał"
        "Tornado" -> "Tornado"
        else -> condition
    }
}

fun translateWeatherDescription(description: String): String {
    return when (description) {
        "clear sky" -> "Bezchmurnie"
        "few clouds" -> "Lekkie zachmurzenie"
        "scattered clouds" -> "Rozproszone chmury"
        "broken clouds" -> "Częściowe zachmurzenie"
        "overcast clouds" -> "Całkowite zachmurzenie"
        "light rain" -> "Lekki deszcz"
        "moderate rain" -> "Umiarkowany deszcz"
        "heavy intensity rain" -> "Intensywny deszcz"
        "very heavy rain" -> "Bardzo intensywny deszcz"
        "extreme rain" -> "Ekstremalny deszcz"
        "freezing rain" -> "Marznący deszcz"
        "light snow" -> "Lekki śnieg"
        "Snow" -> "Śnieg"
        "Heavy snow" -> "Intensywny śnieg"
        "Sleet" -> "Deszcz ze śniegiem"
        "Light shower sleet" -> "Lekki przelotny deszcz ze śniegiem"
        "Shower sleet" -> "Przelotny deszcz ze śniegiem"
        "Light rain and snow" -> "Lekki deszcz i śnieg"
        "Rain and snow" -> "Deszcz i śnieg"
        "Light shower snow" -> "Lekki przelotny śnieg"
        "Shower snow" -> "Przelotny śnieg"
        "Heavy shower snow" -> "Intensywny przelotny śnieg"
        else -> description
    }
}

@Composable
fun WeatherApp() {
    var city by remember { mutableStateOf("") }
    var weatherInfo by remember { mutableStateOf<WeatherResponse?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val apiKey = "eae61b633e7a027ba0e618a135d882d8"

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Apka Pogodowa",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text(stringResource(id = R.string.enter_city)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    errorMessage = ""
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val client = OkHttpClient.Builder()
                                .connectTimeout(30, TimeUnit.SECONDS)
                                .readTimeout(30, TimeUnit.SECONDS)
                                .writeTimeout(30, TimeUnit.SECONDS)
                                .build()
                            val retrofit = Retrofit.Builder()
                                .baseUrl("https://api.openweathermap.org/")
                                .client(client)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                            val service = retrofit.create(WeatherService::class.java)
                            val response = retryIO(times = 3) {
                                service.getCurrentWeather(city, apiKey)
                            }
                            weatherInfo = response
                        } catch (e: Exception) {
                            errorMessage = "Błąd podczas pobierania danych: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.get_weather))
            }
            Spacer(modifier = Modifier.height(16.dp))
            weatherInfo?.let {
                Text(stringResource(id = R.string.temperature, it.main.temp), color = Color.White)
                Text(stringResource(id = R.string.feels_like, it.main.feels_like), color = Color.White)
                Text(stringResource(id = R.string.weather, translateWeatherCondition(it.weather[0].main)), color = Color.White)
                Text(stringResource(id = R.string.description, translateWeatherDescription(it.weather[0].description)), color = Color.White)
            }
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

suspend fun <T> retryIO(
    times: Int,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            // Log exception here if needed
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ApkaapiiTheme {
        WeatherApp()
    }
}
