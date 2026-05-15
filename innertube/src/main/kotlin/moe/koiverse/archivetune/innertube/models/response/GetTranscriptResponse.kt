/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package moe.koiverse.archivetune.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptResponse(
    val actions: List<Action>?,
) {
    @Serializable
    data class Action(
        val updateEngagementPanelAction: UpdateEngagementPanelAction,
    ) {
        @Serializable
        data class UpdateEngagementPanelAction(
            val content: Content,
        ) {
            @Serializable
            data class Content(
                val transcriptRenderer: TranscriptRenderer,
            ) {
                @Serializable
                data class TranscriptRenderer(
                    val body: Body,
                ) {
                    @Serializable
                    data class Body(
                        val transcriptBodyRenderer: TranscriptBodyRenderer,
                    ) {
                        @Serializable
                        data class TranscriptBodyRenderer(
                            val cueGroups: List<CueGroup>,
                        ) {
                            @Serializable
                            data class CueGroup(
                                val transcriptCueGroupRenderer: TranscriptCueGroupRenderer,
                            ) {
                                @Serializable
                                data class TranscriptCueGroupRenderer(
                                    val cues: List<Cue>,
                                ) {
                                    @Serializable
                                    data class Cue(
                                        val transcriptCueRenderer: TranscriptCueRenderer,
                                    ) {
                                        @Serializable
                                        data class TranscriptCueRenderer(
                                            val cue: SimpleText,
                                            val startOffsetMs: Long,
                                            val durationMs: Long,
                                        ) {
                                            @Serializable
                                            data class SimpleText(
                                                val simpleText: String,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
