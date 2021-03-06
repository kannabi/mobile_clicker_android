
package com.awsm_guys.mobileclicker.connection.model

import android.content.Context
import com.awsm_guys.mobileclicker.App
import com.awsm_guys.mobileclicker.clicker.model.controller.DesktopControllerFactory
import com.awsm_guys.mobileclicker.clicker.model.controller.LAN_TAG
import com.awsm_guys.mobileclicker.connection.IConnectionModel
import com.awsm_guys.mobileclicker.connection.model.manager.ConnectionManager
import com.awsm_guys.mobileclicker.primitivestore.CONTROLLER_TAG_KEY
import com.awsm_guys.mobileclicker.primitivestore.CURRENT_NAME_KEY
import com.awsm_guys.mobileclicker.primitivestore.PrimitiveStore
import com.awsm_guys.mobileclicker.utils.LoggingMixin
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.net.SocketException

class ConnectionModel(
        private var primitiveStore: PrimitiveStore,
        private val context: Context,
        private val connectionManager: ConnectionManager
) : IConnectionModel, LoggingMixin {
    private var currentUsername: String? = null
    set(value) {
        connectionManager.setName(value ?: "")
        field = value
    }

    private var managerDisposable: Disposable? = null
    private val connectionSubject = PublishSubject.create<Unit>()

    override fun saveUsername(username: String) {
        if (currentUsername != username) {
            primitiveStore.store(CURRENT_NAME_KEY, username)
            currentUsername = username
        }
    }

    override fun startConnection(): Observable<Unit> {
        managerDisposable =
            connectionManager.startListening()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(::onConnected, ::onError)

        return connectionSubject.hide()
    }

    private fun onConnected(desktopControllerFactory: DesktopControllerFactory) {
        primitiveStore.store(CONTROLLER_TAG_KEY, LAN_TAG) // it should be stored in another place!
        (context as App).componentProvider.desktopControllerFactoryCache =
                desktopControllerFactory
        connectionManager.stopListening()
        managerDisposable?.dispose()
        connectionSubject.onComplete()
    }

    private fun onError(error: Throwable) {
        if (error is SocketException) {
            return
        }
        connectionSubject.onError(error)
    }

    override fun stopConnection() {
        connectionManager.stopListening()
    }

    override fun getCurrentUsername(): Single<String> =
            Single.just(
                    currentUsername ?:
                            primitiveStore.find(CURRENT_NAME_KEY).also(::currentUsername::set) ?:
                            ""
            )

    override fun onDestroy() {
        stopConnection()
        connectionManager.close()
        managerDisposable?.dispose()
    }
}