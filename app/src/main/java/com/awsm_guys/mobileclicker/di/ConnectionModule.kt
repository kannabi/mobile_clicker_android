package com.awsm_guys.mobileclicker.di

import android.content.Context
import com.awsm_guys.mobileclicker.connection.IConnectionModel
import com.awsm_guys.mobileclicker.connection.IConnectionPresenter
import com.awsm_guys.mobileclicker.connection.model.ConnectionModel
import com.awsm_guys.mobileclicker.connection.model.manager.ConnectionManager
import com.awsm_guys.mobileclicker.connection.model.manager.lan.LanConnectionManager
import com.awsm_guys.mobileclicker.connection.presenter.ConnectionPresenter
import com.awsm_guys.mobileclicker.di.scopes.ConnectionScope
import com.awsm_guys.mobileclicker.primitivestore.PrimitiveStore
import dagger.Module
import dagger.Provides

@Module
class ConnectionModule {
    @ConnectionScope @Provides
    fun providePresenter(model: IConnectionModel, context: Context): IConnectionPresenter =
            ConnectionPresenter(model, context)

    @ConnectionScope @Provides
    fun provideModel(
            primitiveStore: PrimitiveStore, context: Context, connectionManager: ConnectionManager
    ): IConnectionModel =
            ConnectionModel(primitiveStore, context, connectionManager)

    @ConnectionScope @Provides
    fun provideConnectionManager() = LanConnectionManager() as ConnectionManager
}