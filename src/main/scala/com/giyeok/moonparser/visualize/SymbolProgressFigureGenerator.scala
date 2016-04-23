package com.giyeok.moonparser.visualize

import org.eclipse.draw2d.Figure
import com.giyeok.moonparser.Parser
import com.giyeok.moonparser.visualize.FigureGenerator.Spacing
import com.giyeok.moonparser.Kernels._

class SymbolProgressFigureGenerator[Fig](g: FigureGenerator.Generator[Fig], ap: FigureGenerator.Appearances[Fig]) {
    val symbolFigureGenerator = new SymbolFigureGenerator(g, ap)

    def kernelFig(k: Kernel): Fig = {
        def dot = g.textFig("\u2022", ap.kernelDot)
        def withDot[T](front: Boolean, last: Boolean, fig: Fig): Seq[Fig] =
            (if (front) Seq(dot) else Seq()) ++ Seq(fig) ++ (if (last) Seq(dot) else Seq())
        k match {
            case EmptyKernel => g.horizontalFig(Spacing.Small, Seq(g.textFig("\u03B5", ap.default), dot))
            case k: AtomicKernel[_] =>
                g.horizontalFig(Spacing.Small, withDot(k.derivable, k.finishable, symbolFigureGenerator.symbolFig(k.symbol)))
            case k: RepeatKernel[_] =>
                g.horizontalFig(Spacing.Small, withDot(k.derivable, k.finishable, symbolFigureGenerator.symbolFig(k.symbol)))
            case k: SequenceKernel =>
                val (past, future) = k.symbol.seq splitAt k.pointer
                g.horizontalFig(Spacing.Small, ((past map { symbolFigureGenerator.symbolFig _ }) :+ dot) ++ (future map { symbolFigureGenerator.symbolFig _ }))
        }
    }

    def symbolProgFig(n: Parser#SymbolProgress) = {
        def dot = g.textFig("\u2022", ap.kernelDot)
        def withDot[T](front: Boolean, last: Boolean, fig: Fig): Seq[Fig] =
            (if (front) Seq(dot) else Seq()) ++ Seq(fig) ++ (if (last) Seq(dot) else Seq())
        g.horizontalFig(Spacing.Small, Seq(
            kernelFig(n.kernel),
            g.textFig(",", ap.default),
            g.textFig((n.derivedGen, n.lastLiftedGen) match { case (start, Some(end)) => s"$start-$end" case (start, None) => s"$start" }, ap.default)))
    }
}
