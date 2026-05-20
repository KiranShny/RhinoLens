package io.github.kiranshny.rhinolens

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import io.github.kiranshny.rhinolens.data.db.RhinoLensDatabase
import io.github.kiranshny.rhinolens.data.mlkit.MlKitModelPackManager
import io.github.kiranshny.rhinolens.data.mlkit.MlKitTranslationEngine
import io.github.kiranshny.rhinolens.data.repository.DataStoreSettingsRepository
import io.github.kiranshny.rhinolens.data.repository.RoomCaptureRepository
import io.github.kiranshny.rhinolens.shared.orchestrator.TranslationOrchestrator
import io.github.kiranshny.rhinolens.shared.port.CaptureRepository
import io.github.kiranshny.rhinolens.shared.port.ModelPackManager
import io.github.kiranshny.rhinolens.shared.port.SettingsRepository
import io.github.kiranshny.rhinolens.shared.port.TranslationEngine
import java.io.File

class AppContainer(private val context: Context) {

    private val database: RhinoLensDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            RhinoLensDatabase::class.java,
            "rhinolens.db",
        ).build()
    }

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = {
                File(context.filesDir, "datastore/rhinolens_settings.preferences_pb")
            },
        )
    }

    val captureRepository: CaptureRepository by lazy {
        RoomCaptureRepository(
            dao = database.captureDao(),
            filesRoot = context.filesDir,
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(dataStore)
    }

    val translationEngine: TranslationEngine by lazy { MlKitTranslationEngine() }

    val modelPackManager: ModelPackManager by lazy { MlKitModelPackManager() }

    val translationOrchestrator: TranslationOrchestrator by lazy {
        TranslationOrchestrator(translator = translationEngine)
    }

    val capturesDir: File by lazy {
        File(context.filesDir, "captures").apply { mkdirs() }
    }
}
