package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.upgrade

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.drawable.GuiDraw
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widgets.SlotGroupWidget
import com.cleanroommc.retrosophisticatedbackpacks.capability.BackpackWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.BatteryUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.RSBTextures
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.slot.NoBackgroundItemSlot
import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler

class BatteryUpgradeWidget(
    slotIndex: Int,
    wrapper: BatteryUpgradeWrapper,
    stack: ItemStack,
    private val backpackWrapper: BackpackWrapper
) : ExpandedUpgradeTabWidget<BatteryUpgradeWrapper>(slotIndex, wrapper, 3, stack, wrapper.settingsLangKey, width = 48) {
    init {
        size(48, 66)
        val slots = SlotGroupWidget().name("battery_$slotIndex").disableSortButtons()
        slots.size(42, 18).pos(3, 42)
        slots.child(NoBackgroundItemSlot().syncHandler("battery_$slotIndex", 0).pos(0, 0))
        slots.child(NoBackgroundItemSlot().syncHandler("battery_$slotIndex", 1).pos(21, 0))
        child(slots)
    }

    override fun drawBackground(context: ModularGuiContext, widgetTheme: WidgetThemeEntry<*>) {
        super.drawBackground(context, widgetTheme)
        RSBTextures.SLOT_BACKGROUND.draw(context, 3, 42, 18, 18, widgetTheme.theme)
        RSBTextures.SLOT_BACKGROUND.draw(context, 24, 42, 18, 18, widgetTheme.theme)
    }

    override fun draw(context: ModularGuiContext, widgetTheme: WidgetThemeEntry<*>) {
        GuiDraw.drawText(energyText(wrapper), 4f, 26f, 0.5f, 0x404040, false)
        super.draw(context, widgetTheme)
    }

    override fun drawOverlay(context: ModularGuiContext, widgetTheme: WidgetThemeEntry<*>) {
        super.drawOverlay(context, widgetTheme)
        drawEmptySlotIcons(context, widgetTheme)
    }

    private fun drawEmptySlotIcons(context: ModularGuiContext, widgetTheme: WidgetThemeEntry<*>) {
        val inventory = wrapper.getInventory()
        drawEmptySlotIcon(context, widgetTheme, inventory, 0, 4, 43, true)
        drawEmptySlotIcon(context, widgetTheme, inventory, 1, 25, 43, false)
    }

    private fun drawEmptySlotIcon(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
        inventory: IItemHandler,
        slot: Int,
        x: Int,
        y: Int,
        input: Boolean
    ) {
        if (!inventory.getStackInSlot(slot).isEmpty) {
            return
        }
        (if (input) RSBTextures.EMPTY_BATTERY_INPUT_SLOT else RSBTextures.EMPTY_BATTERY_OUTPUT_SLOT)
            .draw(context, x, y, 16, 16, widgetTheme.theme)
    }

    private fun energyText(wrapper: BatteryUpgradeWrapper): String =
        "${wrapper.energyStored}/${wrapper.getMaxEnergyStored(backpackWrapper)} FE"
}
