package org.olabdynamics.compose.tools.xmlgenerator

import groovy.xml.QName
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

import org.olabdynamics.compose.Application
import org.olabdynamics.compose.DatabaseAdapter
import org.olabdynamics.compose.EventHandler
import org.olabdynamics.compose.HttpAdapter
import org.olabdynamics.compose.Input
import org.olabdynamics.compose.JavaServiceAdapter
import org.olabdynamics.compose.tools.visitor.Aggregator
import org.olabdynamics.compose.tools.visitor.Instruction
import org.springframework.context.ApplicationContext;

class XmlGenerator {
	
	def xmlSpringContent
	def instructions
	def aggregators

	def xmls = []
	
	void generate(){
		
		def hashcodes = []
		
		instructions.each {
			if(it.springBean instanceof Application){
				def xmlForCompute = generateApplication(it)
				xmls.add(xmlForCompute)
			} else if(it.springBean instanceof EventHandler){
				if(it.hashCode() in hashcodes){
					def xmlRouter = generateRouter(it)
					xmls.add(xmlRouter)
				} else {
					def xmlEvent = generateEventHandler(it)
					xmls.add(xmlEvent)
					hashcodes.add(it.hashCode())
				}				
			} 
		}
		
		aggregators.each {
			def xmlForAggregator = generateAggregator(it)
			xmls.add(xmlForAggregator)
		}
		
		//new File('essai.xml').withWriter('utf-8') { writer ->
		new File(xmlSpringContent).withWriter('utf-8') { writer ->
			writer.writeLine '<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:int="http://www.springframework.org/schema/integration" xmlns:int-http="http://www.springframework.org/schema/integration/http" xmlns:int-groovy="http://www.springframework.org/schema/integration/groovy" xmlns:context="http://www.springframework.org/schema/context" xmlns:task="http://www.springframework.org/schema/task" xmlns:jdbc="http://www.springframework.org/schema/jdbc" xmlns:int-jdbc="http://www.springframework.org/schema/integration/jdbc" xmlns:int-script="http://www.springframework.org/schema/integration/scripting" xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task.xsd http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration-4.2.xsd http://www.springframework.org/schema/integration/jdbc http://www.springframework.org/schema/integration/jdbc/spring-integration-jdbc-4.2.xsd http://www.springframework.org/schema/jdbc http://www.springframework.org/schema/jdbc/spring-jdbc-4.2.xsd http://www.springframework.org/schema/integration/http http://www.springframework.org/schema/integration/http/spring-integration-http-4.2.xsd http://www.springframework.org/schema/integration/groovy http://www.springframework.org/schema/integration/groovy/spring-integration-groovy-4.2.xsd http://www.springframework.org/schema/integration/scripting http://www.springframework.org/schema/integration/scripting/spring-integration-scripting-4.2.xsd">' 
			xmls.each{
				writer.writeLine it.toString()
			}
			writer.writeLine '</beans>'
		}
		
		//String xmlContext = new File('essai.xml').text
		String xmlContext = new File(xmlSpringContent).text
		
		String springContext = XmlUtil.serialize(xmlContext)
		
		def beans = new XmlParser().parseText(springContext)

		def aggregator = beans."*".find { node->
			node.name() == "int:aggregator"
		}

		aggregators.each {	
			
			it.applications.each {	
				
				// connect the aggregator input
				
				def applicationName = it.name + 'Output'	// it.name == code1 for compute code1
									
				def transformer = beans."*".find { node->	// find a transformer after a compute
					node.name()=="int:transformer" && node.@"input-channel"==applicationName
				}
					
				transformer.@"output-channel" = aggregator.@"input-channel"	// connect the transformer output to the aggregator input
				
				// connect the aggregator output
				
				applicationName = applicationName + 'Channel'
				
				def applicationTransformerNode = beans."*".find { node->	// find the instruction using code1.result
					node.@"channel"==applicationName || node.@"input-channel"==applicationName
				}
				
				//println applicationTransformerNode
				
				def aggregratorTransformer = beans."*".find { node->	// find the transformer after the aggregator
					node.name()=="int:transformer" && node.@"input-channel"==aggregator.@"output-channel"
				}
				
				//println aggregratorTransformer
				
				applicationTransformerNode.@"input-channel" = aggregratorTransformer.@"output-channel"	// connect the aggregator output
						
			}
			
		}
		
		def router = beans."*".find { node->	// find a transformer after a compute
			node.name()=="int:recipient-list-router"
		}
		

		if(router != null){
			
			println "router = " + router.@"input-channel"
			def routeChannelName
			
			Iterator routes = router.iterator()
			while(routes.hasNext()){
				
				routeChannelName = routes.next().@"channel"
				
				println "routes = " + routeChannelName
				
				def serviceActivator = beans."*".find { node->	// find a transformer after a compute
					node.name()=="int:service-activator" && node.@"input-channel"==router.@"input-channel"
				}
			
				println "serviceActivators = " + serviceActivator
				
				serviceActivator.@"input-channel" = routeChannelName
			}
	
		}
		
		def springContexteAsText = XmlUtil.serialize(beans)
		
		//new File('essai.xml').withWriter('utf-8') { writer ->
		new File(xmlSpringContent).withWriter('utf-8') { writer ->
			writer.writeLine springContexteAsText
		}
		
		
	}
	
