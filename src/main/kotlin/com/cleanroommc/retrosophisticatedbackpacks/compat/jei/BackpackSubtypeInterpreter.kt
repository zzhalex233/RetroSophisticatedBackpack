package com.cleanroommc.retrosophisticatedbackpacks.compat.jei

import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import mezz.jei.api.ISubtypeRegistry
import net.minecraft.item.ItemStack

object BackpackSubtypeInterpreter : ISubtypeRegistry.ISubtypeInterpreter {
    override fun apply(itemStack: ItemStack): String {
        val capability = itemStack.getCapability(Capabilities.BACKPACK_CAPABILITY, null) ?: return ""
        return "mainColor:${capability.mainColor};accentColor:${capability.accentColor}"
    }
}
