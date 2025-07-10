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
        binding.aboutUrl.text = Html.fromHtml("<a href=\"http://www.creativepragmatics.com\">creativepragmatics.com</a>")
        binding.aboutUrl.movementMethod = LinkMovementMethod.getInstance()

        // Set up the GitHub link
        binding.aboutGithub.text = Html.fromHtml("<a href=\"https://github.com/manmal/hn-android/\">Fork this at Github</a>")
        binding.aboutGithub.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}