# Using Android Mock #

This document outlines how to integrate Android Mock to your Android test project's build.  See here for information on [how to write tests using Android Mock](WritingTestsUsingAndroidMock.md).

## Table of Contents ##


## How Android Mock Works ##
Android Mock consists of two components, `AndroidMockGenerator.jar` and `AndroidMockRuntime.jar`.  The first of these (the mock generator) is used at compile time to process the various !@UsesMocks annotations that exist in your code.  Based on the contents of these annotations, various mocks will be produced and included in the class-file output folder (most projects tend to use `bin` as the output folder) alongside your compiled test code.  When you then use the `dx` tool to create a `classes.dex` file and finally create an `APK` file, these mocks will be included.

In addition to these mocks, you must also ensure that `AndroidMockRuntime.jar` is included in your `APK` file, as it is `AndroidMockRuntime.jar` that provides the runtime libraries necessary to run your tests (such as `AndroidMock` and `UsesMocks`).

## Using Android Mock From the Command Line ##
Android Mock uses a Java 1.6 Annotation processor to generate the mocks.  By simply including `AndroidMockGenerator.jar` in the classpath when you invoke `javac` to compile your tests, the Annotation processor will automatically be launched and generate the mocks.

After the `javac` step, you need to ensure that `AndroidMockRuntime.jar` is included as a standard dependency in your Android test program.  If you've ever depended on any external jar files for building your Android applications, you already know how to set this up, and should follow your own process.  If you're new to this, then the simplest approach is to copy the `AndroidMockRuntime.jar` file to your test project's class output folder (typically `bin`). _Keep in mind that including `AndroidMockRuntime.jar` in your main Android project will **not** make its classes available to your tests.  You must include it in the test project's APK._


## Using Android Mock From Eclipse ##
The instructions for setting this up in Eclipse are somewhat more complicated.  For detailed instructions, please see http://android-mock.googlecode.com/files/AndroidMockinEclipse.pdf

## Android Mock Annotation Processor options ##
  * RegenerateFrameworkMocks=`<boolean>`
> `[OPTIONAL]` If true, regenerates mocked classes for the Android Framework itself. Otherwise, android\_framework\_mocks.jar file is used to provide all the mocked classes for the framework.

  * bin\_dir=`<absolute path to project's output folder>`
> Absolute path to the bin/output folder of the project where the generated class mocks will be saved.

  * logfile=`<path to the log file>`
> `[OPTIONAL]` Path to the log file where general warnings and errors will be output.

  * target\_apilevel=`<comma-separated android-api levels>`
> `[OPTIONAL]` Use this to target individual android framework releases. For example, to test on ICS only, use target\_apilevel=14, and to test on both Gingerbread and ICS, use target\_apilevel=10,14. Please see http://developer.android.com/guide/appendix/api-levels.html for a complete list of api levels.