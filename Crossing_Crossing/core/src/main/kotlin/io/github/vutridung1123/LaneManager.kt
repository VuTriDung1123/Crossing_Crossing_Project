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
import java.util.HashMap

class LaneManager {
    // --- MODELS ---
    private val grassModel: Model
    private val roadModel: Model
    private val waterModel: Model // Model nước
    private val carModel: Model
    private val logModel: Model   // Model gỗ
    private val stripeModel: Model
    private val treeModel: Model
    private val rockModel: Model

    // --- LISTS ---
    private val lanes = com.badlogic.gdx.utils.Array<ModelInstance>()
    private val stripes = com.badlogic.gdx.utils.Array<ModelInstance>()
    private val cars = com.badlogic.gdx.utils.Array<Car>()
    private val logs = com.badlogic.gdx.utils.Array<Log>() // Danh sách gỗ

    // Map lưu loại đường: Key = Z, Value = Int (0: Cỏ, 1: Đường, 2: Sông)
    private val laneTypes = HashMap<Float, Int>()

    // Temp instances
    private val treeInstance: ModelInstance
    private val rockInstance: ModelInstance
    private val tempVector = Vector3()

    private var nextZ = 10f
    private var lastLaneType = 0 // 0: Cỏ, 1: Đường, 2: Sông

