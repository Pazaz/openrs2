package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.ConditionalExpr
import dev.openrs2.deob.ast.util.countNots
import dev.openrs2.deob.ast.util.not
import dev.openrs2.deob.ast.util.walk

class TernaryTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk(Node.TreeTraversal.POSTORDER) { expr: ConditionalExpr ->
            val condition = expr.condition
            val notCondition = condition.not()
            if (notCondition.countNots() >= condition.countNots()) {
                return@walk
            }

            val thenExpr = expr.thenExpr
            val elseExpr = expr.elseExpr

            expr.condition = notCondition

            expr.thenExpr = elseExpr.clone()
            expr.elseExpr = thenExpr.clone()
        }
    }
}
