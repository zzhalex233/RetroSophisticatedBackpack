package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.upgrade

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widgets.SlotGroupWidget
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.TankUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.RSBTextures
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.slot.NoBackgroundItemSlot
import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler

class TankUpgradeWidget(slotIndex: Int, wrapper: TankUpgradeWrapper, stack: ItemStack) :
    ExpandedUpgradeTabWidget<TankUpgradeWrapper>(slotIndex, wrapper, 3, stack, wrapper.settingsLangKey, width = 48) {
    init {
        size(48, 80)

        val slots = SlotGroupWidget().name("tank_$slotIndex").disableSortButtons()
        slots.size(42, 50).pos(3, 24)
        slots.child(NoBackgroundItemSlot().syncHandler("tank_$slotIndex", 0).pos(0, 0))
        slots.child(NoBackgroundItemSlot().syncHandler("tank_$slotIndex", 1).pos(21, 0))
        slots.child(NoBackgroundItemSlot().syncHandler("tank_$slotIndex", 2).pos(0, 32))
        slots.child(NoBackgroundItemSlot().syncHandler("tank_$slotIndex", 3).pos(21, 32))
        child(slots)
    }

    override fun drawBackground(context: ModularGuiContext, widgetTheme: WidgetThemeEntry<*>) {
        super.drawBackground(context, widgetTheme)
        RSBTextures.SLOT_BACKGROUND.draw(context, 3, 24, 18, 18, widgetTheme.theme)
        RSBTextures.SLOT_BACKGROUND.draw(context, 24, 24, 18, 18, widgetTheme.theme)
        RSBTextures.SLOT_BACKGROUND.draw(context, 3, 56, 18, 18, widgetTheme.theme)
        RSBTextures.SLOT_BACKGROUND.draw(context, 24, 56, 18, 18, widgetTheme.theme)
        RSBTextures.TANK_ARROW.draw(context, 4, 45, 15, 8, widgetTheme.theme)
        RSBTextures.TANK_ARROW.draw(context, 25, 45, 15, 8, widgetTheme.theme)
    }

    override fun drawOverlay(context: ModularGuiContext, widgetTheme: WidgetThemeEntry<*>) {
        super.drawOverlay(context, widgetTheme)
        drawEmptySlotIcons(context, widgetTheme)
    }

    private fun drawEmptySlotIcons(context: ModularGuiContext, widgetTheme: WidgetThemeEntry<*>) {
        val inventory = wrapper.getInventory()
        drawEmptySlotIcon(context, widgetTheme, inventory, 0, 4, 25, true)
        drawEmptySlotIcon(context, widgetTheme, inventory, 1, 25, 25, false)
        drawEmptySlotIcon(context, widgetTheme, inventory, 2, 4, 57, true)
        drawEmptySlotIcon(context, widgetTheme, inventory, 3, 25, 57, false)
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
        (if (input) RSBTextures.EMPTY_TANK_INPUT_SLOT else RSBTextures.EMPTY_TANK_OUTPUT_SLOT)
            .draw(context, x, y, 16, 16, widgetTheme.theme)
    }
}
