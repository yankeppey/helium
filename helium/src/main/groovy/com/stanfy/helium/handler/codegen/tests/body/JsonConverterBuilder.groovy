package com.stanfy.helium.handler.codegen.tests.body

import com.squareup.okhttp.MediaType
import com.squareup.okhttp.RequestBody
import com.stanfy.helium.entities.TypedEntity
import com.stanfy.helium.handler.codegen.tests.RequestBodyBuilder
import com.stanfy.helium.handler.codegen.tests.Utils
import com.stanfy.helium.model.Type
import com.stanfy.helium.model.TypeResolver
import groovy.transform.PackageScope
import okio.BufferedSink

/**
 * Used as default body builder.
 *
 * @see BuilderFactory
 * @author Nikolay Soroka (Stanfy - http://www.stanfy.com)
 */
@PackageScope
class JsonConverterBuilder implements RequestBodyBuilder {
  @Override
  boolean canBuild(final Type bodyType) {
    return true // applicable to all
  }

  @Override
  RequestBody build(final TypeResolver types, final TypedEntity entity, String encoding) {
    return new RequestBody() {
      @Override
      public MediaType contentType() {
        return Utils.jsonType()
      }

      @Override
      public void writeTo(final BufferedSink sink) throws IOException {
        if (entity != null) {
          Writer out = new OutputStreamWriter(sink.outputStream(), encoding)
          Utils.writeEntityWithConverters(entity, out, types)
        }
        sink.close()
      }
    }
  }
}
