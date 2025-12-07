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
    val bounds = BoundingBox() // Hộp va chạm dùng để check với xe

    // Vị trí hiện tại và đích đến
    var currentX = 0f
    var currentZ = 0f
    private var startX = 0f
    private var startZ = 0f
    private var targetX = 0f
    private var targetZ = 0f

    // Biến trạng thái nhảy
    var isJumping = false
    private var jumpTimer = 0f
    private val jumpDuration = 0.2f // Tốc độ nhảy (càng nhỏ nhảy càng nhanh)
    private val jumpHeight = 2f     // Độ cao khi nhảy
    private val stepSize = 2f       // Khoảng cách mỗi bước (2m)

    // GIỚI HẠN DI CHUYỂN NGANG (Để không đi xuyên qua hầm)
    // Hầm đặt tại -20 và 20. Giới hạn 17 là an toàn để không bị kẹt.
    private val MAX_X = 17f

    init {
        val modelBuilder = ModelBuilder()
        // Tạo con gà (Hộp màu trắng 1.2 x 1.2 x 1.2)
        model = modelBuilder.createBox(1.2f, 1.2f, 1.2f,
            Material(ColorAttribute.createDiffuse(Color.WHITE)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        instance = ModelInstance(model)

        reset()
    }

    fun reset() {
        currentX = 0f
        currentZ = 0f
        targetX = 0f
        targetZ = 0f
        isJumping = false
        updateTransform()
    }

    fun update(delta: Float) {
        if (isJumping) {
            jumpTimer += delta
            val progress = jumpTimer / jumpDuration

            if (progress >= 1f) {
                // Kết thúc nhảy
                isJumping = false
                currentX = targetX
                currentZ = targetZ
                updateTransform()
            } else {
                // Đang nhảy: Nội suy vị trí (Lerp)
                val tempX = MathUtils.lerp(startX, targetX, progress)
                val tempZ = MathUtils.lerp(startZ, targetZ, progress)

                // Tạo đường cong hình Sin cho trục Y (nhảy lên xuống)
                val heightY = 0.6f + MathUtils.sin(MathUtils.PI * progress) * jumpHeight

                instance.transform.setToTranslation(tempX, heightY, tempZ)
            }
        }

        // Cập nhật hộp va chạm theo vị trí mới nhất của con gà
        instance.calculateBoundingBox(bounds)
        bounds.mul(instance.transform)
    }

    private fun updateTransform() {
        // 0.6f là nửa chiều cao của gà (1.2 / 2), để nó đứng trên mặt đất (y=0)
        instance.transform.setToTranslation(currentX, 0.6f, currentZ)
    }

    // --- CÁC HÀM DI CHUYỂN ---

    fun moveLeft() {
        // Kiểm tra: Nếu đang nhảy HOẶC đã chạm tường hầm bên trái thì không đi nữa
        if (!isJumping && currentX > -MAX_X) {
            setupJump(currentX - stepSize, currentZ)
        }
    }

    fun moveRight() {
        // Kiểm tra: Nếu đang nhảy HOẶC đã chạm tường hầm bên phải thì không đi nữa
        if (!isJumping && currentX < MAX_X) {
            setupJump(currentX + stepSize, currentZ)
        }
    }

    fun moveForward() {
        if (!isJumping) {
            // Đi tới thì Z giảm (về phía âm)
            setupJump(currentX, currentZ - stepSize)
        }
    }

    private fun setupJump(tx: Float, tz: Float) {
        startX = currentX
        startZ = currentZ
        targetX = tx
        targetZ = tz
        isJumping = true
        jumpTimer = 0f
    }

    fun render(batch: ModelBatch, env: Environment) {
        batch.render(instance, env)
    }

    fun dispose() {
        model.dispose()
    }
}
