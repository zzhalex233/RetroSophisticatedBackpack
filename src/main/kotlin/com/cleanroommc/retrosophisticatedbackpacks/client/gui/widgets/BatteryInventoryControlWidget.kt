package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.screen.RichTooltip
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widget.Widget
import com.cleanroommc.retrosophisticatedbackpacks.capability.BackpackWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.BatteryUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.RSBTextures
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.getThemeOrDefault

class BatteryInventoryControlWidget(
    private val upgradeSlot: Int,
    private val backpackWrapper: BackpackWrapper,
    height: Int
) : Widget<BatteryInventoryControlWidget>() {
    companion object {
        const val WIDTH = 36
        private const val BATTERY_LEFT = 9
        private const val BATTERY_WIDTH = 18
        private const val OVERLAY_LEFT = BATTERY_LEFT + 1
        private const val OVERLAY_WIDTH = 16
    }

    init {
        size(WIDTH, height)
        tooltipAutoUpdate(true)
        tooltipDynamic {
            val wrapper = currentWrapper()
            it.addLine(IKey.str("${wrapper?.energyStored ?: 0}/${wrapper?.getMaxEnergyStored(backpackWrapper) ?: 0} FE"))
            it.pos(RichTooltip.Pos.NEXT_TO_MOUSE)
        }
    }

    override fun draw(context: ModularGuiContext, widgetTheme: WidgetThemeEntry<*>) {
        val theme = widgetTheme.getThemeOrDefault()
        val height = area.height
        RSBTextures.BAR_BACKGROUND_TOP.draw(context, BATTERY_LEFT, 0, BATTERY_WIDTH, if (height < 36) height / 2 else 18, theme)
        var yOffset = 18
        repeat((height - 36).coerceAtLeast(0) / 18) {
            RSBTextures.BAR_BACKGROUND_MIDDLE.draw(context, BATTERY_LEFT, yOffset, BATTERY_WIDTH, 18, theme)
            yOffset += 18
        }
        RSBTextures.BAR_BACKGROUND_BOTTOM.draw(
            context,
            BATTERY_LEFT,
            if (height < 36) height / 2 else yOffset,
            BATTERY_WIDTH,
            if (height < 36) height / 2 else 18,
            theme
        )
        renderCharge(context, currentWrapper(), height, theme)
        yOffset = 0
        repeat(height / 18) {
            RSBTextures.BATTERY_OVERLAY.draw(context, OVERLAY_LEFT, yOffset, OVERLAY_WIDTH, 18, theme)
            yOffset += 18
        }
    }

    private fun currentWrapper(): BatteryUpgradeWrapper? =
        backpackWrapper.upgradeItemStackHandler.inventory[upgradeSlot]
            .getCapability(Capabilities.BATTERY_UPGRADE_CAPABILITY, null)

    private fun renderCharge(
        context: ModularGuiContext,
        wrapper: BatteryUpgradeWrapper?,
        height: Int,
        theme: com.cleanroommc.modularui.theme.WidgetTheme
    ) {
        val max = wrapper?.getMaxEnergyStored(backpackWrapper) ?: 0
        if (wrapper == null || max <= 0 || wrapper.energyStored <= 0) {
            return
        }
        val displayLevel = ((height - 2) * (wrapper.energyStored.toFloat() / max)).toInt().coerceIn(1, height - 2)
        var drawn = 0
        while (drawn < displayLevel) {
            val segment = minOf(6, displayLevel - drawn)
            RSBTextures.BATTERY_CHARGE.draw(
                context,
                OVERLAY_LEFT,
                height - 1 - drawn - segment,
                OVERLAY_WIDTH,
                segment,
                theme
            )
            drawn += segment
        }
    }
}
