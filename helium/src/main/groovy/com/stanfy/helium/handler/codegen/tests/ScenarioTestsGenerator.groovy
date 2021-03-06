package com.stanfy.helium.handler.codegen.tests

import com.squareup.javawriter.JavaWriter
import com.stanfy.helium.DefaultType
import com.stanfy.helium.dsl.ProjectDsl
import com.stanfy.helium.dsl.scenario.ScenarioDelegate
import com.stanfy.helium.dsl.scenario.ScenarioInvoker
import com.stanfy.helium.handler.codegen.internal.UniqueName
import com.stanfy.helium.model.Project
import com.stanfy.helium.model.Service
import com.stanfy.helium.model.tests.Scenario
import groovy.transform.CompileStatic

import javax.lang.model.element.Modifier

import static com.squareup.javawriter.JavaWriter.stringLiteral

/**
 * Generator for scenario tests.
 */
@CompileStatic
public class ScenarioTestsGenerator extends BaseUnitTestsGenerator {

  /** Scenarios file. */
  private final File scenariosFile;

  public ScenarioTestsGenerator(final File scenariosFile, final File srcOutput) {
    this(scenariosFile, srcOutput, null);
  }
  public ScenarioTestsGenerator(final File scenariosFile, final File srcOutput, final File resourcesOutput) {
    this(scenariosFile, srcOutput, resourcesOutput, null);
  }
  public ScenarioTestsGenerator(final File scenariosFile, final File srcOutput, final File resourcesOutput, final String packageName) {
    super(srcOutput, resourcesOutput, packageName);
    if (!scenariosFile.exists()) {
      throw new IllegalArgumentException("Scenarios file does not exist")
    }

    this.scenariosFile = scenariosFile;
  }

  @Override
  protected String getClassName(final Service service) {
    return "${service.canonicalName}ScenariosTest"
  }

  @Override
  public void handle(final Project project) {
    // copy scenarios file
    copy(project)

    eachService(project, { Service service, JavaWriter writer ->
      service.testInfo.scenarios.each { Scenario scenario ->
        addTestMethod service, scenario, writer
      }
      return !service.testInfo.scenarios.empty
    } as BaseUnitTestsGenerator.ServiceHandler)

  }

  private static String replaceVarsInSpec(final File file, final Map<File, String> map, final Project project) {
    def text = file.getText("UTF-8")
    if (project instanceof ProjectDsl) {
      if (project.variablesBinding.hasVariable("baseDir")) {
        GroovyShell shell = new GroovyShell(project.variablesBinding)
        text = text.replaceAll(/include\s+(["'].+?["'])/) { fullLine, arg ->
          String path = shell.evaluate(arg as String) as String
          return "include \"\$baseDir/${map[new File(path)]}\""
        }
      }
    }
    return text
  }

  private void copy(final Project project) {
    def includeMap = [:]
    project.includedFiles.each { File includedSpec ->
      String name = UniqueName.from(includedSpec)
      includeMap[includedSpec] = name
    }
    includeMap.each { key, value ->
      File file = key as File
      String name = value as String
      new File(getResourcesPackageDir(), name).withWriter(UTF_8) { Writer out ->
        out << "// $file.absolutePath\n\n"
        out << replaceVarsInSpec(file, includeMap, project)
      }
    }
    specFile.withWriter(UTF_8) { Writer out ->
      DefaultType.values().each { DefaultType type ->
        try {
          project.types.byName(type.langName)
          out << "type '${type.langName}'\n"
        } catch (IllegalArgumentException ignored) {
          // no such type
        }
      }
      out << replaceVarsInSpec(scenariosFile, includeMap, project)
    }
  }

  @Override
  protected void startTest(final JavaWriter writer, final Service service, final Project project) throws IOException {
    super.startTest(writer, service, project)
    writer.emitField(Service.name, "service")
    writer.emitField(ScenarioDelegate.canonicalName, "proxy", EnumSet.of(Modifier.PRIVATE))

    writer.beginMethod(null, getClassName(service), PUBLIC)
    writer.emitStatement("super()")
    writer.emitStatement("this.proxy = new ${ScenarioDelegate.canonicalName}(service, createExecutor())")
    writer.endMethod()
    writer.emitEmptyLine()

    writer.beginMethod(Project.name, "loadDefaultTestSpec", PROTECTED)
    writer.emitStatement("${Project.name} project = super.loadDefaultTestSpec()")
    writer.emitStatement("this.service = project.serviceByName(%s)", stringLiteral(service.name))
    writer.emitStatement("return project")
    writer.endMethod()
    writer.emitEmptyLine()
  }

  private static void addTestMethod(final Service service, final Scenario scenario, final JavaWriter writer) {
    if (!scenario.action) {
      throw new IllegalStateException("Scenario '$scenario.name' has no action")
    }
    writer.emitAnnotation("Test")
    writer.beginMethod("void", scenario.canonicalName, Collections.<Modifier>singleton(Modifier.PUBLIC))

    writer.emitStatement("${Scenario.canonicalName} scenario = service.getTestInfo().scenarioByName(%s)",
        stringLiteral(scenario.name))
    writer.emitStatement("${ScenarioInvoker.canonicalName}.invokeScenario(proxy, scenario)")

    writer.endMethod()
    writer.emitEmptyLine()
  }

}
