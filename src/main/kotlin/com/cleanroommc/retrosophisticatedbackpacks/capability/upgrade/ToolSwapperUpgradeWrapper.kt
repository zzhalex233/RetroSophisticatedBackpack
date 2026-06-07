package com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade

import com.cleanroommc.retrosophisticatedbackpacks.capability.BackpackWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.UpgradeFilterUtils.matchesAllowEmpty
import com.cleanroommc.retrosophisticatedbackpacks.config.Config
import com.cleanroommc.retrosophisticatedbackpacks.item.BackpackItem
import com.cleanroommc.retrosophisticatedbackpacks.item.ToolSwapperUpgradeItem
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemShears
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.IShearable
import net.minecraftforge.common.capabilities.Capability
import java.util.LinkedList

open class ToolSwapperUpgradeWrapper(
    private val hasSettingsTab: Boolean = false,
    private val swapToolOnKeyPress: Boolean = false,
) : BasicUpgradeWrapper<ToolSwapperUpgradeItem>(Config.toolSwapperUpgrade.filterSlots, Config.toolSwapperUpgrade.slotsInRow),
    IToolSwapperUpgrade {
    companion object {
        private const val SHOULD_SWAP_WEAPON_TAG = "ShouldSwapWeapon"
        private const val TOOL_SWAP_MODE_TAG = "ToolSwapMode"
    }

    override val settingsLangKey = "gui.tool_swapper_settings".asTranslationKey()
    var shouldSwapWeapon = true
    var toolSwapMode = ToolSwapMode.ANY
    private var lastMinedBlock: Block = Blocks.AIR
    private var toolCacheFor: String? = null
    private val toolCache = LinkedList<ItemStack>()

    override fun onBlockClick(player: EntityPlayer, wrapper: BackpackWrapper, pos: BlockPos, state: IBlockState): Boolean {
        if (!enabled || player.isCreative || player.isSpectator || toolSwapMode == ToolSwapMode.NO_SWAP || state.material == Material.AIR) {
            return false
        }

        val held = player.heldItemMainhand
        if (held.item is BackpackItem || (toolSwapMode == ToolSwapMode.ONLY_TOOLS && isWeapon(held, player)) || (!isWeapon(held, player) && isNotTool(held)) || !matchesAllowEmpty(held)) {
            return false
        }

        val heldSpeed = if (isGoodAtBreaking(player, pos, state, held)) {
            if (lastMinedBlock == state.block || state.getBlockHardness(player.world, pos) == 0f) {
                return true
            }
            held.getDestroySpeed(state)
        } else 0f
        lastMinedBlock = state.block
        val selectedSlot = findBestToolSlot(wrapper, player, pos, state, heldSpeed) ?: return false
        return swapMainHandWithBackpackSlot(player, wrapper, selectedSlot)
    }

    override fun onAttackEntity(player: EntityPlayer, wrapper: BackpackWrapper): Boolean {
        if (!enabled || !shouldSwapWeapon) {
            return false
        }

        val held = player.heldItemMainhand
        if (isWeapon(held, player)) {
            return true
        }
        if (held.item is BackpackItem || isNotTool(held) || !matchesAllowEmpty(held)) {
            return false
        }

        val selectedSlot = findBestWeaponSlot(wrapper, player) ?: return false
        return swapMainHandWithBackpackSlot(player, wrapper, selectedSlot)
    }

    override fun onBlockInteract(player: EntityPlayer, wrapper: BackpackWrapper, world: World, pos: BlockPos, state: IBlockState): Boolean {
        if (!enabled || !swapToolOnKeyPress || player.heldItemMainhand.item is BackpackItem) {
            return false
        }
        return tryToSwapTool(player, wrapper, state.block.registryName?.toString()) {
            itemWorksOnBlock(world, pos, state, player, it)
        }
    }

    override fun onEntityInteract(player: EntityPlayer, wrapper: BackpackWrapper, entity: Entity): Boolean {
        if (!enabled || !swapToolOnKeyPress || player.heldItemMainhand.item is BackpackItem) {
            return false
        }
        return tryToSwapTool(player, wrapper, entity.javaClass.name) { itemWorksOnEntity(entity, it) }
    }

    private fun findBestToolSlot(wrapper: BackpackWrapper, player: EntityPlayer, pos: BlockPos, state: IBlockState, heldSpeed: Float): Int? {
        var bestSlot: Int? = null
        var bestSpeed = heldSpeed
        for (slot in 0 until wrapper.slots) {
            val stack = wrapper.getStackInSlot(slot)
            if (stack.isEmpty || !matchesAllowEmpty(stack) || !isGoodAtBreaking(player, pos, state, stack)) {
                continue
            }
            val speed = stack.getDestroySpeed(state)
            if (speed > bestSpeed || state.getPlayerRelativeBlockHardness(player, player.world, pos) >= 1f) {
                bestSpeed = speed
                bestSlot = slot
            }
        }
        return bestSlot
    }

    private fun findBestWeaponSlot(wrapper: BackpackWrapper, player: EntityPlayer): Int? {
        var bestSlot: Int? = null
        var bestDamage = getAttackDamage(player.heldItemMainhand, player)
        for (slot in 0 until wrapper.slots) {
            val stack = wrapper.getStackInSlot(slot)
            if (stack.isEmpty || !matchesAllowEmpty(stack) || !isWeapon(stack, player)) {
                continue
            }
            val damage = getAttackDamage(stack, player)
            if (damage > bestDamage) {
                bestDamage = damage
                bestSlot = slot
            }
        }
        return bestSlot
    }

    private fun tryToSwapTool(
        player: EntityPlayer,
        wrapper: BackpackWrapper,
        targetRegistryName: String?,
        predicate: (ItemStack) -> Boolean
    ): Boolean {
        if (toolCacheFor != targetRegistryName) {
            toolCache.clear()
            toolCacheFor = targetRegistryName
        }

        val held = player.heldItemMainhand
        if (!held.isEmpty && predicate(held) && toolCache.none { isSameTool(it, held) }) {
            toolCache.offer(held.copy().also { it.count = 1 })
        }

        val selectedSlot = findToolToSwapSlot(wrapper, predicate) ?: return false
        val selectedTool = wrapper.getStackInSlot(selectedSlot).copy().also { it.count = 1 }
        if (!swapMainHandWithBackpackSlot(player, wrapper, selectedSlot)) {
            return false
        }
        toolCache.offer(selectedTool)
        return true
    }

    private fun findToolToSwapSlot(wrapper: BackpackWrapper, predicate: (ItemStack) -> Boolean): Int? {
        val alreadyGivenBefore = mutableListOf<ItemStack>()
        for (slot in 0 until wrapper.slots) {
            val stack = wrapper.getStackInSlot(slot)
            if (!stack.isEmpty && matchesAllowEmpty(stack) && predicate(stack)) {
                if (toolCache.none { isSameTool(it, stack) }) {
                    return slot
                }
                alreadyGivenBefore.add(stack.copy().also { it.count = 1 })
            }
        }

        while (toolCache.peek() != null) {
            val cached = toolCache.poll()
            if (alreadyGivenBefore.any { isSameTool(it, cached) }) {
                return findSlotWithSameTool(wrapper, cached, predicate)
            }
        }
        return null
    }

    private fun findSlotWithSameTool(wrapper: BackpackWrapper, tool: ItemStack, predicate: (ItemStack) -> Boolean): Int? {
        for (slot in 0 until wrapper.slots) {
            val stack = wrapper.getStackInSlot(slot)
            if (!stack.isEmpty && matchesAllowEmpty(stack) && predicate(stack) && isSameTool(stack, tool)) {
                return slot
            }
        }
        return null
    }

    private fun isSameTool(first: ItemStack, second: ItemStack): Boolean =
        !first.isEmpty && !second.isEmpty && first.item == second.item

    private fun swapMainHandWithBackpackSlot(player: EntityPlayer, wrapper: BackpackWrapper, slot: Int): Boolean {
        val held = player.heldItemMainhand
        val tool = wrapper.extractItem(slot, 1, true)
        if (tool.isEmpty || (!held.isEmpty && !wrapper.insertStack(held.copy(), true).isEmpty && tool.count != 1)) {
            return false
        }

        player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, wrapper.extractItem(slot, 1, false))
        if (!held.isEmpty) {
            wrapper.insertStack(held.copy(), false)
        }
        return true
    }

    private fun isGoodAtBreaking(player: EntityPlayer, pos: BlockPos, state: IBlockState, stack: ItemStack): Boolean =
        stack.getDestroySpeed(state) > 1.5f || state.getPlayerRelativeBlockHardness(player, player.world, pos) >= 1f

    private fun isNotTool(stack: ItemStack): Boolean =
        stack.isEmpty || stack.getDestroySpeed(Blocks.STONE.defaultState) <= 1f && stack.getDestroySpeed(Blocks.DIRT.defaultState) <= 1f &&
                stack.getDestroySpeed(Blocks.LOG.defaultState) <= 1f && stack.item !is ItemShears && stack.item !is ItemAxe

    private fun isWeapon(stack: ItemStack, player: EntityPlayer): Boolean =
        stack.item is ItemSword || getAttackDamage(stack, player) > getAttackDamage(ItemStack.EMPTY, player)

    private fun getAttackDamage(stack: ItemStack, player: EntityPlayer): Double {
        if (stack.isEmpty) {
            return player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).baseValue
        }
        var damage = 0.0
        stack.getAttributeModifiers(net.minecraft.inventory.EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.name)
            .forEach { damage += it.amount }
        return damage
    }

    private fun itemWorksOnBlock(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, stack: ItemStack): Boolean {
        if (stack.item is ItemShears && state.block is IShearable) {
            return (state.block as IShearable).isShearable(stack, world, pos)
        }
        return isGoodAtBreaking(player, pos, state, stack)
    }

    private fun itemWorksOnEntity(entity: Entity, stack: ItemStack): Boolean =
        stack.item is ItemShears && entity is IShearable && entity.isShearable(stack, entity.world, entity.position)

    override fun serializeNBT(): NBTTagCompound {
        val nbt = super.serializeNBT()
        nbt.setBoolean(SHOULD_SWAP_WEAPON_TAG, shouldSwapWeapon)
        nbt.setByte(TOOL_SWAP_MODE_TAG, toolSwapMode.ordinal.toByte())
        return nbt
    }

    override fun deserializeNBT(nbt: NBTTagCompound) {
        super.deserializeNBT(nbt)
        shouldSwapWeapon = !nbt.hasKey(SHOULD_SWAP_WEAPON_TAG) || nbt.getBoolean(SHOULD_SWAP_WEAPON_TAG)
        toolSwapMode = ToolSwapMode.entries.getOrElse(nbt.getByte(TOOL_SWAP_MODE_TAG).toInt()) { ToolSwapMode.ANY }
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.TOOL_SWAPPER_UPGRADE_CAPABILITY ||
                capability == Capabilities.ITOOL_SWAPPER_UPGRADE_CAPABILITY ||
                super<BasicUpgradeWrapper>.hasCapability(capability, facing)

    fun hasSettingsTab(): Boolean = hasSettingsTab
}

class AdvancedToolSwapperUpgradeWrapper : ToolSwapperUpgradeWrapper(true, true) {
    override val settingsLangKey = "gui.advanced_tool_swapper_settings".asTranslationKey()

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == Capabilities.ADVANCED_TOOL_SWAPPER_UPGRADE_CAPABILITY ||
                super.hasCapability(capability, facing)
}

enum class ToolSwapMode {
    ANY,
    ONLY_TOOLS,
    NO_SWAP;

    fun next(): ToolSwapMode =
        entries[(ordinal + 1) % entries.size]
}
