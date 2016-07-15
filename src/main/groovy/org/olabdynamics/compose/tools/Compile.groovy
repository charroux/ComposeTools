package org.olabdynamics.compose.tools

import org.olabdynamics.compose.tools.javacodegeneration.JavaCodeGenerator
import org.olabdynamics.compose.tools.visitor.ComposeCodeVisitor
import org.olabdynamics.compose.tools.visitor.MyClassLoader
import org.olabdynamics.compose.tools.xmlgenerator.XmlGenerator
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
		
		if(args.size() != 1){
			throw new Exception("A compose source file (.groovy extension) should be in ./compose/source, put the file name (with no path) as a argument")
		}
		
		def visitor = new ComposeCodeVisitor(context: context)
		def myCL = new MyClassLoader(visitor: visitor)
		
		//def uri = new URI("file:///C:/Users/Charroux_std/Documents/projet/ExecAndShare/Compose/ComposeTools/ComposeTools/src/main/compose/Code4.groovy")
		String pathToCode = "." + File.separator + "compose" + File.separator + "source" + File.separator + args[0]
		def script = myCL.parseClass(new GroovyCodeSource(new File(pathToCode)))
		
		//def xmlSpringContent = "./src/main/resources/xmlSpringContext.xml"
		def xmlSpringContent = "." + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "xmlSpringContext.xml"
		
		def xmlGenerator = new XmlGenerator(xmlSpringContent: xmlSpringContent, instructions: visitor.instructions, aggregators: visitor.aggregators)
		xmlGenerator.generate()
		
		def javaCodeGenerator = new JavaCodeGenerator(context: context, instructions: visitor.instructions)
		javaCodeGenerator.generate()
		
		
	}

}
