/*
 * Copyright (C) 2021 The Dagger Authors.
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

import static dagger.internal.codegen.base.Util.reentrantComputeIfAbsent;
import static dagger.internal.codegen.binding.BindingRequest.bindingRequest;
import static dagger.internal.codegen.javapoet.TypeNames.DOUBLE_CHECK;
import static dagger.internal.codegen.javapoet.TypeNames.SINGLE_CHECK;
import static dagger.internal.codegen.writing.DelegateRequestRepresentation.isBindsScopeStrongerThanDependencyScope;
import static dagger.internal.codegen.writing.MemberSelect.staticFactoryCreation;
import static dagger.spi.model.BindingKind.DELEGATE;
import static dagger.spi.model.BindingKind.MULTIBOUND_MAP;
import static dagger.spi.model.BindingKind.MULTIBOUND_SET;

import com.squareup.javapoet.CodeBlock;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import dagger.internal.codegen.binding.BindingGraph;
import dagger.internal.codegen.binding.BindingRequest;
import dagger.internal.codegen.binding.ComponentDescriptor.ComponentMethodDescriptor;
import dagger.internal.codegen.binding.ContributionBinding;
import dagger.internal.codegen.binding.FrameworkType;
import dagger.internal.codegen.binding.ProvisionBinding;
import dagger.internal.codegen.compileroption.CompilerOptions;
import dagger.internal.codegen.langmodel.DaggerTypes;
import dagger.internal.codegen.writing.ComponentImplementation.ShardImplementation;
import dagger.internal.codegen.writing.FrameworkFieldInitializer.FrameworkInstanceCreationExpression;
import dagger.spi.model.BindingKind;
import dagger.spi.model.RequestKind;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.TypeElement;

/**
 * A binding representation that wraps code generation methods that satisfy all kinds of request for
 * that binding.
 */
final class ProvisionBindingRepresentation implements BindingRepresentation {
  private final BindingGraph graph;
  private final boolean isFastInit;
  private final ProvisionBinding binding;
  private final ComponentImplementation componentImplementation;
  private final ComponentMethodRequestRepresentation.Factory
      componentMethodRequestRepresentationFactory;
  private final DelegateRequestRepresentation.Factory delegateRequestRepresentationFactory;
  private final DerivedFromFrameworkInstanceRequestRepresentation.Factory
      derivedFromFrameworkInstanceRequestRepresentationFactory;
  private final ImmediateFutureRequestRepresentation.Factory
      immediateFutureRequestRepresentationFactory;
  private final PrivateMethodRequestRepresentation.Factory
      privateMethodRequestRepresentationFactory;
  private final AssistedPrivateMethodRequestRepresentation.Factory
      assistedPrivateMethodRequestRepresentationFactory;
  private final ProducerNodeInstanceRequestRepresentation.Factory
      producerNodeInstanceRequestRepresentationFactory;
  private final ProviderInstanceRequestRepresentation.Factory
      providerInstanceRequestRepresentationFactory;
  private final UnscopedDirectInstanceRequestRepresentationFactory
      unscopedDirectInstanceRequestRepresentationFactory;
  private final ProducerFromProviderCreationExpression.Factory
      producerFromProviderCreationExpressionFactory;
  private final UnscopedFrameworkInstanceCreationExpressionFactory
      unscopedFrameworkInstanceCreationExpressionFactory;
  private final SwitchingProviders switchingProviders;
  private final Map<BindingRequest, RequestRepresentation> requestRepresentations = new HashMap<>();

