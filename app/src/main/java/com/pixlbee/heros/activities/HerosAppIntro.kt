package com.pixlbee.heros.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.pixlbee.heros.R

class HerosAppIntro : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!
        //isColorTransitionsEnabled = true

        val nightModeFlags: Int = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        var imgCompartments = R.drawable.changelog_compartments_light
        var imgItems = R.drawable.changelog_items_light
        var imgDesign = R.drawable.changelog_design_light
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES){
            imgCompartments = R.drawable.changelog_compartments_dark
            imgItems = R.drawable.changelog_items_dark
            imgDesign = R.drawable.changelog_design_dark
        }


        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(AppIntroFragment.createInstance(
            title = resources.getString(R.string.changelog_1_3_slide1_title),
            imageDrawable  = R.drawable.ic_logo,
            backgroundDrawable = R.drawable.back_slide1,
            description = resources.getString(R.string.changelog_1_3_slide1_text),
        ))
        addSlide(AppIntroFragment.createInstance(
            title = resources.getString(R.string.changelog_1_3_slide2_title),
            imageDrawable = imgCompartments,
            backgroundDrawable = R.drawable.back_slide2,
            description = resources.getString(R.string.changelog_1_3_slide2_text)
        ))
        addSlide(AppIntroFragment.createInstance(
            title =  resources.getString(R.string.changelog_1_3_slide3_title),
            imageDrawable = imgItems,
            backgroundDrawable = R.drawable.back_slide3,
            description = resources.getString(R.string.changelog_1_3_slide3_text)
        ))
        addSlide(AppIntroFragment.createInstance(
            title =  resources.getString(R.string.changelog_1_3_slide4_title),
            imageDrawable = imgDesign,
            backgroundDrawable = R.drawable.back_slide4,
            description = resources.getString(R.string.changelog_1_3_slide4_text)
        ))
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        startActivity(Intent(this, StartupActivity::class.java))
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        startActivity(Intent(this, StartupActivity::class.java))
        finish()
    }
}