package io.github.vutridung1123

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport

class Main_Core : ApplicationAdapter() {
    // --- KHAI BÁO BIẾN ---
    private lateinit var camera: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var environment: Environment

    // Các đối tượng game logic
    private lateinit var player: Player
    private lateinit var laneManager: LaneManager

    // UI (Giao diện)
    private lateinit var stage: Stage
    private lateinit var buttonTexture: Texture
    private lateinit var font: BitmapFont
    private lateinit var gameOverTable: Table

    // Trạng thái game
    private var isGameOver = false

    // Bước nhảy (Phải khớp với Player và LaneManager, thường là 2.0f)
    private val STEP_SIZE = 2f

    // Biến cho Camera mượt (Lerp)
    private val cameraPos = Vector3(0f, 15f, 10f)
    private val cameraTarget = Vector3(0f, 0f, 0f)

    override fun create() {
        // 1. Cấu hình Camera
        camera = PerspectiveCamera(60f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.near = 1f
        camera.far = 300f

        // Đặt vị trí camera ban đầu (Cao và Xa)
        cameraPos.set(10f, 20f, 15f)
        camera.position.set(cameraPos)
        camera.lookAt(0f, 0f, 0f)
        camera.update()

        // 2. Ánh sáng môi trường
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -0.5f, -1f, -0.5f))

        modelBatch = ModelBatch()

        // 3. Khởi tạo Logic Game
        laneManager = LaneManager() // Tạo đường xá, xe cộ, cây cối
        player = Player()           // Tạo nhân vật

        // 4. Tạo Giao diện
        createUI()
    }

    private fun createUI() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage // Bắt buộc: Để nhận diện cảm ứng nút bấm

        // Tạo ảnh màu trắng mờ làm nền cho nút (không cần tải ảnh ngoài)
        val pixmap = Pixmap(200, 200, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color(1f, 1f, 1f, 0.5f)) // Trắng, trong suốt 50%
        pixmap.fill()
        buttonTexture = Texture(pixmap)
        pixmap.dispose()
        val drawable = TextureRegionDrawable(buttonTexture)

        // Font chữ
        font = BitmapFont()
        font.data.setScale(3f) // Phóng to chữ lên

        // --- NÚT TRÁI ---
        val btnLeft = Image(drawable)
        btnLeft.setSize(150f, 150f)
        btnLeft.setPosition(50f, 100f)
        btnLeft.color = Color.ORANGE
        btnLeft.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (!isGameOver && !player.isJumping) {
                    val nextX = player.currentX - STEP_SIZE
                    val nextZ = player.currentZ
                    // Chỉ di chuyển nếu KHÔNG bị chặn bởi cây/đá
                    if (!laneManager.isBlocked(nextX, nextZ)) {
                        player.moveLeft()
                    }
                }
            }
        })

        // --- NÚT PHẢI ---
        val btnRight = Image(drawable)
        btnRight.setSize(150f, 150f)
        btnRight.setPosition(Gdx.graphics.width - 200f, 100f)
        btnRight.color = Color.ORANGE
        btnRight.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (!isGameOver && !player.isJumping) {
                    val nextX = player.currentX + STEP_SIZE
                    val nextZ = player.currentZ
                    if (!laneManager.isBlocked(nextX, nextZ)) {
                        player.moveRight()
                    }
                }
            }
        })

        // --- NÚT TIẾN LÊN (Ở GIỮA) ---
        val btnUp = Image(drawable)
        btnUp.setSize(300f, 200f)
        btnUp.setPosition(Gdx.graphics.width / 2f - 150f, 50f)
        btnUp.color = Color.CYAN
        btnUp.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (!isGameOver && !player.isJumping) {
                    val nextX = player.currentX
                    val nextZ = player.currentZ - STEP_SIZE // Trục Z âm là đi tới
                    if (!laneManager.isBlocked(nextX, nextZ)) {
                        player.moveForward()
                    }
                }
            }
        })

        // Thêm nút vào sân khấu
        stage.addActor(btnLeft)
        stage.addActor(btnRight)
        stage.addActor(btnUp)

        // --- BẢNG GAME OVER ---
        gameOverTable = Table()
        gameOverTable.setFillParent(true) // Phủ kín màn hình
        gameOverTable.isVisible = false   // Ẩn đi lúc đầu

        val labelStyle = Label.LabelStyle(font, Color.RED)
        val loseLabel = Label("BAN DA THUA!", labelStyle)

        val btnStyle = TextButton.TextButtonStyle()
        btnStyle.font = font
        btnStyle.up = drawable

        val restartBtn = TextButton("CHOI LAI", btnStyle)
        restartBtn.color = Color.GREEN
        restartBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                restartGame()
            }
        })

        gameOverTable.add(loseLabel).padBottom(50f).row()
        gameOverTable.add(restartBtn).width(400f).height(150f)

        stage.addActor(gameOverTable)
    }

    private fun restartGame() {
        isGameOver = false
        player.reset()
        laneManager.reset()
        gameOverTable.isVisible = false

        // Reset camera về vị trí đầu
        cameraPos.set(10f, 20f, 15f)
        cameraTarget.set(0f, 0f, 0f)
    }

    override fun render() {
        val delta = Gdx.graphics.deltaTime

        // --- 1. XỬ LÝ LOGIC ---
        if (!isGameOver) {
            player.update(delta)

            // LaneManager update trả về true nếu có xe tông trúng playerBounds
            if (laneManager.update(delta, player.currentZ, player.bounds)) {
                isGameOver = true
                gameOverTable.isVisible = true // Hiện bảng thua
            }

            // Camera bám theo (Lerp - Làm mượt chuyển động)
            val targetCamX = player.currentX + 10f
            val targetCamZ = player.currentZ + 15f

            cameraPos.x += (targetCamX - cameraPos.x) * 5f * delta
            cameraPos.z += (targetCamZ - cameraPos.z) * 5f * delta
            camera.position.set(cameraPos)

            cameraTarget.x += (player.currentX - cameraTarget.x) * 5f * delta
            cameraTarget.z += (player.currentZ - cameraTarget.z) * 5f * delta
            camera.lookAt(cameraTarget.x, 0f, cameraTarget.z)

            camera.update()
        }

        // --- 2. VẼ HÌNH ---
        // Xóa màn hình (Màu xanh da trời)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.4f, 0.7f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Vẽ thế giới 3D
        modelBatch.begin(camera)
        laneManager.render(modelBatch, environment)
        player.render(modelBatch, environment)
        modelBatch.end()

        // Vẽ giao diện 2D (Nút bấm)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    override fun dispose() {
        modelBatch.dispose()
        player.dispose()
        laneManager.dispose()
        stage.dispose()
        buttonTexture.dispose()
        font.dispose()
    }
}
