package com.whtis.memosly.core.common

import android.net.Uri
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries media URIs from a share intent to the memo editor.
 *
 * These can't ride nav arguments the way shared text does: a share may hand over several
 * URIs at once, some providers emit URIs long enough to bloat the route string, and the
 * read grant belongs to the receiving activity rather than to anything encoded in a route.
 */
@Singleton
class SharedMediaBuffer @Inject constructor() {

    private val pending = AtomicReference<List<Uri>>(emptyList())

    fun put(uris: List<Uri>) {
        pending.set(uris)
    }

    /** Returns the buffered URIs and clears them, so a re-created editor can't replay the share. */
    fun take(): List<Uri> = pending.getAndSet(emptyList())
}
