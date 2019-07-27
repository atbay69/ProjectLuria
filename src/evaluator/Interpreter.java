package evaluator;

import static lexical_analysis.TokenType.*;

import java.util.List;

import org.w3c.dom.Text;

import environment.MemoryEnvironment;
import lexical_analysis.Token;
import luria.Luria;
import syntactic_analysis.Expression;
import syntactic_analysis.RuntimeError;
import syntactic_analysis.Statement;
import syntactic_analysis.Expression.Assignment;
import syntactic_analysis.Expression.Binary;
import syntactic_analysis.Expression.Grouping;
import syntactic_analysis.Expression.Literal;
import syntactic_analysis.Expression.Logical;
import syntactic_analysis.Expression.Unary;
import syntactic_analysis.Expression.VariableExpression;
import syntactic_analysis.Statement.Block;
import syntactic_analysis.Statement.Class;
import syntactic_analysis.Statement.Function;
import syntactic_analysis.Statement.If;
import syntactic_analysis.Statement.Print;
import syntactic_analysis.Statement.Variable;
import syntactic_analysis.Statement.While;

public class Interpreter implements Expression.Visitor<Object>, Statement.Visitor<Void> {
	
	private MemoryEnvironment environment = new MemoryEnvironment();  

/*	public void interpret(Expression e) {
		try {
			Object v = evaluate(e);
			System.out.println(stringify(v));
		} catch (RuntimeError error) {
			Luria.runtimeError(error);
		}
	}*/
	
	public void interpret(List<Statement> statements) {
		try {
			for (Statement s : statements) {
				execute(s);
			}
		} catch (RuntimeError error) {
			Luria.runtimeError(error);
		}
	}
	
	private Object evaluate(Expression e) {
		return e.accept(this);
	}
	
	private void execute(Statement s) {
		s.accept(this);
	}
	
	private void executeBlock(List<Statement> statements, MemoryEnvironment environment) {
		MemoryEnvironment previous = this.environment;
		try {
			this.environment = environment;
			for (Statement s : statements) {
				execute(s);
			}

		} finally {
			this.environment = previous;
		}
	}

	@Override
	public Object visitBinaryExpression(Binary e) {
		Object left = evaluate(e.left);
		Object right = evaluate(e.right);
		
		switch (e.operator.type) {
		// arithmetic
		case PLUS:
			if (left instanceof Double && right instanceof Double) {
				return (double) left + (double) right;
			}
			
			if (left instanceof String && right instanceof String) {
				return (String) left + (String) right;
			}
	        throw new RuntimeError(e.operator, "Error: Operands must be of same type.");  
		case MINUS: checkOperands(e.operator, left, right);
			return (double) left - (double) right;
		case FORWARD_SLASH: checkOperands(e.operator, left, right);
			return (double) left / (double) right;
		case ASTERISK: checkOperands(e.operator, left, right);
			return (double) left * (double) right;
		// comparison operators
		case GREATER: checkOperands(e.operator, left, right);
			return (double) left > (double) right;
	    case GREATER_EQUAL: checkOperands(e.operator, left, right);
	    	return (double) left >= (double) right;
	    case LESS: checkOperands(e.operator, left, right);
	    	return (double) left < (double) right; 
	    case LESS_EQUAL: checkOperands(e.operator, left, right);
	    	return (double) left <= (double) right;
	    // equality
	    case EXCLAMATION_EQUAL: return !equal(left, right);
	    case EQUAL_EQUAL: return equal(left,right);
		}
		return null;
	}

	@Override
	public Object visitGroupingExpression(Grouping e) {
		return evaluate(e.expression);
	}

	@Override
	public Object visitLiteralExpression(Literal e) {
		return e.value;
	}

	@Override
	public Object visitUnaryExpression(Unary e) {
		// evaluate the operand expression; apply the unary operator itself to result
		Object right = evaluate(e.right);
		switch (e.operator.type) {
		// - negates result of subexpression
		case MINUS:
			checkOperand(e.operator, right);
			return -(double)right;
		case EXCLAMATION:
			return !truthy(right);
		}
		return null;
	}
	
	private void checkOperand(Token operator, Object operand) {
	    if (operand instanceof Double) return;                         
	    throw new RuntimeError(operator, "Error: Operand must be a number.");
	}

	private void checkOperands(Token operator, Object left, Object right) {
	    if (left instanceof Double && right instanceof Double) return;	
	    throw new RuntimeError(operator, "Error: Operands must be numbers.");
	}
	
	private boolean truthy(Object o) {
		if (o == null) return false;
		if (o instanceof Boolean) return (boolean) o;
		return true;
	}
	
	private boolean equal(Object l, Object r) {
		if (l == null && r == null) return true;
		if (l == null) return false;
		
		return l.equals(r);
	}

	private String stringify(Object o) {
		if (o == null) return "nil";
		
		if (o instanceof Double) {
			String t = o.toString();
			if (t.endsWith(".0")) {
				t = t.substring(0, t.length() - 2);
			}
			return t;
		}
		return o.toString();
	}

	@Override
	public Void visitExpressionStatement(syntactic_analysis.Statement.ExpressionStatement statement) {
		evaluate(statement.expression);
		return null;
	}

	@Override
	public Void visitPrintStatement(Print statement) {
		Object value = evaluate(statement.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitVariableStatement(Variable statement) {
		Object value = null;
		if (statement.initialisation != null) {
			value = evaluate(statement.initialisation);
		}
	    environment.lookup(statement.symbol.lexeme, value);
	    return null; 
	}

	@Override
	public Object visitVariableExpression(VariableExpression expression) {
		return environment.get(expression.symbol);
	}

	@Override
	public Object visitAssignmentExpression(Assignment expression) {
	    Object value = evaluate(expression.value);
	    
	    environment.store(expression.symbol, value);
		return value;
	}

	@Override
	public Void visitBlockStatement(Block statement) {
	    executeBlock(statement.statements, new MemoryEnvironment(environment));
		return null;
	}

	@Override
	public Void visitClassStatement(Class statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitFunctionStatement(Function statement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitIfStatement(If statement) {
		if (truthy(evaluate(statement.condition))) {
			execute(statement.thenBranch);
		} else if (statement.elseBranch != null) {
			execute(statement.elseBranch);
		}
		return null;
	}

	@Override
	public Object visitLogicalExpression(Logical expression) {
	    Object left = evaluate(expression.left);

	    if (expression.operator.type == OR) {        
	      if (truthy(left)) return left;               
	    } else {                                         
	      if (!truthy(left)) return left;              
	    }                                                

	    return evaluate(expression.right);
	}

	@Override
	public Void visitWhileStatement(While statement) {
		while (truthy(evaluate(statement.condition))) {
			execute(statement.body);
		}
		return null;
	}

}
