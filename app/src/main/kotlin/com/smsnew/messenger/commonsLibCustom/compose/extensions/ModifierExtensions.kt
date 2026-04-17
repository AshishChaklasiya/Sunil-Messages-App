package com.smsnew.messenger.commonsLibCustom.compose.extensions

import androidx.compose.ui.Modifier

inline fun Modifier.ifTrue(predicate: Boolean, builder: Modifier.() -> Modifier) =
    if (predicate) this.builder() else this

inline fun Modifier.ifFalse(predicate: Boolean, builder: Modifier.() -> Modifier) =
    if (!predicate) this.builder() else this

inline infix fun (() -> Unit).andThen(crossinline function: () -> Unit): () -> Unit = {
    this()
    function()
}
