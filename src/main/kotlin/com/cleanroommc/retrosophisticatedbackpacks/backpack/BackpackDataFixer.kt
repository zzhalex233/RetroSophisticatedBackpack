package com.cleanroommc.retrosophisticatedbackpacks.backpack

import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.IFeedingUpgrade
import com.cleanroommc.retrosophisticatedbackpacks.inventory.ExposedItemStackHandler
import net.minecraft.item.ItemStack

object BackpackDataFixer {
    fun fixFeedingUpgrade(filterStacks: ExposedItemStackHandler) {
        for (slotIndex in 0 until filterStacks.slots) {
            val stack = filterStacks.getStackInSlot(slotIndex)

            if (!IFeedingUpgrade.isValidFood(stack)) {
                filterStacks.setStackInSlot(slotIndex, ItemStack.EMPTY)
            }
        }
    }
}
