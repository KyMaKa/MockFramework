package main.simplemock;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used as a wrapper for mocked instances. This wrapper can be used to inject
 * return values and capture arguments passed into specific methods
 *
 * @param <T>
 *            type of mocked instance wrapped within an instance of this class
 */
public class Mock<T> {

  private final Map<Method, MethodMock> responses = new HashMap<>();
  private final Map<Method, ArrayList<Object[]>> capturedRequests = new HashMap<>();
  private T mocked;
  private final Class<T> mockedType;

  /**
   * @param mockedType
   *            type of mocked instance wrapped within an instance of this
   *            class.
   */
  Mock(Class<T> mockedType) {
    this.mockedType = mockedType;
  }

  /**
   * Use this to specify a return value for a specific method. By default,
   * mocked methods will return 'null'.
   *
   * @param returnValue
   *            value to be returned when the mocked method is called
   * @param methodName
   *            name of the instance method to be mocked
   * @param argumentTypes
   *            leave this blank if only one method by the specified name
   *            exists, or if return value for the no-arg method is being
   *            mocked. Otherwise, list the argument types in order they are
   *            specified for the method of interest.
   */
  public void setReturnValue(final Object returnValue, String methodName, Class<?>... argumentTypes) {
    setMockImplementation(args -> returnValue, methodName, argumentTypes);
  }

  /**
   * This method allows
   * you to define a mock implementation of the instance method of interest
   * using the MethodMock interface
   *
   * @param mockImpl
   *            new implementation of the method
   * @param methodName
   *            name of the instance method to be mocked
   * @param argumentTypes
   *            leave this blank if only one method by the specified name
   *            exists, or if return value for the no-arg method is being
   *            mocked. Otherwise, list the argument types in order they are
   *            specified for the method of interest.
   */
  public void setMockImplementation(MethodMock mockImpl, String methodName, Class<?>... argumentTypes) {
    Method method = findMethod(methodName, argumentTypes);
    responses.put(method, mockImpl);
  }

  /**
   * @param methodName
   *            name of the instance method
   * @param argumentTypes
   *            leave this blank if only one method by the specified name
   *            exists, or if the method of interest is a 'no-arg method'.
   *            Otherwise, list the argument types in order they are specified
   *            for the method of interest.
   * @return the arguments passed in during the last time the specified method
   *         was invoked. 'null' if method has not yet been invoked
   */
  public Object[] getLastRequest(String methodName, Class<?>... argumentTypes) {
    Method method = findMethod(methodName, argumentTypes);
    List<Object[]> requests = capturedRequests.get(method);
    if (requests == null || requests.size() < 1) {
      return null;
    }
    return requests.get(requests.size() - 1);
  }

  /**
   * @param methodName
   *            name of the instance method
   * @param argumentTypes
   *            leave this blank if only one method by the specified name
   *            exists, or if the method of interest is a 'no-arg method'.
   *            Otherwise, list the argument types in order they are specified
   *            for the method of interest.
   * @return the arguments passed in during each invocation of the specified
   *         method via the mocked instance
   */
  @SuppressWarnings("unchecked")
  public List<Object[]> getAllCapturedRequests(String methodName, Class<?>... argumentTypes) {
    Method method = findMethod(methodName, argumentTypes);
    ArrayList<Object[]> requests = capturedRequests.get(method);
    if (requests == null) {
      return new ArrayList<>();
    }
    return (List<Object[]>) requests.clone();
  }

  /**
   * Clear all previously captured requests for specified method
   *
   * @param methodName
   *            name of the instance method
   * @param argumentTypes
   *            leave this blank if only one method by the specified name
   *            exists, or if the method of interest is a 'no-arg method'.
   *            Otherwise, list the argument types in order they are specified
   *            for the method of interest.
   */
  public void clearCapturedRequests(String methodName, Class<?>... argumentTypes) {
    Method method = findMethod(methodName, argumentTypes);
    ArrayList<Object[]> requests = capturedRequests.get(method);
    if (requests != null) {
      requests.clear();
    }
  }

  /** Clear all previously captured requests */
  public void clearCapturedRequests() {
    capturedRequests.clear();
  }

  /**
   * Used by proxies to invoke methods on the mocked instance
   *
   * @param method
   *            method to invoke
   * @param args
   *            arguments to pass into the method during invocation
   * @return result of invocation. 'null' if a return value hasn't been set
   *         using setReturnValue(Object, String, Class...)
   */
  Object runMethod(Method method, Object[] args) {

    // capture request
    ArrayList<Object[]> requests;

    requests = capturedRequests.computeIfAbsent(method, k -> new ArrayList<>());
    requests.add(args);

    // return response
    MethodMock mockImpl = responses.get(method);
    if (mockImpl == null) {
      return null;
    }
    return mockImpl.runMockImplementation(args);
  }

  /** @return the mocked instance */
  public T getMocked() {
    return mocked;
  }

  /**
   * @param mocked
   *            the mocked instance
   */
  void setMocked(T mocked) {
    this.mocked = mocked;
  }

  // find method from mocked type given name and argument types
  private Method findMethod(final String methodName, final Class<?>... argumentTypes) {
    try {
      return findMethodInClassHierarchy(mockedType, methodName, argumentTypes);
    } catch (NoSuchMethodException e) {
      try {
        return findMethodInInterfaceHierarchy(mockedType, methodName, argumentTypes);
      } catch (NoSuchMethodException e1) {
        throw MockException.wrap(new NoSuchMethodException(
            mockedType + "." + methodName + "(" + Arrays.asList(argumentTypes) + ")"));
      }
    }
  }

  private static Method findMethodInClassHierarchy(final Class<?> mockedType, final String methodName,
      final Class<?>... argumentTypes) throws NoSuchMethodException {
    try {
      return mockedType.getDeclaredMethod(methodName, argumentTypes);
    } catch (NoSuchMethodException e) {
      if (mockedType.getSuperclass() == null) {
        throw e;
      }
      return findMethodInClassHierarchy(mockedType.getSuperclass(), methodName, argumentTypes);
    }
  }

  private static Method findMethodInInterfaceHierarchy(final Class<?> mockedType, final String methodName,
      final Class<?>... argumentTypes) throws NoSuchMethodException {
    for (Class<?> _interface : mockedType.getInterfaces()) {
      try {
        return _interface.getDeclaredMethod(methodName, argumentTypes);
      } catch (NoSuchMethodException e) {
        try {
          return findMethodInInterfaceHierarchy(_interface, methodName, argumentTypes);
        } catch (NoSuchMethodException e1) {
          // ignore
        }
      }
    }
    throw new NoSuchMethodException();
  }
}
