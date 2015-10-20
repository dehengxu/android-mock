AndroidMockGenerator.jar contains all of the files required to

  1. Process your @UsesMocks annotations
  1. Generate the Mocks
  1. And also contains all of the files that are contained within AndroidMockRuntime.jar

This last point is a matter of convenience so that you do not need to include both jar files in the classpath when compiling. Only this (AndroidMockGenerator.jar) jar file is needed at compile time. Only AndroidMockRuntime.jar should be present at runtime. This implies that this (AndroidMockGenerator.jar) jar file should **not** be used at the dex (dx) stage.