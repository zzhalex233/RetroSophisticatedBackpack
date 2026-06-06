package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.upgrade

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.AdvancedToolSwapperUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.ToolSwapMode
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.RSBTextures
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.CyclicVariantButtonWidget
import com.cleanroommc.retrosophisticatedbackpacks.sync.UpgradeSlotSH
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.item.ItemStack

class AdvancedToolSwapperUpgradeWidget(slotIndex: Int, wrapper: AdvancedToolSwapperUpgradeWrapper, stack: ItemStack) :
    BasicExpandedTabWidget<AdvancedToolSwapperUpgradeWrapper>(
        slotIndex,
        wrapper,
        stack,
        wrapper.settingsLangKey,
        coveredTabSize = 4
    ) {
    init {
        startingRow
            .height(20)
            .child(createSwapWeaponButton())
            .child(createToolSwapModeButton())
    }

    private fun createSwapWeaponButton(): CyclicVariantButtonWidget =
        CyclicVariantButtonWidget(SWAP_WEAPON_VARIANTS, if (wrapper.shouldSwapWeapon) 1 else 0) {
            wrapper.shouldSwapWeapon = !wrapper.shouldSwapWeapon
            slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_TOOL_SWAPPER_SWAP_WEAPON) {}
        }

    private fun createToolSwapModeButton(): CyclicVariantButtonWidget =
        CyclicVariantButtonWidget(TOOL_SWAP_MODE_VARIANTS, wrapper.toolSwapMode.ordinal) {
            wrapper.toolSwapMode = ToolSwapMode.entries[it]
            slotSyncHandler?.syncToServer(UpgradeSlotSH.UPDATE_TOOL_SWAPPER_MODE) {
                it.writeEnumValue(wrapper.toolSwapMode)
            }
        }
}

private val SWAP_WEAPON_VARIANTS = listOf(
    CyclicVariantButtonWidget.Variant(IKey.lang("gui.tool_swapper_swap_weapon_disabled".asTranslationKey()), RSBTextures.CROSS_ICON),
    CyclicVariantButtonWidget.Variant(IKey.lang("gui.tool_swapper_swap_weapon_enabled".asTranslationKey()), RSBTextures.CHECK_ICON),
)

private val TOOL_SWAP_MODE_VARIANTS = listOf(
    CyclicVariantButtonWidget.Variant(IKey.lang("gui.tool_swapper_any".asTranslationKey()), RSBTextures.IN_OUT_ICON),
    CyclicVariantButtonWidget.Variant(IKey.lang("gui.tool_swapper_only_tools".asTranslationKey()), RSBTextures.SOLID_UP_ARROW_ICON),
    CyclicVariantButtonWidget.Variant(IKey.lang("gui.tool_swapper_no_swap".asTranslationKey()), RSBTextures.CROSS_ICON),
)
