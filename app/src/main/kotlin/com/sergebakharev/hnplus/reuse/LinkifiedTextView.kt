package com.sergebakharev.hnplus.reuse

import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView

class LinkifiedTextView(context: Context?, attrs: AttributeSet?) :
    TextView(context, attrs) {
    /**
     * Inspired by http://stackoverflow.com/a/7327332/458603 - without this
     * custom code, clicks on links also trigger comment folding/unfolding.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val widget = this as TextView
        val text: Any = widget.text
        var linkHandled = false
        if (text is Spanned) {
            val buffer = text as Spannable

            val action = event.action

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                var x = event.x.toInt()
                var y = event.y.toInt()

                x -= widget.totalPaddingLeft
                y -= widget.totalPaddingTop

                x += widget.scrollX
                y += widget.scrollY

                val layout = widget.layout
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())

                val link = buffer.getSpans(
                    off, off,
                    ClickableSpan::class.java
                )

                if (link.size != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(widget)
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        Selection.setSelection(
                            buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(
                                link[0]
                            )
                        )
                    }
                    linkHandled = true
                    return true
                }
            }
        }
        return if (!linkHandled) {
            super.onTouchEvent(event)
        } else false
    }
}
