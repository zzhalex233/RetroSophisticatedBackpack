package com.cleanroommc.retrosophisticatedbackpacks.compat.theoneprobe

import com.cleanroommc.retrosophisticatedbackpacks.Tags
import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import com.cleanroommc.retrosophisticatedbackpacks.tileentity.BackpackTileEntity
import com.google.common.base.Function
import mcjty.theoneprobe.Tools
import mcjty.theoneprobe.api.*
import mcjty.theoneprobe.config.Config
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class OneProbePlugin : Function<ITheOneProbe, Void> {
    override fun apply(input: ITheOneProbe?): Void? {
        input?.registerBlockDisplayOverride(BackpackBlockDisplayOverride)
        return null
    }

    object BackpackBlockDisplayOverride : IBlockDisplayOverride {
        override fun overrideStandardInfo(
            mode: ProbeMode,
            info: IProbeInfo,
            player: EntityPlayer,
            world: World,
            state: IBlockState,
            data: IProbeHitData
        ): Boolean {
            val te = world.getTileEntity(data.pos) as? BackpackTileEntity ?: return false
            val teWrapper = te.wrapper
            val stack = ItemStack(state.block, 1)
            val stackWrapper = stack.getCapability(Capabilities.BACKPACK_CAPABILITY, null) ?: return false
            stackWrapper.mainColor = teWrapper.mainColor
            stackWrapper.accentColor = teWrapper.accentColor

            if (Tools.show(mode, Config.getRealConfig().showModName)) {
                info.horizontal()
                    .item(stack)
                    .vertical()
                    .itemLabel(stack)
                    .text("${TextStyleClass.MODNAME}${Tags.MOD_ID}")
            } else {
                info.horizontal()
                    .item(stack)
                    .vertical()
                    .itemLabel(stack)
            }

            return true
        }
    }
}
