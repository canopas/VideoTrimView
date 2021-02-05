# VideoTrim View

<img src="https://github.com/canopas/VideoTrimView/blob/VideTrim/screenshot/Feb-05-2021%2009-50-41.gif" alt="VideoTrimmer Screenshot" width="360" height="640" />


# Usage

*For a working implementation, please have a look at the [Sample Project](https://github.com/canopas/VideoTrimView/tree/VideTrim/sample-app)
1. Add the dependency.

    ```  implementation 'com.github.canopas:VideoTrimView:1.0.1' ```
    
2. Add VideoTrimView view into your layout.

    ```
      <com.canopas.trimview.VideoTrimView
        android:id="@+id/timeLine"
        android:layout_width="match_parent"
        android:layout_height="70dp"/>

    ```

3. Set the video Uri.

    ```java
        videoTrimmer.setVideoURI(Uri.parse(path));
    ```
