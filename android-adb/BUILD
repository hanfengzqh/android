load("//tools/base/bazel:bazel.bzl", "iml_module")
load("//tools/base/bazel:maven.bzl", "maven_repo")

iml_module(
    name = "android-adb",
    srcs = ["src"],
    iml_files = ["android-adb.iml"],
    tags = ["managed"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/base/ddmlib:studio.ddmlib[module]",
        "//tools/idea/.idea/libraries:jsr305",
        "//tools/idea/platform/platform-api[module]",
        "//tools/idea/platform/xdebugger-api[module]",
        "//tools/idea/.idea/libraries:KotlinJavaRuntime",
        "//tools/idea/.idea/libraries:Guava",
    ],
)

iml_module(
    name = "android-adb-tests",
    iml_files = ["android-adb-tests.iml"],
    tags = ["managed"],
    test_class = "com.android.tools.idea.adb.AndroidAdbTestSuite",
    test_data = ["//prebuilts/studio/sdk:platform-tools"],
    # do not sort: must match IML order
    test_runtime_deps = [
        "//tools/idea/RegExpSupport",
        "//tools/idea/xml/xml-structure-view-impl",
        "//tools/idea/spellchecker",
        "//tools/idea/images",
        "//tools/idea/platform/built-in-server",
        "//tools/idea/java/execution/impl:execution-impl",
        "//tools/idea/platform/external-system-impl",
    ],
    test_srcs = ["testSrc"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    runtime_deps = [
        "//tools/adt/idea/adt-branding",
        "//tools/idea/platform/xdebugger-impl",
        "//tools/idea/platform/lvcs-impl",
        "//tools/idea/platform/credential-store",
        "//tools/idea/platform/configuration-store-impl",
        "//tools/idea/platform/tasks-platform-impl",
        "//tools/idea/json",
        "//tools/idea/xml/relaxng",
        "//tools/idea/java/compiler/instrumentation-util-8",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools/adt/idea/android-adb[module, test]",
        "//tools/base/ddmlib:studio.ddmlib[module, test]",
        "//tools/idea/platform/testFramework[module, test]",
        "//tools/base/testutils:studio.testutils[module, test]",
        "//tools/idea/.idea/libraries:truth[test]",
        "//tools/adt/idea/adt-testutils[module, test]",
    ],
)
