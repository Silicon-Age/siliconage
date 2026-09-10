package com.opal.creator.database;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/* This currently only works for CHECK constraints on individual columns (but they can either have been declared as column
 * or table constraints).
 */

/* How much of this is SQL Server specific?  None of it? */
public class CheckConstraintParser {

	private CheckConstraintParser() {
		throw new AssertionError();
	}

	/* Ultimately, this can probably just take a DatabaseColumn */
	public static ParseResult parseColumnCheck(Class<?> argJavaType, boolean argNullable, String argColumnName, String argDefinition) {
		Objects.requireNonNull(argColumnName);
		Objects.requireNonNull(argDefinition);

		final JavaType<?> lclType = JavaType.forClass(argJavaType);

		if (lclType == null) {
			return new Unsupported(argDefinition, "Unsupported SQL/Java type " + argJavaType.getName());
		}

		if (argNullable && argJavaType.isPrimitive()) {
			return new Unsupported(argDefinition, "A nullable SQL column cannot be represented by primitive " + argJavaType.getName());
		}

		try {
			final Parser lclParser = new Parser(argDefinition, argColumnName, lclType, argNullable);

			final CheckExpression lclExpression = lclParser.parse();

			return new Supported(new ParsedCheck(argJavaType, argNullable, argColumnName, argDefinition, lclExpression));
		} catch (UnsupportedCheckException lclE) {
			return new Unsupported(argDefinition, lclE.getMessage());
		}
	}

	public static String generateStaticValidationMethod(String argMethodName, String argParameterName, int indentationLevel, ParsedCheck argCheck) {
		Objects.requireNonNull(argMethodName);
		Objects.requireNonNull(argParameterName);
		if (indentationLevel < 0) {
			throw new IllegalArgumentException("identationLevel must be non-negative.");
		}
		Objects.requireNonNull(argCheck);

		final JavaEmitter lclEmitter = new JavaEmitter(argParameterName);

		final String lclResultVariable = lclEmitter.emit(argCheck.expression());

		String indent = StringUtils.repeat('\t', indentationLevel);
		String indentBody = StringUtils.repeat('\t', indentationLevel + 1);
		
		final StringBuilder lclSB = new StringBuilder(1024);

		lclSB.append(indent)
			.append("/* package */ static boolean ")
			.append(argMethodName)
			.append("(")
			.append(sourceName(argCheck.javaType()))
			.append(' ')
			.append(argParameterName)
			.append(") {\n");

		for (String lclLine : lclEmitter.lines()) {
			lclSB.append(indentBody).append(lclLine).append('\n');
		}

		/*
		 * CHECK constraints reject FALSE.  TRUE and UNKNOWN both satisfy the constraint.
		 */
		lclSB.append(indentBody)
			.append("return ")
			.append(lclResultVariable)
			.append(" != Trinary.FALSE;\n");

		lclSB.append(indent)
			.append("}\n");

		return lclSB.toString();
	}

	/* A ParseResult is either a Supported (containing the parsed expression tree) or an Unsupported.  Unsupported ones could be real
	 * errors (like syntactically broken SQL from the database), but they are probably just expressions that involve functions
	 * like UPPER() or LEN() that we don't currently support.
	 */
	public sealed interface ParseResult permits Supported, Unsupported {}
	public record Supported(ParsedCheck check) implements ParseResult {}
	public record Unsupported(String definition, String reason) implements ParseResult {}

	public record ParsedCheck(Class<?> javaType, boolean nullable, String columnName, String definition, CheckExpression expression) {}

	public sealed interface CheckExpression permits And, Or, Not, Comparison, IsNull, Between, In {}

	public record And(CheckExpression left, CheckExpression right) implements CheckExpression {}

	public record Or(CheckExpression left, CheckExpression right) implements CheckExpression {}

	public record Not(CheckExpression operand) implements CheckExpression {}

	public enum ComparisonOperator {
		EQ,
		NE,
		LT,
		LE,
		GT,
		GE
	}

