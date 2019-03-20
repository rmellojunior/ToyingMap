package com.example.toyingmap

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

  private lateinit var lastLocation: LatLng //I will use a mock -> Sydney
  private lateinit var map: GoogleMap

  private lateinit var onCameraIdleListener: GoogleMap.OnCameraIdleListener
  private lateinit var onCameraMoveStartedListener: GoogleMap.OnCameraMoveStartedListener

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    val mapFragment = supportFragmentManager
      .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)

    configureCameraIdle()
    configureCameraStarted()
  }

  private fun configureCameraStarted() {
    onCameraMoveStartedListener = GoogleMap.OnCameraMoveStartedListener {
      map.clear()
    }
  }

  private fun configureCameraIdle() {
    onCameraIdleListener = GoogleMap.OnCameraIdleListener {
      lastLocation = map.cameraPosition.target
      drawCircle(lastLocation)
    }
  }

  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  override fun onMapReady(googleMap: GoogleMap) {
    map = googleMap

    // Add a marker in Sydney
    lastLocation = LatLng(-34.0, 151.0)

    setUpMap()
  }

  private fun setUpMap() {
    // permissions
    if (ActivityCompat.checkSelfPermission(this,
        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
      return
    }

    // Got last known location. In some rare situations this can be null.
    if (lastLocation != null) {
      map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 17f))

      drawCircle(lastLocation)

      map.setOnCameraMoveStartedListener(onCameraMoveStartedListener)
      map.setOnCameraIdleListener(onCameraIdleListener)
    }
  }

  private fun drawCircle(position: LatLng) {
    val radiusInMeters = 100.0
    val strokeColor = -0x10000 //red outline
    val shadeColor = 0x44ff0000 //opaque red fill

    map.addCircle(
      CircleOptions().apply {
        center(position)
        radius(radiusInMeters)
        fillColor(shadeColor)
        strokeColor(strokeColor)
        strokeWidth(8f)
      }
    )
  }

  companion object {
    private const val LOCATION_PERMISSION_REQUEST_CODE = 1
  }

}
