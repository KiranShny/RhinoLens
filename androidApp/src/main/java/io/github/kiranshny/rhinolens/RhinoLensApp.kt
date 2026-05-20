package io.github.kiranshny.rhinolens

import android.app.Application

class RhinoLensApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}

val android.content.Context.appContainer: AppContainer
    get() = (applicationContext as RhinoLensApp).container
