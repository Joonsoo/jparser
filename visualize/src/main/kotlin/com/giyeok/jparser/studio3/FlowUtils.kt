package com.giyeok.jparser.studio3

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlin.time.Duration

fun <T, U> Flow<T>.debounceWithStarter(
  duration: Duration,
  starter: Flow<U>,
  processor: suspend (T) -> U
): Flow<U> = this.flatMapLatest {
  flow {
    emitAll(starter)
    delay(duration)
    if (currentCoroutineContext().isActive) {
      println("Continued by debounceWithStarter")
      emit(processor(it))
    } else {
      println("Cancelled by debounceWithStarter")
    }
  }
}

sealed class DataUpdateEvent<T> {
  class NoDataAvailable<T> : DataUpdateEvent<T>()
  class InvalidateLatestData<T> : DataUpdateEvent<T>()
  data class NewDataAvailable<T>(val data: T) : DataUpdateEvent<T>()
  data class ExceptionThrown<T>(val throwable: Throwable) : DataUpdateEvent<T>()

  companion object {
    fun <U, T> processorFlow(
      source: Flow<U>,
      debounce: Duration,
      processor: suspend (U) -> T
    ): Flow<DataUpdateEvent<T>> =
      flowOf<DataUpdateEvent<T>>(NoDataAvailable()).onCompletion {
        emitAll(source.debounceWithStarter(
          debounce,
          flowOf(InvalidateLatestData())
        ) { sourceData ->
          try {
            val processed = processor(sourceData)
            NewDataAvailable(processed)
          } catch (e: Throwable) {
            ExceptionThrown(e)
          }
        })
      }
  }
}

fun <T> scala.collection.immutable.List<T>.toKtList(): List<T> =
  List<T>(this.size()) { idx -> this.apply(idx) }

fun <T> scala.collection.immutable.Seq<T>.toKtList(): List<T> =
  List<T>(this.size()) { idx -> this.apply(idx) }