	public record Comparison<T>(ValueExpression<T> left, ComparisonOperator operator, ValueExpression<T> right) implements CheckExpression {}

	public record IsNull<T>(ValueExpression<T> operand, boolean negated) implements CheckExpression {}

	public record Between<T>(ValueExpression<T> operand, ValueExpression<T> lower, ValueExpression<T> upper, boolean negated) implements CheckExpression {}

	public record In<T>(ValueExpression<T> operand, List<ValueExpression<T>> alternatives, boolean negated) implements CheckExpression {
		public In {
			alternatives = List.copyOf(alternatives);
		}
	}

	public sealed interface ValueExpression<T> permits Column, Literal {
		JavaType<T> type();
	}

	public record Column<T>(String name, JavaType<T> type, boolean nullability) implements ValueExpression<T> {}

	public record Literal<T>(T value, JavaType<T> type) implements ValueExpression<T> {}

	public enum JavaKind {
		INTEGRAL,
		FLOATING_POINT,
		BIG_DECIMAL,
		STRING,
		LOCAL_DATE,
		LOCAL_DATE_TIME,
		BOOLEAN
	}

	public record JavaType<T>(Class<T> javaClass, JavaKind kind) {
		
		static JavaType<?> forClass(Class<?> argJavaType) {
		    if (argJavaType == byte.class || argJavaType == Byte.class
		            || argJavaType == short.class || argJavaType == Short.class
		            || argJavaType == int.class || argJavaType == Integer.class
		            || argJavaType == long.class || argJavaType == Long.class) {
		        return new JavaType<>(argJavaType, JavaKind.INTEGRAL);
		    }

		    if (argJavaType == float.class || argJavaType == Float.class
		            || argJavaType == double.class || argJavaType == Double.class) {
		        return new JavaType<>(argJavaType, JavaKind.FLOATING_POINT);
		    }

		    if (argJavaType == BigDecimal.class) {
		        return new JavaType<>(BigDecimal.class, JavaKind.BIG_DECIMAL);
		    }

		    if (argJavaType == String.class) {
		        return new JavaType<>(String.class, JavaKind.STRING);
		    }

		    if (argJavaType == LocalDate.class) {
		        return new JavaType<>(LocalDate.class, JavaKind.LOCAL_DATE);
		    }

		    if (argJavaType == LocalDateTime.class) {
		        return new JavaType<>(LocalDateTime.class, JavaKind.LOCAL_DATE_TIME);
		    }

		    if (argJavaType == boolean.class || argJavaType == Boolean.class) {
		        return new JavaType<>(argJavaType, JavaKind.BOOLEAN);
		    }

		    return null;
		}
	}

	private static final class JavaEmitter {
		private final String myParameterName;
		private final List<String> myLines = new ArrayList<>();
		private int myNextTemporary;

		private JavaEmitter(String argParameterName) {
			myParameterName = Objects.requireNonNull(argParameterName);
		}

		private List<String> lines() {
			return List.copyOf(myLines);
		}

		private String emit(CheckExpression argExpression) {
			Objects.requireNonNull(argExpression);

			return switch (argExpression) {
				case And lclAnd -> emitAnd(lclAnd);
				case Or lclOr -> emitOr(lclOr);
				case Not lclNot -> emitNot(lclNot);
				case Comparison<?> lclComparison -> emitComparison(lclComparison);
				case IsNull<?> lclIsNull -> emitIsNull(lclIsNull);
				case Between<?> lclBetween -> emitBetween(lclBetween);
				case In<?> lclIn -> emitIn(lclIn);
			};
		}

		private String emitAnd(And argAnd) {
			final String lclLeft = emit(argAnd.left());
			final String lclRight = emit(argAnd.right());

			final String lclResult = nextTemporary();

			myLines.add(
				"Trinary "
					+ lclResult
					+ " = "
					+ lclLeft
					+ ".and("
					+ lclRight
					+ ");"
			);

			return lclResult;
		}

