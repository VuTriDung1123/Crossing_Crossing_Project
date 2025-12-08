package io.github.vutridung1123

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.collision.BoundingBox

class Player {
    private var model: Model
    private var instance: ModelInstance
    val bounds = BoundingBox()

    var currentX = 0f
    var currentZ = 0f
    private var startX = 0f
    private var startZ = 0f
    private var targetX = 0f
    private var targetZ = 0f

    var isJumping = false
    private var jumpTimer = 0f
    private val jumpDuration = 0.2f
    private val jumpHeight = 2f
    private val stepSize = 2f

    var groundY = 0.6f
    // ĐÃ XÓA BIẾN MAX_X -> TỰ DO VÔ TẬN!

    init {
        val modelBuilder = ModelBuilder()
        model = modelBuilder.createBox(1.2f, 1.2f, 1.2f,
            Material(ColorAttribute.createDiffuse(Color.WHITE)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        instance = ModelInstance(model)
        reset()
    }

    fun reset() {
        currentX = 0f; currentZ = 0f; targetX = 0f; targetZ = 0f
        isJumping = false
        updateTransform()
    }

    fun update(delta: Float) {
        if (isJumping) {
            jumpTimer += delta
            val progress = jumpTimer / jumpDuration
            if (progress >= 1f) {
                isJumping = false
                currentX = targetX
                currentZ = targetZ
                updateTransform()
            } else {
                val tempX = MathUtils.lerp(startX, targetX, progress)
                val tempZ = MathUtils.lerp(startZ, targetZ, progress)

                // SỬA CÔNG THỨC NHẢY:
                // Nhảy từ độ cao groundY lên, rồi rớt xuống độ cao groundY
                val heightY = groundY + MathUtils.sin(MathUtils.PI * progress) * jumpHeight
                instance.transform.setToTranslation(tempX, heightY, tempZ)
            }
        } else {
            // Nếu không nhảy, luôn giữ vị trí ở groundY
            updateTransform()
        }

        instance.calculateBoundingBox(bounds)
        bounds.mul(instance.transform)
    }

    private fun updateTransform() {
        instance.transform.setToTranslation(currentX, groundY, currentZ)
    }
    // --- DI CHUYỂN KHÔNG GIỚI HẠN ---
    fun moveLeft() {
        if (!isJumping) setupJump(currentX - stepSize, currentZ)
    }

    fun moveRight() {
        if (!isJumping) setupJump(currentX + stepSize, currentZ)
    }

    fun moveForward() {
        if (!isJumping) setupJump(currentX, currentZ - stepSize)
    }

    private fun setupJump(tx: Float, tz: Float) {
        startX = currentX; startZ = currentZ
        targetX = tx; targetZ = tz
        isJumping = true; jumpTimer = 0f
    }

    fun render(batch: ModelBatch, env: Environment) { batch.render(instance, env) }
    fun dispose() { model.dispose() }
}
