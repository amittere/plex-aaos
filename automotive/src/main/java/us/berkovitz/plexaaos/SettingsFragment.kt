package us.berkovitz.plexaaos

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import com.android.car.ui.preference.PreferenceFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.berkovitz.plexapi.myplex.MyPlexResource
import us.berkovitz.plexapi.myplex.MyPlexUser

class SettingsFragment : PreferenceFragment() {

    private var plexToken: String? = null
    private lateinit var plexUtil: PlexUtil
    private var users: List<MyPlexUser> = emptyList()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.account_preferences, rootKey)

        plexUtil = PlexUtil(requireContext())
        plexToken = plexUtil.getToken()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupServerPreference()
        setupUserPreference()
        setupSignOutPreference()
    }

    private fun getServerText(server: MyPlexResource?): String {
        return server?.name ?: "Auto"
    }

    private fun setupServerPreference() {
        val serverPref = findPreference<DropDownPreference>("pref_server")
        serverPref?.isSelectable = false

        serverPref?.setOnPreferenceChangeListener { preference, newValue ->
            if ((preference as DropDownPreference).value == newValue) {
                return@setOnPreferenceChangeListener true
            }

            val serverId = newValue as String
            val context = context?.applicationContext ?: return@setOnPreferenceChangeListener true
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AndroidStorage.setServer(if (serverId == "auto") null else serverId, context)
                }
                (activity as? SettingsActivity)?.notifyRefresh()
            }
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val settingsActivity = activity as? SettingsActivity
            val servers = settingsActivity?.cachedServers ?: withContext(Dispatchers.IO) {
                val fetched = PlexUtil.getServers(plexToken ?: "")
                settingsActivity?.cachedServers = fetched
                fetched
            }
            if (servers.isNotEmpty()) {
                val entries = mutableListOf("Auto")
                val entryValues = mutableListOf("auto")

                entries.addAll(servers.map { it.name })
                entryValues.addAll(servers.map { it.clientIdentifier ?: "" })

                serverPref?.isEnabled = true
                serverPref?.entries = entries.toTypedArray()
                serverPref?.entryValues = entryValues.toTypedArray()

                val currentServer = withContext(Dispatchers.IO) {
                    AndroidStorage.getServer(requireContext())
                }
                serverPref?.value = currentServer ?: "auto"
                serverPref?.summary = getServerText(servers.find { it.clientIdentifier == currentServer })
            } else {
                serverPref?.isEnabled = false
                serverPref?.summary = "No servers found"
            }

            serverPref?.isSelectable = true
        }
    }

    private fun getUserText(user: MyPlexUser?): String {
        return user?.title ?: "Change the active Plex user"
    }

    private fun setupUserPreference() {
        val userPref = findPreference<DropDownPreference>("pref_switch_user")
        userPref?.isSelectable = false

        userPref?.setOnPreferenceChangeListener { preference, newValue ->
            if ((preference as DropDownPreference).value == newValue) {
                return@setOnPreferenceChangeListener true
            }

            val userId = newValue as String
            val user = users.find { it.id.toString() == userId }
            
            if (user?.protected == 1) {
                // TODO: Implement PIN dialog
                Toast.makeText(requireContext(), "User is protected by PIN. Switching not yet supported for protected users.", Toast.LENGTH_LONG).show()
                false
            } else {
                val context = context?.applicationContext ?: return@setOnPreferenceChangeListener true
                lifecycleScope.launch {
                    try {
                        val newToken = withContext(Dispatchers.IO) {
                            PlexUtil.switchUser(plexToken ?: "", userId, null)
                        }
                        plexUtil.setToken(newToken)
                        (activity as? SettingsActivity)?.notifyRefresh()
                        Toast.makeText(context, "Switched user to ${user?.title}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to switch user: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val settingsActivity = activity as? SettingsActivity
            users = settingsActivity?.cachedUsers ?: withContext(Dispatchers.IO) {
                val fetched = PlexUtil.getUsers(plexToken ?: "")
                settingsActivity?.cachedUsers = fetched
                fetched
            }
            if (users.isNotEmpty()) {
                val entries = users.map { it.title }.toTypedArray()
                val entryValues = users.map { it.id.toString() }.toTypedArray()
                userPref?.entries = entries
                userPref?.entryValues = entryValues
                userPref?.summary = getUserText(users.find { it.id.toString() == userPref.value })
            } else {
                userPref?.isEnabled = false
                userPref?.summary = "No users found"
            }

            userPref?.isSelectable = true
        }
    }

    private fun setupSignOutPreference() {
        val signOutPref = findPreference<Preference>("pref_sign_out")
        signOutPref?.isEnabled = (plexToken != null)
        signOutPref?.setOnPreferenceClickListener {
            (activity as? SettingsActivity)?.signOut()
            true
        }
    }
}
