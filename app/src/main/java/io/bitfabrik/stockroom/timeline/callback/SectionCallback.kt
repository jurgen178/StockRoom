package io.bitfabrik.stockroom.timeline.callback

import io.bitfabrik.stockroom.timeline.model.SectionInfo

interface SectionCallback {
    /**
     * To check if section is
     */
    fun isSection(position: Int): Boolean

    /**
     * Functions that return a section header in a section
     */
    fun getSectionHeader(position: Int): SectionInfo?
}