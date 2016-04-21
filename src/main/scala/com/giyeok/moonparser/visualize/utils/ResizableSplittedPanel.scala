package com.giyeok.moonparser.visualize.utils

import com.giyeok.moonparser.Grammar
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.SWT
import com.giyeok.moonparser.Inputs.Input
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Sash
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Composite

class HorizontalResizableSplittedComposite(parent: Composite, style: Int, initialPercent: Int = 50) extends Composite(parent, style) {
    private var upper = new Composite(this, SWT.NONE)
    private val sash = new Sash(this, SWT.BORDER | SWT.HORIZONTAL)
    private var lower = new Composite(this, SWT.NONE)

    upper.setLayout(new FillLayout())
    lower.setLayout(new FillLayout())

    val form = new FormLayout()

    upperPanel.setLayoutData({
        val d = new FormData()
        d.left = new FormAttachment(0, 0)
        d.right = new FormAttachment(100, 0)
        d.top = new FormAttachment(0, 0)
        d.bottom = new FormAttachment(sash, 0)
        d
    })

    val limit = 20 // pixels
    val sashData = new FormData()
    sashData.left = new FormAttachment(0, 0)
    sashData.right = new FormAttachment(100, 0)
    sashData.top = new FormAttachment(initialPercent, 0)
    sash.setLayoutData(sashData)
    sash.addListener(SWT.Selection, new Listener() {
        def handleEvent(e: Event): Unit = {
            val sashRect = sash.getBounds()
            val shellRect = getClientArea()
            val bottom = shellRect.height - sashRect.height - limit
            val ey = math.max(math.min(e.y, bottom), limit)
            if (ey != sashRect.y) {
                sashData.top = new FormAttachment(0, ey)
                layout()
            }
        }
    })

    lowerPanel.setLayoutData({
        val d = new FormData()
        d.left = new FormAttachment(0, 0)
        d.right = new FormAttachment(100, 0)
        d.top = new FormAttachment(sash, 0)
        d.bottom = new FormAttachment(100, 0)
        d
    })

    setLayout(form)

    def upperPanel = upper
    def lowerPanel = lower
}

class VerticalResizableSplittedComposite(parent: Composite, style: Int, initialPercent: Int = 50) extends Composite(parent, style) {
    private var left = new Composite(this, SWT.NONE)
    private val sash = new Sash(this, SWT.BORDER | SWT.VERTICAL)
    private var right = new Composite(this, SWT.NONE)

    left.setLayout(new FillLayout())
    right.setLayout(new FillLayout())

    val form = new FormLayout()

    leftPanel.setLayoutData({
        val d = new FormData()
        d.left = new FormAttachment(0, 0)
        d.right = new FormAttachment(sash, 0)
        d.top = new FormAttachment(0, 0)
        d.bottom = new FormAttachment(100, 0)
        d
    })

    val limit = 20 // pixels
    val sashData = new FormData()
    sashData.left = new FormAttachment(initialPercent, 0)
    sashData.top = new FormAttachment(0, 0)
    sashData.bottom = new FormAttachment(100, 0)
    sash.setLayoutData(sashData)
    sash.addListener(SWT.Selection, new Listener() {
        def handleEvent(e: Event): Unit = {
            val sashRect = sash.getBounds()
            val shellRect = getClientArea()
            val right = shellRect.width - sashRect.width - limit
            val ex = math.max(math.min(e.x, right), limit)
            if (ex != sashRect.x) {
                sashData.left = new FormAttachment(0, ex)
                layout()
            }
        }
    })

    rightPanel.setLayoutData({
        val d = new FormData()
        d.left = new FormAttachment(sash, 0)
        d.right = new FormAttachment(100, 0)
        d.top = new FormAttachment(0, 0)
        d.bottom = new FormAttachment(100, 0)
        d
    })

    setLayout(form)

    def leftPanel = left
    def rightPanel = right
}