  @AssistedInject
  ProvisionBindingRepresentation(
      @Assisted ProvisionBinding binding,
      SwitchingProviders switchingProviders,
      BindingGraph graph,
      ComponentImplementation componentImplementation,
      ComponentMethodRequestRepresentation.Factory componentMethodRequestRepresentationFactory,
      DelegateRequestRepresentation.Factory delegateRequestRepresentationFactory,
      DerivedFromFrameworkInstanceRequestRepresentation.Factory
          derivedFromFrameworkInstanceRequestRepresentationFactory,
      ImmediateFutureRequestRepresentation.Factory immediateFutureRequestRepresentationFactory,
      PrivateMethodRequestRepresentation.Factory privateMethodRequestRepresentationFactory,
      AssistedPrivateMethodRequestRepresentation.Factory
          assistedPrivateMethodRequestRepresentationFactory,
      ProducerNodeInstanceRequestRepresentation.Factory
          producerNodeInstanceRequestRepresentationFactory,
      ProviderInstanceRequestRepresentation.Factory providerInstanceRequestRepresentationFactory,
      UnscopedDirectInstanceRequestRepresentationFactory
          unscopedDirectInstanceRequestRepresentationFactory,
      ProducerFromProviderCreationExpression.Factory producerFromProviderCreationExpressionFactory,
      UnscopedFrameworkInstanceCreationExpressionFactory
          unscopedFrameworkInstanceCreationExpressionFactory,
      CompilerOptions compilerOptions,
      DaggerTypes types) {
    this.binding = binding;
    this.switchingProviders = switchingProviders;
    this.graph = graph;
    this.componentImplementation = componentImplementation;
    this.componentMethodRequestRepresentationFactory = componentMethodRequestRepresentationFactory;
    this.delegateRequestRepresentationFactory = delegateRequestRepresentationFactory;
    this.derivedFromFrameworkInstanceRequestRepresentationFactory =
        derivedFromFrameworkInstanceRequestRepresentationFactory;
    this.immediateFutureRequestRepresentationFactory = immediateFutureRequestRepresentationFactory;
    this.privateMethodRequestRepresentationFactory = privateMethodRequestRepresentationFactory;
    this.producerNodeInstanceRequestRepresentationFactory =
        producerNodeInstanceRequestRepresentationFactory;
    this.providerInstanceRequestRepresentationFactory =
        providerInstanceRequestRepresentationFactory;
    this.unscopedDirectInstanceRequestRepresentationFactory =
        unscopedDirectInstanceRequestRepresentationFactory;
    this.producerFromProviderCreationExpressionFactory =
        producerFromProviderCreationExpressionFactory;
    this.unscopedFrameworkInstanceCreationExpressionFactory =
        unscopedFrameworkInstanceCreationExpressionFactory;
    this.assistedPrivateMethodRequestRepresentationFactory =
        assistedPrivateMethodRequestRepresentationFactory;
    TypeElement rootComponent =
        componentImplementation.rootComponentImplementation().componentDescriptor().typeElement();
    this.isFastInit = compilerOptions.fastInit(rootComponent);
  }

  @Override
  public RequestRepresentation getRequestRepresentation(BindingRequest request) {
    return reentrantComputeIfAbsent(
        requestRepresentations, request, this::getRequestRepresentationUncached);
  }

  private RequestRepresentation getRequestRepresentationUncached(BindingRequest request) {
    switch (request.requestKind()) {
      case INSTANCE:
        return instanceRequestRepresentation();

      case PROVIDER:
        return providerRequestRepresentation();

      case LAZY:
      case PRODUCED:
      case PROVIDER_OF_LAZY:
        return derivedFromFrameworkInstanceRequestRepresentationFactory.create(
            request, FrameworkType.PROVIDER);

      case PRODUCER:
        return producerFromProviderRequestRepresentation();

      case FUTURE:
        return immediateFutureRequestRepresentationFactory.create(binding.key());

      case MEMBERS_INJECTION:
        throw new IllegalArgumentException();
    }

    throw new AssertionError();
  }

