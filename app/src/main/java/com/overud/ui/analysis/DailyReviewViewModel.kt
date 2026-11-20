package com.overud.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overud.analytics.DailyOfferSummary
import com.overud.analytics.LoggedOfferItem
import com.overud.analytics.OrderQuality
import com.overud.data.repository.OfferHistoryRepository
import com.overud.model.OfferChannel
import com.overud.model.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class DailyDateRange(val label: String) { TODAY("今天"), YESTERDAY("昨天"), LAST_7_DAYS("近7天") }
enum class DailyItemFilter(val label: String) {
    ALL("全部"), GOOD("好单"), LOW("垃圾单"), FAKE_PREFERRED("假好单"), RADAR("雷达单")
}

data class DailyReviewUiState(
    val range: DailyDateRange = DailyDateRange.TODAY,
    val platform: Platform? = null,
    val filter: DailyItemFilter = DailyItemFilter.ALL,
    val summary: DailyOfferSummary = DailyOfferSummary(),
    val items: List<LoggedOfferItem> = emptyList(),
)

class DailyReviewViewModel(private val repository: OfferHistoryRepository) : ViewModel() {
    private val range = MutableStateFlow(DailyDateRange.TODAY)
    private val platform = MutableStateFlow<Platform?>(null)
    private val filter = MutableStateFlow(DailyItemFilter.ALL)

    val state: StateFlow<DailyReviewUiState> = combine(
        repository.revision, range, platform, filter,
    ) { _, selectedRange, selectedPlatform, selectedFilter ->
        Triple(selectedRange, selectedPlatform, selectedFilter)
    }.map { (selectedRange, selectedPlatform, selectedFilter) ->
        withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            val (start, end) = when (selectedRange) {
                DailyDateRange.TODAY -> today to today
                DailyDateRange.YESTERDAY -> today.minusDays(1) to today.minusDays(1)
                DailyDateRange.LAST_7_DAYS -> today.minusDays(6) to today
            }
            val fmt = DateTimeFormatter.ISO_LOCAL_DATE
            val raw = repository.itemsByDateRange(start.format(fmt), end.format(fmt))
            val byPlatform = if (selectedPlatform == null) raw else raw.filter { it.platform == selectedPlatform }
            val summary = DailyOfferSummary.from(byPlatform)
            val filtered = when (selectedFilter) {
                DailyItemFilter.ALL -> byPlatform
                DailyItemFilter.GOOD -> byPlatform.filter { it.quality == OrderQuality.GOOD }
                DailyItemFilter.LOW -> byPlatform.filter { it.quality == OrderQuality.LOW }
                DailyItemFilter.FAKE_PREFERRED -> byPlatform.filter { it.fakePreferred }
                DailyItemFilter.RADAR -> byPlatform.filter { it.channel == OfferChannel.TRIP_RADAR }
            }
            DailyReviewUiState(selectedRange, selectedPlatform, selectedFilter, summary, filtered)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyReviewUiState())

    fun selectRange(value: DailyDateRange) { range.value = value }
    fun selectPlatform(value: Platform?) { platform.value = value }
    fun selectFilter(value: DailyItemFilter) { filter.value = value }
    fun clearAll() { viewModelScope.launch(Dispatchers.IO) { repository.clearAll() } }
}
