/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.testing.mocking;

import javassist.CannotCompileException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;


/**
 * Annotation Processor to generate the mocks for Android Mock.
 * 
 * This processor will automatically create mocks for all classes
 * specified by {@link UsesMocks} annotations.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
@SupportedAnnotationTypes("com.google.android.testing.mocking.UsesMocks")
@SupportedSourceVersion(SourceVersion.RELEASE_5)
@SupportedOptions({
    UsesMocksProcessor.REGENERATE_FRAMEWORK_MOCKS,
    UsesMocksProcessor.LOGFILE,
    UsesMocksProcessor.BIN_DIR,
    UsesMocksProcessor.TARGET_APILEVEL
})
public class UsesMocksProcessor extends AbstractProcessor {
  public static final String LOGFILE = "logfile";
  public static final String REGENERATE_FRAMEWORK_MOCKS = "RegenerateFrameworkMocks";
  public static final String BIN_DIR = "bin_dir";
  public static final String TARGET_APILEVEL = "target_apilevel";
  
  private String binDir;
  private AndroidFrameworkMockGenerator frameworkMockGenerator =
    new AndroidFrameworkMockGenerator();
  private String logFileName;
  private AndroidMockGenerator mockGenerator = new AndroidMockGenerator();
  private boolean regenerateMocks;
  Collection<SdkVersion> targetSdkVersions = EnumSet.noneOf(SdkVersion.class);
  ProcessorLogger logger;

  /**
   * Main entry point of the processor.  This is called by the Annotation framework.
   * {@link javax.annotation.processing.AbstractProcessor} for more details.
   */
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
    try {
      prepareLogger();
      readProcessorOptions();
      applyProcessorOptions();
      List<Class<?>> classesToMock = getClassesToMock(environment);
      Set<GeneratedClassFile> mockedClassesSet = getMocksFor(classesToMock);
      writeMocks(mockedClassesSet);
    } catch (Exception e) {
      logger.printMessage(Kind.ERROR, e);
    } finally {
      logger.close();
    }
    return false;
  }

  /**
   * Returns a Set of GeneratedClassFile objects which represent all of the classes to be mocked.
   * 
   * @param classesToMock the list of classes which need to be mocked.
   * @return a set of mock support classes to support the mocking of all the classes specified in
   *         {@literal classesToMock}.
   */
  private Set<GeneratedClassFile> getMocksFor(List<Class<?>> classesToMock) throws IOException,
      CannotCompileException {
    logger.printMessage(Kind.NOTE, "Found " + classesToMock.size() + " classes to mock");
    if (regenerateMocks) {
      logger.printMessage(Kind.NOTE, "Regenerating Framework Mocks on Request");
    }
    Set<GeneratedClassFile> mockedClassesSet =
        getClassMocks(classesToMock, regenerateMocks);
    logger.printMessage(Kind.NOTE, "Found " + mockedClassesSet.size()
        + " mocked classes to save");
    return mockedClassesSet;
  }

  /**
   * @param environment the environment for this round of processing as provided to the main
   *        {@link #process(Set, RoundEnvironment)} method.
   * @return a List of Class objects for the classes that need to be mocked.
   */
  private List<Class<?>> getClassesToMock(RoundEnvironment environment) {
    logger.printMessage(Kind.NOTE, "Start Processing Annotations");
    List<Class<?>> classesToMock = new ArrayList<Class<?>>();
    classesToMock.addAll(
        findClassesToMock(environment.getElementsAnnotatedWith(UsesMocks.class)));
    return classesToMock;
  }

  private void prepareLogger() {
    if (logger == null) {
      logger = new ProcessorLogger(processingEnv);
    }
  }

  private String toClassPath(TypeElement element) {
    if (element.getNestingKind().isNested()) {
      return toClassPath((TypeElement) element.getEnclosingElement()) + "$" +
          element.getSimpleName().toString();
    }
    return element.getQualifiedName().toString();
  }

  /**
   * Finds all of the classes that should be mocked, based on {@link UsesMocks} annotations
   * in the various source files being compiled.
   * 
   * @param annotatedElements a Set of all elements holding {@link UsesMocks} annotations.
   * @return all of the classes that should be mocked.
   */
  List<Class<?>> findClassesToMock(Set<? extends Element> annotatedElements) {
    logger.printMessage(Kind.NOTE, "Processing " + annotatedElements);
    List<Class<?>> classList = new ArrayList<Class<?>>();
    for (Element annotation : annotatedElements) {
      List<? extends AnnotationMirror> mirrors = annotation.getAnnotationMirrors();
      for (AnnotationMirror mirror : mirrors) {
        if (mirror.getAnnotationType().toString().equals(UsesMocks.class.getName())) {
          for (AnnotationValue annotationValue : mirror.getElementValues().values()) {
            @SuppressWarnings("unchecked")
            Iterable<? extends AnnotationValue> classValueIterable =
                (Iterable<? extends AnnotationValue>) annotationValue.getValue();
            for (AnnotationValue classValue : classValueIterable) {
              String classPath = null;
              Object value = classValue.getValue();
              if (value instanceof DeclaredType) {
                Element element = ((DeclaredType) value).asElement();
                if (element instanceof TypeElement) {
                  classPath = toClassPath((TypeElement) element);
                }
              }
              if (classPath == null) {
                classPath = value.toString();
                if (classPath.endsWith(".class")) {
                  classPath = classPath.substring(0, classPath.length() - 6);
                }
              }
              logger.printMessage(Kind.NOTE, "Adding Class to Mocking List: " + classPath);
              try {
                classList.add(Class.forName(classPath, false, getClass().getClassLoader()));
              } catch (ClassNotFoundException e) {
                logger.reportClasspathError(classPath, e);
              }
            }
          }
        }
      }
    }
    return classList;
  }

  /**
   * Gets a set of GeneratedClassFiles to represent all of the support classes required to mock the
   * List of classes provided in {@code classesToMock}.
   * 
   * @param classesToMock the list of classes to be mocked.
   * @param regenerateFrameworkMocks if true, then mocks for the framework classes will be created
   *        instead of pulled from the existing set of framework support classes.
   * @return a Set of {@link GeneratedClassFile} for all of the mocked classes.
   */
  Set<GeneratedClassFile> getClassMocks(List<Class<?>> classesToMock,
      boolean regenerateFrameworkMocks) throws IOException, CannotCompileException {
    Set<GeneratedClassFile> mockedClassesSet = new HashSet<GeneratedClassFile>();
    for (Class<?> clazz : classesToMock) {
      try {
        boolean isAndroidClass = AndroidMock.isAndroidClass(clazz);
        if (!isAndroidClass || regenerateFrameworkMocks) {
          mockedClassesSet.addAll(createMocksForClass(clazz, isAndroidClass));
        } else {
          mockedClassesSet.addAll(getMocksForClass(clazz));
        }
      } catch (ClassNotFoundException e) {
        logger.reportClasspathError(clazz.getName(), e);
      } catch (NoClassDefFoundError e) {
        logger.reportClasspathError(clazz.getName(), e);
      }
    }
    return mockedClassesSet;
  }

  private AndroidFrameworkMockGenerator getAndroidFrameworkMockGenerator() {
    return frameworkMockGenerator;
  }

  private List<GeneratedClassFile> createMocksForClass(Class<?> clazz, boolean isAndroidClass)
      throws ClassNotFoundException, IOException, CannotCompileException {
    if (targetSdkVersions.contains(SdkVersion.UNKNOWN) || !isAndroidClass) {
      logger.printMessage(Kind.NOTE,
          String.format("Creating mocks of class: %s", clazz.getName()));
      return getAndroidMockGenerator().createMocksForClass(clazz);
    } else {
      List<GeneratedClassFile> mockClassList = new ArrayList<GeneratedClassFile>();
      for (SdkVersion targetSdkVersion : targetSdkVersions) {
        logger.printMessage(
            Kind.NOTE,
            String.format("Creating mocks of class: %s for %s", clazz.getName(),
                targetSdkVersion.name()));
        mockClassList
            .addAll(getAndroidMockGenerator().createMocksForClass(clazz, targetSdkVersion));
      }
      return mockClassList;
    }
  }

  private List<GeneratedClassFile> getMocksForClass(Class<?> clazz) throws ClassNotFoundException,
      IOException {
    if (targetSdkVersions.contains(SdkVersion.UNKNOWN)) {
      logger.printMessage(Kind.NOTE,
          String.format("Fetching mocks of class: %s", clazz.getName()));
      return getAndroidFrameworkMockGenerator().getMocksForClass(clazz);
    } else {
      List<GeneratedClassFile> mockClassList = new ArrayList<GeneratedClassFile>();
      for (SdkVersion targetSdkVersion : targetSdkVersions) {
        logger.printMessage(
            Kind.NOTE,
            String.format("Fetching mocks of class: %s for %s", clazz.getName(),
                targetSdkVersion.name()));
        mockClassList.addAll(getAndroidFrameworkMockGenerator().getMocksForClass(clazz,
            targetSdkVersion));
      }
      return mockClassList;
    }
  }

  /**
   * Reads the processor options passed in to the annotation processor and stores them in local
   * variables.
   * 
   * @throws NumberFormatException if the target version is not numeric.
   */
  private void readProcessorOptions() throws NumberFormatException {
    String targetApiLevel = processingEnv.getOptions().get(TARGET_APILEVEL);
    if (targetApiLevel == null) {
      targetSdkVersions.add(SdkVersion.UNKNOWN);
    } else {
      try {
        for (String s : targetApiLevel.split(",")) {
          targetSdkVersions.add(SdkVersion.getVersionFor(Integer.parseInt(s)));
        }
      } catch (NumberFormatException e) {
        logger.printMessage(Kind.ERROR,
            "target_apilevel must be numeric. for ex., -Atarget_apilevel=10");
        throw e;
      }
    }

    logFileName = processingEnv.getOptions().get(LOGFILE);
    binDir = processingEnv.getOptions().get(BIN_DIR);
    regenerateMocks = processingEnv.getOptions().get(REGENERATE_FRAMEWORK_MOCKS) != null;
  }

  /**
   * Apply the processor options to entities that need it.
   */
  private void applyProcessorOptions() {
    if (logger != null) {
      logger.setLogFile(logFileName);
    }
  }

  /**
   * Writes the provided mocks from {@code mockedClassesSet} to the bin folder alongside the .class
   * files being generated by the javac call which invoked this annotation processor. In Eclipse,
   * additional information is needed as the Eclipse annotation processor framework is missing key
   * functionality required by this method. Instead the classes are saved using a FileOutputStream
   * and the -Abin_dir processor option must be set.
   * 
   * @param mockedClassesSet the set of mocks to be saved.
   */
  void writeMocks(Set<GeneratedClassFile> mockedClassesSet) {
    for (GeneratedClassFile clazz : mockedClassesSet) {
      OutputStream classFileStream;
      try {
        logger.printMessage(Kind.NOTE, "Saving " + clazz.getClassName());
        JavaFileObject classFile = processingEnv.getFiler().createClassFile(clazz.getClassName());
        classFileStream = classFile.openOutputStream();
        classFileStream.write(clazz.getContents());
        classFileStream.close();
      } catch (IOException e) {
        logger.printMessage(Kind.ERROR, "Internal Error saving mock: " + clazz.getClassName());
        logger.printMessage(Kind.ERROR, e);
      } catch (UnsupportedOperationException e) {
        // Eclipse annotation processing doesn't support class creation.
        logger.printMessage(Kind.NOTE, "Saving via Eclipse " + clazz.getClassName());
        saveMocksEclipse(clazz, binDir.trim().toString());
      }
    }
    logger.printMessage(Kind.NOTE, "Finished Processing Mocks");
  }

  /**
   * Workaround to save the mocks for Eclipse's annotation processing framework which doesn't
   * support the JavaFileObject object.
   * @param clazz the class to save.
   * @param outputFolderName the output folder where the class will be saved.
   */
  private void saveMocksEclipse(GeneratedClassFile clazz, String outputFolderName) {
    try {
      FileUtils.saveClassToFolder(clazz, outputFolderName);
    } catch (FileNotFoundException e) {
      logger.printMessage(Kind.ERROR, e);
    } catch (IOException e) {
      logger.printMessage(Kind.ERROR, e);
    }
  }

  private AndroidMockGenerator getAndroidMockGenerator() {
    return mockGenerator;
  }
}
