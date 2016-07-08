package org.olabdynamics.compose.tools.visitor

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.Statement
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
	boolean aggregateInstruction = false
	
	def instructions = []
	
	Application getByName(def applicationName){
		def application
		for(int i=0; i<instructions.size(); i++){
			if(instructions.getAt(i).springBean.name == applicationName){
				return instructions.getAt(i).springBean
			}
		}
		return null
	}

	Application[] getApplicationsInExpression(def expression){
		def applications = []
		String[] elements = expression.split(" ")
		Application application
		for(String element: elements){
			element = element.trim()
			application = this.getByName(element)
			if(application != null){
				applications.add(application)
			}
		}
		return applications
	}

/*	void visitVariableExpression(VariableExpression expression) {
		super.visitVariableExpression(expression)
		log.info "visitVariableExpression = " + expression
	}*/
	
/*	void visitArgumentlistExpression(ArgumentListExpression expression){
		super.visitArgumentlistExpression(expression)
		log.info "visitArgumentlistExpression = " + expression
	}*/
	
/*	void visitPropertyExpression(PropertyExpression propertyExpression){
		
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
	}*/

	void visitConstantExpression(ConstantExpression expression){
		log.info "debut"
	}	
	
	/**
	 * méthode qui appelle la classe de création de fichier xml
	 */
	void visitMethodCallExpression(MethodCallExpression call){
			
		log.info "debut"
		
		def method
		
		def instruction
		
		Expression expression = call.getObjectExpression()
		
		if(expression instanceof MethodCallExpression){
			MethodCallExpression methodExpression = (MethodCallExpression)expression
			expression = methodExpression.getMethod()
			if(expression instanceof ConstantExpression){
				ConstantExpression constantExpression = (ConstantExpression)expression
				method = constantExpression.getText() 
				log.info method	+ " from ConstantExpression"	// from
			}
			expression = methodExpression.getObjectExpression()
			if(expression instanceof MethodCallExpression){
				MethodCallExpression methodExp = (MethodCallExpression)expression
				expression = methodExp.getArguments()
				if(expression instanceof ArgumentListExpression){
					ArgumentListExpression argumentListExpression = (ArgumentListExpression)expression
					Iterator<Expression> args = argumentListExpression.getExpressions().iterator()
					def i = 0
					while(args.hasNext()){
						expression = args.next()
						if(expression instanceof VariableExpression){
							VariableExpression variableExpression = (VariableExpression)expression
							def arg = variableExpression.getText()	// incomingMessageEvent
							log.info arg + " incomingMessageEvent VariableArgumentExpression " + i
							if(method == 'from'){
								instruction = new Instruction(variable: arg)
							}
							i++
						}
					}
				}
				expression = methodExp.getMethod()
				if(expression instanceof ConstantExpression){
					ConstantExpression constantExpression = (ConstantExpression)expression
					method = constantExpression.getText()
					log.info method	+ " receive ConstantExpression"	// receive
					if(method == 'receive'){
						instruction.instruction = 'receive'
					}
				}
			}
						
			expression = methodExpression.getArguments()
			if(expression instanceof ArgumentListExpression){
				ArgumentListExpression argumentListExpression = (ArgumentListExpression)expression
				Iterator<Expression> args = argumentListExpression.getExpressions().iterator()
				def i = 0
				while(args.hasNext()){
					expression = args.next()
					if(expression instanceof VariableExpression){
						VariableExpression variableExpression = (VariableExpression)expression
						def arg = variableExpression.getText()	// event, code1, composeEvents
						log.info arg + " composeEvents VariableArgumentExpression " + i
						i++
						if(instruction!=null && instruction.instruction=='receive'){
							instruction.springBean = context.getBean(arg)
						} else if(method == 'compute'){
							instruction = new Instruction(instruction:'compute', springBean: context.getBean(arg))		// arg instanceof Application
						} else if(method == 'receive'){
							instruction = new Instruction(instruction:'receive', variable: arg)
						} 
					}
				}
				if(expression instanceof PropertyExpression){
					PropertyExpression propertyExpression = (PropertyExpression)expression
					expression = propertyExpression.getObjectExpression()
					if(expression instanceof VariableExpression){
						VariableExpression variableExpression = (VariableExpression)expression
						def ar = variableExpression.getText()	// code1
						if(method == 'send'){
							instruction = new Instruction(instruction:'send', variable: ar)
						}
						log.info ar  + " PropertyVariableExpression"
					}
					expression = propertyExpression.getProperty()
					if(expression instanceof ConstantExpression){
						ConstantExpression constantExpression = (ConstantExpression)expression
						def value = constantExpression.getText()	// code1.result
						if(method == 'send'){
							instruction.variableProperty = value							
						}
						log.info value + " PropertyValueExpression"
					}
				}
			}
		}
		
		expression = call.getMethod()
		if(expression instanceof ConstantExpression){
			ConstantExpression constantExpression = (ConstantExpression)expression
			method = constantExpression.getText()
			log.info method	+ " with MethodExpression" //runScript, from, with
		}
		
		expression = call.getArguments()
		if(expression instanceof ArgumentListExpression){
			ArgumentListExpression argumentListExpression = (ArgumentListExpression)expression
			Iterator<Expression> args = argumentListExpression.getExpressions().iterator()
			def i = 0
			while(args.hasNext()){
				expression = args.next()
				if(expression instanceof VariableExpression){
					VariableExpression variableExpression = (VariableExpression)expression
					def arg = variableExpression.getText()	// args, input, database
					log.info arg  + " VariableArgumentExpression " + i
					i++
					if(method == 'from'){
						instruction.springBean = context.getBean(arg)		// arg instanceof EventHandler
						//instructions.add(context.getBean(arg))		// arg instanceof EventHandler
					} else if(method == 'to'){
						instruction.springBean = context.getBean(arg)		// arg instanceof EventHandler
						//instructions.add(context.getBean(arg))		// arg instanceof EventHandler
					} else if(method == 'with'){
						def with = new Instruction.With()
						with.with = arg
						instruction.withs.add(with)
					}
				}
				if(expression instanceof PropertyExpression){
					PropertyExpression propertyExpression = (PropertyExpression)expression
					expression = propertyExpression.getObjectExpression()
					
					def with = new Instruction.With()
					
					if(expression instanceof VariableExpression){
						VariableExpression variableExpression = (VariableExpression)expression
						def prop = variableExpression.getText()		// event
						log.info prop + " PropertyVariableExpression"
						if(method == 'with'){
							with.with = prop  
						}
					}
					expression = propertyExpression.getProperty()
					if(expression instanceof ConstantExpression){
						ConstantExpression constantExpression = (ConstantExpression)expression
						def value = constantExpression.getText() // event.value
						log.info value + " PropertyValueExpression"
						if(method == 'with'){
							//instruction.withProperty = value
							with.withProperty = value
							instruction.withs.add(with)
						}
					}
				} else if(expression instanceof ConstantExpression){
					ConstantExpression constantExpression = (ConstantExpression)expression
					def m = constantExpression.getText()
					log.info m	+ " incomingMessageEvent.state=INCOMING_MESSAGE ConstantExpression" // receive event with constantExpression
					if(method == 'whose'){
						instruction.whose = m
					}
				}
			}
		}
		
		if(method=='compute' || method=='with' || method=='whose' ||method=='from' || method=='to'){
			instructions.add(instruction)
			if(aggregateInstruction == true){
				int index = aggregators.size()-1
				aggregators.get(index).nextInstruction = instruction
				aggregateInstruction = false
			}
		}
		
		if(method=="when" && expression instanceof ConstantExpression){
			ConstantExpression constantExpression = (ConstantExpression)expression
			def value = constantExpression.getText() // event.value
			log.info value + " PropertyValueExpression"
			
			def applications = this.getApplicationsInExpression(value)

			def aggregator
			aggregator = new Aggregator(releaseExpression: value, applications: applications)
			aggregators.add(aggregator)
			aggregateInstruction = true
		} else {
			aggregateInstruction = false
		}
		
		
		
		log.info "fin"
		
	}
	
	protected SourceUnit getSourceUnit() {
		return source;
	}

}
