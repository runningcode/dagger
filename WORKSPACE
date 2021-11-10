# Copyright (C) 2017 The Dagger Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

#############################
# Upgrade java_tools version
#############################

# These targets added per instructions at
# https://github.com/bazelbuild/java_tools/releases/tag/javac11_v10.7
http_archive(
    name = "remote_java_tools_linux",
    sha256 = "cf57fc238ed5c24c718436ab4178ade5eb838fe56e7c32c4fafe0b6fbdaec51f",
    urls = [
        "https://mirror.bazel.build/bazel_java_tools/releases/javac11/v10.7/java_tools_javac11_linux-v10.7.zip",
        "https://github.com/bazelbuild/java_tools/releases/download/javac11_v10.7/java_tools_javac11_linux-v10.7.zip",
    ],
)

http_archive(
    name = "remote_java_tools_windows",
    sha256 = "a0fc3a3be3ea01a4858d12f56892dd663c02f218104e8c1dc9f3e90d5e583bcb",
    urls = [
        "https://mirror.bazel.build/bazel_java_tools/releases/javac11/v10.7/java_tools_javac11_windows-v10.7.zip",
        "https://github.com/bazelbuild/java_tools/releases/download/javac11_v10.7/java_tools_javac11_windows-v10.7.zip",
    ],
)

http_archive(
    name = "remote_java_tools_darwin",
    sha256 = "51a4cf424d3b26d6c42703cf2d80002f1489ba0d28c939519c3bb9c3d6ee3720",
    urls = [
        "https://mirror.bazel.build/bazel_java_tools/releases/javac11/v10.7/java_tools_javac11_darwin-v10.7.zip",
        "https://github.com/bazelbuild/java_tools/releases/download/javac11_v10.7/java_tools_javac11_darwin-v10.7.zip",
    ],
)

#############################
# Load nested repository
#############################

# Declare the nested workspace so that the top-level workspace doesn't try to
# traverse it when calling `bazel build //...`
local_repository(
    name = "examples_bazel",
    path = "examples/bazel",
)

#############################
# Load Bazel-Common repository
#############################

http_archive(
    name = "google_bazel_common",
    sha256 = "8b6aebdc095c8448b2f6a72bb8eae4a563891467e2d20c943f21940b1c444e38",
    strip_prefix = "bazel-common-3d0e5005cfcbee836e31695d4ab91b5328ccc506",
    urls = ["https://github.com/google/bazel-common/archive/3d0e5005cfcbee836e31695d4ab91b5328ccc506.zip"],
)

load("@google_bazel_common//:workspace_defs.bzl", "google_common_workspace_rules")

google_common_workspace_rules()

#############################
# Load Protobuf dependencies
#############################

# rules_python and zlib are required by protobuf.
# TODO(ronshapiro): Figure out if zlib is in fact necessary, or if proto can depend on the
# @bazel_tools library directly. See discussion in
# https://github.com/protocolbuffers/protobuf/pull/5389#issuecomment-481785716
# TODO(cpovirk): Should we eventually get rules_python from "Bazel Federation?"
# https://github.com/bazelbuild/rules_python#getting-started

http_archive(
    name = "rules_python",
    sha256 = "e5470e92a18aa51830db99a4d9c492cc613761d5bdb7131c04bd92b9834380f6",
    strip_prefix = "rules_python-4b84ad270387a7c439ebdccfd530e2339601ef27",
    urls = ["https://github.com/bazelbuild/rules_python/archive/4b84ad270387a7c439ebdccfd530e2339601ef27.tar.gz"],
)

http_archive(
    name = "zlib",
    build_file = "@com_google_protobuf//:third_party/zlib.BUILD",
    sha256 = "629380c90a77b964d896ed37163f5c3a34f6e6d897311f1df2a7016355c45eff",
    strip_prefix = "zlib-1.2.11",
    urls = ["https://github.com/madler/zlib/archive/v1.2.11.tar.gz"],
)

#############################
# Load Robolectric repository
#############################

ROBOLECTRIC_VERSION = "4.4"

http_archive(
    name = "robolectric",
    sha256 = "d4f2eb078a51f4e534ebf5e18b6cd4646d05eae9b362ac40b93831bdf46112c7",
    strip_prefix = "robolectric-bazel-%s" % ROBOLECTRIC_VERSION,
    urls = ["https://github.com/robolectric/robolectric-bazel/archive/%s.tar.gz" % ROBOLECTRIC_VERSION],
)

load("@robolectric//bazel:robolectric.bzl", "robolectric_repositories")

robolectric_repositories()

#############################
# Load Kotlin repository
#############################