		private String emitOr(Or argOr) {
			final String lclLeft = emit(argOr.left());
			final String lclRight = emit(argOr.right());

			final String lclResult = nextTemporary();

			myLines.add(
				"Trinary "
					+ lclResult
					+ " = "
					+ lclLeft
					+ ".or("
					+ lclRight
					+ ");"
			);

			return lclResult;
		}

		private String emitNot(Not argNot) {
			final String lclOperand = emit(argNot.operand());

			final String lclResult = nextTemporary();

			myLines.add(
				"Trinary "
					+ lclResult
					+ " = "
					+ lclOperand
					+ ".not();"
			);

			return lclResult;
		}

		private String emitComparison(Comparison<?> argComparison) {
			return emitComparisonCaptured(argComparison);
		}

		private <T> String emitComparisonCaptured(final Comparison<T> argComparison) {
			ValueExpression<T> lclLeftExpression = argComparison.left();

			ValueExpression<T> lclRightExpression = argComparison.right();

			 // These are the expressions as they appear in Java before assuming that they are non-null.
			String lclLeft = emitValue(lclLeftExpression);
			String lclRight = emitValue(lclRightExpression);

			/*
			 * Construct the SQL UNKNOWN guard one operand at a time.
			 *
			 * We can't simply do left == null || right == null because one might be a literal like 0.0d.
			 * that can't be compared to null.
			 */
			final List<String> lclNullChecks = new ArrayList<>(2);

			if (canBeNull(lclLeftExpression)) {
				lclNullChecks.add(lclLeft + " == null");
			}

			if (canBeNull(lclRightExpression)) {
				lclNullChecks.add(lclRight + " == null");
			}

			/*
			 * Unboxing is now fine.
			 */
			final String lclNonNullLeft =
				renderNonNullValue(
					lclLeftExpression,
					lclLeft
				);

			final String lclNonNullRight =
				renderNonNullValue(
					lclRightExpression,
					lclRight
				);

			final String lclComparison =
				renderComparison(
					lclNonNullLeft,
					argComparison.operator(),
					lclNonNullRight,
					lclLeftExpression.type()
				);

			final String lclResult = nextTemporary();

			if (lclNullChecks.isEmpty()) {
				myLines.add(
					"Trinary "
						+ lclResult
						+ " = Trinary.of("
						+ lclComparison
						+ ");"
				);
			} else {
				myLines.add(
					"Trinary "
						+ lclResult
						+ ";"
				);

				myLines.add(
					"if ("
						+ String.join(" || ", lclNullChecks)
						+ ") {"
				);

				myLines.add(
					"\t"
						+ lclResult
						+ " = Trinary.UNKNOWN;"
				);

				myLines.add("} else {");

				myLines.add(
					"\t"
						+ lclResult
						+ " = Trinary.of("
						+ lclComparison
						+ ");"
				);

				myLines.add("}");
			}

			return lclResult;
		}

		private String emitIsNull(IsNull<?> argIsNull) {
			return emitIsNullCaptured(argIsNull);
		}

		private <T> String emitIsNullCaptured(IsNull<T> argIsNull) {
			final ValueExpression<T> lclOperandExpression = argIsNull.operand();

			final String lclResult = nextTemporary();
			
			if (canBeNull(lclOperandExpression)) {			
				String lclOperand = emitValue(lclOperandExpression);
	
				/*
				 * IS NULL and IS NOT NULL never produce UNKNOWN in SQL.
				 */
				String lclCondition =
					argIsNull.negated()
						? lclOperand + " != null"
						: lclOperand + " == null";
	
				myLines.add(
					"Trinary "
						+ lclResult
						+ " = Trinary.of("
						+ lclCondition
						+ ");"
				);
	
			} else {
				myLines.add("Trinary " + lclResult + " = Trinary.of(" + (argIsNull.negated() == false) + ");");
			}
			return lclResult;
		}

		private String emitBetween(Between<?> argBetween) {
			return emitBetweenCaptured(argBetween);
		}

