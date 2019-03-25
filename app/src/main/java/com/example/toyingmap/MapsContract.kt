package com.example.toyingmap

import android.view.MotionEvent
import io.reactivex.Observable

interface MapsContract {

  interface View {

    fun markerTouch(): Observable<MotionEvent>

    fun updateCircle(event: MotionEvent)

    fun updateZoom(event: MotionEvent)

  }

  interface Presenter {

    fun handleMarkerTouch()

  }

  interface Navigator

}