package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets.slot

import com.cleanroommc.modularui.api.drawable.IDrawable
import com.cleanroommc.modularui.widgets.slot.ItemSlot

open class NoBackgroundItemSlot : ItemSlot() {
    init {
        background(IDrawable.EMPTY)
    }
}
