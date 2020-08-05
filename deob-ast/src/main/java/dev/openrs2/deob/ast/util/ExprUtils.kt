package dev.openrs2.deob.ast.util

import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.UnaryExpr

fun IntegerLiteralExpr.checkedAsInt(): Int {
    val n = asNumber()
    if (n !is Int) {
        error("Invalid IntegerLiteralExpr type")
    }
    return n
}

fun Int.toHexLiteralExpr(): IntegerLiteralExpr {
    return IntegerLiteralExpr("0x${Integer.toUnsignedString(this, 16).toUpperCase()}")
}

fun LongLiteralExpr.checkedAsLong(): Long {
    val n = asNumber()
    if (n !is Long) {
        error("Invalid LongLiteralExpr type")
    }
    return n
}

fun Long.toHexLiteralExpr(): LongLiteralExpr {
    return LongLiteralExpr("0x${java.lang.Long.toUnsignedString(this, 16).toUpperCase()}L")
}

fun Expression.isIntegerOrLongLiteral(): Boolean {
    return isIntegerLiteralExpr || isLongLiteralExpr
}

fun Long.toLongLiteralExpr(): LongLiteralExpr {
    return LongLiteralExpr(this.toString() + "L")
}

fun Expression.negate(): Expression {
    return when (this) {
        is UnaryExpr -> when (operator) {
            UnaryExpr.Operator.PLUS -> UnaryExpr(expression.clone(), UnaryExpr.Operator.MINUS)
            UnaryExpr.Operator.MINUS -> expression.clone()
            else -> UnaryExpr(clone(), UnaryExpr.Operator.MINUS)
        }
        is IntegerLiteralExpr -> when (val n = asNumber()) {
            IntegerLiteralExpr.MAX_31_BIT_UNSIGNED_VALUE_AS_LONG -> IntegerLiteralExpr(Integer.MIN_VALUE.toString())
            is Int -> IntegerLiteralExpr((-n.toInt()).toString())
            else -> error("Invalid IntegerLiteralExpr type")
        }
        is LongLiteralExpr -> when (val n = asNumber()) {
            LongLiteralExpr.MAX_63_BIT_UNSIGNED_VALUE_AS_BIG_INTEGER -> Long.MIN_VALUE.toLongLiteralExpr()
            is Long -> (-n).toLongLiteralExpr()
            else -> error("Invalid LongLiteralExpr type")
        }
        else -> UnaryExpr(clone(), UnaryExpr.Operator.MINUS)
    }
}

fun Expression.not(): Expression {
    if (isUnaryExpr) {
        val unary = asUnaryExpr()
        if (unary.operator == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
            return unary.expression.clone()
        }
    } else if (isBinaryExpr) {
        val binary = asBinaryExpr()

        val left = binary.left
        val right = binary.right

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (binary.operator) {
            BinaryExpr.Operator.EQUALS ->
                return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.NOT_EQUALS)
            BinaryExpr.Operator.NOT_EQUALS ->
                return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.EQUALS)
            BinaryExpr.Operator.GREATER ->
                return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.LESS_EQUALS)
            BinaryExpr.Operator.GREATER_EQUALS ->
                return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.LESS)
            BinaryExpr.Operator.LESS ->
                return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.GREATER_EQUALS)
            BinaryExpr.Operator.LESS_EQUALS ->
                return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.GREATER)
            BinaryExpr.Operator.AND ->
                return BinaryExpr(left.not(), right.not(), BinaryExpr.Operator.OR)
            BinaryExpr.Operator.OR ->
                return BinaryExpr(left.not(), right.not(), BinaryExpr.Operator.AND)
        }
    } else if (isBooleanLiteralExpr) {
        return BooleanLiteralExpr(!asBooleanLiteralExpr().value)
    }
    return UnaryExpr(clone(), UnaryExpr.Operator.LOGICAL_COMPLEMENT)
}

fun Expression.countNots(): Int {
    var count = 0

    if (isUnaryExpr && asUnaryExpr().operator == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
        count++
    } else if (isBinaryExpr && asBinaryExpr().operator == BinaryExpr.Operator.NOT_EQUALS) {
        count++
    }

    for (child in findAll(Expression::class.java)) {
        if (child !== this) {
            count += child.countNots()
        }
    }

    return count
}

fun Expression.hasSideEffects(): Boolean {
    if (isLiteralExpr || isNameExpr || isThisExpr) {
        return false
    } else if (isUnaryExpr) {
        return asUnaryExpr().expression.hasSideEffects()
    } else if (isBinaryExpr) {
        val binary = asBinaryExpr()
        return binary.left.hasSideEffects() || binary.right.hasSideEffects()
    } else if (isArrayAccessExpr) {
        val access = asArrayAccessExpr()
        return access.name.hasSideEffects() || access.index.hasSideEffects()
    } else if (isFieldAccessExpr) {
        val access = asFieldAccessExpr()
        return access.scope.hasSideEffects()
    }

    // TODO(gpe): more cases
    return true
}

fun BinaryExpr.Operator.flip(): BinaryExpr.Operator? {
    return when (this) {
        BinaryExpr.Operator.PLUS, BinaryExpr.Operator.MULTIPLY -> this
        BinaryExpr.Operator.EQUALS, BinaryExpr.Operator.NOT_EQUALS -> this
        BinaryExpr.Operator.BINARY_AND, BinaryExpr.Operator.BINARY_OR -> this
        BinaryExpr.Operator.XOR, BinaryExpr.Operator.OR, BinaryExpr.Operator.AND -> this
        BinaryExpr.Operator.GREATER -> BinaryExpr.Operator.LESS
        BinaryExpr.Operator.GREATER_EQUALS -> BinaryExpr.Operator.LESS_EQUALS
        BinaryExpr.Operator.LESS -> BinaryExpr.Operator.GREATER
        BinaryExpr.Operator.LESS_EQUALS -> BinaryExpr.Operator.GREATER_EQUALS
        else -> null
    }
}