    init {
        val modelBuilder = ModelBuilder()
        val attr = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()

        // 1. Các Model nền (10000f để trải dài vô tận 2 bên)
        grassModel = modelBuilder.createBox(10000f, 0.01f, 2f,
            Material(ColorAttribute.createDiffuse(Color.valueOf("76c442"))), attr)

        roadModel = modelBuilder.createBox(10000f, 0.01f, 2f,
            Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)), attr)

        waterModel = modelBuilder.createBox(10000f, 0.01f, 2f,
            Material(ColorAttribute.createDiffuse(Color.valueOf("29b6f6"))), attr) // Màu xanh dương

        // 2. Các Model vật thể
        carModel = modelBuilder.createBox(2.2f, 1.2f, 1.2f,
            Material(ColorAttribute.createDiffuse(Color.BLUE)), attr)

        logModel = modelBuilder.createBox(3f, 0.5f, 1.2f,
            Material(ColorAttribute.createDiffuse(Color.valueOf("795548"))), attr) // Màu nâu gỗ

        stripeModel = modelBuilder.createBox(1.5f, 0.05f, 0.2f,
            Material(ColorAttribute.createDiffuse(Color.WHITE)), attr)

        treeModel = modelBuilder.createBox(1.2f, 3f, 1.2f,
            Material(ColorAttribute.createDiffuse(Color.valueOf("2d5a27"))), attr)

        rockModel = modelBuilder.createBox(1.5f, 1.5f, 1.5f,
            Material(ColorAttribute.createDiffuse(Color.GRAY)), attr)

        treeInstance = ModelInstance(treeModel)
        rockInstance = ModelInstance(rockModel)

        reset()
    }

    fun reset() {
        lanes.clear(); cars.clear(); logs.clear(); stripes.clear(); laneTypes.clear()
        nextZ = 10f
        lastLaneType = 0
        // Sinh sẵn 40 làn đầu tiên
        for (i in 0..40) spawnNextLane()
    }

    // --- LOGIC SINH LÀN ĐƯỜNG MỚI ---
    fun spawnNextLane() {
        val isSafeZone = (nextZ > -5f) // Vùng an toàn lúc bắt đầu

        // Random loại đường: 0=Cỏ, 1=Đường, 2=Sông
        val currentType = if (isSafeZone) 0 else {
            val r = MathUtils.random(100)
            when {
                r < 40 -> 0 // 40% Cỏ
                r < 75 -> 1 // 35% Đường
                else -> 2   // 25% Sông
            }
        }

        // Lưu vào Map để check va chạm sau này
        laneTypes[nextZ] = currentType

        // 1. Tạo nền đường
        val modelToUse = when (currentType) {
            1 -> roadModel
            2 -> waterModel
            else -> grassModel
        }
        val instance = ModelInstance(modelToUse)
        instance.transform.setToTranslation(0f, 0f, nextZ)
        lanes.add(instance)

        // 2. Sinh vật thể theo loại đường
        if (currentType == 1) { // ĐƯỜNG NHỰA
            // Sinh xe ngẫu nhiên
            if (MathUtils.randomBoolean(0.4f)) {
                val speed = MathUtils.random(6f, 14f) * if (MathUtils.randomBoolean()) 1 else -1
                val car = Car(carModel, 0f, nextZ, speed)
                car.setColor(getRandomCarColor())
                cars.add(car)
            }
            // Vạch kẻ đường (nếu làn trước cũng là đường)
            if (lastLaneType == 1) spawnStripeLine(nextZ + 1f)

        } else if (currentType == 2) { // SÔNG
            // Luôn sinh gỗ trên sông
            val speed = MathUtils.random(3f, 7f) * if (MathUtils.randomBoolean()) 1 else -1

            // Sinh 3 khúc gỗ rải rác để người chơi nhảy
            for (k in 0..2) {
                // Random vị trí xuất phát để không bị thẳng hàng
                val startX = MathUtils.random(-15f, 15f) + (k * 10f)
                val log = Log(logModel, startX, nextZ, speed)
                logs.add(log)
            }
        }

        lastLaneType = currentType
        nextZ -= 2f
    }

    private fun spawnStripeLine(zPos: Float) {
        var x = -200f
        while (x <= 200f) {
            val stripe = ModelInstance(stripeModel)
            stripe.transform.setToTranslation(x, 0.05f, zPos)
            stripes.add(stripe)
            x += 6f
        }
    }

    // --- CHECK LOGIC GAME ---

    // Hàm 1: Kiểm tra có đi được vào ô đó không (Chặn bởi Cây/Đá)
    fun isBlocked(targetX: Float, targetZ: Float): Boolean {
        val gridX = MathUtils.round(targetX / 2f)
        val gridZ = MathUtils.round(targetZ / 2f)
        val zKey = gridZ * 2f

        val type = laneTypes[zKey] ?: return false

        // Nếu là Cỏ (Type 0) -> Check cây/đá
        if (type == 0) {
            // Vùng an toàn
            if (gridZ >= -2 && gridZ <= 5 && gridX == 0) return false
            return hasObstacleAt(gridX, gridZ) > 0
        }
        // Đường (1) và Sông (2) luôn đi vào được (Dù có thể chết)
        return false
    }

    // Hàm 2: Kiểm tra trạng thái trên sông
    // Trả về:
    // - Float.NaN: CHẾT (Rớt xuống nước)
    // - 0f: AN TOÀN (Trên cạn)
    // - Khác 0: AN TOÀN (Trên gỗ) -> Trả về tốc độ gỗ để player trôi theo
    fun checkRiverStatus(playerBounds: BoundingBox, playerZ: Float): Float {
        val zKey = MathUtils.round(playerZ / 2f) * 2f
        val type = laneTypes[zKey] ?: return 0f // Mặc định an toàn

        if (type == 2) { // Đang ở làn SÔNG
            // Check va chạm với Gỗ
            for (log in logs) {
                // Vì gỗ và người cùng z, ta check Bounds
                if (log.checkCollision(playerBounds)) {
                    return log.speed // Đứng trên gỗ -> Trôi theo
                }
            }
            return Float.NaN // Ở sông mà không chạm gỗ -> Chết đuối
        }
        return 0f // Không phải sông
    }

    // Hash function sinh Cây/Đá cố định
    private fun hasObstacleAt(gridX: Int, gridZ: Int): Int {
        var seed = gridX * 12345L + gridZ * 67890L
        seed = (seed xor (seed shl 13))
        seed = (seed xor (seed ushr 7))
        val randomVal = (seed and 0xFF).toInt()

        if (randomVal < 30) { // 30% có vật cản
            return if (randomVal % 2 == 0) 1 else 2 // 1: Cây, 2: Đá
        }
        return 0
    }

    fun update(delta: Float, playerX: Float, playerZ: Float, playerBounds: BoundingBox): Boolean {
        // Lazy Load
        if (playerZ < nextZ + 60f) spawnNextLane()

        val removeThreshold = playerZ + 30f

        // Dọn dẹp làn cũ
        while (lanes.notEmpty()) {
            val item = lanes.first()
            item.transform.getTranslation(tempVector)
            if (tempVector.z > removeThreshold) {
                lanes.removeIndex(0)
                laneTypes.remove(tempVector.z)
            } else break
        }

        // Dọn Stripes
        while (stripes.notEmpty()) {
            val item = stripes.first()
            item.transform.getTranslation(tempVector)
            if (tempVector.z > removeThreshold) stripes.removeIndex(0) else break
        }

        // Update Xe & Check va chạm
        var hasCarCollision = false
        for (car in cars) {
            car.update(delta, playerX)
            if (car.checkCollision(playerBounds)) hasCarCollision = true
        }
        // Dọn xe cũ
        while (cars.notEmpty()) { val item=cars.first(); if(item.position.z > removeThreshold) cars.removeIndex(0) else break }

        // Update Gỗ (Sông)
        for (log in logs) {
            log.update(delta, playerX)
        }
        // Dọn gỗ cũ
        while (logs.notEmpty()) { val item=logs.first(); if(item.position.z > removeThreshold) logs.removeIndex(0) else break }

        return hasCarCollision
    }

    fun render(batch: ModelBatch, env: Environment, playerX: Float, playerZ: Float) {
        // Vẽ làn đường & Vạch
        for (lane in lanes) batch.render(lane, env)
        for (stripe in stripes) batch.render(stripe, env)

        // Vẽ Xe & Gỗ
        for (car in cars) car.render(batch, env)
        for (log in logs) log.render(batch, env)

        // Vẽ Cây/Đá (Procedural)
        val pGridX = MathUtils.round(playerX / 2f)
        val pGridZ = MathUtils.round(playerZ / 2f)
        val viewRange = 14

        for (gz in (pGridZ - viewRange)..(pGridZ + viewRange)) {
            val zPos = gz * 2f
            val type = laneTypes[zPos] ?: 0

            // Chỉ vẽ cây/đá trên làn CỎ (Type 0)
            if (type == 0) {
                for (gx in (pGridX - viewRange)..(pGridX + viewRange)) {
                    if (gz >= -2 && gz <= 5 && gx == 0) continue // Safe zone

                    val obsType = hasObstacleAt(gx, gz)
                    if (obsType > 0) {
                        val instance = if (obsType == 1) treeInstance else rockInstance
                        val y = if (obsType == 1) 1.5f else 0.75f
                        instance.transform.setToTranslation(gx * 2f, y, zPos)
                        batch.render(instance, env)
                    }
                }
            }
        }
    }

    private fun getRandomCarColor(): Color {
        return when (MathUtils.random(4)) { 0->Color.RED; 1->Color.BLUE; 2->Color.YELLOW; 3->Color.CYAN; else->Color.MAGENTA }
    }

    fun dispose() {
        grassModel.dispose(); roadModel.dispose(); waterModel.dispose()
        carModel.dispose(); logModel.dispose()
        stripeModel.dispose(); treeModel.dispose(); rockModel.dispose()
    }
}
