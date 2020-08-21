package fi.thl.koronahaavi.common

sealed class RemoteResource<T>(
    val data: T? = null
) {
    class Ready<T>(data: T) : RemoteResource<T>(data)
    class Loading<T>(data: T? = null) : RemoteResource<T>(data)
    class Failed<T>(data: T? = null) : RemoteResource<T>(data)
}