		private <T> String emitBetweenCaptured(Between<T> argBetween) {
			CheckExpression lclExpanded =
				new And(
					new Comparison<>(
						argBetween.operand(),
						ComparisonOperator.GE,
						argBetween.lower()
					),
					new Comparison<>(
						argBetween.operand(),
						ComparisonOperator.LE,
						argBetween.upper()
					)
				);

			if (argBetween.negated()) {
				return emit(new Not(lclExpanded));
			} else {
				return emit(lclExpanded);
			}
		}

		private String emitIn(In<?> argIn) {
			return emitInCaptured(argIn);
		}

		private <T> String emitInCaptured(In<T> argIn) {
			if (argIn.alternatives().isEmpty()) {
				throw new IllegalStateException("An IN expression must contain at least one alternative.");
			}

			CheckExpression lclExpression = null;

			for (ValueExpression<T> lclAlternative : argIn.alternatives()) {
				CheckExpression lclEquality =new Comparison<>(argIn.operand(), ComparisonOperator.EQ, lclAlternative);

				if (lclExpression == null) {
					lclExpression = lclEquality;
				} else {
					lclExpression = new Or(lclExpression, lclEquality);
				}
			}

			assert lclExpression != null;

			if (argIn.negated()) {
				return emit(new Not(lclExpression));
			} else {
				return emit(lclExpression);
			}
		}

		private <T> String emitValue(ValueExpression<T> argExpression) {
			return switch (argExpression) {
				case Column<T> _ -> myParameterName;
				case Literal<T> lclLiteral -> renderLiteral(lclLiteral);
			};
		}

		private String nextTemporary() {
			return "lclCheck" + myNextTemporary++;
		}
	}

	/* This should probably become a recursive notion that can be applied to any kind of expression. */
	private static boolean canBeNull(ValueExpression<?> argExpression) {
		return switch (argExpression) {
			case Column<?> lclColumn -> lclColumn.nullability();
			case Literal<?> lclLiteral -> lclLiteral.value() == null;
		};
	}

	/*
	 * Render an operand when execution has already established that it isn't null.
	 */
	private static String renderNonNullValue(ValueExpression<?> argExpression, String argRenderedValue) {
		if (argExpression instanceof Literal<?>) {
			return argRenderedValue;
		}

		final Class<?> lclClass = argExpression.type().javaClass();

		Class<?> lclPrimitive = ClassUtils.wrapperToPrimitive(lclClass);
		if (lclPrimitive != null) {
			return argRenderedValue + "." + lclPrimitive.getName() + "Value()"; // "int" -> "intValue"
		}
		
		return argRenderedValue;
	}
	
	/* Types of token that the parser recognizes. */
	private enum TokenType {
		IDENTIFIER,
		NUMBER,
		STRING,
		NULL,

		AND,
		OR,
		NOT,
		IS,
		BETWEEN,
		IN,

		EQ,
		NE,
		LT,
		LE,
		GT,
		GE,

		LPAREN,
		RPAREN,
		COMMA,

		END
	}

	private record Token(TokenType type, String text) {}

	private static final class Lexer {
		private final String myInput;
		private int myPosition;

		private Lexer(String argInput) {
			myInput = argInput;
		}

		private Token next() {
			skipWhitespace();

			if (myPosition >= myInput.length()) {
				return new Token(TokenType.END, "");
			}

			final char lclC = myInput.charAt(myPosition);

			switch (lclC) {
				case '(' -> {
					++myPosition;
					return new Token(TokenType.LPAREN, "(");
				}

				case ')' -> {
					++myPosition;
					return new Token(TokenType.RPAREN, ")");
				}

				case ',' -> {
					++myPosition;
					return new Token(TokenType.COMMA, ",");
				}

				case '=' -> {
					++myPosition;
					return new Token(TokenType.EQ, "=");
				}

				case '<' -> {
					if (peek('=')) {
						myPosition += 2;
						return new Token(TokenType.LE, "<=");
					}

					if (peek('>')) {
						myPosition += 2;
						return new Token(TokenType.NE, "<>");
					}

					++myPosition;
					return new Token(TokenType.LT, "<");
				}

				case '>' -> {
					if (peek('=')) {
						myPosition += 2;
						return new Token(TokenType.GE, ">=");
					}

					++myPosition;
					return new Token(TokenType.GT, ">");
				}

				case '\'' -> {
					return readString();
				}

				case '[' -> {
					return readBracketedIdentifier();
				}

				default -> {
					// Continue below.
				}
			}

			/*
			 * N'Unicode string' literals.
			 */
			if ((lclC == 'N' || lclC == 'n')
				&& myPosition + 1 < myInput.length()
				&& myInput.charAt(myPosition + 1) == '\'') {
				++myPosition;
				return readString();
			}

			if (Character.isDigit(lclC)
				|| (lclC == '-'
					&& myPosition + 1 < myInput.length()
					&& Character.isDigit(myInput.charAt(myPosition + 1)))) {
				return readNumber();
			}

			if (Character.isJavaIdentifierStart(lclC)) {
				return readWord();
			}

			throw unsupported("Unexpected character '" + lclC + "' at position " + myPosition);
		}

