package io.github.vutridung1123

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport

class Main_Core : ApplicationAdapter() {
    enum class State { MENU, PLAYING, GAMEOVER }

    private var currentState = State.MENU

    private lateinit var camera: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var player: Player
    private lateinit var laneManager: LaneManager

    // UI
    private lateinit var stage: Stage
    private lateinit var buttonTexture: Texture
    private lateinit var font: BitmapFont
    private lateinit var drawable: TextureRegionDrawable
    private lateinit var scoreLabel: Label

    // Data
    private var score = 0
    private var highScore = 0
    private lateinit var prefs: Preferences
    private val STEP_SIZE = 2f

    // Camera vars
    private val cameraPos = Vector3(10f, 20f, 15f)
    private val cameraTarget = Vector3(0f, 0f, 0f)

    override fun create() {
        // Setup Camera
        camera = PerspectiveCamera(60f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.near = 0.1f
        camera.far = 300f
        camera.position.set(cameraPos)
        camera.lookAt(0f, 0f, 0f)
        camera.update()

        // Setup Light & Environment
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f))

        modelBatch = ModelBatch()
        laneManager = LaneManager()
        player = Player()

        prefs = Gdx.app.getPreferences("CrossyRoadPrefs")
        highScore = prefs.getInteger("highScore", 0)

