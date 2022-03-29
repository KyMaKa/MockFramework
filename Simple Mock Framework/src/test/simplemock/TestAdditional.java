package test.simplemock;

import main.simplemock.MethodMock;
import main.simplemock.Mock;
import main.simplemock.SimpleMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestAdditional {

  private Mock<ClassToMock> mock;

  @Before
  public void setup() {
    mock = SimpleMock.mockType(ClassToMock.class);
    Assert.assertNotNull(mock);
  }

  @Test
  public void test_mocked_implementation() {

    MethodMock mockImp = args -> false;
    mock.setMockImplementation(mockImp, "someMethod");

    System.out.println(mock.getMocked().someMethod());
  }

  @Test
  public void test_public_field() {
    ClassToMock c = new ClassToMock();
    int c_field = SimpleMock.getFieldValue(c, "public_field", int.class);
    Assert.assertEquals(c.public_field, c_field);

    SimpleMock.mockField(c, "public_field", 2);
    System.out.println(c.public_field);
  }

  @Test
  public void test_private_field() {
    ClassToMock c = new ClassToMock();
    Boolean c_field = SimpleMock.getFieldValue(c, "private_field", Boolean.class);
    Assert.assertEquals(c.private_field, c_field);

    SimpleMock.mockField(c, "private_field", true);
    System.out.println(c.private_field);
  }

  public static class ClassToMock {

    public int public_field = 5;

    private Boolean private_field = false;

    public Object someMethod() {
      return "some string";
    }

  }

}
