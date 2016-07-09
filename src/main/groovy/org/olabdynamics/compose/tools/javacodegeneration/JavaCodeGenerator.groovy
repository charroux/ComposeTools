package org.olabdynamics.compose.tools.javacodegeneration

import groovy.util.logging.Slf4j;

import org.olabdynamics.compose.EventHandler
import org.olabdynamics.compose.HttpAdapter
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource

import com.sun.codemodel.JBlock
import com.sun.codemodel.JCodeModel
import com.sun.codemodel.JDefinedClass
import com.sun.codemodel.ClassType
import com.sun.codemodel.JFieldVar
import com.sun.codemodel.JInvocation
import com.sun.codemodel.JMethod
import com.sun.codemodel.JMod
import com.sun.codemodel.JVar
import com.sun.codemodel.JExpr

@Slf4j
class JavaCodeGenerator {
	
	ApplicationContext context
	def instructions
	
	def generate(){
		
		log.info "generate Java Code"
		
		File outputDirectory = new File("./src/main/java/generated")
		File[] files = outputDirectory.listFiles();
		if(files!=null) { //some JVMs return null for empty dirs
			for(File f: files) {
				if(f.isDirectory()==false){
					f.delete();
				}
			}
		}
		
		outputDirectory = new File("./src/main/java/myservice")
		files = outputDirectory.listFiles();
		if(files!=null) { //some JVMs return null for empty dirs
			for(File f: files) {
				if(f.isDirectory()==false){
					f.delete();
				}
			}
		}
		
		instructions.each{
					
			if(it.instruction == 'receive'){
				
				if(it.springBean instanceof EventHandler && it.springBean.input.adapter instanceof HttpAdapter) {
					 
					// generate interface Gateway
					def interfaceName = it.springBean.name
					def springName = interfaceName.substring(0,1).toUpperCase() + interfaceName.substring(1)
					interfaceName = 'myservice.' + springName + "Gateway"
					JCodeModel codeModel = new JCodeModel();
					JDefinedClass gateWayInterface = codeModel._class(JMod.PUBLIC, interfaceName, ClassType.INTERFACE)
					JMethod method = gateWayInterface.method(JMod.PUBLIC, void.class, "method")
					JVar param = method.param(Object.class, "object")
					codeModel.build(new File("./src/main/java"))
					
					log.info interfaceName  + " generated"
					
					// generate Rest web service
					codeModel = new JCodeModel();
					def className = 'generated.' + springName + "RestWebService"
					JDefinedClass webService = codeModel._class(JMod.PUBLIC, className, ClassType.CLASS)
					webService.annotate(RestController.class)
					JFieldVar field = webService.field(JMod.PRIVATE, gateWayInterface, "gateway");
					field.annotate(Autowired.class)
					
					
					def methodName = it.variable
					method = webService.method(JMod.PUBLIC, void.class, methodName)
					method.annotate(codeModel.ref("org.springframework.web.bind.annotation.RequestMapping")).param("value", "/microserviceUrl").param("method", RequestMethod.PUT);
					param = method.param(Integer.class, "i")
					param.annotate(RequestBody.class)
					
					JInvocation gatewayInvocation = JExpr.invoke(field, "method")
					gatewayInvocation.arg(param)
					
					JBlock body = method.body();
					body.add(gatewayInvocation)
					
					codeModel.build(new File("./src/main/java"))
					
					log.info className  + " generated"
					
					// main spring boot
/*					codeModel = new JCodeModel();
					className = 'generated.' + "MainApplication"
					JDefinedClass mainClass = codeModel._class(JMod.PUBLIC, className, ClassType.CLASS)
					mainClass.annotate(SpringBootApplication.class)
					mainClass.annotate(ImportResource.class)*/
					
				}
			}
		}
		
	}

}
