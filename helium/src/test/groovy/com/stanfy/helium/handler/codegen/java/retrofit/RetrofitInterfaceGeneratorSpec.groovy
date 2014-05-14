package com.stanfy.helium.handler.codegen.java.retrofit

import com.stanfy.helium.DefaultType
import com.stanfy.helium.dsl.ProjectDsl
import spock.lang.Specification

/**
 * Spec for RetrofitInterfaceGenerator.
 */
class RetrofitInterfaceGeneratorSpec extends Specification {

  ProjectDsl project

  RetrofitInterfaceGenerator gen
  RetrofitGeneratorOptions options
  File output

  def setup() {
    output = File.createTempDir()
    options = RetrofitGeneratorOptions.defaultOptions("test.api")
    gen = new RetrofitInterfaceGenerator(output, options)

    project = new ProjectDsl()
    project.type 'int32' spec { }
    project.type 'AMessage' message { }
    project.type 'BMessage' message { }
    project.service {
      name "A"
      location "http://www.stanfy.com"

      get "/get/void" spec { }
      post "/post/complex/@id" spec {
        name "Post something complex"
        parameters {
          a 'int32'
        }
        body 'AMessage'
        response 'BMessage'
      }

      delete "/example" spec {
        name "Delete stuff"
      }

    }
    project.service {
      name "B"
    }
  }

  def "should generate interfaces"() {
    when:
    gen.handle(project)

    then:
    new File("$output/test/api/A.java").exists()
    new File("$output/test/api/B.java").exists()
  }

  def "should write default location"() {
    when:
    gen.handle(project)
    def text = new File("$output/test/api/A.java").text

    then:
    text.contains("String DEFAULT_URL = \"http://www.stanfy.com\"")
  }

  def "should emit imports"() {
    when:
    options.entitiesPackage = "another"
    gen.handle(project)
    def text = new File("$output/test/api/A.java").text

    then:
    text.contains("import another.AMessage;")
    text.contains("import another.BMessage;")
  }

  def "should write methods"() {
    when:
    gen.handle(project)
    def text = new File("$output/test/api/A.java").text

    then:
    text.contains('@GET("/get/void")\n')
    text.contains('void getGetVoid(ResponseCallback callback);\n')
  }

  def "should write different parameters"() {
    when:
    gen.handle(project)
    def text = new File("$output/test/api/A.java").text

    then:
    text.contains('@POST("/post/complex/{id}")\n')
    text.contains(
        'BMessage postSomethingComplex(@Path("id") String id, @Query("a") int a, @Body AMessage body);\n'
    )
  }

  def "can use method names"() {
    when:
    gen.handle(project)
    def text = new File("$output/test/api/A.java").text

    then:
    text.contains('@DELETE("/example")\n')
    text.contains('void deleteStuff(ResponseCallback callback)')
  }

}