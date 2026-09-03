package cz.b2brental

import android.app.Application
import cz.b2brental.di.networkModule
import cz.b2brental.di.repositoryModule
import cz.b2brental.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Vstupní bod aplikace B2B Rental.
 * Inicializuje Koin DI kontext se síťovým, repozitářovým a ViewModel modulem.
 */
public class B2bApplication : Application() {

    /**
     * Vytvoří a nakonfiguruje Koin DI kontext při spuštění aplikace.
     */
    override fun onCreate(): Unit {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@B2bApplication)
            modules(networkModule, repositoryModule, viewModelModule)
        }
    }
}
