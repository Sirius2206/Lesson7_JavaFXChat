import Lesson6_LogTest.TestsClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Test_TestsClass {

    public Stream<Arguments> dataForOF() {
        List<Arguments> out = new ArrayList<>();
        out.add(Arguments.arguments(new int[]{1, 2, 3, 4, 5, 6, 7, 8}, true));
        out.add(Arguments.arguments(new int[]{2, 2, 5}, false));
        out.add(Arguments.arguments(new int[]{3, 2, 4}, false));
        out.add(Arguments.arguments(new int[]{1, 7, 8, 5}, false));
        return out.stream();
    }

    @ParameterizedTest
    @MethodSource("dataForOF")
    public void testOneFour(int[] arr, boolean check) {
        if (!check) {
            Assertions.assertFalse(TestsClass.oneFour(arr));
            return;
        }
        Assertions.assertTrue(TestsClass.oneFour(arr));
    }

    public Stream<Arguments> dataForAAF() {
            List<Arguments> out = new ArrayList<>();
            out.add(Arguments.arguments(new int[]{1, 2, 3, 4, 5, 6, 7, 8}, new int[]{5, 6, 7, 8}, 1));
            out.add(Arguments.arguments(new int[]{2, 2, 5}, new int[]{}, 0));
            out.add(Arguments.arguments(new int[]{3, 2, 4}, new int[]{}, 1));
            out.add(Arguments.arguments(new int[]{4, 4, 5}, new int[]{5}, 2));
            return out.stream();
    }


    @ParameterizedTest
    @MethodSource("dataForAAF")
    public void testArrayAfterFour(int[] initialArray, int[] result, int fours) {
        if (fours == 0) {
            Throwable thrown = assertThrows(RuntimeException.class, () -> TestsClass.arrayAfterFour(initialArray));
            assertNotNull(thrown.getMessage());
            return;
        }
        Assertions.assertArrayEquals(result, TestsClass.arrayAfterFour(initialArray));
    }
}