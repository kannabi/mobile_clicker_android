package com.awsm_guys.mobileclicker.clicker.model

import com.awsm_guys.mobileclicker.clicker.IClickerModel
import com.awsm_guys.mobileclicker.clicker.model.controller.DesktopController
import com.awsm_guys.mobileclicker.clicker.model.controller.DesktopControllerFactory
import com.awsm_guys.mobileclicker.clicker.model.controller.DesktopControllerFactoryGenerator
import com.awsm_guys.mobileclicker.clicker.model.events.*
import com.awsm_guys.mobileclicker.primitivestore.CONTROLLER_TAG_KEY
import com.awsm_guys.mobileclicker.primitivestore.PrimitiveStore
import com.awsm_guys.mobileclicker.utils.LoggingMixin
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class ClickerModel(
        private val primitiveStore: PrimitiveStore,
        private val controllerFactory: DesktopControllerFactory? = null
) : IClickerModel, LoggingMixin {

    private val clickerEventSubject = BehaviorSubject.create<ClickerEvent>()
    private val controllerFactoryGenerator by lazy { DesktopControllerFactoryGenerator() }
    private lateinit var desktopController: DesktopController
    private val compositeDisposable = CompositeDisposable()

    private var currentPage: Int = 0
    private lateinit var currentMeta: MetaUpdate
    private var connected = false


    override fun connect(): Observable<ClickerEvent> =
        Observable.fromCallable {
            desktopController =
                    (controllerFactory ?:
                        controllerFactoryGenerator.generate(
                                primitiveStore.find(CONTROLLER_TAG_KEY)!!
                        )!!
                    ).create(primitiveStore)

            desktopController.init()
            subscribeToDesktopController()
            connected = true
        }
        .doOnNext { clickerEventSubject.onNext(ConnectionOpen()) }
        .flatMap { clickerEventSubject.hide() }

    override fun disconnect() {
        if (::desktopController.isInitialized) {
            desktopController.disconnect()
            primitiveStore.remove(CONTROLLER_TAG_KEY)
        }
        compositeDisposable.clear()
    }

    private fun subscribeToDesktopController() {
        compositeDisposable.add(
                desktopController.getPageSwitchingObservable()
                        .subscribeOn(Schedulers.io())
                        .map { PageSwitch(it.also(::currentPage::set)) }
                        .subscribe(clickerEventSubject::onNext, {
                            trace(it)
                            clickerEventSubject.onNext(ClickerBroken())
                        }, {
                            clickerEventSubject.onNext(ConnectionClose())
                        })
        )

        compositeDisposable.add(
                desktopController.getMetaUpdateObservable()
                        .subscribeOn(Schedulers.io())
                        .map(::MetaUpdate)
                        .doOnNext(::currentMeta::set)
                        .subscribe(clickerEventSubject::onNext, ::trace)
        )
    }

    override fun switchPage(number: Int) = desktopController.switchPage(number)

    override fun onNextClick() = switchPage(currentPage + 1)

    override fun onPreviousClick() = switchPage(currentPage - 1)

    override fun isConnected() = connected && desktopController.isConnected()

    override fun restoreState(): Completable =
            Completable.fromAction {
                clickerEventSubject.onNext(currentMeta)
                clickerEventSubject.onNext(PageSwitch(currentPage))
            }
}