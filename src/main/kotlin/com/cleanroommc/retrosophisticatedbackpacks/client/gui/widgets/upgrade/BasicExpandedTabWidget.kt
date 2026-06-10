package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.upgrade

import com.cleanroommc.modularui.widgets.layout.Column
import com.cleanroommc.modularui.widgets.layout.Row
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.IBasicFilterable
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.UpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.sync.UpgradeSlotSH
import net.minecraft.item.ItemStack

open class BasicExpandedTabWidget<T>(
    slotIndex: Int,
    wrap: T,
    delegatedIconStack: ItemStack,
    titleKey: String,
    filterSyncKey: String = "common_filter",
    coveredTabSize: Int = 4,
    width: Int = 75,
    upstreamLayout: Boolean = false,
    contentX: Int = 8,
    contentY: Int = 28,
    contentWidth: Int = 64,
    contentPadding: Int = 2,
    filterWidth: Int = 64,
    showFilterButton: Boolean = true,
    slotFactory: (Int, () -> UpgradeSlotSH?) -> PhantomItemSlot = { _, _ -> PhantomItemSlot() },
) : ExpandedUpgradeTabWidget<T>(slotIndex, wrap, coveredTabSize, delegatedIconStack, titleKey, width)
        where T : IBasicFilterable, T : UpgradeWrapper<*> {
    protected val startingRow: Row = Row()
        .height(0)
        .name("starting_row") as Row
    protected val filterWidget: BasicFilterWidget = BasicFilterWidget(wrap, slotIndex, filterSyncKey, upstreamLayout, showFilterButton, slotFactory)
        .width(filterWidth)
        .coverChildrenHeight()
        .name("filter_widget")

    override fun onWrapperChange(after: T) {
        super.onWrapperChange(after)
        filterWidget.filterableWrapper = after
    }

    init {
        val column = Column()
            .pos(contentX, contentY)
            .width(contentWidth)
            .childPadding(contentPadding)
            .child(startingRow)
            .child(filterWidget)

        child(column)
    }
}
