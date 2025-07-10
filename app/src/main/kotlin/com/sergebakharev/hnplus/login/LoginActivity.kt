package com.sergebakharev.hnplus.login

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sergebakharev.hnplus.R
import com.sergebakharev.hnplus.Settings
import com.sergebakharev.hnplus.databinding.LoginDialogBinding
import com.sergebakharev.hnplus.task.HNLoginTask
import com.sergebakharev.hnplus.task.ITaskFinishedHandler
import com.sergebakharev.hnplus.task.ITaskFinishedHandler.TaskResultCode

class LoginActivity : AppCompatActivity(), ITaskFinishedHandler<Boolean?> {
    private lateinit var binding: LoginDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.userSettingsDialogSaveButton.setOnClickListener {
            binding.userSettingsDialogSaveButton.text = getString(R.string.checking)
            HNLoginTask.start(
                binding.userSettingsDialogUsername.text.toString(),
                binding.userSettingsDialogPassword.text.toString(),
                this,
                this,
                0
            )
        }
        binding.userSettingsDialogCancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onTaskFinished(taskCode: Int, code: TaskResultCode, result: Boolean?, tag: Any?) {
        if (result == true) {
            setResult(RESULT_OK)
            Settings.isUserLoggedIn(this) // Optionally check login
            finish()
        } else {
            val messageId = when {
                result != null && !result -> R.string.error_login_failed
                code == TaskResultCode.NoNetworkConnection -> R.string.error_login_device_offline
                else -> R.string.error_unknown_error
            }
            Toast.makeText(this, getString(messageId), Toast.LENGTH_LONG).show()
            binding.userSettingsDialogSaveButton.text = getString(R.string.check_and_save)
        }
    }
}
