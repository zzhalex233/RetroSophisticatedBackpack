package com.cleanroommc.retrosophisticatedbackpacks.compat.jei

import com.cleanroommc.retrosophisticatedbackpacks.common.gui.BackpackCraftingTransferInfo
import com.cleanroommc.retrosophisticatedbackpacks.item.Items
import mezz.jei.api.*

@JEIPlugin
class RetroSophisticatedBackpacksJEIPlugin : IModPlugin {
    companion object {
        lateinit var helpers: IJeiHelpers
    }

    override fun registerItemSubtypes(subtypeRegistry: ISubtypeRegistry) {
        for (backpack in Items.BACKPACK_ITEMS) {
            subtypeRegistry.registerSubtypeInterpreter(backpack, BackpackSubtypeInterpreter)
        }
    }

    override fun register(registry: IModRegistry) {
        helpers = registry.jeiHelpers

        // By hijacking vanilla recipe types
        // We can utilize JEI's built-in recipe transfer handler
        val recipeTransferRegistry = registry.recipeTransferRegistry
        recipeTransferRegistry.addRecipeTransferHandler(BackpackCraftingTransferInfo())
    }
}
