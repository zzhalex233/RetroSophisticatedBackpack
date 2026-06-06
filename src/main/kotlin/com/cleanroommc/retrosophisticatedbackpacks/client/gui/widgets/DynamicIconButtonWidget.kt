package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets

import com.cleanroommc.modularui.api.drawable.IDrawable
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widgets.ButtonWidget
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.getThemeOrDefault

class DynamicIconButtonWidget(
    private val icon: () -> IDrawable,
    private val iconOffset: Int = 1,
    private val iconSize: Int = 16
) : ButtonWidget<DynamicIconButtonWidget>() {
    override fun drawOverlay(context: ModularGuiContext?, widgetTheme: WidgetThemeEntry<*>?) {
        super.drawOverlay(context, widgetTheme)
        icon().draw(context, iconOffset, iconOffset, iconSize, iconSize, widgetTheme.getThemeOrDefault())
    }
}
