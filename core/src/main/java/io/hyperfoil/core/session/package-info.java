/**
 * <h1>Design</h1>
 *
 * There are two main components:
 * <ul>
 * <li>{@link io.hyperfoil.core.session.SequenceImpl Sequence templates} - instructions 'what to do'
 * <li>Session (execution context) holds any state, including current state of the state machine and variables
 * </ul>
 * <h2>Memory allocation</h2>
 *
 * In order to keep object allocations at minimum we're expected to know all variables in advance and pre-allocate
 * these in the Session object. During consecutive repetitions of the user scenario
 * the {@link io.hyperfoil.core.session.SessionImpl} is {@link io.hyperfoil.core.session.SessionImpl#reset()}
 * which does not release the memory.
 * <p>
 * Any memory required by validators/extractors must be known ahead and these should implement
 * the {@link io.hyperfoil.core.api.ResourceUtilizer} interface to register itselves. The reservation is invoked
 * once when the session is created through {@link io.hyperfoil.core.session.SequenceImpl#reserve(Session)}
 * which in turn calls this on all {@link io.hyperfoil.api.config.Step steps} and these call the
 * {@link io.hyperfoil.api.session.Session.Processor processors} or any other handlers.
 *
 * <h2>Execution</h2>
 *
 * After the session is constructed or reset you should create {@link io.hyperfoil.api.session.SequenceInstance sequence instances}
 * from the {@link io.hyperfoil.core.session.SequenceImpl templates} and subsequently
 * {@link io.hyperfoil.core.session.SessionImpl#enableSequence(SequenceInstance) enable}
 * them in the session. Upon {@link io.hyperfoil.core.session.SessionImpl#runSession()} the session tries to invoke all enabled
 * sequence instances; some of the enabled sequences may be blocked because of missing data dependency.
 * <p>
 * The sequence consists of several {@link io.hyperfoil.api.config.Step steps}, each of which may have some
 * data dependency. Therefore the sequence may be blocked in the middle. Other enabled sequence may be still invoked
 * as soon as its dependencies are satisfied. Each step can enable further sequences.
 * <p>
 * The execution of sequence cannot be forked but it can be terminated by calling
 * {@link io.hyperfoil.core.session.SessionImpl#currentSequence(SequenceInstance)}
 * with <code>null</code> argument - {@link io.hyperfoil.core.steps.BreakSequenceStep} is an example of that.
 * <p>
 * Execution is terminated when there are no enabled sequences in the session.
 *
 * <h2>Variables</h2>
 *
 * The {@link io.hyperfoil.api.session.Session} is provided as a parameter to most calls and stores all state of the scenario.
 * The state is operated using {@link io.hyperfoil.core.session.SessionImpl#getObject(java.lang.Object)} and
 * {@link io.hyperfoil.core.session.SessionImpl#setObject(java.lang.Object, java.lang.Object)} methods or their integer
 * counterparts.
 * <p>
 * Initially all variables are in undefined state; reading such variable is considered an error. The unset/set state
 * forms the basis of data-dependencies mentioned earlier: when a step requires the variable to be defined, you should
 * declare that with {@link io.hyperfoil.core.steps.DependencyStep#addDependency(VarReference)}.
 * <p>
 * It is possible to find out if the variable is set calling {@link io.hyperfoil.core.session.SessionImpl#isSet(java.lang.Object)}.
 * <p>
 * Simple variables are scalar, these are useful for scenario-scoped data. Other variables are scoped for particular
 * {@link io.hyperfoil.api.session.SequenceInstance}; these should be implemented as arrays (or collections) with
 * a limited size equal to the number of instances. When a Step/Processor needs to address sequence-scoped data
 * it fetches its index through {@link io.hyperfoil.core.session.SessionImpl#currentSequence()}.{@link io.hyperfoil.api.session.SequenceInstance#index() index()}.
 * <p>
 * The choice of index is up to the Step that creates the new sequences. Two concurrently enabled sequences may share
 * the same index, but in that case these should not use the same variable names for sequence-scoped data.
 *
 * <h2>Threading model</h2>
 *
 * There's no internal synchronization of anything; we rely on the event-loop model.
 * Each {@link io.hyperfoil.api.session.Session session} is tied to a single-threaded {@link io.netty.channel.EventLoop event loop}.
 */
package io.hyperfoil.core.session;

import io.hyperfoil.api.session.SequenceInstance;
import io.hyperfoil.api.session.VarReference;
import io.hyperfoil.api.session.Session;