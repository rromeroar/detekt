package io.gitlab.arturbosch.detekt.rules.style

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.DetektVisitor
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

class DataClassContainsFunctionsRule(config: Config = Config.empty) : Rule(config) {
	override val issue: Issue = Issue("DataClassContainsFunctions",
			Severity.Style,
			"Data classes should mainly be used to store data and should not have any extra functions." +
					"(Compiler will automatically generate equals, toString and hashCode functions)")

	private val visitor = FunctionsVisitor(this)

	override fun visitClass(klass: KtClass) {
		if (klass.isData()) {
			klass.getBody()?.declarations?.forEach {
				it.accept(visitor)
			}
		}
		super.visitClass(klass)
	}

	private fun handleNamedFunction(function: KtNamedFunction) {
		if (!isOverridden(function) && !isConversionFunction(function)) {
			report(CodeSmell(issue, Entity.from(function)))
		}
	}

	private fun isOverridden(function: KtNamedFunction): Boolean {
		return function.modifierList?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false
	}

	private fun isConversionFunction(function: KtNamedFunction): Boolean {
		if (shouldCheckConversionFunction()) {
			return containsConversionPrefix(function)
		}
		return false
	}

	private fun shouldCheckConversionFunction(): Boolean {
		return valueOrDefault(CONVERSION_FUNCTION_PREFIX, "").isNotBlank()
	}

	private fun containsConversionPrefix(function: KtNamedFunction): Boolean {
		val prefixes = valueOrDefault(CONVERSION_FUNCTION_PREFIX, CONVERSION_FUNCTION_DEFAULT_PREFIX)
		return prefixes.split(',').count { function.name?.startsWith(it) ?: false } != 0
	}

	private class FunctionsVisitor(val rule: DataClassContainsFunctionsRule) : DetektVisitor() {
		override fun visitNamedFunction(function: KtNamedFunction) {
			rule.handleNamedFunction(function)
		}
	}

	companion object {
		const val CONVERSION_FUNCTION_PREFIX = "conversionFunctionPrefix"
		private const val CONVERSION_FUNCTION_DEFAULT_PREFIX = "to"
	}
}
