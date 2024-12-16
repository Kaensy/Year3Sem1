package pdm.application.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import pdm.application.TournamentApplication
import pdm.application.api.LoginRequest
import pdm.application.api.RetrofitClient
import pdm.application.databinding.FragmentLoginBinding
import pdm.application.util.SessionManager


class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())

        // Check if already logged in
        lifecycleScope.launch {
            if (sessionManager.isLoggedIn()) {
                navigateToTournamentList()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val email = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Snackbar.make(view, "Please fill in all fields", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            handleLogin(email, password)
        }
    }


    private fun handleLogin(email: String, password: String) {
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "Logging in..."

        lifecycleScope.launch {
            try {
                // First check stored credentials
                if (sessionManager.getUserEmail() == email && sessionManager.getUserPassword() == password) {
                    Log.d("LoginFragment", "Using stored credentials")
                    TournamentApplication.startOneTimeSync(requireContext())
                    navigateToTournamentList()
                    return@launch
                }

                // If no stored credentials match, try online login
                if (!RetrofitClient.isNetworkAvailable(requireContext())) {
                    Snackbar.make(
                        binding.root,
                        "No network connection and no matching stored credentials",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val response = RetrofitClient.apiService.login(LoginRequest(email, password))

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    sessionManager.saveAuthToken(loginResponse.token)
                    sessionManager.saveUserCredentials(email, password)
                    TournamentApplication.startOneTimeSync(requireContext())
                    navigateToTournamentList()
                } else {
                    Snackbar.make(binding.root, "Invalid credentials", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("LoginFragment", "Login error", e)
                if (!RetrofitClient.isNetworkAvailable(requireContext())) {
                    Snackbar.make(binding.root, "No network connection available", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(binding.root, "Login failed: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            } finally {
                binding.loginButton.isEnabled = true
                binding.loginButton.text = "Login"
            }
        }
    }

    private fun navigateToTournamentList() {
        findNavController().navigate(
            LoginFragmentDirections.actionLoginToTournamentList()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
