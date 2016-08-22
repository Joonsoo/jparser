package com.giyeok.jparser.studio

import org.eclipse.draw2d.ColorConstants
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Shell
import org.eclipse.jface.resource.JFaceResources
import com.giyeok.jparser.Inputs
import com.giyeok.jparser.visualize.ParsingProcessVisualizer
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormAttachment
import com.giyeok.jparser.ParseForestFunc
import org.eclipse.jface.dialogs.MessageDialog
import com.giyeok.jparser.ParsingErrors.UnexpectedInput
import com.giyeok.jparser.nparser.NaiveParser
import com.giyeok.jparser.nparser.NGrammar
import com.giyeok.jparser.nparser.ParseTreeConstructor
import com.giyeok.jparser.visualize.ZestParsingContextWidget
import com.giyeok.jparser.nparser.Parser.NaiveWrappedContext
import com.giyeok.jparser.gramgram.GrammarGrammar

trait Editor {
    val parser = new NaiveParser(NGrammar.fromGrammar(GrammarGrammar))

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
                            case Left(wctx) =>
                                new ParseTreeConstructor(ParseForestFunc)(parser.grammar)(wctx.inputs, wctx.history, wctx.conditionFate).reconstruct() match {
                                    case Some(forest) =>
                                        assert(forest.trees.size == 1)
                                        val tree = forest.trees.head
                                        val source = Inputs.fromString(testText.getText())
                                        GrammarGrammar.translate(tree) match {
                                            case Some(grammar) =>
                                                val ngrammar = NGrammar.fromGrammar(grammar)
                                                // TODO 다른 파서로도 열 수 있게
                                                val parser = new NaiveParser(ngrammar)
                                                ParsingProcessVisualizer.start[NaiveWrappedContext](grammar.name, parser, source, display, new Shell(display), new ZestParsingContextWidget(_, _, _, _, _))
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
