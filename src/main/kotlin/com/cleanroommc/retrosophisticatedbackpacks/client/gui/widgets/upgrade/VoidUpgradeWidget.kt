package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.upgrade

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.AdvancedVoidUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.VoidType
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.VoidUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.RSBTextures
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.CyclicVariantButtonWidget
import com.cleanroommc.retrosophisticatedbackpacks.sync.UpgradeSlotSH
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.item.ItemStack

class VoidUpgradeWidget(slotIndex: Int, wrapper: VoidUpgradeWrapper, stack: ItemStack) :
    BasicExpandedTabWidget<VoidUpgradeWrapper>(slotIndex, wrapper, stack, wrapper.settingsLangKey) {
    init {
        startingRow
            .height(20)
            .child(createVoidTypeButton(wrapper.voidType))
            .child(createWorkInGuiButton(wrapper.shouldWorkInGui) {
                wrapper.shouldWorkInGui = !wrapper.shouldWorkInGui
                slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_VOID_WORK_IN_GUI) {}
            })
    }

    private fun createVoidTypeButton(current: VoidType): CyclicVariantButtonWidget =
        CyclicVariantButtonWidget(VOID_TYPE_VARIANTS, current.ordinal) { index ->
            wrapper.voidType = VoidType.entries[index]
            slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_VOID_TYPE) {
                it.writeEnumValue(VoidType.entries[index])
            }
        }
}

class AdvancedVoidUpgradeWidget(slotIndex: Int, wrapper: AdvancedVoidUpgradeWrapper, stack: ItemStack) :
    AdvancedExpandedTabWidget<AdvancedVoidUpgradeWrapper>(slotIndex, wrapper, stack, wrapper.settingsLangKey) {
    init {
        startingRow
            .height(20)
            .child(createVoidTypeButton(wrapper.voidType))
            .child(createWorkInGuiButton(wrapper.shouldWorkInGui) {
                wrapper.shouldWorkInGui = !wrapper.shouldWorkInGui
                slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_VOID_WORK_IN_GUI) {}
            })
    }

    private fun createVoidTypeButton(current: VoidType): CyclicVariantButtonWidget =
        CyclicVariantButtonWidget(VOID_TYPE_VARIANTS, current.ordinal) { index ->
            wrapper.voidType = VoidType.entries[index]
            slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_VOID_TYPE) {
                it.writeEnumValue(VoidType.entries[index])
            }
        }
}

private val VOID_TYPE_VARIANTS =
        listOf(
            CyclicVariantButtonWidget.Variant(IKey.lang("gui.void_always".asTranslationKey()), RSBTextures.CROSS_ICON),
            CyclicVariantButtonWidget.Variant(IKey.lang("gui.void_slot_overflow".asTranslationKey()), RSBTextures.IN_ICON),
            CyclicVariantButtonWidget.Variant(IKey.lang("gui.void_storage_overflow".asTranslationKey()), RSBTextures.IN_OUT_ICON),
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
