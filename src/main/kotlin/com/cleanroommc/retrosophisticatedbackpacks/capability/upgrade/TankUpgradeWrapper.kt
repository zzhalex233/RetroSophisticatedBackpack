package com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade

import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import com.cleanroommc.retrosophisticatedbackpacks.capability.BackpackWrapper
import com.cleanroommc.retrosophisticatedbackpacks.inventory.ExposedItemStackHandler
import com.cleanroommc.retrosophisticatedbackpacks.item.TankUpgradeItem
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidUtil
import net.minecraftforge.fluids.capability.IFluidTankProperties
import net.minecraftforge.fluids.capability.FluidTankProperties
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemHandlerHelper

class TankUpgradeWrapper : UpgradeWrapper<TankUpgradeItem>(), ITankUpgrade {
    companion object {
        private const val FLUID_TAG = "Fluid"
        private const val INVENTORY_TAG = "Inventory"
        private const val INPUT_SLOT = 0
        private const val OUTPUT_SLOT = 1
        private const val INPUT_RESULT_SLOT = 2
        private const val OUTPUT_RESULT_SLOT = 3
        private const val BUCKET = 1000
        private const val CAPACITY_PER_ROW = 4000
        private const val MAX_INPUT_OUTPUT_PER_ROW = 20
    }

