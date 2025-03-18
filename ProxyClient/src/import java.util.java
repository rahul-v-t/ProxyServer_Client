public class BinarySearch { 
    public static int binarySearch(int[] arr, int key) { 
        int low = 0, high = arr.length - 1; 
 
        while (low <= high) { 
            int mid = low + (high - low) / 2; 
 
            if (arr[mid] == key) { 
                return mid; 
            } else if (arr[mid] < key) { 
                low = mid + 1; 
            } else { 
                high = mid - 1; 
            } 
        } 
        return -1; // Element not found 
    }
    public static void main(String[] args) { 
        int[] arr = {10, 20, 30, 40, 50}; 
        Arrays.sort(arr); // Ensure array is sorted 
        int key = 30; 
        int result = binarySearch(arr, key); 
        System.out.println("Element found at index: " + result); 
    } 
} 
