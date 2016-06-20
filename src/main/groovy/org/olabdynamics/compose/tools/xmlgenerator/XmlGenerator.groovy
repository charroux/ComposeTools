package org.olabdynamics.compose.tools.xmlgenerator

import groovy.util.logging.Slf4j;
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

@Slf4j
class XmlGenerator {
	
	def xmlSpringContent
	def instructions
	def aggregators

	def xmls = []
	
	void generate(){
		
		def xmlForEvents = generateComposeEventManagement()
		xmls.add(xmlForEvents)
		
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
			writer.writeLine '<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:int="http://www.springframework.org/schema/integration" xmlns:int-http="http://www.springframework.org/schema/integration/http" xmlns:int-groovy="http://www.springframework.org/schema/integration/groovy" xmlns:context="http://www.springframework.org/schema/context" xmlns:task="http://www.springframework.org/schema/task" xmlns:jdbc="http://www.springframework.org/schema/jdbc" xmlns:int-jdbc="http://www.springframework.org/schema/integration/jdbc" xmlns:int-script="http://www.springframework.org/schema/integration/scripting" xmlns:int-event="http://www.springframework.org/schema/integration/event" xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task.xsd http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration-4.2.xsd http://www.springframework.org/schema/integration/jdbc http://www.springframework.org/schema/integration/jdbc/spring-integration-jdbc-4.2.xsd http://www.springframework.org/schema/jdbc http://www.springframework.org/schema/jdbc/spring-jdbc-4.2.xsd http://www.springframework.org/schema/integration/http http://www.springframework.org/schema/integration/http/spring-integration-http-4.2.xsd http://www.springframework.org/schema/integration/groovy http://www.springframework.org/schema/integration/groovy/spring-integration-groovy-4.2.xsd http://www.springframework.org/schema/integration/scripting http://www.springframework.org/schema/integration/scripting/spring-integration-scripting-4.2.xsd http://www.springframework.org/schema/integration/event http://www.springframework.org/schema/integration/event/spring-integration-event-4.2.xsd">' 
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
				
				log.info "application to aggregate: " + it
				
				// connect the aggregator input
				
				def applicationName = it.name + 'Output'	// it.name == code1 for compute code1
									
				log.info "application output channel: " + applicationName
				
				def transformer = beans."*".find { node->	// find a transformer after a compute
					node.name()=="int:transformer" && node.@"input-channel"==applicationName
				}
				
				log.info "transformer after the compute: " + transformer
				
				def transformerOutput = transformer.@"output-channel"
				
				def router = beans."*".find { node->	// find a router after a compute transformer
					node.name()=="int:recipient-list-router" && node.@"input-channel"==transformerOutput
				}
				
				log.info "router after the transformer: " + router
				
				def routeChannelName
				def route
				Iterator routes = router.iterator()
				while(routes.hasNext()){
					route = routes.next()
					routeChannelName = route.@"channel"
					if(routeChannelName.contains("ComposeEvent") == false){
						route.@"channel" = aggregator.@"input-channel"	// connect the route output to the aggregator input
					}
				}
				
				log.info "router output connected to the aggregator input: " + router
				
				def routeChannelAggregator = beans."*".find { node->	
					node.name()=="int:channel" && node.@"id"==aggregator.@"input-channel"
				}
				
				log.info "Channel with id = aggregator input(" + aggregator.@"input-channel" + "): " + routeChannelAggregator
				
				if(routeChannelAggregator == null){
				
					def routeChannel = beans."*".find { node->	// find a channel with the router's name
						node.name()=="int:channel" && node.@"id"==routeChannelName
					}
					
					log.info "Channel with id = aggregator input(" + routeChannelName + "): " + routeChannel
					
					routeChannel.@"id" = aggregator.@"input-channel"
					
					log.info "Channel changed with id = aggregator input(" + routeChannelName + "): " + routeChannel
	
				} else {
				
					log.info "Channel deleted: " + routeChannelAggregator
					
					def parent = routeChannelAggregator.parent()
					parent.remove(routeChannelAggregator)
				}
					
			
				// connect the aggregator output
				
				applicationName = applicationName + 'Channel'
				
				log.info "Aggregator output must be connected to: " + applicationName
				
				def applicationTransformerNode = beans."*".find { node->	// find the instruction using code1.result
					node.@"channel"==applicationName || (node.@"input-channel"==applicationName && node.name()!="int:recipient-list-router")
				}
				
				log.info "Channel to be connected: " + applicationTransformerNode				
				
				if(applicationTransformerNode != null){
							
					def aggregratorTransformer = beans."*".find { node->	// find the transformer after the aggregator
						node.name()=="int:transformer" && node.@"input-channel"==aggregator.@"output-channel"
					}
					
					log.info "Transformer after the aggregator: " + aggregratorTransformer
					
					applicationTransformerNode.@"input-channel" = aggregratorTransformer.@"output-channel"	// connect the aggregator output
					
					log.info "Aggregator changed: " + applicationTransformerNode
					
	
				}
				
						
			}
			
		}
		
		instructions.each {
			if(it.springBean instanceof EventHandler){				
				if(it.hashCode() in hashcodes){
					def inputName = it.variable + 'Channel'
					def router = beans."*".find { node->	// find a
						node.name()=="int:recipient-list-router" && node.@"input-channel"==inputName
					}
					if(router != null){	
						def routeChannelName
						Iterator routes = router.iterator()
						while(routes.hasNext()){
							routeChannelName = routes.next().@"channel"
							def serviceActivator = beans."*".find { node->	// find a transformer after a compute
								node.name()=="int:service-activator" && node.@"input-channel"==router.@"input-channel"
							}
							if(serviceActivator != null){
								serviceActivator.@"input-channel" = routeChannelName
							}
							
						}
					}
				}
			}
		}
		
		
		
		def springContexteAsText = XmlUtil.serialize(beans)
		
		//new File('essai.xml').withWriter('utf-8') { writer ->
		new File(xmlSpringContent).withWriter('utf-8') { writer ->
			writer.writeLine springContexteAsText
		}
		
		
	}
	
	def composeEventManagementContext = {
		def clos = {
			
			"int:publish-subscribe-channel"(id:"composeEventChannel"){  }
			
			"int-event:outbound-channel-adapter"(channel:"composeEventChannel"){  }
			
			"int:logging-channel-adapter"(channel:"composeEventChannel", level:"INFO"){  }
			
			"int-event:inbound-channel-adapter"(channel:"springContextEventChannel", "event-types":"org.springframework.context.event.ContextRefreshedEvent"){  }
					
			"int:transformer"("input-channel":"springContextEventChannel", "output-channel":"composeEventChannel"){
				"int-script:script"(lang:"groovy", location:"#{createReInitializedServiceEvent.input.adapter.file}"){  }
			}
				   
		}
	}
	
	def generateComposeEventManagement(){
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		return xml.bind(composeEventManagementContext())
	}
	
	def localApplicationContext = {
		String codeName, String inputName, String expression ->
		def id = codeName + 'Channel'
		def inputChannel = inputName + 'Channel'
		def outputServiceChannel = codeName + "Output"
		def outputChannel = codeName + 'OutputChannel'
		def outputRouterChannel = codeName + 'OutputRouteChannel'
		def exp = expression
		def transformerBean = codeName + "TransformerBean"
		def transformerRef = codeName
		def clos = {
	
				"int:service-activator"(id:"service-activator-"+id+"-id", "input-channel":inputChannel, "output-channel":outputServiceChannel, expression:exp){}
				
				"int:transformer"(id:"transformer-"+outputServiceChannel+"-id", "input-channel":outputServiceChannel, "output-channel":outputChannel, ref:transformerBean, "method":"transform"){ }
				
				"bean"(id:transformerBean, class:"org.olabdynamics.compose.tools.code.ObjectToApplicationTransformer"){
					"property"(name:"application", ref:transformerRef){ }
				}
				
				/*"int:publish-subscribe-channel"(id:outputChannel){  }
				
				"int:transformer"("input-channel":outputChannel, "output-channel":"composeEventChannel"){
					"int-script:script"(lang:"groovy", location:"#{createServiceCallReturnEvent.input.adapter.file}"){  }
				}*/
				
				"int:recipient-list-router"(id:"router-"+ id + "-event-id", "input-channel":outputChannel){
					"int:recipient"(channel:outputRouterChannel){  }
					"int:recipient"(channel:codeName + "ComposeEventRoute"){  }
				}
				
				"int:channel"(id:outputRouterChannel){
				}
				
				"int:channel"(id:codeName + "ComposeEventRoute"){
				}
				
				"int:transformer"("input-channel":codeName + "ComposeEventRoute", "output-channel":"composeEventChannel"){
					"int-script:script"(lang:"groovy", location:"#{createServiceCallReturnEvent.input.adapter.file}"){  }
				}
		
		}
	}
	
	def generateApplication(Instruction instruction){
		//def fileName = 'localApplicationContext.xml'
		def inputName = instruction.with
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
				
/*			"int:recipient-list-router"("input-channel":"inputRouterChannel"){
				"int:recipient"(channel:"executorChannel"){  }
				"int:recipient"(channel:"inputComposeEventChannel"){  }
			}
				
			"int:transformer"("input-channel":"inputComposeEventChannel", "output-channel":"composeEventChannel"){
				"int-script:script"(lang:"groovy", location:"#{createIncomingMessageEvent.input.adapter.file}"){  }
			}*/
		   		
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

	def releaseExpression(String expression, def applicationNames){
		int i=0
		def releaseExpression = ""
		Iterator names = applicationNames.iterator()
		def name
		int index
		while(names.hasNext()){
			name = names.next()
			index = expression.indexOf(name) + name.length() + 1
			expression = expression.substring(index)
			expression = expression.trim()
			if(expression.startsWith("terminates")){
				releaseExpression = releaseExpression + " ([" + i + "].payload instanceof T(org.olabdynamics.compose.Application) AND [" + i + "].payload.state==T(org.olabdynamics.compose.State).TERMINATED) "
				i++
				index = expression.indexOf("terminates") + "terminates".length() + 1
				if(index >= expression.length()){
					return releaseExpression
				}
				expression = expression.substring(index)
				expression = expression.trim()				
				if(expression.startsWith("and")){
					releaseExpression = releaseExpression + " and "
					index = expression.indexOf("and") + "and".length() + 1
					expression = expression.substring(index)
					expression = expression.trim()
				} else if(expression.startsWith("or")){
					releaseExpression = releaseExpression + " or "
					index = expression.indexOf("or") + "or".length() + 1
					expression = expression.substring(index)
					expression = expression.trim()
				}
				
			} 
			
		}
		return releaseExpression
	}
	
	def aggregatorContext = {
		Aggregator aggregator ->
		def aggregatorName = ""
		def applicationNames = []
		aggregator.applications.each{
			applicationNames.add(it.name)
			aggregatorName = aggregatorName + it.name
		}
		
		def inputChannel = aggregatorName + "AggregatorInput"
		def outputChannel = aggregatorName + "AggregatorOutput"
		def outputTransformerChannel = aggregatorName + "AggregatorOutputTransformer"
		def releaseExpression = this.releaseExpression(aggregator.releaseExpression, applicationNames)
		def size = "size()=="  + aggregator.applications.size()
		releaseExpression = size + " and " + "(" + releaseExpression + ")"
		//def releaseExpression = size + " AND " + "([0].payload instanceof T(org.olabdynamics.compose.Application) AND [0].payload.state==T(org.olabdynamics.compose.State).TERMINATED)"
		def nextIntruction = aggregator.nextInstruction.variable
		def transformerExpression = "(payload[0] instanceof T(org.olabdynamics.compose.Application) AND payload[0].name=='" + nextIntruction + "') ? payload[0] : payload[1]"
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
	
			"int:recipient-list-router"(id:"router-"+inputChannel+"-id", "input-channel":inputChannel){
				"int:recipient"(channel:inputChannel + "Route1"){ }
				"int:recipient"(channel:inputChannel + "Route2"){ }
				"int:recipient"(channel:inputChannel + "ComposeEventRoute"){ }
			}
			
			"int:channel"(id:inputChannel + "Route1"){
			}
			
			"int:channel"(id:inputChannel + "Route2"){
			}
			
			"int:channel"(id:inputChannel + "ComposeEventRoute"){
			}
			
			"int:transformer"("input-channel":inputChannel + "ComposeEventRoute", "output-channel":"composeEventChannel"){
				"int-script:script"(lang:"groovy", location:"#{createIncomingMessageEvent.input.adapter.file}"){  }
			}
		}
	}
}
