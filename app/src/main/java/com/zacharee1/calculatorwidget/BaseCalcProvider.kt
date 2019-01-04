package com.zacharee1.calculatorwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.text.Html
import android.text.TextUtils
import android.widget.RemoteViews
import android.widget.Toast
import java.text.DecimalFormat

/**
 * Implementation of App Widget functionality.
 */
abstract class BaseCalcProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_BUTTON_PRESSED = "com.zacharee1.calculatorwidget.action.BUTTON_PRESSED"
        const val EXTRA_BUTTON = "button"
        const val EXTRA_ID = "id"

        const val DIVIDE = '\u00F7'
        const val MULTIPLY = '\u00D7'
        const val SUBTRACT = '\u2212'
        const val ADD = '\u002B'
        const val DOT = '\u002E'
        const val DELETE = '\u007F'
        const val CLEAR = '\u239A'
        const val EQUALS = '\u003D'
        const val INPUT = '\u2402'

        var currentInputText = HashMap<Int, ArrayList<String>?>()
        var results = HashMap<Int, String>()

        fun isNotOperator(button: Char?) = button != DIVIDE && button != MULTIPLY && button != SUBTRACT && button != ADD
        fun isNotOperator(button: String?) = button != null && button.length > 1 || isNotOperator(button?.toCharArray()?.get(0))
        fun isOperator(button: Char?) = !isNotOperator(button)
        fun isOperator(button: String?) = !isNotOperator(button)

        fun canAddDot(id: Int): Boolean {
            var canAdd = true

            val string = TextUtils.join("", currentInputText[id]!!)
            if (string.contains(DOT)) {
                string.forEach {
                    if (it == DOT) canAdd = false
                    else if (!it.isDigit()) canAdd = true
                }
            }

            return canAdd
        }

        fun performOp(first: Double, last: Double, op: Char?): Double {
            when (op) {
                DIVIDE -> return first / last
                MULTIPLY -> return first * last
                SUBTRACT -> return first - last
                ADD -> return first + last
            }

            return Double.MIN_VALUE
        }
    }

    internal abstract val color: Int
    internal abstract val border: Int
    internal abstract val clazz: Class<out BaseCalcProvider>

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_BUTTON_PRESSED -> {
                if (intent.hasExtra(EXTRA_BUTTON) && intent.hasExtra(EXTRA_ID)) {
                    val button = intent.getCharExtra(EXTRA_BUTTON, Char.MAX_HIGH_SURROGATE)
                    val id = intent.getIntExtra(EXTRA_ID, -1)

                    when (button) {
                        EQUALS -> {
                            val numbers = ArrayList<String>()
                            val temp = ArrayList<String>()

                            currentInputText[id]?.forEach {
                                if (isNotOperator(it)) {
                                    temp.add(it)
                                } else {
                                    numbers.add(TextUtils.join("", temp))
                                    temp.clear()
                                    numbers.add(it)
                                }
                            }

                            if (temp.isNotEmpty()) numbers.add(TextUtils.join("", temp))

                            var result = Double.MIN_VALUE
                            var prevOp: Char? = null

                            numbers.forEach {
                                if (isNotOperator(it)) {
                                    if (result == Double.MIN_VALUE) result = it.toDouble()
                                    else if (prevOp != null) result = performOp(result, it.toDouble(), prevOp)
                                } else {
                                    prevOp = it.toCharArray()[0]
                                }
                            }

                            currentInputText[id]?.clear()

                            results[id] = result.toString()
                        }

                        DELETE -> {
                            val t = currentInputText[id] ?: return
                            if (t.size > 0) t.removeAt(t.lastIndex)
                        }

                        CLEAR -> {
                            currentInputText[id]?.clear()
                            results.remove(id)
                        }

                        INPUT -> {
                            val result = results[id] ?: return
                            val cbm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                            cbm.primaryClip = ClipData.newPlainText(context.resources.getString(R.string.app_name), result)
                            Toast.makeText(context, context.resources.getString(R.string.copied, result), Toast.LENGTH_SHORT).show()
                        }

                        else -> {
                            val text = currentInputText[id] ?: addToMapIfNeeded(id)

                            val last = if (text.size > 0) text[text.lastIndex] else null
                            val oldResult = results[id]

                            val canAddForResult = (isOperator(button) && oldResult != null && !oldResult.isBlank())
                            val canAdd =
                                    (!(isOperator(button) && text.size < 1)
                                            && !(isOperator(button) && isOperator(last))
                                            && if (button == DOT) canAddDot(id) else true)
                                            || canAddForResult
                            if (canAdd) {
                                if (canAddForResult) text.add(oldResult!!)
                                text.add(Character.toString(button))
                            }
                        }
                    }

                    update(context)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach {
            val current = addToMapIfNeeded(it)
            val views = RemoteViews(context.packageName, R.layout.calc_provider)

            views.setOnClickPendingIntent(R.id.one, makePendingIntent(context, '1', it))
            views.setOnClickPendingIntent(R.id.two, makePendingIntent(context, '2', it))
            views.setOnClickPendingIntent(R.id.three, makePendingIntent(context, '3', it))
            views.setOnClickPendingIntent(R.id.four, makePendingIntent(context, '4', it))
            views.setOnClickPendingIntent(R.id.five, makePendingIntent(context, '5', it))
            views.setOnClickPendingIntent(R.id.six, makePendingIntent(context, '6', it))
            views.setOnClickPendingIntent(R.id.seven, makePendingIntent(context, '7', it))
            views.setOnClickPendingIntent(R.id.eight, makePendingIntent(context, '8', it))
            views.setOnClickPendingIntent(R.id.nine, makePendingIntent(context, '9', it))
            views.setOnClickPendingIntent(R.id.zero, makePendingIntent(context, '0', it))

            views.setOnClickPendingIntent(R.id.divide, makePendingIntent(context, DIVIDE, it))
            views.setOnClickPendingIntent(R.id.delete, makePendingIntent(context, DELETE, it))
            views.setOnClickPendingIntent(R.id.times, makePendingIntent(context, MULTIPLY, it))
            views.setOnClickPendingIntent(R.id.clear, makePendingIntent(context, CLEAR, it))
            views.setOnClickPendingIntent(R.id.minus, makePendingIntent(context, SUBTRACT, it))
            views.setOnClickPendingIntent(R.id.dot, makePendingIntent(context, DOT, it))
            views.setOnClickPendingIntent(R.id.plus, makePendingIntent(context, ADD, it))
            views.setOnClickPendingIntent(R.id.equals, makePendingIntent(context, EQUALS, it))
            views.setOnClickPendingIntent(R.id.input_text, makePendingIntent(context, INPUT, it))
            
            val parsedColor = context.resources.getColor(color)

            views.setTextColor(R.id.one, parsedColor)
            views.setTextColor(R.id.two, parsedColor)
            views.setTextColor(R.id.three, parsedColor)
            views.setTextColor(R.id.four, parsedColor)
            views.setTextColor(R.id.five, parsedColor)
            views.setTextColor(R.id.six, parsedColor)
            views.setTextColor(R.id.seven, parsedColor)
            views.setTextColor(R.id.eight, parsedColor)
            views.setTextColor(R.id.nine, parsedColor)
            views.setTextColor(R.id.zero, parsedColor)

            views.setTextColor(R.id.divide, parsedColor)
            views.setTextColor(R.id.delete, parsedColor)
            views.setTextColor(R.id.times, parsedColor)
            views.setTextColor(R.id.clear, parsedColor)
            views.setTextColor(R.id.minus, parsedColor)
            views.setTextColor(R.id.dot, parsedColor)
            views.setTextColor(R.id.plus, parsedColor)
            views.setTextColor(R.id.equals, parsedColor)
            views.setTextColor(R.id.input_text, parsedColor)

            views.setInt(R.id.one, "setBackgroundResource", border)
            views.setInt(R.id.two, "setBackgroundResource", border)
            views.setInt(R.id.three, "setBackgroundResource", border)
            views.setInt(R.id.four, "setBackgroundResource", border)
            views.setInt(R.id.five, "setBackgroundResource", border)
            views.setInt(R.id.six, "setBackgroundResource", border)
            views.setInt(R.id.seven, "setBackgroundResource", border)
            views.setInt(R.id.eight, "setBackgroundResource", border)
            views.setInt(R.id.nine, "setBackgroundResource", border)
            views.setInt(R.id.zero, "setBackgroundResource", border)

            views.setInt(R.id.divide, "setBackgroundResource", border)
            views.setInt(R.id.delete, "setBackgroundResource", border)
            views.setInt(R.id.times, "setBackgroundResource", border)
            views.setInt(R.id.clear, "setBackgroundResource", border)
            views.setInt(R.id.minus, "setBackgroundResource", border)
            views.setInt(R.id.dot, "setBackgroundResource", border)
            views.setInt(R.id.plus, "setBackgroundResource", border)
            views.setInt(R.id.equals, "setBackgroundResource", border)
            views.setInt(R.id.input_text, "setBackgroundResource", border)

            val format = DecimalFormat("0.########")
            var text = TextUtils.join("", current)
            val isResult = text.isBlank() && results[it] != null

            if (isResult) text = results[it]

            val formatted = if (isResult) try {
                format.format(text.toDouble())
            } catch (e: Exception) {
                text
            } else text

            views.setTextViewText(R.id.input_text, Html.fromHtml(formatted))
            appWidgetManager.updateAppWidget(it, views)
        }
    }

    override fun onEnabled(context: Context) {}

    override fun onDisabled(context: Context) {}

    override fun onDeleted(context: Context?, appWidgetIds: IntArray) {
        appWidgetIds.forEach { currentInputText.remove(it) }
    }

    internal fun getComponent(context: Context) = ComponentName(context, clazz)

    private fun addToMapIfNeeded(id: Int): ArrayList<String> {
        if (!currentInputText.containsKey(id)) currentInputText[id] = ArrayList()
        return currentInputText[id]!!
    }

    private fun makeIntent(context: Context, button: Char, id: Int): Intent {
        return Intent(ACTION_BUTTON_PRESSED).apply {
            component = getComponent(context)
            `package` = component!!.packageName
            putExtra(EXTRA_BUTTON, button)
            putExtra(EXTRA_ID, id)
        }
    }

    private fun makePendingIntent(context: Context, button: Char, id: Int): PendingIntent {
        return PendingIntent.getBroadcast(context, button.hashCode() + id.hashCode() + Math.random().hashCode(), makeIntent(context, button, id), 0)
    }

    private fun update(context: Context) {
        val manager = AppWidgetManager.getInstance(context)

        val ids = manager.getAppWidgetIds(getComponent(context))
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.`package` = context.packageName
        intent.component = getComponent(context)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)

        context.sendBroadcast(intent)
    }
}
