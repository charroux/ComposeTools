package org.olabdynamics.compose.tools.visitor

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.olabdynamics.compose.Application
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service
import groovy.util.logging.Slf4j

@Slf4j
class ComposeCodeVisitor extends ClassCodeVisitorSupport{
	
	ApplicationContext context
	
	def aggregators = []
	
	def instructions = []
	
	Application getByName(def applicationName){
		def application
		for(int i=0; i<instructions.size(); i++){
			if(instructions.getAt(i).name == applicationName){
				return instructions.getAt(i)
			}
		}
		return null
	}
	
/*	void visitMethod(MethodNode node){
		super.visitMethod(node)
		println "visitMethod = " + node
	}*/
	
/*	void visitClassExpression(ClassExpression expression){
		super.visitClassExpression(expression)
		println "visitClassExpression = " + expression
	}*/
	
/*	void visitConstantExpression(ConstantExpression expression){
		super.visitConstantExpression(expression)
		println "visitConstantExpression = " + expression
	}*/

	void visitVariableExpression(VariableExpression expression) {
		super.visitVariableExpression(expression)
		log.info "visitVariableExpression = " + expression
	}
	
	void visitArgumentlistExpression(ArgumentListExpression expression){
		super.visitArgumentlistExpression(expression)
		log.info "visitArgumentlistExpression = " + expression
	}
	
	void visitPropertyExpression(PropertyExpression propertyExpression){
		
		super.visitPropertyExpression(propertyExpression)
		log.info "visitPropertyExpression = " + propertyExpression
		
		def method
		def applications = []
		
		Expression expression = propertyExpression.getObjectExpression()
		if(expression instanceof MethodCallExpression){
			MethodCallExpression methodExpression = (MethodCallExpression)expression
			expression = methodExpression.getMethod()
			if(expression instanceof ConstantExpression){
				ConstantExpression constantExpression = (ConstantExpression)expression
				method = constantExpression.getText()
				log.info method	+ " MethodExpression"	// when
			}
			expression = methodExpression.getArguments()
			if(expression instanceof ArgumentListExpression){
				ArgumentListExpression argumentListExpression = (ArgumentListExpression)expression
				Iterator<Expression> args = argumentListExpression.getExpressions().iterator()
				while(args.hasNext()){
					expression = args.next()
					if(expression instanceof VariableExpression){
						VariableExpression variableExpression = (VariableExpression)expression
						def arg = variableExpression.getText()	// code1 de when
						def application = getByName(arg)
						applications.add(application)
						log.info arg 	+ " VariableArgumentExpression"
						//println 'context : ' + context.getBean(code)
						
					}
				}
			}
		}
		
		def aggregator
		
		Expression propExpression = propertyExpression.getProperty()
		if(propExpression instanceof ConstantExpression){
			ConstantExpression constantExpression = (ConstantExpression)propExpression
			def value = constantExpression.getText()	// terminates
			aggregator = new Aggregator(releaseExpression: value, applications: applications)
			aggregators.add(aggregator)
			log.info value 	+ " PropertyConstantExpression"
		}	
	}

	
	/**
	 * m�thode qui appelle la classe de cr�ation de fichier xml
	 */
	void visitMethodCallExpression(MethodCallExpression call){
			
		def method
		
		Expression expression = call.getObjectExpression()
		if(expression instanceof MethodCallExpression){
			MethodCallExpression methodExpression = (MethodCallExpression)expression
			expression = methodExpression.getMethod()
			if(expression instanceof ConstantExpression){
				ConstantExpression constantExpression = (ConstantExpression)expression
				method = constantExpression.getText() 
				log.info method	+ " MethodExpression"	// receive, compute, send
			}
			expression = methodExpression.getArguments()
			if(expression instanceof ArgumentListExpression){
				ArgumentListExpression argumentListExpression = (ArgumentListExpression)expression
				Iterator<Expression> args = argumentListExpression.getExpressions().iterator()
				while(args.hasNext()){
					expression = args.next()
					if(expression instanceof VariableExpression){
						VariableExpression variableExpression = (VariableExpression)expression
						def arg = variableExpression.getText()	// event, code1
						log.info arg + " VariableArgumentExpression"
						//println 'context : ' + context.getBean(code)
						if(method == 'compute'){
							instructions.add(context.getBean(arg))				// arg instanceof Application
						} 
					}
				}
				if(expression instanceof PropertyExpression){
					PropertyExpression propertyExpression = (PropertyExpression)expression
					expression = propertyExpression.getObjectExpression()
					if(expression instanceof VariableExpression){
						VariableExpression variableExpression = (VariableExpression)expression
						def ar = variableExpression.getText()	// code1
						log.info ar  + " PropertyVariableExpression"
					}
					expression = propertyExpression.getProperty()
					if(expression instanceof ConstantExpression){
						ConstantExpression constantExpression = (ConstantExpression)expression
						def value = constantExpression.getText()	// code1.result
						log.info value + " PropertyValueExpression"
					}
				}
			}
		}
		
		expression = call.getMethod()
		if(expression instanceof ConstantExpression){
			ConstantExpression constantExpression = (ConstantExpression)expression
			method = constantExpression.getText()
			log.info method	+ " MethodExpression" //runScript
		}
		
		expression = call.getArguments()
		if(expression instanceof ArgumentListExpression){
			ArgumentListExpression argumentListExpression = (ArgumentListExpression)expression
			Iterator<Expression> args = argumentListExpression.getExpressions().iterator()
			while(args.hasNext()){
				expression = args.next()
				if(expression instanceof VariableExpression){
					VariableExpression variableExpression = (VariableExpression)expression
					def arg = variableExpression.getText()	// args, input, database
					log.info arg  + " VariableArgumentExpression"
					//println 'context : ' + context.getBean(code)
					if(method == 'from'){
						instructions.add(context.getBean(arg))		// arg instanceof EventHandler
					} else if(method == 'to'){
						instructions.add(context.getBean(arg))		// arg instanceof EventHandler
					}
					
				}
				if(expression instanceof PropertyExpression){
					PropertyExpression propertyExpression = (PropertyExpression)expression
					expression = propertyExpression.getObjectExpression()
					if(expression instanceof VariableExpression){
						VariableExpression variableExpression = (VariableExpression)expression
						def prop = variableExpression.getText()		// event
						log.info prop + " PropertyVariableExpression"
					}
					expression = propertyExpression.getProperty()
					if(expression instanceof ConstantExpression){
						ConstantExpression constantExpression = (ConstantExpression)expression
						def value = constantExpression.getText() // event.value
						log.info value + " PropertyValueExpression"
					}
				}
			}
		}
		
		
		
	}
	
	protected SourceUnit getSourceUnit() {
		return source;
	}

}
