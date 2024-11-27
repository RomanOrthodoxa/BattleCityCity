package ru.appngo.tankstutorial.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ru.appngo.tankstutorial.GameCore
import ru.appngo.tankstutorial.LevelStorage
import ru.appngo.tankstutorial.R
import ru.appngo.tankstutorial.databinding.ActivityMainBinding
import ru.appngo.tankstutorial.drawers.BulletDrawer
import ru.appngo.tankstutorial.drawers.ElementsDrawer
import ru.appngo.tankstutorial.drawers.EnemyDrawer
import ru.appngo.tankstutorial.drawers.GridDrawer
import ru.appngo.tankstutorial.enums.Direction
import ru.appngo.tankstutorial.enums.Material
import ru.appngo.tankstutorial.models.Coordinate
import ru.appngo.tankstutorial.models.Element
import ru.appngo.tankstutorial.models.Tank
import ru.appngo.tankstutorial.sounds.MainSoundPlayer

const val CELL_SIZE = 50
const val VERTICAL_CELL_AMOUNT = 38
const val HORIZONTAL_CELL_AMOUNT = 25
const val VERTICAL_MAX_SIZE = CELL_SIZE * VERTICAL_CELL_AMOUNT
const val HORIZONTAL_MAX_SIZE = CELL_SIZE * HORIZONTAL_CELL_AMOUNT
const val HALF_WIDTH_OF_CONTAINER = VERTICAL_MAX_SIZE / 2

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var editMode = false
    private lateinit var item: MenuItem
    private val playerTank by lazy {
        Tank(
            Element(
                material = Material.PLAYER_TANK,
                coordinate = getPlayerTankCoordinate()
            ), Direction.UP, enemyDrawer
        )
    }

    private val bulletDrawer by lazy {
        BulletDrawer(
            binding.container,
            elementsDrawer.elementsOnContainer,
            enemyDrawer,
            soundManager,
            gameCore
        )
    }

    private val gameCore by lazy {
        GameCore(this)
    }

    private val soundManager by lazy {
        MainSoundPlayer(this)
    }

    private fun getPlayerTankCoordinate() = Coordinate(
        top = HORIZONTAL_MAX_SIZE - Material.PLAYER_TANK.height * CELL_SIZE,
        left = HALF_WIDTH_OF_CONTAINER - 8 * CELL_SIZE
    )

    private val eagle = Element(
        material = Material.EAGLE,
        coordinate = getEagleCoordinate()
    )

    private fun getEagleCoordinate() = Coordinate(
        top = HORIZONTAL_MAX_SIZE - Material.EAGLE.height * CELL_SIZE,
        left = HALF_WIDTH_OF_CONTAINER - Material.EAGLE.width * CELL_SIZE / 2
    )

    private val gridDrawer by lazy {
        GridDrawer(binding.container)
    }

    private val elementsDrawer by lazy {
        ElementsDrawer(binding.container)
    }

    private val levelStorage by lazy {
        LevelStorage(this)
    }

    private val enemyDrawer by lazy {
        EnemyDrawer(
            binding.container,
            elementsDrawer.elementsOnContainer,
            soundManager,
            gameCore
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        soundManager.loadSounds()
        enemyDrawer.bulletDrawer = bulletDrawer

        // Устанавливаем размеры контейнера
        binding.container.layoutParams = FrameLayout.LayoutParams(
            VERTICAL_MAX_SIZE,
            HORIZONTAL_MAX_SIZE
        )

        // Настроим кнопки редактора
        binding.editorClear.setOnClickListener { elementsDrawer.currentMaterial = Material.EMPTY }
        binding.editorBrick.setOnClickListener { elementsDrawer.currentMaterial = Material.BRICK }
        binding.editorConcrete.setOnClickListener { elementsDrawer.currentMaterial = Material.CONCRETE }
        binding.editorGrass.setOnClickListener { elementsDrawer.currentMaterial = Material.GRASS }

        // Настроим обработчик для контейнера
        binding.container.setOnTouchListener { _, event ->
            if (!editMode) {
                return@setOnTouchListener true
            }
            elementsDrawer.onTouchContainer(event.x, event.y)
            return@setOnTouchListener true
        }

        elementsDrawer.drawElementsList(levelStorage.loadLevel())
        elementsDrawer.drawElementsList(listOf(playerTank.element, eagle))

        hideSettings()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings, menu)
        item = menu.findItem(R.id.menu_play)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                switchEditMode()
                true
            }
            R.id.menu_save -> {
                levelStorage.saveLevel(elementsDrawer.elementsOnContainer)
                true
            }
            R.id.menu_play -> {
                if (editMode) {
                    return true
                }
                gameCore.startOrPauseTheGame()
                if (gameCore.isPlaying()) {
                    startTheGame()
                } else {
                    pauseTheGame()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startTheGame() {
        item.icon = ContextCompat.getDrawable(this, R.drawable.ic_pause)
        enemyDrawer.startEnemyCreation()
        soundManager.playIntroMusic()
    }

    private fun pauseTheGame() {
        item.icon = ContextCompat.getDrawable(this, R.drawable.ic_play)
        gameCore.pauseTheGame()
        soundManager.pauseSounds()
    }

    override fun onPause() {
        super.onPause()
        pauseTheGame()
    }

    private fun switchEditMode() {
        editMode = !editMode
        if (editMode) {
            showSettings()
        } else {
            hideSettings()
        }
    }

    private fun showSettings() {
        gridDrawer.drawGrid()
        binding.materialsContainer.visibility = View.VISIBLE
    }

    private fun hideSettings() {
        gridDrawer.removeGrid()
        binding.materialsContainer.visibility = View.GONE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!gameCore.isPlaying()) {
            return super.onKeyDown(keyCode, event)
        }
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> onButtonPressed(Direction.UP)
            KeyEvent.KEYCODE_DPAD_LEFT -> onButtonPressed(Direction.LEFT)
            KeyEvent.KEYCODE_DPAD_DOWN -> onButtonPressed(Direction.BOTTOM)
            KeyEvent.KEYCODE_DPAD_RIGHT -> onButtonPressed(Direction.RIGHT)
            KeyEvent.KEYCODE_SPACE -> bulletDrawer.addNewBulletForTank(playerTank)
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun onButtonPressed(direction: Direction) {
        soundManager.tankMove()
        playerTank.move(direction, binding.container, elementsDrawer.elementsOnContainer)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> onButtonReleased()
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun onButtonReleased() {
        if (enemyDrawer.tanks.isEmpty()) {
            soundManager.tankStop()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == SCORE_REQUEST_CODE) {
            recreate()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
