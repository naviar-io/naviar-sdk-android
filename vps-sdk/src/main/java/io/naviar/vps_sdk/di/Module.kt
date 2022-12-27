package io.naviar.vps_sdk.di

import android.content.Context
import android.hardware.SensorManager
import android.location.LocationManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.naviar.vps_sdk.common.CompassManager
import io.naviar.vps_sdk.common.CoordinateConverter
import io.naviar.vps_sdk.data.api.IVpsApiManager
import io.naviar.vps_sdk.data.api.NeuroApi
import io.naviar.vps_sdk.data.api.VpsApiManager
import io.naviar.vps_sdk.data.model.request.RequestVpsModel
import io.naviar.vps_sdk.data.repository.*
import io.naviar.vps_sdk.domain.interactor.INeuroInteractor
import io.naviar.vps_sdk.domain.interactor.IVpsInteractor
import io.naviar.vps_sdk.domain.interactor.NeuroInteractor
import io.naviar.vps_sdk.domain.interactor.VpsInteractor
import io.naviar.vps_sdk.ui.ArManager
import io.naviar.vps_sdk.ui.VpsArViewModel
import io.naviar.vps_sdk.ui.VpsService
import io.naviar.vps_sdk.ui.VpsServiceImpl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit

internal object Module {

    private const val HOST_MOCK = "http://mock/"

    val repository: Module = module {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
            .apply { level = if (io.naviar.vps_sdk.BuildConfig.DEBUG) Level.BASIC else Level.NONE }

        single {
            OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .build()
        }
        single<Moshi> {
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        }
        single<JsonAdapter<RequestVpsModel>> { get<Moshi>().adapter(RequestVpsModel::class.java) }
        single<IVpsApiManager> { VpsApiManager(get(), get()) }
        factory<IVpsRepository> { VpsRepository(get(), get()) }

        single<NeuroApi> {
            Retrofit.Builder()
                .baseUrl(HOST_MOCK)
                .client(get())
                .build()
                .create(NeuroApi::class.java)
        }
        single<INeuroRepository> { NeuroRepository(get(), get()) }
        single<IPrefsRepository> { PrefsRepository(get()) }
    }

    val domain: Module = module {
        factory<INeuroInteractor> { NeuroInteractor(get()) }
        factory<IVpsInteractor> { VpsInteractor(get(), get(), get()) }
    }

    val presentation: Module = module {
        factory { ArManager() }
        factory { get<Context>().getSystemService(LocationManager::class.java) }
        factory { get<Context>().getSystemService(SensorManager::class.java) }
        factory { CompassManager(get()) }
        single { CoordinateConverter() }
        factory<VpsService> { VpsServiceImpl(get(), get(), get(), get(), get()) }
        factory { VpsArViewModel(get(), get(), get()) }
    }

}