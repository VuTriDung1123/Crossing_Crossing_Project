// File: Log.kt
package io.github.vutridung1123

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox

class Log(model: Model, x: Float, z: Float, val speed: Float) {
    private val instance: ModelInstance = ModelInstance(model)
    val position = Vector3(x, 0.4f, z) // Gỗ thấp hơn bờ một chút (Y=0.4)
    private val bounds = BoundingBox()
    val width = 3f // Giả sử khúc gỗ dài 3 đơn vị

    init {
        instance.transform.setToTranslation(position)
    }

    fun update(delta: Float, playerX: Float) {
        position.x += speed * delta

        // Logic lặp lại vô tận giống xe
        if (speed > 0) {
            if (position.x > playerX + 35f) position.x = playerX - 35f
        } else {
            if (position.x < playerX - 35f) position.x = playerX + 35f
        }
        instance.transform.setToTranslation(position)
    }

    fun checkCollision(playerBounds: BoundingBox): Boolean {
        instance.calculateBoundingBox(bounds)
        bounds.mul(instance.transform)
        return bounds.intersects(playerBounds)
    }

    fun render(batch: ModelBatch, env: Environment) {
        batch.render(instance, env)
    }
}