		private Token readBracketedIdentifier() {
			++myPosition;

			final StringBuilder lclSB = new StringBuilder();

			while (myPosition < myInput.length()) {
				final char lclC = myInput.charAt(myPosition++);

				if (lclC == ']') {
					if (myPosition < myInput.length()
						&& myInput.charAt(myPosition) == ']') {
						++myPosition;
						lclSB.append(']');
					} else {
						return new Token(
							TokenType.IDENTIFIER,
							lclSB.toString()
						);
					}
				} else {
					lclSB.append(lclC);
				}
			}

			throw unsupported("Unterminated bracketed identifier");
		}

		private Token readString() {
			++myPosition;

			final StringBuilder lclSB = new StringBuilder();

			while (myPosition < myInput.length()) {
				final char lclC = myInput.charAt(myPosition++);

				if (lclC == '\'') {
					if (myPosition < myInput.length()
						&& myInput.charAt(myPosition) == '\'') {
						++myPosition;
						lclSB.append('\'');
					} else {
						return new Token(TokenType.STRING, lclSB.toString());
					}
				} else {
					lclSB.append(lclC);
				}
			}

			throw unsupported("Unterminated string literal");
		}

		private Token readNumber() {
			final int lclStart = myPosition;

			if (myInput.charAt(myPosition) == '-') {
				++myPosition;
			}

			while (myPosition < myInput.length() && Character.isDigit(myInput.charAt(myPosition))) {
				++myPosition;
			}

			if (myPosition < myInput.length() && myInput.charAt(myPosition) == '.') {
				++myPosition;

				while (myPosition < myInput.length() && Character.isDigit(myInput.charAt(myPosition))) {
					++myPosition;
				}
			}

			return new Token(
				TokenType.NUMBER,
				myInput.substring(lclStart, myPosition)
			);
		}

		private Token readWord() {
			final int lclStart = myPosition++;

			while (myPosition < myInput.length() && Character.isJavaIdentifierPart(myInput.charAt(myPosition))) {
				++myPosition;
			}

			final String lclText =
				myInput.substring(lclStart, myPosition);

			return switch (lclText.toUpperCase(Locale.ROOT)) {
				case "AND" -> new Token(TokenType.AND, lclText);
				case "OR" -> new Token(TokenType.OR, lclText);
				case "NOT" -> new Token(TokenType.NOT, lclText);
				case "IS" -> new Token(TokenType.IS, lclText);
				case "NULL" -> new Token(TokenType.NULL, lclText);
				case "BETWEEN" -> new Token(TokenType.BETWEEN, lclText);
				case "IN" -> new Token(TokenType.IN, lclText);
				default -> new Token(TokenType.IDENTIFIER, lclText);
			};
		}

		private boolean peek(char argExpected) {
			return myPosition + 1 < myInput.length() && myInput.charAt(myPosition + 1) == argExpected;
		}

		private void skipWhitespace() {
			while (myPosition < myInput.length() && Character.isWhitespace(myInput.charAt(myPosition))) {
				++myPosition;
			}
		}
	}

