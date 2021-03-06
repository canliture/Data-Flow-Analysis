package ch.yvu.dfa.parser.dot;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.yvu.dfa.controlflowgraph.ControlflowGraph;
import ch.yvu.dfa.expressions.Expression;
import ch.yvu.dfa.expressions.Variable;
import ch.yvu.dfa.parser.FormatException;
import ch.yvu.dfa.parser.expression.ExpressionParser;

public class SimpleDOTParser {

	private final static String ASSIGNMENT_OPERATOR = ":=";
	
	private final static String GREATER_THAN = ">";
	private final static String GREATER_OR_EQUAL_THAN = ">=";
	private final static String SMALLER_THAN = "<";
	private final static String SMALLER_OR_EQUAL_THAN = "<=";
	private final static String EQUAL = "==";
	
	private String input;	
	private ControlflowGraph graph;
	private ExpressionParser parser;
	
	public SimpleDOTParser(String input, ExpressionParser parser){
		this.input = input;
		this.graph = new ControlflowGraph();
		this.parser = parser;
	}
	
	public ControlflowGraph parse() throws FormatException{
		String trimmedInput = this.input.replaceAll("\\s","");
		String edgeInput = extractEdgeInput(trimmedInput);
		
		StringTokenizer tokens = new StringTokenizer(edgeInput, ";");
		Pattern edgePattern = Pattern.compile("^([0-9]+)->([0-9]+)(\\[label=\"(true|false)\"\\])?");
		
		Pattern nodePattern = Pattern.compile("^([0-9]+)\\[label=\"([0-9]+:)?([^\"]+)\"\\]");
		while(tokens.hasMoreElements()){
			String token = tokens.nextToken();
			Matcher edgeMatcher = edgePattern.matcher(token);
			Matcher nodeMatcher = nodePattern.matcher(token);
			if(nodeMatcher.find()){
				int nodeId = Integer.parseInt(nodeMatcher.group(1));
				String expression = nodeMatcher.group(3);
				createNode(nodeId, expression);
			} else if(edgeMatcher.find()){
				int nodeFromId = Integer.parseInt(edgeMatcher.group(1));
				int nodeToId = Integer.parseInt(edgeMatcher.group(2));
				createEdge(nodeFromId, nodeToId);
			} else {
				//If format not recognize don't consider it for
				//controlflow graph but don't throw an execption
				//because only a subset of DOT language is supported
				//(unrecognized tokens can still be valid DOT)
			}
		}
		return this.graph;
	}
	
	private void createNode(int id, String expression) throws FormatException {
		if(expression.contains(ASSIGNMENT_OPERATOR)){
			createAssignmentNode(id, expression);
		} else {
			createConditionalNode(id, expression);
		}
	}
	
	private void createConditionalNode(int id, String conditionalExpression) throws FormatException{
		//Order of if...else if... is important
		//as SMALLER ("<") is a substring of SMALLER_OR_EQUAL ("<=")
		
		String[] tokens;
		String operation;
		if(conditionalExpression.contains(SMALLER_OR_EQUAL_THAN)){
			 tokens = conditionalExpression.split(SMALLER_OR_EQUAL_THAN);
			 operation = SMALLER_OR_EQUAL_THAN;
		} else if(conditionalExpression.contains(GREATER_OR_EQUAL_THAN)){
			tokens = conditionalExpression.split(GREATER_OR_EQUAL_THAN);
			operation = GREATER_OR_EQUAL_THAN;
		} else if(conditionalExpression.contains(SMALLER_THAN)){
			tokens = conditionalExpression.split(SMALLER_THAN);
			operation = SMALLER_THAN;
		} else if(conditionalExpression.contains(GREATER_THAN)){
			tokens = conditionalExpression.split(GREATER_THAN);
			operation = GREATER_THAN;
		}  else if(conditionalExpression.contains(EQUAL)){
			tokens = conditionalExpression.split(EQUAL);
			operation = EQUAL;
		} else {
			throw new FormatException();
		}
		if(tokens.length != 2) throw new FormatException();
		Expression lhs = this.parser.parse(tokens[0]);
		Expression rhs = this.parser.parse(tokens[1]);
		try{
			this.graph.createConditionalNode(id, lhs, rhs, operation);
		}catch(Exception e){
			throw new FormatException(e);
		}
	}
	
	private void createAssignmentNode(int id, String assignmentExpression) throws FormatException{
		String[] tokens = assignmentExpression.split(ASSIGNMENT_OPERATOR);
		if(tokens.length != 2) throw new FormatException();
		try{
			Expression lhs = this.parser.parse(tokens[0]);
			assert(lhs instanceof Variable);
			
			Expression rhs = this.parser.parse(tokens[1]);
			
			this.graph.createAssignmentNode(id, (Variable) lhs, rhs);
		} catch(Exception e){
			throw new FormatException(e);
		}
	}

	private void createEdge(int nodeFromId, int nodeToId) throws FormatException{
		try {
			this.graph.addEdge(nodeFromId, nodeToId);
		} catch (Exception e) {
			throw new FormatException(e);
		}
	}
	
	/**
	 * Extract <node> part from 
	 * digraphgraphName{<node>}
	 */
	private static String extractEdgeInput(String input) throws FormatException{
		Pattern blockPattern = Pattern.compile("[^\\{]*\\{([^\\}]*)\\}");
		Matcher blockMatcher = blockPattern.matcher(input);
		
		if(!blockMatcher.find()) throw new FormatException();
		
		return blockMatcher.group(1);
	}	
}
