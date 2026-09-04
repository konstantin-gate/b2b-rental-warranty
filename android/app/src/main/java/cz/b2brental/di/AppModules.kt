package cz.b2brental.di

import cz.b2brental.data.local.B2bDatabase
import cz.b2brental.data.local.DataStoreTokenStorage
import cz.b2brental.data.local.TokenStorage
import cz.b2brental.data.remote.B2bApiClient
import cz.b2brental.data.remote.SessionClearer
import cz.b2brental.data.remote.SessionClearerImpl
import cz.b2brental.data.remote.createB2bHttpClient
import cz.b2brental.data.repository.AuthRepositoryImpl
import cz.b2brental.data.repository.CatalogRepositoryImpl
import cz.b2brental.data.repository.ContractRepositoryImpl
import cz.b2brental.data.repository.PaymentRepositoryImpl
import cz.b2brental.data.repository.TicketRepositoryImpl
import cz.b2brental.domain.repository.AuthRepository
import cz.b2brental.domain.repository.CatalogRepository
import cz.b2brental.domain.repository.ContractRepository
import cz.b2brental.domain.repository.PaymentRepository
import cz.b2brental.domain.repository.TicketRepository
import cz.b2brental.domain.util.DefaultPhotoEncoder
import cz.b2brental.domain.util.PhotoEncoder
import cz.b2brental.presentation.SessionViewModel
import cz.b2brental.presentation.feature.auth.LoginViewModel
import cz.b2brental.presentation.feature.auth.RegisterViewModel
import cz.b2brental.presentation.feature.catalog.CatalogViewModel
import cz.b2brental.presentation.feature.catalog.EquipmentDetailViewModel
import cz.b2brental.presentation.feature.contract.ContractDetailViewModel
import cz.b2brental.presentation.feature.contract.ContractsViewModel
import cz.b2brental.presentation.feature.contract.CreateContractViewModel
import cz.b2brental.presentation.feature.document.PdfViewerViewModel
import cz.b2brental.presentation.feature.equipment.MyEquipmentViewModel
import cz.b2brental.presentation.feature.payment.PaymentsViewModel
import cz.b2brental.presentation.feature.ticket.ReportIssueViewModel
import cz.b2brental.presentation.feature.ticket.TicketDetailViewModel
import cz.b2brental.presentation.feature.ticket.TicketsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Modul síťové vrstvy: HTTP klient, TokenStorage, SessionClearer, API klient.
 * TokenStorage se registruje před SessionClearer, protože SessionClearerImpl závisí na TokenStorage.
 */
public val networkModule: Module = module {
    single { createB2bHttpClient() }
    single<TokenStorage> { DataStoreTokenStorage(androidContext()) }
    single<SessionClearer> { SessionClearerImpl(get()) }
    single { B2bApiClient(get(), get(), get()) }
    single { B2bDatabase.create(androidContext()) }
    single { get<B2bDatabase>().catalogDao() }
    single<PhotoEncoder> { DefaultPhotoEncoder() }
}

/**
 * Modul repozitářů — spravuje přístup k datům (auth session).
 */
public val repositoryModule: Module = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<CatalogRepository> { CatalogRepositoryImpl(get(), get()) }
    single<ContractRepository> { ContractRepositoryImpl(get()) }
    single<TicketRepository> { TicketRepositoryImpl(get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }
}

/**
 * Modul ViewModelů — spravuje UI stav a komunikaci s repozitáři.
 */
public val viewModelModule: Module = module {
    viewModelOf(::SessionViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::CatalogViewModel)
    viewModelOf(::EquipmentDetailViewModel)
    viewModelOf(::CreateContractViewModel)
    viewModelOf(::ContractsViewModel)
    viewModelOf(::ContractDetailViewModel)
    viewModelOf(::PdfViewerViewModel)
    viewModelOf(::MyEquipmentViewModel)
    viewModelOf(::ReportIssueViewModel)
    viewModelOf(::TicketsViewModel)
    viewModelOf(::TicketDetailViewModel)
    viewModelOf(::PaymentsViewModel)
}
