package icu.takeneko.omms.crystal.plugin.support

data class ScanData(
    val classFqcn: String,
    val classAnnotations: List<AnnotationData>,
    val methodAnnotations: Map<MethodData, List<AnnotationData>>
)

data class MethodData(
    val modifier: Int,
    val owner: String,
    val name: String,
    val desc: String
)

data class AnnotationData(
    val desc: String,
    val values: Map<String, Any>
)
