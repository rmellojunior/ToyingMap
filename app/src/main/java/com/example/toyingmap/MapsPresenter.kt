package com.example.toyingmap

import android.util.Log
import android.view.MotionEvent
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable

class MapsPresenter(
  private val view: MapsContract.View,
  private val viewScheduler: Scheduler
) : MapsContract.Presenter {

  private var subscriptions: CompositeDisposable = CompositeDisposable()

  init {
    handleMarkerTouch()
  }

  override fun handleMarkerTouch() {
    subscriptions.add(
      view.markerTouch()
        .observeOn(viewScheduler)
        .subscribe(
          { event -> handleMotionEvent(event) },
          { error -> Log.e("ERROR", error.toString()) }
        )
    )
  }

  private fun handleMotionEvent(event: MotionEvent?) =
    when (event?.action) {
      MotionEvent.ACTION_MOVE -> {
        view.updateCircle(event)
        true
      }
      MotionEvent.ACTION_UP -> {
//        view.updateCircle(event)
        view.updateZoom(event)
        true
      }
      else -> {
        true
      }
    }

}