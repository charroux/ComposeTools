package org.olabdynamics.compose.tools

import org.olabdynamics.compose.tools.javacodegeneration.JavaCodeGenerator
import org.olabdynamics.compose.tools.visitor.ComposeCodeVisitor
import org.olabdynamics.compose.tools.visitor.MyClassLoader
import org.olabdynamics.compose.tools.xmlgenerator.XmlGenerator
import groovy.xml.XmlUtil
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component

@Component
class Compile implements CommandLineRunner{
	
	@Autowired
	ApplicationContext context

	@Override
	public void run(String... args) throws Exception {
		
		if(args.length != 2){
			throw new Exception("A compose source file (.groovy extension) should be in ./compose/source, put the file name (with no path) as the first argument, true or false for the Quality Of Service as a second argumeent")
		}
		
		def visitor = new ComposeCodeVisitor(context: context)
		def myCL = new MyClassLoader(visitor: visitor)
		
		String pathToCode = "." + File.separator + "compose" + File.separator + "source" + File.separator + args[0]
		def script = myCL.parseClass(new GroovyCodeSource(new File(pathToCode)))

		boolean QoS = Boolean.parseBoolean(args[1])
			
		if(QoS == true){
	
			def xmlSpringContent = "." + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "QoSSpringContext.xml"
			def xmlGenerator = new XmlGenerator(xmlSpringContent: xmlSpringContent, instructions: visitor.instructions, aggregators: visitor.aggregators, composeEvents: false)
			xmlGenerator.generate()
	
		} else {

			def xmlSpringContent = "." + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "xmlSpringContext.xml"
			def xmlGenerator = new XmlGenerator(xmlSpringContent: xmlSpringContent, instructions: visitor.instructions, aggregators: visitor.aggregators, composeEvents: true)
			xmlGenerator.generate()
			
			xmlSpringContent = "." + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "QoSSpringContext.xml"
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(xmlSpringContent)))
			bufferedWriter.writeLine('<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:int="http://www.springframework.org/schema/integration" xmlns:int-file="http://www.springframework.org/schema/integration/file" xmlns:int-http="http://www.springframework.org/schema/integration/http" xmlns:int-groovy="http://www.springframework.org/schema/integration/groovy" xmlns:context="http://www.springframework.org/schema/context" xmlns:task="http://www.springframework.org/schema/task" xmlns:jdbc="http://www.springframework.org/schema/jdbc" xmlns:int-jdbc="http://www.springframework.org/schema/integration/jdbc" xmlns:int-script="http://www.springframework.org/schema/integration/scripting" xmlns:int-event="http://www.springframework.org/schema/integration/event" xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task.xsd http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration-4.2.xsd http://www.springframework.org/schema/integration/file http://www.springframework.org/schema/integration/file/spring-integration-file-4.2.xsd http://www.springframework.org/schema/integration/jdbc http://www.springframework.org/schema/integration/jdbc/spring-integration-jdbc-4.2.xsd http://www.springframework.org/schema/jdbc http://www.springframework.org/schema/jdbc/spring-jdbc-4.2.xsd http://www.springframework.org/schema/integration/http http://www.springframework.org/schema/integration/http/spring-integration-http-4.2.xsd http://www.springframework.org/schema/integration/groovy http://www.springframework.org/schema/integration/groovy/spring-integration-groovy-4.2.xsd http://www.springframework.org/schema/integration/scripting http://www.springframework.org/schema/integration/scripting/spring-integration-scripting-4.2.xsd http://www.springframework.org/schema/integration/event http://www.springframework.org/schema/integration/event/spring-integration-event-4.2.xsd">')
			bufferedWriter.writeLine('</beans>')
			bufferedWriter.flush()
			bufferedWriter.close()
			
			// usefull to write line by line
			String xmlContext = new File(xmlSpringContent).text
			String springContexteAsText = XmlUtil.serialize(xmlContext)
			new File(xmlSpringContent).withWriter('utf-8') { writer ->
				writer.writeLine springContexteAsText
			}
			
		} 
		
		def javaCodeGenerator = new JavaCodeGenerator(context: context, instructions: visitor.instructions)
		javaCodeGenerator.generate()
		
		
	}

}
