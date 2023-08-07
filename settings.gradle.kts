rootProject.name = "intellij-autodev"

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("companion")
include("plugin")

include(
    "pycharm",
    "java",
    "kotlin",
    "webstorm",
    "goland",
    "clion",
    "csharp",
)
