package com.cleanroommc.retrosophisticatedbackpacks.handler

import baubles.api.BaublesApi
import com.cleanroommc.retrosophisticatedbackpacks.RetroSophisticatedBackpacks
import com.cleanroommc.retrosophisticatedbackpacks.Tags
import com.cleanroommc.retrosophisticatedbackpacks.backpack.BackpackInventoryHelper
import com.cleanroommc.retrosophisticatedbackpacks.capability.Capabilities
import com.cleanroommc.retrosophisticatedbackpacks.capability.BackpackWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.EverlastingUpgradeWrapper
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.IToolSwapperUpgrade
import com.cleanroommc.retrosophisticatedbackpacks.item.BackpackItem
import com.cleanroommc.retrosophisticatedbackpacks.mixin.EntityItemAccessor
import net.minecraft.block.material.Material
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumActionResult
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.item.ItemExpireEvent
import net.minecraftforge.event.entity.player.EntityItemPickupEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.ExplosionEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper.InvWrapper

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
object EntityEventHandler {

    @SubscribeEvent
    @JvmStatic
    fun onItemPickup(event: EntityItemPickupEvent) {
        val player = event.entityPlayer
        val inventory = player.inventory
        var stack = event.item.item.copy()

        stack = attemptPickup(InvWrapper(inventory), stack)

        if (!stack.isEmpty && RetroSophisticatedBackpacks.baublesLoaded) {
            stack = attemptPickup(BaublesApi.getBaublesHandler(player), stack)
        }

        if (stack.isEmpty) {
            event.item.setDead()
            event.isCanceled = true

            event.item.world.playSound(
                null,
                event.item.posX, event.item.posY, event.item.posZ, SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS, 0.2f,
                ((player.rng.nextFloat() - player.rng.nextFloat()) * 0.7f + 1.0f) * 2.0f
            )
            return
        } else if (stack.count != event.item.item.count) {
            event.item.setDead()
            event.isCanceled = true

            val world = event.item.world
            val alteredEntityItem = EntityItem(world, event.item.posX, event.item.posY, event.item.posZ, stack)
            alteredEntityItem.setNoPickupDelay()
            world.spawnEntity(alteredEntityItem)
        }
    }

    /**
     * Attempts to perform pickup to any backpack exists in targetInventory.
     */
    private fun attemptPickup(targetInventory: IItemHandler, stack: ItemStack): ItemStack {
        var stack = stack

        for (i in 0 until targetInventory.slots) {
            val backpackStack = targetInventory.getStackInSlot(i)

            if (backpackStack.item !is BackpackItem)
                continue

            val wrapper = backpackStack.getCapability(Capabilities.BACKPACK_CAPABILITY, null) ?: continue

            if (!wrapper.canPickupItem(stack))
                continue

            stack = wrapper.insertStack(stack, false, true)

            if (stack.isEmpty)
                break
        }

        return stack
    }

