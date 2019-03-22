package com.example.toyingmap

import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_maps.centerMarker
import kotlinx.android.synthetic.main.fragment_maps.edgeMarker

class MapsFragment : Fragment(), MapsContract.View, OnMapReadyCallback {

  private lateinit var lastLocation: LatLng //mocked -> Sydney
  private lateinit var map: GoogleMap

  private var radiusInMeters = 100.0  //mocked

  // save in resources
  private val myStrokeColor = -0x10000 //red outline
  private val myShadeColor = 0x44ff0000 //opaque red fill
  private val myStrokeWidth = 8f

  private lateinit var onCameraIdleListener: GoogleMap.OnCameraIdleListener
  private lateinit var onCameraMoveStartedListener: GoogleMap.OnCameraMoveStartedListener

  private lateinit var presenter: MapsPresenter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View? {
    return inflater.inflate(LAYOUT, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)

    configureListeners()
  }

  override fun onResume() {
    super.onResume()
    //TODO: inject through dagger
    presenter = MapsPresenter(
      view = this,
      viewScheduler = AndroidSchedulers.mainThread()
    )
  }

  override fun markerTouch(): Observable<MotionEvent> {
    return RxView.touches(edgeMarker)
      .map { motionEvent -> motionEvent }
  }

  private fun configureListeners() {
    configureCameraIdleListener()
    configureCameraStartedListener()
  }

  override fun updateCircle(event: MotionEvent) {
    val lastLocationPoint = map.projection.toScreenLocation(lastLocation)
    if (event.rawX > lastLocationPoint.x) {
      val currentPosition = map.projection.fromScreenLocation(
        Point(event.rawX.toInt(), edgeMarker.y.toInt()))
      radiusInMeters = MapsUtils.meterDistanceBetweenPoints(lastLocation.latitude,
        lastLocation.longitude,
        currentPosition.latitude, currentPosition.longitude)
      map.clear()
      drawCircle(lastLocation)
    }
  }

  private fun configureCameraStartedListener() {
    onCameraMoveStartedListener = GoogleMap.OnCameraMoveStartedListener { map.clear() }
  }

  private fun configureCameraIdleListener() {
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
    lastLocation = LatLng(30.0, 151.0)

    setUpMap()
  }

  private fun setUpMap() {
    // permissions
    if (ActivityCompat.checkSelfPermission(activity!!,
        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(activity!!,
        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
        LOCATION_PERMISSION_REQUEST_CODE)
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

    map.addCircle(
      CircleOptions().apply {
        center(position)
        radius(radiusInMeters)
        fillColor(myShadeColor)
        strokeColor(myStrokeColor)
        strokeWidth(myStrokeWidth)
      }
    )

    val radiusPixels = MapsUtils.convertZoneRadiusToPixels(map, lastLocation.latitude,
      lastLocation.longitude, radiusInMeters)

    edgeMarker.x = centerMarker.x + radiusPixels
    edgeMarker.y = centerMarker.y
    edgeMarker.visibility = View.VISIBLE

  }

  companion object {
    private const val LAYOUT = R.layout.fragment_maps

    private const val EARTH_RADIUS = 6378100.0
    private const val LOCATION_PERMISSION_REQUEST_CODE = 1

    fun newInstance(): MapsFragment {
      return MapsFragment()
    }
  }

  private class MapsUtils {
    companion object {
      fun convertZoneRadiusToPixels(map: GoogleMap, lat: Double, lng: Double,
        radiusInMeters: Double): Int {
        val lat1 = radiusInMeters / EARTH_RADIUS
        val lng1 = radiusInMeters / (EARTH_RADIUS * Math.cos(Math.PI * lat / 180))

        val lat2 = lat + lat1 * 180 / Math.PI
        val lng2 = lng + lng1 * 180 / Math.PI

        val p1 = map.projection.toScreenLocation(LatLng(lat, lng))
        val p2 = map.projection.toScreenLocation(LatLng(lat2, lng2))

        return Math.abs(p1.x - p2.x)
      }

      fun meterDistanceBetweenPoints(latA: Double, lngA: Double, latB: Double,
        lngB: Double): Double {
        val pk = (180f / Math.PI)

        val a1 = latA / pk
        val a2 = lngA / pk
        val b1 = latB / pk
        val b2 = lngB / pk

        val t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2)
        val t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2)
        val t3 = Math.sin(a1) * Math.sin(b1)
        val tt = Math.acos(t1 + t2 + t3)

        return EARTH_RADIUS * tt
      }
    }
  }
}