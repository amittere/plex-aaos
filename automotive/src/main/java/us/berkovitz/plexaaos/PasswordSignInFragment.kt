package us.berkovitz.plexaaos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class PasswordSignInFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_password_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usernameEdit = view.findViewById<EditText>(R.id.username)
        val passwordEdit = view.findViewById<EditText>(R.id.password)
        val signInButton = view.findViewById<Button>(R.id.btn_sign_in)
        val errorTextView = view.findViewById<TextView>(R.id.error_message)
        val switchToQrButton = view.findViewById<Button>(R.id.switch_to_qr)

        signInButton.setOnClickListener {
            val username = usernameEdit.text.toString()
            val password = passwordEdit.text.toString()
            
            errorTextView.visibility = View.GONE
            signInButton.isEnabled = false
            
            (activity as? LoginActivity)?.doLogin(username, password) { error ->
                activity?.runOnUiThread {
                    errorTextView.text = error
                    errorTextView.visibility = View.VISIBLE
                    signInButton.isEnabled = true
                }
            }
        }

        switchToQrButton.setOnClickListener {
            (activity as? LoginActivity)?.switchToQrCode()
        }
    }
}
