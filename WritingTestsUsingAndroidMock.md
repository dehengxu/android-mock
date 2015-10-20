# Writing Tests using Android Mock #

## Table of Contents ##


## Android Mock and EasyMock ##

Android Mock completely reuses the grammar and usage of EasyMock.  It is, in fact, a wrapper around EasyMock meant to trick out the Dalvik VM's ClassLoader to allow for mocking of classes.  Users who are familiar with EasyMock will already be able to use Android Mock in the way with which they are familiar.

## Starting Simply ##

### Sample Code ###
```
package com.google.android.testing.mocktest.test;

import com.google.android.testing.mocking.AndroidMock;

public class MockingTest extends TestCase {
  @UsesMocks(ClassToMock.class)
  public void testMocks() throws ClassNotFoundException {
    ClassToMock myMockObject = AndroidMock.createMock(ClassToMock.class);
    AndroidMock.expect(myMockObject.getString()).andReturn("Woohoo");
    AndroidMock.expect(myMockObject.getNextInt(2)).andReturn(42);
    AndroidMock.replay(myMockObject);
    assertEquals("Woohoo", myMockObject.getString());
    assertEquals(42, myMockObject.getNextInt(2));
    AndroidMock.verify(myMockObject);
  }
}
```

### Creating the Mock ###
Parsing the above example, the call to `AndroidMock.createMock` will generate the mock for `ClassToMock.class`.  The returned object is castable to the type `ClassToMock`.  If `ClassToMock` is an interface, then the returned object is a `java.lang.reflect.Proxy` instance implementing the `ClassToMock` interface.  If `ClassToMock` is a class, then the returned object is a subclass of `ClassToMock`.  In both cases, all calls to **non-static, public/protected, non-final** methods will be handled by the mock object.

### Expectations ###
The purpose of a mock is not simply to stub out behaviours, but to also verify ordered correctness and completeness of a sequence of calls on the mock object.  Thus the next step is to define expectations for the mock.  The created mock object starts in the **recording** state.  In this state, all calls to mocked methods on the mock will be intercepted by the mock object, and a queue of expected invocations is generated.  For methods that return a value, return values are associated with each expectation.  Thus the following lines:

```
    AndroidMock.expect(myMockClass.getString()).andReturn("Woohoo");
    AndroidMock.expect(myMockClass.getNextInt(2)).andReturn(42);
```

Indicate to the mock object that the `getString()` method will be called, and when this happens, the mock object will return the String `"Woohoo"`.  Following this call, the `getNextInt(2)` method will be called on the mock object, with the parameter `2`, which will then return `42`.  At this point, these are the only two method calls that are expected on this mock object.

