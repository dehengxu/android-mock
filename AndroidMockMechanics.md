# Introduction #

Note that these details may change in the future.

This page outlines how mock generation works in Android Mock, and which components are necessary at which stages.


# Details #

The following diagram is a visual representation of how it all fits together, and is taken from http://developer.android.com/guide/developing/building/index.html.

![http://android-mock.googlecode.com/files/Including%20AndroidMock.jpg](http://android-mock.googlecode.com/files/Including%20AndroidMock.jpg)

## Compilation ##
At this step, you only need to include the **AndroidMockGenerator.jar** in the classpath, along with android-framework-mocks.jar (in addition to anything else you would have needed without using Android Mock).

**AndroidMockGenerator.jar** contains the entire contents of **AndroidMockRuntime.jar**, and also contains the classes necessary to process and generate the mocks. (You may also include **AndroidMockRuntime.jar** at this stage, but since it is a subset of the **AndroidMockGenerator.jar**, it is not needed.)

**[android-framework-mocks.jar](AndroidFrameworkMocks.md)** contains a pre-generated mock for every mockable Android SDK class for every supported SDK, starting from 1.5 (Cupcake) and as of the writing of this document, ending at 2.3 (Gingerbread). As new SDKs are created, this jar is updated to include the latest SDKs, at which point those SDKs are considered to be supported by Android Mock.  These mocks are pulled out of the jar at compilation time and included in the output of the Compilation step.

### Pre-generated Mocks? ###
The pre-generated mocks are needed to support the use case of compiling against SDK 1.5 and running against SDK 2.3 (replace 1.5 and 2.3 however you like). This is required for developers who want to build versions of their software that are Android SDK version agnostic, and who want to be able to test their program using Android Mock.


## Dex -- Turning it into Dalvik Code ##
Dexing is the process of creating a dex-file, normally named classes.dx.  This file has rough semantic equivalents to a jar file, but differs from jar files in several key ways which are not of particular interest to this document.

The key thing to know is that at the Dex step, you are inputting Class files, and outputting a Dex file.  The set of class files here should be only the set of class files output by the Compilation step, in addition to directly included 3rd party libraries needed by your test project.  This includes **AndroidMockRuntime.jar**.  **AndroidMockGenerator.jar** is not needed any more, as no more annotation processing will be done.  Similarly, **android-framework-mocks.jar** must not be included in the dex step.

If you include files at this step which already exist on the Android system, then you will have errors such as **java.lang.IllegalArgumentException: already added** when trying to build your APK.


## APK Step -- Bundling it All Up ##

By this point in time, Android Mock need not be involved in the building process, and the normal flow of Android compilation may proceed.

If you see errors at this stage, then the seeds of those errors were sown in the Compilation or Dex steps.