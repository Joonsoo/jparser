package com.giyeok.jparser.tests

import com.giyeok.jparser.Grammar
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Figure
import org.eclipse.draw2d.FigureCanvas
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Shell
import org.eclipse.draw2d.ToolbarLayout
import org.eclipse.draw2d.Label
import org.eclipse.jface.resource.JFaceResources
import com.giyeok.jparser.visualize.GrammarTextFigureGenerator
import com.giyeok.jparser.visualize.GrammarTextFigureGenerator
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.visualize.FigureGenerator
import com.giyeok.jparser.visualize.ParsingProcessVisualizer
import org.eclipse.swt.widgets.MessageBox
import com.giyeok.jparser.visualize.DerivationGraphVisualizer
import com.giyeok.jparser.Inputs.ConcreteInput
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormAttachment
import com.giyeok.jparser.NewParser
import com.giyeok.jparser.ParseResultGraphFunc
import com.giyeok.jparser.DerivationSliceFunc
import com.giyeok.jparser.visualize.editor.GrammarGrammar
import com.giyeok.jparser.ParseForestFunc
import org.eclipse.swt.events.SelectionListener
import org.eclipse.jface.dialogs.MessageDialog
import com.giyeok.jparser.ParsingErrors.UnexpectedInput

trait Editor {
    val parser = new NewParser(GrammarGrammar, ParseForestFunc, new DerivationSliceFunc(GrammarGrammar, ParseForestFunc))

    def start(): Unit = {
        val display = Display.getDefault()
        val shell = new Shell(display)
        val mono = JFaceResources.getFont(JFaceResources.TEXT_FONT)

        shell.setLayout(new FormLayout())

        val grammarText = new org.eclipse.swt.widgets.Text(shell, SWT.MULTI)
        val testText = new org.eclipse.swt.widgets.Text(shell, SWT.MULTI)
        val testButton = new org.eclipse.swt.widgets.Button(shell, SWT.NONE)

        val grammarTextLabel = new org.eclipse.swt.widgets.Label(shell, SWT.NONE)
        val testTextLabel = new org.eclipse.swt.widgets.Label(shell, SWT.NONE)

        grammarText.setLayoutData({
            val formData = new FormData()
            formData.top = new FormAttachment(0, 20)
            formData.bottom = new FormAttachment(100, 0)
            formData.left = new FormAttachment(0, 0)
            formData.right = new FormAttachment(50, 0)
            formData
        })
        grammarText.setBackground(ColorConstants.button)
        grammarText.setFont(mono)
        grammarText.setText("""S = `Stmt+
                              #Stmt = LetStmt
                              #     | ExprStmt
                              #LetStmt = Let ' ' Id ' ' Expr ';'
                              #Let = Let0&Name
                              #Let0 = 'l' 'e' 't'
                              #Name = L(`[a-z]+)
                              #Id = Name-Let
                              #ExprStmt = Expr ';' la(LetStmt)
                              #Token = '+' | Id
                              #Expr = `Token+
                              #`Stmt+ = `Stmt+ Stmt | Stmt
                              #`Token+ = `Token+ Token | Token
                              #`[a-z]+ = `[a-z]+ `[a-z] | `[a-z]
                              #`[a-z] = [a-z]""".stripMargin('#') + "\n")

        grammarTextLabel.setLayoutData({
            val formData = new FormData()
            formData.top = new FormAttachment(0, 0)
            formData.bottom = new FormAttachment(0, 20)
            formData.left = new FormAttachment(0, 0)
            formData.right = new FormAttachment(50, 0)
            formData
        })
        grammarTextLabel.setText("Grammar Definition")

        testText.setLayoutData({
            val formData = new FormData()
            formData.top = new FormAttachment(0, 20)
            formData.bottom = new FormAttachment(70, 0)
            formData.left = new FormAttachment(50, 0)
            formData.right = new FormAttachment(100, 0)
            formData
        })
        testText.setBackground(ColorConstants.lightGray)
        testText.setFont(mono)
        testText.setText("ab+cd;let wx yz;")

        testTextLabel.setLayoutData({
            val formData = new FormData()
            formData.top = new FormAttachment(0, 0)
            formData.bottom = new FormAttachment(0, 20)
            formData.left = new FormAttachment(50, 0)
            formData.right = new FormAttachment(100, 0)
            formData
        })
        testTextLabel.setText("Test Text")

        testButton.setLayoutData({
            val formData = new FormData()
            formData.top = new FormAttachment(70, 0)
            formData.bottom = new FormAttachment(100, 0)
            formData.left = new FormAttachment(50, 0)
            formData.right = new FormAttachment(100, 0)
            formData
        })
        testButton.setText("Test!")

        testButton.addListener(SWT.Selection, new Listener() {
            def handleEvent(e: Event) {
                e.`type` match {
                    case SWT.Selection =>
                        val grammarDef = grammarText.getText()
                        parser.parse(grammarDef) match {
                            case Left(parseResult) =>
                                parseResult.result match {
                                    case Some(forest) =>
                                        assert(forest.trees.size == 1)
                                        val tree = forest.trees.head
                                        val source = Inputs.fromString(testText.getText())
                                        GrammarGrammar.translate(tree) match {
                                            case Some(grammar) =>
                                                // TODO new parser로도 열 수 있게
                                                ParsingProcessVisualizer.startNaiveParser(grammar, source, display, new Shell(display))
                                            case None =>
                                                MessageDialog.openError(shell, "Error!", "Empty grammar!")
                                        }
                                    case None =>
                                        MessageDialog.openError(shell, "Error!", "Incomplete grammar definition!")
                                }
                            case Right(error) =>
                                val msg = error match {
                                    case UnexpectedInput(Inputs.Character(char: Char, location: Inputs.Location)) =>
                                        // grammarText.setText(grammarDef.substring(0, location) + "!" + grammarDef.substring(location))
                                        val lineNum = (grammarDef.substring(0, location) count { _ == '\n' }) + 1
                                        val lastLine = grammarDef.substring(0, location)
                                        val colNum = lastLine.length() - lastLine.lastIndexOf('\n')
                                        s"Unexpected input '$char' at Line $lineNum Column $colNum (Character at $location)"
                                    case _ => "" // cannot happen
                                }
                                MessageDialog.openError(shell, "Error!", msg)
                        }
                    case _ =>
                }
            }
        })

        try {
            shell.open()
            try {
                while (!shell.isDisposed()) {
                    if (!display.readAndDispatch()) {
                        display.sleep()
                    }
                }
            } finally {
                if (!shell.isDisposed()) {
                    shell.dispose()
                }
            }
        } finally {
            display.dispose()
        }
    }
}

object GrammarEditor extends Editor {
    def main(args: Array[String]): Unit = {
        GrammarEditor.start()
    }
}
