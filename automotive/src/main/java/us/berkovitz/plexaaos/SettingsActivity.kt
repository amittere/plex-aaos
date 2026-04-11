package us.berkovitz.plexaaos

import android.content.ComponentName
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    lateinit var plexUtil: PlexUtil
    private lateinit var musicServiceConnection: MusicServiceConnection
    var plexToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        musicServiceConnection = MusicServiceConnection(
            applicationContext,
            ComponentName(applicationContext, PlexMediaService::class.java)
        )

        plexUtil = PlexUtil(this)
        plexToken = plexUtil.getToken()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun signOut() {
        musicServiceConnection.sendCommand(LOGOUT, Bundle.EMPTY)
        finish()
    }

    fun notifyRefresh() {
        musicServiceConnection.sendCommand(REFRESH, Bundle.EMPTY)
    }
}