	private static final class Parser {
		private final Lexer myLexer;
		private final String myColumnName;
		private final JavaType<?> myType;
		private final boolean myColumnNullability;

		private Token myToken;

		private Parser(String argDefinition, String argColumnName, JavaType<?> argType, boolean argColumnNullability) {
			myLexer = new Lexer(stripCheckKeyword(argDefinition));
			myColumnName = argColumnName;
			myType = argType;
			myColumnNullability = argColumnNullability;
			myToken = myLexer.next();
		}

		private CheckExpression parse() {
			final CheckExpression lclResult = parseOr();
			require(TokenType.END);
			return lclResult;
		}

		private CheckExpression parseOr() {
			CheckExpression lclLeft = parseAnd();

			while (accept(TokenType.OR)) {
				lclLeft = new Or(lclLeft, parseAnd());
			}

			return lclLeft;
		}

		private CheckExpression parseAnd() {
			CheckExpression lclLeft = parseNot();

			while (accept(TokenType.AND)) {
				lclLeft = new And(lclLeft, parseNot());
			}

			return lclLeft;
		}

		private CheckExpression parseNot() {
			if (accept(TokenType.NOT)) {
				return new Not(parseNot());
			}

			if (accept(TokenType.LPAREN)) {
				final CheckExpression lclResult = parseOr();
				require(TokenType.RPAREN);
				return lclResult;
			}

			return parsePredicate();
		}

		private CheckExpression parsePredicate() {
			return parsePredicateTyped(castType(myType));
		}

		private <T> CheckExpression parsePredicateTyped(JavaType<T> argType) {
			final ValueExpression<T> lclLeft = parseValue(argType);

			if (accept(TokenType.IS)) {
				final boolean lclNegated = accept(TokenType.NOT);
				require(TokenType.NULL);

				return new IsNull<>(lclLeft, lclNegated);
			}

			boolean lclNegated = false;

			if (accept(TokenType.NOT)) {
				lclNegated = true;
			}

			if (accept(TokenType.BETWEEN)) {
				final ValueExpression<T> lclLower = parseValue(argType);

				require(TokenType.AND);

				final ValueExpression<T> lclUpper = parseValue(argType);

				return new Between<>(
					lclLeft,
					lclLower,
					lclUpper,
					lclNegated
				);
			}

			if (accept(TokenType.IN)) {
				require(TokenType.LPAREN);

				final List<ValueExpression<T>> lclAlternatives =
					new ArrayList<>();

				if (myToken.type() == TokenType.RPAREN) {
					throw unsupported("IN list may not be empty");
				}

				lclAlternatives.add(parseValue(argType));

				while (accept(TokenType.COMMA)) {
					lclAlternatives.add(parseValue(argType));
				}

				require(TokenType.RPAREN);

				return new In<>(
					lclLeft,
					lclAlternatives,
					lclNegated
				);
			}

			if (lclNegated) {
				throw unsupported("NOT is supported here only with BETWEEN or IN");
			}

			final ComparisonOperator lclOperator = switch (myToken.type()) {
				case EQ -> ComparisonOperator.EQ;
				case NE -> ComparisonOperator.NE;
				case LT -> ComparisonOperator.LT;
				case LE -> ComparisonOperator.LE;
				case GT -> ComparisonOperator.GT;
				case GE -> ComparisonOperator.GE;

				default -> throw unsupported("Expected comparison operator, found '" + myToken.text() + "'");
			};

			advance();

			final ValueExpression<T> lclRight = parseValue(argType);

			validateComparisonType(argType, lclOperator);

			return new Comparison<>(
				lclLeft,
				lclOperator,
				lclRight
			);
		}

