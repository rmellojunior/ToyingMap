package com.example.toyingmap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

/**
 * Simple project to apply some ideas to build a map like Uber or Kapten.
 * TODO: 1.Dagger; 2.Better mvp pattern structure
 */
class MapsActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(LAYOUT)
    openMapsFragment()
  }

  private fun openMapsFragment() {
    val ft = supportFragmentManager.beginTransaction()
      .replace(R.id.container, MapsFragment.newInstance())
      .addToBackStack(MapsFragment::class.java.name)
    ft.commit()
  }

  companion object {
    private const val LAYOUT = R.layout.activity_maps

    //if other views will open this activity/fragment use this method
    fun getNavigationIntent(parentActivity: Activity): Intent {
      return Intent(parentActivity, MapsActivity::class.java)
    }
  }

}
