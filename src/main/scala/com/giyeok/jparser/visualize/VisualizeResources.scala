package com.giyeok.jparser.visualize

import org.eclipse.swt.graphics.Font
import org.eclipse.jface.resource.JFaceResources
import org.eclipse.swt.SWT

trait VisualizeResources {
    val default12Font: Font
    val fixedWidth12Font: Font
    val italic14Font: Font
    val bold14Font: Font
    val smallFont: Font
}

object BasicVisualizeResources extends VisualizeResources {
    val defaultFontName = JFaceResources.getTextFont.getFontData.head.getName
    val default12Font = new Font(null, defaultFontName, 12, SWT.NONE)
    val fixedWidth12Font = new Font(null, defaultFontName, 12, SWT.NONE)
    val italic14Font = new Font(null, defaultFontName, 14, SWT.ITALIC)
    val bold14Font = new Font(null, defaultFontName, 14, SWT.BOLD)
    val smallFont = new Font(null, defaultFontName, 8, SWT.NONE)
}
