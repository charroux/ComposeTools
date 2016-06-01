package org.olabdynamics.compose.tools.visitor

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;

class CustomSourceOperation extends CompilationUnit.PrimaryClassNodeOperation { 
	CodeVisitorSupport visitor 
	void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException { 
		classNode.visitContents(visitor) 
	} 
	
} 