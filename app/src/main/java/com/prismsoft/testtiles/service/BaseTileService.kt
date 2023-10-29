package com.prismsoft.testtiles.service

import android.service.quicksettings.TileService
import android.util.Log

abstract class BaseTileService: TileService() {
    protected val TAG = this::class.java.simpleName
    protected abstract fun updateUi()

    override fun onTileAdded() {
        Log.d(TAG, "onTileAdded")
        super.onTileAdded()
    }

    override fun onTileRemoved() {
        Log.d(TAG, "onTileRemoved")
        super.onTileRemoved()
    }
}