RULES_KOTLIN_COMMIT = "2c283821911439e244285b5bfec39148e7d90e21"

RULES_KOTLIN_SHA = "b04cd539e7e3571745179da95069586b6fa76a64306b24bb286154e652010608"

http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = RULES_KOTLIN_SHA,
    strip_prefix = "rules_kotlin-%s" % RULES_KOTLIN_COMMIT,
    type = "zip",
    urls = ["https://github.com/bazelbuild/rules_kotlin/archive/%s.zip" % RULES_KOTLIN_COMMIT],
)

load("@io_bazel_rules_kotlin//kotlin:dependencies.bzl", "kt_download_local_dev_dependencies")

kt_download_local_dev_dependencies()

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories")

KOTLIN_VERSION = "1.4.20"

KOTLINC_RELEASE_SHA = "11db93a4d6789e3406c7f60b9f267eba26d6483dcd771eff9f85bb7e9837011f"

KOTLINC_RELEASE = {
    "sha256": KOTLINC_RELEASE_SHA,
    "urls": ["https://github.com/JetBrains/kotlin/releases/download/v{v}/kotlin-compiler-{v}.zip".format(v = KOTLIN_VERSION)],
}

kotlin_repositories(compiler_release = KOTLINC_RELEASE)

register_toolchains("//:kotlin_toolchain")

#############################
# Load Maven dependencies
#############################

RULES_JVM_EXTERNAL_TAG = "2.7"

RULES_JVM_EXTERNAL_SHA = "f04b1466a00a2845106801e0c5cec96841f49ea4e7d1df88dc8e4bf31523df74"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

ANDROID_LINT_VERSION = "26.6.2"

maven_install(
    artifacts = [
        "androidx.annotation:annotation:1.1.0",
        "androidx.appcompat:appcompat:1.3.1",
        "androidx.activity:activity:1.3.1",
        "androidx.fragment:fragment:1.3.6",
        "androidx.lifecycle:lifecycle-common:2.3.1",
        "androidx.lifecycle:lifecycle-viewmodel:2.3.1",
        "androidx.lifecycle:lifecycle-viewmodel-savedstate:2.3.1",
        "androidx.multidex:multidex:2.0.1",
        "androidx.savedstate:savedstate:1.0.0",
        "androidx.test:monitor:1.4.0",
        "androidx.test:core:1.4.0",
        "androidx.test.ext:junit:1.1.3",
        "com.android.support:appcompat-v7:25.0.0",
        "com.android.support:support-annotations:25.0.0",
        "com.android.support:support-fragment:25.0.0",
        "com.android.tools.external.org-jetbrains:uast:%s" % ANDROID_LINT_VERSION,
        "com.android.tools.external.com-intellij:intellij-core:%s" % ANDROID_LINT_VERSION,
        "com.android.tools.external.com-intellij:kotlin-compiler:%s" % ANDROID_LINT_VERSION,
        "com.android.tools.lint:lint:%s" % ANDROID_LINT_VERSION,
        "com.android.tools.lint:lint-api:%s" % ANDROID_LINT_VERSION,
        "com.android.tools.lint:lint-checks:%s" % ANDROID_LINT_VERSION,
        "com.android.tools.lint:lint-tests:%s" % ANDROID_LINT_VERSION,
        "com.android.tools:testutils:%s" % ANDROID_LINT_VERSION,
        "com.github.tschuchortdev:kotlin-compile-testing:1.2.8",
        "com.google.devtools.ksp:symbol-processing-api:1.5.30-1.0.0",
        "com.google.guava:guava:27.1-android",
        "junit:junit:4.13",
        "org.jetbrains.kotlin:kotlin-stdlib:%s" % KOTLIN_VERSION,
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:%s" % KOTLIN_VERSION,
        "org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.3.0",
        "org.robolectric:robolectric:4.4",
        "org.robolectric:shadows-framework:4.4",  # For ActivityController
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://maven.google.com",
        "https://jcenter.bintray.com/",  # Lint has one trove4j dependency in jCenter
    ],
)

#############################
# Load Bazel Skylib rules
#############################

BAZEL_SKYLIB_VERSION = "1.0.2"

BAZEL_SKYLIB_SHA = "97e70364e9249702246c0e9444bccdc4b847bed1eb03c5a3ece4f83dfe6abc44"

http_archive(
    name = "bazel_skylib",
    sha256 = BAZEL_SKYLIB_SHA,
    urls = [
        "https://github.com/bazelbuild/bazel-skylib/releases/download/{version}/bazel-skylib-{version}.tar.gz".format(version = BAZEL_SKYLIB_VERSION),
    ],
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()
