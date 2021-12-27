package Lesson6_LogTest;

//Написать метод, которому в качестве аргумента передается не пустой одномерный целочисленный массив.
//        Метод должен вернуть новый массив, который получен путем вытаскивания из исходного массива элементов,
//        идущих после последней четверки. Входной массив должен содержать хотя бы одну четверку, иначе в методе
//        необходимо выбросить RuntimeException.
//        Написать набор тестов для этого метода (по 3-4 варианта входных данных).
//        Вх: [ 1 2 4 4 2 3 4 1 7 ] -> вых: [ 1 7 ].

public class TestsClass {

    public static int[] arrayAfterFour (int[] initialArray) {
        int count = 0;
        int i;
        for (i = initialArray.length - 1; i >= 0; i--) {
            if (initialArray[i] == 4) break;
            count++;
            if (i == 0) {
                throw new RuntimeException("Передан массив без единой четверки.");
            }
        }
        int[] newArray = new int[count];
        int j = count;
        for (i = initialArray.length - 1; i >= initialArray.length - j; i--) {
            count--;
            newArray[count] = initialArray[i];
        }
        return newArray;

    }

public static boolean oneFour (int[] array) {
        boolean one = false;
        boolean four = false;
    for (int j : array) {
        if (j == 1) one = true;
        if (j == 4) four = true;
    }
        return one && four;
}
}