		private <T> ValueExpression<T> parseValue(JavaType<T> argType) {
			if (accept(TokenType.LPAREN)) {
				final ValueExpression<T> lclResult =
					parseValue(argType);

				require(TokenType.RPAREN);

				return lclResult;
			}

			if (myToken.type() == TokenType.IDENTIFIER) {
				final String lclName = myToken.text();

				if (!lclName.equalsIgnoreCase(myColumnName)) {
					throw unsupported("Constraint references column '" + lclName + "'; expected only '" + myColumnName + "'"); // This will have to get more sophisticated for table-level constraints.
				}

				advance();

				return new Column<>(lclName, argType, myColumnNullability);
			}

			if (myToken.type() == TokenType.NULL) {
				advance();
				return new Literal<>(null, argType);
			}

			if (myToken.type() == TokenType.NUMBER) {
				final String lclText = myToken.text();
				advance();

				return new Literal<>(
					parseNumericLiteral(lclText, argType),
					argType
				);
			}

			if (myToken.type() == TokenType.STRING) {
				final String lclText = myToken.text();
				advance();

				return new Literal<>(
					parseStringLiteral(lclText, argType),
					argType
				);
			}

			throw unsupported("Expected a column or literal; found '" + myToken.text() + "'");
		}

		private boolean accept(TokenType argType) {
			if (myToken.type() != argType) {
				return false;
			}

			advance();
			return true;
		}

		private void require(TokenType argType) {
			if (!accept(argType)) {
				throw unsupported("Expected " + argType + ", found " + myToken.type() + " ('" + myToken.text() + "')");
			}
		}

