package com.example.toyingmap

import android.content.ClipData
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_maps.centerMarker
import kotlinx.android.synthetic.main.activity_maps.edgeMarker

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

  private lateinit var lastLocation: LatLng //I will use a mock -> Sydney
  private lateinit var originalMarkerPosition: LatLng
  private lateinit var map: GoogleMap

  private var radiusInMeters = 100.0

  private lateinit var onCameraIdleListener: GoogleMap.OnCameraIdleListener
  private lateinit var onCameraMoveStartedListener: GoogleMap.OnCameraMoveStartedListener

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    val mapFragment = supportFragmentManager
      .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)

    //pass to onViewCreate in fragments
    configureCameraIdleListener()
    configureCameraStartedListener()
    configureMarkerDragListener()
  }

  private fun configureMarkerDragListener() {
    edgeMarker.setOnTouchListener(object : View.OnTouchListener {
      override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
          MotionEvent.ACTION_DOWN -> {
//            val lastLocationPoint = map.projection.toScreenLocation(lastLocation)
//            val originalMarkerPositionPoint = map.projection.toScreenLocation(originalMarkerPosition)
//            val currentPosition = map.projection.fromScreenLocation(Point(view?.x?.toInt()!!, view.y.toInt()))
//            when {
//              view.x < lastLocationPoint.x -> {
//                // bellow range
////                true
//              }
//              view.x > originalMarkerPositionPoint.x -> {
//                // exceed range
//                // reset
//                radiusInMeters = 100.0
//                drawCircle(lastLocation)
////                true
//              }
//              else -> {
////             between range
//                radiusInMeters = meterDistanceBetweenPoints(lastLocation.latitude, lastLocation.longitude,
//                  currentPosition.latitude, originalMarkerPosition.longitude)
//                drawCircle(lastLocation)
////                true
//              }
//            }
//            view?.startDrag(ClipData.newPlainText("", ""), View.DragShadowBuilder(view), view, 0)
//            x = event.x
//            y = event.y
            return true
          }
          MotionEvent.ACTION_MOVE -> {
            Log.v("mapsActivity", "${event.rawX} + ${event.rawY}")
            Log.v("mapsActivity", "${view?.x} + ${view?.y}")
//            view?.startDrag(ClipData.newPlainText("", ""), View.DragShadowBuilder(view), view, 0)
            val lastLocationPoint = map.projection.toScreenLocation(lastLocation)
            if (event.rawX > lastLocationPoint.x) {
              view?.x = event.rawX
              view!!.invalidate()
              val currentPosition = map.projection.fromScreenLocation(Point(view.x.toInt(), view.y.toInt()))
              radiusInMeters = meterDistanceBetweenPoints(lastLocation.latitude, lastLocation.longitude,
                currentPosition.latitude, currentPosition.longitude)
              drawCircle(lastLocation)
              Log.v("curr", "curr = $currentPosition")
              Log.v("radius", "radius = $radiusInMeters")
            }
//            when {
//              event.rawX < lastLocationPoint.x -> {
//                //
//              }
//              event.rawX > originalMarkerPositionPoint.x -> {
//                //
//                radiusInMeters = 100.0
//                drawCircle(lastLocation)
//              }
//              else -> {
//                // between range
//                val currentPosition = map.projection.fromScreenLocation(Point(event.rawX.toInt(), event.rawY.toInt()))
//                radiusInMeters = meterDistanceBetweenPoints(lastLocation.latitude, lastLocation.longitude,
//                  currentPosition.latitude, originalMarkerPosition.longitude)
//                drawCircle(lastLocation)
//              }
//            }
            return true
          }
          else -> {
            return true
          }
        }
      }
    })