    override val settingsLangKey = "gui.tank_settings".asTranslationKey()
    override val tankCapacity = CAPACITY_PER_ROW * 3
    private var fluid: FluidStack? = null
    private val inventory = object : ExposedItemStackHandler(4) {
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean =
            slot == INPUT_RESULT_SLOT || slot == OUTPUT_RESULT_SLOT || stack.isEmpty || FluidUtil.getFluidHandler(stack) != null

        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)
        }
    }

    override fun getFluid(): FluidStack? =
        fluid?.copy()

    override fun getTankCapacity(wrapper: BackpackWrapper): Int =
        maxOf(BUCKET, getSlotRows(wrapper) * CAPACITY_PER_ROW * getStackMultiplier(wrapper))

    private fun getMaxInOut(wrapper: BackpackWrapper): Int =
        maxOf(BUCKET, getSlotRows(wrapper) * MAX_INPUT_OUTPUT_PER_ROW * getStackMultiplier(wrapper))

    private fun getSlotRows(wrapper: BackpackWrapper): Int =
        maxOf(1, (wrapper.backpackInventorySize() + 8) / 9)

    private fun getStackMultiplier(wrapper: BackpackWrapper): Int =
        maxOf(1, wrapper.getTotalStackMultiplier())

    override fun fill(wrapper: BackpackWrapper, resource: FluidStack, doFill: Boolean, ignoreInOutLimit: Boolean): Int {
        val current = fluid
        if (resource.amount <= 0 || current != null && !current.isFluidEqual(resource)) {
            return 0
        }

        val accepted = minOf(resource.amount, getTankCapacity(wrapper) - (current?.amount ?: 0), if (ignoreInOutLimit) Int.MAX_VALUE else getMaxInOut(wrapper))
        if (doFill && accepted > 0) {
            fluid = if (current == null) FluidStack(resource.fluid, accepted, resource.tag?.copy())
            else current.also { it.amount += accepted }
        }
        return accepted
    }

    override fun drain(wrapper: BackpackWrapper, maxDrain: Int, doDrain: Boolean, ignoreInOutLimit: Boolean): FluidStack? {
        val current = fluid ?: return null
        val drained = minOf(maxDrain, current.amount, if (ignoreInOutLimit) Int.MAX_VALUE else getMaxInOut(wrapper))
        if (drained <= 0) {
            return null
        }

        val result = FluidStack(current.fluid, drained, current.tag?.copy())
        if (doDrain) {
            current.amount -= drained
            if (current.amount <= 0) {
                fluid = null
            }
        }
        return result
    }

    override fun drain(wrapper: BackpackWrapper, resource: FluidStack, doDrain: Boolean, ignoreInOutLimit: Boolean): FluidStack? {
        val current = fluid ?: return null
        if (!current.isFluidEqual(resource)) {
            return null
        }
        return drain(wrapper, resource.amount, doDrain, ignoreInOutLimit)
    }

    override fun tick(wrapper: BackpackWrapper, world: World) {
        if (world.totalWorldTime % 20L != 0L) {
            return
        }
        tryDrainInput(wrapper)
        tryFillOutput(wrapper)
    }

    override fun getInventory(): IItemHandler =
        inventory

    override fun interactWithCursorStack(player: EntityPlayer, wrapper: BackpackWrapper) {
        val cursor = player.inventory.itemStack
        if (cursor.isEmpty) {
            return
        }
        val handler = FluidUtil.getFluidHandler(cursor.copy()) ?: return
        val current = fluid
        if (current != null && fillHandler(wrapper, handler, false)) {
            player.inventory.itemStack = handler.container
        } else if (drainHandler(wrapper, handler, false)) {
            player.inventory.itemStack = handler.container
        }
    }

    private fun tryDrainInput(wrapper: BackpackWrapper): Boolean {
        val stack = inventory.getStackInSlot(INPUT_SLOT)
        if (stack.isEmpty) {
            return false
        }
        val single = ItemHandlerHelper.copyStackWithSize(stack, 1)
        val handler = FluidUtil.getFluidHandler(single) ?: return false
        if (!drainHandler(wrapper, handler, true)) {
            return false
        }
        stack.shrink(1)
        if (stack.isEmpty) {
            inventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY)
        }
        inventory.insertItem(INPUT_RESULT_SLOT, handler.container, false)
        return true
    }

    private fun tryFillOutput(wrapper: BackpackWrapper): Boolean {
        val stack = inventory.getStackInSlot(OUTPUT_SLOT)
        if (stack.isEmpty) {
            return false
        }
        val single = ItemHandlerHelper.copyStackWithSize(stack, 1)
        val handler = FluidUtil.getFluidHandler(single) ?: return false
        if (!fillHandler(wrapper, handler, true)) {
            return false
        }
        stack.shrink(1)
        if (stack.isEmpty) {
            inventory.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY)
        }
        inventory.insertItem(OUTPUT_RESULT_SLOT, handler.container, false)
        return true
    }

    private fun drainHandler(wrapper: BackpackWrapper, handler: net.minecraftforge.fluids.capability.IFluidHandlerItem, moveResult: Boolean): Boolean {
        val extracted = if (fluid == null) handler.drain(getMaxInOut(wrapper), false) else handler.drain(FluidStack(fluid!!.fluid, getTankCapacity(wrapper) - fluid!!.amount), false)
        if (extracted == null || extracted.amount <= 0 || fill(wrapper, extracted, false) <= 0) {
            return false
        }
        if (moveResult) {
            val preview = FluidUtil.getFluidHandler(handler.container.copy()) ?: return false
            val filled = fill(wrapper, extracted, false)
            preview.drain(FluidStack(extracted.fluid, filled, extracted.tag?.copy()), true)
            if (!inventory.insertItem(INPUT_RESULT_SLOT, preview.container, true).isEmpty) {
                return false
            }
        }
        val filled = fill(wrapper, extracted, true)
        handler.drain(FluidStack(extracted.fluid, filled, extracted.tag?.copy()), true)
        return true
    }

    private fun fillHandler(wrapper: BackpackWrapper, handler: net.minecraftforge.fluids.capability.IFluidHandlerItem, moveResult: Boolean): Boolean {
        val current = fluid ?: return false
        val filled = handler.fill(FluidStack(current.fluid, minOf(getMaxInOut(wrapper), current.amount), current.tag?.copy()), false)
        if (filled <= 0 || drain(wrapper, filled, false) == null) {
            return false
        }
        if (moveResult) {
            val preview = FluidUtil.getFluidHandler(handler.container.copy()) ?: return false
            val drained = drain(wrapper, filled, false) ?: return false
            preview.fill(drained, true)
            if (!inventory.insertItem(OUTPUT_RESULT_SLOT, preview.container, true).isEmpty) {
                return false
            }
        }
        val drained = drain(wrapper, filled, true) ?: return false
        handler.fill(drained, true)
        return true
    }

    override fun serializeNBT(): NBTTagCompound {
        val nbt = super.serializeNBT()
        fluid?.let { nbt.setTag(FLUID_TAG, it.writeToNBT(NBTTagCompound())) }
        nbt.setTag(INVENTORY_TAG, inventory.serializeNBT())
        return nbt
    }

    override fun deserializeNBT(nbt: NBTTagCompound) {
        super.deserializeNBT(nbt)
        fluid = if (nbt.hasKey(FLUID_TAG)) FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag(FLUID_TAG)) else null
        inventory.deserializeNBT(nbt.getCompoundTag(INVENTORY_TAG))
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.TANK_UPGRADE_CAPABILITY ||
                capability == Capabilities.ITANK_UPGRADE_CAPABILITY ||
                super<UpgradeWrapper>.hasCapability(capability, facing)

    fun getTankProperties(wrapper: BackpackWrapper): Array<IFluidTankProperties> =
        arrayOf(FluidTankProperties(getFluid(), getTankCapacity(wrapper), true, true))
}
