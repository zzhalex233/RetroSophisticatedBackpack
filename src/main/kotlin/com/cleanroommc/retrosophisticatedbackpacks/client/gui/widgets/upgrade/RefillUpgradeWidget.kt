package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.upgrade

import com.cleanroommc.modularui.api.drawable.IDrawable
import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.drawable.ItemDrawable
import com.cleanroommc.modularui.widgets.layout.Row
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.AdvancedRefillUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.RefillUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.RSBTextures
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.CyclicVariantButtonWidget
import com.cleanroommc.retrosophisticatedbackpacks.sync.UpgradeSlotSH
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

class RefillUpgradeWidget(slotIndex: Int, wrapper: RefillUpgradeWrapper, stack: ItemStack) :
    BasicExpandedTabWidget<RefillUpgradeWrapper>(slotIndex, wrapper, stack, wrapper.settingsLangKey) {
    init {
        startingRow.height(0)
    }
}

class AdvancedRefillUpgradeWidget(slotIndex: Int, wrapper: AdvancedRefillUpgradeWrapper, stack: ItemStack) :
    BasicExpandedTabWidget<AdvancedRefillUpgradeWrapper>(slotIndex, wrapper, stack, wrapper.settingsLangKey, width = 130) {
    init {
        startingRow
            .height(42)
            .childPadding(1)
            .child(targetRow(wrapper, 0, 6))
            .child(targetRow(wrapper, 6, 12))
    }

    private fun targetRow(wrapper: AdvancedRefillUpgradeWrapper, start: Int, end: Int): Row {
        val row = Row().height(20).childPadding(1) as Row
        for (filterSlot in start until end) {
            row.child(createTargetButton(wrapper, filterSlot))
        }
        return row
    }

    private fun createTargetButton(wrapper: AdvancedRefillUpgradeWrapper, filterSlot: Int): CyclicVariantButtonWidget =
        CyclicVariantButtonWidget(
            RefillUpgradeWrapper.TargetSlot.entries.map { CyclicVariantButtonWidget.Variant(it.langKey(), it.icon()) },
            wrapper.getTargetSlot(filterSlot).ordinal,
            iconOffset = 4,
            iconSize = 12,
            buttonWidth = 18,
            buttonHeight = 18
        ) { index ->
            wrapper.setTargetSlot(filterSlot, RefillUpgradeWrapper.TargetSlot.entries[index])
            slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_REFILL_TARGET_SLOT) {
                it.writeInt(filterSlot)
                it.writeEnumValue(RefillUpgradeWrapper.TargetSlot.entries[index])
            }
        }
}

private fun RefillUpgradeWrapper.TargetSlot.langKey(): IKey =
    IKey.lang("gui.refill_target_${name.lowercase()}".asTranslationKey())

private fun RefillUpgradeWrapper.TargetSlot.icon(): IDrawable =
    when (this) {
        RefillUpgradeWrapper.TargetSlot.ANY -> RSBTextures.SMALL_A_ICON
        RefillUpgradeWrapper.TargetSlot.MAIN_HAND -> RSBTextures.SMALL_M_ICON
        RefillUpgradeWrapper.TargetSlot.OFF_HAND -> RSBTextures.SMALL_O_ICON
        else -> ItemDrawable(ItemStack(Items.PAPER, 1, ordinal - RefillUpgradeWrapper.TargetSlot.HOTBAR_1.ordinal + 1))
    }
