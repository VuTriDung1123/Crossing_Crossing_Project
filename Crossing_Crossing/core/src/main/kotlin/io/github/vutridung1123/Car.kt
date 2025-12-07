package io.github.vutridung1123

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox

class Car(model: Model, x: Float, z: Float, private val speed: Float) {
    private val instance: ModelInstance = ModelInstance(model)
    val position = Vector3(x, 0.5f, z) // 0.5 là nửa chiều cao xe
    private val bounds = BoundingBox() // Hộp va chạm

    init {
        instance.transform.setToTranslation(position)
    }

    fun update(delta: Float) {
        // Di chuyển xe theo trục X
        position.x += speed * delta
        instance.transform.setToTranslation(position)

        // Nếu xe chạy xa quá khỏi màn hình thì cho nó quay lại (tạo vòng lặp)
        if (speed > 0 && position.x > 30f) position.x = -30f
        if (speed < 0 && position.x < -30f) position.x = 30f
    }

    fun checkCollision(playerBounds: BoundingBox): Boolean {
        // Tính toán hộp va chạm của xe tại vị trí hiện tại
        instance.calculateBoundingBox(bounds)
        bounds.mul(instance.transform)

        // Kiểm tra xem hộp xe có cắt hộp người chơi không
        return bounds.intersects(playerBounds)
    }

    // --- ĐÂY LÀ HÀM BẠN ĐANG THIẾU ---
    fun setColor(color: Color) {
        // Lấy vật liệu (Material) đầu tiên của xe và đổi màu nó
        instance.materials.get(0).set(ColorAttribute.createDiffuse(color))
    }

    fun render(batch: ModelBatch, env: Environment) {
        batch.render(instance, env)
    }
}
