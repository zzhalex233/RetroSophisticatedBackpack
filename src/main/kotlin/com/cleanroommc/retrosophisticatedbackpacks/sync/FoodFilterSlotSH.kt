package com.cleanroommc.retrosophisticatedbackpacks.sync

import com.cleanroommc.modularui.widgets.slot.ModularSlot
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.IFeedingUpgrade
import net.minecraft.item.ItemStack

class FoodFilterSlotSH(slot: ModularSlot) : FilterSlotSH(slot) {
    override fun isItemValid(itemStack: ItemStack): Boolean =
        IFeedingUpgrade.isValidFood(itemStack)
}
