package com.stanfy.helium.handler.codegen.tests;

import com.squareup.okhttp.RequestBody;
import com.stanfy.helium.entities.TypedEntity;
import com.stanfy.helium.model.Type;
import com.stanfy.helium.model.TypeResolver;

/**
 * Represents converter from {@link com.stanfy.helium.entities.TypedEntity}
 * and {@link com.squareup.okhttp.RequestBody}
 * that may check if it's applicable and can build RequestBody from given entity.
 *
 * @see com.stanfy.helium.handler.codegen.tests.HttpExecutor
 * @author Nikolay Soroka (Stanfy - http://www.stanfy.com)
 */
public interface RequestBodyBuilder {

  boolean canBuild(final Type bodyType);

  RequestBody build(final TypeResolver types, final TypedEntity entity, final String encoding);

}
