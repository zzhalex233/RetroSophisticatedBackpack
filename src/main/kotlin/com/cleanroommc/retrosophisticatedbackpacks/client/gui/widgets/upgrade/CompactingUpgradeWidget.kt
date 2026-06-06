package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.upgrade

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.AdvancedCompactingUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.CompactingUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.RSBTextures
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.CyclicVariantButtonWidget
import com.cleanroommc.retrosophisticatedbackpacks.sync.UpgradeSlotSH
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.item.ItemStack

class CompactingUpgradeWidget(slotIndex: Int, wrapper: CompactingUpgradeWrapper, stack: ItemStack) :
    BasicExpandedTabWidget<CompactingUpgradeWrapper>(slotIndex, wrapper, stack, wrapper.settingsLangKey) {
    init {
        startingRow
            .height(20)
            .child(createCompactModeButton(wrapper.compactNonUncraftable))
            .child(createWorkInGuiButton(wrapper.shouldWorkInGui) {
                wrapper.shouldWorkInGui = !wrapper.shouldWorkInGui
                slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_COMPACT_WORK_IN_GUI) {}
            })
    }

    private fun createCompactModeButton(compactNonUncraftable: Boolean): CyclicVariantButtonWidget =
        CyclicVariantButtonWidget(COMPACT_MODE_VARIANTS, if (compactNonUncraftable) 1 else 0) {
            wrapper.compactNonUncraftable = !wrapper.compactNonUncraftable
            slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_COMPACT_NON_UNCRAFTABLE) {}
        }
}

class AdvancedCompactingUpgradeWidget(slotIndex: Int, wrapper: AdvancedCompactingUpgradeWrapper, stack: ItemStack) :
    AdvancedExpandedTabWidget<AdvancedCompactingUpgradeWrapper>(slotIndex, wrapper, stack, wrapper.settingsLangKey) {
    init {
        startingRow
            .height(20)
            .child(createCompactModeButton(wrapper.compactNonUncraftable))
            .child(createWorkInGuiButton(wrapper.shouldWorkInGui) {
                wrapper.shouldWorkInGui = !wrapper.shouldWorkInGui
                slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_COMPACT_WORK_IN_GUI) {}
            })
    }

    private fun createCompactModeButton(compactNonUncraftable: Boolean): CyclicVariantButtonWidget =
        CyclicVariantButtonWidget(COMPACT_MODE_VARIANTS, if (compactNonUncraftable) 1 else 0) {
            wrapper.compactNonUncraftable = !wrapper.compactNonUncraftable
            slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_COMPACT_NON_UNCRAFTABLE) {}
        }
}

private val COMPACT_MODE_VARIANTS =
        listOf(
            CyclicVariantButtonWidget.Variant(IKey.lang("gui.compact_uncraftable_only".asTranslationKey()), RSBTextures.CHECK_ICON),
            CyclicVariantButtonWidget.Variant(IKey.lang("gui.compact_any_recipe".asTranslationKey()), RSBTextures.CROSS_ICON),
        )

private fun createWorkInGuiButton(shouldWorkInGui: Boolean, toggle: () -> Unit): CyclicVariantButtonWidget =
    CyclicVariantButtonWidget(WORK_IN_GUI_VARIANTS, if (shouldWorkInGui) 1 else 0) {
        toggle()
    }

private val WORK_IN_GUI_VARIANTS =
    listOf(
        CyclicVariantButtonWidget.Variant(IKey.lang("gui.work_in_gui_disabled".asTranslationKey()), RSBTextures.CROSS_ICON),
        CyclicVariantButtonWidget.Variant(IKey.lang("gui.work_in_gui_enabled".asTranslationKey()), RSBTextures.CHECK_ICON),
    )