    @SubscribeEvent
    @JvmStatic
    fun onItemExpire(event: ItemExpireEvent) {
        val stack = event.entityItem.item
        val wrapper = stack.getCapability(Capabilities.BACKPACK_CAPABILITY, null) ?: return
        if (wrapper.hasEverlastingUpgrade()) {
            event.extraLife = Int.MAX_VALUE
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onEntityJoinWorld(event: EntityJoinWorldEvent) {
        val entity = event.entity as? EntityItem ?: return
        if (entity.item.getCapability(Capabilities.BACKPACK_CAPABILITY, null)?.hasEverlastingUpgrade() == true) {
            keepEverlastingItemAlive(entity)
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onWorldTick(event: net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent) {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END || event.world.isRemote || event.world.totalWorldTime % 20L != 0L) {
            return
        }
        event.world.loadedEntityList.asSequence()
            .filterIsInstance<EntityItem>()
            .filter { it.item.getCapability(Capabilities.BACKPACK_CAPABILITY, null)?.hasEverlastingUpgrade() == true }
            .forEach { entity ->
                keepEverlastingItemAlive(entity)
                if (entity.posY < 0) {
                    entity.setPosition(entity.posX, 1.0, entity.posZ)
                    entity.motionY = 0.2
                }
                val material = entity.world.getBlockState(BlockPos(entity)).material
                if (material == Material.WATER || material == Material.LAVA) {
                    entity.motionY = 0.08
                    entity.fallDistance = 0f
                }
            }
        }

    @SubscribeEvent
    @JvmStatic
    fun onExplosionDetonate(event: ExplosionEvent.Detonate) {
        event.affectedBlocks.removeIf { pos ->
            val tile = event.world.getTileEntity(pos) as? com.cleanroommc.retrosophisticatedbackpacks.tileentity.BackpackTileEntity
            tile?.wrapper?.hasEverlastingUpgrade() == true
        }
        event.affectedEntities.removeIf { entity ->
            entity is EntityItem && entity.item.getCapability(Capabilities.BACKPACK_CAPABILITY, null)?.hasEverlastingUpgrade() == true
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onLeftClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
        if (event.world.isRemote) {
            return
        }
        val state = event.world.getBlockState(event.pos)
        if (forEachBackpack(event.entityPlayer) { wrapper ->
                wrapper.gatherCapabilityUpgrades(Capabilities.ITOOL_SWAPPER_UPGRADE_CAPABILITY)
                    .any { it.onBlockClick(event.entityPlayer, wrapper, event.pos, state) }
            }) {
            event.entityPlayer.inventoryContainer.detectAndSendChanges()
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onAttackEntity(event: AttackEntityEvent) {
        if (event.entityPlayer.world.isRemote) {
            return
        }
        if (forEachBackpack(event.entityPlayer) { wrapper ->
                wrapper.gatherCapabilityUpgrades(Capabilities.ITOOL_SWAPPER_UPGRADE_CAPABILITY)
                    .any { it.onAttackEntity(event.entityPlayer, wrapper) }
            }) {
            event.entityPlayer.inventoryContainer.detectAndSendChanges()
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onPlayerInteract(event: PlayerInteractEvent.EntityInteract) {
        val player = event.entityPlayer
        val stack = player.heldItemMainhand
        val entity = event.target

        if (stack.item is BackpackItem) {
            if (player.isSneaking) {
                val wrapper = stack.getCapability(Capabilities.BACKPACK_CAPABILITY, null)
                    ?: return
                var transferred = BackpackInventoryHelper.attemptDepositOnEntity(wrapper, entity)
                transferred =
                    BackpackInventoryHelper.attemptRestockFromEntity(wrapper, entity) || transferred

                if (transferred) {
                    player.world.playSound(
                        null,
                        player.position,
                        SoundEvents.ITEM_ARMOR_EQUIP_IRON,
                        SoundCategory.BLOCKS,
                        0.5f,
                        0.5f
                    )

                    event.isCanceled = true
                    event.cancellationResult = EnumActionResult.SUCCESS
                }
            }
        }

        if (!player.world.isRemote && forEachBackpack(player) { wrapper ->
                wrapper.gatherCapabilityUpgrades(Capabilities.ITOOL_SWAPPER_UPGRADE_CAPABILITY)
                    .filterIsInstance<IToolSwapperUpgrade>()
                    .any { it.onEntityInteract(player, wrapper, entity) }
            }) {
            player.inventoryContainer.detectAndSendChanges()
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        if (event.world.isRemote) {
            return
        }
        val state = event.world.getBlockState(event.pos)
        if (forEachBackpack(event.entityPlayer) { wrapper ->
                wrapper.gatherCapabilityUpgrades(Capabilities.ITOOL_SWAPPER_UPGRADE_CAPABILITY)
                    .filterIsInstance<IToolSwapperUpgrade>()
                    .any { it.onBlockInteract(event.entityPlayer, wrapper, event.world, event.pos, state) }
            }) {
            event.entityPlayer.inventoryContainer.detectAndSendChanges()
        }
    }

    private fun BackpackWrapper.hasEverlastingUpgrade(): Boolean =
        gatherCapabilityUpgrades(Capabilities.EVERLASTING_UPGRADE_CAPABILITY)
            .filterIsInstance<EverlastingUpgradeWrapper>()
            .isNotEmpty()

    private fun keepEverlastingItemAlive(entity: EntityItem) {
        entity.lifespan = Int.MAX_VALUE
        entity.setEntityInvulnerable(true)
        (entity as EntityItemAccessor).`rsb$setAge`(0)
    }

    private fun forEachBackpack(player: net.minecraft.entity.player.EntityPlayer, action: (BackpackWrapper) -> Boolean): Boolean {
        if (forEachBackpackIn(InvWrapper(player.inventory), action)) {
            return true
        }
        return RetroSophisticatedBackpacks.baublesLoaded && forEachBackpackIn(BaublesApi.getBaublesHandler(player), action)
    }

    private fun forEachBackpackIn(inventory: IItemHandler, action: (BackpackWrapper) -> Boolean): Boolean {
        for (slot in 0 until inventory.slots) {
            val wrapper = inventory.getStackInSlot(slot).getCapability(Capabilities.BACKPACK_CAPABILITY, null) ?: continue
            if (action(wrapper)) {
                return true
            }
        }
        return false
    }
}