### Replaying the Expectations ###
By calling `AndroidMock.replay(myMockObject)` the mock object (named `myMockObject`) is now ready to be tested.  The following calls to `myMockObject.getString()` and `myMockObject.getNextInt(2)` return `"Woohoo"` and `42` respectively (since this is what was defined during record mode.

The mock object expects that both of these calls will be made in this order, with the specified parameters as defined in the record phase.  Had the calls happened out of turn, or had `getNextInt()` received a parameter with a value other than `2`, then =Android Mock= would have thrown an Exception to indicate the failure.

### Verifying the End of Replay ###
Finally, `AndroidMock.verify(myMockObject)` is called to tell =Android Mock= that the test has finished making calls on `myMockObject`.  At this point, if any expected method calls have not yet occurred, then an Exception is thrown to fail the test.


## Methods without Return Values ##
One of the first problems newbies encounter (myself included) is what to do when you have a method with return type `void`.

```
AndroidMock.expect(myMockObject.getString()).andReturn("Hello world");
```

The above call-chain only works because `myMockObject.getString()` returns something (in this case, a String).  But if the return type of `getString()` was `void`, then the code simply won't compile.

In these cases, just call the method as you need.  For instance, if you want to mock a class such as:
```
class MyClass {
  public void print(int arg) {
    System.out.println(arg);
  }
}
```

Set up a test like this:
```
...
  @UsesMocks(ClassToMock.class)
  public void testPrint() throws ClassNotFoundException {
    MyClass myMockObject = AndroidMock.createMock(MyClass.class);
    myMockObject.print(5);
    AndroidMock.replay(myMockObject);
    myMockObject.print(5);
    AndroidMock.verify(myMockObject);
  }
...
```

Of course, the above test is not very interesting, since all it does is print something to the screen, but in this case, the mock object would not print to `System.out`, and further more, there would be a failure if any parameter other than `5` had been passed to `print` and had there been multiple expectations, the ordering of the expectations, and whether or not all expectations were invoked would have been tested.


## Reusing a Mock / Looping on a Mock ##
To reuse a mock object, call `AndroidMock.reset()`.

### Sample Code ###
```
package com.google.android.testing.mocktest.test;

import com.google.android.testing.mocking.AndroidMock;

public class MockingTest extends TestCase {
  @UsesMocks(ClassToMock.class)
  public void testMocks() throws ClassNotFoundException {
    ClassToMock myMockObject = AndroidMock.createMock(ClassToMock.class);
    for (int i = 0; i < 5; ++i) {
      AndroidMock.expect(myMockObject.getString()).andReturn("Woohoo");
      AndroidMock.expect(myMockObject.getNextInt(i)).andReturn(42 + i);
      AndroidMock.replay(myMockObject);
      assertEquals("Woohoo", myMockObject.getString());
      assertEquals(42 + i, myMockObject.getNextInt(i));
      AndroidMock.verify(myMockObject);
      AndroidMock.reset(myMockObject);
    }
  }
}
```

Using `reset` is necessary for loops such as in the sample code, and is a very useful and compact way of testing a series of expectations on a mock object without having to write a separate test for each iteration of expectations.


## Getting Fancy with Expectations ##
There are a host of methods allowing for special expectations.  These are useful in a number of situations, such as when the value of a parameter is not easily controlled by the test.  Various degrees of control are available from allowing any of a given type (primitive or object type), or restricting to very specific criteria.

### Permissive Expectations ###

The `anyXxxx()` type of expectations are used instead of parameters to indicate that anything of the given type is acceptable.

**You cannot mix literal values with permissive expectations when passing values to the `expect()` call.  See below for sample code concerning this restriction.**

### Sample Code ###
```
package com.google.android.testing.mocktest.test;

import com.google.android.testing.mocking.AndroidMock;

public class MockingTest extends TestCase {
  @UsesMocks(ClassToMock.class)
  public void testMocks() throws ClassNotFoundException {
    ClassToMock myMockObject = AndroidMock.createMock(ClassToMock.class);
    AndroidMock.expect(myMockObject.getNextInt(AndroidMock.anyInt())).andReturn(42);
    AndroidMock.replay(myMockObject);
    assertEquals(42, myMockObject.getNextInt(31));
    AndroidMock.verify(myMockObject);
  }
}
```

In the code above, instead of specifying a specific `int` in the `expect(getNextInt())` call, `AndroidMock.anyInt()` is used, thus allowing any `int` value to be acceptable as a parameter.

This is useful when the incoming parameter value really doesn't matter in the context of the test.

### Mixing Literals with Matchers ###
If you use literals with Permissive matchers, you will encounter this error: `java.lang.IllegalStateException: 2 matchers expected, 1 recorded.`

EasyMock does _some_ auto-magic boxing of literals into expectation matchers for you.  But it will only do so when you don't use any matchers while defining the expectations.

e.g.
```
// This code is wrong  -- String literal mixed with anyObject() in the call to "myMock.set"

AndroidMock.expect(myMock.set("my key", AndroidMock.anyObject())).andReturn("old object");
```

The correct way to write this is:
```
// This code is right  -- String literal is wrapped in AndroidMock.eq()

AndroidMock.expect(myMock.set(AndroidMock.eq("my key"), AndroidMock.anyObject())).andReturn("old object");
```


### Asserting Preconditions with Expectations ###

More complicated expectations can be built up using the various _operator_ style expectations.  These are:
```
geq()    // Greater than or Equals to
leq()    // Less than or Equals to
gt()     // Greater than
lt()     // Less than
eq()     // Equals
```

As an example `AndroidMock.expect(myMockObject.getNextInt(AndroidMock.geq(0))).andReturn(42);` would be a way of verifying that the next call to `getNextInt` (in replay mode) will fail if the parameter is not a positive number (that is, equal to or greater than 0).

On its own, the usefulness of this seems limited, but if you use are passing a mock object into a real object under test as a dependency, then this is one means of ensuring that the object under test never calls the mock with values that are unacceptable to the mocked object.  An example will help:

### Sample Code ###
```
package com.google.android.testing.mocktest.test;

import com.google.android.testing.mocking.AndroidMock;

public class MockingTest extends TestCase {
  @UsesMocks(MyWorkerClass.class)
  public void testMocks() throws ClassNotFoundException {
    MyWorkerClass myMockWorker = AndroidMock.createMock(MyWorkerClass.class);
    AndroidMock.expect(myMockWorker.getValueForPositiveNumber(AndroidMock.geq(0))).andReturn(1);
    AndroidMock.replay(myMockWorker);
    MyComplicatedClass complicated = new MyComplicatedClass(myMockWorker);

    assertEquals(complicated.doComplicatedWork(), 100);
    AndroidMock.verify(myMockWorker);
  }
}
```

 And for completeness, we will assume that `MyComplicatedClass.doComplicatedWork()` is expected to return `100` whenever the worker class returns `1` from `getValueForPositiveNumber`.

Note that in this scenario, the test has no control over what value is passed to `myMockWorker.getValueForPositiveNumber(int)`.  If, for instance, the calling class `MyComplicatedClass` were to use different values based on different conditions (such as time, state of the system, return value from something else, etc) it could be very difficult to ensure the value passed to `getValueForPositiveNumber` and it could be even more difficult to ensure that the value is a valid value (in this case, greater than or equal to zero).

#### Why AndroidMock.eq(42)?  Why not just 42? ####
EasyMock does _some_ auto-magic boxing of literals into expectation matchers for you.  But it will only do so when you don't use any matchers while defining the expectations.

The following snippet of code contains two `expects()` calls.  In this case, the two lines are semantically equivalent.  But this is only true because the first call only uses literals (42) as an expectation, while the second only uses an explicit `eq` matcher.

```
AndroidMock.expect(myMock.set(42));

AndroidMock.expect(myMock.set(AndroidMock.eq(42)));
```

Observe the following code, though, when there are multiple parameters:

```
// This works.
AndroidMock.expect(myMock.set(42, 43));

// This also works exactly the same as above.
AndroidMock.expect(myMock.set(AndroidMock.eq(42), AndroidMock.eq(43)));

// This is a runtime error (IllegalStateException)
AndroidMock.expect(myMock.set(42, AndroidMock.eq(43)));

// This is a runtime error (IllegalStateException)
AndroidMock.expect(myMock.set(42, AndroidMock.anyInt()));
```

This, then, is why the `eq()` matcher exists.  While you can easily change the second and third examples above to match the first, the fourth can only be rewritten using the `eq()` matcher:
```
// This works.
AndroidMock.expect(myMock.set(AndroidMock.eq(42), AndroidMock.anyInt()));
```


### Complex Expectations ###
Taking the previous example on step further, it is also possible to combine different expectations using the `and`, `or`, and `not` expectations.

These are very confusing, since it is not at all clear that the parameters passed to them are expected to be other custom expectations and not just plain values.  Again, an example is much more illustrative.

### Sample Code ###
Repeating the example from above, let's change the preconditions to be not only greater than or equal to zero, but also strictly less than 100.  In this case, the example becomes:

```
package com.google.android.testing.mocktest.test;

import com.google.android.testing.mocking.AndroidMock;

public class MockingTest extends TestCase {
  @UsesMocks(MyWorkerClass.class)
  public void testMocks() throws ClassNotFoundException {
    MyWorkerClass myMockWorker = AndroidMock.createMock(MyWorkerClass.class);
    AndroidMock.expect(myMockWorker.getValueForAcceptableNumber(
        AndroidMock.and(AndroidMock.geq(0), AndroidMock.lt(100)))).andReturn(1);
    AndroidMock.replay(myMockWorker);
    MyComplicatedClass complicated = new MyComplicatedClass(myMockWorker);

    assertEquals(complicated.doComplicatedWork(), 100);
    AndroidMock.verify(myMockWorker);
  }
}
```

 As above, we will assume that `MyComplicatedClass.doComplicatedWork()` is expected to return `100` whenever the worker class returns `1` from `getValueForAcceptableNumber`.

The code is starting to become difficult to read with all of the embedded method calls, but it should be pretty clear that the `geq` and `lt` expectations have been `and`'ed together to create a single parameter expectation to be set on the mock `MyWorkerClass` object's `getValueForAcceptableNumber` method.

### Complex and Simple Expectations ###
For simplicity's sake, a list of complex and simple expectations are listed here.

| **Definition** | Complex Expectation | An expectation whose parameter(s) themselves must also be expectations.  Complex expectations may be chained together. |
|:---------------|:--------------------|:-----------------------------------------------------------------------------------------------------------------------|
| **Definition** | Simple Expectation  | An expectation whose parameter(s) are non-expectation values.                                                          |

| **Expectation Name** | **Expectation type** | **Description** | **Example** |
|:---------------------|:---------------------|:----------------|:------------|
| `and`                | Complex              | Sets an expectation of both of the expectations provided as parameters | `AndroidMock.and(AndroidMock.leq(0), AndroidMock.gt(-5))` |
| `or`                 | Complex              | Sets an expectation of at least one of the expectations provided as parameters | `AndroidMock.or(AndroidMock.leq(0), AndroidMock.gt(15))` |
| `not`                | Complex              | Sets an expectation that is the opposite of the expectation provided as a parameter | `AndroidMock.not(AndroidMock.eq(42))` |
| `eq`                 | Simple               | Sets an expectation that is satisfied only when the received parameter equals the expectation's parameter | `AndroidMock.eq(42)` |
| `leq`                | Simple               | Sets an expectation that is satisfied only when the received parameter is less than or equal to the expectation's parameter | `AndroidMock.leq(42)` |
| `geq`                | Simple               | Sets an expectation that is satisfied only when the received parameter is greater than or equal to the expectation's parameter | `AndroidMock.geq(42)` |
| `lt`                 | Simple               | Sets an expectation that is satisfied only when the received parameter is less than the expectation's parameter | `AndroidMock.lt(42)` |
| `gt`                 | Simple               | Sets an expectation that is satisfied only when the received parameter is greater than the expectation's parameter | `AndroidMock.gt(42)` |
| `find`               | Simple               | Sets an expectation that is satisfied only when the received String parameter contains the expectation's String parameter (when interpreted as a regular expression) | `AndroidMock.find("[A-Z]*")` |
| `matches`            | Simple               | Sets an expectation that is satisfied only when the received String parameter matches the expectation's String parameter (when interpreted as a regular expression) | `AndroidMock.matches("[A-Z]*")` |
| `startsWith`         | Simple               | Sets an expectation that is satisfied only when the received String parameter starts with the expectation's String parameter | `AndroidMock.startsWith("Hello")` |
| `same`               | Simple               | Sets an expectation that is satisfied only when the received parameter is the exact same object as the expectation's parameter | `AndroidMock.same(thisExactObject)` |
| `isNull`             | Simple               | Sets an expectation that is satisfied only when the received parameter is `null` | `AndroidMock.isNull(myObjectMaybeNull)` |
| `notNull`            | Simple               | Sets an expectation that is satisfied only when the received parameter is not `null` | `AndroidMock.notNull(myObjectMaybeNull)` |
| `anyBoolean`         | Simple               | Sets an expectation that is satisfied for any received `boolean` parameter | `AndroidMock.anyBoolean(someValue)` |
| `anyByte`            | Simple               | Sets an expectation that is satisfied for any received `byte` parameter | `AndroidMock.anyByte(someValue)` |
| `anyChar`            | Simple               | Sets an expectation that is satisfied for any received `char` parameter | `AndroidMock.anyChar(someValue)` |
| `anyDouble`          | Simple               | Sets an expectation that is satisfied for any received `double` parameter | `AndroidMock.anyDouble(someValue)` |
| `anyFloat`           | Simple               | Sets an expectation that is satisfied for any received `float` parameter | `AndroidMock.anyFloat(someValue)` |
| `anyInt`             | Simple               | Sets an expectation that is satisfied for any received `int` parameter | `AndroidMock.anyInt(someValue)` |
| `anyLong`            | Simple               | Sets an expectation that is satisfied for any received `long` parameter | `AndroidMock.anyLong(someValue)` |
| `anyObject`          | Simple               | Sets an expectation that is satisfied for any received `Object` parameter | `AndroidMock.anyObject(someValue)` |
| `anyShort`           | Simple               | Sets an expectation that is satisfied for any received `short` parameter | `AndroidMock.anyShort(someValue)` |