		private void advance() {
			myToken = myLexer.next();
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T parseNumericLiteral(String argText, JavaType<T> argType) {
		final Class<T> lclClass = argType.javaClass();

		final Object lclValue;

		if (lclClass == byte.class || lclClass == Byte.class) {
			lclValue = Byte.valueOf(argText);
		} else if (lclClass == short.class || lclClass == Short.class) {
			lclValue = Short.valueOf(argText);
		} else if (lclClass == int.class || lclClass == Integer.class) {
			lclValue = Integer.valueOf(argText);
		} else if (lclClass == long.class || lclClass == Long.class) {
			lclValue = Long.valueOf(argText);
		} else if (lclClass == float.class || lclClass == Float.class) {
			lclValue = Float.valueOf(argText);
		} else if (lclClass == double.class || lclClass == Double.class) {
			lclValue = Double.valueOf(argText);
		} else if (lclClass == BigDecimal.class) {
			lclValue = new BigDecimal(argText);
		} else if (lclClass == boolean.class || lclClass == Boolean.class) {
			lclValue = switch (argText) {
				case "0" -> Boolean.FALSE;
				case "1" -> Boolean.TRUE;
				default -> throw unsupported("BIT/Boolean literal must be 0 or 1");
			};
		} else {
			throw unsupported("Numeric literal is inappropriate for " + lclClass.getName());
		}

		return (T) lclValue;
	}

	@SuppressWarnings("unchecked")
	private static <T> T parseStringLiteral(String argText, JavaType<T> argType) {
		final Object lclValue = switch (argType.kind()) {
			case STRING -> argText;
			case LOCAL_DATE -> LocalDate.parse(argText);
			case LOCAL_DATE_TIME -> LocalDateTime.parse(argText);
			default -> throw unsupported("String literal is inappropriate for " + argType.javaClass().getName());
		};

		return (T) lclValue;
	}

	private static <T> String renderComparison(String argLeft, ComparisonOperator argOperator, String argRight, JavaType<T> argType) {
		return switch (argType.kind()) {
			case INTEGRAL,
				 FLOATING_POINT ->
				argLeft + " "
					+ javaOperator(argOperator)
					+ " " + argRight;

			case BOOLEAN -> {
				if (argOperator != ComparisonOperator.EQ
					&& argOperator != ComparisonOperator.NE) {
					throw new IllegalStateException("Boolean ordering comparison reached emitter");
				}

				yield argLeft + " " + javaOperator(argOperator) + " " + argRight;
			}

			case BIG_DECIMAL,
				 LOCAL_DATE,
				 LOCAL_DATE_TIME -> {
				final String lclComparison = argLeft + ".compareTo(" + argRight + ")";

				yield compareToExpression(lclComparison, argOperator);
			}

			case STRING -> {
				/*
				 * SQL Server collation-awareness might be required to really do this right.
				 */
				yield switch (argOperator) {
					case EQ -> argLeft + ".equals(" + argRight + ")";
					case NE -> "!" + argLeft + ".equals(" + argRight + ")";
					case LT, LE, GT, GE -> compareToExpression(
							argLeft + ".compareTo(" + argRight + ")",
							argOperator
							);
				};
			}
		};
	}

	private static String compareToExpression(String argComparison, ComparisonOperator argOperator) {
		return argComparison + " " + switch (argOperator) {
			case EQ -> "== 0";
			case NE -> "!= 0";
			case LT -> "< 0";
			case LE -> "<= 0";
			case GT -> "> 0";
			case GE -> ">= 0";
		};
	}

	private static String javaOperator(ComparisonOperator argOperator) {
		return switch (argOperator) {
			case EQ -> "==";
			case NE -> "!=";
			case LT -> "<";
			case LE -> "<=";
			case GT -> ">";
			case GE -> ">=";
		};
	}

	private static <T> String renderLiteral(Literal<T> argLiteral) {
		final T lclValue = argLiteral.value();

		if (lclValue == null) {
			return "null";
		}

		if (lclValue instanceof String lclString) {
			return "\"" + StringEscapeUtils.escapeJava(lclString) + "\"";
		}

		if (lclValue instanceof Character lclCharacter) {
			return "'" + StringEscapeUtils.escapeJava(String.valueOf(lclCharacter)) + "'";
		}

		if (lclValue instanceof Long lclLong) {
			return lclLong + "L";
		}

		if (lclValue instanceof Float lclFloat) {
			return Float.toString(lclFloat.floatValue()) + "f";
		}

		if (lclValue instanceof Double lclDouble) {
			return Double.toString(lclDouble.doubleValue()) + "d"; // d is unnecessary, as literals are doubles by default
		}

		if (lclValue instanceof BigDecimal lclBigDecimal) {
			return "new BigDecimal(\"" + lclBigDecimal.toPlainString() + "\")";
		}

		if (lclValue instanceof Boolean lclBoolean) {
			return Boolean.toString(lclBoolean.booleanValue());
		}

		if (lclValue instanceof LocalDate lclDate) {
			return "LocalDate.of("
				+ lclDate.getYear() + ", "
				+ lclDate.getMonthValue() + ", "
				+ lclDate.getDayOfMonth()
				+ ")";
		}

		if (lclValue instanceof LocalDateTime lclDateTime) {
			return "LocalDateTime.of("
				+ lclDateTime.getYear() + ", "
				+ lclDateTime.getMonthValue() + ", "
				+ lclDateTime.getDayOfMonth() + ", "
				+ lclDateTime.getHour() + ", "
				+ lclDateTime.getMinute() + ", "
				+ lclDateTime.getSecond() + ", "
				+ lclDateTime.getNano()
				+ ")";
		}

		return lclValue.toString();
	}

	private static void validateComparisonType(JavaType<?> argType, ComparisonOperator argOperator) {
		if (argType.kind() == JavaKind.BOOLEAN
			&& argOperator != ComparisonOperator.EQ
			&& argOperator != ComparisonOperator.NE) {
			throw unsupported("Only = and <> are supported for BIT/Boolean");
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> JavaType<T> castType(JavaType<?> argType) {
		return (JavaType<T>) argType;
	}

	private static String stripCheckKeyword(String argDefinition) {
		final String lclTrimmed = argDefinition.trim();

		if (lclTrimmed.regionMatches(true, 0, "CHECK", 0, 5)) {
			return lclTrimmed.substring(5).trim();
		}

		return lclTrimmed;
	}

	private static String sourceName(Class<?> argClass) {
		return argClass.isPrimitive() ? argClass.getName() : argClass.getCanonicalName();
	}

	private static UnsupportedCheckException unsupported(String argMessage) {
		return new UnsupportedCheckException(argMessage);
	}

	private static final class UnsupportedCheckException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private UnsupportedCheckException(String argMessage) {
			super(argMessage);
		}
	}
}