	def localApplicationContext = {
		String codeName, String inputName, String expression ->
		//def inputFileChannel = 'inputFileChannel' + i
		def inputChannel = inputName + 'Channel'
		def outputServiceChannel = codeName + "Output"
		def outputChannel = codeName + 'OutputChannel'
		def exp = expression
		def transformerBean = codeName + "TransformerBean"
		def transformerRef = codeName
		def clos = {
	
				"int:service-activator"(id:"service-activator-"+inputChannel+"-id", "input-channel":inputChannel, "output-channel":outputServiceChannel, expression:exp){}
				
				"int:transformer"(id:"transformer-"+outputServiceChannel+"-id", "input-channel":outputServiceChannel, "output-channel":outputChannel, ref:transformerBean, "method":"transform"){ }
				
				"bean"(id:transformerBean, class:"org.olabdynamics.compose.tools.code.ObjectToApplicationTransformer"){
					"property"(name:"application", ref:transformerRef){ }
				}
		//	}
		
		}
	}
	
	def generateApplication(Instruction instruction){
		//def fileName = 'localApplicationContext.xml'
		def inputName = instruction.variable
		def Application application = instruction.springBean
		def applicationName = application.name
		def methodName = application.input.adapter.method
		if(application.input.adapter instanceof JavaServiceAdapter){
			JavaServiceAdapter javaServiceAdapter = (JavaServiceAdapter)application.input.adapter
			String className = javaServiceAdapter.javaClass
			className = className.substring(className.lastIndexOf('.')+1)
			className = className.substring(0,1).toLowerCase() + className.substring(1)
			def expression = '@' + className + '.' + methodName + '(payload)'
			def xml = new StreamingMarkupBuilder()
			xml.useDoubleQuotes = true
			return xml.bind(localApplicationContext(applicationName,inputName,expression))
		} else {
			def message = application.input.adapter + ' is not supported yet.'
			throw new CompilationException(message)
		}
	}
	
	def inputHttpAdapterContext = {
		String inputName, String outputName ->
		def inputChannel = inputName + 'InputChannel'
		def gatewayReplyChannel = inputName + "ReplyChannel"
		def outputChannel = outputName + "Channel"
		def serviceInterface = "myservice." + inputName.substring(0, 1).toUpperCase() + inputName.substring(1) + "Gateway"
		def id = "headers['id'].toString()"
		def clos = {
	
			//"int:gateway"(id:"gateway-"+inputChannel+"-id", "default-request-channel":inputChannel, "default-reply-channel":gatewayReplyChannel, "service-interface":serviceInterface){ }
			
			"int:gateway"(id:"gateway-"+inputChannel+"-id", "default-request-channel":inputChannel, "service-interface":serviceInterface){ }
			
			"int:channel"(id:inputChannel){ }
			
			"int:header-enricher"(id:"header-enricher-"+inputChannel+"-id", "input-channel":inputChannel, "output-channel":outputChannel){
				"int:header"(name:"messageID", expression:id){ }
			}
				 
			//"int:channel"(id:gatewayReplyChannel){ }
			
		}
	}
	
	def generateEventHandler(Instruction instruction){
		def EventHandler eventHandler = instruction.springBean
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		if(eventHandler.input != null){
			if(eventHandler.input.adapter instanceof HttpAdapter){
				def inputName = eventHandler.name
				def outputName = instruction.variable
				return xml.bind(inputHttpAdapterContext(inputName, outputName))
				//BufferedWriter file = springApplicationContext.newWriter()
				//xml.bind(inputHttpAdapterContext(inputName)).writeTo( file )
			}
		} 
		if(eventHandler.output != null){
			if(eventHandler.output.adapter instanceof DatabaseAdapter){
				def inputName = instruction.variable
				return xml.bind(outputDatabaseAdapterContext(inputName,eventHandler))
				//BufferedWriter file = springApplicationContext.newWriter()
				//xml.bind(outputDatabaseAdapterContext(eventHandler)).writeTo( file )
			}
		}
		
	}

