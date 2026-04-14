package us.berkovitz.plexaaos

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import qrcode.QRCode
import us.berkovitz.plexapi.myplex.MyPlexPinLogin

class QrSignInFragment : Fragment() {

    private val plexPinLogin = MyPlexPinLogin()
    private var pinLoginJob: Job? = null
    
    private lateinit var qrImageView: ImageView
    private lateinit var pinTextView: TextView

    override fun onCreateView(
        LayoutInflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.inflate(R.layout.fragment_qr_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        qrImageView = view.findViewById(R.id.qr_code)
        pinTextView = view.findViewById(R.id.pin_text)

        view.findViewById<Button>(R.id.switch_to_password).setOnClickListener {
            (activity as? LoginActivity)?.switchToPasswordSignIn()
        }

        setupQrCode()
        startPinLogin()
    }

    private fun setupQrCode() {
        val qrBytes = QRCode.ofSquares()
            .build("https://plex.tv/link")
            .renderToBytes()
        val bitmap = BitmapFactory.decodeByteArray(qrBytes, 0, qrBytes.size)
        qrImageView.setImageBitmap(bitmap)
    }

    private fun startPinLogin() {
        plexPinLogin.pinChangeCb = { pin ->
            lifecycleScope.launch(Dispatchers.Main) {
                pinTextView.text = pin
            }
        }

        pinLoginJob = lifecycleScope.launch(Dispatchers.IO) {
            val loginRes = plexPinLogin.pinLogin()
            if (loginRes.authToken != null) {
                val token = "${loginRes.clientIdentifier!!}|${loginRes.authToken!!}"
                withContext(Dispatchers.Main) {
                    (activity as? LoginActivity)?.setToken(token)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pinLoginJob?.cancel()
    }
}