  /**
   * Returns a binding expression that uses a {@link javax.inject.Provider} for provision bindings.
   */
  private RequestRepresentation frameworkInstanceRequestRepresentation() {
    FrameworkInstanceCreationExpression frameworkInstanceCreationExpression =
        unscopedFrameworkInstanceCreationExpressionFactory.create(binding);

    if (isFastInit
        // Some creation expressions can opt out of using switching providers.
        && frameworkInstanceCreationExpression.useSwitchingProvider()) {
      // First try to get the instance expression via getRequestRepresentation(). However, if that
      // expression is a DerivedFromFrameworkInstanceRequestRepresentation (e.g. fooProvider.get()),
      // then we can't use it to create an instance within the SwitchingProvider since that would
      // cause a cycle. In such cases, we try to use the unscopedDirectInstanceRequestRepresentation
      // directly, or else fall back to default mode.
      BindingRequest instanceRequest = bindingRequest(binding.key(), RequestKind.INSTANCE);
      RequestRepresentation instanceExpression = getRequestRepresentation(instanceRequest);
      if (!(instanceExpression instanceof DerivedFromFrameworkInstanceRequestRepresentation)) {
        frameworkInstanceCreationExpression =
            switchingProviders.newFrameworkInstanceCreationExpression(binding, instanceExpression);
      } else {
        Optional<RequestRepresentation> unscopedInstanceExpression =
            unscopedDirectInstanceRequestRepresentationFactory.create(binding);
        if (unscopedInstanceExpression.isPresent()) {
          frameworkInstanceCreationExpression =
              switchingProviders.newFrameworkInstanceCreationExpression(
                  binding,
                  unscopedInstanceExpression.get().requiresMethodEncapsulation()
                      ? privateMethodRequestRepresentationFactory.create(
                          instanceRequest, binding, unscopedInstanceExpression.get())
                      : unscopedInstanceExpression.get());
        }
      }
    }

    // In default mode, we always use the static factory creation strategy. In fastInit mode, we
    // prefer to use a SwitchingProvider instead of static factories in order to reduce class
    // loading; however, we allow static factories that can reused across multiple bindings, e.g.
    // {@code MapFactory} or {@code SetFactory}.
    // TODO(bcorso): Consider merging the static factory creation logic into CreationExpressions?
    Optional<MemberSelect> staticMethod =
        useStaticFactoryCreation() ? staticFactoryCreation(binding) : Optional.empty();
    FrameworkInstanceSupplier frameworkInstanceSupplier =
        staticMethod.isPresent()
            ? staticMethod::get
            : new FrameworkFieldInitializer(
                componentImplementation,
                binding,
                binding.scope().isPresent()
                    ? scope(frameworkInstanceCreationExpression)
                    : frameworkInstanceCreationExpression);

    return providerInstanceRequestRepresentationFactory.create(binding, frameworkInstanceSupplier);
  }

  private FrameworkInstanceCreationExpression scope(FrameworkInstanceCreationExpression unscoped) {
    return () ->
        CodeBlock.of(
            "$T.provider($L)",
            binding.scope().get().isReusable() ? SINGLE_CHECK : DOUBLE_CHECK,
            unscoped.creationExpression());
  }

  /**
   * Returns a binding expression for {@link RequestKind#PROVIDER} requests.
   *
   * <p>{@code @Binds} bindings that don't {@linkplain #needsCaching(ContributionBinding) need to be
   * cached} can use a {@link DelegateRequestRepresentation}.
   *
   * <p>Otherwise, return a {@link FrameworkInstanceRequestRepresentation}.
   */
  private RequestRepresentation providerRequestRepresentation() {
    if (binding.kind().equals(DELEGATE) && !needsCaching()) {
      return delegateRequestRepresentationFactory.create(binding, RequestKind.PROVIDER);
    }
    return frameworkInstanceRequestRepresentation();
  }

  /**
   * Returns a binding expression that uses a {@link dagger.producers.Producer} field for a
   * provision binding.
   */
  private FrameworkInstanceRequestRepresentation producerFromProviderRequestRepresentation() {
    return producerNodeInstanceRequestRepresentationFactory.create(
        binding,
        new FrameworkFieldInitializer(
            componentImplementation,
            binding,
            producerFromProviderCreationExpressionFactory.create(binding)));
  }

