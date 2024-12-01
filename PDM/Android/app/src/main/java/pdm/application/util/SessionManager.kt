package pdm.application.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveAuthToken(token: String) {
        prefs.edit().putString(AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(AUTH_TOKEN, null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "TournamentAppPrefs"
        private const val AUTH_TOKEN = "auth_token"
    }
}