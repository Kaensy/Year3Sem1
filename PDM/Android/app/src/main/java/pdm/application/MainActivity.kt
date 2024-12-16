package pdm.application

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pdm.application.api.LoginRequest
import pdm.application.api.RetrofitClient
import pdm.application.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Test API connection
        testApiConnection()
    }

    private fun testApiConnection() {
        lifecycleScope.launch {
            try {
                // Test login
                val loginResponse = RetrofitClient.apiService.login(
                    LoginRequest("user@example.com", "password123")
                )

                if (loginResponse.isSuccessful) {
                    Log.d("API_TEST", "Login successful: ${loginResponse.body()}")

                    // Test getting tournaments
                    val token = loginResponse.body()?.token ?: return@launch
                    val tournamentsResponse = RetrofitClient.apiService.getTournaments(
                        "Bearer $token",
                        page = 1,
                        limit = 10
                    )

                    if (tournamentsResponse.isSuccessful) {
                        Log.d("API_TEST", "Tournaments fetched: ${tournamentsResponse.body()}")
                    } else {
                        Log.e("API_TEST", "Failed to fetch tournaments: ${tournamentsResponse.errorBody()?.string()}")
                    }
                } else {
                    Log.e("API_TEST", "Login failed: ${loginResponse.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("API_TEST", "Error testing API", e)
            }
        }
    }
}