  /** Returns a binding expression for {@link RequestKind#INSTANCE} requests. */
  private RequestRepresentation instanceRequestRepresentation() {
    Optional<RequestRepresentation> maybeDirectInstanceExpression =
        unscopedDirectInstanceRequestRepresentationFactory.create(binding);
    if (maybeDirectInstanceExpression.isPresent()) {
      RequestRepresentation directInstanceExpression = maybeDirectInstanceExpression.get();
      if (binding.kind() == BindingKind.ASSISTED_INJECTION) {
        BindingRequest request = bindingRequest(binding.key(), RequestKind.INSTANCE);
        return assistedPrivateMethodRequestRepresentationFactory.create(
            request, binding, directInstanceExpression);
      }

      boolean isDefaultModeAssistedFactory =
          binding.kind() == BindingKind.ASSISTED_FACTORY && !isFastInit;

      // If this is the case where we don't need to use Provider#get() because there's no caching
      // and it isn't a default mode assisted factory, we can try to use the direct expression,
      // possibly wrapped in a method if necessary (e.g. if it has dependencies).
      // Note: We choose not to use a direct expression for assisted factories in default mode
      // because they technically act more similar to a Provider than an instance, so we cache them
      // using a field in the component similar to Provider requests. This should also be the case
      // in FastInit, but it hasn't been implemented yet.
      if (!needsCaching() && !isDefaultModeAssistedFactory) {
        return directInstanceExpression.requiresMethodEncapsulation()
            ? wrapInMethod(RequestKind.INSTANCE, directInstanceExpression)
            : directInstanceExpression;
      }
    }
    return derivedFromFrameworkInstanceRequestRepresentationFactory.create(
        bindingRequest(binding.key(), RequestKind.INSTANCE), FrameworkType.PROVIDER);
  }

  /**
   * Returns {@code true} if the binding should use the static factory creation strategy.
   *
   * <p>In default mode, we always use the static factory creation strategy. In fastInit mode, we
   * prefer to use a SwitchingProvider instead of static factories in order to reduce class loading;
   * however, we allow static factories that can reused across multiple bindings, e.g. {@code
   * MapFactory} or {@code SetFactory}.
   */
  private boolean useStaticFactoryCreation() {
    return !isFastInit
        || binding.kind().equals(MULTIBOUND_MAP)
        || binding.kind().equals(MULTIBOUND_SET);
  }

  /**
   * Returns a binding expression that uses a given one as the body of a method that users call. If
   * a component provision method matches it, it will be the method implemented. If it does not
   * match a component provision method and the binding is modifiable, then a new public modifiable
   * binding method will be written. If the binding doesn't match a component method and is not
   * modifiable, then a new private method will be written.
   */
  RequestRepresentation wrapInMethod(
      RequestKind requestKind, RequestRepresentation bindingExpression) {
    // If we've already wrapped the expression, then use the delegate.
    if (bindingExpression instanceof MethodRequestRepresentation) {
      return bindingExpression;
    }

    BindingRequest request = bindingRequest(binding.key(), requestKind);
    Optional<ComponentMethodDescriptor> matchingComponentMethod =
        graph.componentDescriptor().firstMatchingComponentMethod(request);

    ShardImplementation shardImplementation = componentImplementation.shardImplementation(binding);

    // Consider the case of a request from a component method like:
    //
    //   DaggerMyComponent extends MyComponent {
    //     @Overrides
    //     Foo getFoo() {
    //       <FOO_BINDING_REQUEST>
    //     }
    //   }
    //
    // Normally, in this case we would return a ComponentMethodRequestRepresentation rather than a
    // PrivateMethodRequestRepresentation so that #getFoo() can inline the implementation rather
    // than
    // create an unnecessary private method and return that. However, with sharding we don't want to
    // inline the implementation because that would defeat some of the class pool savings if those
    // fields had to communicate across shards. Thus, when a key belongs to a separate shard use a
    // PrivateMethodRequestRepresentation and put the private method in the shard.
    if (matchingComponentMethod.isPresent() && shardImplementation.isComponentShard()) {
      ComponentMethodDescriptor componentMethod = matchingComponentMethod.get();
      return componentMethodRequestRepresentationFactory.create(bindingExpression, componentMethod);
    } else {
      return privateMethodRequestRepresentationFactory.create(request, binding, bindingExpression);
    }
  }

  /**
   * Returns {@code true} if the component needs to make sure the provided value is cached.
   *
   * <p>The component needs to cache the value for scoped bindings except for {@code @Binds}
   * bindings whose scope is no stronger than their delegate's.
   */
  private boolean needsCaching() {
    if (!binding.scope().isPresent()) {
      return false;
    }
    if (binding.kind().equals(DELEGATE)) {
      return isBindsScopeStrongerThanDependencyScope(binding, graph);
    }
    return true;
  }

  @AssistedFactory
  static interface Factory {
    ProvisionBindingRepresentation create(ProvisionBinding binding);
  }
}
