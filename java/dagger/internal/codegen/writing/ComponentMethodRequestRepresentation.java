/*
 * Copyright (C) 2017 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.internal.codegen.writing;

import static androidx.room.compiler.processing.compat.XConverters.toJavac;
import static com.google.common.base.Preconditions.checkNotNull;
import static dagger.internal.codegen.xprocessing.XElements.getSimpleName;

import androidx.room.compiler.processing.XProcessingEnv;
import com.squareup.javapoet.CodeBlock;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import dagger.internal.codegen.binding.ComponentDescriptor.ComponentMethodDescriptor;
import dagger.internal.codegen.langmodel.DaggerTypes;
import javax.lang.model.type.TypeMirror;

/**
 * A binding expression that implements and uses a component method.
 *
 * <p>Dependents of this binding expression will just call the component method.
 */
final class ComponentMethodRequestRepresentation extends MethodRequestRepresentation {
  private final RequestRepresentation wrappedRequestRepresentation;
  private final ComponentImplementation componentImplementation;
  private final ComponentMethodDescriptor componentMethod;
  private final XProcessingEnv processingEnv;

  @AssistedInject
  ComponentMethodRequestRepresentation(
      @Assisted RequestRepresentation wrappedRequestRepresentation,
      @Assisted ComponentMethodDescriptor componentMethod,
      ComponentImplementation componentImplementation,
      XProcessingEnv processingEnv,
      DaggerTypes types) {
    super(componentImplementation.getComponentShard(), types);
    this.wrappedRequestRepresentation = checkNotNull(wrappedRequestRepresentation);
    this.componentMethod = checkNotNull(componentMethod);
    this.componentImplementation = componentImplementation;
    this.processingEnv = processingEnv;
  }

  @Override
  protected CodeBlock getComponentMethodImplementation(
      ComponentMethodDescriptor componentMethod, ComponentImplementation component) {
    // There could be several methods on the component for the same request key and kind.
    // Only one should use the BindingMethodImplementation; the others can delegate that one.
    // Separately, the method might be defined on a supertype that is also a supertype of some
    // parent component. In that case, the same ComponentMethodDescriptor will be used to add a CMBE
    // for the parent and the child. Only the parent's should use the BindingMethodImplementation;
    // the child's can delegate to the parent. So use methodImplementation.body() only if
    // componentName equals the component for this instance.
    return componentMethod.equals(this.componentMethod) && component.equals(componentImplementation)
        ? CodeBlock.of(
            "return $L;",
            wrappedRequestRepresentation
                .getDependencyExpressionForComponentMethod(componentMethod, componentImplementation)
                .codeBlock())
        : super.getComponentMethodImplementation(componentMethod, component);
  }

  @Override
  protected CodeBlock methodCall() {
    return CodeBlock.of("$N()", getSimpleName(componentMethod.methodElement()));
  }

  @Override
  protected TypeMirror returnType() {
    return toJavac(componentMethod.resolvedReturnType(processingEnv));
  }

  @AssistedFactory
  static interface Factory {
    ComponentMethodRequestRepresentation create(
        RequestRepresentation wrappedRequestRepresentation,
        ComponentMethodDescriptor componentMethod);
  }
}