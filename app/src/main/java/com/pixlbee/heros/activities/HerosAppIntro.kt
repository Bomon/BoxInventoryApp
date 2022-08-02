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
        var img_vehicle = R.drawable.changelog_vehicles_light
        var img_item = R.drawable.changelog_item_light
        var img_setting = R.drawable.changelog_settings_light
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES){
            img_vehicle = R.drawable.changelog_vehicles_dark
            img_item = R.drawable.changelog_item_dark
            img_setting = R.drawable.changelog_settings_dark
        }


        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(AppIntroFragment.createInstance(
            title = "Neuigkeiten\n- Version 1.2 -",
            imageDrawable  = R.drawable.ic_logo,
            backgroundDrawable = R.drawable.back_slide1,
            description = "Update 1.2 ist erschienen und bringt neben neue Funktionen auch viele kleine Verbesserungen"
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "Fahrzeuge",
            imageDrawable = img_vehicle,
            backgroundDrawable = R.drawable.back_slide2,
            description = "Es gibt eine neue Hauptansicht, in der Fahrzeuge angelegt und verwaltet werden können. Jede Box kann nun einem Fahrzeug zugewiesen werden."
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "Einstellungen",
            imageDrawable = img_setting,
            backgroundDrawable = R.drawable.back_slide3,
            description = "In den Einstellungen können jetzt Texte / Logos gesetzt werden, die bei der Erstellung des PDF Ausdrucks verwendet werden."
        ))
        addSlide(AppIntroFragment.createInstance(
            title = "Ausrüstungs Verfügbarkeit",
            imageDrawable = img_item,
            backgroundDrawable = R.drawable.back_slide4,
            description = "Es wird nun angezeigt, wie viele Gegenstände in einer Box verfügbar sind.\n\nDurch ein kurzes Wischen (oder im Box-Editor) können Gegenstände als 'entnommen' markiert werden."
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