package com.hongenit.Aplay;

import android.os.SystemClock;
import android.util.Log;

import com.hongenit.Aplay.api.HttpClitens;

import org.junit.Test;

import static android.R.attr.foreground;
import static android.R.attr.left;
import static android.R.attr.right;
import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    private String blogUrls[] = {"http://blog.csdn.net/hongenit/article/details/53573212http://blog.csdn.net/hongenit/article/details/53558808", " http://blog.csdn.net/hongenit/article/details/53526827", " http://blog.csdn.net/hongenit/article/details/53516787", " http://blog.csdn.net/hongenit/article/details/53488355", " http://blog.csdn.net/hongenit/article/details/53486193", " http://blog.csdn.net/hongenit/article/details/53485807", " http://blog.csdn.net/hongenit/article/details/53471714", " http://blog.csdn.net/hongenit/article/details/53471611", " http://blog.csdn.net/hongenit/article/details/53466278", " http://blog.csdn.net/hongenit/article/details/53454482", " http://blog.csdn.net/hongenit/article/details/53454463", " http://blog.csdn.net/hongenit/article/details/53378894", " http://blog.csdn.net/hongenit/article/details/53378860", " http://blog.csdn.net/hongenit/article/details/53378700", " http://blog.csdn.net/hongenit/article/details/53376695", " http://blog.csdn.net/hongenit/article/details/53376277", " http://blog.csdn.net/hongenit/article/details/53376032", " http://blog.csdn.net/hongenit/article/details/53371734", " http://blog.csdn.net/hongenit/article/details/53371683", " http://blog.csdn.net/hongenit/article/details/53366120", " http://blog.csdn.net/hongenit/article/details/53366114", " http://blog.csdn.net/hongenit/article/details/53366103", " http://blog.csdn.net/hongenit/article/details/53366097", " http://blog.csdn.net/hongenit/article/details/53366094", " http://blog.csdn.net/hongenit/article/details/53366092", " http://blog.csdn.net/hongenit/article/details/53366091", " http://blog.csdn.net/hongenit/article/details/53366082", " http://blog.csdn.net/hongenit/article/details/53366080", " http://blog.csdn.net/hongenit/article/details/53366076", " http://blog.csdn.net/hongenit/article/details/53366070", " http://blog.csdn.net/hongenit/article/details/53366017", " http://blog.csdn.net/hongenit/article/details/53366013", " http://blog.csdn.net/hongenit/article/details/53365770", " http://blog.csdn.net/hongenit/article/details/53365766", " http://blog.csdn.net/hongenit/article/details/53356287", " http://blog.csdn.net/hongenit/article/details/53341246", " http://blog.csdn.net/hongenit/article/details/53323563", " http://blog.csdn.net/hongenit/article/details/53323330", " http://blog.csdn.net/hongenit/article/details/53303885", " http://blog.csdn.net/hongenit/article/details/53296650", " http://blog.csdn.net/hongenit/article/details/53292148", " http://blog.csdn.net/hongenit/article/details/53248492", " http://blog.csdn.net/hongenit/article/details/53235833", " http://blog.csdn.net/hongenit/article/details/53225387", " http://blog.csdn.net/hongenit/article/details/53146038", " http://blog.csdn.net/hongenit/article/details/53141935", " http://blog.csdn.net/hongenit/article/details/53107167", " http://blog.csdn.net/hongenit/article/details/53106674", " http://blog.csdn.net/hongenit/article/details/53039071", " http://blog.csdn.net/hongenit/article/details/53033746", " http://blog.csdn.net/hongenit/article/details/41481917", " http://blog.csdn.net/hongenit/article/details/39960845", " http://blog.csdn.net/hongenit/article/details/18216823", " http://blog.csdn.net/hongenit/article/details/9154949", " http://blog.csdn.net/hongenit/article/details/8440993", " http://blog.csdn.net/hongenit/article/details/8137167", " http://blog.csdn.net/hongenit/article/details/8133997", " http://blog.csdn.net/hongenit/article/details/8133283", " http://blog.csdn.net/hongenit/article/details/8133277"
    };

    int intArray[] = {45, 654, 66, 32, 213, 213, 131, 313, 23, 32, 13, 14, 8546, 498, 42, 16, 46, 321, 68, 46, 3, 1};

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
//        for (int i = 0; i < 14400; i++) {
////            Thread.sleep(1000);
//            int randomUrlIndex = (int) (Math.random() * blogUrls.length);
//            Thread.sleep(500 * randomUrlIndex);
//            HttpClitens.visitBlog(blogUrls[randomUrlIndex]);
//            System.out.println(i + "---------" + randomUrlIndex + "----" + blogUrls[randomUrlIndex]);
//        }
        System.out.println("-----1111");
        sort_arr(intArray, 0, intArray.length - 1);
        for (int i = 0; i < intArray.length; i++) {
            System.out.println("i=" + intArray[i]);
        }

    }

    void sort_arr(int arr[], int low, int high) {
        System.out.println("-----"+low+"-----"+high);
        if (low < high) {
            int left = low;
            int right = high;
            int key_str = arr[left];
            while (left < right) {
                while (left < right && arr[right] > key_str) {
                    right--;
                }
                arr[left] = arr[right];
                while (left < right && arr[left] <= key_str) {
                    left++;
                }
                arr[right] = arr[left];

            }
            arr[left] = key_str;
            sort_arr(arr, low, left - 1);
            sort_arr(arr, left + 1, high);
        }
    }

}