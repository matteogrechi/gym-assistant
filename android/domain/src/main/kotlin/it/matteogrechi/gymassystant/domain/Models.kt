package it.matteogrechi.gymassystant.domain

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

// GYM-004/010/024: kg remain exact decimal JSON numbers through storage and metrics.
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object DecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("Decimal", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        require(encoder is JsonEncoder) { "Decimals require JSON" }
        encoder.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
    }
    override fun deserialize(decoder: Decoder): BigDecimal {
        require(decoder is JsonDecoder) { "Decimals require JSON" }
        val number = decoder.decodeJsonElement() as? JsonPrimitive ?: error("Expected decimal number")
        require(!number.isString && number.content.matches(Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")))
        require(number.content.length <= 128) { "Decimal too long" }
        return BigDecimal(number.content).also {
            require(it.precision() <= 128 && it.scale() in -128..128) { "Decimal out of bounds" }
        }
    }
}

@Serializable enum class Language { SYSTEM, EN, IT, ES }
@Serializable enum class Theme { SYSTEM, LIGHT, DARK }
@Serializable enum class Lifecycle { READY, IN_PROGRESS, PAUSED, FINISHED }
@Serializable enum class SetKind { WORKING, WARM_UP }
@Serializable enum class TimerMode { EXECUTION, RECOVERY }
enum class SessionAction { START, PAUSE, RESUME, FINISH }
enum class TimerAction { START, PAUSE, RESET }

// GYM-022/027: preferences contain no account or remote configuration.
@Serializable data class AppSettings(
    val language: Language = Language.SYSTEM,
    val theme: Theme = Theme.SYSTEM,
    val referenceWeights: List<ReferenceWeight> = emptyList(),
    val backupTreeUri: String? = null,
    val backupLabel: String? = null,
)
typealias Settings = AppSettings

@Serializable data class MediaMetadata(
    val id: String,
    val exerciseId: String,
    val originalName: String,
    val mime: String,
    val size: Long,
    val sha256: String,
    val relativePath: String,
)

@Serializable data class ComparisonContext(val exerciseId: String, val setup: String)
@Serializable data class ReferenceWeight(
    val context: ComparisonContext,
    @Serializable(with = DecimalSerializer::class) val kg: BigDecimal,
)

// GYM-028: definitions are append-only; occurrence setup is a separate copy.
@Serializable data class ExerciseDefinition(
    val id: String,
    val name: String,
    val machineVariant: String,
    val defaultSetup: String = "",
    val mediaId: String? = null,
)

// GYM-018/026: prescriptions and retained drafts never contain actual measurements.
@Serializable data class Prescription(
    val id: String,
    val exerciseId: String,
    val workingSets: Int = 3,
    val minReps: Int = 8,
    val maxReps: Int = 12,
    val recoverySeconds: Int = 90,
)
@Serializable data class SessionTemplate(
    val id: String,
    val name: String,
    val focus: String = "",
    val exercises: List<Prescription>,
)
@Serializable data class Plan(
    val id: String,
    val revisionId: String,
    val name: String,
    val sessions: List<SessionTemplate>,
    val weeklySchedule: Map<Int, String> = emptyMap(),
    val dateOverrides: Map<String, String?> = emptyMap(),
)
@Serializable data class PlanDraft(val plan: Plan, val baseRevisionId: String? = null)
@Serializable data class EffectiveSchedule(val effectiveFrom: String, val plan: Plan)

// GYM-016/020: an observed date/zone is not recomputed from the original planned date.
@Serializable data class ObservationTime(val instant: String, val localDate: String, val timeZone: String)
data class ClockReading(val observation: ObservationTime, val monotonicMs: Long, val bootId: String?)

// GYM-012/024: a running anchor is meaningful only on the same verifiable boot.
@Serializable data class TimerState(
    val elapsedMs: Long = 0,
    val anchorMonotonicMs: Long? = null,
    val bootId: String? = null,
    val interrupted: Boolean = false,
    val capturedRunning: Boolean = false,
) {
    val running: Boolean get() = anchorMonotonicMs != null
}

// GYM-004/005/014: unrecorded actual values are null, including newly created warm-ups.
@Serializable data class SetLog(
    val id: String,
    val kind: SetKind,
    val reps: Int? = null,
    @Serializable(with = DecimalSerializer::class) val kg: BigDecimal? = null,
    val done: Boolean = false,
    val observedAt: ObservationTime? = null,
)
@Serializable data class ExerciseOccurrence(
    val prescription: Prescription,
    val definition: ExerciseDefinition,
    val setup: String,
    val sets: List<SetLog>,
    val executionTimer: TimerState = TimerState(),
    val recoveryTimer: TimerState = TimerState(),
)

// GYM-017/019/026: snapshots retain their original owner, prescription and stable order IDs.
@Serializable data class Occurrence(
    val id: String,
    val plannedDate: String,
    val planId: String,
    val planRevisionId: String,
    val planName: String,
    val sessionId: String,
    val sessionName: String,
    val focus: String,
    val exercises: List<ExerciseOccurrence>,
    val lifecycle: Lifecycle = Lifecycle.READY,
    val protected: Boolean = false,
    val firstVisit: ObservationTime? = null,
    val finishedAt: ObservationTime? = null,
)

// GYM-020/023/024/027: createdDate excludes pre-install obligations; history starts empty.
@Serializable data class Dataset(
    val id: String,
    val createdDate: String,
    val revision: Long = 0,
    val format: String = "gym-assystant-status",
    val formatVersion: Int = 3,
    val dataKind: String = "user_data",
    val settings: Settings = Settings(),
    val exercises: List<ExerciseDefinition> = emptyList(),
    val media: List<MediaMetadata> = emptyList(),
    val plans: List<Plan> = emptyList(),
    val activePlanId: String? = null,
    val drafts: List<PlanDraft> = emptyList(),
    val schedules: List<EffectiveSchedule> = emptyList(),
    val occurrences: List<Occurrence> = emptyList(),
)
