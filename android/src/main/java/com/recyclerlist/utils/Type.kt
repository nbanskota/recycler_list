package com.recyclerlist.utils

import android.content.Context
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import androidx.core.view.GestureDetectorCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import kotlin.math.abs

enum class Type(val value: String) {
    Touch("Touch"), KeyPress("KeyPress")
}

enum class EventKeyType(val value: Int) {
    UNKNOWN(0),
    TAB(9), ENTER(13), ESCAPE(27), SPACE(32), DELETE(127),
    SELECT(300), PLAY_PAUSE(301), PLAY(302), PAUSE(303),
    CLOSE(304), NEXT(305), PREVIOUS(306), REWIND(307), FAST_FORWARD(308),
    RECORD(309), STOP(310), INFO(311), CAPTIONS(312),
    MUTE(313), VOLUME_UP(314), VOLUME_DOWN(315), MENU(316), BACK(317),
    LEFT(318), RIGHT(319), UP(320), DOWN(321), PAGE_UP(322), PAGE_DOWN(323)
}

data class EventResult(
    var type: Type,
    var isShiftPressed: Boolean = false,
    var isLongPressed: Boolean = false,
    var isDown: Boolean = false,
    var keyCode: Int = 0,
    var timeStamp: Long = 0,
    var nativeKeyCode: Int? = null
) {
    fun toWritableMap(): WritableMap {
        val writableMap: WritableMap = Arguments.createMap()
        writableMap.putString("type", type.name)
        writableMap.putBoolean("isDown", isDown)
        writableMap.putInt("keyCode", keyCode)
        writableMap.putBoolean("isShiftPressed", isShiftPressed)
        writableMap.putBoolean("isLongPressed", isLongPressed)
        writableMap.putInt("nativeKeyCode", nativeKeyCode ?: 0)
        writableMap.putDouble("timeStamp", timeStamp.toDouble())
        return writableMap
    }
}


