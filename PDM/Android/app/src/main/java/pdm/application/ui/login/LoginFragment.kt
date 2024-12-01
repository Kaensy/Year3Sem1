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
import pdm.application.api.LoginRequest
import pdm.application.api.RetrofitClient
import pdm.application.databinding.FragmentLoginBinding
import pdm.application.util.SessionManager

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
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

            // Show loading state
            binding.loginButton.isEnabled = false
            binding.loginButton.text = "Logging in..."

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.login(LoginRequest(email, password))

                    if (response.isSuccessful && response.body() != null) {
                        val loginResponse = response.body()!!
                        sessionManager.saveAuthToken(loginResponse.token)
                        Log.d("LoginFragment", "Login successful")

                        // Navigate to tournament list
                        findNavController().navigate(
                            LoginFragmentDirections.actionLoginToTournamentList()
                        )
                    } else {
                        Log.e("LoginFragment", "Login failed: ${response.errorBody()?.string()}")
                        Snackbar.make(view, "Login failed: Invalid credentials", Snackbar.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("LoginFragment", "Login error", e)
                    Snackbar.make(view, "Login failed: ${e.message}", Snackbar.LENGTH_LONG).show()
                } finally {
                    // Reset button state
                    binding.loginButton.isEnabled = true
                    binding.loginButton.text = "Login"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}