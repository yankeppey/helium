package com.stanfy.helium.gradle

import com.stanfy.helium.gradle.tasks.GenerateJavaConstantsTask
import com.stanfy.helium.gradle.tasks.GenerateJavaEntitiesTask
import com.stanfy.helium.gradle.tasks.GenerateJsonSchemaTask
import com.stanfy.helium.gradle.tasks.GenerateObjcEntitiesTask
import com.stanfy.helium.gradle.tasks.GenerateRetrofitTask
import com.stanfy.helium.handler.codegen.java.constants.ConstantNameConverter
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import javax.lang.model.element.Modifier

import static com.stanfy.helium.gradle.HeliumPluginSpec.generateSpec

/**
 * Tests for HeliumExtension.
 */
class HeliumPluginSourceGenSpec extends Specification {

  Project project

  def setup() {
    project = ProjectBuilder.builder().build()
    project.apply plugin: "helium"
    project.helium {
      specification generateSpec("s1")
    }
  }

  private void createTasks() {
    (project.plugins.withType(HeliumPlugin).collect() as List)[0].createTasks(project)
  }

  def "source generation should support entities generator"() {
    given:

    project.helium {
      File outputDir = project.file("../lll")
      sourceGen {
        entities {
          output outputDir
          options {
            packageName = "some.package"
            prettifyNames = true
            fieldModifiers = [Modifier.FINAL] as Set
          }
        }
      }
    }

    createTasks()

    def task = project.tasks['generateEntities']

    expect:
    task != null
    task.output == project.file("../lll")
    task.options.packageName == "some.package"
    task.options.prettifyNames
    task.options.fieldModifiers == [Modifier.FINAL] as Set
  }

  def "source generation should support constants generator"() {
    given:
    project.helium {
      File outputDir = project.file("../ccc")
      sourceGen {
        constants {
          output outputDir
          options {
            packageName = "some.consts"
            nameConverter = { "COLUMN_${it.canonicalName.toUpperCase(Locale.US)}" } as ConstantNameConverter
          }
        }
      }
    }

    createTasks()

    def task = project.tasks['generateConstants']

    expect:
    task != null
    task.output == project.file("../ccc")
    task.options.packageName == "some.consts"
    task.options.nameConverter.class.name.contains("Proxy")
  }

  def "source generation should support retrofit"() {
    given:
    project.helium {
      File outputDir = project.file("../rrr")
      sourceGen {
        retrofit {
          output outputDir
          options {
            packageName = "example"
            entitiesPackage = "another.pkg"
            prettifyNames = true
          }
        }
      }
    }

    createTasks()

    def task = project.tasks["generateRetrofit"]

    expect:
    task != null
    task.output == project.file("../rrr")
    task.options.packageName == "example"
    task.options.entitiesPackage == "another.pkg"
    task.options.prettifyNames
  }

  def "source generation tasks should be accessible"() {
    given:
    project.helium {
      sourceGen {
        entities {
          options {
            packageName = "p1"
          }
        }
        constants {
          options {
            packageName = "p2"
          }
        }
        retrofit {
          options {
            packageName = "p1"
          }
        }
      }
    }

    createTasks()

    expect:
    project.helium.sourceGen.entities instanceof GenerateJavaEntitiesTask
    project.helium.sourceGen.entities.options.packageName == 'p1'
    project.helium.sourceGen.entities.output == new File(project.buildDir, "generated/source/helium/entities/s1")
    project.helium.sourceGen.constants instanceof GenerateJavaConstantsTask
    project.helium.sourceGen.constants.options.packageName == 'p2'
    project.helium.sourceGen.constants.output == new File(project.buildDir, "generated/source/helium/constants/s1")
    project.helium.sourceGen.retrofit instanceof GenerateRetrofitTask
    project.helium.sourceGen.retrofit.options.packageName == 'p1'
    project.helium.sourceGen.retrofit.output == new File(project.buildDir, "generated/source/helium/retrofit/s1")
  }