open class InputDetector(private val context: Context, private val inputListener: InputListener) :
    GestureDetector.SimpleOnGestureListener() {

    private var isScrolling = false
    private var isLongPress = false
    private var scrollDirection: EventKeyType? = null
    private val gestureDetector = GestureDetectorCompat(context, this)
    private val eventResult = EventResult(
        type = Type.Touch,
        isShiftPressed = false,
        isLongPressed = false,
        isDown = false,
        keyCode = 0,
        timeStamp = 0,
        nativeKeyCode = null
    )

    interface InputListener {
        fun dispatchUserAction(writableMap: WritableMap?)
    }

    private fun dispatchEvent(
        type: Type,
        isShiftPressed: Boolean = false,
        isLongPressed: Boolean = false,
        isDown: Boolean = false,
        keyCode: Int = 0,
        timeStamp: Long = 0,
        nativeKeyCode: Int? = 0
    ) {
        eventResult.type = type
        eventResult.isDown = isDown
        eventResult.keyCode = keyCode
        eventResult.timeStamp = timeStamp
        eventResult.isLongPressed = isLongPressed
        eventResult.isShiftPressed = isShiftPressed
        eventResult.nativeKeyCode = nativeKeyCode
        val writableMap = eventResult.toWritableMap()
        inputListener.dispatchUserAction(writableMap)
    }

    fun onTouchEvent(event: MotionEvent?) {
        event?.let {
            if (isScrolling && it.action == ACTION_UP) {
                dispatchEvent(
                    Type.Touch,
                    isDown = false,
                    timeStamp = event.eventTime,
                    keyCode = this.scrollDirection?.value ?: EventKeyType.UNKNOWN.value
                )
                isScrolling = false
                scrollDirection = null
            }
            if (this.isLongPress && it.action == ACTION_UP) {
                dispatchEvent(
                    Type.Touch,
                    isDown = false,
                    isLongPressed = true,
                    timeStamp = event.eventTime,
                    keyCode = EventKeyType.SELECT.value
                )
                this.isLongPress = false
            }
            gestureDetector.onTouchEvent(it)
        }
    }

    override fun onDown(e: MotionEvent): Boolean {
        dispatchEvent(
            Type.Touch,
            isDown = true,
            timeStamp = e.eventTime,
            keyCode = EventKeyType.SELECT.value
        )
        return super.onDown(e)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        dispatchEvent(
            Type.Touch,
            isDown = (e.action == KeyEvent.ACTION_DOWN),
            keyCode = EventKeyType.SELECT.value,
            timeStamp = e.eventTime
        )
        return super.onSingleTapUp(e)
    }

    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)
        this.isLongPress = true

    }


    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        isScrolling = true
        scrollDirection = if (abs(distanceX) > abs(distanceY)) {
            if (distanceX > 0) EventKeyType.LEFT else EventKeyType.RIGHT
        } else {
            if (distanceY > 0) EventKeyType.UP else EventKeyType.DOWN
        }
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    fun onKeyEvent(event: KeyEvent?) {
        event?.let {
            val asciiVal = when {
                it.isPrintingKey -> it.displayLabel.lowercaseChar().code
                else -> mapKeyCodeToSpecialValue(it.keyCode)
            }
            dispatchEvent(
                Type.KeyPress,
                isDown = (it.action == KeyEvent.ACTION_DOWN),
                isLongPressed = it.isLongPress,
                isShiftPressed = it.isShiftPressed,
                keyCode = asciiVal,
                nativeKeyCode = it.keyCode,
                timeStamp = it.eventTime
            )
        }
    }

    /*Values from REMOTE CONTROL and non-ascii characters are mapped to special values from EventKeyType Enum*/
    private fun mapKeyCodeToSpecialValue(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> EventKeyType.UP.value
            KeyEvent.KEYCODE_DPAD_DOWN -> EventKeyType.DOWN.value
            KeyEvent.KEYCODE_DPAD_LEFT -> EventKeyType.LEFT.value
            KeyEvent.KEYCODE_DPAD_RIGHT -> EventKeyType.RIGHT.value
            KeyEvent.KEYCODE_DPAD_CENTER -> EventKeyType.SELECT.value
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> EventKeyType.PLAY_PAUSE.value
            KeyEvent.KEYCODE_MEDIA_PLAY -> EventKeyType.PLAY.value
            KeyEvent.KEYCODE_MEDIA_PAUSE -> EventKeyType.PAUSE.value
            KeyEvent.KEYCODE_MEDIA_STOP -> EventKeyType.STOP.value
            KeyEvent.KEYCODE_MEDIA_NEXT -> EventKeyType.NEXT.value
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> EventKeyType.PREVIOUS.value
            KeyEvent.KEYCODE_MEDIA_REWIND -> EventKeyType.REWIND.value
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> EventKeyType.FAST_FORWARD.value
            KeyEvent.KEYCODE_MEDIA_RECORD -> EventKeyType.RECORD.value
            KeyEvent.KEYCODE_MEDIA_CLOSE -> EventKeyType.CLOSE.value
            KeyEvent.KEYCODE_MENU -> EventKeyType.MENU.value
            KeyEvent.KEYCODE_BACK -> EventKeyType.BACK.value
            KeyEvent.KEYCODE_VOLUME_UP -> EventKeyType.VOLUME_UP.value
            KeyEvent.KEYCODE_VOLUME_DOWN -> EventKeyType.VOLUME_DOWN.value
            KeyEvent.KEYCODE_INFO -> EventKeyType.INFO.value
            KeyEvent.KEYCODE_CAPTIONS -> EventKeyType.CAPTIONS.value
            KeyEvent.KEYCODE_MUTE -> EventKeyType.MUTE.value
            KeyEvent.KEYCODE_SPACE -> EventKeyType.SPACE.value
            KeyEvent.KEYCODE_ENTER -> EventKeyType.ENTER.value
            KeyEvent.KEYCODE_DEL -> EventKeyType.DELETE.value
            KeyEvent.KEYCODE_ESCAPE -> EventKeyType.ESCAPE.value
            KeyEvent.KEYCODE_TAB -> EventKeyType.TAB.value
            else -> EventKeyType.UNKNOWN.value
        }
    }
}
