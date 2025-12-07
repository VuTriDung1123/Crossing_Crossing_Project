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
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox

class LaneManager {
    private val grassModel: Model
    private val roadModel: Model
    private val carModel: Model
    private val stripeModel: Model
    private val treeModel: Model
    private val rockModel: Model
    private val tunnelModel: Model
    private val fenceModel: Model

    private val lanes = com.badlogic.gdx.utils.Array<ModelInstance>()
    private val stripes = com.badlogic.gdx.utils.Array<ModelInstance>()
    private val cars = com.badlogic.gdx.utils.Array<Car>()
    private val obstacles = com.badlogic.gdx.utils.Array<ModelInstance>()
    private val tunnels = com.badlogic.gdx.utils.Array<ModelInstance>()
    private val fences = com.badlogic.gdx.utils.Array<ModelInstance>()

    private var nextZ = 10f
    private var lastLaneWasRoad = false
    private val tempVector = Vector3()

    init {
        val modelBuilder = ModelBuilder()

        // 1. CỎ
        grassModel = modelBuilder.createBox(40f, 1f, 2f,
            Material(ColorAttribute.createDiffuse(Color.valueOf("76c442"))),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        // 2. ĐƯỜNG
        roadModel = modelBuilder.createBox(40f, 1f, 2f,
            Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        // 3. XE
        carModel = modelBuilder.createBox(2.2f, 1.2f, 1.2f,
            Material(ColorAttribute.createDiffuse(Color.BLUE)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        // 4. VẠCH KẺ
        stripeModel = modelBuilder.createBox(1.5f, 0.05f, 0.2f,
            Material(ColorAttribute.createDiffuse(Color.WHITE)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        // 5. CÂY
        treeModel = modelBuilder.createBox(1.2f, 3f, 1.2f,
            Material(ColorAttribute.createDiffuse(Color.valueOf("2d5a27"))),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        // 6. ĐÁ
        rockModel = modelBuilder.createBox(1.5f, 1.5f, 1.5f,
            Material(ColorAttribute.createDiffuse(Color.GRAY)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())

        // 7. ĐƯỜNG HẦM (To hơn một chút cho hoành tráng)
        tunnelModel = modelBuilder.createBox(5f, 5f, 2f,
            Material(ColorAttribute.createDiffuse(Color.BLACK)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())

        // 8. HÀNG RÀO
        fenceModel = modelBuilder.createBox(1f, 1.5f, 2f,
            Material(ColorAttribute.createDiffuse(Color.valueOf("8B4513"))),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())

        reset()
    }

    fun reset() {
        lanes.clear(); cars.clear(); stripes.clear(); obstacles.clear()
        tunnels.clear(); fences.clear()
        nextZ = 10f
        lastLaneWasRoad = false
        for (i in 0..40) spawnNextLane()
    }

    fun spawnNextLane() {
        val isSafeZone = (nextZ > -5f)
        val isRoad = if (isSafeZone) false else MathUtils.randomBoolean(0.5f)

        val instance = ModelInstance(if (isRoad) roadModel else grassModel)
        instance.transform.setToTranslation(0f, -0.5f, nextZ)
        lanes.add(instance)

        if (isRoad) {
            // --- SỬA ĐỔI Ở ĐÂY ---
            // Đưa hầm về sát mép đường (vị trí ±20f)
            val tunnelLeft = ModelInstance(tunnelModel)
            tunnelLeft.transform.setToTranslation(-20f, 2f, nextZ)
            tunnels.add(tunnelLeft)

            val tunnelRight = ModelInstance(tunnelModel)
            tunnelRight.transform.setToTranslation(20f, 2f, nextZ)
            tunnels.add(tunnelRight)

            // Sinh xe
            if (MathUtils.randomBoolean(0.4f)) {
                val speed = MathUtils.random(6f, 14f) * if (MathUtils.randomBoolean()) 1 else -1
                // Xe xuất phát từ sâu trong hầm (±30f là đủ)
                val startX = if (speed > 0) -30f else 30f
                val car = Car(carModel, startX, nextZ, speed)
                car.setColor(getRandomCarColor())
                cars.add(car)
            }
            if (lastLaneWasRoad) spawnStripeLine(nextZ + 1f)

        } else {
            // Hàng rào cũng ở mép cỏ (±20f)
            val fenceLeft = ModelInstance(fenceModel)
            fenceLeft.transform.setToTranslation(-20f, 0.5f, nextZ)
            fences.add(fenceLeft)

            val fenceRight = ModelInstance(fenceModel)
            fenceRight.transform.setToTranslation(20f, 0.5f, nextZ)
            fences.add(fenceRight)

            // Sinh cây/đá
            var x = -14f
            while (x <= 14f) {
                if (MathUtils.randomBoolean(0.25f) && !(isSafeZone && MathUtils.isEqual(x, 0f, 1f))) {
                    val isTree = MathUtils.randomBoolean(0.7f)
                    val obs = ModelInstance(if (isTree) treeModel else rockModel)
                    val y = if (isTree) 1f else 0.25f
                    obs.transform.setToTranslation(x, y, nextZ)
                    obstacles.add(obs)
                }
                x += 2f
            }
        }
        lastLaneWasRoad = isRoad
        nextZ -= 2f
    }

    // ... (Các hàm còn lại giữ nguyên) ...
    private fun spawnStripeLine(zPos: Float) {
        var x = -18f
        while (x <= 18f) {
            val stripe = ModelInstance(stripeModel)
            stripe.transform.setToTranslation(x, 0.02f, zPos)
            stripes.add(stripe)
            x += 3f
        }
    }
    private fun getRandomCarColor(): Color {
        return when (MathUtils.random(4)) { 0->Color.RED; 1->Color.BLUE; 2->Color.YELLOW; 3->Color.CYAN; else->Color.MAGENTA }
    }
    fun isBlocked(targetX: Float, targetZ: Float): Boolean {
        for (obs in obstacles) {
            obs.transform.getTranslation(tempVector)
            if (MathUtils.isEqual(targetX, tempVector.x, 0.8f) && MathUtils.isEqual(targetZ, tempVector.z, 0.8f)) return true
        }
        return false
    }

    fun update(delta: Float, playerZ: Float, playerBounds: BoundingBox): Boolean {
        if (playerZ < nextZ + 60f) spawnNextLane()
        val removeThreshold = playerZ + 25f

        while (lanes.notEmpty()) { val item = lanes.first(); item.transform.getTranslation(tempVector); if (tempVector.z > removeThreshold) lanes.removeIndex(0) else break }
        while (stripes.notEmpty()) { val item = stripes.first(); item.transform.getTranslation(tempVector); if (tempVector.z > removeThreshold) stripes.removeIndex(0) else break }
        while (obstacles.notEmpty()) { val item = obstacles.first(); item.transform.getTranslation(tempVector); if (tempVector.z > removeThreshold) obstacles.removeIndex(0) else break }
        while (cars.notEmpty()) { val item = cars.first(); if (item.position.z > removeThreshold) cars.removeIndex(0) else break }
        while (tunnels.notEmpty()) { val item = tunnels.first(); item.transform.getTranslation(tempVector); if (tempVector.z > removeThreshold) tunnels.removeIndex(0) else break }
        while (fences.notEmpty()) { val item = fences.first(); item.transform.getTranslation(tempVector); if (tempVector.z > removeThreshold) fences.removeIndex(0) else break }

        var hasCollision = false
        for (car in cars) {
            car.update(delta)
            if (car.checkCollision(playerBounds)) hasCollision = true
        }
        return hasCollision
    }

    fun render(batch: ModelBatch, env: Environment) {
        for (lane in lanes) batch.render(lane, env)
        for (stripe in stripes) batch.render(stripe, env)
        for (obs in obstacles) batch.render(obs, env)
        for (tunnel in tunnels) batch.render(tunnel, env)
        for (fence in fences) batch.render(fence, env)
        for (car in cars) car.render(batch, env)
    }

    fun dispose() {
        grassModel.dispose(); roadModel.dispose(); carModel.dispose()
        stripeModel.dispose(); treeModel.dispose(); rockModel.dispose()
        tunnelModel.dispose(); fenceModel.dispose()
    }
}