  def "source generation tasks are created per specification"() {
    given:
    project.helium {
      // 2nd spec
      specification(generateSpec("s-foo-2.api")) {
        sourceGen {
          entities {
            options {
              packageName = "p2"
            }
          }
        }
      }
      sourceGen {
        entities {
          options {
            packageName = "p1"
          }
        }
      }
    }
    createTasks()

    expect:
    project.helium.specifications.size() == 2
    project.helium.sourceGen('s1').entities instanceof GenerateJavaEntitiesTask
    project.helium.sourceGen('s1').entities.options.packageName == 'p1'
    project.helium.sourceGen('sFoo2').entities instanceof GenerateJavaEntitiesTask
    project.helium.sourceGen('sFoo2').entities.options.packageName == 'p2'
  }

  def "default sourceGen property is not available if there are multiple specifications"() {
    when:
    project.helium {
      specification generateSpec("s2.api")
      sourceGen {
        entities {
          options {
            packageName = "p1"
          }
        }
      }
    }
    def task = project.helium.sourceGen.entities

    then:
    def e = thrown(GradleException)
    e.message.contains("multiple specifications")
  }

  def "supports objc description"() {
    when:
    project.helium {
      sourceGen {
        objc {
          output = new File("test")
          options {
            prefix = 'TT'
          }
        }
      }
    }
    createTasks()

    then:
    project.helium.sourceGen.objc instanceof GenerateObjcEntitiesTask
    project.helium.sourceGen.objc.output == new File("test")
    project.helium.sourceGen.objc.options.prefix == 'TT'
  }

  def "it's possible to add more tasks for a spec later"() {
    when:
    project.helium {
      sourceGen {
        objc {
          output = new File("objc")
          options {
            prefix = 'TT'
          }
        }
      }
      sourceGen {
        entities {
          output = new File("entities")
        }
      }
      specification generateSpec("anotherSpec.api"), {
        sourceGen {
          constants { }
        }
      }
      specification generateSpec("anotherSpec.api"), {
        sourceGen {
          retrofit { }
        }
      }
    }
    createTasks()

    then:
    project.helium.sourceGen("anotherSpec").constants != null
    project.helium.sourceGen("anotherSpec").retrofit != null
    project.helium.sourceGen("anotherSpec").objc != null
    project.helium.sourceGen("anotherSpec").entities != null
    project.helium.sourceGen("s1").objc != null
    project.helium.sourceGen("s1").entities != null
  }

  def "json schema generation task should be accessible by specification name"() {
    given:
    project.helium {
      specification new File("asdf/Testspec1") {}
      specification new File("asdf/Testspec2") {}

      sourceGen {
        jsonSchema{ }
      }
    }

    createTasks()

    expect:
    project.helium.sourceGen('s1').jsonSchema instanceof GenerateJsonSchemaTask
    project.helium.sourceGen('s1').jsonSchema.options != null
    project.helium.sourceGen('Testspec1').jsonSchema instanceof GenerateJsonSchemaTask
    project.helium.sourceGen('Testspec2').jsonSchema instanceof GenerateJsonSchemaTask
    project.tasks.generateJsonSchema != null
    project.tasks.generateJsonSchemaTestspec1 != null
    project.tasks.generateJsonSchemaTestspec2 != null
  }

  def "source generation should support json scheme generator"() {
    given:
    project.helium {
      File outputDir = project.file("../jjj")

      specification(generateSpec("s2")) {
        sourceGen {
          jsonSchema {
            output outputDir
          }
        }
      }
    }

    createTasks()

    def task = project.tasks['generateJsonSchemaS2']

    expect:
    task != null
    task.output == project.file("../jjj")
    task.options != null
  }

  def "multiple spec trigger tasks should be created for source gen"() {
    given:
    project.helium {
      File outputDir = project.file("../jjj")

      specification(generateSpec("s2"))
      sourceGen {
        retrofit { }
        entities { }
      }
    }

    createTasks()

    expect:
    project.tasks["generateRetrofitS1"]
    project.tasks["generateRetrofitS2"]
    project.tasks["generateRetrofit"]
    project.tasks["generateEntitiesS1"]
    project.tasks["generateEntitiesS2"]
    project.tasks["generateEntities"]
  }

}
