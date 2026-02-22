package com.whtis.memosly.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val memosDispatcher: MemosDispatchers)

enum class MemosDispatchers {
    Default,
    IO,
}
