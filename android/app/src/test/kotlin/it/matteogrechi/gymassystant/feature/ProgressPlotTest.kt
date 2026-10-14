package it.matteogrechi.gymassystant.feature

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class ProgressPlotTest {
    private fun observation(id: String, date: String, value: String?) =
        ProgressPlotObservation(id, date, value?.let(::BigDecimal))

    @Test fun irregularDatesUseElapsedDaysAndKeepTouchTargetsSeparate() {
        val layout = progressPlotLayout(listOf(
            observation("a", "2026-09-01", "8"), observation("b", "2026-09-02", "9"),
            observation("c", "2026-09-12", "10")), 240f, 1f)
        val distances = layout.points.zipWithNext().map { (a, b) -> b.x - a.x }
        assertTrue(distances[0] >= 48f)
        assertEquals(10f, distances[1] / distances[0], .0001f)
        assertTrue(layout.width > 240f)
    }

    @Test fun sameDayRecordsRetainIdentityAndDatesWithoutOverlappingTargets() {
        val layout = progressPlotLayout(listOf(
            observation("b", "2026-09-02", "12"), observation("a", "2026-09-02", "10"),
            observation("c", "2026-09-03", "13")), 220f, 1f)
        assertEquals(listOf("a", "b", "c"), layout.points.map { it.observation.id })
        assertEquals("2026-09-02", layout.points[0].observation.date)
        assertEquals("2026-09-02", layout.points[1].observation.date)
        layout.points.zipWithNext().forEach { (a, b) -> assertTrue(b.x - a.x >= 48f) }
        assertEquals(0, progressNearest(layout.points, layout.points[0].x))
        assertEquals(1, progressNearest(layout.points, layout.points[1].x))
    }

    @Test fun missingValuesBreakLinesWhileRecordedZeroRemainsAValue() {
        val layout = progressPlotLayout(listOf(
            observation("a", "2026-09-01", "0"), observation("b", "2026-09-02", null),
            observation("c", "2026-09-03", "0.125"), observation("d", "2026-09-04", "0.25")), 240f, 1f)
        assertEquals(BigDecimal("0"), layout.points[0].observation.value)
        assertEquals(1f, layout.points[0].y!!, 0f)
        assertNull(layout.points[1].y)
        assertEquals(listOf(2 to 3), layout.segments)
        assertEquals(BigDecimal("0.125"), layout.points[2].observation.value)
        assertEquals(0, layout.ticks.last().signum())
        assertTrue(layout.maximum < BigDecimal.ONE)
    }

    @Test fun temporalZoomDoesNotChangeValuesOrValueAxis() {
        val points = listOf(observation("a", "2026-09-01", "10.000000000000000001"),
            observation("b", "2026-09-03", "11.125"))
        val normal = progressPlotLayout(points, 240f, 1f)
        val zoomed = progressPlotLayout(points, 240f, 4f)
        assertEquals(normal.ticks, zoomed.ticks)
        assertEquals(normal.maximum, zoomed.maximum)
        assertEquals(normal.points.map { it.observation.value }, zoomed.points.map { it.observation.value })
        assertEquals(4f, (zoomed.points[1].x - zoomed.points[0].x) /
            (normal.points[1].x - normal.points[0].x), .0001f)
        assertThrows(IllegalArgumentException::class.java) { progressPlotLayout(points, 240f, 16f) }
    }

    @Test fun revealPreservesDeliberatePanAndClampsBothEndpoints() {
        assertEquals(120f, progressReveal(180f, 120f, 240f, 1000f), 0f)
        assertEquals(0f, progressReveal(24f, 120f, 240f, 1000f), 0f)
        assertEquals(760f, progressReveal(976f, 120f, 240f, 1000f), 0f)
        assertEquals(0f, progressReveal(120f, 0f, 240f, 240f), 0f)
    }

    @Test fun emptyAndSingleObservationAreSafeAndMissingStaysMissing() {
        val empty = progressPlotLayout(emptyList(), 240f, 1f)
        assertTrue(empty.points.isEmpty())
        assertEquals(-1, progressNearest(empty.points, 0f))
        val single = progressPlotLayout(listOf(observation("a", "2026-09-01", null)), 240f, 1f)
        assertEquals(120f, single.points.single().x, 0f)
        assertNull(single.points.single().y)
        assertTrue(single.segments.isEmpty())
    }
}
