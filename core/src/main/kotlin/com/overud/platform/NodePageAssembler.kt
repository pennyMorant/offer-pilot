package com.overud.platform

import com.overud.model.*

object NodePageAssembler {
    fun assemble(platform: Platform, nodes: List<UiNodeSnapshot>): NodePageText {
        val root = nodes.firstOrNull() ?: return NodePageText("")
        val visible = nodes.filter { it.visible && it.packageName == platform.packageName &&
            it.windowId == root.windowId && it.bounds.valid }
        val full = text(visible)
        val fallback = NodePageText(full)
        // A subtree must not hide history, completion or other contradictory page evidence.
        if (OfferParser.classify(platform, full).type != PlatformPageType.OFFER) return fallback
        val actions = visible.filter { node -> node.enabled &&
            (AcceptanceActionPolicy.matchesInitialButton(platform, node.text) ||
                AcceptanceActionPolicy.matchesInitialButton(platform, node.description)) }
        val action = actions.singleOrNull() ?: return fallback
        val byIndex = nodes.associateBy { it.index }
        var ancestor = action.parentIndex
        repeat(8) {
            val container = byIndex[ancestor] ?: return fallback
            if (container.parentIndex < 0) return fallback
            val members = visible.filter { descendant(it, ancestor, byIndex) }
            if (members.isNotEmpty() && members.all { container.bounds.contains(it.bounds) }) {
                val body = text(members)
                val offer = OfferParser.parse(platform, body, 0)
                if (offer?.reliable == true && !offer.isAddon && !offer.isStacked &&
                    !offer.merchant.isNullOrBlank() && !offer.destination.isNullOrBlank()) {
                    return NodePageText(body, ancestor)
                }
            }
            ancestor = container.parentIndex
        }
        return fallback
    }

    private fun descendant(node: UiNodeSnapshot, root: Int, nodes: Map<Int, UiNodeSnapshot>): Boolean {
        var index = node.index
        repeat(36) {
            if (index == root) return true
            val parent = nodes[index]?.parentIndex ?: return false
            if (parent < 0 || parent >= index) return false
            index = parent
        }
        return false
    }

    private fun text(nodes: List<UiNodeSnapshot>) = nodes.flatMap { listOfNotNull(it.text, it.description) }
        .filter(String::isNotBlank).distinct().joinToString("\n")
}
