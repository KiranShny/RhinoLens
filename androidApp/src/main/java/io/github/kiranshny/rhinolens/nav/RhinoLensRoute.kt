package io.github.kiranshny.rhinolens.nav

sealed class RhinoLensRoute(val path: String) {

    data object Home : RhinoLensRoute("home")
    data object Camera : RhinoLensRoute("camera")
    data object Library : RhinoLensRoute("library")
    data object ImportImage : RhinoLensRoute("import-image")
    data object Settings : RhinoLensRoute("settings")

    data object CaptureDetail : RhinoLensRoute("capture-detail/{id}") {
        const val ID_ARG = "id"
        fun build(id: String) = "capture-detail/$id"
    }
}
