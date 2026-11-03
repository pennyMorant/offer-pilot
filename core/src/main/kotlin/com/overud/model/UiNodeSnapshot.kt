package com.overud.model

data class NodeBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val valid: Boolean get() = right > left && bottom > top
    fun contains(other: NodeBounds) = valid && other.valid && left <= other.left && top <= other.top &&
        right >= other.right && bottom >= other.bottom
}

/** Transient UI structure; never stored in the database. Indices refer to preorder traversal. */
data class UiNodeSnapshot(
    val index: Int,
    val parentIndex: Int,
    val text: String?,
    val description: String?,
    val viewId: String?,
    val packageName: String?,
    val windowId: Int,
    val bounds: NodeBounds,
    val visible: Boolean,
    val enabled: Boolean,
    val clickable: Boolean,
)

data class NodePageText(val text: String, val selectedRoot: Int? = null)
