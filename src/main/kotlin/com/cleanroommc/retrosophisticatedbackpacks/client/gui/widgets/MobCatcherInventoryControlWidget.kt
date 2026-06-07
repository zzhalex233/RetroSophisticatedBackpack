package com.cleanroommc.retrosophisticatedbackpacks.client.gui.widgets

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.api.layout.IViewportStack
import com.cleanroommc.modularui.api.widget.Interactable
import com.cleanroommc.modularui.screen.RichTooltip
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widget.Widget
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.mobcatcher.CapturedMob
import com.cleanroommc.retrosophisticatedbackpacks.capability.upgrade.mobcatcher.MobCatcherStorage
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.BackpackPanel
import com.cleanroommc.retrosophisticatedbackpacks.client.gui.RSBTextures
import com.cleanroommc.retrosophisticatedbackpacks.handler.NetworkHandler
import com.cleanroommc.retrosophisticatedbackpacks.network.C2SMobCatcherReleasePacket
import com.cleanroommc.retrosophisticatedbackpacks.util.Utils.asTranslationKey
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.EntityList
import net.minecraft.util.text.TextFormatting
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class MobCatcherInventoryControlWidget(private val panel: BackpackPanel) :
    Widget<MobCatcherInventoryControlWidget>(), Interactable {
    companion object {
        private const val SLOT_SIZE = 18
        private const val CAPTURED_MOB_BORDER_DARK = 0xFF171717.toInt()
        private const val CAPTURED_MOB_BORDER_LIGHT = 0xFF5A5A5A.toInt()
        private const val CAPTURED_MOB_BACKGROUND = 0xFF2B2B2B.toInt()
        private const val CAPTURED_MOB_BACKGROUND_ALT = 0xFF353535.toInt()
        private const val HEAD_STATIC_YAW_RANGE = 24f
        private const val HEAD_IDLE_YAW_AMPLITUDE = 33f
        private const val HEAD_STATIC_PITCH_RANGE = 6f
        private const val HEAD_IDLE_PITCH_AMPLITUDE = 10f
        private const val HEAD_IDLE_MIN_CYCLE_TICKS = 340f
        private const val HEAD_IDLE_CYCLE_VARIATION_TICKS = 180f
        private const val HEAD_IDLE_MOVE_TICKS = 30f
        private const val HEAD_IDLE_HOLD_TICKS = 15f
    }

    private val renderEntities = mutableMapOf<java.util.UUID, EntityLivingBase>()

    init {
        size(panel.backpackSlotsWidth, panel.colSize * SLOT_SIZE)
        tooltipAutoUpdate(true)
        tooltipDynamic { tooltip ->
            val hovered = hoveredCapturedMob(getContext().mouseX, getContext().mouseY) ?: return@tooltipDynamic
            val entity = renderEntity(hovered)
            tooltip.addLine(IKey.str(tooltipDisplayName(hovered, entity)))
            tooltip.addLine(
                IKey.lang("gui.mob_catcher.click_to_release".asTranslationKey())
                    .style(TextFormatting.GRAY, TextFormatting.ITALIC)
            )
            tooltip.addLine(
                IKey.str(
                    "${TextFormatting.RED}\u2665 ${hovered.currentHealth}/${hovered.maxHealth}"
                )
            )
            tooltip.pos(RichTooltip.Pos.NEXT_TO_MOUSE)
        }
    }

    override fun isInside(stack: IViewportStack, mx: Int, my: Int, absolute: Boolean): Boolean {
        val x = if (absolute) stack.unTransformX(mx.toFloat(), my.toFloat()) else mx
        val y = if (absolute) stack.unTransformY(mx.toFloat(), my.toFloat()) else my
        return hoveredCapturedMob(x, y) != null
    }

    override fun canHoverThrough(): Boolean = true

    override fun canClickThrough(): Boolean = true

    override fun onMousePressed(mouseButton: Int): Interactable.Result {
        if (mouseButton != 0) {
            return Interactable.Result.STOP
        }
        val capturedMob = hoveredCapturedMob(context.mouseX, context.mouseY) ?: return Interactable.Result.IGNORE
        NetworkHandler.INSTANCE.sendToServer(C2SMobCatcherReleasePacket(capturedMob.id))
        Interactable.playButtonClickSound()
        return Interactable.Result.SUCCESS
    }

    override fun draw(context: ModularGuiContext, widgetTheme: WidgetThemeEntry<*>) {
        panel.backpackWrapper.ensureCapturedMobLayoutCurrent()
        val theme = widgetTheme.theme
        val hoveredMob = hoveredCapturedMob(context.mouseX, context.mouseY)
        for (capturedMob in MobCatcherStorage.getCapturedMobs(panel.backpackWrapper)) {
            val slotX = capturedMob.slot % panel.rowSize * SLOT_SIZE
            val slotY = capturedMob.slot / panel.rowSize * SLOT_SIZE
            val width = capturedMob.width * SLOT_SIZE
            val height = capturedMob.height * SLOT_SIZE
            renderCapturedMobArea(slotX, slotY, capturedMob.width, capturedMob.height)
            renderEntity(capturedMob)?.let { entity ->
                val state = EntityRenderState.capture(entity)
                try {
                    prepareEntityForRender(entity, capturedMob, context)
                    val scale = getRenderScale(entity, width, height)
                    renderEntityInInventory(
                        slotX + width / 2,
                        getRenderBottomY(entity, slotY, height, scale),
                        scale,
                        entity,
                        context.partialTicks
                    )
                } finally {
                    state.restore(entity)
                }
            }
            if (hoveredMob?.id == capturedMob.id) {
                RSBTextures.SOLID_DOWN_ARROW_ICON.draw(context, slotX + width - 14, slotY + height - 14, 12, 12, theme)
            }
        }
    }

    private fun renderCapturedMobArea(x: Int, y: Int, widthSlots: Int, heightSlots: Int) {
        val backgroundX = x
        val backgroundY = y
        val width = widthSlots * SLOT_SIZE
        val height = heightSlots * SLOT_SIZE
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.color(1f, 1f, 1f, 1f)
        Gui.drawRect(backgroundX, backgroundY, backgroundX + width, backgroundY + height, CAPTURED_MOB_BORDER_DARK)
        Gui.drawRect(backgroundX + 1, backgroundY + 1, backgroundX + width - 1, backgroundY + height - 1, CAPTURED_MOB_BACKGROUND)
        Gui.drawRect(backgroundX + 2, backgroundY + 2, backgroundX + width - 2, backgroundY + height - 2, CAPTURED_MOB_BACKGROUND_ALT)
        Gui.drawRect(backgroundX + 1, backgroundY + 1, backgroundX + width - 1, backgroundY + 2, CAPTURED_MOB_BORDER_LIGHT)
        Gui.drawRect(backgroundX + 1, backgroundY + 1, backgroundX + 2, backgroundY + height - 1, CAPTURED_MOB_BORDER_LIGHT)
        Gui.drawRect(backgroundX + 1, backgroundY + height - 2, backgroundX + width - 1, backgroundY + height - 1, CAPTURED_MOB_BORDER_DARK)
        Gui.drawRect(backgroundX + width - 2, backgroundY + 1, backgroundX + width - 1, backgroundY + height - 1, CAPTURED_MOB_BORDER_DARK)
        GlStateManager.color(1f, 1f, 1f, 1f)
    }

    private fun renderEntityInInventory(posX: Int, posY: Float, scale: Int, entity: EntityLivingBase, partialTicks: Float) {
        val renderManager = Minecraft.getMinecraft().renderManager
        val previousViewY = renderManager.playerViewY
        val previousShadow = renderManager.isRenderShadow
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.enableColorMaterial()
        GlStateManager.enableDepth()
        GlStateManager.pushMatrix()
        try {
            GlStateManager.translate(posX.toFloat(), posY, 50f)
            GlStateManager.scale((-scale).toFloat(), scale.toFloat(), scale.toFloat())
            GlStateManager.rotate(180f, 0f, 0f, 1f)
            GlStateManager.rotate(135f, 0f, 1f, 0f)
            RenderHelper.enableStandardItemLighting()
            GlStateManager.rotate(-135f, 0f, 1f, 0f)
            GlStateManager.disableCull()
            renderManager.setPlayerViewY(180f)
            renderManager.setRenderShadow(false)
            renderManager.renderEntity(entity, 0.0, 0.0, 0.0, 0f, partialTicks, false)
        } finally {
            renderManager.setRenderShadow(previousShadow)
            renderManager.setPlayerViewY(previousViewY)
            GlStateManager.popMatrix()
            GlStateManager.enableCull()
            RenderHelper.disableStandardItemLighting()
            GlStateManager.disableRescaleNormal()
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit)
            GlStateManager.disableTexture2D()
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit)
            GlStateManager.disableDepth()
            GlStateManager.color(1f, 1f, 1f, 1f)
        }
    }

    private fun hoveredCapturedMob(mouseX: Int, mouseY: Int): CapturedMob? {
        panel.backpackWrapper.ensureCapturedMobLayoutCurrent()
        return MobCatcherStorage.getCapturedMobs(panel.backpackWrapper).firstOrNull { capturedMob ->
            val x = capturedMob.slot % panel.rowSize * SLOT_SIZE
            val y = capturedMob.slot / panel.rowSize * SLOT_SIZE
            mouseX >= x && mouseX < x + capturedMob.width * SLOT_SIZE &&
                    mouseY >= y && mouseY < y + capturedMob.height * SLOT_SIZE
        }
    }

    private fun renderEntity(capturedMob: CapturedMob): EntityLivingBase? {
        renderEntities[capturedMob.id]?.let { return it }
        val world = Minecraft.getMinecraft().world ?: return null
        val entity = EntityList.createEntityByIDFromName(capturedMob.entityType, world) as? EntityLivingBase ?: return null
        entity.readFromNBT(capturedMob.entityNbt.copy() as net.minecraft.nbt.NBTTagCompound)
        renderEntities[capturedMob.id] = entity
        return entity
    }

    private fun tooltipDisplayName(capturedMob: CapturedMob, entity: EntityLivingBase?): String =
        when {
            entity == null -> capturedMob.displayName
            entity.hasCustomName() -> entity.customNameTag
            else -> entity.displayName.unformattedText
        }

    private fun prepareEntityForRender(entity: EntityLivingBase, capturedMob: CapturedMob, context: ModularGuiContext) {
        val renderTime = renderTime(capturedMob, context)
        resetCapturedEntityVisualState(entity)
        entity.setLocationAndAngles(0.0, 0.0, 0.0, 0f, 0f)
        val bodyRot = 0f
        val headOffset = -HEAD_STATIC_YAW_RANGE / 2f + uuidFloat(capturedMob.id, 1) * HEAD_STATIC_YAW_RANGE +
                idlePoseOffset(capturedMob.id, renderTime, 0, HEAD_IDLE_YAW_AMPLITUDE)
        val pitch = -HEAD_STATIC_PITCH_RANGE / 2f + uuidFloat(capturedMob.id, 2) * HEAD_STATIC_PITCH_RANGE +
                idlePoseOffset(capturedMob.id, renderTime, 1, HEAD_IDLE_PITCH_AMPLITUDE)
        entity.ticksExisted = renderTime.toInt()
        entity.renderYawOffset = bodyRot
        entity.prevRenderYawOffset = bodyRot
        entity.rotationYaw = bodyRot
        entity.prevRotationYaw = bodyRot
        entity.rotationYawHead = bodyRot + headOffset
        entity.prevRotationYawHead = entity.rotationYawHead
        entity.rotationPitch = pitch
        entity.prevRotationPitch = pitch
    }

    private fun resetCapturedEntityVisualState(entity: EntityLivingBase) {
        entity.hurtResistantTime = 0
        entity.hurtTime = 0
        entity.maxHurtTime = 0
        entity.deathTime = 0
        entity.attackedAtYaw = 0f
        entity.limbSwing = 0f
        entity.limbSwingAmount = 0f
        entity.prevLimbSwingAmount = 0f
    }

    private fun renderTime(capturedMob: CapturedMob, context: ModularGuiContext): Float {
        val player = Minecraft.getMinecraft().player
        val baseTime = player?.ticksExisted?.toFloat() ?: context.tick.toFloat()
        return baseTime + context.partialTicks + uuidFloat(capturedMob.id, 3) * 200f
    }

    private fun idlePoseOffset(uuid: java.util.UUID, renderTime: Float, salt: Int, amplitude: Float): Float {
        val cycleLength = HEAD_IDLE_MIN_CYCLE_TICKS + uuidFloat(uuid, salt + 4) * HEAD_IDLE_CYCLE_VARIATION_TICKS
        val cycle = floor(renderTime / cycleLength).toInt()
        val phase = renderTime - cycle * cycleLength
        val target = (cycleFloat(uuid, cycle, salt) - 0.5f) * 2f * amplitude
        return when {
            phase < HEAD_IDLE_MOVE_TICKS -> smoothStep(phase / HEAD_IDLE_MOVE_TICKS) * target
            phase < HEAD_IDLE_MOVE_TICKS + HEAD_IDLE_HOLD_TICKS -> target
            phase < HEAD_IDLE_MOVE_TICKS * 2f + HEAD_IDLE_HOLD_TICKS ->
                (1f - smoothStep((phase - HEAD_IDLE_MOVE_TICKS - HEAD_IDLE_HOLD_TICKS) / HEAD_IDLE_MOVE_TICKS)) * target
            else -> 0f
        }
    }

    private fun smoothStep(value: Float): Float =
        value * value * (3f - 2f * value)

    private fun cycleFloat(uuid: java.util.UUID, cycle: Int, salt: Int): Float =
        Math.floorMod(uuid.hashCode() * 31 + cycle * 131 + salt * 17, 1000) / 999f

    private fun uuidFloat(uuid: java.util.UUID, salt: Int): Float =
        Math.floorMod(uuid.hashCode() + salt * 31, 1000) / 999f

    private fun getRenderScale(entity: EntityLivingBase, width: Int, height: Int): Int {
        val entityWidth = max(entity.width, 0.25f)
        val entityHeight = max(entity.height, 0.25f)
        return max(8, (min((width - 6) / entityWidth, (height - 6) / entityHeight) * 0.5625f).toInt())
    }

    private fun getRenderBottomY(entity: EntityLivingBase, y: Int, height: Int, scale: Int): Float =
        y + height / 2f + entity.height * scale / 2f

    private data class EntityRenderState(
        val renderYawOffset: Float,
        val prevRenderYawOffset: Float,
        val rotationYaw: Float,
        val prevRotationYaw: Float,
        val rotationYawHead: Float,
        val prevRotationYawHead: Float,
        val rotationPitch: Float,
        val prevRotationPitch: Float,
        val ticksExisted: Int,
        val hurtResistantTime: Int,
        val hurtTime: Int,
        val maxHurtTime: Int,
        val deathTime: Int,
        val attackedAtYaw: Float,
        val limbSwing: Float,
        val limbSwingAmount: Float,
        val prevLimbSwingAmount: Float
    ) {
        fun restore(entity: EntityLivingBase) {
            entity.renderYawOffset = renderYawOffset
            entity.prevRenderYawOffset = prevRenderYawOffset
            entity.rotationYaw = rotationYaw
            entity.prevRotationYaw = prevRotationYaw
            entity.rotationYawHead = rotationYawHead
            entity.prevRotationYawHead = prevRotationYawHead
            entity.rotationPitch = rotationPitch
            entity.prevRotationPitch = prevRotationPitch
            entity.ticksExisted = ticksExisted
            entity.hurtResistantTime = hurtResistantTime
            entity.hurtTime = hurtTime
            entity.maxHurtTime = maxHurtTime
            entity.deathTime = deathTime
            entity.attackedAtYaw = attackedAtYaw
            entity.limbSwing = limbSwing
            entity.limbSwingAmount = limbSwingAmount
            entity.prevLimbSwingAmount = prevLimbSwingAmount
        }

        companion object {
            fun capture(entity: EntityLivingBase): EntityRenderState =
                EntityRenderState(
                    entity.renderYawOffset,
                    entity.prevRenderYawOffset,
                    entity.rotationYaw,
                    entity.prevRotationYaw,
                    entity.rotationYawHead,
                    entity.prevRotationYawHead,
                    entity.rotationPitch,
                    entity.prevRotationPitch,
                    entity.ticksExisted,
                    entity.hurtResistantTime,
                    entity.hurtTime,
                    entity.maxHurtTime,
                    entity.deathTime,
                    entity.attackedAtYaw,
                    entity.limbSwing,
                    entity.limbSwingAmount,
                    entity.prevLimbSwingAmount
                )
        }
    }
}
