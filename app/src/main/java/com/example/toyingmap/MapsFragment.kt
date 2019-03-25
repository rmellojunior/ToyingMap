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
import com.google.android.gms.maps.model.LatLngBounds
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_maps.centerMarker
import kotlinx.android.synthetic.main.fragment_maps.edgeMarker
import kotlinx.android.synthetic.main.fragment_maps.line
import java.lang.Math.asin
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.toDegrees
import java.lang.StrictMath.toRadians

class MapsFragment : Fragment(), MapsContract.View, OnMapReadyCallback {

  private lateinit var lastLocation: LatLng //mocked -> Sydney
  private lateinit var googleMap: GoogleMap

  /*****************
   **     MAP     **
   *****************/
  private var mapRadius = 100.0
  private var mapZoom = 17.8f

  // save in resources
  private val mapStrokeColor = -0x10000 //red outline
  private val mapShadeColor = 0x44ff0000 //opaque red fill
  private val mapStrokeWidth = 8f

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

  /**
   * Manipulates the googleMap once available.
   * This callback is triggered when the googleMap is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  override fun onMapReady(googleMap: GoogleMap) {
    this.googleMap = googleMap

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

    if (lastLocation != null) {
      googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, mapZoom))

      drawCircle(lastLocation)

      googleMap.setOnCameraMoveStartedListener(onCameraMoveStartedListener)
      googleMap.setOnCameraIdleListener(onCameraIdleListener)
    }
  }

  override fun markerTouch(): Observable<MotionEvent> {
    return RxView.touches(edgeMarker)
      .map { motionEvent -> motionEvent }
  }

  override fun updateCircle(event: MotionEvent) {
    val lastLocationPoint = googleMap.projection.toScreenLocation(lastLocation)
    if (event.rawX > lastLocationPoint.x) {
      val currentPosition = googleMap.projection.fromScreenLocation(
        Point(event.rawX.toInt(), edgeMarker.y.toInt()))
      mapRadius = MapsUtils.meterDistanceBetweenPoints(lastLocation.latitude,
        lastLocation.longitude,
        currentPosition.latitude, currentPosition.longitude)
      mapClear()
      drawCircle(lastLocation)
    }
  }

  override fun updateZoom(event: MotionEvent) {
    mapClear()
    drawCircle(lastLocation)
    animateCamera()
  }

  private fun drawCircle(position: LatLng) {

    googleMap.addCircle(
      CircleOptions().apply {
        center(position)
        radius(mapRadius)
        fillColor(mapShadeColor)
        strokeColor(mapStrokeColor)
        strokeWidth(mapStrokeWidth)
      }
    )

    val radiusPixels = MapsUtils.convertZoneRadiusToPixels(googleMap, lastLocation.latitude,
      lastLocation.longitude, mapRadius)

    edgeMarker.apply {
      x = centerMarker.x + radiusPixels
      y = centerMarker.y
      visibility = View.VISIBLE
    }

    line.apply {
      layoutParams.width = radiusPixels
      val arr = intArrayOf(0, 0)
      centerMarker.getLocationOnScreen(arr)
      y = (arr[1]).toFloat()
      x = (arr[0] + centerMarker.width / 2.0).toFloat()
      requestLayout()
      visibility = View.VISIBLE
    }

    //the dashed line it is not so good for me
//    googleMap.addPolyline(
//      PolylineOptions()
//        .add(lastLocation, MapsUtils.getPoint(lastLocation, mapRadius.toInt(),0.0))
//        .width(5f)
//        .pattern(PATTERN_POLYGON_ALPHA)
//        .color(Color.RED))

  }

  private fun animateCamera() {
    val targetNorthEast = MapsUtils.computeOffset(lastLocation, mapRadius * Math.sqrt(2.0), 45.0)
    val targetSouthWest = MapsUtils.computeOffset(lastLocation, mapRadius * Math.sqrt(2.0), 225.0)
    val padding = MapsUtils.calculatePadding(view!!.width)
    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
      LatLngBounds.builder().include(targetNorthEast).include(targetSouthWest).build(), padding))
  }

  private fun configureListeners() {
    configureCameraIdleListener()
    configureCameraStartedListener()
  }

  private fun configureCameraStartedListener() {
    onCameraMoveStartedListener = GoogleMap.OnCameraMoveStartedListener { mapClear() }
  }

  private fun configureCameraIdleListener() {
    onCameraIdleListener = GoogleMap.OnCameraIdleListener {
      lastLocation = googleMap.cameraPosition.target
      drawCircle(lastLocation)
    }
  }

  private fun mapClear() {
    googleMap.clear()
  }

  companion object {
    private const val LAYOUT = R.layout.fragment_maps

    private const val EARTH_RADIUS = 6378100.0
    private const val LOCATION_PERMISSION_REQUEST_CODE = 1

    fun newInstance(): MapsFragment {
      return MapsFragment()
    }
  }

  /**
   * This class is a simple version of Android-maps-utils open-source library. MapsUtils contains
   * Google Maps Android API utilities that are useful to our application.
   */
  private class MapsUtils {
    companion object {

      /**
       *
       */
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

      /** Calculate padding offset from edges of the map - 10% of screen */
      fun calculatePadding(mapWidth: Int): Int {
        return (mapWidth * 0.10).toInt()
      }

      /**
       * This example belongs to android-maps-utils SphericalUtil class
       * Returns the LatLng resulting from moving a distance from an origin
       * in the specified heading (expressed in degrees clockwise from north).
       * @see [stackoverflow](https://stackoverflow.com/questions/15319431/how-to-convert-a-latlng-and-a-radius-to-a-latlngbounds-in-android-google-maps-ap/31029389#31029389)
       * @see [SphericalUtil](https://github.com/googlemaps/android-maps-utils/blob/master/library/src/com/google/maps/android/SphericalUtil.java)
       * @param from     The LatLng from which to start.
       * @param distance The distance to travel.
       * @param heading  The heading in degrees clockwise from north.
       */
      fun computeOffset(from: LatLng, distance: Double, heading: Double): LatLng {
        var distance = distance
        var heading = heading
        distance /= EARTH_RADIUS
        heading = toRadians(heading)
        // http://williams.best.vwh.net/avform.htm#LL
        val fromLat = toRadians(from.latitude)
        val fromLng = toRadians(from.longitude)
        val cosDistance = cos(distance)
        val sinDistance = sin(distance)
        val sinFromLat = sin(fromLat)
        val cosFromLat = cos(fromLat)
        val sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * cos(heading)
        val dLng = atan2(
          sinDistance * cosFromLat * sin(heading),
          cosDistance - sinFromLat * sinLat)
        return LatLng(toDegrees(asin(sinLat)), toDegrees(fromLng + dLng))
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

      fun getPoint(center: LatLng, radius: Int, angle: Double): LatLng {
        // Get the coordinates of a circle point at the given angle
        val east = radius * Math.cos(angle)

        val cLat = center.latitude
        val cLng = center.longitude
        val latRadius = EARTH_RADIUS * Math.cos(cLat / 180 * Math.PI)

        val newLng = cLng + (east / latRadius / Math.PI * 180)

        return LatLng(center.latitude, newLng)
      }

//      val PATTERN_DASH_LENGTH_PX = 20f
//      val PATTERN_GAP_LENGTH_PX = 20f
//      val DASH = Dash(PATTERN_DASH_LENGTH_PX)
//      val GAP = Gap(PATTERN_GAP_LENGTH_PX)
//      val PATTERN_POLYGON_ALPHA = listOf(GAP, DASH)

    }
  }
}