//    edgeMarker.setOnDragListener { view, _ ->
//      Log.v("","DRAG")
//      val lastLocationPoint = map.projection.toScreenLocation(lastLocation)
//      val originalMarkerPositionPoint = map.projection.toScreenLocation(originalMarkerPosition)
//      val currentPosition = map.projection.fromScreenLocation(Point(view?.x?.toInt()!!, view.y.toInt()))
//      when {
//        view.x < lastLocationPoint.x -> {
//          // bellow range
//                true
//        }
//        view.x > originalMarkerPositionPoint.x -> {
//          // exceed range
//          // reset
//          radiusInMeters = 100.0
//          drawCircle(lastLocation)
//                true
//        }
//        else -> {
////             between range
//          radiusInMeters = meterDistanceBetweenPoints(lastLocation.latitude, lastLocation.longitude,
//            currentPosition.latitude, originalMarkerPosition.longitude)
//          drawCircle(lastLocation)
//                true
//        }
//      }
//      val currentPosition = map.projection.fromScreenLocation(Point(view.x.toInt(), view.y.toInt()))
//      when {
//        currentPosition.latitude < lastLocation.latitude -> {
//          // bellow range
//          true
//        }
//        currentPosition.latitude > originalMarkerPosition.latitude -> {
//          // exceed range
//          // reset
//          radiusInMeters = 100.0
//          drawCircle(lastLocation)
//          true
//        }
//        else -> {
////             between range
//          radiusInMeters = meterDistanceBetweenPoints(lastLocation.latitude, lastLocation.longitude,
//            currentPosition.latitude, originalMarkerPosition.longitude)
//          drawCircle(lastLocation)
//          true
//        }
//      }
//    }
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
    originalMarkerPosition = calculateMarkerPosition(lastLocation, radiusInMeters)

    setUpMap()
  }

  private fun configureCameraStartedListener() {
    onCameraMoveStartedListener = GoogleMap.OnCameraMoveStartedListener {
      map.clear()
    }
  }

  private fun configureCameraIdleListener() {
    onCameraIdleListener = GoogleMap.OnCameraIdleListener {
      lastLocation = map.cameraPosition.target
      drawCircle(lastLocation)
    }
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

//      map.setOnMarkerDragListener(onMarkerDragListener)
      map.setOnCameraMoveStartedListener(onCameraMoveStartedListener)
      map.setOnCameraIdleListener(onCameraIdleListener)
    }
  }

  private fun drawCircle(position: LatLng) {
//    val radiusInMeters = 100.0
    map.clear()

    val strokeColor = -0x10000 //red outline
    val shadeColor = 0x44ff0000 //opaque red fill

    val circleOptions = CircleOptions().apply {
      center(position)
      radius(radiusInMeters)
      fillColor(shadeColor)
      strokeColor(strokeColor)
      strokeWidth(8f)
    }
//    map.clear()
    map.addCircle(circleOptions)

    val px = convertZoneRadiusToPixels(lastLocation.latitude, lastLocation.longitude, radiusInMeters)

    edgeMarker.x = centerMarker.x + px
    edgeMarker.y = centerMarker.y
    edgeMarker.visibility = View.VISIBLE

    // here mMap is my GoogleMap object
//    addMarker(calculateMarkerPosition(position, radiusInMeters))
  }

  private fun calculateMarkerPosition(centre: LatLng, radius: Double): LatLng {

    val EARTH_RADIUS = 6378100.0
    // Convert to radians.
    val lat = centre.latitude * Math.PI / 180.0
    val lon = centre.longitude * Math.PI / 180.0

    // y
    val latPoint = lat + radius / EARTH_RADIUS * Math.sin(0.0)
    // x
    val lonPoint = lon + radius / EARTH_RADIUS * Math.cos(0.0) / Math.cos(lat)

    // saving the location on circle as a LatLng point
    return LatLng(latPoint * 180.0 / Math.PI, lonPoint * 180.0 / Math.PI)
  }

  private fun convertZoneRadiusToPixels(lat: Double, lng: Double, radiusInMeters: Double): Int {
    val EARTH_RADIUS = 6378100.0
    val lat1 = radiusInMeters / EARTH_RADIUS
    val lng1 = radiusInMeters / (EARTH_RADIUS * Math.cos(Math.PI * lat / 180))

    val lat2 = lat + lat1 * 180 / Math.PI
    val lng2 = lng + lng1 * 180 / Math.PI

    val p1 = map.projection.toScreenLocation(LatLng(lat, lng))
    val p2 = map.projection.toScreenLocation(LatLng(lat2, lng2))

    return Math.abs(p1.x - p2.x)
  }

  private fun meterDistanceBetweenPoints(latA: Double, lngA: Double, latB: Double, lngB: Double): Double {
    val pk = (180f / Math.PI)

    val a1 = latA / pk
    val a2 = lngA / pk
    val b1 = latB / pk
    val b2 = lngB / pk

    val t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2)
    val t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2)
    val t3 = Math.sin(a1) * Math.sin(b1)
    val tt = Math.acos(t1 + t2 + t3)

    return 6366000 * tt
  }

  companion object {
    private const val LOCATION_PERMISSION_REQUEST_CODE = 1
  }

}
