package us.berkovitz.plexaaos

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import com.android.car.ui.preference.PreferenceFragment
import kotlinx.coroutines.launch
import us.berkovitz.plexapi.myplex.MyPlexUser

class SettingsFragment : PreferenceFragment() {

    private var plexToken: String? = null
    private lateinit var plexUtil: PlexUtil
    private var users: List<MyPlexUser> = emptyList()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.account_preferences, rootKey)

        plexUtil = PlexUtil(requireContext())
        plexToken = plexUtil.getToken()

        setupServerPreference()
        setupUserPreference()
        setupSignOutPreference()
    }

    private fun setupServerPreference() {
        val serverPref = findPreference<DropDownPreference>("pref_server")
        serverPref?.setOnPreferenceChangeListener { _, newValue ->
            val serverId = newValue as String
            lifecycleScope.launch {
                AndroidStorage.setServer(if (serverId == "auto") null else serverId, requireContext())
                (activity as? SettingsActivity)?.notifyRefresh()
            }
            true
        }

        lifecycleScope.launch {
            val servers = PlexUtil.getServers(plexToken ?: "")
            val entries = mutableListOf("Auto")
            val entryValues = mutableListOf("auto")

            entries.addAll(servers.map { it.name })
            entryValues.addAll(servers.map { it.clientIdentifier ?: "" })

            serverPref?.entries = entries.toTypedArray()
            serverPref?.entryValues = entryValues.toTypedArray()

            val currentServer = AndroidStorage.getServer(requireContext())
            serverPref?.value = currentServer ?: "auto"
        }
    }

    private fun setupUserPreference() {
        val userPref = findPreference<DropDownPreference>("pref_switch_user")
        userPref?.setOnPreferenceChangeListener { _, newValue ->
            val userId = newValue as String
            val user = users.find { it.id.toString() == userId }
            
            if (user?.protected == 1) {
                // TODO: Implement PIN dialog
                Toast.makeText(requireContext(), "User is protected by PIN. Switching not yet supported for protected users.", Toast.LENGTH_LONG).show()
                false
            } else {
                lifecycleScope.launch {
                    try {
                        val newToken = PlexUtil.switchUser(plexToken ?: "", userId, null)
                        plexUtil.setToken(newToken)
                        (activity as? SettingsActivity)?.notifyRefresh()
                        Toast.makeText(requireContext(), "Switched user to ${user?.title}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Failed to switch user: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
        }

        lifecycleScope.launch {
            users = PlexUtil.getUsers(plexToken ?: "")
            val entries = users.map { it.title }.toTypedArray()
            val entryValues = users.map { it.id.toString() }.toTypedArray()

            userPref?.entries = entries
            userPref?.entryValues = entryValues
        }
    }

    private fun setupSignOutPreference() {
        val signOutPref = findPreference<Preference>("pref_sign_out")
        signOutPref?.setOnPreferenceClickListener {
            (activity as? SettingsActivity)?.signOut()
            true
        }
    }
}
