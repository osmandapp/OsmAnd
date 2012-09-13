package net.osmand.translator.visitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class MethodsBodyVisitor extends ASTVisitor {
	private List<Statement> statements = new ArrayList<Statement>();

	@Override
	public boolean visit(ForStatement node) {
		node.getStartPosition();
		System.out.println("ForStatement:" + node.toString());
		statements.add(node);
		return true;
	}

	@Override
	public boolean visit(IfStatement node) {
		System.out.println("IfStatement:" + node.toString());
		Statement elseStatement = node.getElseStatement();
		if (elseStatement != null) {
			System.out.println("ElseStatement:" + elseStatement.toString());
		}
		statements.add(node);
		return true;
	}

	@Override
	public boolean visit(WhileStatement node) {
		System.out.println("WhileStatement:" + node.toString());
		statements.add(node);
		return true;
	}

	@Override
	public boolean visit(DoStatement node) {
		System.out.println("DoStatement:" + node.toString());
		statements.add(node);
		return true;
	}

	@Override
	public boolean visit(SwitchStatement node) {
		System.out.println("SwitchStatement:" + node.toString());
		statements.add(node);
		return true;
	}

	@Override
	public boolean visit(TryStatement node) {
		System.out.println("TryStatement:" + node.toString());
		statements.add(node);
		return true;
	}

	@Override
	public boolean visit(SynchronizedStatement node) {
		System.out.println("SynchronizedStatement:" + node.toString());
		statements.add(node);
		return true;
	}

	public List<Statement> getStatements() {
		return statements;
	}
}
