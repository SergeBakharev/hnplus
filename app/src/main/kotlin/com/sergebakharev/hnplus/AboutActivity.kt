package com.sergebakharev.hnplus

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import com.sergebakharev.hnplus.databinding.AboutBinding
import com.sergebakharev.hnplus.util.FontHelper

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: AboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set custom font for the HN title
        binding.aboutHn.typeface = FontHelper.getComfortaa(this, true)

        // Set up the URL link
        binding.aboutUrl.text = Html.fromHtml("<a href=\"https://serge.industries\">serge.industries</a>")
        binding.aboutUrl.movementMethod = LinkMovementMethod.getInstance()

        // Set up the GitHub link
        binding.aboutGithub.text = Html.fromHtml("<a href=\"https://github.com/SergeBakharev/hnplus/\">Fork this at Github</a>")
        binding.aboutGithub.movementMethod = LinkMovementMethod.getInstance()

        // Set up the Open source components link
        binding.aboutNotice.text = Html.fromHtml("<a href=\"notice://open\">Open source components used</a>")
        binding.aboutNotice.movementMethod = LinkMovementMethod.getInstance()
        binding.aboutNotice.setOnClickListener {
            showNoticeDialog()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun showNoticeDialog() {
        try {
            val noticeContent = assets.open("NOTICE").bufferedReader().use { it.readText() }
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Open Source Components")
                .setMessage(noticeContent)
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.show()
        } catch (e: Exception) {
            // Fallback: try to read from root directory
            try {
                val noticeFile = java.io.File(filesDir.parentFile?.parentFile, "NOTICE")
                val noticeContent = noticeFile.readText()
                val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Open Source Components")
                    .setMessage(noticeContent)
                    .setPositiveButton("Close") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.show()
            } catch (e2: Exception) {
                // Show error message if file can't be read
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Could not load open source components information.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }
}