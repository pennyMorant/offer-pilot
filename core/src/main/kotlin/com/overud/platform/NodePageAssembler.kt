package com.overud.platform

import com.overud.model.*

object NodePageAssembler {
    fun assemble(platform: Platform, nodes: List<UiNodeSnapshot>): NodePageText {
        val root = nodes.firstOrNull() ?: return NodePageText("")
        val visible = nodes.filter { it.visible && it.packageName == platform.packageName &&
            it.windowId == root.windowId && it.bounds.valid }
        val full = text(visible)
        var fallback = NodePageText(full)
        // A subtree must not hide history, completion or other contradictory page evidence.
        if (OfferParser.classify(platform, full).type != PlatformPageType.OFFER) return fallback
        if (OfferParser.parse(platform, full, 0)?.isStacked == true) return fallback
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
            if (container.visible && root.bounds.contains(container.bounds) &&
                members.isNotEmpty() && members.all { container.bounds.contains(it.bounds) }) {
                val body = text(members)
                val offer = OfferParser.parse(platform, body, 0)
                if (offer?.reliable == true && !offer.isAddon && !offer.isStacked &&
                    (!offer.merchant.isNullOrBlank() || !offer.pickupAddress.isNullOrBlank()) && !offer.destination.isNullOrBlank()) {
                    return NodePageText(body, ancestor)
                }
                if (fallback.ocrRoot == null && platform == Platform.UBER_EATS && UberOfferCard.hasAnchors(body)) {
                    fallback = fallback.copy(ocrRoot = ancestor)
                }
                // Partial node evidence may locate an OCR region without replacing the full-page text.
                if (fallback.ocrRoot == null && offer != null && !offer.isAddon && !offer.isStacked &&
                    offer.evidence?.amountVerified == true && (offer.hasVerifiedDistance() || offer.hasVerifiedDuration()) &&
                    (!offer.merchant.isNullOrBlank() || !offer.destination.isNullOrBlank())) {
                    fallback = fallback.copy(ocrRoot = ancestor)
                }
            }
            ancestor = container.parentIndex
        }
        val spatialRegions = visible.flatMap { node ->
            listOfNotNull(node.text, node.description).filter(String::isNotBlank).map { label ->
                TextRegion(label, node.bounds.left, node.bounds.top, node.bounds.right, node.bounds.bottom)
            }
        }
        val located = OfferCardLocator.locate(platform, spatialRegions)
        if ((located.amountRegion != null || located.actionRegion != null) && located.cardText.isNotBlank()) {
            return fallback.copy(text = located.cardText)
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

    private fun text(nodes: List<UiNodeSnapshot>) = nodes.sortedWith(compareBy<UiNodeSnapshot> { it.bounds.top }.thenBy { it.bounds.left }).flatMap { listOfNotNull(it.text, it.description) }
        .filter(String::isNotBlank).distinct().joinToString("\n")
}