	def outputDatabaseAdapterContext = {
		String inputName, EventHandler eventHandler ->
		def outputChannel = inputName + 'OutputChannel'
		def transformerBean = outputChannel + "TransformerBean"
		def databaseChannel = inputName + 'DatabaseChannel'
		def dataSourceBeanID = eventHandler.name + "DataSource"
		def dataSource = "org.springframework.jdbc.datasource.DriverManagerDataSource"
		def driver = eventHandler.output.adapter.dataSource.driver
		def url = eventHandler.output.adapter.dataSource.url
		def username = eventHandler.output.adapter.dataSource.username
		def password = eventHandler.output.adapter.dataSource.password
		def request = eventHandler.output.adapter.request
		def persitSpelSource = eventHandler.name + 'PersitSpelSource'
		def clos = {
	
			"int:transformer"(id:"transformer-"+outputChannel+"-id", "input-channel":outputChannel, "output-channel":databaseChannel, ref:transformerBean, "method":"transform"){ }
			
			"bean"(id:transformerBean, class:"org.olabdynamics.compose.tools.code.ApplicationToObjectTransformer"){
			}
	
			"bean"(id:dataSourceBeanID, class:dataSource){
				"property"(name:"driverClassName", value:driver){ }
				"property"(name:"url", value:url){ }
				"property"(name:"username", value:username){ }
				"property"(name:"password", value:password){ }
			}
	
			"int:channel"(id:databaseChannel){ }
			
			"int-jdbc:outbound-channel-adapter"(id:"outbound-channel-adapter-"+outputChannel+"-id", "data-source":dataSourceBeanID, channel:databaseChannel, query:request, "sql-parameter-source-factory":persitSpelSource){ }
			
			"bean"(id:persitSpelSource, class:"org.springframework.integration.jdbc.ExpressionEvaluatingSqlParameterSourceFactory"){
				"property"(name:"parameterExpressions"){
					"map"(){
						"entry"(key:"result", value:"payload"){  }	// ! result et payload à changer
					}
				}
			}			   			
		}
	}
	
	def generateAggregator(Aggregator aggregator){
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		//BufferedWriter file = springApplicationContext.newWriter()
		//xml.bind(aggregatorContext(aggregator)).writeTo( file )
		return xml.bind(aggregatorContext(aggregator))
	}

	def aggregatorContext = {
		Aggregator aggregator ->
		def aggregatorName = ""
		aggregator.applications.each{
			aggregatorName = aggregatorName + it.name
		}
		def inputChannel = aggregatorName + "AggregatorInput"
		def outputChannel = aggregatorName + "AggregatorOutput"
		def outputTransformerChannel = aggregatorName + "AggregatorOutputTransformer"
		def size = "size()=="  + aggregator.applications.size()
		def releaseExpression = size + " AND " + "([0].payload instanceof T(org.olabdynamics.compose.Application) AND [0].payload.state==T(org.olabdynamics.compose.State).TERMINATED)"
		def transformerExpression = "(payload[0] instanceof T(org.olabdynamics.compose.Application) AND payload[0].name=='" + aggregatorName + "') ? payload[0] : payload[1]"
		def clos = {
	
			"int:aggregator"(id:"aggregator-"+inputChannel+"-id", "input-channel":inputChannel, "output-channel": outputChannel, "correlation-strategy-expression":"headers['messageID']", "release-strategy-expression":releaseExpression){
			}
		    
			"int:transformer"(id:"transformer-"+outputChannel+"-id", "input-channel":outputChannel, "output-channel":outputTransformerChannel, expression:transformerExpression){	} 
		
			"int:channel"(id:outputTransformerChannel){ }
		}
	}
	
	def generateRouter(Instruction instruction){
		def EventHandler eventHandler = instruction.springBean
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		if(eventHandler.input != null){
			if(eventHandler.input.adapter instanceof HttpAdapter){
				def inputName = instruction.variable
				return xml.bind(routerContext(inputName))
			}
		} 
	}
	
	def routerContext = {
		String inputName ->
		def inputChannel = inputName + 'Channel'
		def clos = {
	
			"task:executor"(id:"taskExecutor", "pool-size":"20", "queue-capacity":"20", "rejection-policy":"CALLER_RUNS"){ }
			
			"int:recipient-list-router"(id:"router-"+inputChannel+"-id", "input-channel":inputChannel){
				"int:recipient"(channel:inputChannel + "Route1"){ }
				"int:recipient"(channel:inputChannel + "Route2"){ }
			}
			
			"int:channel"(id:inputChannel + "Route1"){
				"int:dispatcher"("task-executor":"taskExecutor"){ }
			}
			
			"int:channel"(id:inputChannel + "Route2"){
				"int:dispatcher"("task-executor":"taskExecutor"){ }
			}
		}
	}
}
