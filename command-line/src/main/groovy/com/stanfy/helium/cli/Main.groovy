package com.stanfy.helium.cli

import com.stanfy.helium.Helium
import com.stanfy.helium.handler.Handler
import com.stanfy.helium.handler.codegen.java.entity.EntitiesGenerator
import com.stanfy.helium.handler.codegen.java.entity.EntitiesGeneratorOptions;

/**
 * Main entry point.
 */
class Main {

  private static final def HANDLERS = [
      "java-entities" : [
          description: "Generate Java entity classes",
          properties: [
              "package": "Package name for generated classes. Required."
          ],
          factory: { def options, File output ->
            EntitiesGeneratorOptions genOptions = EntitiesGeneratorOptions.defaultOptions(
                requiredProperty(options, "package")
            )
            return new EntitiesGenerator(output, genOptions)
          }
      ]
  ]

  private static final def CLI = new CliBuilder(usage: "helium [options] <spec>", header: "Options:")
  static {
    CLI.x("Do not include default types")
    CLI.H(args: 2, valueSeparator: '=', argName: 'property=value', "Set value of a property\n")
    CLI.o(longOpt: "output", args: 1, argName: 'dir', "Output directory")

    HANDLERS.each { name, definition ->
      String propsDescr = definition.properties.keySet().collect {
        "-H${it}=<value>:\n${definition.properties[it]}\n"
      }.inject("", {x, y -> x + y})
      CLI._(longOpt: name, "$definition.description\nUsed properties:\n$propsDescr\n")
    }
  }

  private static String requiredProperty(def options, String name) {
    String res = property(options, name)
    if (!res) {
      println "Property -H$name=<value> is required"
      System.exit(1)
    }
    return res
  }

  private static String property(def options, String name) {
    if (!options.Hs) {
      return null
    }
    def props = options.Hs as List
    for (int i = 0; i < props.size() / 2; i++) {
      if (name == props[i * 2]) {
        return props[i * 2 + 1]
      }
    }
    return null
  }

  static void main(final String[] args) {
    def options = CLI.parse(args)
    def specs = options.arguments()
    if (!options || !specs) {
      println CLI.usage()
      System.exit(1);
      return;
    }

    File output = options.o ? new File(options.o as String) : new File(".")

    specs.each { String fileName ->
      def file = new File(fileName)
      if (!file.exists()) {
        println "File $file does not exist"
        System.exit(1)
      }

      def h = new Helium()
      if (!options.x) {
        h.defaultTypes()
      }

      h.from(file)

      HANDLERS.each { name, definition ->
        if (options.hasOption(name)) {
          h.processBy(definition.factory(options, output) as Handler)
        }
      }
    }
  }

}