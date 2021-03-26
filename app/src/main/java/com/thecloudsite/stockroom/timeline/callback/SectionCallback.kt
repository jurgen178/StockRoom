package com.thecloudsite.stockroom.timeline.callback

import com.thecloudsite.stockroom.timeline.model.SectionInfo

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