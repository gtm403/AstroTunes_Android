package com.example.moodmusicgenerator

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import com.example.moodmusicgenerator.ui.theme.MoodMusicGeneratorTheme
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import coil.request.ImageRequest
import com.example.moodmusicgenerator.ui.theme.SpaceTheme
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.io.encoding.ExperimentalEncodingApi


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpaceTheme {
                AppNavigator()
            }
        }
    }
}


val moods = listOf("Calm", "Energetic", "Happy", "Sad", "Focused")

val moodToGenre = mapOf(
    "Calm" to "ambient",
    "Energetic" to "dance",
    "Happy" to "happy",
    "Sad" to "sad",
    "Focused" to "study"
)

@Composable
fun MoodSelectionScreen(onMoodSelected: (String) -> Unit) {
    SpaceBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Select Your Mood",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            moods.forEach { mood ->
                Button(
                    onClick = { onMoodSelected(mood) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Text(text = mood, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}


@Composable
fun SpaceBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.space_background)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        content()
    }
}



sealed class Screen {
    object MoodSelection : Screen()
    data class SongRecommendations(val mood: String) : Screen()
}

@Composable
fun AppNavigator() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MoodSelection) }

    when (val screen = currentScreen) {
        is Screen.MoodSelection -> MoodSelectionScreen { mood ->
            currentScreen = Screen.SongRecommendations(mood)
        }
        is Screen.SongRecommendations -> SongRecommendationsScreen(screen.mood) {
            currentScreen = Screen.MoodSelection
        }
    }
}

interface SpotifyApiService {
    @GET("v1/recommendations")
    suspend fun getRecommendations(
        @Header("Authorization") authHeader: String,
        @Query("seed_genres") seedGenres: String,
        @Query("limit") limit: Int = 20
    ): RecommendationsResponse
}

data class RecommendationsResponse(
    val tracks: List<Track>
)

data class Track(
    val id: String,
    val name: String,
    val artists: List<Artist>,
    val album: Album,
    @SerializedName("external_urls") val externalUrls: ExternalUrls
)

data class ExternalUrls(
    val spotify: String
)

data class Artist(
    val name: String
)

data class Album(
    val images: List<Image>
)

data class Image(
    val url: String
)

object SpotifyAuthManager {
    private const val CLIENT_ID = "0d30e13496a842fb9b2446f9d79d3e50"
    private const val CLIENT_SECRET = "ed595db979ac4dcdbc438c3458366f5c"
    private var accessToken: String? = null

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun getAccessToken(): String {
        if (accessToken == null) {
            val credentials = "$CLIENT_ID:$CLIENT_SECRET"
            val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val response = RetrofitInstance.spotifyAuthService.getAccessToken(
                authorization = basicAuth,
                grantType = "client_credentials"
            )
            accessToken = response.accessToken
        }
        return accessToken!!
    }
}

object RetrofitInstance {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val spotifyApiService: SpotifyApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyApiService::class.java)
    }

    val spotifyAuthService: SpotifyAuthService by lazy {
        Retrofit.Builder()
            .baseUrl("https://accounts.spotify.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyAuthService::class.java)
    }
}
interface SpotifyAuthService {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun getAccessToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String,
        @Field("code") code: String? = null,
        @Field("redirect_uri") redirectUri: String? = null,
        @Field("refresh_token") refreshToken: String? = null
    ): SpotifyTokenResponse
}

data class SpotifyTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("token_type") val tokenType: String
)

suspend fun fetchSongsForMood(mood: String): List<Track> {
    val genre = moodToGenre[mood] ?: "pop"
    val token = SpotifyAuthManager.getAccessToken()
    val service = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpotifyApiService::class.java)

    val response = service.getRecommendations(
        authHeader = "Bearer $token",
        seedGenres = genre
    )
    return response.tracks
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongRecommendationsScreen(mood: String, onBack: () -> Unit) {
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mood) {
        try {
            tracks = fetchSongsForMood(mood)
        } catch (e: Exception) {
            Log.e("SongRecommendations", "Error fetching tracks: ${e.message}")
            // Optionally, handle the error state (e.g., show a message)
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recommendations for $mood", style = MaterialTheme.typography.labelLarge) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                items(tracks, key = { it.id }) { track ->
                    TrackItem(track)
                }
            }
        }
    }
}

@Composable
fun TrackItem(track: Track) {
    val context = LocalContext.current
    var token by remember { mutableStateOf<String?>(null) }
    var isTokenFetched by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for composables

    // Fetch the token once using LaunchedEffect
    LaunchedEffect(Unit) {
        token = try {
            SpotifyAuthManager.getAccessToken()
        } catch (e: Exception) {
            Log.e("TrackItem", "Error fetching access token: ${e.message}")
            null
        }
        isTokenFetched = true
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(enabled = isTokenFetched) { // Check if the token is fetched
                coroutineScope.launch {
                    // Pause the current playback if a session exists
                    if (token != null) {
                        try {
                            val client = OkHttpClient()
                            val pauseRequest = Request.Builder()
                                .url("https://api.spotify.com/v1/me/player/pause")
                                .header("Authorization", "Bearer $token")
                                .put(RequestBody.create(null, ByteArray(0)))
                                .build()
                            client.newCall(pauseRequest).execute()

                            // Small delay to let Spotify register the pause
                            delay(200)

                        } catch (e: Exception) {
                            Log.e("TrackItem", "Error pausing playback: ${e.message}")
                        }

                        // Start Spotify to play the selected track
                        val spotifyUrl = track.externalUrls.spotify
                        val uri = Uri.parse(spotifyUrl)
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.spotify.music")
                        }

                        try {
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            // Spotify app isn't installed, open in browser
                            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(browserIntent)
                        }
                    }
                }
            }
    ) {
        // Existing UI code for displaying track information
        val imageUrl = track.album.images.firstOrNull()?.url
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Album Art",
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = track.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = track.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}






