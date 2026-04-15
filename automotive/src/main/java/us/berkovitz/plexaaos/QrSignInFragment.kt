package us.berkovitz.plexaaos

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import qrcode.QRCode
import us.berkovitz.plexaaos.databinding.FragmentQrSignInBinding
import us.berkovitz.plexapi.myplex.MyPlexPinLogin

class QrSignInFragment : Fragment() {

    private var _binding: FragmentQrSignInBinding? = null
    private val binding get() = _binding!!

    private val plexPinLogin = MyPlexPinLogin()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchToPassword.setOnClickListener {
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
        binding.qrCode.setImageBitmap(bitmap)
    }

    private fun startPinLogin() {
        plexPinLogin.pinChangeCb = { pin ->
            viewLifecycleOwner.lifecycleScope.launch {
                binding.pinText.text = pin
            }
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
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
        _binding = null
    }
}