        initUIResources()
        showMenu()
    }

    private fun initUIResources() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        val pixmap = Pixmap(200, 200, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color(1f, 1f, 1f, 0.3f))
        pixmap.fill()
        buttonTexture = Texture(pixmap)
        pixmap.dispose()
        drawable = TextureRegionDrawable(buttonTexture)

        font = BitmapFont()
        font.data.setScale(3f)
    }

    private fun showMenu() {
        currentState = State.MENU
        stage.clear()

        val table = Table(); table.setFillParent(true)
        val title = Label("CROSSY ROAD", Label.LabelStyle(font, Color.YELLOW))
        title.setFontScale(4f)
        val record = Label("KY LUC: $highScore", Label.LabelStyle(font, Color.WHITE))

        val btnStyle = TextButton.TextButtonStyle(); btnStyle.font = font; btnStyle.up = drawable
        val playBtn = TextButton("CHOI NGAY", btnStyle); playBtn.color = Color.GREEN
        playBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { startGame() }
        })

        table.add(title).padBottom(20f).row()
        table.add(record).padBottom(80f).row()
        table.add(playBtn).width(400f).height(150f)
        stage.addActor(table)

        player.reset()
        laneManager.reset()
        cameraPos.set(10f, 20f, 15f)
    }

    private fun startGame() {
        currentState = State.PLAYING
        stage.clear()
        score = 0

        scoreLabel = Label("0", Label.LabelStyle(font, Color.WHITE))
        scoreLabel.setFontScale(4f)
        scoreLabel.setPosition(50f, Gdx.graphics.height - 100f)
        stage.addActor(scoreLabel)

        // Nút Trái
        val btnLeft = Image(drawable)
        btnLeft.setSize(150f, 150f); btnLeft.setPosition(50f, 100f); btnLeft.color = Color.ORANGE
        btnLeft.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (currentState == State.PLAYING && !player.isJumping) {
                    if (!laneManager.isBlocked(player.currentX - STEP_SIZE, player.currentZ))
                        player.moveLeft()
                }
            }
        })

        // Nút Phải
        val btnRight = Image(drawable)
        btnRight.setSize(150f, 150f); btnRight.setPosition(Gdx.graphics.width - 200f, 100f); btnRight.color = Color.ORANGE
        btnRight.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (currentState == State.PLAYING && !player.isJumping) {
                    if (!laneManager.isBlocked(player.currentX + STEP_SIZE, player.currentZ))
                        player.moveRight()
                }
            }
        })

        // Nút Lên
        val btnUp = Image(drawable)
        btnUp.setSize(300f, 200f); btnUp.setPosition(Gdx.graphics.width / 2f - 150f, 50f); btnUp.color = Color.CYAN
        btnUp.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (currentState == State.PLAYING && !player.isJumping) {
                    // Reset trạng thái gỗ để tránh lỗi nhảy
                    if (!laneManager.isBlocked(player.currentX, player.currentZ - STEP_SIZE)) {
                        player.moveForward()
                        score++
                        scoreLabel.setText(score.toString())
                    }
                }
            }
        })

        stage.addActor(btnLeft); stage.addActor(btnRight); stage.addActor(btnUp)
    }

    private fun showGameOver() {
        currentState = State.GAMEOVER
        if (score > highScore) {
            highScore = score
            prefs.putInteger("highScore", highScore)
            prefs.flush()
        }

        val table = Table(); table.setFillParent(true)
        val loseLbl = Label("BAN DA THUA!", Label.LabelStyle(font, Color.RED))
        val scoreLbl = Label("DIEM: $score", Label.LabelStyle(font, Color.YELLOW))

        val btnStyle = TextButton.TextButtonStyle(); btnStyle.font = font; btnStyle.up = drawable
        val retryBtn = TextButton("CHOI LAI", btnStyle); retryBtn.color = Color.GREEN
        retryBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                player.reset(); laneManager.reset(); cameraPos.set(10f, 20f, 15f)
                startGame()
            }
        })

        val homeBtn = TextButton("TRANG CHU", btnStyle); homeBtn.color = Color.BLUE
        homeBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) { showMenu() }
        })

        table.add(loseLbl).padBottom(20f).row()
        table.add(scoreLbl).padBottom(50f).row()
        table.add(retryBtn).width(300f).height(120f).padBottom(20f).row()
        table.add(homeBtn).width(300f).height(120f)
        stage.addActor(table)
    }

    override fun render() {
        val delta = Gdx.graphics.deltaTime

        // Background color
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.4f, 0.7f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // LOGIC LOOP
        when (currentState) {
            State.MENU -> cameraPos.x += delta * 2f

            State.PLAYING -> {
                player.update(delta)

                // 1. Check xe tông
                val isCarHit = laneManager.update(delta, player.currentX, player.currentZ, player.bounds)
                if (isCarHit) showGameOver()

                // 2. Check Sông Nước & Gỗ
                if (!player.isJumping) {
                    val riverStatus = laneManager.checkRiverStatus(player.bounds, player.currentZ)

                    if (riverStatus.isNaN()) {
                        // ==> CHẾT ĐUỐI (Rớt xuống nước)
                        // Hiệu ứng phụ: Cho nhân vật chìm xuống một chút cho thật
                        player.groundY = -0.5f
                        showGameOver()
                    }
                    else if (riverStatus != 0f) {
                        // ==> ĐANG TRÊN GỖ (AN TOÀN)
                        // Logic: Trôi theo gỗ
                        player.currentX += riverStatus * delta

                        // Visual: Nâng độ cao nhân vật lên!
                        // Gỗ cao 0.5f, nằm ở y=0. nên mặt gỗ tầm y=0.5 -> 0.6
                        // Ta set player cao hơn bình thường (0.6f -> 1.1f) để đứng HẲN lên trên gỗ
                        player.groundY = 1.1f
                    }
                    else {
                        // ==> ĐANG TRÊN ĐẤT LIỀN / ĐƯỜNG
                        player.groundY = 0.6f // Trả về độ cao chuẩn
                    }
                }
            }
            State.GAMEOVER -> { }
        }

        // Camera Follow Logic
        if (currentState != State.MENU) {
            val targetCamX = player.currentX + 10f
            val targetCamZ = player.currentZ + 15f

            // Camera bám theo mượt mà (Lerp)
            cameraPos.x += (targetCamX - cameraPos.x) * 5f * delta
            cameraPos.z += (targetCamZ - cameraPos.z) * 5f * delta
            camera.position.set(cameraPos)

            cameraTarget.x += (player.currentX - cameraTarget.x) * 5f * delta
            cameraTarget.z += (player.currentZ - cameraTarget.z) * 5f * delta
            camera.lookAt(cameraTarget.x, 0f, cameraTarget.z)
        } else {
            // Menu camera
            camera.position.set(cameraPos.x + 10f, 20f, 15f)
            camera.lookAt(cameraPos.x, 0f, 0f)
        }
        camera.update()

        // Render 3D
        modelBatch.begin(camera)
        laneManager.render(modelBatch, environment, player.currentX, player.currentZ)
        player.render(modelBatch, environment)
        modelBatch.end()

        // Render UI
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
        modelBatch.dispose(); player.dispose(); laneManager.dispose()
        stage.dispose(); buttonTexture.dispose(); font.dispose()
    }
}
